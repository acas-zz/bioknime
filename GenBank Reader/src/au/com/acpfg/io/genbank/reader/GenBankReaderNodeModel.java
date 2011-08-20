package au.com.acpfg.io.genbank.reader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.biojava.bio.seq.Feature.Template;
import org.biojava.bio.seq.io.ParseException;
import org.biojava.bio.seq.io.SymbolTokenization;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.SimpleSymbolList;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojavax.Namespace;
import org.biojavax.RankedCrossRef;
import org.biojavax.RankedDocRef;
import org.biojavax.SimpleNamespace;
import org.biojavax.bio.BioEntryRelationship;
import org.biojavax.bio.seq.RichFeature;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.SimpleRichFeature;
import org.biojavax.bio.seq.io.GenbankFormat;
import org.biojavax.bio.seq.io.RichSeqIOListener;
import org.biojavax.bio.taxa.NCBITaxon;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
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


/**
 * This is the model implementation of GenBankReader.
 * Using BioJava, this node reads the specified files/folder for compressed genbank or .gb files and loads the sequences into a single table along with most of key metadata
 *
 * @author http://www.plantcell.unimelb.edu.au
 */
public class GenBankReaderNodeModel extends NodeModel implements RichSeqIOListener {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(GenBankReaderNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_ISFILE = "folder-or-folder?";
	static final String CFGKEY_FILE   = "filename";
	static final String CFGKEY_FOLDER = "foldername";
	static final String CFGKEY_FEATURES="feature-list";
	static final String CFGKEY_SEQTYPE="sequence-type";
	static final String CFGKEY_TAXONOMY_FILTER="taxonomy-filter-keywords";

	
	private final SettingsModelBoolean m_isfile = new SettingsModelBoolean(CFGKEY_ISFILE, true);
	private final SettingsModelString  m_filename= new SettingsModelString(CFGKEY_FILE, "c:/temp/gb.seq");
	private final SettingsModelString  m_folder  = new SettingsModelString(CFGKEY_FOLDER, "c:/temp");
	private final SettingsModelStringArray m_features = new SettingsModelStringArray(CFGKEY_FEATURES, new String[] { "COMMENT", "ID" });
	private final SettingsModelString  m_seqtype = new SettingsModelString(CFGKEY_SEQTYPE, "DNA");
	private final SettingsModelString  m_taxonomy_filter = new SettingsModelString(CFGKEY_TAXONOMY_FILTER, "Lolium");
	
	// internal state during execute -- not persisted
	private StringBuffer m_symbols;
	private String       m_accsn;
    private StringBuffer m_comments;
    private NCBITaxon    m_taxon;
    private boolean      m_circular;
    private String       m_descr;
    private String       m_seq_version;
    private int          m_entry_version;
    private RichFeature  m_feature;
    private ArrayList<StringCell> m_feature_cells;

    /**
     * Constructor for the node model.
     */
    protected GenBankReaderNodeModel() {
        //  one outgoing port only
        super(0, 1);
        // ensure model is correctly initialised
        m_folder.setEnabled(!m_isfile.getBooleanValue());
        m_filename.setEnabled(m_isfile.getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	
    	ArrayList<File> files_to_read = new ArrayList<File>();
    	File[] scan_files;
    	if (m_isfile.getBooleanValue()) {
    		scan_files = new File[] { new File(m_filename.getStringValue()) };
    	} else {
    		scan_files = new File(m_folder.getStringValue()).listFiles();
    	}
    	for (File f : scan_files) {
    		if (!f.isFile() || !f.canRead() || f.length() < 1) {
    			logger.warn("Skipping inaccessible file: "+f.getName());
    			continue;
    		}
    		String fname = f.getName().toLowerCase();
    		if (fname.startsWith("gbest")) {
    			files_to_read.add(f);
    		} else if (fname.endsWith(".gb") || fname.endsWith(".gb.gz") || fname.endsWith(".gbk") || fname.endsWith(".gbk.gz")) {
    			files_to_read.add(f);
    		} else if (fname.endsWith(".seq.gz") || fname.endsWith(".seq")) {
    			files_to_read.add(f);
    		}
    	}
        logger.info("GenBank Reader: found "+files_to_read.size()+" plausible GenBank data files to load");

        
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[9];
        allColSpecs[0] = 
            new DataColumnSpecCreator("GenBank ID", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("GenBank Sequence", StringCell.TYPE).createSpec();
        allColSpecs[2] = 
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[3] =
        	new DataColumnSpecCreator("NCBI Taxon ID", IntCell.TYPE).createSpec();
        allColSpecs[4] =
        	new DataColumnSpecCreator("Sequence Version", StringCell.TYPE).createSpec();
        allColSpecs[5] =
        	new DataColumnSpecCreator("Entry Version", IntCell.TYPE).createSpec();
        allColSpecs[6] =
        	new DataColumnSpecCreator("Comments", StringCell.TYPE).createSpec();
        allColSpecs[7] = 
        	new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec();
        allColSpecs[8] =
        	new DataColumnSpecCreator("Feature Properties (list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
      
        int done_files = 0;
        int hit = 1;
        
        // setup the match data structure (int[]) for the taxa
        String[] taxa_ids = m_taxonomy_filter.getStringValue().split("\\s+");
        int max = 0;
        ArrayList<Integer> bits_to_set = new ArrayList<Integer>();
        for (String id : taxa_ids) {
        	if (id.trim().length() > 0) {
        		Integer taxa_id = new Integer(id.trim());
        		if (taxa_id.intValue() > max) {
        			max = taxa_id.intValue();
        			bits_to_set.add(taxa_id);
        		}
        	}
        }
        DenseBitVector bv = new DenseBitVector(max+1);
        for (Integer i : bits_to_set) {
        	bv.set(i.intValue());
        }
		boolean has_taxa_filter = (bv.cardinality() > 0);
		int[] final_taxa_ids = new int[(int) bv.cardinality()];
		
		// process the files
		int failed_files = 0;
    	for (File f : files_to_read) {
    		int cnt = 0;
    		int accepted = 0;
    		// here we use the fully qualified type to make it clear which biojava package we want
    		org.biojavax.bio.seq.io.GenbankFormat gbf = new GenbankFormat();
    		InputStream is;
    		SymbolTokenization st = RichSequence.IOTools.getDNAParser();
    		if (m_seqtype.getStringValue().equalsIgnoreCase("RNA")) {
    			st = RichSequence.IOTools.getRNAParser();
    		} else if (m_seqtype.getStringValue().equalsIgnoreCase("Protein")) {
    			st = RichSequence.IOTools.getProteinParser();
    		}
    	    		
    		// make a new stream rather than use one which has been partially read
    		BufferedReader rdr = new BufferedReader(new InputStreamReader(make_input_stream(f)));
    		
    		// the SeqIOListener (this) will setup internal member variables for the loop 
    		// to process...
    		boolean more = true;
    		try {
	    		do {
	    			// setup internal state to ensure missing cells get generated if the entry does not specify it
	    			m_accsn       = null;
	    			m_symbols     = null;
	    			m_taxon       = null;
	    			m_comments    = null;
	    			m_seq_version = null;
	    			m_descr         = null;
	    			m_feature_cells = null;
	    			
	    			// read the next genbank sequence from the input, failing gracefully to handle poor entries well
    				more = gbf.readRichSequence(rdr, st, this, null);
    				cnt++;
	    			
	    			if (has_taxa_filter) {
	    				int t_id = m_taxon.getNCBITaxID();
	    				if (t_id < 0 || t_id >= bv.length()) {
	    					continue;
	    				}
	    				
	    				if (!bv.get(t_id)) {
	    						continue;
	    				}
	    			}
	    			
	    			// add the row to the table, since it has passed the taxonomy filter (if any)
	    			DataCell[] cells = new DataCell[9];
	    			cells[0] = safe_cell(m_accsn);
	    			cells[1] = safe_cell(m_symbols);
	    			cells[2] = new StringCell(f.getName());
	    			
	    			cells[3] = (m_taxon != null) ? new IntCell(m_taxon.getNCBITaxID()) : DataType.getMissingCell();
	    			cells[4] = safe_cell(m_seq_version);
	    			cells[5] = new IntCell(m_entry_version);
	    			cells[6] = safe_cell(m_comments);
	    			cells[7] = safe_cell(m_descr);
	    			cells[8] = safe_cell(m_feature_cells);
	    			
	    			accepted++;
	    			container.addRowToTable(new DefaultRow("GB"+hit, cells));
	    			hit++;
	    			if (hit % 200 == 0) {
	    				exec.checkCanceled();
	    			}
	    		} while (more);
	    		logger.info("Processed "+cnt+" genbank entries (accepted "+accepted+") in "+f.getName());

    		} catch (Exception e) {
    			failed_files++;
    			logger.warn("Error in genbank record in "+f.getName()+" error msg is: ");
    			logger.warn(e.getMessage());
    			e.printStackTrace();
    		}
    		rdr.close();
    		
    		done_files++;
    		exec.checkCanceled();
    		exec.setProgress(((double) done_files) / files_to_read.size());
    	}
    	
    	logger.info("Processed "+done_files+" files ("+failed_files+" contained errors). Loading complete.");
    	
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    /**
     * Responsible for ensuring a valid DataCell is returned (use a missing cell if <code>str</code> is not valid)
     * @param str
     * @return a valid KNIME data cell (NOT guaranteed to be a StringCell)
     */
    protected DataCell safe_cell(String str) {
		return (str != null) ? new StringCell(str) : DataType.getMissingCell();
	}

    /**
     * Responsible for ensuring a valid DataCell is returned (use a missing cell if <code>str</code> is not valid)
     * @param str
     * @return a valid KNIME data cell (NOT guaranteed to be a StringCell)
     */
    protected DataCell safe_cell(StringBuffer str) {
    	if (str == null)
    		return DataType.getMissingCell();
    	return safe_cell(str.toString());
    }
    
    /**
     * Responsible for ensuring a valid collection cell is returned
     */
    protected DataCell safe_cell(List<StringCell> cells) {
    	if (cells == null || cells.size() < 1) 
    		return DataType.getMissingCell();
    	
    	return CollectionCellFactory.createListCell(cells);
    }
    
	/**
     * Returns an inputstream object, depending on whether the file is likely compressed or not
     * 
     * @param f
     * @return
     * @throws IOException
     */
    private InputStream make_input_stream(File f) throws IOException {
    	boolean is_compressed = f.getName().endsWith(".gz");

		if (is_compressed) {
	        	   return new GZIPInputStream(new FileInputStream(f));
	    } else {
	        	   return new FileInputStream(f);
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
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_isfile.saveSettingsTo(settings);
    	m_folder.saveSettingsTo(settings);
    	m_filename.saveSettingsTo(settings);
    	m_features.saveSettingsTo(settings);
    	m_seqtype.saveSettingsTo(settings);
    	m_taxonomy_filter.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_isfile.loadSettingsFrom(settings);
    	m_folder.loadSettingsFrom(settings);
    	m_filename.loadSettingsFrom(settings);
    	m_features.loadSettingsFrom(settings);
    	m_seqtype.loadSettingsFrom(settings);
    	m_taxonomy_filter.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_isfile.validateSettings(settings);
    	m_folder.validateSettings(settings);
    	m_filename.validateSettings(settings);
    	m_features.validateSettings(settings);
    	m_seqtype.validateSettings(settings);
    	m_taxonomy_filter.validateSettings(settings);
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

    /**
     *  **************************** RichSeqIOListener methods ****************************
     *  
     */
	@Override
	public void addFeatureProperty(Object arg0, Object arg1)
			throws ParseException {
		if (m_feature_cells == null) {
			m_feature_cells = new ArrayList<StringCell>();
		}
		if (arg0 != null && arg1 != null) {
			String key = arg0.toString();
			int colon_idx = key.indexOf(':');		// remove namespace prefix from key (not for users!)
			if (colon_idx >= 0) {
				key = key.substring(colon_idx+1);
			}
			m_feature_cells.add(new StringCell(key+"="+arg1.toString()));
		}
	}

	@Override
	public void addSequenceProperty(Object key, Object value)
			throws ParseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addSymbols(Alphabet alphabet, Symbol[] symbols, int start, int len)
			throws IllegalAlphabetException {
		assert(start < len && start >= 0);
		assert(alphabet != null && symbols != null);
		
		SymbolList sl;
		if (start == 0) {
			sl = new SimpleSymbolList(symbols, len, alphabet);
		} else {
			Symbol[] new_list = new Symbol[len-start];
			System.arraycopy(symbols, start, new_list, 0, len-start);
			sl = new SimpleSymbolList(new_list, new_list.length, alphabet);
		}
		m_symbols.append(sl.seqString().toUpperCase());
	}

	@Override
	public void endFeature() throws ParseException {	
	}

	@Override
	public void endSequence() throws ParseException {
	}

	@Override
	public void setName(String arg0) throws ParseException {	
	}

	@Override
	public void startFeature(Template arg0) throws ParseException {	
		 m_feature = RichFeature.Tools.makeEmptyFeature();	
	}

	@Override
	public void startSequence() throws ParseException {
		m_symbols = new StringBuffer(1024);
	}

	@Override
	public RichFeature getCurrentFeature() throws ParseException {
		return m_feature;
	}

	@Override
	public void setAccession(String arg0) throws ParseException {
		m_accsn = arg0;
	}

	@Override
	public void setCircular(boolean arg0) throws ParseException {
		m_circular = arg0;
	}

	@Override
	public void setComment(String arg0) throws ParseException {
		if (m_comments == null) {
			m_comments = new StringBuffer(1024);
		}
		m_comments.append(arg0+"\n");
	}

	@Override
	public void setDescription(String arg0) throws ParseException {
		m_descr = arg0;
	}

	@Override
	public void setDivision(String arg0) throws ParseException {	
	}

	@Override
	public void setIdentifier(String arg0) throws ParseException {	
	}

	@Override
	public void setNamespace(Namespace arg0) throws ParseException {	
	}

	@Override
	public void setRankedCrossRef(RankedCrossRef arg0) throws ParseException {
	}

	@Override
	public void setRankedDocRef(RankedDocRef arg0) throws ParseException {
	}

	@Override
	public void setRelationship(BioEntryRelationship arg0)
			throws ParseException {		
	}

	@Override
	public void setSeqVersion(String arg0) throws ParseException {
		m_seq_version = arg0;
	}

	@Override
	public void setTaxon(NCBITaxon arg0) throws ParseException {
		m_taxon = arg0;
	}

	@Override
	public void setURI(String arg0) throws ParseException {		
	}

	@Override
	public void setVersion(int arg0) throws ParseException {
		m_entry_version = arg0;
	}

}

