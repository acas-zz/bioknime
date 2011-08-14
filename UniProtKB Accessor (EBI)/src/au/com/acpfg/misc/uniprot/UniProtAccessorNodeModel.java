package au.com.acpfg.misc.uniprot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of UniProtAccessor. Accesses the UniProt
 * data source (via webservices)
 * 
 * @author Andrew Cassin
 */
public class UniProtAccessorNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(UniProtAccessorNodeModel.class);

	/**
	 * the settings key which is used to retrieve and store the settings (from
	 * the dialog or from a settings file) (package visibility to be usable from
	 * the dialog).
	 */
	static final String CFGKEY_TASK      = "task";
	static final String CFGKEY_ACCSN_COL = "accsn-col";
	static final String CFGKEY_UNIREF    = "uniref-db";
	static final String CFGKEY_FROM_ACCSN= "from-accsn";
	static final String CFGKEY_TO_ACCSN  = "to-accsn";
	static final String CFGKEY_WANTXML   = "want-xml";
	static final String CFGKEY_CACHE      = "cache-results";
	static final String CFGKEY_CACHE_FRESHNESS = "cache-freshness";
	static final String CFGKEY_CACHE_FILENAME  = "cache-filename";

	/** initial defaults for configured parameters */
	private static final String DEFAULT_TASK = "Retrieve UniProt Entries";
	private static final String DEFAULT_ACCSN_COL = "Accession";

	
	
	// example value: the models count variable filled from the dialog
	// and used in the models execution method. The default components of the
	// dialog work with "SettingsModels".
	private final SettingsModelString         m_task = make_as_string(CFGKEY_TASK);
	private final SettingsModelColumnName m_accsn_col= (SettingsModelColumnName) make(CFGKEY_ACCSN_COL);
	private final SettingsModelString m_uniref_db    = make_as_string(CFGKEY_UNIREF);
	private final SettingsModelString m_from_accsn   = make_as_string(CFGKEY_FROM_ACCSN);
	private final SettingsModelString m_to_accsn     = make_as_string(CFGKEY_TO_ACCSN);
	private final SettingsModelBoolean m_want_xml    = (SettingsModelBoolean) make(CFGKEY_WANTXML);
	private final SettingsModelBoolean m_cache       = (SettingsModelBoolean) make(CFGKEY_CACHE);
	private final SettingsModelString  m_cache_file  = make_as_string(CFGKEY_CACHE_FILENAME);
	private final SettingsModelNumber  m_cache_freshness = (SettingsModelNumber) make(CFGKEY_CACHE_FRESHNESS);
	
	/**
	 * Constructor for the node model.
	 */
	protected UniProtAccessorNodeModel() {
		super(1, 1);
	}

	protected static SettingsModel make(String key) {
		if (key.equals(CFGKEY_TASK))
			return new SettingsModelString(CFGKEY_TASK, DEFAULT_TASK);
		else if (key.equals(CFGKEY_ACCSN_COL))
			return new SettingsModelColumnName(CFGKEY_ACCSN_COL,
					DEFAULT_ACCSN_COL);
		else if (key.equals(CFGKEY_UNIREF)) {
			SettingsModelString sms = new SettingsModelString(CFGKEY_UNIREF, "UniRef100");
			sms.setEnabled(false);
			return sms;
		} else if (key.equals(CFGKEY_FROM_ACCSN)) {
			SettingsModelString sms2 = new SettingsModelString(CFGKEY_FROM_ACCSN, "UniProt");
			sms2.setEnabled(false);
			return sms2;
		} else if (key.equals(CFGKEY_TO_ACCSN)) {
			SettingsModelString sms3 = new SettingsModelString(CFGKEY_TO_ACCSN, "TAIR");
			sms3.setEnabled(false);
			return sms3;
		} else if (key.equals(CFGKEY_WANTXML)) {
			SettingsModelBoolean b = new SettingsModelBoolean(CFGKEY_WANTXML, false);
			return b;
		} else if (key.equals(CFGKEY_CACHE)) {
			SettingsModelBoolean b = new SettingsModelBoolean(CFGKEY_CACHE, true);
			// NB: since true is the default for b, the other cache parameters can default to setEnabled(true) ie. default constructed
			return b;
		} else if (key.equals(CFGKEY_CACHE_FRESHNESS)) {
			SettingsModelNumber n = new SettingsModelIntegerBounded(key, 180, 0, 2000);
			return n;
		} else if (key.equals(CFGKEY_CACHE_FILENAME)) {
			try {
				File f = File.createTempFile("uniprot-cache", ".db4o");
				return new SettingsModelString(CFGKEY_CACHE_FILENAME, f.getAbsolutePath());
			} catch (Exception e) {
				return new SettingsModelString(CFGKEY_CACHE_FILENAME, "");
			}
		}
		return null;
	}

	protected static SettingsModelString make_as_string(String key) {
		return (SettingsModelString) make(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
			
		// 1. go thru the rows handling exceptions from the task object
		UniProtTaskInterface up = null;
		try {
		boolean use_rid = m_accsn_col.useRowID();
		int accsn_col_idx = -1;
	    if (!use_rid) {
	    	accsn_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_accsn_col.getStringValue());
	    	if (accsn_col_idx < 0)
	           throw new Exception("Cannot find column: "+m_accsn_col.getStringValue()+" - reset the node?");
	    }
	        
		// setup the columns to hold the data
		// inData[0] is the data to be matched, inData[1] is the strings to use
		// for matching (or RE's)
		DataTableSpec spec = inData[0].getDataTableSpec();
		int         n_rows = inData[0].getRowCount();
		int      done_rows = 0;
		int         n_hits = 0;

		String        task = m_task.getStringValue();
		if (task.equals(DEFAULT_TASK)) {
			up = new RetrieveEntryTask(this, "/uniprot/");
		} else if (task.equals("Retrieve UniRef Entries")) {
			up = new UniRefEntryTask(this, m_uniref_db.getStringValue());
		} else if (task.startsWith("Retrieve UniPARC")) {
			up = new UniPARCEntryTask(this, "/uniparc/");
		} else if (task.startsWith("Map")) {
			up = new AccessionMapTask(m_from_accsn.getStringValue(), m_to_accsn.getStringValue());
		} else {
			throw new InvalidSettingsException("Unsupported task: "+task);
		}
		
		DataTableSpec outputSpec = new DataTableSpec("UniProt appended data", spec, up.getTableSpec(m_want_xml.getBooleanValue()));

		BufferedDataContainer container = exec.createDataContainer(outputSpec);

		// run the necessary queries in batches of up to m_batch_size rows ie.
		// accessions each
		RowIterator it = inData[0].iterator();
		int batch_cnt = 0;
		int batch_size= 20;
		
		// batch data structures
		ArrayList<String> batch_accsns = new ArrayList<String>();
		ArrayList<DataRow> batch_rows  = new ArrayList<DataRow>();
		
		// main loop
		while (it.hasNext()) {
			DataRow r = it.next();
			
			String accsn = use_rid ? get_accsn(r, use_rid) : get_accsn(r, accsn_col_idx);
			if (accsn == null || accsn.length() < 1) {
				done_rows++;
				continue;
			}
			
			// batch up the current row into internal data structures
			if (batch_cnt < batch_size) {
				String final_accsn = up.fix_accsn(accsn);
				if (final_accsn != null && final_accsn.length() > 0) {
					batch_accsns.add(final_accsn);
					batch_rows.add(r);
					batch_cnt++;
				}
				done_rows++;
			}
			
			if (batch_cnt == batch_size) {
				exec.checkCanceled();
				exec.setProgress(((double) done_rows) / n_rows, "Fetching batch, from: "+batch_accsns.get(0)+ " (size "+batch_accsns.size()+")");
				batch_run(up, batch_accsns, batch_rows, container);
				batch_accsns.clear();
				batch_rows.clear();
				batch_cnt = 0;
				
				// sleep for 20s between each batch (be nice to EBI facilities if they are being used ie. not cached)
				up.pause(exec, ((double) done_rows) / n_rows, "Pausing for 20sec. (to be nice to UniProt servers)");
			}
		}
		// NB: dont forget the last batch (probably not a multiple of batch_size)!
		batch_run(up, batch_accsns, batch_rows, container);
	
		// finalise output for the node...
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	} catch (Exception e) {
		logger.error(e.getMessage());
		e.printStackTrace();
		throw e;
	} finally {
		// and ensure that any cleanup code is given a chance to run... regardless of exceptions
		if (up != null)
			up.cleanup();	
	}
		
	}

	/**
	 * Responsible for returning an accession for the specified row. If you call this method, 
	 * <code>use_rid</code> must be true or an assertion will fail. Ugly implementation.
	 * 
	 * @param r
	 * @param use_rid
	 * @returns the row ID
	 */
	protected String get_accsn(DataRow r, boolean use_rid) {
		assert(use_rid);
		String rid = r.getKey().getString().trim();
		return rid;
	}

	/**
	 * Returns the accession from the specified column
	 * @param r
	 * @param accsn_col_idx
	 * @return
	 */
	protected String get_accsn(DataRow r, int accsn_col_idx) {
		assert(accsn_col_idx >= 0);
		DataCell c = r.getCell(accsn_col_idx);
		if (c == null || c.isMissing()) {
			return null;
		}
		String accsn = c.toString().trim();
		return accsn;
	}
	
	/**
	 * Process the entire batch of rows (each with a corresponding accsn) and add the results into the specified
	 * container for the UniProt task object to run. The batch can be of arbitrary size, but most tasks will only
	 * process them one at a time.
	 * 
	 * @param up
	 * @param batch_accsns
	 * @param batch_rows
	 * @param container
	 * @return
	 * @throws Exception
	 */
	private int batch_run(UniProtTaskInterface up, ArrayList<String> batch_accsns,
			ArrayList<DataRow> batch_rows, BufferedDataContainer container) throws Exception {
		String[] accsns = batch_accsns.toArray(new String[0]);
		if (accsns.length < 1)
			return 0;
		logger.debug("Running batch of accessions: "+accsns[0]+" - "+accsns[accsns.length-1]);
		
		return up.run(accsns, batch_rows.toArray(new DataRow[0]), container);
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

		return new DataTableSpec[] { null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_task.saveSettingsTo(settings);
		m_accsn_col.saveSettingsTo(settings);
		m_uniref_db.saveSettingsTo(settings);
		m_from_accsn.saveSettingsTo(settings);
		m_to_accsn.saveSettingsTo(settings);
		m_want_xml.saveSettingsTo(settings);
		
		m_cache.saveSettingsTo(settings);
		m_cache_file.saveSettingsTo(settings);
		m_cache_freshness.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		m_task.loadSettingsFrom(settings);
		m_accsn_col.loadSettingsFrom(settings);
		m_uniref_db.loadSettingsFrom(settings);
		m_from_accsn.loadSettingsFrom(settings);
		m_to_accsn.loadSettingsFrom(settings);
		m_want_xml.loadSettingsFrom(settings);
		
		m_cache.loadSettingsFrom(settings);
		m_cache_file.loadSettingsFrom(settings);
		m_cache_freshness.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		m_task.validateSettings(settings);
		m_accsn_col.validateSettings(settings);
		m_uniref_db.validateSettings(settings);
		m_from_accsn.validateSettings(settings);
		m_to_accsn.validateSettings(settings);
		m_want_xml.validateSettings(settings);
		
		m_cache.validateSettings(settings);
		m_cache_file.validateSettings(settings);
		m_cache_freshness.validateSettings(settings);
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
	 * Has the user requested to cache files?
	 * @return
	 */
	public boolean isCaching() {
		return m_cache.getBooleanValue();
	}
	
	/**
	 * Returns the File instance representing the cache file, or <code>null</code> if the user does not want caching of results
	 */
	public File getCacheFile() {
		if (!isCaching())
			return null;
		return new File(m_cache_file.getStringValue());
	}
	
	/**
	 * Returns the number of days old which a cache entry is to be considered current, or <code>-1</code> if not 
	 */
	public int getCacheFreshness() {
		if (!isCaching())
			return -1;
		return ((SettingsModelIntegerBounded)m_cache_freshness).getIntValue();
	}
}
