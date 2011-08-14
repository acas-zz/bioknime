package au.com.acpfg.misc.picr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.date.DateAndTimeCell;
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

import uk.ac.ebi.picr.AccessionMapperInterface;
import uk.ac.ebi.picr.AccessionMapperService;
import uk.ac.ebi.picr.CrossReference;
import uk.ac.ebi.picr.UPEntry;

/**
 * This is the model implementation of PICRAccessor.
 * Provides access to the Protein Identifier Cross Reference (PICR) web service at EBI
 * 
 * Some (IMHO) broken code inside Java Web Services throws a NullPointerException to denote something catchable/correctable,
 * so be sure your exception handling in Eclipse (or whatever IDE you use) does not catch caught NPE's. Sigh... whats wrong with a proper exception type?
 *
 * @author Andrew Cassin
 */
public class PICRAccessorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(PICRAccessorNodeModel.class);
        
    static final String CFGKEY_TAXON = "taxa";
    static final String CFGKEY_DB    = "databases";
    static final String CFGKEY_ACTIVE_ONLY = "active-only";
    static final String CFGKEY_ACCSNS = "accessions";

    

    private final SettingsModelString m_accsns       = new SettingsModelString(CFGKEY_ACCSNS, "Accession");
    private final SettingsModelString m_taxon       = new SettingsModelString(CFGKEY_TAXON, "9606 Homo Sapiens");
    private final SettingsModelBoolean m_active_only = new SettingsModelBoolean(CFGKEY_ACTIVE_ONLY, true);
    private final SettingsModelStringArray m_db      = new SettingsModelStringArray(CFGKEY_DB, new String[]{"SWISSPROT"});
    
    
    private int m_accsn_idx;
    private static List<String> m_databases = null;
    private AccessionMapperService m_service;
    private AccessionMapperInterface m_port;
    
    /**
     * Constructor for the node model.
     */
    protected PICRAccessorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	return new DataTableSpec[] { make_output_spec() };
    }
    
    private DataTableSpec make_output_spec() {
    	DataColumnSpec[] new_cols = new DataColumnSpec[12];
    
    	new_cols[0] = new DataColumnSpecCreator("Sequence (PICR)", StringCell.TYPE).createSpec();
    	new_cols[1] = new DataColumnSpecCreator("UPI", StringCell.TYPE).createSpec();
    	new_cols[2] = new DataColumnSpecCreator("Accession (PICR)", StringCell.TYPE).createSpec();
    	new_cols[3] = new DataColumnSpecCreator("Accession Version", StringCell.TYPE).createSpec();
    	new_cols[4] = new DataColumnSpecCreator("Database Description", StringCell.TYPE).createSpec();
    	new_cols[5] = new DataColumnSpecCreator("Database Name", StringCell.TYPE).createSpec();
    	new_cols[6] = new DataColumnSpecCreator("Date Added", DateAndTimeCell.TYPE).createSpec();
    	new_cols[7] = new DataColumnSpecCreator("Date Deleted", DateAndTimeCell.TYPE).createSpec();
    	new_cols[8] = new DataColumnSpecCreator("GI", StringCell.TYPE).createSpec();
    	new_cols[9] = new DataColumnSpecCreator("Taxon ID", StringCell.TYPE).createSpec();
    	new_cols[10]= new DataColumnSpecCreator("Accession (user-supplied)", StringCell.TYPE).createSpec();
    	new_cols[11]= new DataColumnSpecCreator("Cross Reference Type", StringCell.TYPE).createSpec();
    	
    	return new DataTableSpec(new_cols);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int n_rows = inData[0].getRowCount();
        logger.info("Performing conversion of "+n_rows+" accessions via PICR@EBI");
        m_accsn_idx = inData[0].getSpec().findColumnIndex(m_accsns.getStringValue());
        
        if (m_accsn_idx < 0) {
        	throw new InvalidSettingsException("Cannot find column: "+m_accsns.getStringValue());
        }
        
        List<String> dbs = new ArrayList<String>();
		for (String tmp : m_db.getStringArrayValue()) {
			dbs.add(tmp);
		}
		logger.info("Searching for mappings into "+dbs.size()+ " EBI databases.");
		
		// check required taxa
		String taxon = m_taxon.getStringValue();
		if (taxon == null || taxon.length() < 1 || taxon.toLowerCase().trim().startsWith("any")) {
			taxon = null;		// any species
			logger.info("Searching for mappings to any species.");
		} else {
			Pattern p = Pattern.compile("^\\s*(\\d+)\\b");
			Matcher m = p.matcher(taxon);
			if (!m.find()) {
				throw new Exception("Invalid or unknown taxonomy: "+taxon);
			}
			taxon = m.group(1);
			logger.info("Search for mappings to NCBI taxonomy ID: "+taxon);
		}
		
		// setup accession web service objects
        m_service = new AccessionMapperService();
        m_port    = m_service.getAccessionMapperPort();
        
        // create output table
        BufferedDataContainer container = exec.createDataContainer(make_output_spec(), true);
        double done = 0.0;
        RowIterator it = inData[0].iterator();
        int max_batch_size = 25;
        HashMap<String,String> outstanding_jobs = new HashMap<String,String>();
        int hit = 1;
        while (it.hasNext()) {
        	DataRow r = it.next();
        	DataCell accsn_cell = r.getCell(m_accsn_idx);
        	if (accsn_cell == null || accsn_cell.isMissing()) 
        		continue;
        	
        	if (outstanding_jobs.size() < max_batch_size && it.hasNext()) {
        		outstanding_jobs.put(accsn_cell.toString(), null);
        	} else {
        		// run batch
        		Thread.sleep(5 * 1000);			// be nice to EBI servers
        		for (String accsn : outstanding_jobs.keySet()) {
        			List<UPEntry> entries = fetch_entries(accsn, dbs, taxon, m_active_only.getBooleanValue());
	        		
	    			for (UPEntry e : entries) {
	        			DataCell[] cells    = new DataCell[12];
	        			cells[0] = new StringCell(e.getSequence());
	        			cells[1] = new StringCell(e.getUPI());
	        			cells[10]= new StringCell(accsn);
	        			cells[11]= new StringCell("identical");
	        			
	        			for (CrossReference xref : e.getIdenticalCrossReferences()) {
	        				cells[2] = safe_cell(xref.getAccession());
	        				cells[3] = safe_cell(xref.getAccessionVersion());
	        				cells[4] = safe_cell(xref.getDatabaseDescription());
	        				cells[5] = safe_cell(xref.getDatabaseName());
	        				
	        				cells[6] = safe_cell(xref.getDateAdded());	
	        				cells[7] = safe_cell(xref.getDateDeleted());
	        				cells[8] = safe_cell(xref.getGi());
	        				cells[9] = safe_cell(xref.getTaxonId());
	        				
		        			container.addRowToTable(new DefaultRow("Hit"+hit++, cells));
	        			}
	        			
	        			cells[11] = new StringCell("logical");
	        			for (CrossReference xref : e.getLogicalCrossReferences()) {
	        				cells[2] = safe_cell(xref.getAccession());
	        				cells[3] = safe_cell(xref.getAccessionVersion());
	        				cells[4] = safe_cell(xref.getDatabaseDescription());
	        				cells[5] = safe_cell(xref.getDatabaseName());
	        				
	        				cells[6] = safe_cell(xref.getDateAdded());	
	        				cells[7] = safe_cell(xref.getDateDeleted());
	        				cells[8] = safe_cell(xref.getGi());
	        				cells[9] = safe_cell(xref.getTaxonId());
	        				
		        			container.addRowToTable(new DefaultRow("Hit"+hit++, cells));
	        			}
	        			
	    			}
        		}
        	
        		exec.checkCanceled();
        		exec.setProgress(done++ / n_rows);
        	}
        }
        container.close();
        BufferedDataTable out = container.getTable();
    	return new BufferedDataTable[] {out};
    }

    /**
     * Fetches PICR data from EBI with retry in case of temporary network failure
     * @param accsn		Accession to find map entries to
     * @param dbs		Databases to search for entries
     * @param taxon		Taxonomy constraint (only single taxon currently available). <code>Null</code> if any species is ok.
     * @param booleanValue	Active entries only? (true means active only)
     * @return
     */
    private List<UPEntry> fetch_entries(String accsn, List<String> dbs, String taxon, boolean booleanValue) throws Exception {
    	assert(accsn != null && dbs != null && dbs.size() > 0);
    	
    	for (int retry=0; retry<4; retry++) {
    		try {
    			List<UPEntry> entries = m_port.getUPIForAccession(accsn, null, dbs, taxon, m_active_only.getBooleanValue());
    			return entries;
    		} catch (Exception e) {
    			logger.warn(e.getMessage());
    			// fall thru
    		}
    		
    		int delay = 500 * (retry+1);
    		logger.warn("Temporary network failure, delaying for "+delay+" seconds");
    		try { 
    			Thread.sleep( delay * 1000);
    		} catch (InterruptedException e) {
    			// ignore
    		}
    	}
		
    	// persistent network problem... abort
    	throw new Exception("Cannot reach EBI PICR service: aborting!");
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

   
    /**
     * Ensures that a valid DataCell is returned even if the value parameter is <code>null</code>
     */
    protected DataCell safe_cell(String value) {
    	if (value == null) 
    		return DataType.getMissingCell();
    	return new StringCell(value);
    }
    
    protected DataCell safe_cell(XMLGregorianCalendar cal) {
    	/**
    	 * No calendar? return a missing value
    	 */
    	if (cal == null) {
    		return DataType.getMissingCell();
    	}
    	
    	int year = cal.getYear();
    	int month= cal.getMonth();
    	int day  = cal.getDay();
    	
    	/*
    	 * return a missing value if the date is incomplete
    	 */
    	if (year  == DatatypeConstants.FIELD_UNDEFINED ||
    		month == DatatypeConstants.FIELD_UNDEFINED ||
    		day   == DatatypeConstants.FIELD_UNDEFINED) {
    		return DataType.getMissingCell();
    	}
    	
    	return new DateAndTimeCell(year, month, day);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_accsns.saveSettingsTo(settings);
    	m_db.saveSettingsTo(settings);
    	m_taxon.saveSettingsTo(settings);
    	m_active_only.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_accsns.loadSettingsFrom(settings);
    	m_db.loadSettingsFrom(settings);
    	m_taxon.loadSettingsFrom(settings);
    	m_active_only.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_accsns.validateSettings(settings);
    	m_db.validateSettings(settings);
    	m_taxon.validateSettings(settings);
    	m_active_only.validateSettings(settings);
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

	public static synchronized List<String> load_databases() {
		if (m_databases == null || m_databases.size() < 1) {
			logger.info("PICR: loading databases from EBI... please wait a few moments... ");
			AccessionMapperService service = new AccessionMapperService();
			AccessionMapperInterface port  = service.getAccessionMapperPort();
			m_databases = port.getMappedDatabaseNames();
			Collections.sort(m_databases);
			logger.info("PICR: done loading databases!");
		}
		return m_databases;
	}

}

