package au.com.acpfg.misc.uniprot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;


import com.fatdog.xmlEngine.exceptions.CantParseDocumentException;

import au.com.acpfg.xml.reader.XMLCell;

/**
 * Responsible for retreiving the uniprot record for the user-specifed accessions. Tries very hard to keep getting
 * data in the face of network disruptions as long-running fetches should not fail under normal circumstances. 
 * Also uses EHCache to cache retrieved records (if wanted by the user)
 * 
 * @author andrew.cassin
 *
 */
public class RetrieveEntryTask implements UniProtTaskInterface {
	private final int NUM_COLUMNS = 13;		// 14 if m_want_xml is true
	private final static int MAX_RETRIES = 5;	// give up after X attempts
	private final int MAX_UNIPROT_CACHE_ELEMENTS = 1000000;		// 1 million uniprot records (max.) in cache (BUG:!?!)
	private boolean m_want_xml = false;
	private int      m_fetched = 0;
	
	
	protected CacheableUniProtRecord.UniProtDatabase m_db;
	protected int m_hit;
	
	// object caching state (if requested)
	private int          m_cache_freshness;
	private Cache        m_cache;
	private CacheManager m_cache_mgr;

	
	public RetrieveEntryTask(UniProtAccessorNodeModel m, String db) {
		assert(db != null);
		
		if (db.startsWith("/uniprot/")) {
			m_db = CacheableUniProtRecord.UniProtDatabase.UNIPROT_KB;
		} else if (db.startsWith("/uniref/")) {
			m_db = CacheableUniProtRecord.UniProtDatabase.UNIREF;
		} else if (db.startsWith("/uniparc/")) {
			m_db = CacheableUniProtRecord.UniProtDatabase.UNIPARC;
		} else {
			m_db = CacheableUniProtRecord.UniProtDatabase.UNKNOWN;
		}
		m_hit             = 1;
		File objcache     = m.getCacheFile();
		m_cache_freshness = m.getCacheFreshness();
		
		// try to create/use an existing cache or switch it off...
		try {
			m_cache = null;
			if (objcache != null) {
				m_cache_mgr = new CacheManager();
				Cache c = new Cache(new CacheConfiguration("uniprot", MAX_UNIPROT_CACHE_ELEMENTS)
									.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
									.overflowToDisk(true)
									.eternal(true)
									.timeToLiveSeconds(m_cache_freshness * 24 * 60 * 60)
									.diskPersistent(true)
									.diskStorePath(m.getCacheFile().getAbsolutePath())
									);
				m_cache_mgr.addCache(c);
				
				m_cache = m_cache_mgr.getCache("uniprot"); // ensures the cache is live...
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DataTableSpec getTableSpec(boolean want_xml) {
		m_want_xml = want_xml;
		int ncols = NUM_COLUMNS;
		if (m_want_xml) 
			ncols++;
		DataColumnSpec[] appended_col_specs = new DataColumnSpec[ncols];

		DataType dt = ListCell.getCollectionType(StringCell.TYPE);
		appended_col_specs[0] = new DataColumnSpecCreator("UniProt ID", StringCell.TYPE).createSpec();
		appended_col_specs[1] = new DataColumnSpecCreator("UniProt Recommended Protein Name", StringCell.TYPE).createSpec();
		appended_col_specs[2] = new DataColumnSpecCreator("UniProt X-Refs", dt).createSpec();
		appended_col_specs[3] = new DataColumnSpecCreator("UniProt Sequence", StringCell.TYPE).createSpec();
		appended_col_specs[4] = new DataColumnSpecCreator("UniProt Gene (primary)", StringCell.TYPE).createSpec();
		appended_col_specs[5] = new DataColumnSpecCreator("UniProt Organism", StringCell.TYPE).createSpec();
		appended_col_specs[6] = new DataColumnSpecCreator("UniProt Comments", dt).createSpec();
		appended_col_specs[7] = new DataColumnSpecCreator("UniProt Organelles", dt).createSpec();
		appended_col_specs[8] = new DataColumnSpecCreator("UniProt Taxon", dt).createSpec();
		appended_col_specs[9] = new DataColumnSpecCreator("UniProt Keywords", dt).createSpec();
		appended_col_specs[10]= new DataColumnSpecCreator("UniProt Protein Existence Evidence", StringCell.TYPE).createSpec();
		appended_col_specs[11]= new DataColumnSpecCreator("UniProt Features", dt).createSpec();
		appended_col_specs[12]= new DataColumnSpecCreator("UniProt Citations", dt).createSpec();
		if (m_want_xml) {
			appended_col_specs[13]= new DataColumnSpecCreator("XML", XMLCell.TYPE).createSpec();
		}
		return new DataTableSpec(appended_col_specs);
	}
	
	@Override
	public int run(String[] accsns, DataRow[] in_rows, DataContainer out) throws Exception {
		assert(accsns != null && out != null);
		if (m_db != CacheableUniProtRecord.UniProtDatabase.UNKNOWN) {		// one of UNIPROT_KB, UNIPARC or UNIREF
			HttpClient cli = new HttpClient();
			int     n_hits = 0;
			int        idx = 0;
			m_fetched = 0;			// how many records in batch needed to be fetched?
			for (final String accsn : accsns) {
				DataCell[]        cells = null;
				boolean   get_via_www   = true;
				
				// 1. try to find a suitable object in the cache and use it where possible...
				if (m_cache != null) {
					  String key = CacheableUniProtRecord.makeKey(m_db, accsn);
					  Element e = m_cache.get(key);
					  if (e != null) {
						  Object v = e.getObjectValue();		// NB: this is not serialisable
						  assert(v != null);
						  cells = grok_entry(((CacheableUniProtRecord)v).getXML());
						  get_via_www = false;
					  }
					  // else FALLTHRU...
				}
				
				// 2. else not in the cache so fetch it from uniprot...
				if (get_via_www) {
					String db_str = "uniprot";
					if (m_db == CacheableUniProtRecord.UniProtDatabase.UNIREF)
						db_str = "uniref";
					else if (m_db == CacheableUniProtRecord.UniProtDatabase.UNIPARC)
						db_str = "uniparc";
					m_fetched++;

					// cells must be null if no data obtained for the row (for any reason)
					for (int retry=1; retry < MAX_RETRIES; retry++) {
						URL url = new URL("http://www.uniprot.org/"+db_str+"/"+accsn+".xml");
						Logger.getAnonymousLogger().info(url.toString());

						try {
							URLConnection conn = url.openConnection();
							InputStream     is = conn.getInputStream();
						
							cells = grok_entry(m_cache, accsn, is);
							is.close();
							break;
						} catch (DeletedEntryException de) {
									Logger.getAnonymousLogger().warning("Entry "+accsn+" appears to be deleted. Ignoring.");
									cells = null;
									idx++;	// no output for this row
									break;
						} catch (SocketTimeoutException ste) {
									int delay = retry * 100;
									Logger.getAnonymousLogger().warning("Entry "+accsn+" timed out getting XML (network problem?). Retrying in "+delay+" seconds");
									ste.printStackTrace();
									Thread.sleep(delay*1000);
									cells = null;
						} catch (FileNotFoundException fe) {
							// maybe a bad accsn and we tried to fetch it?
							Logger.getAnonymousLogger().warning("Entry "+accsn+" can not be found. Are you sure the accession column is configured?");
							cells = null;
							idx++;	// no output for this row
							break;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					Thread.sleep(3 * 1000);		// be nice to uniprot servers between accsns
				}
				
				// output current row (if any)
				if (cells != null) {
					if (in_rows != null) {
						DataRow r = new DefaultRow(in_rows[idx].getKey(), cells);	// NB: cells must match tablespec
						out.addRowToTable(new JoinedRow(in_rows[idx++], r));
						n_hits++;
					} else {
						out.addRowToTable(new DefaultRow("Hit"+m_hit++, cells));
						n_hits++;
					}
				}
			}
			return n_hits;
		} else {
			throw new Exception("Unsupported operation: "+m_db);
		}
	}

	

	protected DataCell list2listcell(List<String> data) {
		if (data == null || data.size() < 1) {
			return DataType.getMissingCell();
		}
		ArrayList<StringCell> coll = new ArrayList<StringCell>();
		for (int i=0; i<data.size(); i++) {
			coll.add(new StringCell(data.get(i)));
		}
		return CollectionCellFactory.createListCell(coll);
	}
	
	protected DataCell safe_string(String datum) {
		if (datum == null)
			return DataType.getMissingCell();
		return new StringCell(datum);
	}
	
	public String fix_accsn(String in_accsn) throws Exception {
		String tmp = in_accsn.trim();
		
		if (tmp.startsWith("UniRef")) {
			Pattern p = Pattern.compile("^UniRef(50|90|100)_(.*+)$");
			Matcher m = p.matcher(tmp);
			if (m.matches()) {
				String db = m.group(1);
				tmp = m.group(2);	// only want the accsn, not the uniref db identifier
			}
		}
		return tmp;
	}
	
	/**
	 * Template function to turn the stream into a string for subsequent processing. XML is limited in size (by the record sizes) from UniProt so this 
	 * shouldn't be too wasteful of memory. This method also stores the retrieve XML in a DB4O cache to speed subsequent access (eg. if more uniprot work is done later in the
	 * KNIME workflow)
	 *  
	 * @param xml_stream
	 * @param cache the db4o-compatible object container to store the record into
	 * @return
	 * @throws Exception
	 */
	protected final DataCell[] grok_entry(Cache cache, String accsn, InputStream response_stream) throws Exception {		
		String fixed_xml = UniProtHit.xml2string(response_stream, true);
		
		if (cache != null && fixed_xml != null && fixed_xml.length() > 0) {
			CacheableUniProtRecord rec = new CacheableUniProtRecord(m_db, accsn, fixed_xml);
			cache.put(new Element(rec.getKey(), rec), false);
			rec = null;
		}
		
		return grok_entry(fixed_xml);
	}
	
	/**
	 * Override this method to implement a custom response to the retrieved record. Note that this method <b>DOES NOT</b> cache the record,
	 * use the other form of the method for that
	 * 
	 * @param xml a single, complete, XML record from UniProtKB/UniPARC/UniRef (should be well-formed XML or an exception is the likely result)
	 * @return the cells representing the results for the current record (must match the columns specified by <code>getTableSpec()</code>)
	 * @throws Exception
	 */
	protected DataCell[] grok_entry(String xml) throws Exception {
		List<UniProtHit> hits = null;
		try {
			hits = UniProtHit.make_entries(xml);
		} catch (CantParseDocumentException cpde) {
			// BUG: this exception tends to be thrown with &lt; occurs in an id attribute from a 
			// UniProt entry. I dont think the XML is invalid, but the UniProtHit parsing code doesnt handle it for now...
			Logger.getAnonymousLogger().warning("Cannot parse XML: ignoring data (bug)!");
			// fallthru: return missing
			hits = null;
		} catch (DeletedEntryException dee) {
			// dont do a stack trace here...
			throw dee;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		int ncols = NUM_COLUMNS;
		if (m_want_xml) {
			ncols++;
		}
		DataCell[] cells = new DataCell[ncols];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		if (hits == null || hits.size() < 1) {
			return cells;
		} else if (hits.size() > 1) {
			Logger.getAnonymousLogger().warning("Multiple choices available: entry looks to have been replaced, only first choice is reported.");
		}
		UniProtHit hit = hits.get(0);
		cells[0] = safe_string(hit.getID());
		cells[1] = safe_string(hit.getRecommendedName());
		cells[2] = list2listcell(hit.getXrefs());
		cells[3] = safe_string(hit.getSequence());
		cells[4] = safe_string(hit.getGenePrimary());
		cells[5] = safe_string(hit.getOrganism());
		cells[6] = list2listcell(hit.getComments());
		cells[7] = DataType.getMissingCell();
		cells[8] = list2listcell(hit.getLineage());
		cells[9] = list2listcell(hit.getKeywords());
		cells[10]= safe_string(hit.getExistenceEvidence());
		cells[11]= list2listcell(hit.getFeatures());
		cells[12]= list2listcell(hit.getCitations());
		if (m_want_xml) {
			cells[13]= new XMLCell(xml);
		}
		return cells;
	}
	
	@Override 
	public void cleanup() throws Exception {
		if (m_cache != null) {
			m_cache.flush();
			m_cache_mgr.shutdown();
		}
	}

	@Override
	public void pause(ExecutionContext exec, double progress, String msg)
			throws InterruptedException, CanceledExecutionException {
		exec.checkCanceled();
		if (m_fetched >= 20) {
			exec.setProgress(progress, "Pause to be nice to UniProt servers (20sec. delay)");
			Thread.sleep(20 * 1000);
			m_fetched = 0;
		} else {
			exec.setProgress(progress);
		}
	}
}
