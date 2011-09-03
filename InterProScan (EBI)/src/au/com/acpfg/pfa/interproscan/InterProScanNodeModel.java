package au.com.acpfg.pfa.interproscan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Set;

import javax.xml.rpc.ServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import uk.ac.ebi.webservices.jaxws.IPRScanClient;
import uk.ac.ebi.webservices.jaxws.stubs.iprscan.ArrayOfString;
import uk.ac.ebi.webservices.jaxws.stubs.iprscan.InputParameters;
import uk.ac.ebi.webservices.jaxws.stubs.iprscan.ObjectFactory;
import uk.ac.ebi.webservices.jaxws.stubs.iprscan.WsResultType;
import au.com.acpfg.xml.reader.XMLCell;


/**
 * This is the model implementation of InterProScan.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * @author Andrew Cassin
 */
public class InterProScanNodeModel extends NodeModel {
	/**
	 * Sequences shorter than 80aa are unlikely to match anything in InterProScan, so
	 * we skip them to avoid upsetting EBI with useless jobs
	 */
	public static final int MIN_LIKELY_INTERPROSCAN_DB = 80;

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(InterProScanNodeModel.class);
    
    static final String CFGKEY_EMAIL    = "email-address";
    static final String CFGKEY_SEQ      = "sequences-from";
    static final String CFGKEY_USE_CRC  = "use-crc?";
    static final String CFGKEY_USE_APPL = "algorithms-to-use";
    static final String CFGKEY_IMGDIR   = "image-directory";
    static final String CFGKEY_SAVEIMGS = "save-images?";
    static final String CFGKEY_IMGSUBSET= "image-subset";
    
    private static final String DEFAULT_EMAIL    = "who@what.ever.some.where";
    private static final String DEFAULT_SEQ      = "Sequence";
    private static final boolean DEFAULT_USE_CRC = true;
    private static final String DEFAULT_USE_APPL= "blastprodom";

    
    // configure-dialog state which must be persistent
    private final SettingsModelString m_email = new SettingsModelString(CFGKEY_EMAIL, DEFAULT_EMAIL);
    private final SettingsModelString m_seq   = new SettingsModelString(CFGKEY_SEQ, DEFAULT_SEQ);
    private final SettingsModelBoolean m_crc  = new SettingsModelBoolean(CFGKEY_USE_CRC, DEFAULT_USE_CRC);
    private final SettingsModelStringArray m_vec = new SettingsModelStringArray(CFGKEY_USE_APPL, new String[] {DEFAULT_USE_APPL});
    private final SettingsModelBoolean m_save_imgs = new SettingsModelBoolean(CFGKEY_SAVEIMGS, false);
    private final SettingsModelString  m_img_dir   = new SettingsModelString(CFGKEY_IMGDIR, "c:/temp");
    
    /**
     * Constructor for the node model.
     */
    protected InterProScanNodeModel() {
            super(1, 1);
            m_img_dir.setEnabled(false);		// since m_save_imgs is false by default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	IPRScanClient cli = new IPRScanClient();
    	int seq_idx = inData[0].getDataTableSpec().findColumnIndex(m_seq.getStringValue());
    	if (seq_idx < 0) {
    		throw new Exception("Invalid sequence column... re-configure the node?");
    	}
    	if (m_email.getStringValue().equals(DEFAULT_EMAIL) || m_email.getStringValue().trim().length() < 1) {
    		throw new Exception("You must provide a valid email address. Re-configure the node.");
    	}
    	RowIterator it = inData[0].iterator();
    
    	// creator of objects for webservice
		ObjectFactory of = new ObjectFactory();
    	
    	// create output container
		int n_cols = 3;
		if (m_save_imgs.getBooleanValue())
			n_cols++;
    	DataColumnSpec[] cols = new DataColumnSpec[n_cols];
    	cols[0] = new DataColumnSpecCreator("Job ID", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("InterProScan Results (XML)", XMLCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("InterProScan Tool Output", StringCell.TYPE).createSpec();
    	
    	if (n_cols > 3) {
    		cols[3] = new DataColumnSpecCreator("InterProScan Results Summary (PNG)", DataType.getType(PNGImageCell.class)).createSpec();
    	}
    	DataTableSpec outputSpec = new DataTableSpec(cols);
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
    	
        
    	// run the jobs...
        double n_rows = inData[0].getRowCount();
       
        exec.setProgress(0.0);
        HashMap<String,String> outstanding_jobs = new HashMap<String,String>();
        int batch_size = 25;
    	for (int done=0; (done<n_rows) || (outstanding_jobs.size() > 0); ) {
    		if (outstanding_jobs.size() < batch_size && it.hasNext()) {
	    		DataRow r = it.next();
	    		DataCell seq_cell = r.getCell(seq_idx);
	    		if (seq_cell.isMissing()) {
	    			continue;
	    		}
	    		StringCell sc = (StringCell) seq_cell;
	    		String seq = sc.getStringValue().trim();
	    		String rkey = r.getKey().getString();

	    		if (seq.length() < 1) {
	    			logger.warn("Skipping empty sequence for row "+rkey);
	    			continue;
	    		}
	    		if (seq.length() < MIN_LIKELY_INTERPROSCAN_DB) {
	    			logger.warn("Sequence for row "+rkey+" is too short to match InterProScan, skipping.");
	    			continue;
	    		}
	    	
	    		String job_id = submit_job_async(cli, m_email.getStringValue(), seq, rkey, of);
	    		
	    		logger.info("Submitted job for row "+rkey+": "+job_id);
	    		
	    		// if no exception thrown, we assume job was successfully submitted, so...
    			outstanding_jobs.put(job_id, rkey);
    		} else {
    			// must wait for entire batch to complete -- EBI terms of service
    			wait_for_completion(cli, outstanding_jobs.keySet());
    			
    			// process results
    			for (String key : outstanding_jobs.keySet()) {
    				//WsResultType[] types = cli.getResultTypes(key);
    				//for (WsResultType type : types) {
    				//	System.err.println(type.getIdentifier()+" "+type.getFileSuffix()+" "+type.getMediaType());
    				//}
    				byte[] results = cli.getSrvProxy().getResult(key, "xml", null);
    				DataCell xml = DataType.getMissingCell();
    				if (results != null) {
    					xml = new XMLCell(new String(results));
    				}
    				
    				// retrieve the InterProSequence so that user can compare with input sequence
    				String tool = "<html><pre>";
    				try {
	    				byte[] tool_bytes = cli.getSrvProxy().getResult(key, "out", null);
	    				if (tool_bytes != null && tool_bytes.length > 0) {
	    					tool = "<html><pre>"+new String(tool_bytes);
	    				}
    				} catch (Exception e) {
    					logger.warn("No tool output for (no data available from EBI), job:"+key);
    				}
    				
    				// fetch an image of the results as well?
    				if (n_cols > 3) {
    					// fetch PNG from EBI and install into table (if data available from EBI)
    					results = cli.getSrvProxy().getResult(key, "visual-png", null);
    					DataCell png_cell = DataType.getMissingCell();
    					if (results != null && results.length > 0) {
    						png_cell = new PNGImageContent(results).toImageCell();
    					}
    					container.addRowToTable(new DefaultRow(outstanding_jobs.get(key), new StringCell(key), xml, new StringCell(tool), png_cell));	
    					FileOutputStream fos = null;
    					try {
    						fos = new FileOutputStream(new File(m_img_dir.getStringValue(), key+".png"));
    						fos.write(results);
    						fos.close();
    					} catch (Exception e) {
    						logger.warn("Unable to save: "+key+", reason: "+e.getMessage());
    						if (fos != null)
    							fos.close();
    					}
    					
    				} else {
    					container.addRowToTable(new DefaultRow(outstanding_jobs.get(key), new StringCell(key), xml, new StringCell(tool)));	
    				}
    				
    			}
        		done += outstanding_jobs.size();
        		outstanding_jobs.clear();
        		exec.setProgress(((double)done) / n_rows);

    		}
    		
    		exec.checkCanceled();
       	}
    	
    	container.close();
        BufferedDataTable out = container.getTable();
    	return new BufferedDataTable[] {out};
    }

    private String submit_job_async(IPRScanClient cli, String email_address, String seq, String rkey, ObjectFactory of) throws Exception {
		for (int retry=0; retry < 4; retry++) { 
			try {
				InputParameters job_params = new InputParameters();
	    		ArrayOfString aos = new ArrayOfString();
	    		for (String appl : m_vec.getStringArrayValue()) {
	    			aos.getString().add(appl.toLowerCase());
	    		}
	    		job_params.setAppl(of.createInputParametersAppl(aos));
	    		job_params.setSequence(of.createInputParametersSequence(seq));
	    		job_params.setGoterms(of.createInputParametersGoterms(new Boolean(true)));
	    		job_params.setNocrc(of.createInputParametersNocrc(new Boolean(!m_crc.getBooleanValue())));
   	    		
				return cli.runApp(m_email.getStringValue(), rkey, job_params);
			} catch (RemoteException re) {
				throw re;
			} catch (ServiceException se) {
				throw se;
			} catch (SOAPFaultException soape) {
				throw soape;
			} catch (Exception e) {
				int delay = (retry+1)*500;	// seconds
				logger.warn("Problem when submitting job: "+e.getMessage()+ "... retrying in "+delay+" seconds");
				Thread.sleep(delay*1000);
			}
		}
		throw new FailedJobException("Cannot submit job after four attempts... giving up on "+rkey+"!");
	}

	private void wait_for_completion(IPRScanClient cli, Set<String> keySet) 
    				throws ServiceException, InterruptedException, FailedJobException, IOException {
    	boolean wait = true;	// mandatory wait for first job in batch
    	for (String s : keySet) {
    		for (int idx=0; idx < 1000; idx++) {
    			if (wait) {
    				int delay = (20+idx);		// seconds
    				logger.info("Pausing to meet EBI requirements: "+delay+" seconds.");
    				Thread.sleep(delay*1000);
    			}
        		String status = cli.checkStatus(s).toLowerCase();
        		// completed or finished?
        		if (status.startsWith("complete") || status.startsWith("finish")) {
        			wait = false;	// check status without waiting for rest of batch
        			break;		// wait for next job
        		} else if (status.startsWith("fail") || status.startsWith("error")) {			// something go wrong?
        			throw new FailedJobException("Job "+s+" has failed at EBI. Aborting run.");
        		}
        		else {
        			// incomplete so just go around again...
        			wait = true;
        		}
    		}
    	}
    	
    	// once we get here the entire batch is done
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

        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_email.saveSettingsTo(settings);
    	m_seq.saveSettingsTo(settings);
    	m_crc.saveSettingsTo(settings);
    	m_vec.saveSettingsTo(settings);
    	m_save_imgs.saveSettingsTo(settings);
    	m_img_dir.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.loadSettingsFrom(settings);
    	m_seq.loadSettingsFrom(settings);
    	m_crc.loadSettingsFrom(settings);
    	m_vec.loadSettingsFrom(settings);
    	m_save_imgs.loadSettingsFrom(settings);
    	m_img_dir.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.validateSettings(settings);
    	m_seq.validateSettings(settings);
    	m_crc.validateSettings(settings);
    	m_vec.validateSettings(settings);
    	m_save_imgs.loadSettingsFrom(settings);
    	m_img_dir.loadSettingsFrom(settings);
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

