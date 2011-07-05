package au.com.acpfg.xml.reader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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


/**
 * This is the model implementation of XMLReader.
 * Loads XML documents (either in a folder or files) into XML cells for further processing by the XQuery Processor node
 *
 * @author Andrew Cassin
 */
public class XMLReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(XMLReaderNodeModel.class);
        
    // configuration keys used by the dialog class
	static final String CFGKEY_FILE           = "filename";
	static final String CFGKEY_FOLDER         = "folder";
	static final String CFGKEY_LOAD_FOLDER    = "load-folder?";
	static final String CFGKEY_SINGLE_NS      = "single-namespace";
	
	// defaults for persistent state
	private static final String DEFAULT_FILE   = "c:/temp/foo.xml";
	private static final String DEFAULT_FOLDER = "c:/temp";
	    
	// persistent state 
	private final SettingsModelString m_filename     = new SettingsModelString(CFGKEY_FILE, DEFAULT_FILE);
	private final SettingsModelString m_folder       = new SettingsModelString(CFGKEY_FOLDER, DEFAULT_FOLDER);
	private final SettingsModelBoolean m_load_folder = new SettingsModelBoolean(CFGKEY_LOAD_FOLDER, false);
	private final SettingsModelBoolean m_ns          = new SettingsModelBoolean(CFGKEY_SINGLE_NS, true);

    

    /**
     * Constructor for the node model.
     */
    protected XMLReaderNodeModel() {
        super(0, 1);
        
        m_filename.setEnabled(true); // must match defaults for the fields
        m_folder.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	/* ensure namespaces are not required in XQueries if requested by user */
        if (m_ns.getBooleanValue()) {
        	
        }
        
        /* setup list of files as requested by user */
        File[] files;
        if (m_load_folder.getBooleanValue()) {
        	File folder = new File(m_folder.getStringValue());
        	files = folder.listFiles(new FileFilter() {

				@Override
				public boolean accept(File arg0) {
					return (arg0.isFile() && arg0.getAbsolutePath().toLowerCase().endsWith("xml"));
				}
        		
        	});
        } else {
        	files = new File[] { new File(m_filename.getStringValue()) };
        }
        logger.info("Found "+files.length+" plausible XML files to process.");
       
        // create output container
        DataColumnSpec[] cols = new DataColumnSpec[2];
        cols[0] = new DataColumnSpecCreator("XML Filename", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("XML Data", XMLCell.TYPE).createSpec();
        DataTableSpec out = new DataTableSpec(cols);
        BufferedDataContainer container = exec.createDataContainer(out, true);

        // load XML
        int done = 0;
        for (File f : files) {
        	DataCell[] cells = new DataCell[2];
        	cells[0]         = new StringCell(f.getAbsolutePath());
        	XMLCell xc       = new XMLCell(f);
        	cells[1]         = xc;
        	container.addRowToTable(new DefaultRow("File"+done++, cells));
        	exec.checkCanceled();
        	exec.setProgress(((double)++done) / files.length);
        }
        container.close();
        
        return new BufferedDataTable[] { container.getTable() };
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
    	DataColumnSpec[] cols = new DataColumnSpec[2];
    	cols[0] = new DataColumnSpecCreator("XML Filename", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("XML Data", XMLCell.TYPE).createSpec();
        return new DataTableSpec[]{new DataTableSpec(cols)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_filename.saveSettingsTo(settings);
    	m_folder.saveSettingsTo(settings);
    	m_load_folder.saveSettingsTo(settings);
    	m_ns.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_filename.loadSettingsFrom(settings);
    	m_folder.loadSettingsFrom(settings);
    	m_load_folder.loadSettingsFrom(settings);
    	m_ns.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       	m_filename.validateSettings(settings);
    	m_folder.validateSettings(settings);
    	m_load_folder.validateSettings(settings);
    	m_ns.validateSettings(settings);
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

