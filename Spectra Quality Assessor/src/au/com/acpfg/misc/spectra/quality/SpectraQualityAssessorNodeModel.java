package au.com.acpfg.misc.spectra.quality;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.com.acpfg.misc.spectra.SpectralDataInterface;


/**
 * This is the model implementation of SpectraQualityAssessor.
 * Implements the 'Xrea' algorithm in the paper entitled "Quality Assessment of Tandem Mass Spectra Based on Cumulative Intensity Normalization" in the journal of proteome research. May implement other algorithms at a future date.
 *
 * @author Andrew Cassin
 */
public class SpectraQualityAssessorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SpectraQualityAssessorNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_SPECTRA              = "spectra-column";
	static final String CFGKEY_ADJUSTMENT_THRESHOLD = "xrea-adjustment-threshold";		// 85% TIC threshold before Xrea adjustment takes place
	static final String CFGKEY_ADJUSTMENT_PEAKS     = "xrea-adjustment-peaks";			// no more than 5 peaks must comprise xrea-adjustment-threshold
	
    /** initial default count value. */
    static final String DEFAULT_SPECTRA = "Spectra";
   
    private final SettingsModelString m_spectra              = new SettingsModelString(CFGKEY_SPECTRA, DEFAULT_SPECTRA);
    private final SettingsModelDoubleBounded m_adj_threshold = new SettingsModelDoubleBounded(CFGKEY_ADJUSTMENT_THRESHOLD, 0.85, 0.0, 1.0);
    private final SettingsModelIntegerBounded m_adj_peaks    = new SettingsModelIntegerBounded(CFGKEY_ADJUSTMENT_PEAKS, 10, 0, 100);
    
    /**
     *  Internal data structure -- not saved
     */
    private SortablePeak[] m_peaks;
    private double[] m_mz;
    private double[] m_i;
    private double m_i_max;
    private double m_tic;
    private int m_num_peaks;

    /**
     * Constructor for the node model.
     */
    protected SpectraQualityAssessorNodeModel() {
        // one incoming port and one outgoing port
        super(1, 1);
        m_peaks = null;
    }

    private ColumnRearranger createColumnRearranger(DataTableSpec in) {
    	ColumnRearranger c = new ColumnRearranger(in);
    	final int index = in.findColumnIndex(m_spectra.getStringValue());
    	
    	DataColumnSpec newColSpec = new DataColumnSpecCreator("Xrea Value", DoubleCell.TYPE).createSpec();
    	DataColumnSpec bigPeakSpec= new DataColumnSpecCreator("Dominant Peak Adjusted Xrea Value", DoubleCell.TYPE).createSpec();
    	
    
    	CellFactory cf = new SingleCellFactory(newColSpec) {
    		public DataCell getCell(DataRow r) {
    			if (index<0)
    				return DataType.getMissingCell();
    			DataCell c = r.getCell(index);
    			if (c.isMissing() || !(c instanceof SpectralDataInterface))
    				return DataType.getMissingCell();
    			SpectralDataInterface spectrum = (SpectralDataInterface) c;
    			
    			// this list is computed here and then thrown away at the end of processing each row for other quality ranking code to use
    	    	double sum = 0.0;
    	    	m_mz = spectrum.getMZ();
    	    	m_i  = spectrum.getIntensity();
    	    	m_peaks = new SortablePeak[m_mz.length];
    	    	for (int j=0; j<m_mz.length; j++) {
    	    		m_peaks[j] = new SortablePeak(m_mz[j], m_i[j]);
    	    		sum += m_i[j];
    	    	}
    	    	Arrays.sort(m_peaks);
    	    	m_tic = sum;
    	    	m_num_peaks = spectrum.getNumPeaks();
    	    	m_i_max = spectrum.getIntensityMostIntense();
    	    	
    			return new DoubleCell(calc_xrea());	
    		}
    	};
    	
    	CellFactory cf2 = new SingleCellFactory(bigPeakSpec) {
    		public DataCell getCell(DataRow r) {
    			DataCell c = r.getCell(index);
    			if (c.isMissing() || !(c instanceof SpectralDataInterface)) {
    				return DataType.getMissingCell();
    			}
    			return new DoubleCell(calc_adjusted_xrea());
    		}
    	};
    	m_peaks = null;
    	c.append(cf);
    	c.append(cf2);
    	
    	return c;
    }
    
    /*
    // code from Henry Lam (author of TPP spectrast, ported from C++ by Andrew Cassin)
    protected double calc_xrea(SpectralDataInterface c) {
    	 int n_peaks = c.getNumPeaks();
    	 
    	 if (n_peaks < 6) {
    		 return (0.0);
    	 }
    	 double[] mz = c.getMZ();
    	 double[] i  = c.getIntensity();
    	 SortablePeak[] ranking = new SortablePeak[mz.length];
     	 double sum = 0.0;
     	 for (int j=0; j<mz.length; j++) {
     		ranking[j] = new SortablePeak(mz[j], i[j]);
     		sum += i[j];
     	 }
     	 Arrays.sort(ranking, new Comparator() {		// descending order
     		public int compare(Object arg0, Object arg1) {
    			SortablePeak a = (SortablePeak) arg0;
    			SortablePeak b = (SortablePeak) arg1;
    		
    			if (a.m_i < b.m_i) 
    				return 1;
    			else if (a.m_i > b.m_i) 
    				return 0;
    			else {
    				return 0;
    			}
    		}
     	 });

     	  double tic = 0.0;
    	  for (double d : i) {
    		  tic += d;
    	  }
    	  double slope = tic / i.length;

    	  double cumInten = 0.0;
    	  double diagonal = 0.0;
    	  double xrea = 0.0;
    	  double triangle = 0.0;

    	  for (int rank = ranking.length - 1; rank >= 0; rank--) {
    	    diagonal += slope;
    	    cumInten += ranking[rank].getIntensity();
    	    xrea     += diagonal - cumInten;
    	    triangle += diagonal;
    	  }

    	  xrea = xrea / triangle;

    	  return ((double)(xrea));
    }*/
    
    /**
     *  A problem with the Xrea calculation is what happens when only a few peaks dominate more than 90% of the TIC (sum of all intensity).
     *  This code removes the dominant peaks, subject to user configuration, and then computes the adjusted xrea score using the remaining peaks only.
     *  If a spectra does not have dominant peaks, the result will be the same as calc_xrea()
     */
    protected double calc_adjusted_xrea() {
    	assert(m_peaks != null && m_tic >= 0.0); // m_peaks must already be computed by xrea
    	
    	// poor spectra?
    	if (m_peaks.length < 6) 
    		return 0.0;
    	
    	// how many peaks to reach x% of tic?
    	int          cnt = 0;
    	double       cum = 0.0;
    	double threshold = m_adj_threshold.getDoubleValue();
    	int max_peaks    = m_adj_peaks.getIntValue();
    	
    	for (int i=m_peaks.length-1; i>=0; i--) {
    		cum += m_peaks[i].getIntensity();
    		
    		if (cum >= threshold * m_tic && cnt <= max_peaks) {
    			// recompute the members with dominant peaks removed and recompute xrea
    			double sum = 0.0;
    			m_num_peaks -= cnt;	// eliminate dominant peaks
    			m_i_max = 0.0;
    	    	m_mz = new double[m_num_peaks];
    	    	m_i  = new double[m_num_peaks];
    	    	SortablePeak[] tmp  = new SortablePeak[m_num_peaks];
    	    	for (int j=m_num_peaks-1; j>=0; j--) {
    	    		SortablePeak sp = new SortablePeak(m_peaks[j].getMZ(), m_peaks[j].getIntensity());
    	    		m_mz[j]= sp.getMZ();
    	    		m_i[j] = sp.getIntensity();
    	    		tmp[j] = sp;
    	    		sum   += sp.getIntensity();
    	    		if (sp.getIntensity() > m_i_max) {
    	    			m_i_max = sp.getIntensity();
    	    		}
    	    	}
    	    	m_peaks = tmp;
    	    	Arrays.sort(m_peaks);
    	    	m_tic = sum;
    	    	
    	    	// since the members have been adjusted to exclude dominant peaks, this will not be the same as the adjacent column
    			return calc_xrea();
    		}
    		
    		cnt++;
    	}
    	
    	// no adjustment
    	return calc_xrea();
    }
    
    protected double calc_xrea() {
    	// spectra with only a few peaks are useless for this calculation...
    	if (m_num_peaks > 5) {
        	assert(m_i.length == m_mz.length);
        	
        	// xrea calculation
        	double[] relative_intensity = new double[m_mz.length];
        	double sum_so_far = 0.0;
        	double peak_area = 0.0;
        	
        	for (int j=0; j<m_mz.length; j++) {
        		sum_so_far += m_peaks[j].getIntensity();
        		relative_intensity[j] = sum_so_far / m_tic;
        		peak_area += relative_intensity[j];
        		
        		//System.err.println(peaks[j].getMZ() + " " + relative_intensity[j]);
        	}
        	int last_intensity = m_mz.length - 1;
        	double triangle_area = 0.5 * m_mz.length * relative_intensity[last_intensity];
        	
        	double alpha = relative_intensity[last_intensity] - relative_intensity[last_intensity-1];
        	return ((triangle_area - peak_area) / (triangle_area + alpha));
    	}
    	
    	return 0.0; // minimum area between curve and diagonal ie. poorest quality spectra
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	ColumnRearranger c = createColumnRearranger(inData[0].getDataTableSpec());
    	BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], c, exec);
    	return new BufferedDataTable[] {out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
    	DataColumnSpec c = inSpecs[0].getColumnSpec(m_spectra.getStringValue());
    	if (c==null || !c.getType().isCompatible(SpectralDataInterface.class)) {
    		throw new InvalidSettingsException("No suitable spectra column found!");
    	}
    	ColumnRearranger cr = createColumnRearranger(inSpecs[0]);
    	DataTableSpec result = cr.createSpec();
    	return new DataTableSpec[] {result};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_spectra.saveSettingsTo(settings);
        m_adj_threshold.saveSettingsTo(settings);
        m_adj_peaks.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_spectra.loadSettingsFrom(settings);
        m_adj_threshold.loadSettingsFrom(settings);
        m_adj_peaks.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_spectra.validateSettings(settings);
        m_adj_threshold.validateSettings(settings);
        m_adj_peaks.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * Sorts peaks by ascending intensity (or area if thats what the data contains)
     * @author andrew.cassin
     *
     */
    private class SortablePeak implements Comparator, Comparable, Serializable {
    	private double m_mz;
    	private double m_i;
    	
    	public SortablePeak(double mz, double i) {
    		m_mz = mz;
    		m_i  = i;
    	}
    	
    	public double getMZ() {
    		return m_mz;
    	}

    	public double getIntensity() {
    		return m_i;
    	}
    	
    	public String toString() {
    		return m_mz + " " + m_i;
    	}
    	
		@Override
		public int compare(Object arg0, Object arg1) {
			SortablePeak a = (SortablePeak) arg0;
			SortablePeak b = (SortablePeak) arg1;
		
			if (a.m_i < b.m_i) 
				return -1;
			else if (a.m_i > b.m_i) 
				return 1;
			else {
				if (a.m_mz < b.m_mz)
					return -1;
				else if (a.m_mz > b.m_mz)
					return 1;
				// else...
				return 0;
			}
		}

		@Override
		public int compareTo(Object arg0) {
			return compare(this, arg0);
		}
    	
    	
    }

}

