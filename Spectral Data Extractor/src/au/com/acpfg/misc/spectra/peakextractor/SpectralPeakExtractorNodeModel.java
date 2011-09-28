package au.com.acpfg.misc.spectra.peakextractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.com.acpfg.misc.spectra.SpectralDataInterface;


/**
 * This is the model implementation of SpectralPeakExtractor.
 * Extracts data defining a peak from any cell supporting SpectralDataInterface (defined in the SpectraReader node)
 *
 * @author Andrew Cassin
 */
public class SpectralPeakExtractorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SpectralPeakExtractorNodeModel.class);
    
    /* what tolerance to the left OR right of the peak should be considered part of the peak? 
     * This value may be customized by the user in the configure dialog for the node 
     */
	public final double DEFAULT_PEAK_HALF_WIDTH = 0.05;		// UNITLESS: IE. Da or Th depending on user
	
	public final double BIN_SIZE = 0.1;
	
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String SPECTRA_COLUMNS = "spectra-cols";


    private final SettingsModelStringArray m_spectra_cols =
        new SettingsModelStringArray(SPECTRA_COLUMNS, new String[] {});
    
    private final Map<String, PeakWindow[]> m_column2Peaks = 
        new HashMap<String, PeakWindow[]>();

    /* internal state for hasPeaksOfInterest() */
    private BitSet m_peakset;
    /* used for row keys -- internal use only */
    private int m_row;
        
    /**
     * Constructor for the node model.
     */
    protected SpectralPeakExtractorNodeModel() {
        // one incoming port and one outgoing port
        super(1, 1);
        m_peakset = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	String[] cols = m_spectra_cols.getStringArrayValue();
    	logger.info("Extracting peak data from "+cols.length+" spectra columns.");
    	int[] col_vec_idx = new int[cols.length];
    	BitSet[] extracted_peaks = new BitSet[cols.length];
    	
    	for (int i=0; i<cols.length; i++) {
    		col_vec_idx[i] = inData[0].getDataTableSpec().findColumnIndex(cols[i]);
    		if (col_vec_idx[i] < 0) {
    			throw new InvalidSettingsException("Cannot find column: "+cols[i]);
    		}
    		PeakWindow[] pw = m_column2Peaks.get(cols[i]);
    		assert(pw != null && pw.length > 0);
    		ArrayList<Double> mz = new ArrayList<Double>();
    		double max_mz = Double.NEGATIVE_INFINITY;
    		for (int j=0; j<pw.length; j++) {
    			double lBound = pw[j].getMZ() - pw[j].getLeft();
    			double uBound = pw[j].getMZ() + pw[j].getRight();
    			if (uBound > max_mz) 
    				max_mz = uBound;
    			for (double k=lBound; k<uBound; k += BIN_SIZE) {
    				mz.add(k);
    			}
    			mz.add(uBound);	// make sure upper bound is represented in window of interest
    		}
    		
    		double[] d_mz = new double[mz.size()];
    		for (int j=0; j<d_mz.length; j++) {
    			d_mz[j] = mz.get(j).doubleValue();
    		}
    		extracted_peaks[i] = calcPeakSet(d_mz, max_mz);
    	}
        
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[10];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Spectra ID", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("User-Specified Peak Name", StringCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("User-Specified Peak M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[3] = 
        	new DataColumnSpecCreator("Input RowID", StringCell.TYPE).createSpec();
        
        allColSpecs[4] = 
            new DataColumnSpecCreator("Number of Intensities", IntCell.TYPE).createSpec();
        allColSpecs[5] =
        	new DataColumnSpecCreator("Sum of Intensities", DoubleCell.TYPE).createSpec();
        allColSpecs[6] = 
        	new DataColumnSpecCreator("Intensities within tolerance", DataType.getType(ListCell.class, DoubleCell.TYPE)).createSpec();
        allColSpecs[7] =
        	new DataColumnSpecCreator("M/Z within tolerance", DataType.getType(ListCell.class, DoubleCell.TYPE)).createSpec();
        allColSpecs[8] =
        	new DataColumnSpecCreator("Maximum Peak Intensity in Spectra", DoubleCell.TYPE).createSpec();
        allColSpecs[9] =
        	new DataColumnSpecCreator("Minimum Peak Intensity in Spectra", DoubleCell.TYPE).createSpec();
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        RowIterator it = inData[0].iterator();
        int todo = inData[0].getRowCount();
        int done = 0;
        
        // first compute the target bits for each column specified in node configure
        
        
        // process each row
        logger.info("Processing "+todo+" rows.");
        int interesting = 0;
        m_row = 1;
        while (it.hasNext()) {
        	DataRow r = it.next();
        	
        	int k = 0;
        	for (int col_idx : col_vec_idx) {
        		SpectralDataInterface sdi = (SpectralDataInterface) r.getCell(col_idx);
        		if (sdi != null && hasPeaksOfInterest(sdi, extracted_peaks[k])) {
        			// still not sure if the exact window has been met, so we check for this before saving rows...
        			if (savePeaksOfInterest(sdi, container, 
        					m_column2Peaks.get(cols[k]), r.getKey().toString()))
        				interesting++;
        		}
        		k++;
        	}
        	if (done % 100 == 0) {
        		exec.checkCanceled();
        		exec.setProgress(((double)done/todo), "Processing row "+done);
        	}
        	done++;
        }
        logger.info("Found "+interesting+" rows (total "+todo+") with some peaks matching your peak windows."); 
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        logger.info("done!");
        return new BufferedDataTable[]{out};
    }

    protected BitSet calcPeakSet(double[] mz, double max) {
    	int n_bits = ((int)(max / BIN_SIZE))+1;
    	BitSet ret = new BitSet(n_bits); // NB: all bits cleared by default
    	
    	for (int i=0; i<mz.length; i++) {
    		int sel = ((int)(mz[i] / BIN_SIZE)) + 1;
    		ret.set(sel);
    	}
    	return ret;
    }
    
    protected boolean hasPeaksOfInterest(SpectralDataInterface sdi, BitSet target) {
    	assert(sdi != null && target != null);
    	assert(m_peakset != null);
    	double[] mz = sdi.getMZ();		// parallel ordered arrays: mz & intensity ie. equal length
    	double[] intensity = sdi.getIntensity();
    	assert(mz.length == intensity.length);
    	
    	BitSet mz_bitset = calcPeakSet(mz, sdi.getMaxMZ());
    	return (mz_bitset.intersects(target));
    }
    
    protected DataCell safe_cell(String s) {
    	return (s != null) ? new StringCell(s) : DataType.getMissingCell();
    }
    
    protected boolean savePeaksOfInterest(SpectralDataInterface sdi, BufferedDataContainer c, PeakWindow[] peaks, String rowkey) {
    	int added = 0;		// number of rows added to table for the current spectra
    	
    	double[] mz = sdi.getMZ();
    	double[] intensity = sdi.getIntensity();
    
    	ArrayList<DoubleCell> matching_mz = new ArrayList<DoubleCell>();
    	ArrayList<DoubleCell> matching_i  = new ArrayList<DoubleCell>();
    	for (PeakWindow cur : peaks) {
    		double lBound = cur.getMZ() - cur.getLeft();
    		double uBound = cur.getMZ() + cur.getRight();
    		double sum = 0.0;
        	int n_peaks = 0;
        	
    		for (int i=0; i<mz.length; i++) {
    			if (mz[i] >= lBound && mz[i] < uBound) {
    				assert(intensity[i] > 0.0);		// does it make sense to have a peak with zero intensity?
    				n_peaks++;
    				sum += intensity[i];
    				matching_i.add(new DoubleCell(intensity[i]));
    				matching_mz.add(new DoubleCell(mz[i]));
    			}
    		}
    		
    		if (n_peaks > 0) {
    			DataCell[] cells = new DataCell[10];
    	    	cells[0] = safe_cell(sdi.getID());
    	    	cells[1] = safe_cell(cur.getName());
    	    	cells[2] = new DoubleCell(cur.getMZ());
    	    	cells[3] = new StringCell(rowkey);
    	    	cells[4] = new IntCell(n_peaks);
    	    	cells[5] = new DoubleCell(sum);
    	    	cells[6] = CollectionCellFactory.createListCell(matching_i);
    	    	cells[7] = CollectionCellFactory.createListCell(matching_mz);
    	    	cells[8] = new DoubleCell(sdi.getIntensityMostIntense());
    	    	cells[9] = new DoubleCell(sdi.getIntensityLeastIntense());
    	    	
    	    	DefaultRow row = new DefaultRow(new RowKey("Match"+m_row), cells);
    	    	m_row++;
    	    	c.addRowToTable(row);
    	    	added++;
    	    	matching_i.clear();
    	    	matching_mz.clear();
    		}
    	}
    
    	return (added > 0) ? true : false;
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	// NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	 for (String peakKey : m_column2Peaks.keySet()) {
             NodeSettingsWO column = settings.addNodeSettings(peakKey);
              
             PeakWindow[] peaks = m_column2Peaks.get(peakKey);
             for (int b = 0; b < peaks.length; b++) {
                 NodeSettingsWO bin = column.addNodeSettings(peaks[b].getName() + "_" + b);
                 try {
                	//logger.info("model save "+peaks[b].getName()+"_"+b);
					peaks[b].saveToSettings(bin);
				} catch (InvalidSettingsException e) {
					// TODO Auto-generated catch block
					logger.warn(e);
					e.printStackTrace();
				}
             }
         }
    	 //logger.info("model save spectra columns "+m_column2Peaks.keySet().size());
         settings.addStringArray(SPECTRA_COLUMNS, m_column2Peaks.keySet().toArray(new String[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 String[] vec = settings.getStringArray(SPECTRA_COLUMNS);
    	 m_spectra_cols.setStringArrayValue(vec);
    	 m_column2Peaks.clear();
   
    	// logger.info("model load spectra columns "+vec.length);
         for (int i = 0; i < vec.length; i++) {
             NodeSettingsRO column = settings.getNodeSettings(vec[i].toString());
             Set<String> peak_names = column.keySet();
             PeakWindow[] peaks = new PeakWindow[peak_names.size()];
             int s = 0;
             for (String peakKey : peak_names) {
                 NodeSettingsRO bin = column.getNodeSettings(peakKey);
                 peaks[s] = new PeakWindow(bin);
                 s++;
             }
             m_column2Peaks.put(vec[i], peaks);
         }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       StringBuffer sb = new StringBuffer();
       
       String[] columns2extract = settings.getStringArray(SPECTRA_COLUMNS);
       if (columns2extract.length < 1) {
    	   sb.append("No spectra column selected!\n");
       }
       for (String col : columns2extract) {
    	   NodeSettingsRO node_set =null;
    	   try {
    		   node_set = settings.getNodeSettings(col);
    	   } catch (InvalidSettingsException e) {
    		   sb.append("Could not find peaks for column: "+col+"!\n");
    	   }
    	   if (node_set != null) {
    		   Set<String> peakIds = node_set.keySet();
    		   if (peakIds.size() < 1) {
    			   sb.append("You must add a peak for "+col+"!\n");
    		   }
    	   }
       }
       if  (sb.length() > 0) {
    	   throw new InvalidSettingsException(sb.toString());
       }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	// NO-OP
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	// NO-OP
    }

}

