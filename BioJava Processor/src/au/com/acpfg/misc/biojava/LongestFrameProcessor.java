package au.com.acpfg.misc.biojava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.SimpleSymbolList;
import org.biojava.bio.symbol.SymbolList;
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

public class LongestFrameProcessor implements BioJavaProcessorInterface {
	private boolean m_start_codon, m_stop_codon;
	private boolean m_convert_to_protein;
	private boolean m_forward, m_reverse;
	
	public LongestFrameProcessor(BioJavaProcessorNodeModel m, String task) {
		m_convert_to_protein = (task != null && task.trim().toLowerCase().endsWith("AA)")) ? true : false;
		m_forward = (task != null && (task.indexOf("(all") > 0 || task.indexOf("(3 forward") > 0)) ? true : false;
		m_reverse = (task != null && (task.indexOf("(all") > 0 || task.indexOf("(3 reverse") > 0)) ? true : false;
	}
	
	@Override
	public void execute(BioJavaProcessorNodeModel m, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		if (!m.areSequencesDNA()) {
			throw new InvalidSettingsException("Only DNA sequences are currently supported. Re-configure...");
		}
		int n_rows = inData[0].getRowCount();
		int done = 0;
		RowIterator it = inData[0].iterator();
		Pattern p = Pattern.compile("ATG((?:[^T]..)+?)T((?:GA)|(?:AG)|(?:AA))");
		SymbolList rev_syms = null;
		String rev_seq = null;
		
		while (it.hasNext()) {
			DataRow r = it.next();
			// compute distance from methionine AA to stop codon
			String seq= m.getSequence(r).toUpperCase();
			if (m_reverse) {
				rev_syms = DNATools.complement(DNATools.createDNA(seq)); // rev_syms is lowercase...
				rev_seq  = new StringBuffer(rev_syms.seqString()).reverse().toString().toUpperCase();
			}
			String seq_best = "";
			int    seq_dist = -1;
			int    seq_frame= 0;
			int found_start = 0;
			int found_stop  = 0;
			
			for (int offset=0; offset < 3; offset++) {
				// try the forward frame first 
				if (m_forward) {
					String     rf = seq.substring(offset, seq.length() - (seq.length() - offset) % 3);
					int best_dist = find_best_dist(p, rf);
					if (best_dist > seq_dist) {
						seq_dist     = best_dist;
						seq_best     = rf;
						seq_frame    = offset+1;
						found_start += m_start_codon ? 1 : 0;
						found_stop  += m_stop_codon  ? 1 : 0;
					}
				}
				
				// now the reverse
				if (m_reverse) {
					String rf = rev_seq.substring(offset, seq.length() - (seq.length() - offset) % 3);
					//l.info(rf.substring(0,100));
					int best_dist = find_best_dist(p, rf);
					if (best_dist > seq_dist) {
						seq_dist     = best_dist;
						seq_best     = rf;
						seq_frame    = -(offset + 1);
						found_start += m_start_codon ? 1 : 0;
						found_stop  += m_stop_codon  ? 1 : 0;
					}
				}
			}
			
			DataCell[] cells = new DataCell[4];
			if (seq_dist >= 0) {
				cells[0] = dna2cell(seq_best);
				cells[1] = new IntCell(seq_frame);
				cells[2] = new IntCell(found_start);
				cells[3] = new IntCell(found_stop);
			} else {
				cells[0] = DataType.getMissingCell();
				cells[1] = new IntCell(1);
				cells[2] = new IntCell(0);
				cells[3] = new IntCell(0);
			}
			DataRow      row = new DefaultRow(r.getKey(), cells);
			c.addRowToTable(new JoinedRow(r, row));
			
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done) / n_rows, "Processed "+done+" sequences.");
			}
		}
	}

	protected StringCell dna2cell(String rf) throws Exception {
		StringCell ret;
		if (m_convert_to_protein) {
			SymbolList syms = DNATools.createDNA(rf);
			syms = DNATools.toRNA(syms);
			// ensure multiple of 3 (trim excess)
			if (syms.length() % 3 != 0) {
				syms = syms.subList(1, syms.length() - (syms.length() % 3));
			}
			SymbolList prot = RNATools.translate(syms);
			ret = new StringCell(prot.seqString());
		} else {
			ret = new StringCell(rf);
		}
		return ret;
	}
	
	protected int find_best_dist(Pattern p, String rf) {
		Matcher matcher = p.matcher(rf);
		int best_dist   = -1;
		int base        = 0;
		
		/*
		 * if the DNA sequence does not contain a stop codon, it's best distance extends from the start codon to
		 * the end of the sequence, otherwise we use the pattern p to determine the best distance for the transcript
		 */
		boolean found_start = false;
		boolean found_stop  = false;
		
		while ((base = rf.indexOf("ATG", base)) >= 0) {
			if (base % 3 != 0) {
				base++;
				continue;
			}
			found_start = true;
			
			// no stop codon available?
			if (rf.indexOf("TAA", base+3) < 0 && rf.indexOf("TAG", base+3) < 0 && rf.indexOf("TGA", base+3) < 0) {
				m_start_codon = true;
				m_stop_codon  = false;
				return rf.length() - base;
			}
			
			// else compute distance in terms of the number of nucleotides between start & stop codons
			boolean found = false;
			while (matcher.find(base)) {
				String dist = matcher.group(1);
				int     len = dist.length();
				if (len > best_dist) {
					best_dist = len;
				}
				base      += len + 3;
				found      = true;
				found_stop = true;
			}
			if (!found) {
				base += 3;		// ensure no infinite loop by skipping to next codon
			}
		}
		
		m_start_codon = found_start;
		m_stop_codon  = found_stop;
		
		// if we could not find a start codon, the we compute the distance to any available stop codon instead
		if (! m_start_codon) {
			int end_codon1 = -1;
			int end_codon2 = -1;
			int end_codon3 = -1;
		    base = 0;
			while ((end_codon1 = rf.indexOf("TAA", base)) >= 0) {
				if (end_codon1 % 3 == 0)
					break;
				base = end_codon1 + 3;
			}
			base = 0;
			while ((end_codon2 = rf.indexOf("TAG", base)) >= 0) {
				if (end_codon2 % 3 == 0) 
					break;
				base = end_codon2 + 3;
			}
			base = 0;
			while ((end_codon3 = rf.indexOf("TGA", base)) >= 0) {
				if (end_codon3 % 3 == 0) 
					break;
				base = end_codon3 + 3;
			}
			if (end_codon1 >= 0 || end_codon2 >= 0 || end_codon3 >= 0) {
				if (end_codon1 < 0)
					end_codon1 = Integer.MAX_VALUE;
				if (end_codon2 < 0)
					end_codon2 = Integer.MAX_VALUE;
				if (end_codon3 < 0)
					end_codon3 = Integer.MAX_VALUE;
				best_dist = Math.min(end_codon1, end_codon2);
				best_dist = Math.min(end_codon3, best_dist);
				m_stop_codon = true;
			}
		}
		
		return best_dist;
	}
	
	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[4];
		
        allColSpecs[0] = 
            new DataColumnSpecCreator("Longest Reading Frame Sequence", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
        	new DataColumnSpecCreator("Chosen Frame", IntCell.TYPE).createSpec();
        allColSpecs[2] = 
        	new DataColumnSpecCreator("Start codons (total across reading frames)", IntCell.TYPE).createSpec();
        allColSpecs[3] =
        	new DataColumnSpecCreator("Stop codons (total across reading frames)", IntCell.TYPE).createSpec();
      //  allColSpecs[4] =
        //	new DataColumnSpecCreator("debug", ListCell.getCollectionType(IntCell.TYPE)).createSpec();
        return new DataTableSpec(allColSpecs);
    }

	@Override
	public boolean isMerged() {
		return true;
	}

}
