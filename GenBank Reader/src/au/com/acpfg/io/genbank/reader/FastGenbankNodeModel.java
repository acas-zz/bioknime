package au.com.acpfg.io.genbank.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Non-biojava based implementation which should be much faster and resilient to poor annotation
 * The factory class now instantiates this node model rather than the biojava-based one, but changing
 * the factory should be possible in future.
 * 
 * @author andrew.cassin
 *
 */
public class FastGenbankNodeModel extends NodeModel implements GenbankFeatureListener {
	// number of columns in first output port
	private final static int  NCOLS_PORT0 = 10;		// summary data output port
	private final static int  NCOLS_PORT1 = 10;		// source properties eg. /organism
	private final static int  NCOLS_PORT2 = 8;		// cds properties eg. proteins thought coded by a given gene
	
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(FastGenbankNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_ISFILE = "folder-or-folder?";
	static final String CFGKEY_FILE   = "filename";
	static final String CFGKEY_FOLDER = "foldername";
	static final String CFGKEY_TAXONOMY_FILTER ="taxonomy-filter-keywords";
	static final String CFGKEY_SOURCE_FEATURES = "output-source-features?";
	static final String CFGKEY_CDS_FEATURES    = "output-cgs-features?";
	static final String CFGKEY_FILENAME_FILTER = "filename-filter-keywords";
	
	private final SettingsModelBoolean m_isfile = new SettingsModelBoolean(CFGKEY_ISFILE, true);
	private final SettingsModelString  m_filename= new SettingsModelString(CFGKEY_FILE, "c:/temp/gb.seq");
	private final SettingsModelString  m_folder  = new SettingsModelString(CFGKEY_FOLDER, "c:/temp");
	private final SettingsModelString  m_taxonomy_filter = new SettingsModelString(CFGKEY_TAXONOMY_FILTER, "Lolium");
	private final SettingsModelBoolean m_source_features = new SettingsModelBoolean(CFGKEY_SOURCE_FEATURES, true);
	private final SettingsModelBoolean m_cds_features    = new SettingsModelBoolean(CFGKEY_CDS_FEATURES, true);
	private final SettingsModelString  m_fname_filter    = new SettingsModelString(CFGKEY_FILENAME_FILTER, "");
	
	// HACK: as the GenbankFeatureListener is called before the taxonomy filter is applied, we must save the rows temporarily... yuk!
	private Vector<DefaultRow> m_container2_rows;
	private Vector<DefaultRow> m_container3_rows;
	private static int src_id;
	private static int cds_id;
	private final static Pattern feature_match = Pattern.compile("\\s/(\\w+)=\"([^\"]+?)\"\\s*$", Pattern.MULTILINE | Pattern.DOTALL);

	
	protected FastGenbankNodeModel() {
		super(0, 3);
		m_filename.setEnabled(m_isfile.getBooleanValue());
		m_folder.setEnabled(!m_isfile.getBooleanValue());
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

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
     * Similar to <code>safe_cell</code> but this guards against a non-existant map as well
     */
    protected DataCell safe_feature(Map<String, String> map, String key) {
    	if (map == null)
    		return DataType.getMissingCell();
    	String val = map.get(key);
    	if (val == null)
    		return DataType.getMissingCell();
    	return new StringCell(val);
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
    
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_cols();
    }
    
    protected DataTableSpec[] make_output_cols() {
    	DataTableSpec[] out_tables = new DataTableSpec[3];
    	   // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[NCOLS_PORT0];
        allColSpecs[0] = 
            new DataColumnSpecCreator("GenBank Locus Name", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("GenBank Sequence", StringCell.TYPE).createSpec();
        allColSpecs[2] = 
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[3] =
        	new DataColumnSpecCreator("Molecule Type", StringCell.TYPE).createSpec();
        allColSpecs[4] =
        	new DataColumnSpecCreator("Entry Last Modified Date", StringCell.TYPE).createSpec();
        allColSpecs[5] =
        	new DataColumnSpecCreator("Entry Version", StringCell.TYPE).createSpec();
        allColSpecs[6] =
        	new DataColumnSpecCreator("Comments", StringCell.TYPE).createSpec();
        allColSpecs[7] = 
        	new DataColumnSpecCreator("Accession", StringCell.TYPE).createSpec();
        allColSpecs[8] =
        	new DataColumnSpecCreator("Definition", StringCell.TYPE).createSpec();
        allColSpecs[9] = 
        	new DataColumnSpecCreator("NCBI Taxonomy (& lineage)", StringCell.TYPE).createSpec();
        
        out_tables[0] = new DataTableSpec(allColSpecs);
        DataColumnSpec[] allFeatureColSpecs = new DataColumnSpec[NCOLS_PORT1];
        allFeatureColSpecs[0] = 
        	new DataColumnSpecCreator("Accession", StringCell.TYPE).createSpec();
        allFeatureColSpecs[1] =
        	new DataColumnSpecCreator("Organism", StringCell.TYPE).createSpec();
        allFeatureColSpecs[2] =
        	new DataColumnSpecCreator("Molecule Type", StringCell.TYPE).createSpec();
        allFeatureColSpecs[3] =
        	new DataColumnSpecCreator("Strain", StringCell.TYPE).createSpec();
        allFeatureColSpecs[4] =
        	new DataColumnSpecCreator("Database Xref", StringCell.TYPE).createSpec();
        allFeatureColSpecs[5] =
        	new DataColumnSpecCreator("Clone ID", StringCell.TYPE).createSpec();
        allFeatureColSpecs[6] =
        	new DataColumnSpecCreator("Tissue Type", StringCell.TYPE).createSpec();
        allFeatureColSpecs[7] = 
        	new DataColumnSpecCreator("Development Stage", StringCell.TYPE).createSpec();
        allFeatureColSpecs[8] =
        	new DataColumnSpecCreator("Clone Library",StringCell.TYPE).createSpec();
        allFeatureColSpecs[9] =
        	new DataColumnSpecCreator("Note", StringCell.TYPE).createSpec();
        out_tables[1] = new DataTableSpec(allFeatureColSpecs);
        
        DataColumnSpec[] allCodingColSpecs = new DataColumnSpec[NCOLS_PORT2];
        allCodingColSpecs[0] =
        	new DataColumnSpecCreator("Accession", StringCell.TYPE).createSpec();
        allCodingColSpecs[1] = 
        	new DataColumnSpecCreator("Gene", StringCell.TYPE).createSpec();
        allCodingColSpecs[2] = 
        	new DataColumnSpecCreator("Product", StringCell.TYPE).createSpec();
        allCodingColSpecs[3] = 
        	new DataColumnSpecCreator("Database Xref", StringCell.TYPE).createSpec();
        allCodingColSpecs[4] = 
        	new DataColumnSpecCreator("Translation", StringCell.TYPE).createSpec();
        allCodingColSpecs[5] = 
        	new DataColumnSpecCreator("Note", StringCell.TYPE).createSpec();
        allCodingColSpecs[6] =
        	new DataColumnSpecCreator("Protein ID", StringCell.TYPE).createSpec();
        allCodingColSpecs[7] =
        	new DataColumnSpecCreator("Function", StringCell.TYPE).createSpec();
        out_tables[2] = new DataTableSpec(allCodingColSpecs);
        
    	return  out_tables;
    }
    
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
    	
    	boolean has_filename_filter= (m_fname_filter.getStringValue().trim().length() > 0);
    	String[] filename_keywords = m_fname_filter.getStringValue().split("\\s+");
    	for (File f : scan_files) {
    		if (!f.isFile() || !f.canRead() || f.length() < 1) {
    			logger.info("Skipping inaccessible file: "+f.getName());
    			continue;
    		}
    		String fname = f.getName().toLowerCase();
    		if (has_filename_filter) {
    			boolean found = false;
	    		for (String keyword : filename_keywords) {
	    			if (fname.indexOf(keyword.toLowerCase()) >= 0) {
	    				found = true;
	    				break;
	    			}
	    		}
	    		if (!found) {
	    			logger.info("Filename "+fname+" does not meet filename filter... ignoring");
	    			continue;
	    		}
	    		files_to_read.add(f);
    		} else {	// no filename filter specified... assume likely genbank files (uncompressed or gzip'ed)
	    		if (fname.startsWith("gbest")) {
	    			files_to_read.add(f);
	    		} else if (fname.endsWith(".gb") || fname.endsWith(".gb.gz") || fname.endsWith(".gbk") || fname.endsWith(".gbk.gz")) {
	    			files_to_read.add(f);
	    		} else if (fname.endsWith(".seq.gz") || fname.endsWith(".seq")) {
	    			files_to_read.add(f);
	    		}
    		}
    	}
        logger.info("GenBank Reader: found "+files_to_read.size()+" plausible GenBank data files to load");

        
        DataTableSpec[] out_tables = make_output_cols();
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        final BufferedDataContainer container = exec.createDataContainer(out_tables[0]);
        final BufferedDataContainer container2= exec.createDataContainer(out_tables[1]);
        final BufferedDataContainer container3= exec.createDataContainer(out_tables[2]);
        
        int done_files = 0;
        int hit = 1;
        src_id  = 1;
        cds_id  = 1;
        
        // setup the match data structure (int[]) for the taxa
        String[] taxa = m_taxonomy_filter.getStringValue().split("\\s+");
		boolean has_taxa_filter = false;
		for (String t : taxa) {
			if (t.length() > 0) {
				has_taxa_filter = true;
				break;
			}
		}
		
		// process the files
		int failed_files = 0;
    	for (File f : files_to_read) {
    		int cnt = 0;
    		int accepted = 0;
    		
    	    		
    		// make a new stream rather than use one which has been partially read
    		BufferedReader rdr = null;
    		
    		try {
    			rdr = new BufferedReader(new InputStreamReader(make_input_stream(f)));
    		
    			String line;
    			while ((line = rdr.readLine()) != null) {
    				if (!line.startsWith("LOCUS")) {
    					continue;
    				}
        			StringBuffer rec = new StringBuffer(10*1024);
					rec.append(line);
					rec.append('\n');
					while ((line = rdr.readLine()) != null) {
						rec.append(line);
						rec.append('\n');
						if (line.startsWith("//")) {
							cnt++;
							break;
						}
					}	    					
					
					m_container2_rows = null;
					m_container3_rows = null;
					GenbankRecord gbr = new GenbankRecord(rec, this);
					// HACK: for now we only support filtering by organism or lineage... 
					if (has_taxa_filter) {
	    				boolean found = false;
	    				String lineage = gbr.get_taxonomy();
	    				for (String term : taxa) {
	    					if (term.length() < 1)
	    						continue;
	    					if (lineage.toLowerCase().indexOf(term.toLowerCase()) >= 0) {
	    						found = true;
	    						break;
	    					}
	    				}
	    				if (!found)
	    					continue;
	    			}
										
					// add the row to the first output port, since it has passed the taxonomy filter (if any)
	    			DataCell[] cells = new DataCell[NCOLS_PORT0];
	    			cells[0] = safe_cell(gbr.get_locus_name());
	    			cells[1] = safe_cell(gbr.get_filtered_sequence());
	    			cells[2] = new StringCell(f.getName());
	    			
	    			cells[3] = safe_cell(gbr.get_molecule_type());
	    			cells[4] = safe_cell(gbr.get_last_modified());
	    			cells[5] = safe_cell(gbr.get_version());
	    			cells[6] = safe_cell(gbr.get_comment());
	    			cells[7] = safe_cell(gbr.get_accession());
	    			cells[8] = safe_cell(gbr.get_definition());
	    			cells[9] = safe_cell(gbr.get_taxonomy());
	    			
	    			accepted++;
	    			String row_key = "GB"+hit;
	    			container.addRowToTable(new DefaultRow(row_key, cells));
	    			
	    			// add the cells to the feature output ports
	    			if (m_container2_rows != null) {
		    			for (DefaultRow r : m_container2_rows) {
		    				container2.addRowToTable(r);
		    			}
	    			}
	    			if (m_container3_rows != null) {
		    			for (DefaultRow r : m_container3_rows) {
		    				container3.addRowToTable(r);
		    			}
	    			}
	    			
	    			hit++;
	    			if (hit % 200 == 0) {
	    				exec.checkCanceled();
	    			}
	    			
					// TODO...
					if (line == null)
						break;
    			}
        		rdr.close();

	    		logger.info("Processed "+cnt+" genbank entries (accepted "+accepted+") in "+f.getName());
    		} catch (Exception e) {
    			if (rdr != null)
    				rdr.close();
    			failed_files++;
    			logger.warn("Error in genbank record in "+f.getName()+" error msg is: ");
    			logger.warn(e.getMessage());
    			e.printStackTrace();
    		}
    		
    		done_files++;
    		exec.checkCanceled();
    		exec.setProgress(((double) done_files) / files_to_read.size());
    	}
    	
    	logger.info("Processed "+done_files+" files ("+failed_files+" contained errors). Loading complete.");
    	
        // once we are done, we close the container and return its table
        container.close();
        container2.close();
        container3.close();
        BufferedDataTable out = container.getTable();
        BufferedDataTable out2= container2.getTable();
        BufferedDataTable out3= container3.getTable();
        return new BufferedDataTable[]{out, out2, out3};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_isfile.saveSettingsTo(settings);
    	m_folder.saveSettingsTo(settings);
    	m_filename.saveSettingsTo(settings);
    	m_taxonomy_filter.saveSettingsTo(settings);
    	m_source_features.saveSettingsTo(settings);
    	m_cds_features.saveSettingsTo(settings);
    	m_fname_filter.saveSettingsTo(settings);
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
    	m_taxonomy_filter.loadSettingsFrom(settings);
    	m_source_features.loadSettingsFrom(settings);
    	m_cds_features.loadSettingsFrom(settings);
    	m_fname_filter.loadSettingsFrom(settings);
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
    	m_taxonomy_filter.validateSettings(settings);
    	m_source_features.validateSettings(settings);
    	m_cds_features.validateSettings(settings);
    	m_fname_filter.validateSettings(settings);
    }

	@Override
	protected void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parse_section(String title, String accsn, String content)
			throws InvalidGenbankRecordException {

		if (title.equals("source") && m_source_features.getBooleanValue()) {
			if (m_container2_rows == null) 
				m_container2_rows = new Vector<DefaultRow>();
			HashMap<String,String> feature_properties = new HashMap<String,String>();
			//TODO... do something with feature position?
			Matcher m = feature_match.matcher(content);
			while (m.find()) {
				feature_properties.put(m.group(1).toLowerCase(), m.group(2));
			}
			DataCell[] cells = new DataCell[NCOLS_PORT1];
			cells[0] = safe_cell(accsn);
			cells[1] = safe_feature(feature_properties, "organism");
			cells[2] = safe_feature(feature_properties, "mol_type");
			cells[3] = safe_feature(feature_properties, "strain");
			cells[4] = safe_feature(feature_properties, "db_xref");
			cells[5] = safe_feature(feature_properties, "clone");
			cells[6] = safe_feature(feature_properties, "tissue_type");
			cells[7] = safe_feature(feature_properties, "dev_stage");
			cells[8] = safe_feature(feature_properties, "clone_lib");
			cells[9] = safe_feature(feature_properties, "note");
			m_container2_rows.add(new DefaultRow("S"+src_id++, cells));
		} else if (title.equals("cds") && m_cds_features.getBooleanValue()) {
			
			if (m_container3_rows == null) 
				m_container3_rows = new Vector<DefaultRow>();
			HashMap<String,String> feature_properties = new HashMap<String,String>();
			Matcher m = feature_match.matcher(content);
			while (m.find()) {
				feature_properties.put(m.group(1).toLowerCase(), m.group(2));
			}
			DataCell[] cells = new DataCell[NCOLS_PORT2];
			cells[0] = safe_cell(accsn);
			cells[1] = safe_feature(feature_properties, "gene");
			cells[2] = safe_feature(feature_properties, "product");
			cells[3] = safe_feature(feature_properties, "db_xref");
			cells[4] = safe_feature(feature_properties, "translation");
			cells[5] = safe_feature(feature_properties, "note");
			cells[6] = safe_feature(feature_properties, "protein_id");
			cells[7] = safe_feature(feature_properties, "function");
			m_container3_rows.add(new DefaultRow("CDS"+cds_id++, cells));
		}
	}
}
