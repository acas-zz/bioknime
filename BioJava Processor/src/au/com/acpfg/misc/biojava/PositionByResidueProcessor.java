package au.com.acpfg.misc.biojava;

import java.util.ArrayList;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * Adds columns for each position (up to the specified maximum length in the constructor) 
 * with a cell for each residue in each column. Each cells contains the number of residues at that
 * position. Positions are numbered from 1 to correspond to what biologists expect ;-)
 * 
 * @author andrew.cassin
 *
 */
public class PositionByResidueProcessor implements BioJavaProcessorInterface {

	private BioJavaProcessorNodeModel m_owner;
	private int m_maxlen;
	
	
	public PositionByResidueProcessor(BioJavaProcessorNodeModel m, String task, int maxlen) {
		assert(m != null && maxlen > 0);
		m_maxlen = maxlen;
		m_owner = m;
	}
	
	@Override
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		RowIterator it = inData[0].iterator();
		
		ResidueByPosition[] pos = new ResidueByPosition[m_maxlen];
		for (int i=0; i<m_maxlen; i++) {
			pos[i] = new ResidueByPosition(i+1);
		}
		
		// scan the sequences -- speed is key here
		int done = 0;
		int n_rows = inData[0].getRowCount();
		while (it.hasNext()) {
			DataRow  r = it.next();
			String seq = m.getSequence(r).toUpperCase();
			int len = m_maxlen;
			if (seq.length() < m_maxlen)
				len = seq.length();
			for (int i=0; i<len; i++ ) {
				pos[i].bump(seq.charAt(i));
			}
			if (done % 1000 == 0) {
				exec.setProgress((double) done / n_rows);
				exec.checkCanceled();
			}
			done++;
		}
		
		// build the output table...
		char[] letters = pos[0].getResidueLetters();
		for (int i=0; i<letters.length; i++) {
			DataCell[] row = new DataCell[m_maxlen];
			for (int j=0; j<m_maxlen; j++) {
				row[j] = new IntCell(pos[j].count(letters[i]));
			}
			c.addRowToTable(new DefaultRow(new RowKey(new Character(letters[i]).toString()), row));
		}
		
		// all done
		c.close(); 
	}

	@Override
	public DataTableSpec get_table_spec() {
		assert(m_maxlen > 0);
		DataColumnSpec[] cols = new DataColumnSpec[m_maxlen];
		for (int i=0; i<m_maxlen; i++) {
			cols[i] = new DataColumnSpecCreator("Position "+new Integer(i+1).toString(), IntCell.TYPE).createSpec();
		}
		return new DataTableSpec(cols);
	}

	public class ResidueByPosition {
		private int m_pos;
		private final char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',  
				'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
		private int[]  count;
		
		public ResidueByPosition(int i) {
			m_pos = i;
			count = new int[] {0, 0, 0, 0, 0, 0,
					 0, 0, 0, 0, 0, 0,
					 0, 0, 0, 0, 0, 0,
					 0, 0, 0, 0, 0, 0, 0, 0};	
		}
		
		public char[] getResidueLetters() {
			return letters;
		}
	
		public void bump(char c) {
			assert(c >= 'A' && c <= 'Z');
			int offset = c - 'A';
			count[offset]++;
		}

		public int count(char c) {
			assert(c >= 'A' && c <= 'Z');
			int offset = c - 'A';
			return count[offset];
		}
	}

	@Override
	public boolean isMerged() {
		return false;
	}

}
