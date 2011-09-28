package au.com.acpfg.misc.spectra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.proteomecommons.io.Peak;
import org.proteomecommons.io.GenericPeak;
import org.systemsbiology.jrap.stax.DataProcessingInfo;
import org.systemsbiology.jrap.stax.MSInstrumentInfo;
import org.systemsbiology.jrap.stax.MSOperator;
import org.systemsbiology.jrap.stax.MZXMLFileInfo;
import org.systemsbiology.jrap.stax.SoftwareInfo;


/**
 * Implements support for .mgf and .mgf.gz files using ProteomeCommons IO framework. The
 * only data loaded into the table (in the spectra column!) is as follows:
 * BEGIN IONS
   TITLE=The first peptide - dodgy peak detection, so extra wide tolerance
   PEPMASS=896.05 25674.3
   CHARGE=3+
   TOL=3
   TOLU=Da
   SEQ=n-AC[DHK]s
   COMP=2[H]0[M]3[DE]*[K]
   240.1 3
   242.1 12
   245.2 32
   ...
 * query parameters, in particular are not currently supported.
 * 
 * @author andrew.cassin
 *
 */
public class MGFDataProcessor extends AbstractDataProcessor {
	private BufferedReader m_is;
	private String m_filename;
	
	public MGFDataProcessor() {
		m_is = null;
	}

	@Override
	public boolean can(File f) throws Exception {
		m_filename = f.getName();
		String ext = m_filename.toLowerCase();
		return (ext.endsWith(".mgf") || ext.endsWith(".mgf.gz"));
	}

	@Override
	public void process(boolean load_spectra, RowSequence scan_seq,
			RowSequence file_seq, ExecutionContext exec,
			DataContainer scan_container, DataContainer file_container)
			throws Exception {
		
		if (m_is == null) 
			throw new Exception("No file to load!");
		
		String line;
		StringBuilder headers   = new StringBuilder(10 * 1024);
		StringBuilder peak_list = new StringBuilder(10 * 1024);
		boolean got_start = false;
		boolean in_headers= false;
		int done = 0;
		int peaks= 0;
		int ncols= scan_container.getTableSpec().getNumColumns();
		while ((line = m_is.readLine()) != null) {
			if (!got_start && line.startsWith("BEGIN IONS")) {
				got_start = true;
				in_headers = true;
				headers.delete(0, headers.length());
				peak_list.delete(0, peak_list.length());
				peaks = 0;
			} else if (got_start && line.startsWith("END IONS")) {
				got_start = false;
				in_headers= false;
				done++;
				if (peaks > 0) {
					process_spectra(headers.toString(), peak_list.toString(), peaks,
						        scan_seq, load_spectra, ncols, scan_container);
					
				} else {
					NodeLogger.getLogger(SpectraReaderNodeModel.class).warn("Got spectra with no peaks!");
				}
				if (done % 100 == 0) {
					exec.checkCanceled();
				}
			} else if (got_start) {
				// if its a digit, we have finished the headers
				char c = line.charAt(0);
				if (Character.isDigit(c)) {
					in_headers = false;
					peaks++;
					peak_list.append(line);
					peak_list.append("\n");
				} else {
					in_headers = true;
					headers.append(line);
					headers.append("\n");
				}
			}
		}
		
		// HACK: add a largely blank file container row as the MGF will not
		// provide any suitable data for this table
	    ncols = file_container.getTableSpec().getNumColumns();
	    DataCell[] cells = new DataCell[ncols];
	    assert(ncols == 9);
	    	
	    cells[0] = DataType.getMissingCell();
	    cells[1] = DataType.getMissingCell();
	    cells[2] = DataType.getMissingCell();
	   
	    cells[3] = DataType.getMissingCell();
	  
	    cells[4] = DataType.getMissingCell();
	    cells[5] = DataType.getMissingCell();
	    cells[6] = DataType.getMissingCell();
	    cells[7] = DataType.getMissingCell();
	    cells[8] = safe_cell(m_filename);
	    
	    file_container.addRowToTable(new DefaultRow(new RowKey(file_seq.get()), cells));
}

	/**
	 * Called with the data for a single spectra at a time, this routine must update the 
	 * spectra cells and add the row as appropriate. This code is pretty ugly so as to handle
	 * the variability and flexibility in what may or may not be specified in the file.
	 * 
	 * @param header
	 * @param peak_list
	 */
	protected void process_spectra(String header, String peak_list, int n_peaks,
			RowSequence sseq, boolean load_spectra, int ncols, DataContainer c) {
		assert(header != null && peak_list != null);
		MyMGFPeakList mgf = new MyMGFPeakList();
		
		// 1. process the headers
		for (String line : header.split("\\n")) {
			int pos = line.indexOf('=');
			if (pos >= 0) {
				String key = line.substring(0, pos);
				String val = line.substring(pos+1).trim();
			
				mgf.addHeader(key, val);
			}
		}
		
		// 2. process the peak list
		double[] mz = new double[n_peaks];
		double[] intensity = new double[n_peaks];
		int cnt = 0;
		boolean has_intensity = true;
		for (String line : peak_list.split("\\n")) {
			String[] fields = line.split("\\s+");
			if (fields.length > 1) {
				mz[cnt] = Double.parseDouble(fields[0]);
				intensity[cnt] = Double.parseDouble(fields[1]);
			} else {
				has_intensity = false;
				mz[cnt] = Double.parseDouble(fields[0]);
			}
			cnt++;
		}
		mgf.setPeaks(mz, has_intensity ? intensity : null);
		
		DataCell[] cells = new DataCell[ncols];
		for (int i=0; i<ncols; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[21] = new StringCell(m_filename);
		if (ncols > 23) {
			cells[23] = SpectraUtilityFactory.createCell(mgf);
		}
		cells[22] = new IntCell(mgf.getNumPeaks());
		
		String pepmass = mgf.getPepmass_safe();
		if (pepmass != null)
			cells[13] = new DoubleCell(Double.parseDouble(pepmass));
		else 
			cells[13] = DataType.getMissingCell();
		String charge = mgf.getCharge_safe();
		if (charge != null) {
			charge    = charge.trim().replaceAll("\\+", "");
			if (charge.length() > 0)
				cells[10] = new IntCell(Integer.parseInt(charge));
			else
				cells[10] = DataType.getMissingCell();
		}
		cells[0]  = new StringCell(mgf.getTitle_safe());
		
		c.addRowToTable(new DefaultRow(sseq.get(), cells));
	}
	
	@Override
	public void setInput(String filename) throws Exception {
		try {
			m_is = BufferedFileReader.createNewReader(new FileInputStream(new File(filename)));
		} catch (Exception e) {
			m_is = null;
			NodeLogger.getLogger(SpectraReaderNodeModel.class).warn("Cannot open "+filename+", reason: "+e);
		}
	}

}
