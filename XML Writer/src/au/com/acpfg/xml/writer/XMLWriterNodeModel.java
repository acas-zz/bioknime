package au.com.acpfg.xml.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.com.acpfg.xml.reader.XMLCell;


/**
 * This is the model implementation of XMLWriter.
 * Saves XMLcell's to disk as separate XML documents. 
 *
 * @author Andrew Cassin
 */
public class XMLWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(XMLWriterNodeModel.class);
        
    public static final String CFGKEY_FOLDER    = "xml-folder";
    public static final String CFGKEY_EXTN      = "xml-extension";
    public static final String CFGKEY_BASENAME  ="xml-column-for-basename";
    public static final String CFGKEY_USE_ROWID = "basename-use-rowid?";
    public static final String CFGKEY_XML_COL   = "xml-column";

    
    private final SettingsModelString m_folder     = new SettingsModelString(CFGKEY_FOLDER, "c:/temp");
    private final SettingsModelString m_extn       = new SettingsModelString(CFGKEY_EXTN, ".xml");
    private final SettingsModelString m_basename   = new SettingsModelString(CFGKEY_BASENAME, "");
    private final SettingsModelBoolean m_use_rowid = new SettingsModelBoolean(CFGKEY_USE_ROWID, true);
    private final SettingsModelString m_xml_col    = new SettingsModelString(CFGKEY_XML_COL, "");

    /**
     * Constructor for the node model.
     */
    protected XMLWriterNodeModel() {
        super(1,0);
        m_basename.setEnabled(!m_use_rowid.getBooleanValue());		// must be !m_use_basename.getBooleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	double done = 0.0;
    	int  n_rows = inData[0].getRowCount();
    	RowIterator it = inData[0].iterator();
    	int xml_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_xml_col.getStringValue());
    	while (it.hasNext()) {
    		DataRow row = it.next();
    		DataCell xml_cell = row.getCell(xml_col_idx);
    		if (xml_cell == null || xml_cell.isMissing()) {
    			continue;
    		}
    		XMLCell xc = (XMLCell) xml_cell;
    		
    		// compute the output filename
    		String extn = m_extn.getStringValue().trim();
    		if (extn.length() < 1) {
    			extn = ".xml";
    		} else if (!extn.startsWith(".")) {
    			extn = "." + extn;
    		}
    		String basename = m_use_rowid.getBooleanValue() ? row.getKey().getString() : m_basename.getStringValue();
    		if (basename.trim().length() < 1) {
    			basename = "xml-document";
    		}
    		String dir = m_folder.getStringValue();
    		if (dir.length() < 1) {
    			throw new InvalidSettingsException("Illegal folder to save to... must specify a valid folder. Re-configure the node.");
    		}
    		File       output_file = new File(dir, basename + (int)done + extn);
    		FileReader         fin = null;
    		FileOutputStream   fos = null;
    		BufferedReader      br = null;
    		PrintWriter         pw = null;
    		try {
    			fos                  = new FileOutputStream(output_file);
    			fin                  = new FileReader(xc.asFile());
    			br                   = new BufferedReader(fin);
    			pw                   = new PrintWriter(fos);
    			String line;
    			while ((line = br.readLine()) != null) {
    				pw.println(line);
    			}
    			pw.close();
    			fin.close();
    		} catch (Exception e) {
    			if (br != null)
    				br.close();
    			if (pw != null)
    				pw.close();
    			throw e;
    		}
    		
    		exec.checkCanceled();
    		exec.setProgress(++done / n_rows);
    	}
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
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_folder.saveSettingsTo(settings);
        m_extn.saveSettingsTo(settings);
        m_basename.saveSettingsTo(settings);
        m_use_rowid.saveSettingsTo(settings);
        m_xml_col.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_folder.loadSettingsFrom(settings);
        m_extn.loadSettingsFrom(settings);
        m_basename.loadSettingsFrom(settings);
        m_use_rowid.loadSettingsFrom(settings);
        m_xml_col.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_folder.validateSettings(settings);
        m_extn.validateSettings(settings);
        m_basename.validateSettings(settings);
        m_use_rowid.validateSettings(settings);
        m_xml_col.validateSettings(settings);
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

