package au.com.acpfg.misc.biojava;

import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;


public class SequenceTranslationProcessor implements BioJavaProcessorInterface {
	private boolean m_convert_dna2prot;
	private boolean m_convert_rna2prot;
	private boolean m_convert_dna2rna;
	
	public SequenceTranslationProcessor(BioJavaProcessorNodeModel m, String task) {
		m_convert_dna2prot = false;
		m_convert_rna2prot = false;
		m_convert_dna2rna  = false;
		task = task.toLowerCase().trim();
		if (task.endsWith("dna to protein sequence")) {
			m_convert_dna2prot = true;
		} else if (task.endsWith("rna to protein sequence")) {
			m_convert_rna2prot = true;
		} else {
			m_convert_dna2rna = true;
		}
	}
	
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		if (m.areSequencesProtein()) {
			throw new Exception("Protein Sequences cannot (currently) be converted!");
		}
		boolean is_rna = m.areSequencesRNA();
		boolean is_dna = m.areSequencesDNA();
		RowIterator it = inData[0].iterator();
		
		if (!m_convert_dna2prot && !m_convert_rna2prot && !m_convert_dna2rna) {
			throw new InvalidSettingsException("Implementation error -- please contact the author of this node!");
		}
		if ((is_rna && !m_convert_rna2prot) ||
				is_dna && !(m_convert_dna2prot || m_convert_dna2rna)) {
			throw new InvalidSettingsException("Sequence type does not match requested task -- reconfigure this node!");
		}
		
		int done = 0;
		int n_rows = inData[0].getRowCount();
		while (it.hasNext()) {
			DataRow r = it.next();
			String seq = m.getSequence(r);
			
			// skip missing sequences -- TODO: should we put into output table?
			if (seq == null || seq.length() < 1)
				continue;
			
			if (m_convert_dna2rna) {
				// convert DNA sequence to RNA
				SymbolList dna = m.getSequenceAsSymbol(seq);
				// ensure multiple of 3 (trim excess)
				if (dna.length() % 3 != 0) {
					dna = dna.subList(1, dna.length() - (dna.length() % 3));
				}
				SymbolList rna = DNATools.toRNA(dna);
				seq = rna.seqString();
			} else if (m_convert_rna2prot){
				// convert RNA to protein
				SymbolList rna = m.getSequenceAsSymbol(seq);
				// ensure multiple of 3 (trim excess)
				if (rna.length() % 3 != 0) {
					rna = rna.subList(1, rna.length() - (rna.length() % 3));
				}
				seq = RNATools.translate(rna).seqString();
			} else if (m_convert_dna2prot) {
				SymbolList syms = DNATools.createDNA(m.getSequence(r));
				syms = DNATools.toRNA(syms);
				// ensure multiple of 3 (trim excess)
				if (syms.length() % 3 != 0) {
					syms = syms.subList(1, syms.length() - (syms.length() % 3));
				}
				SymbolList prot = RNATools.translate(syms);
				seq = prot.seqString();
			} else {
				throw new InvalidSettingsException("Unknown conversion -- implementation bug!");
			}
			DataCell[] cells = new DataCell[1];
			cells[0] = new StringCell(seq);
			c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows, "Processed row "+r.getKey());
			}
		}
	}

	public DataTableSpec get_table_spec() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[1];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Converted Sequence", StringCell.TYPE).createSpec();
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
