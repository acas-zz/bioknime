package au.com.acpfg.misc.spectra;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.systemsbiology.jrap.stax.DataProcessingInfo;
import org.systemsbiology.jrap.stax.MSInstrumentInfo;
import org.systemsbiology.jrap.stax.MSOperator;
import org.systemsbiology.jrap.stax.MSXMLParser;
import org.systemsbiology.jrap.stax.MZXMLFileInfo;
import org.systemsbiology.jrap.stax.Scan;
import org.systemsbiology.jrap.stax.ScanHeader;
import org.systemsbiology.jrap.stax.SoftwareInfo;

/**
 * Implements support for mzML/mzXML spectra and scan/file statistics, based on the JRAP-Stax
 * library. Support for gzip-base64 compressed scan data, but not compressed files at this time.
 * 
 * @author andrew.cassin
 *
 */
public class mzMLDataProcessor extends AbstractDataProcessor {
	private MSXMLParser m_p;
	private String m_filename;
	
	public mzMLDataProcessor() {
	}
	
	@Override
	public boolean can(File f) {
		m_filename = f.getAbsolutePath();
		String ext = f.getName().toLowerCase();
		NodeLogger logger = NodeLogger.getLogger(SpectraReaderNodeModel.class);
		if (ext.endsWith(".mzxml") || ext.endsWith(".mzml")) {
			try {
				m_p = new MSXMLParser(m_filename,true);
				logger.info(m_filename+" has "+m_p.getScanCount()+ " scans.");
			} catch (Exception e) {
				logger.warn("Could not process: "+m_filename+", reason:");
				e.printStackTrace();
				return false;
			}
	        return true;
		}
		return false;
	}

	@Override
	public void process(boolean load_spectra, RowSequence scan_seq, RowSequence file_seq, 
			ExecutionContext exec, DataContainer scan_container, DataContainer file_container) throws Exception {
		
		// first output the file port
		process_file(exec, file_container, file_seq, m_p.rapFileHeader());
		
		// now output the scan port
		process_scans(exec, scan_container, scan_seq);
	}
	
	protected void process_scans(ExecutionContext exec, DataContainer scan_container, RowSequence scan_seq) throws Exception {
		int ncols = scan_container.getTableSpec().getNumColumns();
		for (int i=1; i<=m_p.getScanCount(); i++) {
			ScanHeader sh = m_p.nextHeader();
			
			DataCell[] cells = new DataCell[ncols];
			
			String scan_type = (sh.getScanType() != null) ? sh.getScanType() : sh.getFilterLine();
			if (scan_type == null) {
				scan_type = "Offset: " + sh.getScanOffset();
			}
			cells[0] = new StringCell(scan_type);
			cells[1] = new StringCell(sh.getPolarity());
			cells[2] = new StringCell(sh.getRetentionTime());
			cells[3] = new DoubleCell(sh.getBasePeakIntensity());
			cells[4] = new DoubleCell(sh.getBasePeakMz());
			cells[5] = new IntCell(sh.getCentroided());
			cells[6] = new IntCell(sh.getDeisotoped());
			cells[7] = new IntCell(sh.getChargeDeconvoluted());
			cells[8] = new IntCell(sh.getMsLevel());
			cells[9] = new StringCell(sh.getID());
			cells[10]= new IntCell(sh.getPrecursorCharge());
			cells[11]= new IntCell(sh.getPrecursorScanNum());
			cells[12]= new DoubleCell(sh.getPrecursorIntensity());
			cells[13]= new DoubleCell(sh.getPrecursorMz());
			cells[14]= new DoubleCell(sh.getTotIonCurrent());
			cells[15]= new DoubleCell(sh.getCollisionEnergy());
			cells[16]= new DoubleCell(sh.getIonisationEnergy());
			cells[17]= new DoubleCell(sh.getStartMz());
			cells[18]= new DoubleCell(sh.getEndMz());
			cells[19]= new DoubleCell(sh.getLowMz());
			cells[20]= new DoubleCell(sh.getHighMz());
			cells[21]= new StringCell(m_filename);
			cells[22]= new IntCell(sh.getPeaksCount());
			
			// load spectra?
			if (ncols > 23) {
					Scan s = m_p.rap(sh.getNum());
					cells[23] = SpectraUtilityFactory.createCell(s, scan_type);
			}
			DataRow r = new DefaultRow(new RowKey(scan_seq.get()), cells);
			scan_container.addRowToTable(r);
			
			if (i % 300 == 0) {
				exec.checkCanceled();
			}
		}
	}
	
	/**
	 * Report the file summary
	 * @param exec
	 * @param fc
	 * @param file_seq
	 * @param fh
	 * @throws Exception
	 */
	protected void process_file(ExecutionContext exec, DataContainer fc, RowSequence file_seq, MZXMLFileInfo fh) throws Exception {
    	MSInstrumentInfo   ii= fh.getInstrumentInfo();
	    DataProcessingInfo dp=fh.getDataProcessing();
	    int ncols = fc.getTableSpec().getNumColumns();
	    DataCell[] cells = new DataCell[ncols];
	    assert(ncols == 9);
	    	
	    cells[0] = safe_cell(ii.getManufacturer());
	    cells[1] = safe_cell(ii.getModel());
	    SoftwareInfo si = ii.getSoftwareInfo();
	    cells[2] = safe_cell(si != null ? si.toString() : null);
	   
	    MSOperator mso = ii.getOperator();
	    cells[3] = safe_cell(mso != null ? mso.toString() : null);
	  
	    cells[4] = safe_cell(ii.getMassAnalyzer());
	    cells[5] = safe_cell(ii.getIonization());
	    cells[6] = safe_cell(ii.getDetector());
	    cells[7] = safe_cell(dp.toString());
	    cells[8] = safe_cell(m_filename);
	    
	    fc.addRowToTable(new DefaultRow(new RowKey(file_seq.get()), cells));
    }

    @Override
    public boolean finish() {
    	super.finish();
    	
    	m_p = null;
    	return true;	
    }
    
	@Override
	public void setInput(String id) {
		// does nothing (m_filename is set by can() above)
	}

}
