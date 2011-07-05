package au.com.acpfg.misc.fastawriter;

import java.io.*;

import org.knime.core.data.*;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.*;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of FastaWriter.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * @author Andrew Cassin
 */
public class FastaWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(FastaWriterNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
    static final String CFGKEY_ACCSN = "Accession";
	static final String CFGKEY_DESCR = "Description";
	static final String CFGKEY_SEQ   = "Sequence";
	static final String CFGKEY_FILE  = "Output Filename";
    static final String CFGKEY_OVERWRITE = "overwrite";
    static final String CFGKEY_MAXLINELEN= "max-sequence-line-length";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelColumnName m_accsn_col = (SettingsModelColumnName) make(CFGKEY_ACCSN);
    private final SettingsModelColumnName m_descr_col = (SettingsModelColumnName) make(CFGKEY_DESCR);
    private final SettingsModelColumnName m_seq_col   = (SettingsModelColumnName) make(CFGKEY_SEQ);
    private final SettingsModelString      m_filename = (SettingsModelString) make(CFGKEY_FILE);
    private final SettingsModelBoolean m_overwrite = (SettingsModelBoolean) make(CFGKEY_OVERWRITE);
    private final SettingsModelIntegerBounded m_max_line_len = (SettingsModelIntegerBounded) make(CFGKEY_MAXLINELEN);
   
    /**
     * Constructor for the node model.
     */
    protected FastaWriterNodeModel() {
        super(1, 1);
    }

    public static SettingsModel make(String field_name) {
    	if (field_name.equals(CFGKEY_ACCSN)) 
    		return new SettingsModelColumnName(CFGKEY_ACCSN, CFGKEY_ACCSN); 
    	else if (field_name.equals(CFGKEY_DESCR))
    		return new SettingsModelColumnName(CFGKEY_DESCR, CFGKEY_DESCR); 
    	else if (field_name.equals(CFGKEY_SEQ))
    		return new SettingsModelColumnName(CFGKEY_SEQ, CFGKEY_SEQ);
    	else if (field_name.equals(CFGKEY_FILE))
    		return new SettingsModelString(CFGKEY_FILE, ""); 
    	else if (field_name.equals(CFGKEY_OVERWRITE)) 
    		return new SettingsModelBoolean(CFGKEY_OVERWRITE, false);
    	else if (field_name.equals(CFGKEY_MAXLINELEN)) 
    		return new SettingsModelIntegerBounded(CFGKEY_MAXLINELEN, 80, 10, 100000);
    	else
    		return null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Writing fasta file... "+m_filename.getStringValue());
        String fname = m_filename.getStringValue();
        if (fname == null || fname.length() < 1) {
        	throw new Exception("No filename specified... nothing to save!");
        }
        
        File f = new File(fname);
        if (!m_overwrite.getBooleanValue() && f.exists()) {
        	throw new Exception("Will not overwrite existing: "+fname+" - configure the node to override if this is what you want.");
        }
        // replicate input data on output port
        DataTableSpec inSpec  = inData[0].getDataTableSpec();
        DataTableSpec outSpec = new DataTableSpec("Input Data", inSpec, new DataTableSpec());
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outSpec);
      
        PrintWriter out_stream = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
        boolean accsn_use_rid = m_accsn_col.useRowID();
        int accsn_idx = inSpec.findColumnIndex(m_accsn_col.getStringValue());
        boolean descr_use_rid = m_descr_col.useRowID();
        int descr_idx = inSpec.findColumnIndex(m_descr_col.getStringValue());
        boolean seq_use_rid   = m_seq_col.useRowID();
        int seq_idx   = inSpec.findColumnIndex(m_seq_col.getStringValue());
        int maxll     = m_max_line_len.getIntValue();
        RowIterator it = inData[0].iterator();
        for (int i = 0; i < inData[0].getRowCount(); i++) {
            DataRow r = it.next();
            String accsn, descr, seq;
            if (accsn_use_rid) {
            	accsn = r.getKey().getString();
            } else {
            	DataCell cell = r.getCell(accsn_idx);
            	if (cell.isMissing())
            		continue;
            	accsn = cell.toString();
            }
            if (descr_use_rid) {
            	descr = r.getKey().toString();
            } else {
            	DataCell cell = r.getCell(descr_idx);
            	if (cell.isMissing())
            		continue;
            	descr = cell.toString();
            }
            if (seq_use_rid) {
            	seq = r.getKey().toString();
            } else {
            	DataCell cell = r.getCell(seq_idx);
            	if (cell.isMissing())
            		continue;
            	seq   = cell.toString();
            }
            int len = seq.length();
            
            // for correct FASTA files, ignore row if no valid sequence...
            if (len > 0) {
	            out_stream.println(">"+accsn+" "+descr);
	            if (len > maxll) {
		            int offset = 0;
		            
		            int written = 0;
		            while (offset < len) {
		            	int end = offset + maxll;
		            	if (end > len) {
		            		end = len;
		            	}
		            	String substring = seq.substring(offset, end);
		            	written += substring.length();
		            	out_stream.println(substring);
		            	offset += maxll;
		            }
		            if (written != len) {
		            	throw new Exception("Could not save sequence (written != sequence length): "+accsn);
		            }
	            } else {
	            	out_stream.println(seq);
	            }
            }
            container.addRowToTable(r);
            
            // check if the execution monitor was canceled
        	if (i % 100 == 0) {
        		try {
        			exec.checkCanceled();
        		} catch (CanceledExecutionException ce) {
        			out_stream.close();			// avoid file leak
        			throw ce;
        		}
        		exec.setProgress(i / (double)inData[0].getRowCount(), 
        				"Writing row " + i);
        	}
        }
        out_stream.close();
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
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
    	 m_accsn_col.saveSettingsTo(settings);
         m_descr_col.saveSettingsTo(settings);
         m_seq_col.saveSettingsTo(settings);
         m_filename.saveSettingsTo(settings); 
         m_overwrite.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	
        m_accsn_col.loadSettingsFrom(settings);
        m_descr_col.loadSettingsFrom(settings);
        m_seq_col.loadSettingsFrom(settings);
        m_filename.loadSettingsFrom(settings); 
        m_overwrite.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
           
    	 m_accsn_col.validateSettings(settings);
         m_descr_col.validateSettings(settings);
         m_seq_col.validateSettings(settings);
         m_filename.validateSettings(settings);
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

