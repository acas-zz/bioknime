package au.com.acpfg.align.muscle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.com.acpfg.align.muscle.AlignmentValue;
import au.com.acpfg.align.muscle.AlignmentValue.AlignmentType;

import javax.xml.rpc.*;		// explicitly classload exceptions for runApp() below

import uk.ac.ebi.webservices.axis1.MuscleClient;
import uk.ac.ebi.webservices.axis1.stubs.muscle.InputParameters;




/**
 * This is the model implementation of MuscleAccessor.
 * Provides multiple alignment data from MUSCLE as implemented by EBI
 *
 * @author Andrew Cassin
 */
public class MuscleAlignerNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(MuscleAlignerNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_EMAIL     = "email";
	static final String CFGKEY_SEQ_COL   = "sequence-column";			// list of sequences to align (NB: only one muscle alignment PER ROW)
	static final String CFGKEY_ACCSN_COL = "accession-column";		// descriptor for sequences 
	static final String CFGKEY_ALIGNMENT_TYPE = "alignment-type";
  
	static final String DEFAULT_EMAIL = "must.set.this@to.use.this.node";
	static final String DEFAULT_ALIGNMENT_TYPE = "Protein Sequences";
	
    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_email = make_as_string(CFGKEY_EMAIL);
    private final SettingsModel m_seq_col     = make(CFGKEY_SEQ_COL);
    private final SettingsModel m_accsn_col   = make(CFGKEY_ACCSN_COL);
    //TODO
    private final SettingsModel m_alignment_type = make(CFGKEY_ALIGNMENT_TYPE);
    
    /* internal model state */
    private final HashMap<String,MultiAlignmentCell> m_muscle_map = new HashMap<String,MultiAlignmentCell>();
    
    /**
     * Constructor for the node model.
     */
    protected MuscleAlignerNodeModel() {
        super(1, 1);
    }

    public static SettingsModel make(String k) {
    	if (k.equals(CFGKEY_EMAIL)) {
    		return new SettingsModelString(CFGKEY_EMAIL, DEFAULT_EMAIL );
    	} else if (k.equals(CFGKEY_SEQ_COL)) {
    		return new SettingsModelColumnName(CFGKEY_SEQ_COL, "Sequences");
    	} else if (k.equals(CFGKEY_ACCSN_COL)) {
    		return new SettingsModelColumnName(CFGKEY_ACCSN_COL, "Accessions");
    	} else if (k.equals(CFGKEY_ALIGNMENT_TYPE)) {
    		return new SettingsModelString(k, DEFAULT_ALIGNMENT_TYPE);
    	}
    	return null;
    }
    
    public static SettingsModelString make_as_string(String k) {
    	return (SettingsModelString) make(k);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	if (m_email.equals(DEFAULT_EMAIL)) {
    		throw new Exception("You must set a valid E-Mail for EBI to contact you in the event of problems with the service!");
    	}
    	int n_rows   = inData[0].getRowCount();
    	int seq_idx  = inData[0].getSpec().findColumnIndex(((SettingsModelString)m_seq_col).getStringValue());
    	int accsn_idx= inData[0].getSpec().findColumnIndex(((SettingsModelString)m_accsn_col).getStringValue());
    	if (seq_idx < 0 || accsn_idx < 0) {
    		throw new Exception("Cannot find columns... valid data?");
    	}
    	int done = 0;
    	
    	// create the output columns (raw format for use with R)
		DataTableSpec outputSpec = new DataTableSpec(inData[0].getDataTableSpec(), make_output_spec());
		BufferedDataContainer container = exec.createDataContainer(outputSpec, false, 0);
		 
    	// instantiate MUSCLE client
    	MuscleClient cli = new MuscleClient();
    	
    	// each row is a separate MUSCLE job, the sequences are in one collection cell, the accessions (IDs) in the other
    	RowIterator it = inData[0].iterator();
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		ListCell seqs   = (ListCell) r.getCell(seq_idx);
    		ListCell accsns = (ListCell) r.getCell(accsn_idx);
    		if (seqs.size() != accsns.size()) {
    			throw new Exception("Every sequence must have a corresponding accession: error at row "+r.getKey().getString());
    		}
    		if (seqs.size() < 1) {
    			throw new Exception("Cannot MUSCLE zero sequences: error at row "+r.getKey().getString());
    		}
    		if (seqs.size() > 1000) {
    			throw new Exception("Too many sequences in row "+r.getKey().getString());
    		}
    		// dummy a fake "FASTA" file (in memory) and then submit that to MUSCLE@EBI along with other necessary parameters
    		StringBuffer seq_as_fasta = new StringBuffer();
    		for (int i=0; i<seqs.size(); i++) {
    			seq_as_fasta.append(">");
    			seq_as_fasta.append(accsns.get(i).toString());
    			seq_as_fasta.append("\n");
    			seq_as_fasta.append(seqs.get(i).toString());
    			seq_as_fasta.append("\n");
    		}
    		//System.err.println(seq_as_fasta);
    		
    		// lodge the muscle job and store the results in the output table
    		InputParameters ip = new InputParameters();
    		ip.setSequence(seq_as_fasta.toString());
    		
    		// start the job
    		String jobId = cli.runApp(m_email.getStringValue(), r.getKey().getString(), ip);
    		
    		exec.checkCanceled();
    		exec.setProgress(((double)done)/n_rows, "Executing "+jobId);
    		Thread.sleep(20 * 1000);		// 20 seconds
    		waitForCompletion(cli, exec, jobId);
    		done++;
    		
    		// process results and add them into the table...
    		// 1. fasta alignment data
    		byte[] bytes = cli.getSrvProxy().getResult(jobId, "aln-fasta", null);
    		
			DataCell[] cells = new DataCell[3];
			cells[0] = new StringCell(jobId);
			
			// compute the base64 encoded phylip aligned sequences suitable for use by R's phangorn package
			String fasta = new String(bytes);
			String ret = fasta2phylip(fasta);
			
			// it must be encoded (I chose base64) as it is common to both Java and R and it must be 
			// encoded due to containing multiple lines, which confuses the CSV passed between KNIME and R
			String rk = r.getKey().getString();
			DataCell mac = AlignmentCellFactory.createCell(fasta, AlignmentType.AL_AA);
			if (mac instanceof MultiAlignmentCell)
				m_muscle_map.put(rk, (MultiAlignmentCell) mac);
			cells[1] = mac;
			
			bytes = cli.getSrvProxy().getResult(jobId, "out", null);
			cells[2] = new StringCell("<html><pre>"+new String(bytes));
			
			container.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));
    	}
    	container.close();
    	BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    /**
     *  Ensures an accession from MUSCLE is in a format suitable for phylip. Each accession must be unique to 10chars (sigh)
     * 
     */
    protected String fasta_accession2phylip(String accsn) {
    	if (accsn.length() > 10) {
    		return accsn.substring(0, 10);
    	} else {
    		StringBuffer tmp = new StringBuffer(accsn);
    		int n_spaces = 10 - accsn.length();
    		while (n_spaces-- > 0) {
    			tmp.append(" ");
    		}
    		return tmp.toString();
    	}
    }
    
    /**
     * Returns the list of row ID (as strings) in the muscle map member
     */
    public Set<String> getMuscleMapIDs() {
    	Set<String> ret = m_muscle_map.keySet();
    	return m_muscle_map.keySet();
    }
    
    /**
     * Returns the alignment cell which corresponds to the specified rowkey or <tt>null</tt> if the key does not exist
     */
    public MultiAlignmentCell getAlignment(String rk) {
    	return m_muscle_map.get(rk);
    }
    
    public String getFormattedAlignment(String rk) {
    	return getFormattedAlignment(rk, FormattedRenderer.FormatType.F_CLUSTALW);
    }
    
    public String getFormattedAlignment(String rk, FormattedRenderer.FormatType format) {
    	MultiAlignmentCell cell = getAlignment(rk);
    	return cell.getFormattedAlignment(format);
    }
    
    /**
     * Converts the fasta sequences into phylip format:
     * <number of sequences> <length of aligned sequences>
     * <id - exactly ten-space character padded><sequence for id>
     * ...
     * 
     * @param fasta_aligned_sequences
     * @return the phylip formatted result
     */
    protected String fasta2phylip(String fasta_aligned_sequences) throws IOException {
    	StringBuffer seq = new StringBuffer();
    	int count = 0;
    	int length = 0;
    	boolean prev  = false;
    	BufferedReader rdr = new BufferedReader(new StringReader(fasta_aligned_sequences));
    	String line;
    	String cur_id = null;
    	StringBuffer tmp = new StringBuffer();
    	
    	while ((line = rdr.readLine()) != null) {
    		//System.err.println(cur_id+" "+line);
    		if (line.startsWith(">")) {
    			count++;
    			if (prev) {
    				seq.append(fasta_accession2phylip(cur_id)+tmp+"\n");
    				cur_id = line.trim().substring(1);
    				length = tmp.length();
    				tmp = new StringBuffer();
    			} else {
    				prev = true;
    				cur_id = line.trim().substring(1);
    			}
    		} else {
    			tmp.append(line.trim());
    		}
    	}
    	// dont forget the last sequence
    	if (tmp.length() > 0) {
    		seq.append(fasta_accession2phylip(cur_id)+tmp+"\n");
    	}
    	return " "+count+" "+length+"\n"+seq.toString();
    }
    
    protected void waitForCompletion(MuscleClient cli, ExecutionContext exec, String jobId) throws Exception {
    	if (jobId.length() > 0) {
    		int check_period = 20 * 1000; // every 10s
    		String status = "PENDING";
    		int retry = 0;
    		while (status.equals("PENDING") || status.equals("RUNNING")) {
    			try {
    				logger.info("Waiting for "+jobId);
    				
    				status = cli.checkStatus(jobId);
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
    				if (status == "FAILED") {
    					logger.error("MUSCLE job failed: "+jobId);
    				}
    			} catch (IOException e) {
    				if (retry < 3) {
    					logger.warn("Unable to check job "+jobId+" retrying (after linear-backoff delay)... ");
    					Thread.sleep(((420 * retry) + 120)* 1000);
    					status = "PENDING";
    					retry++;
    				} else {
    					throw new Exception("Cannot check job "+jobId+" via MUSCLE (EBI)... aborting"+e);
    				}
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
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }
    
    protected DataTableSpec make_output_spec() {   
    	DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("MUSCLE@EBI JobID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("MUSCLE Aligned Sequences", MultiAlignmentCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("MUSCLE output", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] { new DataTableSpec(inSpecs[0], make_output_spec()) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_email.saveSettingsTo(settings);
    	m_seq_col.saveSettingsTo(settings);
    	m_accsn_col.saveSettingsTo(settings);
    	m_alignment_type.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.loadSettingsFrom(settings);
    	m_seq_col.loadSettingsFrom(settings);
    	m_accsn_col.loadSettingsFrom(settings);
    	m_alignment_type.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.validateSettings(settings);
    	m_seq_col.validateSettings(settings);
    	m_accsn_col.validateSettings(settings);
    	m_alignment_type.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
           File file = new File(internDir, "muscle-internals.xml");
           FileInputStream fis = new FileInputStream(file);
           ModelContentRO modelContent = ModelContent.loadFromXML(fis);
           try {
        	   String[] keys = modelContent.getStringArray("internal-muscle-map-keys");
        	   m_muscle_map.clear();
        	   ModelContentRO subkey = modelContent.getModelContent("internal-muscle-map");
        	   for (String key : keys) {
        		   DataCell dc = subkey.getDataCell(key);
        		   if (dc instanceof MultiAlignmentCell) {
        			   m_muscle_map.put(key, (MultiAlignmentCell) dc);
        		   }
        	   }
        	   fis.close();
           } catch (InvalidSettingsException e) {
               throw new IOException(e.getMessage());
           }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // save m_muscle_map... 
    	 ModelContent modelContent = new ModelContent("muscle-internals.model");
    	 String[] keys = m_muscle_map.keySet().toArray(new String[0]);
    	 modelContent.addStringArray("internal-muscle-map-keys", keys);
    	 ModelContentWO subkey = modelContent.addModelContent("internal-muscle-map");
    	 for (String key : keys) {
    		 subkey.addDataCell(key, m_muscle_map.get(key));
    	 }
    	 // create the XML file alongside the rest of the node data (same folder)
    	 File file = new File(internDir, "muscle-internals.xml");
         FileOutputStream fos = new FileOutputStream(file);
         modelContent.saveToXML(fos);
    }

}

