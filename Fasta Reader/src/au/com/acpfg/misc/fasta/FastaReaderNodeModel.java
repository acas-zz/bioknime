package au.com.acpfg.misc.fasta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of FastaReader.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: 
 * * n1) Accession 
 * * n2) Description - often not accurate in practice 
 * * n3) Sequence data * n * n
 * Neither line breaks or leading/trailing whitespace are preserved.
 *
 * @author Andrew Cassin
 */
public class FastaReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(FastaReaderNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_FASTA    = "fasta-file";
    static final String CFGKEY_ACCSN_RE = "accsn-regexp";
    static final String CFGKEY_DESCR_RE = "description-regexp";
    static final String CFGKEY_ENTRY_HANDLER = "entry-handler";
    static final String CFGKEY_FASTADIR = "fasta-dir";
    static final String CFGKEY_ISDIR    = "read-entire-directory";
    static final String CFGKEY_MAKESTATS= "make-statistics?";
    
    /** initial sequence file */
    private static final String DEFAULT_FASTA    = "/tmp/sequences.fasta";
    private static final String DEFAULT_ACCSN_RE = "^(\\S+)\\b";
    private static final String DEFAULT_DESCR_RE = "^\\S+\\s*(.*)$";
    private static final String DEFAULT_ENTRY_HANDLER = "single";
    private static final String DEFAULT_FASTADIR = "c:/temp";
    private static final Boolean DEFAULT_ISDIR = Boolean.FALSE;
    private static final Boolean DEFAULT_MAKESTATS = Boolean.FALSE;	// dont waste memory and performance by default

    // settings for this node: regular expressions to process the ">" lines, and the fasta sequence filename
    private final SettingsModelString m_fasta    = make(CFGKEY_FASTA);
    private final SettingsModelString m_accsn_re = make(CFGKEY_ACCSN_RE);
    private final SettingsModelString m_descr_re = make(CFGKEY_DESCR_RE);
    private final SettingsModelString m_entry_handler = make(CFGKEY_ENTRY_HANDLER);
    private final SettingsModelString m_fastadir = make(CFGKEY_FASTADIR);
    private final SettingsModelBoolean m_isdir   = new SettingsModelBoolean(CFGKEY_ISDIR, DEFAULT_ISDIR);
    private final SettingsModelBoolean m_stats   = new SettingsModelBoolean(CFGKEY_MAKESTATS, DEFAULT_MAKESTATS);
    

    /**
     * Constructor for the node model.
     */
    protected FastaReaderNodeModel() {
        super(0, 2); // output ports only
    }

    public static SettingsModelString make(String k) {
    	if (k.equals(CFGKEY_FASTA)) {
    		return new SettingsModelString(k, DEFAULT_FASTA);
    	} else if (k.equals(CFGKEY_ACCSN_RE)) {
    		return new SettingsModelString(k, DEFAULT_ACCSN_RE);
    	} else if (k.equals(CFGKEY_DESCR_RE)) {
    		return new SettingsModelString(k, DEFAULT_DESCR_RE);
    	} else if (k.equals(CFGKEY_ENTRY_HANDLER)) {
    		return new SettingsModelString(k, DEFAULT_ENTRY_HANDLER);
    	} else if (k.equals(CFGKEY_FASTADIR)) {
    		SettingsModelString sms = new SettingsModelString(k, DEFAULT_FASTADIR);
    		sms.setEnabled(false);	// since default is for single file load
    		return sms;
    	}
    	return null;
    }
    
    protected DataTableSpec make_output_spec(boolean as_single) {   
        // 1. create the column specification in accordance with the as_single parameter
        DataColumnSpec[] allColSpecs = new DataColumnSpec[4];
        DataType dt = as_single ? StringCell.TYPE : ListCell.getCollectionType(StringCell.TYPE);
        allColSpecs[0] = 
            new DataColumnSpecCreator("Accession", dt).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Description", dt).createSpec();
        allColSpecs[2] = 
            new DataColumnSpecCreator("Sequence", StringCell.TYPE).createSpec();
        allColSpecs[3] =
        	new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        return outputSpec;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	boolean as_single        = m_entry_handler.getStringValue().equals("single");
         
    	DataTableSpec outputSpec = make_output_spec(as_single);
    	DataTableSpec statSpec   = SequenceStatistics.getOutputSpec();
    	
    	ArrayList<String> filenames = new ArrayList<String>();
    	if (!m_isdir.getBooleanValue()) {
    		filenames.add(m_fasta.getStringValue());
    	} else {
    		File[] list = new File(m_fastadir.getStringValue()).listFiles(new FileFilter() {

				@Override
				public boolean accept(File arg0) {
					String fname = arg0.getName().toLowerCase();
					if (fname.endsWith(".fa") ||
						fname.endsWith(".fasta") ||
						fname.endsWith(".txt") ||
						fname.endsWith(".seq") ||
						fname.endsWith(".fa.gz") ||
						fname.endsWith(".fsa.gz") ||
						fname.endsWith(".fsa") ||
						fname.endsWith(".fna") || 
						fname.endsWith(".fna.gz") ||
						fname.endsWith(".fasta.gz") ||
						fname.endsWith(".txt.gz") ||
						fname.endsWith(".seq.gz") ||
						fname.endsWith(".fa.z") ||
						fname.endsWith(".fasta.z") ||
						fname.endsWith(".txt.z") ||
						fname.endsWith(".seq.z"))  {
    					return true;
    				}
					return false;
				}
    			
    		});
    		for (File f : list) {
    			filenames.add(f.getAbsolutePath());
    		}
    	}
    	
    	if (filenames.size() < 1) {
    		throw new InvalidSettingsException("No files to process!");
    	} else {
    		logger.info("Found "+filenames.size()+" FASTA files to process.");
    	}
      
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        BufferedDataContainer statsContainer = exec.createDataContainer(statSpec);
        
        long n_seq   = 0;
        long n_seq_rej = 0;
        Pattern accsn_matcher = Pattern.compile(m_accsn_re.getStringValue());
        Pattern descr_matcher = Pattern.compile(m_descr_re.getStringValue());
       
        // let's add sequences from input file(s) into the output port
        String line  = null;
        String[] accsn = null;
        String[] descr = null;
        StringBuffer seq = null;
      
        int files_done = 0;
        for (String fname : filenames) {
           logger.info("Processing FASTA file: "+fname);
           
           File input_sequences     = new File(fname);
           SequenceStatistics stats = m_stats.getBooleanValue() ? new SequenceStatistics(input_sequences) : null;
           boolean is_compressed    = false;
           if (fname.toLowerCase().endsWith(".gz") || 
        		   fname.toLowerCase().endsWith(".z")) {
        	   is_compressed = true;
           }
           double portion           = 1.0 / filenames.size();
    	   double p_size            = input_sequences.length();
           if (p_size < 1) {
            	logger.warn("Empty file: "+fname+", ignored.");
            	files_done++;
            	continue;
           }
           BufferedReader rseq;
           InputStream is = null;
           
           if (is_compressed) {
        	   is = new GZIPInputStream(new FileInputStream(input_sequences), 16*1024);
           } else {
        	   is = new FileInputStream(input_sequences);
           }
    	   rseq = new BufferedReader(new InputStreamReader(is));

           fname = input_sequences.getName();
           boolean done = false;
           boolean already_got_header = false;
           
	       while (!done) {
	    	   
	    	    // get header line
	    	    if (!already_got_header) {
		    	    do {
		    	    	line = rseq.readLine();
		    	    	if (line == null) {
		    	    		done = true;
		    	    		break;
		    	    	}
		    	    } while (!line.startsWith(">"));
	    	    }
	    	    
	    	    if (!done) {
	    	    	  String[] entries = line.split("\\x01");
		              if (entries.length > 0 && entries[0].startsWith(">")) {
		                	entries[0] = entries[0].substring(1);	// skip over > for parse_accession()
		              }
		              accsn = parse_accession(accsn_matcher,entries);
		              descr = parse_description(descr_matcher,entries);
		              String tline;
		              seq = new StringBuffer(10 * 1024);
		              boolean got_seq = false;
		              already_got_header = false;
		              int tline_len = 0;
		              do {
		            	  if ((line = rseq.readLine()) == null) {
		            		  already_got_header = false;
		            		  break;
		            	  }
		            	  tline         = line.trim();
		            	  tline_len     = tline.length();
		            	  if (tline_len > 0) {
			            	  char first_c  = tline.charAt(0);
			            	  if (first_c == '>') {
			            		  got_seq = false;
			            		  already_got_header = true;
			            		  break;
			            	  } 
			            	  
			            	  if (Character.isLetter(first_c) || first_c == '*' || first_c == '-') {
			            		  seq.append(tline);
			            		  got_seq = true;
			            	  }
		            	  }
		              } while (tline_len == 0 || got_seq );
	    	    }
	            
	    	    // save the sequence to the container
	    	    
	    	    if (!done) {
		    	    DataCell c1 = as_single ? new StringCell(accsn[0]) : CollectionCellFactory.createListCell(toDataCells(accsn));
		    	    DataCell c2 = as_single ? new StringCell(descr[0]) : CollectionCellFactory.createListCell(toDataCells(descr));
		    	    if (save_sequence(container, n_seq, c1, c2, seq, fname, stats)) {
		                	n_seq++;
		                	accsn = null; // help java garbage collector
		                	descr = null;
		            }
	    	    }
	          
	            if (n_seq % 1000 == 0) {
	            	try {
		            	// check if the execution monitor was canceled
		                exec.checkCanceled();
	            	} catch (CanceledExecutionException ce) {
	            		rseq.close();		// avoid open file leak
	            		throw ce;
	            	}
	                // and update node progress "traffic light"
	                double tmp = (((double)files_done)/filenames.size())*portion+((((double)0)/p_size)*portion);
	                exec.setProgress(tmp, "Adding " + n_seq+" from "+fname);
	            }
	        }
	      
	        rseq.close();
	        if (stats != null) {
	        	stats.addStats(statsContainer);
	        }
	        files_done++;
        }
        
        // once we are done, we close the container and return its table
        container.close();
        statsContainer.close();
        
        BufferedDataTable out = container.getTable();
        BufferedDataTable statsTable = statsContainer.getTable();
        
        logger.info("Matched "+n_seq+ " sequences, failed to match "+n_seq_rej+" sequences.");
        return new BufferedDataTable[]{out, statsTable};
    }
    
    protected boolean save_sequence(BufferedDataContainer container, long n_seq, DataCell c1, DataCell c2,
    		StringBuffer seq, String fname, SequenceStatistics stats)
    {
    	if (c1 != null && c2 != null && seq != null) {
  		    RowKey key = new RowKey("Seq" + n_seq);
            // the cells of the current row, the types of the cells must match
            // the column spec (see above)
            DataCell[] cells = new DataCell[4];
        	cells[0]         = c1;
        	cells[1]         = c2;
        
            String str       = seq.toString();
            cells[2]         = new StringCell(str);
            cells[3]         = new StringCell(fname);
            DataRow      row = new DefaultRow(key, cells);
            container.addRowToTable(row);
            if (stats != null) {
            	stats.grokSequence(str);
            }
            return true;
  	    } else {
  	    	// NB: do not update stats object if bogus parameters...
  	    	return false;
  	    }
    }
    
    
    protected Collection<StringCell> toDataCells(String[] vec) {
    	ArrayList<StringCell> al = new ArrayList<StringCell>();
    	for (String s : vec) {
    		if (s == null)		// terminate add early if only a few entries valid
    			break;
    		al.add(new StringCell(s));
    	}
    	return al;
    }
    
    protected String[] parse_accession(Pattern matcher, String[] entries) throws Exception {
    	int cnt = 0;
    	String[] accsns = new String[entries.length];
    	for (String entry : entries) {
    		Matcher m = matcher.matcher(entry);
	    	if (m.find()) {
	    		if (m.groupCount() != 1) {
	    			throw new Exception("You must use capturing parentheses () to match an accession only once!");
	    		}
	    		accsns[cnt] = m.group(1);
	    		cnt++;
	    	} 
    	}
    	if (cnt < entries.length) {
    		accsns[cnt] = null; // make sure array has null after last match
    	}
     	return (cnt > 0) ? accsns : null;
    }
    
    protected String[] parse_description(Pattern matcher, String[] entries) throws Exception {
    	int cnt = 0;
    	String[] descrs = new String[entries.length];
    	for (String entry : entries) {
    		Matcher m = matcher.matcher(entry);
    		if (m.find()) {
    			if (m.groupCount() != 1) {
        			throw new Exception("You must use capturing parentheses() to match a sequence description only once!");
        		}
    			descrs[cnt] = m.group(1);
    			cnt++;
    		}
    	}
    	if (cnt < entries.length) {
    		descrs[cnt] = null;
    	}
    	return (cnt > 0) ? descrs : null;
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
    	boolean as_single = m_entry_handler.getStringValue().equals("single");
        DataTableSpec out = make_output_spec(as_single);
        DataTableSpec out2= SequenceStatistics.getOutputSpec();
        return new DataTableSpec[] {out, out2};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fasta.saveSettingsTo(settings);
        m_accsn_re.saveSettingsTo(settings);
        m_descr_re.saveSettingsTo(settings);
        m_entry_handler.saveSettingsTo(settings);
        m_fastadir.saveSettingsTo(settings);
        m_isdir.saveSettingsTo(settings);
        m_stats.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fasta.loadSettingsFrom(settings);
        m_accsn_re.loadSettingsFrom(settings);
        m_descr_re.loadSettingsFrom(settings);
        m_entry_handler.loadSettingsFrom(settings);
        m_fastadir.loadSettingsFrom(settings);
        m_isdir.loadSettingsFrom(settings);
        if (settings.containsKey(CFGKEY_MAKESTATS)) {
        	m_stats.loadSettingsFrom(settings);
        } else {
        	m_stats.setBooleanValue(Boolean.FALSE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {    
        m_fasta.validateSettings(settings);
        m_accsn_re.validateSettings(settings);
        m_descr_re.validateSettings(settings);
        m_entry_handler.validateSettings(settings);
        m_fastadir.validateSettings(settings);
        m_isdir.validateSettings(settings);
        if (settings.containsKey(CFGKEY_MAKESTATS)) {
        	m_stats.validateSettings(settings);
        }
    }
   
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
   
    }
    
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
    
    }
}

