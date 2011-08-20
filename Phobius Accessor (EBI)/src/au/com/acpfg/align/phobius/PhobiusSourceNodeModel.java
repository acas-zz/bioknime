package au.com.acpfg.align.phobius;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.html.HTMLDocument.Iterator;
import javax.xml.parsers.SAXParserFactory;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import uk.ac.ebi.webservices.axis1.PhobiusClient;
import uk.ac.ebi.webservices.axis1.stubs.phobius.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.phobius.WsResultType;
import uk.ac.ebi.webservices.wsphobius.InputParams;





/**
 * This is the model implementation of PhobiusSource.
 * Takes a list of sequences and appends the results of Phobius webservice invocations (text only for now) to the output port
 *
 * @author Andrew Cassin
 */
public class PhobiusSourceNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(PhobiusSourceNodeModel.class);
    /** the settings key which is used to retrieve and 
    store the settings (from the dialog or from a settings file)    
   (package visibility to be usable from the dialog). */
    static final String CFGKEY_SEQUENCE_COL = "sequence";
    static final String CFGKEY_EMAIL = "email";
    
    private static final String DEFAULT_SEQUENCE_COL = "Sequence";
	private static final String DEFAULT_EMAIL = "must@specify.this.to.use.this.node";
	
	// internal state (persisted as part of workflow)
	private final SettingsModelString m_seq_col     = make_as_string(CFGKEY_SEQUENCE_COL);
	private final SettingsModelString m_email       = make_as_string(CFGKEY_EMAIL);
	    
	// internal state (not persisted)
	private int m_done_rows;
	private PhobiusClient m_phobius;
	
    /**
     * Constructor for the node model.
     */
    protected PhobiusSourceNodeModel() {
        super(1, 1);
        m_phobius = null;
    }
    
    public static SettingsModel make (String k) {
    	if (k.equals(CFGKEY_SEQUENCE_COL)) {
    		return new SettingsModelString(k, DEFAULT_SEQUENCE_COL);
    	} else if (k.equals(CFGKEY_EMAIL)) {
    		return new SettingsModelString(k, DEFAULT_EMAIL);
    	} 
    	return null;
    }
    
    public static SettingsModelString make_as_string(String k) {
    	SettingsModel sm = make(k);
    	return (SettingsModelString) sm;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        int seq_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_seq_col.getStringValue());
        int batch_size = 10;
        if (seq_col_idx < 0) {
        	throw new Exception("Cannot find column: "+m_seq_col.getStringValue()+" - reset the node?");
        }
        
        DataColumnSpec[] cols = new DataColumnSpec[7];
        cols[0] = new DataColumnSpecCreator("JobID (EBI)", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("EBI Results (raw)", StringCell.TYPE).createSpec();
        cols[2] = new DataColumnSpecCreator("Count(Predicted Signal Peptides)", IntCell.TYPE).createSpec();
        cols[3] = new DataColumnSpecCreator("Count(Predicted Transmembrane Helices)", IntCell.TYPE).createSpec();
        cols[4] = new DataColumnSpecCreator("Count(Predicted Domain)", IntCell.TYPE).createSpec();
        cols[5] = new DataColumnSpecCreator("Count(Predicted Cytoplasmic Regions)", IntCell.TYPE).createSpec();
        cols[6] = new DataColumnSpecCreator("Count(Predicted non-Cytoplasmic Regions)", IntCell.TYPE).createSpec();
          
		DataTableSpec outputSpec = new DataTableSpec(inData[0].getDataTableSpec(), new DataTableSpec(cols));
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
          
        RowIterator it = inData[0].iterator();

        m_phobius = new PhobiusClient();
        ArrayList<DataRow> rows_batch = new ArrayList<DataRow>();
        int batch_cnt = 0;
        m_done_rows = 0;
    	ArrayList<HashMap> batch = new ArrayList<HashMap>();

    	
        while (it.hasNext()) {
        	DataRow r = it.next();
        	String sequence = r.getCell(seq_col_idx).toString();
        	
        	if (sequence == null || sequence.length() < 1) {
        		logger.warn("Cannot Phobius with an empty sequence... skipping row "+r.getKey().toString());
        		continue;
        	}
        	
        	HashMap<String,String> f = new HashMap<String,String>();
        	
        	String email = m_email.getStringValue();
        	if (email.equals(DEFAULT_EMAIL) || email.length() < 1) {
        		throw new Exception("Must set email address to be valid for you! EBI require this!");
        	}
        	
        	f.put("key", r.getKey().getString());
        	f.put("email", email);
        	f.put("sequence", sequence);
        	f.put("async", "true");
      
        	batch.add(f);
        	rows_batch.add(r);
        	batch_cnt++;
        	
        	//System.err.println(batch_cnt + " " + batch_size);
        	
        	if (batch_cnt < batch_size && it.hasNext()) {
        		continue;
        	} else {
        		try {
	        		// got complete batch... time to run entire batch on EBI systems...
	        		String[] jobs = runBatch(exec, batch);
	        		waitForBatchCompletion(exec, jobs, inData[0].getRowCount());
	        		batch.clear();

	        		batch_cnt = 0;
	        		int idx = 0;
	        		for (String jobId : jobs) {
	                	String result= getJobResult(jobId);
	                	
	                	// fill in first output port
	                	DataCell[] cells = new DataCell[cols.length];
	                	if (jobId.length() > 0 && result.length() > 0) {
	                		cells[0] = new StringCell(jobId);
	                		cells[1] = new StringCell(result);
	                		grok_cells(jobId, result, cells);
	                	} else {
	                		for (int j=0; j<cells.length; j++) {
	                			cells[j] = DataType.getMissingCell();
	                		}
	                	}
	                	container.addRowToTable(new JoinedRow(rows_batch.get(idx),new DefaultRow(rows_batch.get(idx).getKey(), cells)));
	                	
	                	// check if the execution monitor was canceled
	                    exec.checkCanceled();
	                   
	                    idx++;
	        		}
	        		rows_batch.clear();
        		} catch (Exception e) {
        			e.printStackTrace();
        			System.err.println(e.getMessage());
        			throw e;
        		}
        	}
        }
        	
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }
    
    public String getJobResult(String jobId) throws Exception {
    	WsResultType[] results = m_phobius.getResultTypes(jobId);
    	//System.err.println("Found "+results.length+" files for "+jobId);
    	for (int i=0; i<results.length; i++) {
    		WsResultType file = results[i];
    		if (file.getIdentifier().equals("out")) {
    			byte[] ret = m_phobius.getSrvProxy().getResult(jobId, file.getIdentifier(), null);
    			if (ret == null) {
    				logger.warn("Could not get results for "+jobId+": assuming nothing to report!");
    				return "";
    			}
    			return new String(ret);
    		} /* else {
    			System.err.println("WARNING: Unused result "+i+" filetype: "+file.getIdentifier());
    		} */
    	}
    	return "";
    }
    
    protected void grok_cells(String jobId, String result, DataCell[] cells) {
    	String[] lines = result.split("\n");
    	Pattern p = Pattern.compile("\\s*(FT)\\s*(\\w+)\\s*(\\d+)\\s*(\\d+)\\s*(.*)");
    	int matched_cnt = 0;
    	int n_signals = 0;
    	int n_tm = 0;
    	int n_dom = 0;
    	int n_cyto = 0;
    	int n_non_cyto = 0;
    	for (String l : lines) {
    		Matcher m = p.matcher(l);
    		if (m.matches()) {
    			String entry_type = m.group(1).toUpperCase();
    			String type       = m.group(2).toUpperCase();
    			String start_pos  = m.group(3).toUpperCase();
    			String end_pos    = m.group(4).toUpperCase();
    			String descr      = m.group(5).toUpperCase();
    			matched_cnt++;
    			if (type.startsWith("SIGNAL")) {
    				n_signals++;
    			} else if (type.startsWith("DOM")) {
    				n_dom++;
    			} else if (type.startsWith("TRANS")) {
    				n_tm++;
    			}
    			
    			if (descr.startsWith("CYTO")) {
    				n_cyto++;
    			} else if (descr.startsWith("NON CYTO")) {
    				n_non_cyto++;
    			}
    		}
    	}
    	cells[2] = new IntCell(n_signals);
    	cells[3] = new IntCell(n_tm);
    	cells[4] = new IntCell(n_dom);
    	cells[5] = new IntCell(n_cyto);
    	cells[6] = new IntCell(n_non_cyto);
    	if (matched_cnt < 1) {
    		logger.warn("Did not match any records from job: "+jobId);
    	}
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
     *  Called when each job completes, this routine is responsible for updating the progress bar
     */
    protected void updateProgress(ExecutionContext exec, int n_rows) {
    	 // and update node progress "traffic light"
        exec.setProgress(((double) m_done_rows) / n_rows, "Searched " + m_done_rows);
    }
    
    /**
     * Waits for the entire batch to complete. Since the batch has just been submitted, we wait
     * for at least 60s before checking the first job for completion
     * 
     * @param jobs
     * @throws Exception
     */
    protected void waitForBatchCompletion(ExecutionContext exec, String[] jobs, int n_rows) throws Exception {
    	int to_go = jobs.length;		// assume none have completed
    	
    	for (int i=0; i<12; i++) {
    		Thread.sleep(5 * 1000);     // sleep for five seconds and then check for cancel
    		exec.checkCanceled();
    	}
    	while (to_go > 0) {
    		waitForCompletion(exec, jobs[jobs.length - to_go]);
    		m_done_rows++;
    		logger.info("Job completed: "+jobs[jobs.length - to_go]);
    		exec.checkCanceled();
    		updateProgress(exec, n_rows);
    		to_go--;
    	}
    	logger.info("Batch completed.");
    }
    
    protected void waitForCompletion(ExecutionContext exec, String jobId) throws Exception {
    	if (jobId.length() > 0) {
    		int check_period = 20 * 1000; // every 10s
    		String status = "PENDING";
    		while (status.equals("PENDING") || status.equals("RUNNING")) {
    			try {
    				logger.info("Waiting for "+jobId);
    				status = m_phobius.checkStatus(jobId);
    				if (status.equals("RUNNING") || status.equals("PENDING")) {
    					logger.info(jobId + " " + status + ", sleeping for "+check_period+ " milliseconds");
    					
    					// check ten times each check_period to see if the user pressed cancel
    					for (int i=0; i<10; i++) {
    						Thread.sleep(check_period / 10);
    						exec.checkCanceled();
    					}
    					
    					// each time job is still going, we double check_period to reduce likelihood of overloading EBI
    					check_period *= 2;
    					if (check_period > 200000) {
    						check_period = 200000;
    					}
    				}
    			} catch (IOException e) {
    				throw new Exception("Cannot connect with Phobius (EBI)... aborting"+e);
    			}
    		}
    	} else {
    		throw new Exception("Bogus EBI job id... aborting!");
    	}
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
     * Submits a batch of jobs to EBI and returns the EBI-assigned job-id's to the caller. Returns probably before the jobs complete.
     * @param ip
     * @param d
     * @return
     */
    protected String[] runBatch(ExecutionContext exec, List<HashMap> batch) throws Exception {
    	String[] jobs = new String[batch.size()];
    	int i = 0;
    	for (HashMap h : batch) {
    		exec.checkCanceled(); // stop submitting once cancel chosen by user
    		InputParameters ip = new InputParameters();
    		ip.setSequence(h.get("sequence").toString());
    		ip.setFormat("long");
    		
    		jobs[i++] = m_phobius.runApp(h.get("email").toString(), 
    				                   h.get("key").toString(), 
    				                   ip);
    		logger.info("Submitted Phobius job for row: " + h.get("key"));
    	}
    	return jobs;
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    	m_email.saveSettingsTo(settings);
    	m_seq_col.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_email.loadSettingsFrom(settings);
    	m_seq_col.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_email.validateSettings(settings);
    	m_seq_col.validateSettings(settings);
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

