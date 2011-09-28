package au.com.acpfg.misc.biojava;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

public class SNPFrameshiftDetector implements BioJavaProcessorInterface {
	public SNPFrameshiftDetector(BioJavaProcessorNodeModel m, String task) {
	}
	
	@Override
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer cont)
			throws Exception {
		if (!m.areSequencesDNA()) {
			throw new InvalidSettingsException("Only DNA sequences are supported for now!");
		}
		RowIterator it = inData[0].iterator();
		int done = 0;
		int n_rows = inData[0].getRowCount();
		while (it.hasNext()) {
			DataRow   r = it.next();
			String  seq = m.getSequence(r);
			int seq_len = seq.length();
			
			DataCell[] cells = new DataCell[1];
			if (seq != null && seq_len > 0) {		
				int[] codon_pos = new int[seq_len];
				int codon_idx = 0;
			
				for (int i=0; i<seq_len; i++) {
					char c = seq.charAt(i);
					if (c != 'A' && c != 'T' && c !='G' && c != 'C' ) {
						if (!Character.isLetter(c))
							throw new Exception("Bad char: "+ (int) c+" (encountered in row "+r.getKey()+")");
						codon_pos[codon_idx++] = (i+1) % 3;
					} 
				}
				// according to Andreas, SNPs are mostly in the 3rd nucleotide per codon. If this
				// holds true then a windowed-mode should yield the region of a frame shift.
				// Obviously, this doesn't help much when no SNP's are available or are too sparsely
				// distributed amongst the sequence
				cells[0]         = new IntCell(0);
				StringBuffer codon_str = new StringBuffer();
				for (int i=0; i<codon_idx; i++) {
					codon_str.append(codon_pos[i]);
				}
				// now compute the mode with a window size of 3
				StringBuffer mode_str = new StringBuffer();
				int n_modes = 0;
				for (int i=0; i<codon_str.length()-2; i++) {
					ModeSummary ms = new ModeSummary(codon_str.charAt(i),
								codon_str.charAt(i+1),
								codon_str.charAt(i+2)
							);
					mode_str.append(ms.toString());
					mode_str.append(", ");
					n_modes++;
				}
				int min_percent_modes = (int) (n_modes * 0.1);
				if (n_modes < 3) {
				}
				//cells[1] = new StringCell(mode_str.toString());
			} else {
				cells[0] = DataType.getMissingCell();
				//cells[1] = DataType.getMissingCell();
			}
			cont.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));
			cells = null;
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows, "Completed row "+r.getKey());
			}
		}
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Number of detected frameshifts", IntCell.TYPE).createSpec();
		//cols[1] = new DataColumnSpecCreator("Debug", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
