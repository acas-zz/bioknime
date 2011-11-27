package au.com.acpfg.misc.fasta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;

/**
 * This class assumes each sequence for a given file is presented as a block (which is fine for this node) to save
 * memory for the median/mean calculations.
 * 
 * @author andrew.cassin
 *
 */
public class SequenceStatistics {
	private final File m_for_file;
	private int m_min, m_max;
	private int m_n, m_n_1kb, m_n_10kb, m_n_100kb;
	private int m_total, m_total_1kb, m_total_10kb, m_total_100kb;
	private final ArrayList<Integer> m_lengths = new ArrayList<Integer>(10*1000);
	private static int m_id = 1;	// bumped for each row added to the output container
	
	// members for the sole sharing of calc_nxx() and caller's
	private int m_nxx, m_nxx_length;
	
	/**
	 * Sole constructor which takes a file that the stats relate to
	 * Callers must call <code>grokSequence()</code> for all sequences that are part of this file,
	 * before processing any other file with the invoking object
	 * 
	 * @param f
	 */
	public SequenceStatistics(File f) {
		m_for_file  = f;
		m_min       = Integer.MAX_VALUE;
		m_max       = Integer.MIN_VALUE;
		m_n         = 0;
		m_n_1kb     = 0;	    // number of sequences over 1kb
		m_n_10kb    = 0;
		m_n_100kb   = 0;
		m_total     = 0;	    // total sequence length
		m_total_1kb = 0;	// total sequence length for all sequences at least 1kb
		m_total_10kb= 0;
		m_total_100kb=0;
	}
	
	public boolean isFile(File f) {
		return m_for_file.equals(f);
	}
	
	public static DataTableSpec getOutputSpec() {
		DataColumnSpec[] cols = new DataColumnSpec[31];
		cols[0] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("N", IntCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Minimum", IntCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Maximum", IntCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Mean (rounded)", IntCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Median (actual sequence length)", IntCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Total Length", IntCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Total Length (sequences >1kb only)", IntCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("Total Length (sequences >10kb only)", IntCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("Total Length (sequences >100kb only)", IntCell.TYPE).createSpec();
		cols[10]= new DataColumnSpecCreator("Number of sequences >1kb only", IntCell.TYPE).createSpec();
		cols[11]= new DataColumnSpecCreator("Number of sequences >10kb only", IntCell.TYPE).createSpec();
		cols[12]= new DataColumnSpecCreator("Number of sequences >100kb only", IntCell.TYPE).createSpec();
		
		cols[13]= new DataColumnSpecCreator("N10", IntCell.TYPE).createSpec();
		cols[14]= new DataColumnSpecCreator("N10 Length", IntCell.TYPE).createSpec();
		cols[15]= new DataColumnSpecCreator("N20", IntCell.TYPE).createSpec();
		cols[16]= new DataColumnSpecCreator("N20 Length", IntCell.TYPE).createSpec();
		cols[17]= new DataColumnSpecCreator("N30", IntCell.TYPE).createSpec();
		cols[18]= new DataColumnSpecCreator("N30 Length", IntCell.TYPE).createSpec();
		cols[19]= new DataColumnSpecCreator("N40", IntCell.TYPE).createSpec();
		cols[20]= new DataColumnSpecCreator("N40 Length", IntCell.TYPE).createSpec();
		
		cols[21]= new DataColumnSpecCreator("N50", IntCell.TYPE).createSpec();
		cols[22]= new DataColumnSpecCreator("N50 Length", IntCell.TYPE).createSpec();
		cols[23]= new DataColumnSpecCreator("N60", IntCell.TYPE).createSpec();
		cols[24]= new DataColumnSpecCreator("N60 Length", IntCell.TYPE).createSpec();
		cols[25]= new DataColumnSpecCreator("N70", IntCell.TYPE).createSpec();
		cols[26]= new DataColumnSpecCreator("N70 Length", IntCell.TYPE).createSpec();
		cols[27]= new DataColumnSpecCreator("N80", IntCell.TYPE).createSpec();
		cols[28]= new DataColumnSpecCreator("N80 Length", IntCell.TYPE).createSpec();
		cols[29]= new DataColumnSpecCreator("N90", IntCell.TYPE).createSpec();
		cols[30]= new DataColumnSpecCreator("N90 Length", IntCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}
	
	/**
	 * Adds the cells as defined by <code>getOutputSpec()</code> to the specified container
	 * @param c
	 */
	public void addStats(BufferedDataContainer c) {
		assert(c != null);
		DataCell[] cells = new DataCell[31];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[0] = new StringCell(m_for_file.getName());
		cells[1] = new IntCell(m_n);
		cells[2] = new IntCell(m_min);
		cells[3] = new IntCell(m_max);
		
		// sort the length of sequences for use below
		Collections.sort(m_lengths, new Comparator<Integer>() {

			@Override
			public int compare(Integer arg0, Integer arg1) {
				return arg1.compareTo(arg0);
			}
			
		});
		
		cells[4] = new IntCell(calculate_mean_length(m_lengths));
		cells[5] = new IntCell(calculate_median_length(m_lengths));
		cells[6] = new IntCell(m_total);
		cells[7] = new IntCell(m_total_1kb);
		cells[8] = new IntCell(m_total_10kb);
		cells[9] = new IntCell(m_total_100kb);
		
		cells[10] = new IntCell(m_n_1kb);
		cells[11] = new IntCell(m_n_10kb);
		cells[12] = new IntCell(m_n_100kb);
		
		double fac = 0.1;
		for (int cell_idx = 13; cell_idx < 31; cell_idx += 2) {
			calc_nxx(fac);
			cells[cell_idx]   = new IntCell(m_nxx);
			cells[cell_idx+1] = new IntCell(m_nxx_length);
			fac += 0.1;
		}
		
		c.addRowToTable(new DefaultRow("file"+m_id++, cells));
	}
	
	protected void calc_nxx(double frac) {
		int sum_target = (int) (m_total * frac);
		m_nxx = 0;
		m_nxx_length = 0;
		int so_far = 0;
		for (int i=0; i<m_lengths.size(); i++) {
			int len = m_lengths.get(i).intValue();
			so_far += len;
			m_nxx++;
			m_nxx_length = len;
			if (so_far >= sum_target)
				return;
		}
	}
	
	public void grokSequence(String seq) {
		String tmp = seq.trim().replaceAll("\\s+", "");
		int    len = tmp.length();
		
		// stupid sequences dont count
		if (len < 1)
			return;
		m_total += len;
		m_n++;
		if (len >= 1000) {
			m_total_1kb += len;
			m_n_1kb++;
			if (len >= 10000) {
				m_total_10kb += len;
				m_n_10kb++;
				if (len >= 100000) {
					m_total_100kb += len;
					m_n_100kb++;
				}
			}
		}
		if (len < m_min) {
			m_min = len;
		}
		if (len > m_max) {
			m_max = len;
		}
		m_lengths.add(new Integer(len));
	}
	
	protected int calculate_mean_length(List<Integer> i) {
		double sum = 0.0;
		for (Integer l : i) {
			sum += l.intValue();
		}
		return (((int)Math.round(sum)) / m_n);
	}
	
	/**
	 * Although not strictly a median calculation, we always want an actual length rather than an average if the 
	 * number of sequences is even. Hence this implementation for now.
	 * 
	 */
	protected int calculate_median_length(ArrayList<Integer> i) {
		if (i.size() < 1) {
			return 0;
		}
		boolean is_odd = (m_n % 2 == 1);
		if (is_odd) {
			return i.get(m_n / 2 + 1);
		} else {
			return i.get(m_n / 2);
		}
	}
	
}
