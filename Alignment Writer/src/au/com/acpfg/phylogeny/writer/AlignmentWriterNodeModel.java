package au.com.acpfg.phylogeny.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.com.acpfg.align.muscle.FormattedRenderer;
import au.com.acpfg.align.muscle.MultiAlignmentCell;


/**
 * This is the model implementation of AlignmentWriter.
 * Saves one or more alignments to disk (ie. AlignmentCell's). The filename comes from the RowID
 * with a suitable extension for the desired format (.aln, .clustalw etc.)
 *
 * @author Andrew Cassin
 */
public class AlignmentWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(AlignmentWriterNodeModel.class);
        
    static final String CFGKEY_FORMAT = "alignment-format";
    static final String CFGKEY_COLUMN = "alignment-column";
    static final String CFGKEY_FOLDER = "destination-folder";
    
    private final SettingsModelString m_format = new SettingsModelString(CFGKEY_FORMAT, "Clustal");
    private final SettingsModelString m_column = new SettingsModelString(CFGKEY_COLUMN, "Alignment");
    private final SettingsModelString m_folder = new SettingsModelString(CFGKEY_FOLDER, "c:/temp");

    /**
     * Constructor for the node model.
     */
    protected AlignmentWriterNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	RowIterator it = inData[0].iterator();
    	int col_idx = inData[0].getDataTableSpec().findColumnIndex(m_column.getStringValue());
    	if (col_idx < 0) {
    		throw new Exception("Cannot find column: "+m_column.getStringValue()+", re-configure the node?");
    	}
    	while (it.hasNext()) {
    		DataRow         r = it.next();
    		DataCell aln_cell = r.getCell(col_idx);
    		String basename   = r.getKey().getString();
    		
    		if (aln_cell instanceof MultiAlignmentCell) {
    			MultiAlignmentCell ac = (MultiAlignmentCell) aln_cell;
    			save_alignment(new File(m_folder.getStringValue(), basename + ".aln"), ac, m_format.getStringValue().toLowerCase());
    		}
    	}
    	return null;
    }

    /**
     * Saves the specified alignment cell (instance of MultiAlignmentCell) in the specified file, with the specified format.
     * An error is thrown if the alignment is not valid for the chosen format or cannot be saved for any reason
     * 
     * @param file			   file to save to 
     * @param ac			   cell to save
     * @param alignment_format must be in all lowercase eg. clustal, nexus etc...
     * @throws IOException	   thrown upon exception eg. disk full
     */
    private void save_alignment(File file, MultiAlignmentCell ac, String alignment_format) throws IOException, UnsupportedAlignmentException  {
    	if (alignment_format.startsWith("clustal")) {
    		String txt = ac.getFormattedAlignment(FormattedRenderer.FormatType.F_CLUSTALW);
    		PrintWriter pw = new PrintWriter(new FileOutputStream(file));
    		pw.print(txt);
    		pw.close();
    	} else {
    		throw new UnsupportedAlignmentException("Unsupported format "+alignment_format);
    	}
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_format.saveSettingsTo(settings);
    	m_column.saveSettingsTo(settings);
    	m_folder.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_format.loadSettingsFrom(settings);
    	m_column.loadSettingsFrom(settings);
    	m_folder.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_format.validateSettings(settings);
    	m_column.validateSettings(settings);
    	m_folder.validateSettings(settings);
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

