package au.com.acpfg.misc.blast.wublast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.rpc.ServiceException;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.JoinedRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import uk.ac.ebi.webservices.axis1.WUBlastClient;
import uk.ac.ebi.webservices.axis1.stubs.wublast.InputParameters;
import uk.ac.ebi.webservices.axis1.stubs.wublast.JDispatcherService_PortType;
import uk.ac.ebi.webservices.axis1.stubs.wublast.JDispatcherService_Service;
import uk.ac.ebi.webservices.axis1.stubs.wublast.JDispatcherService_ServiceLocator;
import uk.ac.ebi.webservices.axis1.stubs.wublast.WsParameterValue;
import uk.ac.ebi.webservices.axis1.stubs.wublast.WsResultType;
import au.com.acpfg.xml.reader.XMLCell;
import au.com.acpfg.xml.reader.XMLUtilityFactory;


/**
 * This is the model implementation of WUBlast.
 * Performs a WU-Blast with the chosen parameters using the EBI webservices. Rate controlled so as not to overload EBI computer systems.
 *
 * @author Andrew Cassin
 */
public class WUBlastNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(WUBlastNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_DB     = "databases";
    static final String CFGKEY_FILTER = "filter";
	static final String CFGKEY_FILTERSTR = "database-filter-string";
    static final String CFGKEY_MATRIX = "matrix";
    static final String CFGKEY_PROGRAMS="programs";
    static final String CFGKEY_SENSITIVITY = "sensitivity";
    static final String CFGKEY_SORT = "sort-by";
    static final String CFGKEY_STATS= "statistics";
    static final String CFGKEY_SEQUENCE_COL = "sequence";
    static final String CFGKEY_EMAIL = "email";
    static final String CFGKEY_NUM_ALIGNMENTS = "num-alignments";
    static final String CFGKEY_NUM_SCORES = "num-scores";
    static final String CFGKEY_EVAL_THRESHOLD = "eval-threshold";
    static final String CFGKEY_EBI_BATCH_SIZE = "ebi-batch-size";
    static final String CFGKEY_STYPE = "sequence-type";
    static final String CFGKEY_SAVE_IMAGE = "save-image";
    
	private static final String DEFAULT_DB = "nr";
	private static final String DEFAULT_FILTER = "";
	private static final String DEFAULT_MATRIX = "blosum62";
	private static final String DEFAULT_PROGRAM= "blastp";
	private static final String DEFAULT_SENSITIVITY = "normal";
	private static final String DEFAULT_SORT = "pvalue";
	private static final String DEFAULT_STATS= "sump";
	private static final String DEFAULT_SEQUENCE_COL = "Sequence";
	private static final String DEFAULT_EMAIL = "must@specify.this.to.use.this.node";
	private static final String DEFAULT_STYPE = "Protein";
	private static final String DEFAULT_EVAL = "1e-5";
	private static final int    DEFAULT_EBI_BATCH_SIZE = 10;

	
    // parameters which must be persisted (see saveSettings() below)
    private final SettingsModelString m_db     = make_as_string(CFGKEY_DB);
    private final SettingsModelString m_filter = make_as_string(CFGKEY_FILTER);
    private final SettingsModelString m_matrix = make_as_string(CFGKEY_MATRIX);
    private final SettingsModelString m_program= make_as_string(CFGKEY_PROGRAMS);
    private final SettingsModelString m_sensitivity = make_as_string(CFGKEY_SENSITIVITY);
    private final SettingsModelString m_sortby      = make_as_string(CFGKEY_SORT);
    private final SettingsModelString m_stats       = make_as_string(CFGKEY_STATS);
    private final SettingsModelString m_seq_col     = make_as_string(CFGKEY_SEQUENCE_COL);
    private final SettingsModelString m_email       = make_as_string(CFGKEY_EMAIL);
    private final SettingsModelString m_stype       = make_as_string(CFGKEY_STYPE);
    private final SettingsModelIntegerBounded m_num_alignments = (SettingsModelIntegerBounded) make(CFGKEY_NUM_ALIGNMENTS);
    private final SettingsModelIntegerBounded m_num_scores = (SettingsModelIntegerBounded) make(CFGKEY_NUM_SCORES);
    private final SettingsModelString  m_eval_threshold =  make_as_string(CFGKEY_EVAL_THRESHOLD);
    private final SettingsModelIntegerBounded m_ebi_batch_size = (SettingsModelIntegerBounded) make(CFGKEY_EBI_BATCH_SIZE);
    private final SettingsModelBoolean m_save_image = (SettingsModelBoolean) make(CFGKEY_SAVE_IMAGE);
    
    // internal state to support Configure dialog
    private static WsParameterValue[] m_ebi_progs, m_ebi_filters, m_ebi_matrices, m_ebi_sensitivity, m_ebi_sort, m_ebi_databases, m_ebi_stats;
        
    // internal stuff to each instance of the model
    private JDispatcherService_PortType m_srv_proxy = null;
    private int m_done_rows, m_n_rows;
    private ExecutionContext m_exec;
    // as each EBI job is returned, these member variables reflect key results from the BLAST
    private DataCell m_result_png;
    private String   m_result_xml;
    private boolean m_first_job, m_has_png, m_has_text, m_has_xml;
    
    /**
     * Constructor for the node model.
     */
    protected WUBlastNodeModel() {
        // one incoming port and two outgoing ports
        super(1, 2);
       
        try {
			m_srv_proxy = get_proxy();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
        try {
        	if (m_ebi_progs == null || m_ebi_progs.length == 0) {
        		logger.info("Loading WU-BLAST configuration data... ");
        		WUBlastClient client = new WUBlastClient();
        		
            	m_ebi_databases   = client.getParamDetail("database").getValues();
            	m_ebi_progs       = client.getParamDetail("program").getValues();
            	m_ebi_filters     = client.getParamDetail("filter").getValues();
            	m_ebi_matrices    = client.getParamDetail("matrix").getValues();
            	m_ebi_sensitivity = client.getParamDetail("sensitivity").getValues();
            	m_ebi_sort        = client.getParamDetail("sort").getValues();
            	m_ebi_stats       = client.getParamDetail("stats").getValues();
            	logger.info("Loaded WU-BLAST configuration data successfully ("+m_ebi_databases.length+" databases). Node may now be configured.");
        	}
        } catch (Exception ex) {
        	logger.warn("Unable to contact EBI WU-BLAST server: results may be incorrect!\n"+ex);
        }
    }

    public static String[] get_ebi(String cfgkey) {
    	if (cfgkey.equals(CFGKEY_DB)) {
    		String[] dbs = get_printable_names(m_ebi_databases, false);
    		Arrays.sort(dbs);
    		return dbs;
    	} else if (cfgkey.equals(CFGKEY_PROGRAMS)) {
    		return get_printable_names(m_ebi_progs, false);
    	} else if (cfgkey.equals(CFGKEY_FILTER)) {
    		return get_printable_names(m_ebi_filters, false);
    	} else if (cfgkey.equals(CFGKEY_MATRIX)) {
    		return get_printable_names(m_ebi_matrices, true);
    	} else if (cfgkey.equals(CFGKEY_SENSITIVITY)) {
    		return get_printable_names(m_ebi_sensitivity, false);
    	} else if (cfgkey.equals(CFGKEY_SORT)) {
    		return get_printable_names(m_ebi_sort, false);
    	} else if (cfgkey.equals(CFGKEY_STATS)) {
    		return get_printable_names(m_ebi_stats, false);
    	}
    	return null; 
    }
    
    protected static String get_ebi_field(WsParameterValue[] od, String val, boolean return_value) {
    	for (WsParameterValue o : od) {
    		if (o.getLabel().equals(val))
    			return return_value ? o.getValue() : val;
    	}
    	return null;
    }
    
    protected static String[] get_printable_names(WsParameterValue[] od, boolean use_value) {
    	ArrayList<String> names = new ArrayList<String>();
    	for (WsParameterValue o : od) {
    		names.add(use_value ? o.getValue() : o.getLabel());
    	}
    	return names.toArray(new String[0]);
    }
    
    public static SettingsModel make (String k) {
    	if (k.equals(CFGKEY_DB)) {
    		return new SettingsModelString(k, DEFAULT_DB);
    	} else if (k.equals(CFGKEY_FILTER)) {
    		return new SettingsModelString(k, DEFAULT_FILTER);
    	} else if (k.equals(CFGKEY_MATRIX)) {
    		return new SettingsModelString(k, DEFAULT_MATRIX);
    	} else if (k.equals(CFGKEY_PROGRAMS)) {
    		return new SettingsModelString(k, DEFAULT_PROGRAM);
    	} else if (k.equals(CFGKEY_SENSITIVITY)) {
    		return new SettingsModelString(k, DEFAULT_SENSITIVITY);
    	} else if (k.equals(CFGKEY_SORT)) {
    		return new SettingsModelString(k, DEFAULT_SORT);
    	} else if (k.equals(CFGKEY_STATS)) {
    		return new SettingsModelString(k, DEFAULT_STATS);
    	} else if (k.equals(CFGKEY_SEQUENCE_COL)) {
    		return new SettingsModelString(k, DEFAULT_SEQUENCE_COL);
    	} else if (k.equals(CFGKEY_EMAIL)) {
    		return new SettingsModelString(k, DEFAULT_EMAIL);
    	} else if (k.equals(CFGKEY_NUM_ALIGNMENTS)) {
    		return new SettingsModelIntegerBounded(k, 50, 0, 1000);
    	} else if (k.equals(CFGKEY_NUM_SCORES)) {
    		return new SettingsModelIntegerBounded(k, 50, 0, 1000);
    	} else if (k.equals(CFGKEY_EVAL_THRESHOLD)) {
    		return new SettingsModelString(k, DEFAULT_EVAL);
    	} else if (k.equals(CFGKEY_EBI_BATCH_SIZE)) {
    		return new SettingsModelIntegerBounded(k, 10, 1, 25);
    	} else if (k.equals(CFGKEY_STYPE)) {
    		return new SettingsModelString(k, DEFAULT_STYPE);
    	} else if (k.equals(CFGKEY_FILTERSTR)) {
    		return new SettingsModelString(k, "");		// no filtering by default
    	} else if (k.equals(CFGKEY_SAVE_IMAGE)) {
    		return new SettingsModelBoolean(CFGKEY_SAVE_IMAGE, false);
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
    	
    	m_exec = exec;
        DataColumnSpec[] cols = new DataColumnSpec[3];
        cols[0] = new DataColumnSpecCreator("EBI JobID", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("Blast Result", XMLCell.TYPE).createSpec();
        cols[2] = new DataColumnSpecCreator("Graphical Result Summary", DataType.getType(PNGImageCell.class)).createSpec();
        
        int seq_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_seq_col.getStringValue());
        int batch_size = m_ebi_batch_size.getIntValue();
        if (seq_col_idx < 0) {
        	throw new Exception("Cannot find column: "+m_seq_col.getStringValue()+" - reset the node?");
        }
        
        DataTableSpec outputSpec = new DataTableSpec(cols);
        
        // here we ensure the XML is put straight onto the disk... not encouraged by knime.org, but realistically the best 
        // option on memory constrained computers
        BufferedDataContainer container = exec.createDataContainer(outputSpec, false, 0);
        
        cols = new DataColumnSpec[13];
        cols[0] = new DataColumnSpecCreator("JobID (EBI)", StringCell.TYPE).createSpec();
        cols[1] = new DataColumnSpecCreator("Hit From", StringCell.TYPE).createSpec();
        cols[2] = new DataColumnSpecCreator("Hit Accession (EBI)", StringCell.TYPE).createSpec();
        cols[3] = new DataColumnSpecCreator("Hit Description", StringCell.TYPE).createSpec();
        cols[4] = new DataColumnSpecCreator("Alignment Score", DoubleCell.TYPE).createSpec();
        cols[5] = new DataColumnSpecCreator("Alignment Bits", DoubleCell.TYPE).createSpec();
        cols[6] = new DataColumnSpecCreator("Alignment E-Value", DoubleCell.TYPE).createSpec();
        cols[7] = new DataColumnSpecCreator("Alignment Identities", DoubleCell.TYPE).createSpec();
        cols[8] = new DataColumnSpecCreator("Alignment Positives", DoubleCell.TYPE).createSpec();
        cols[9] = new DataColumnSpecCreator("Alignment Query Sequence", StringCell.TYPE).createSpec();
        cols[10] = new DataColumnSpecCreator("Alignment Pattern", StringCell.TYPE).createSpec();
        cols[11] = new DataColumnSpecCreator("Alignment Match Sequence", StringCell.TYPE).createSpec();
        cols[12] = new DataColumnSpecCreator("Alignment Query Start,Query End/Match Start,Match End", StringCell.TYPE).createSpec();
        
        DataTableSpec processedResultsOutputSpec = new DataTableSpec(inData[0].getDataTableSpec(), new DataTableSpec(cols));
        BufferedDataContainer proc_results = exec.createDataContainer(processedResultsOutputSpec, false, 0);
        
        RowIterator it = inData[0].iterator();
        m_n_rows = inData[0].getRowCount();
        m_done_rows = 0;
        int                       n_cnt = 1;
        ArrayList<InputParameters> ip_batch = new ArrayList<InputParameters>();
        ArrayList<DataRow>   rows_batch = new ArrayList<DataRow>();
        int                   batch_cnt = 0;
        IXMLParser p = XMLParserFactory.createDefaultXMLParser();
        boolean has_next = it.hasNext();        
        
        if (m_srv_proxy == null) {
        	m_srv_proxy = get_proxy();
        }
        
        while (has_next) {
        	DataRow r = it.next();
        	String sequence = r.getCell(seq_col_idx).toString();
        	
        	if (sequence == null || sequence.length() < 1) {
        		logger.warn("Cannot BLAST with an empty sequence... skipping row "+r.getKey().toString());
        		continue;
        	}
        	InputParameters ip = new InputParameters();
        	
        	ip.setProgram(get_ebi_field(m_ebi_progs, m_program.getStringValue(), false));
        	ip.setDatabase(new String[] {get_ebi_field(m_ebi_databases, m_db.getStringValue(), true) });
        	ip.setMatrix(get_ebi_field(m_ebi_matrices, m_matrix.getStringValue(), false));
        	ip.setFilter(get_ebi_field(m_ebi_filters, m_filter.getStringValue(), false));
        	ip.setSensitivity(get_ebi_field(m_ebi_sensitivity, m_sensitivity.getStringValue(), false));
        	ip.setStats(get_ebi_field(m_ebi_stats, m_stats.getStringValue(), false));
        	ip.setSort(get_ebi_field(m_ebi_sort, m_sortby.getStringValue(), false));
        	ip.setExp(m_eval_threshold.getStringValue());
        	ip.setAlignments(new Integer(m_num_alignments.getIntValue()));
        	ip.setScores(new Integer(m_num_scores.getIntValue()));
        	ip.setSequence(sequence);
        	ip.setStype(m_stype.getStringValue());
        	String email = m_email.getStringValue();
        	if (email.equals(DEFAULT_EMAIL) || email.length() < 1) {
        		throw new Exception("Must set email address to be valid for you! EBI require this!");
        	}
        	
        	ip_batch.add(ip);
        	rows_batch.add(r);
        	
        	batch_cnt++;
        	setFirstJob(true);
        	
        	//System.err.println(batch_cnt + " " + batch_size);
        	has_next = it.hasNext();
        	
        	if (batch_cnt == batch_size || !has_next) {
        		try {
	        		// got full batch... time to run entire batch on EBI systems...
	        		String[] jobs = runBatch(ip_batch);
	        		waitForBatchCompletion(jobs);
	        		ip_batch.clear();
	        		batch_cnt = 0;
	        		int idx = 0;
	        		for (String jobId : jobs) {
	        			m_result_xml = null;
	        			m_result_png = null;
	                	getJobResult(jobId);
	                	
	                	DataRow batch_row = rows_batch.get(idx);
	                	RowKey         rk = batch_row.getKey();
	                	DataCell[] orig_cells = new DataCell[batch_row.getNumCells()];
	        			Iterator<DataCell> rit = batch_row.iterator();
	        			int j=0; 
	        			while (rit.hasNext()){
	        				orig_cells[j++] = rit.next();
	        			}
	        			rit = null;
	        			
	                	// fill in first output port
	                	if (jobId.length() > 0 && m_result_xml.length() > 0) {
		                	container.addRowToTable(new DefaultRow(rk, 
		                			new DataCell[] {new StringCell(jobId), 
		                							safe_xml_cell(m_result_xml),
		                							safe_image_cell(m_result_png)}));
	                	} else {
	                		container.addRowToTable(new JoinedRow(new DefaultRow(rk, orig_cells),
	                											  new DefaultRow(rk, new DataCell[] { DataType.getMissingCell(), DataType.getMissingCell()})));
	                	}
	              
	                	// fill in second output port
	                	n_cnt = process_xml(proc_results, orig_cells, p, jobId, n_cnt);
	                		                	               	
	                	logger.debug("Processed and downloaded results for "+n_cnt+" hits for job: "+jobId);
	                	
	                	// check if the execution monitor was canceled
	                    exec.checkCanceled();
	                   
	                    idx++;
	        		}
	        		rows_batch.clear();
	        		rows_batch = new ArrayList<DataRow>();
	        		jobs = null;
        		}  catch (Exception e) {
        			logger.info(e.getMessage());
        			e.printStackTrace();
        			throw e;
        		}
        	}
        }
        	
        // once we are done, we close the container and return its table
        container.close();
        proc_results.close();
        
        BufferedDataTable out = container.getTable();
        BufferedDataTable out2 = proc_results.getTable();
        return new BufferedDataTable[]{out, out2}; 
    }

    private void setFirstJob(boolean b) {
		m_first_job = b;
	}

	private DataCell safe_xml_cell(String xml) {
    	if (xml == null || xml.length() < 1) 
    		return DataType.getMissingCell();
    	return XMLUtilityFactory.createCell(xml, null);
    }

	private DataCell safe_image_cell(DataCell png_image) {
		if (png_image == null)
			return DataType.getMissingCell();
		return png_image;
	}

	protected String getServiceEndpoint() {
    	return null;
    }
    
    protected JDispatcherService_PortType get_proxy() throws ServiceException {
		JDispatcherService_Service srv = new JDispatcherService_ServiceLocator();

		if (m_srv_proxy == null) {
			if (getServiceEndpoint() != null) {
				try {
					m_srv_proxy = srv.getJDispatcherServiceHttpPort(new java.net.URL(getServiceEndpoint()));
				} catch (java.net.MalformedURLException ex) {
					logger.warn(ex.getMessage());
					m_srv_proxy = srv.getJDispatcherServiceHttpPort();
				}
			} else {
				m_srv_proxy = srv.getJDispatcherServiceHttpPort();
			}
		}
		
		return m_srv_proxy;
	}

	/**
     *  Called when each job completes, this routine is responsible for updating the progress bar
     */
    protected void updateProgress() {
    	 // and update node progress "traffic light"
        m_exec.setProgress(((double) m_done_rows) / m_n_rows, "Searched " + m_done_rows);
    }
   
    
    /**
     * Responsible for parsing the XML results from EBI and storing the de-normalised data into the table as multiple rows per result
     * @param proc_results where to put the results
     * @param r current row being processed in the input data
     * @return 
     */
    protected int process_xml(BufferedDataContainer proc_results, DataCell[] orig_cells, IXMLParser p, String jobId, int rk) throws Exception { 	
    	IXMLReader rdr = StdXMLReader.stringReader(m_result_xml);
    	p.setReader(rdr);
    	//System.err.println(xml);
    	
    	if (!m_result_xml.startsWith("<?xml")) {
    		logger.warn("Result from EBI does not look like XML for job "+jobId+ ": skipping results!");
    		return 0;
    	}
    	IXMLElement root = (IXMLElement) p.parse();
    	Vector v_ss = root.getChildrenNamed("SequenceSimilaritySearchResult");
    	if (v_ss == null) {
    		logger.warn("Unable to find hit for "+jobId+" - maybe a problem at EBI?");
    		System.err.println(m_result_xml);
    		return 0;
    	}
    	Iterator i_sssr = v_ss.iterator();
    	while (i_sssr.hasNext()) {
    		IXMLElement sssr = (IXMLElement) i_sssr.next();
	    	
	    	IXMLElement hits = sssr.getFirstChildNamed("hits");
	    	Vector each_hit  = hits.getChildrenNamed("hit");
	    	Iterator i = each_hit.iterator();
	    	int n_rows = 0;
	    	while (i.hasNext()) {
	    		IXMLElement hit = (IXMLElement) i.next();
	    		IXMLElement alignments = hit.getFirstChildNamed("alignments");
	    		Vector al = alignments.getChildrenNamed("alignment");
	    		Iterator ia = al.iterator();
	    		
	    		while (ia.hasNext()) {
	    			IXMLElement alignment  = (IXMLElement) ia.next();
	    			IXMLElement score      = alignment.getFirstChildNamed("score");
	    			IXMLElement bits       = alignment.getFirstChildNamed("bits");
	    			IXMLElement eval       = alignment.getFirstChildNamed("expectation");
	    			IXMLElement identities = alignment.getFirstChildNamed("identity");
	    			IXMLElement positives  = alignment.getFirstChildNamed("positives");
	    			IXMLElement query      = alignment.getFirstChildNamed("querySeq");
	    			IXMLElement pattern    = alignment.getFirstChildNamed("pattern");
	    			IXMLElement match      = alignment.getFirstChildNamed("matchSeq");
	    			
	    			DataCell[] cells = new DataCell[13];
	            	for (int k=0; k<cells.length; k++) {
	            		cells[k] = DataType.getMissingCell();
	            	}
	            	
	    			cells[0]             = new StringCell(jobId);
	    			String db   = hit.getAttribute("database", "");
	    			String acsn = hit.getAttribute("ac", "");
	    			String descr= hit.getAttribute("description", "");
	    			
	    			cells[1]             = new StringCell(db);
	    			cells[2]             = new StringCell(acsn);
	    			cells[3]             = new StringCell(descr);
	    			String str_score     = score.getContent();
	    			String str_bits      = bits.getContent();
	    			String str_eval      = eval.getContent();
	    			String str_ident     = identities.getContent();
	    			String str_positives = positives.getContent();
	    			cells[4] = new DoubleCell(new Double(str_score).doubleValue());
	    			cells[5] = new DoubleCell(new Double(str_bits).doubleValue());
	    			cells[6] = new DoubleCell(new Double(str_eval).doubleValue());
	    			cells[7] = new DoubleCell(new Double(str_ident).doubleValue());
	    			cells[8] = new DoubleCell(new Double(str_positives).doubleValue());
	    			cells[9] = new StringCell(query.getContent());
	    			cells[10]= new StringCell(pattern.getContent());
	    			cells[11]= new StringCell(match.getContent());
	    			
	    			String attr = query.getAttribute("start", "") +"," +
	                				query.getAttribute("end", "") + "/" + 
	                				match.getAttribute("start", "") + "," +
	                				match.getAttribute("end", "");
	    			cells[12]= new StringCell(attr);
	    			String rkey = "Row"+rk;
	    			
	     			proc_results.addRowToTable(new JoinedRow(new DefaultRow(rkey, orig_cells), new DefaultRow(rkey, cells)));
	     			
	     			cells = null;
	     			rkey = null;
	     			attr = null;
	     			str_score = null;
	     			str_bits  = null;
	     			str_eval  = null;
	     			str_ident = null;
	     			str_positives = null;
	     			db = null;
	     			acsn = null;
	     			descr=null;
	     			rk++;
	    		}
	    		ia = null;
	    		al = null;
	    	}
	    	i        = null;
	    	rdr      = null;
	    	each_hit = null;
    	}
    	
    	return rk;
    }
    
    /**
     * Returns true if the result format of the hits is XML, false if its text
     * 
     * @param jobId
     * @return
     * @throws Exception
     */
    public boolean getJobResult(String jobId) throws Exception {
    	// retry in case of intermittent failure
    	m_result_png = null;
    	m_result_xml = null;
    	for (int i=0; i<4; i++) {
    		try {
    		    if (isFirstJob()) {
    		       WsResultType[] types = get_proxy().getResultTypes(jobId);
    		       setHasPNG(false);
    		       setHasXML(false);
    		       setHasText(false);
    			   for (WsResultType t : types) {
    				   String id = t.getIdentifier();
    				   if (id.equalsIgnoreCase("xml")) {
    					   setHasPNG(true);
    				   } else if (id.equalsIgnoreCase("out")) {
    					   setHasText(true);
    				   } else if (id.equals("visual-png")) {
    					   setHasPNG(true);
    				   }
    			   }
    		    }
    			
    		    // get the xml or text result if XML is not available
    			byte[] result = get_proxy().getResult(jobId, hasXML() ? "xml" : "out", null); // m_output_format is either 'xml' or 'out' for what this node needs
    			if (result != null) {
    	    		String s = new String(result);
    	    		result = null;
    	    		if (hasXML()) {
    	    			m_result_xml = s;
    	    		} else {
    	    			// for text output we "wrap" it in a fake xml header to ensure it complies with the XML cell...
    	    			m_result_xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<FakeXMLSimilarityResult>\n"+s+"\n</FakeXMLSimilarityResult>";
    	    		}
    	    	}
    			
    			// save the graphical results summary if configured by the user
    			m_result_png = DataType.getMissingCell();
    			if (m_save_image.getBooleanValue()) {
    				if (!hasPNG() && isFirstJob()) {
    					logger.warn("PNG BLAST results not available - perhaps reconfigure the node.");
    				}
    				result = get_proxy().getResult(jobId, "visual-png", null);
    				if (result != null) {
    					m_result_png = new PNGImageContent(result).toImageCell();
    				} 
    			}
    			
 			   setFirstJob(false);
	    	   return hasXML();
    		} catch (IOException ce) {
    			if (m_done_rows < 1)		// an error at the first job usually indicates a parameter problem ie. no retry
    				throw ce;
    			if (i<3) {
    				logger.info(ce.getMessage());
    				logger.warn("Blast getJobResult(): could not connect, retrying in "+((i+1)*500)+" seconds.");
    				Thread.sleep((i+1)*500*1000);
    			}
    			// else fallthru
    		} 
    	}
    	
    	return false;	// if this is executed, something is very wrong so assume text...
    }
    
    private boolean hasPNG() {
		return m_has_png;
	}

	private boolean hasXML() {
		return m_has_xml;
	}

	private void setHasXML(boolean b) {
		m_has_xml = b;
	}

	private void setHasText(boolean b) {
		m_has_text = b;
	}

	private void setHasPNG(boolean b) {
		m_has_png = b;
	}

	private boolean isFirstJob() {
		return m_first_job;
	}

	/**
     * Waits for the entire batch to complete. Since the batch has just been submitted, we wait
     * for at least 60s before checking the first job for completion
     * 
     * @param jobs
     * @throws Exception
     */
    protected void waitForBatchCompletion(String[] jobs) throws Exception {
    	int to_go = jobs.length;		// assume none have completed
    	
    	/* this system produces a lot of objects: XML etc. so keep the garbage collector busy... */
		System.gc();	
		System.runFinalization();
    	for (int i=0; i<12; i++) {   		
    		Thread.sleep(5 * 1000);     // sleep for five seconds and then check for cancel
    		m_exec.checkCanceled();
    	}
    	while (to_go > 0) {
    		waitForCompletion(jobs[jobs.length - to_go]);
    		m_done_rows++;
    		logger.info("Job completed: "+jobs[jobs.length - to_go]);
    		m_exec.checkCanceled();
    		updateProgress();
    		to_go--;
    	}
    	logger.info("Batch completed.");
    }
    
    protected void waitForCompletion(String jobId) throws Exception {
    	if (jobId.length() > 0) {
    		int check_period = 20 * 1000; // every 10s
    		String status = "PENDING";
    		int retry = 0;
    		while (status.equals("PENDING") || status.equals("RUNNING")) {
    			try {
    				logger.info("Waiting for "+jobId);
    				
    				status = get_proxy().getStatus(jobId);
    				if (status.equals("RUNNING") || status.equals("PENDING")) {
    					logger.info(jobId + " " + status + ", sleeping for "+check_period+ " milliseconds");
    					
    					// check ten times each check_period to see if the user pressed cancel
    					for (int i=0; i<10; i++) {
    						Thread.sleep(check_period / 10);
    						m_exec.checkCanceled();
    					}
    					
    					// each time job is still going, we double check_period to reduce likelihood of overloading EBI
    					check_period *= 2;
    					if (check_period > 200000) {
    						check_period = 200000;
    					}
    				}
    				if (status == "FAILED") {
    					logger.error("WU-BLAST job failed: "+jobId);
    				}
    			} catch (IOException e) {
    				if (m_done_rows < 1)		// an error at the first job usually indicates a parameter problem ie. no retry
        				throw e;
    				if (retry < 3) {
    					logger.warn("Unable to check job "+jobId+" retrying (after linear-backoff delay)... ");
    					Thread.sleep(((420 * retry) + 120)* 1000);
    					status = "PENDING";
    					retry++;
    				} else {
    					throw new Exception("Cannot check job "+jobId+" via WU-BLAST (EBI)... aborting"+e);
    				}
    			} 
    		}
    	} else {
    		throw new Exception("Bogus EBI job id... aborting!");
    	}
    }
    
    /**
     * Submits a batch of jobs to EBI and returns the EBI-assigned job-id's to the caller. Returns probably before the jobs complete.
     * @param ip
     * @param d
     * @return
     */
    protected String[] runBatch(ArrayList<InputParameters> ip) throws Exception {
    	assert ip.size() > 0;
    	String[] jobs = new String[ip.size()];
    	for (int i=0; i<ip.size(); i++) {
    		m_exec.checkCanceled(); // stop submitting once cancel chosen by user
    		jobs[i] = runApp(ip.get(i));
    	}
    	return jobs;
    }
    
    /**
     * Submits a single job to EBI. Does not wait for the job to complete before returning to the caller.
     * @param ip
     * @param data
     * @return
     * @throws Exception
     */
    protected String runApp(InputParameters ip) throws Exception {
    	 String jobId = "";
    	 //System.err.println(ip);
    	 //System.err.println(data);
    	 for (int retry = 0; retry < 4; retry++) {
    		 try {
    			 jobId = get_proxy().run(m_email.getStringValue(), "", ip);
    			 if (jobId.length() > 0) {
            		 logger.info("Successfully submitted WU-BLAST job: "+jobId);
            	 }
    			 break;
    		 }
    		 catch (IOException e) {
    			 if (m_done_rows < 1)		// an error at the first job usually indicates a parameter problem ie. no retry
        				throw e;
    			 if (retry == 3) {
    				 throw e;
    			 } else {
    				 logger.warn("Unable to submit job: "+e+ " .... retrying....");
    				 Thread.sleep((420 * retry + 120) * 1000);
    			 }
    		 }
    	 }
    	
    	 //System.err.println(jobId);
    	 return jobId;
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

        return new DataTableSpec[]{null, null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    	 m_db.saveSettingsTo(settings);
    	 m_filter.saveSettingsTo(settings);
    	 m_matrix.saveSettingsTo(settings);
    	 m_program.saveSettingsTo(settings);
    	 m_sensitivity.saveSettingsTo(settings);
    	 m_sortby.saveSettingsTo(settings);
    	 m_stats.saveSettingsTo(settings);
    	 m_seq_col.saveSettingsTo(settings);
    	 m_email.saveSettingsTo(settings);
    	 m_num_alignments.saveSettingsTo(settings);
    	 m_num_scores.saveSettingsTo(settings);
    	 m_eval_threshold.saveSettingsTo(settings);
    	 m_ebi_batch_size.saveSettingsTo(settings);
    	 m_stype.saveSettingsTo(settings);
    	 m_save_image.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	 m_db.loadSettingsFrom(settings);
    	 m_filter.loadSettingsFrom(settings);
    	 m_matrix.loadSettingsFrom(settings);
    	 m_program.loadSettingsFrom(settings);
    	 m_sensitivity.loadSettingsFrom(settings);
    	 m_sortby.loadSettingsFrom(settings);
    	 m_stats.loadSettingsFrom(settings);
    	 m_seq_col.loadSettingsFrom(settings);
    	 m_email.loadSettingsFrom(settings);
    	 m_num_alignments.loadSettingsFrom(settings);
    	 m_num_scores.loadSettingsFrom(settings);
    	 m_eval_threshold.loadSettingsFrom(settings);
    	 m_ebi_batch_size.loadSettingsFrom(settings);
    	 m_stype.loadSettingsFrom(settings);
    	 m_save_image.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	 m_db.validateSettings(settings);
    	 m_filter.validateSettings(settings);
    	 m_matrix.validateSettings(settings);
    	 m_program.validateSettings(settings);
    	 m_sensitivity.validateSettings(settings);
    	 m_sortby.validateSettings(settings);
    	 m_stats.validateSettings(settings);
    	 m_seq_col.validateSettings(settings);
    	 m_email.validateSettings(settings);
    	 m_num_alignments.validateSettings(settings);
    	 m_num_scores.validateSettings(settings);
    	 m_eval_threshold.validateSettings(settings);
    	 m_ebi_batch_size.validateSettings(settings);
    	 m_stype.validateSettings(settings);
    	 m_save_image.validateSettings(settings);
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

