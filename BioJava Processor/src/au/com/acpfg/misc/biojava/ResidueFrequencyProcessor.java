package au.com.acpfg.misc.biojava;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.biojava.bio.dist.IndexedCount;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.AlphabetManager;
import org.biojava.bio.symbol.AtomicSymbol;
import org.biojava.bio.symbol.FiniteAlphabet;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 *  Speed is important here, for large sequence databases (eg. short reads from Solexa etc.)
 *  
 * @author acassin
 *
 */
public class ResidueFrequencyProcessor implements BioJavaProcessorInterface {
	private boolean m_single_residue;
	private BioJavaProcessorNodeModel m_owner;
	private HashMap<String, Integer> m_colmap;		    // maps column name (ie. symbol name) to a corresponding column id

	public ResidueFrequencyProcessor(BioJavaProcessorNodeModel owner, String task) {
		m_single_residue = task.equals("Count Residues");
		m_owner          = owner;
		m_colmap         = new HashMap();
	}
	
	@Override
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		
		RowIterator it = inData[0].iterator();
		int n = inData[0].getRowCount();
		int i = 0;
		int[] vec = new int[m_colmap.size()];
		String[] id = new String[m_colmap.size()];
		
		// populate id array
		Iterator iid = m_colmap.keySet().iterator();
		int j = 0;
		while (iid.hasNext()) {
			String col_id = (String) iid.next();
			id[j++] = col_id;
		}
		
		// process rows for user's dataset
		if (m_single_residue) {
			while (it.hasNext()) {
				DataRow r = it.next();
				i++;
				
				String seq = m.getSequence(r);
				
				DataCell[] cells = new DataCell[vec.length];
				for (int k=0; k<vec.length; k++) {
					int cnt = 0;
					String colname = id[k];
					assert(colname.length() == 1);
					char ch = colname.charAt(0);
					for (int m2=0; m2<seq.length(); m2++) {
						if (seq.charAt(m2) == ch) 
							cnt++;
					}
					
					if (m_colmap.containsKey(colname)) {
						Integer column_idx = m_colmap.get(colname);
						cells[column_idx.intValue()] = new IntCell(cnt);
					}
				}
				
				c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));

				if (i % 1000 == 0) {
					exec.checkCanceled();
					exec.setProgress(((double) i)/n, "Processed "+i+" sequences");
				}
			}	
		} else {
			// di-mer/di-peptide composition?
			while (it.hasNext()) {
				DataRow r = it.next();
				i++;
				
				String seq = m.getSequence(r).trim().toUpperCase();
				
				int[] cells = new int[vec.length];
				for (int k=0; k<cells.length; k++) {
					cells[k] = 0;
				}
				for (int k=0; k<seq.length()-1; k++) {
					StringBuffer sb = new StringBuffer();
					sb.append(seq.charAt(k));
					sb.append(seq.charAt(k+1));
					String dimer = sb.toString();
 					
					if (m_colmap.containsKey(dimer)) {
						Integer column_idx = m_colmap.get(dimer);
						cells[column_idx.intValue()]++;
					}
				}
				
				DataCell[] knime_cells = new DataCell[cells.length];
				for (int k=0; k<cells.length; k++) {
					knime_cells[k] = new IntCell(new Integer(cells[k]));
				}
				c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), knime_cells)));

				if (i % 1000 == 0) {
					exec.checkCanceled();
					exec.setProgress(((double) i)/n, "Processed "+i+" sequences");
				}
			}	
		}
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols;
		
		// decide output columns based on the type of sequences being analysed
	
		char[] vec;
		
		if (m_owner.areSequencesProtein()) {
			vec = new char[] {'A', 'R', 'N', 'D', 'C', 'Q', 'E', 'G', 'H', 'I', 'L', 'K', 'M', 'F', 'P', 'S', 'T', 'W', 'Y', 'V' };
		} else if (m_owner.areSequencesDNA()) {
			vec = new char[] {'A', 'C', 'G', 'T', 'N' }; // BUG: support all IUPAC ambiguity conventions?
		} else if (m_owner.areSequencesRNA()) {
			vec = new char[] {'A', 'C', 'G', 'U' };
		} else {
			System.err.println("ResidueFrequencyProcessor.java: unsupported sequence type! Aborting execution!");
			return null;
		}
	
		int k = 0;
		for (char i : vec) {
			for (char j : vec) {
				StringBuffer tmp = new StringBuffer();
				tmp.append(i);
				if (!m_single_residue) {
					tmp.append(j);
				}
				String as_str = tmp.toString();
				if (!m_single_residue || (m_single_residue && i==j)) {
					// handle symettry eg. AA
					if (!m_colmap.containsKey(as_str)) {
						//System.err.println(as_str+ " "+k);
						m_colmap.put(as_str, new Integer(k));
						k++;
					}
				}
			}
		}
		
		// columns are built from the final map to avoid duplicates
		int n_cols = m_colmap.size();
		cols = new DataColumnSpec[n_cols];
		Set<String> colnames = m_colmap.keySet();
		
		for (String colname : colnames) {
			k = m_colmap.get(colname).intValue();
			cols[k] = new DataColumnSpecCreator(colname, IntCell.TYPE).createSpec();
		}
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
