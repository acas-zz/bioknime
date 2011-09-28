package au.com.acpfg.misc.biojava;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

public class WeightedHomopolymerRateProcessor implements
		BioJavaProcessorInterface {

	public WeightedHomopolymerRateProcessor(BioJavaProcessorNodeModel m,String task) {
		
	}
	
	@Override
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		if (!m.areSequencesDNA()) {
			throw new InvalidSettingsException("Only DNA sequences are currently supported. No ambiguity in DNA sequence is permitted.");
		}
		RowIterator it = inData[0].iterator();
		int done = 0;
		int n_rows = inData[0].getRowCount();
		int total_bad = 0;
		while (it.hasNext()) {
			DataRow r = it.next();
			String seq = m.getSequence(r).trim().toUpperCase();
			// calculation as per http://www.broadinstitute.org/crd/wiki/index.php/Homopolymer
			int idx = 0;
			int cnt = 0;
			int sum = 0;
			int seq_len = seq.length();
			boolean bad = false;
			while (idx < seq_len) {
				cnt++;
				char residue = seq.charAt(idx);
				if (residue != 'A' && residue != 'C' && residue != 'T' && residue != 'G') {
					bad = true;
				}
				idx++;
				int len = 1;
				while (idx < seq_len && seq.charAt(idx) == residue) {
					idx++;
					len++;
				}
				sum += len * len;
			}
			DataCell[] cells = new DataCell[1];
			if (bad) {	
				cells[0] = DataType.getMissingCell();
				total_bad++;
			} else {
				double whr = ((double)sum) / cnt;
				cells[0] = new DoubleCell(whr);
			}
			c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows, "Computed WHR for "+r.getKey());
			}
		}
		
		if (total_bad > 0) {
			l.warn(""+total_bad+" sequences had ambiguous/unknown base calls, they are ignored (missing WHR).");
		}
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Weighted Homopolymer Rate (WHR)", DoubleCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
