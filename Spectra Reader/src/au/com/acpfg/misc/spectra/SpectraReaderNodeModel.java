package au.com.acpfg.misc.spectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
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
import org.systemsbiology.jrap.stax.DataProcessingInfo;
import org.systemsbiology.jrap.stax.MSInstrumentInfo;
import org.systemsbiology.jrap.stax.MSOperator;
import org.systemsbiology.jrap.stax.MSXMLParser;
import org.systemsbiology.jrap.stax.MZXMLFileInfo;
import org.systemsbiology.jrap.stax.ScanHeader;
import org.systemsbiology.jrap.stax.SoftwareInfo;


/**
 * This is the model implementation of MzXMLReader.
 * Using the jrap-stax library, this node reads mzXML/mzML
 *
 * @author Andrew Cassin
 */
public class SpectraReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SpectraReaderNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
    static final String CFGKEY_SPECTRA_FOLDER= "spectra-folder";
    static final String CFGKEY_LOAD_SPECTRA= "load-spectra";
    static final String CFGKEY_MZML = "load-mzml";
    static final String CFGKEY_MGF  = "load-mgf";
    
    /** initial default folder to scan for mzxml */
    static final String DEFAULT_SPECTRA_FOLDER = "c:/temp";
    static final boolean DEFAULT_MZML = true;
    static final boolean DEFAULT_MGF  = true;
    
    // number of columns in scan output
    private final static int NUM_SCAN_COLS = 23;
    // number of columns in file summary output
    private final static int NUM_FILE_COLS = 9;

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_folder=new SettingsModelString(CFGKEY_SPECTRA_FOLDER, DEFAULT_SPECTRA_FOLDER);
    private final SettingsModelBoolean m_spectra= new SettingsModelBoolean(CFGKEY_LOAD_SPECTRA, false);
    private final SettingsModelBoolean m_mzml = new SettingsModelBoolean(CFGKEY_MZML, DEFAULT_MZML);
    private final SettingsModelBoolean m_mgf  = new SettingsModelBoolean(CFGKEY_MGF, DEFAULT_MGF);
    
    /**
     * Constructor for the node model.
     */
    protected SpectraReaderNodeModel() {
        //  two outgoing ports
        super(0, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Processing mzXML/mzML files in folder: "+ m_folder.getStringValue());

        File[] entries = new File(m_folder.getStringValue()).listFiles();
        
        // if user requests it we will add columns for spectra/chromatograms
        int extra = 0;
        if (m_spectra.getBooleanValue()) {
        	extra++;
        }
        
        // first output port
        DataColumnSpec[] allColSpecs = new DataColumnSpec[NUM_SCAN_COLS+extra];
        allColSpecs[0] = new DataColumnSpecCreator("Scan Type", StringCell.TYPE).createSpec();
        allColSpecs[1] = new DataColumnSpecCreator("Polarity", StringCell.TYPE).createSpec();
        allColSpecs[2] = new DataColumnSpecCreator("Retention Time", StringCell.TYPE).createSpec();
        allColSpecs[3] = new DataColumnSpecCreator("Base Peak Intensity", DoubleCell.TYPE).createSpec();
        allColSpecs[4] = new DataColumnSpecCreator("Base Peak M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[5] = new DataColumnSpecCreator("Centroided?", IntCell.TYPE).createSpec();
        allColSpecs[6] = new DataColumnSpecCreator("Deisotoped?", IntCell.TYPE).createSpec();
        allColSpecs[7] = new DataColumnSpecCreator("Charge Deconvoluted?", IntCell.TYPE).createSpec();
        allColSpecs[8] = new DataColumnSpecCreator("MS Level (2=MS/MS)", IntCell.TYPE).createSpec();
        allColSpecs[9] = new DataColumnSpecCreator("Scan ID", StringCell.TYPE).createSpec();
        allColSpecs[10] = new DataColumnSpecCreator("Precursor Charge", IntCell.TYPE).createSpec();
        allColSpecs[11] = new DataColumnSpecCreator("Precursor Scan Number", IntCell.TYPE).createSpec();
        allColSpecs[12] = new DataColumnSpecCreator("Precursor Intensity", DoubleCell.TYPE).createSpec();
        allColSpecs[13] = new DataColumnSpecCreator("Precursor M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[14] = new DataColumnSpecCreator("Total Ion Current", DoubleCell.TYPE).createSpec();
        allColSpecs[15] = new DataColumnSpecCreator("Collision Energy", DoubleCell.TYPE).createSpec();
        allColSpecs[16] = new DataColumnSpecCreator("Ionisation Energy", DoubleCell.TYPE).createSpec();
        allColSpecs[17] = new DataColumnSpecCreator("Start M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[18] = new DataColumnSpecCreator("End M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[19] = new DataColumnSpecCreator("Low M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[20] = new DataColumnSpecCreator("High M/Z", DoubleCell.TYPE).createSpec();
        allColSpecs[21] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[22] = new DataColumnSpecCreator("Number of peaks", IntCell.TYPE).createSpec();
        if (extra == 1) {
        	allColSpecs[23] = new DataColumnSpecCreator("Spectra", AbstractSpectraCell.TYPE).createSpec();
        }
        
        // second output port
        DataColumnSpec[] fileSpecs =  new DataColumnSpec[NUM_FILE_COLS];
        fileSpecs[8] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        fileSpecs[0] = new DataColumnSpecCreator("Instrument Manufacturer", StringCell.TYPE).createSpec();
        fileSpecs[1] = new DataColumnSpecCreator("Instrument Model", StringCell.TYPE).createSpec();
        fileSpecs[2] = new DataColumnSpecCreator("Instrument Software", StringCell.TYPE).createSpec();
        fileSpecs[3] = new DataColumnSpecCreator("Instrument Operator", StringCell.TYPE).createSpec();
        fileSpecs[4] = new DataColumnSpecCreator("Mass Analyzer", StringCell.TYPE).createSpec();
        fileSpecs[5] = new DataColumnSpecCreator("Ionization", StringCell.TYPE).createSpec();
        fileSpecs[6] = new DataColumnSpecCreator("Detector", StringCell.TYPE).createSpec();
        fileSpecs[7] = new DataColumnSpecCreator("Data Processing", StringCell.TYPE).createSpec();
        
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        DataTableSpec outputFileSpec = new DataTableSpec(fileSpecs);
        BufferedDataContainer file_container = exec.createDataContainer(outputFileSpec);
       
        // NB: here we dont check with the readers for each filename (maybe take too long with a large number of readers...)
        //     instead, we just hardcode what is supported
        int done = 0;
        ArrayList<File> filtered_entries = new ArrayList<File>();
        for (File f : entries) {
        	String ext = f.getName().toLowerCase();
        	if (! f.isFile()) {
        		continue;
        	}
        	if (ext.endsWith(".xml") || ext.endsWith(".mzxml") 
        			|| ext.endsWith(".mzml") || ext.endsWith(".mgf") || ext.endsWith(".mgf.gz")) {
        		filtered_entries.add(f);
        	}
        }
        int cnt = filtered_entries.size();
        logger.info("Found "+cnt+" plausible files for loading.");
        long scan_id = 1; // must be unique across multiple files
        int file_id = 1;
        
        // instantiate the data processor's for each supported filetype
        ArrayList<AbstractDataProcessor> dp_list = new ArrayList<AbstractDataProcessor>();
        if (m_mzml.getBooleanValue())
        	dp_list.add(new mzMLDataProcessor());
        if (m_mgf.getBooleanValue())
        	dp_list.add(new MGFDataProcessor());
        
        /*
         * For each filtered file we try each processor which can process the file in the order
         * constructed above
         */
        RowSequence scan_seq = new RowSequence("Scan");
        RowSequence file_seq = new RowSequence("File");
        
        for (File f : filtered_entries) {
	        String filename = f.getName();

    		try {
    			logger.info("Processing file: "+filename);
        		exec.checkCanceled();
        		exec.setProgress(((double)done)/cnt, "Processing file "+f.getName());
            	
	    		for (int i=0; i<dp_list.size(); i++) {
	    			AbstractDataProcessor dp = dp_list.get(i);
	    			if (dp.can(f)) {
	    				dp.setInput(f.getAbsolutePath());
	    				dp.process(m_spectra.getBooleanValue(), scan_seq, file_seq,
	    						   exec, container, file_container);
	    				dp.finish();
	    				// short-circuit if successfully processed
	    				break;
	    			}
	    		}
    		} catch (CanceledExecutionException ce) {
    			container.close();
    			file_container.close();
    			throw ce;
    		} catch (Exception e) {
    			e.printStackTrace();
    			logger.warn("Unable to process "+filename+ "... skipping! (file ignored)");
    			logger.warn(e);
    		}
	        
	        done++;
	    	exec.setProgress(((double)done)/cnt, "Completed processing file "+f.getName());
        }
        
        // once we are done, we close the container and return its table
        container.close();
        file_container.close();
        BufferedDataTable out = container.getTable();
        BufferedDataTable out2= file_container.getTable();
        return new BufferedDataTable[]{out,out2};
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
        return new DataTableSpec[]{null,null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_folder.saveSettingsTo(settings);
        m_spectra.saveSettingsTo(settings);
        m_mgf.saveSettingsTo(settings);
        m_mzml.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {   
        m_folder.loadSettingsFrom(settings);
        m_spectra.loadSettingsFrom(settings);
        m_mgf.loadSettingsFrom(settings);
        m_mzml.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_folder.validateSettings(settings);
        m_spectra.validateSettings(settings);
        m_mgf.validateSettings(settings);
        m_mzml.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }

}

