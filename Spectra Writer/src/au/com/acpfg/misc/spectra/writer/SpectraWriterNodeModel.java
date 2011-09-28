package au.com.acpfg.misc.spectra.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.com.acpfg.misc.spectra.MGFSpectraCell;
import au.com.acpfg.misc.spectra.SpectralDataInterface;


/**
 * This is the model implementation of SpectraWriter.
 * Writes a spectra column out to disk for processing with other Mass Spec. software. Supports MGF format but does not guarantee that all input data will be preserved in the created file.
 *
 * @author Andrew Cassin
 */
public class SpectraWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SpectraWriterNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_FILE = "output-file";
	static final String CFGKEY_OVERWRITE = "overwrite";
	static final String CFGKEY_FORMAT = "file-format";
	static final String CFGKEY_COLUMN = "spectra";
	
    private static final String DEFAULT_FILE = "c:/temp/spectra.mgf";
    private static final boolean DEFAULT_OVERWRITE = false;
    private static final String DEFAULT_FORMAT = "Mascot Generic Format";
    private static final String DEFAULT_COLUMN = "Spectra";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_file = new SettingsModelString(CFGKEY_FILE, DEFAULT_FILE);
    private final SettingsModelBoolean m_overwrite = new SettingsModelBoolean(CFGKEY_OVERWRITE, DEFAULT_OVERWRITE);
    private final SettingsModelString m_format = new SettingsModelString(CFGKEY_FORMAT, DEFAULT_FORMAT);
    private final SettingsModelString m_col = new SettingsModelString(CFGKEY_COLUMN, DEFAULT_COLUMN);
    

    /**
     * Constructor for the node model.
     */
    protected SpectraWriterNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int col_idx = inData[0].getDataTableSpec().findColumnIndex(m_col.getStringValue());
    	if (col_idx < 0) {
    		throw new Exception("Cannot find column: "+m_col.getStringValue()+"... bug?");
    	}
    	RowIterator it = inData[0].iterator();
    	
    	int done = 0;
    	int todo = inData[0].getRowCount();
    	PrintWriter pw = new PrintWriter(new FileWriter(new File(m_file.getStringValue())));
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		SpectralDataInterface sdi = (SpectralDataInterface) r.getCell(col_idx);
    		double[] mz = sdi.getMZ();
    		double[] intensity = sdi.getIntensity();
    		String title = sdi.getID();
    		int tc  = sdi.getMSLevel();
    		
    		// HACK TODO: get charge and pepmass via SpectraDataInterface?
    		String charge = "";
    		String pepmass= null;
    		if (sdi instanceof MGFSpectraCell) {
    			MGFSpectraCell mgf = (MGFSpectraCell) sdi;
    			charge = mgf.getCharge();
    			pepmass= mgf.getPepmass();
    			if (pepmass != null && pepmass.trim().length() == 0)
    				pepmass = null;
    		}
    		
    		if (done % 100 == 0) {
    			exec.checkCanceled();
    			exec.setProgress(((double) done)/todo, "Processing spectra "+done);
    		}
    		
    		// write the spectra to the output file
    		pw.println("BEGIN IONS");
    		pw.println("TITLE="+title);
    		pw.println("CHARGE="+charge);
    		if (pepmass != null)
    			pw.println("PEPMASS="+pepmass);
    		for (int i=0; i<mz.length; i++) {
    			pw.print(mz[i]);
    			pw.print(' ');
    			pw.println(intensity[i]);
    		}
    		pw.println("END IONS");
    		
    		done++;
    	}
    	
    	// close the file
    	pw.close();
    	logger.info("Wrote "+done+" spectra.");
    	
    	// done!
        return new BufferedDataTable[]{};
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
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_col.saveSettingsTo(settings);
    	m_file.saveSettingsTo(settings);
    	m_format.saveSettingsTo(settings);
    	m_overwrite.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
     	m_col.loadSettingsFrom(settings);
    	m_file.loadSettingsFrom(settings);
    	m_format.loadSettingsFrom(settings);
    	m_overwrite.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_col.validateSettings(settings);
    	m_file.validateSettings(settings);
    	m_format.validateSettings(settings);
    	m_overwrite.validateSettings(settings);
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

}

