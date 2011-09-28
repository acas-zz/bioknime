package au.com.acpfg.misc.biojava;

import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.proteomics.*;
import org.biojava.bio.symbol.*;


public class FrameTranslationProcessor implements BioJavaProcessorInterface {
	private boolean m_incl_na_seqs;		// include NA frames for use by later processing steps
	
	public FrameTranslationProcessor(String task) {
		m_incl_na_seqs = false;
		if (task.toLowerCase().endsWith("(incl. na frames)")) {
			m_incl_na_seqs = true;
		}
	}
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec, NodeLogger logger,
			BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		
		if (m.areSequencesProtein()) {
			throw new Exception("Cannot perform this task with protein sequences!");
		}
		
		int n_rows = inData[0].getRowCount();
		int done   = 0;
		RowIterator it = inData[0].iterator();
		boolean is_dna = m.areSequencesDNA();
	
		int ncols = 6;
		if (m_incl_na_seqs) {
			ncols += 6;
		}
		
		while (it.hasNext()) {
			DataRow r = it.next();
			String str = m.getSequence(r);
			if (str == null || str.length() < 1)
				continue;
			SymbolList syms = m.getSequenceAsSymbol(str);
			DataCell[] cells = new DataCell[ncols];
		
			for (int i=0; i<3; i++) {
				// take the reading frame
				SymbolList rf = syms.subList(i+1, syms.length()-(syms.length() - i) % 3);
				
				// if it is DNA transcribe it to RNA first
				if (is_dna) {
					rf = DNATools.toRNA(rf);
				}
				
				SymbolList prot = RNATools.translate(rf);
				cells[i+3] = new StringCell(prot.seqString());
				if (m_incl_na_seqs) {
					cells[i+9] = new StringCell(rf.seqString().toUpperCase());
				}
				
				// reverse frame translation
				rf       = RNATools.reverseComplement(rf);
				prot     = RNATools.translate(rf);
				cells[i] = new StringCell(prot.seqString());
				if (m_incl_na_seqs) {
					cells[i+6] = new StringCell(rf.seqString().toUpperCase());
				}
			}
			
			// add all the cells into the row
			DataRow      row = new DefaultRow(r.getKey(), cells);
			c.addRowToTable(new JoinedRow(r, row));
			
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done) / n_rows, "Processed "+done+" sequences.");
			}
		}
	}

	public DataTableSpec get_table_spec() {
		int add_na = 0;
		if (m_incl_na_seqs)
			add_na = 6;
		DataColumnSpec[] allColSpecs = new DataColumnSpec[6+add_na];
		
        allColSpecs[0] = 
            new DataColumnSpecCreator("Translation Frame -3", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Translation Frame -2", StringCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("Translation Frame -1", StringCell.TYPE).createSpec();
        allColSpecs[3] = 
            new DataColumnSpecCreator("Translation Frame +1", StringCell.TYPE).createSpec();
        allColSpecs[4] =
        	new DataColumnSpecCreator("Translation Frame +2", StringCell.TYPE).createSpec();
        allColSpecs[5] = 
            new DataColumnSpecCreator("Translation Frame +3", StringCell.TYPE).createSpec();
        if (m_incl_na_seqs) {
        	 allColSpecs[6] = 
                 new DataColumnSpecCreator("NA Frame -3", StringCell.TYPE).createSpec();
             allColSpecs[7] = 
                 new DataColumnSpecCreator("NA Frame -2", StringCell.TYPE).createSpec();
             allColSpecs[8] =
             	new DataColumnSpecCreator("NA Frame -1", StringCell.TYPE).createSpec();
             allColSpecs[9] = 
                 new DataColumnSpecCreator("NA Frame +1", StringCell.TYPE).createSpec();
             allColSpecs[10] =
             	new DataColumnSpecCreator("NA Frame +2", StringCell.TYPE).createSpec();
             allColSpecs[11] = 
                 new DataColumnSpecCreator("NA Frame +3", StringCell.TYPE).createSpec();
        }
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
