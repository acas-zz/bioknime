package au.com.acpfg.misc.biojava;


import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.symbol.*;


public class AlternateTranslationProcessor implements BioJavaProcessorInterface {
	public TranslationTable[] m_tables;
	public String[]           m_table_names;
	
	public AlternateTranslationProcessor() {
		// load translation tables
		m_tables = new TranslationTable[16];
		m_table_names = new String[m_tables.length];
		m_tables[0] = RNATools.getGeneticCode(TranslationTable.ALT_YEAST_NUC);
		m_table_names[0] = TranslationTable.ALT_YEAST_NUC;
		m_tables[1] = RNATools.getGeneticCode(TranslationTable.ASCID_MITO);
		m_table_names[1] = TranslationTable.ASCID_MITO;
		m_tables[2] = RNATools.getGeneticCode(TranslationTable.BACTERIAL);
		m_table_names[2] = TranslationTable.BACTERIAL;
		m_tables[3] = RNATools.getGeneticCode(TranslationTable.BLEPH_MNUC);
		m_table_names[3] = TranslationTable.BLEPH_MNUC;
		m_tables[4] = RNATools.getGeneticCode(TranslationTable.CHLORO_MITO);
		m_table_names[4] = TranslationTable.CHLORO_MITO;
		m_tables[5] = RNATools.getGeneticCode(TranslationTable.CILIATE_NUC);
		m_table_names[5] = TranslationTable.CILIATE_NUC;
		m_tables[6] = RNATools.getGeneticCode(TranslationTable.ECHIN_MITO);
		m_table_names[6] = TranslationTable.ECHIN_MITO;
		m_tables[7] = RNATools.getGeneticCode(TranslationTable.EUPL_NUC);
		m_table_names[7] = TranslationTable.EUPL_NUC;
		m_tables[8] = RNATools.getGeneticCode(TranslationTable.FWORM_MITO);
		m_table_names[8] = TranslationTable.FWORM_MITO;
		m_tables[9] = RNATools.getGeneticCode(TranslationTable.INVERT_MITO);
		m_table_names[9] = TranslationTable.INVERT_MITO;
		m_tables[10] = RNATools.getGeneticCode(TranslationTable.MOLD_MITO);
		m_table_names[10] = TranslationTable.MOLD_MITO;
		m_tables[11] = RNATools.getGeneticCode(TranslationTable.SCENE_MITO);
		m_table_names[11] = TranslationTable.SCENE_MITO;
		m_tables[12] = RNATools.getGeneticCode(TranslationTable.TREMA_MITO);
		m_table_names[12] = TranslationTable.TREMA_MITO;
		m_tables[13] = RNATools.getGeneticCode(TranslationTable.UNIVERSAL);
		m_table_names[13] = TranslationTable.UNIVERSAL;
		m_tables[14] = RNATools.getGeneticCode(TranslationTable.VERT_MITO);
		m_table_names[14] = TranslationTable.VERT_MITO;
		m_tables[15] = RNATools.getGeneticCode(TranslationTable.YEAST_MITO);
		m_table_names[15] = TranslationTable.YEAST_MITO;
	}
	
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		if (!m.areSequencesDNA()) {
			throw new Exception("This task only works on DNA sequences! (re-configure the node)");
		}
		RowIterator it = inData[0].iterator();
		while (it.hasNext()) {
			DataRow r = it.next();
			String str = m.getSequence(r);
			if (str == null || str.length() < 1)
				continue;
			SymbolList dna = m.getSequenceAsSymbol(str);
			DataCell[] cells = new DataCell[m_tables.length];
			for (int i=0; i<m_tables.length; i++) {
				// trim if not multiple of 3
				if (dna.length() %3 != 0) {
					dna = dna.subList(1, dna.length() - (dna.length() % 3));
				}
				SymbolList rna = DNATools.toRNA(dna);
				SymbolList syms = SymbolListViews.windowedSymbolList(rna, 3);
				SymbolList prot= SymbolListViews.translate(syms, m_tables[i]);
				cells[i] = new StringCell(prot.seqString());
			}
			c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));
		}
	}

	public DataTableSpec get_table_spec() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[m_tables.length];
		for (int i=0; i<m_tables.length; i++) {
			allColSpecs[i] = 
				new DataColumnSpecCreator("Translation "+m_table_names[i], StringCell.TYPE).createSpec();
		}
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
