package au.com.acpfg.misc.biojava;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConstants;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
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
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

public class TrypticPeptideExtractor implements BioJavaProcessorInterface {
	/**
	 * this is the maximum measurable peptide length (in numbers of AA's) which is
	 * likely to be measured on the available instrumentation. Due to the mass range limitation
	 * of most mass spectrometers, this is likely to be a reasonable limit on tryptic peptides
	 */
	public static final int MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA = 30;
	
	// HACK: static codon table - ok for my needs
	protected static HashMap<String,String> aa_map = new HashMap<String,String>();
	static { 
	     aa_map.put("TTT", "F"); aa_map.put("TTC", "F"); aa_map.put("TTA", "L"); aa_map.put("TTG", "L");	// standard DNA codon table (from wikipedia)
		 aa_map.put("CTT", "L"); aa_map.put("CTC", "L"); aa_map.put("CTA", "L"); aa_map.put("CTG", "L");
		 aa_map.put("ATT", "I"); aa_map.put("ATC", "I"); aa_map.put("ATA", "I"); aa_map.put("ATG", "M");
		 aa_map.put("GTT", "V"); aa_map.put("GTC", "V"); aa_map.put("GTA", "V"); aa_map.put("GTG", "V"); 
		 
		 aa_map.put("TCT", "S"); aa_map.put("TCC", "S"); aa_map.put("TCA", "S"); aa_map.put("TCG", "S");
		 aa_map.put("CCT", "P"); aa_map.put("CCC", "P"); aa_map.put("CCA", "P"); aa_map.put("CCG", "P");
		 aa_map.put("ACT", "T"); aa_map.put("ACC", "T"); aa_map.put("ACA", "T"); aa_map.put("ACG", "T");
		 aa_map.put("GCT", "A"); aa_map.put("GCC", "A"); aa_map.put("GCA", "A"); aa_map.put("GCG", "A");
		
		 aa_map.put("TAT", "Y"); aa_map.put("TAC", "Y"); aa_map.put("TAA", "*"); aa_map.put("TAG", "*");
		 aa_map.put("CAT", "H"); aa_map.put("CAC", "H"); aa_map.put("CAA", "Q"); aa_map.put("CAG", "Q");
		 aa_map.put("AAT", "N"); aa_map.put("AAC", "N"); aa_map.put("AAA", "K"); aa_map.put("AAG", "K");
		 aa_map.put("GAT", "D"); aa_map.put("GAC", "D"); aa_map.put("GAA", "E"); aa_map.put("GAG", "E");
		
		 aa_map.put("TGT", "C"); aa_map.put("TGC", "C"); aa_map.put("TGA", "*"); aa_map.put("TGG", "W");
		 aa_map.put("CGT", "R"); aa_map.put("CGC", "R"); aa_map.put("CGA", "R"); aa_map.put("CGG", "R");
		 aa_map.put("AGT", "S"); aa_map.put("AGC", "S"); aa_map.put("AGA", "R"); aa_map.put("AGG", "R");
		 aa_map.put("GGT", "G"); aa_map.put("GGC", "G"); aa_map.put("GGA", "G"); aa_map.put("GGG", "G");
	}; 
	
	
	public TrypticPeptideExtractor(BioJavaProcessorNodeModel m, String task) {
	}
	
	@Override
	public void execute(BioJavaProcessorNodeModel mdl, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		
		// 1. scan the rows to get the available peptides (whether tryptic or not)
		int      n_rows = inData[0].getRowCount();
		double     done = 0.0;
		RowIterator  it = inData[0].iterator();
		boolean is_prot = mdl.areSequencesProtein();
		boolean is_dna  = mdl.areSequencesDNA();
		
		Pattern p = Pattern.compile("([KR][^KR]{6,17}[KR])[^P]");
		while (it.hasNext()) {
			DataRow        r = it.next();
			String[] seqs    = new String[6];
			String[] na_seqs = new String[6];		// RNA reading frames if !is_prot (MUST be in the same frame order as seqs)
			String str = mdl.getSequence(r);
			if (str == null || str.length() < 1)
				continue;
			
			if (!is_prot) {
				SymbolList syms = mdl.getSequenceAsSymbol(str);
			
				for (int i=0; i<3; i++) {
					// take the reading frame
					SymbolList rf = syms.subList(i+1, syms.length()-(syms.length() - i) % 3);
					
					// if it is DNA transcribe it to RNA first
					if (is_dna) {
						rf = DNATools.toRNA(rf);
					}
					if (!is_prot) {
						na_seqs[i+3] = rf.seqString().toUpperCase();
					}
					
					SymbolList prot = RNATools.translate(rf);
					seqs[i+3]       = prot.seqString();
					
					// reverse frame translation
					rf       = RNATools.reverseComplement(rf);
					prot     = RNATools.translate(rf);
					seqs[i]  = prot.seqString();
					if (!is_prot) {
						na_seqs[i] = rf.seqString().toUpperCase();
					}
				}
			} else {
				// only one sequence as there is no frame translation involved
				seqs = new String[] {  mdl.getSequence(r).toUpperCase() };
			}
			
			// process the available protein sequences looking for tryptic peptides
			DataCell[] cells = new DataCell[5];
			cells[0] = new StringCell(mdl.getSequence(r));
			cells[1] = DataType.getMissingCell();
			cells[2] = DataType.getMissingCell();
			cells[3] = DataType.getMissingCell();
			cells[4] = DataType.getMissingCell();
			
			// 1. compute the fully tryptic in-silico peptides present in the available sequence data
			HashSet<String> unambg_tryptics = new HashSet<String>();		// peptides without any IUPAC base call in them (where known)
			HashSet<String> ambg_tryptics = new HashSet<String>();			// peptides which have one or more IUPAC base calls
			HashSet<String> weak_tryptics = new HashSet<String>();			// peptides which have ambiguity at the terminii (are we sure they are K/R)?
			int idx = 0;
			for (String seq : seqs) {
				Matcher m = p.matcher(seq);
				while (m.find()) {
					String    pep = m.group(1);
					int start_pos = m.start(1);
					
					// skip peptides if they are too long to be reliably measured
					if (pep.length() > MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA)
						continue;
					
					// peptide have an unknown residue due to ambiguous (IUPAC) base call at DNA/RNA level?
					if (pep.indexOf('X') >= 0) {
						addAmbiguousPeptides(ambg_tryptics, seq, 
										 is_prot ? null : na_seqs[idx], 
										 pep, start_pos);
					} else if (pep.indexOf('*') < 0) {
						unambg_tryptics.add(pep);
					}
				}
				
				idx++;
			}
			// 1a. if the K/R terminii have IUPAC codes then try to match that
			for (String na_seq : na_seqs) {
				DenseBitVector terminii_ambig_pos   = new DenseBitVector(na_seq.length());
				DenseBitVector terminii_unambig_pos = new DenseBitVector(na_seq.length());
			
				for (int i=0; i<na_seq.length(); i += 3) {
					String codon = na_seq.substring(i, i+3);
					// NB: we dont have to match IUPAC code-free codons, since that has already been done above
					if (codon.matches("^CG[RMKWSBDHVN]") || codon.matches("^AA[RMKWSBDHVN]") || codon.matches("^AG[RMKWSBDHVN]")) {
						terminii_ambig_pos.set(i);
					} else if (codon.equals("AAA") || codon.equals("AAG") || codon.startsWith("CG") || codon.equals("AGA") || codon.equals("AGG")) {
						// K/R encoding positions vector
						terminii_unambig_pos.set(i);
					}
				}
				if (terminii_ambig_pos.cardinality() > 0) {
					// One of two cases is possible:
					// 1. the set bit corresponds to an IUPAC code marks the start of a tryptic peptide
					// 2. the set bit corresponds to an IUPAC code marks the end of a tryptic peptide
					// But the other terminii may or may not be ambiguous too....
					long each_idx = 0;
					while ((each_idx = terminii_ambig_pos.nextSetBit(each_idx)) >= 0) {
						// case 1: tryptic peptide with IUPAC codon at both terminii
						long bit_idx = each_idx+1;
						while ((bit_idx = terminii_ambig_pos.nextSetBit(bit_idx)) >= 0) {
							long codon_idx      = bit_idx - (bit_idx % 3);
							long each_codon_idx = each_idx - (each_idx % 3);
							long codons         = (codon_idx - each_codon_idx) / 3 + 1;
							if (codons >= 8 && codons <= MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								addUnknownTerminiiPeptides(weak_tryptics, new StringBuffer(na_seq.substring((int)each_codon_idx, (int)(each_codon_idx+(codons*3)))), 0);
							} else if (codons > MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								break;
							}
							bit_idx++;
						}
						// case 1: tryptic peptide with IUPAC codon at start, but not at the end
						bit_idx = each_idx+1;
						while ((bit_idx = terminii_unambig_pos.nextSetBit(bit_idx)) >= 0) {
							long codon_idx      = bit_idx - (bit_idx % 3);
							long each_codon_idx = each_idx - (each_idx % 3);
							long codons         = (codon_idx - each_codon_idx) / 3 + 1;
							if (codons >= 8 && codons <= MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								addUnknownTerminiiPeptides(weak_tryptics, new StringBuffer(na_seq.substring((int)each_codon_idx, (int)(each_codon_idx+(codons*3)))), 0);
							} else if (codons > MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								break;
							}
							bit_idx++;
						}
					
						// case 2: tryptic peptide with IUPAC codon at end
						bit_idx = each_idx - (each_idx%3) - (MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA*3);
						if (bit_idx < 0)
							bit_idx = 0;
						while (bit_idx < each_idx && ((bit_idx = terminii_ambig_pos.nextSetBit(bit_idx)) >= 0)) {
							long codon_idx      = bit_idx - (bit_idx % 3);
							long each_codon_idx = each_idx - (each_idx % 3);
							long codons         = (each_codon_idx - codon_idx) / 3 + 1;
							if (codons >= 8 && codons <= MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								addUnknownTerminiiPeptides(weak_tryptics, new StringBuffer(na_seq.substring((int)codon_idx, (int)(codon_idx+(codons*3)))), 0);
							} else if (codons < 8) {
								break;
							}
							bit_idx++;
						}
						// case 2: tryptic peptide with non-ambiguous codon at start
						bit_idx = each_idx - (each_idx%3) - (MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA*3);
						if (bit_idx < 0)
							bit_idx = 0;
						while (bit_idx < each_idx && ((bit_idx = terminii_unambig_pos.nextSetBit(bit_idx)) >= 0)) {
							long codon_idx      = bit_idx - (bit_idx % 3);
							long each_codon_idx = each_idx - (each_idx % 3);
							long codons         = (each_codon_idx - codon_idx) / 3 + 1;
							if (codons >= 8 && codons <= MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								addUnknownTerminiiPeptides(weak_tryptics, new StringBuffer(na_seq.substring((int)codon_idx, (int)(codon_idx+(codons*3)))), 0);
							} else if (codons < 8) {
								break;
							}
							bit_idx++;
						}
						
						
						// next ambiguous base call
						each_idx++;
					}
				}
			}
			

			// ambig_tryptics & unambig_tryptics are now fully populated... so...
			boolean do_missed_cleaves = false;
			if (unambg_tryptics.size() > 0 || ambg_tryptics.size() > 0) {
				cells[1] = populate_cell(unambg_tryptics, true);
				cells[2] = populate_cell(ambg_tryptics, true);
				do_missed_cleaves = true;
			}
			
			// 2. avoid doing expensive missed cleave computation whenever possible
			if (do_missed_cleaves) {
				HashSet<String> mc_tryptics = new HashSet<String>();
				HashSet<String> all_tryptics = new HashSet<String>();
				all_tryptics.addAll(unambg_tryptics);
				all_tryptics.addAll(ambg_tryptics);
				all_tryptics.addAll(weak_tryptics);
				
				for (String s2 : seqs) {
					for (String tryptic : all_tryptics) {
						Pattern p2 = Pattern.compile("([KR]"+tryptic+")");
						Matcher m = p2.matcher(s2);

						// single missed cleavage at either terminii
						while (m.find()) {
							mc_tryptics.add(m.group(1));
						}
						p2 = Pattern.compile("("+tryptic+"[KR])");
						m = p2.matcher(s2);
						while (m.find()) {
							mc_tryptics.add(m.group(1));
						}
						
						// accept a "missed" cleavage involving a proline after [KR] at N-terminus
						// provided total length is ok (ie. likely to be measured by Mass Spec.)
						p2 = Pattern.compile("("+tryptic+"[^KR]{1,17}[KR])");
						m = p2.matcher(s2);
						while (m.find()) {
							String pep = m.group(1);
							if (pep.length() < MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								mc_tryptics.add(pep);
							}
						}
						p2 = Pattern.compile("([KR][^KR]{1,17}"+tryptic+")");
						m = p2.matcher(s2);
						while (m.find()) {
							String pep = m.group(1);
							if (pep.length() < MAX_MEASURABLE_PEPTIDE_LENGTH_IN_AA) {
								mc_tryptics.add(pep);
							}
						}
					}
				}
				
				if (mc_tryptics.size() > 0) {
					cells[3] = populate_cell(mc_tryptics, true);
				}
				
				if (weak_tryptics.size() > 0) {
					cells[4] = populate_cell(weak_tryptics, true);
				}
			}
			
			c.addRowToTable(new DefaultRow(r.getKey(), cells));
			
			done++;
			if (((int)done) % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(done / n_rows);
			}
		}
		
		
	}


	protected DataCell populate_cell(HashSet<String> tryptics, boolean no_stop_codons) {
		if (tryptics.size() < 1) {
			return DataType.getMissingCell();
		}
		ArrayList<StringCell> tryptic_cells = new ArrayList<StringCell>(tryptics.size());
		for (String cell_str : tryptics) {
			if (!no_stop_codons || cell_str.indexOf("*") < 0)
				tryptic_cells.add(new StringCell(cell_str));
		}
		return CollectionCellFactory.createListCell(tryptic_cells);
	}

	/**
	 * Similar to addAmbiguousPeptides() this method handles ambiguity in the first/last codon of the peptide
	 * @param ambg_tryptics
	 * @param codons
	 * @param sb
	 * @param start_pos
	 */
	protected void addUnknownTerminiiPeptides(HashSet<String> ambg_tryptics, StringBuffer codons, int start_pos) throws InvalidIUPACException {
		if (start_pos == 0) {
			codons = new StringBuffer(codons.toString().replaceAll("U", "T"));
			String codon = codons.substring(0, 3);
			
			if (codon.startsWith("AA")) {
				codons.setCharAt(2, 'A');
				addUnknownTerminiiPeptides(ambg_tryptics, codons, start_pos+3);
				return;
			} else if (codon.startsWith("CG") || codon.startsWith("AG")) {
				codons.setCharAt(2, 'G');
				addUnknownTerminiiPeptides(ambg_tryptics, codons, start_pos+3);
				return;
			} else {
				throw new InvalidIUPACException("Expected K/R encoding codon at start, found "+codon+" in "+codons);
			}
		}
		
		recurse(ambg_tryptics, codons, start_pos, codons.length());
	}

	/** 
	 * A key method for calculation of the tryptics - it is only called when an unknown (X)
	 * AA residue is known to exist in the current peptide. This routine is responsible for
	 * examining the IUPAC code(s) that are the cause of the X and add possible variants
	 * to the tryptics set. The nucleotide sequence, <code>na_seq</code> which was frame translated
	 * from <code>seq</code> is also available, it is up to the implementation to decide if it should
	 * use it or not. <code>pep</code> is the peptide with the unknown residue, <code>start_pos</code>
	 * is the position in <code>seq</code> where the peptide starts (if the peptide occurs
	 * multiple times in a given sequence, there will be separate calls of the method for each)
	 * 
	 * @param tryptics
	 * @param seq          Always amino acid sequence. Some sort of translation from <code>na_seq</code>
	 * @param na_seq	   Only non-null if the input sequence is DNA/RNA.
	 * @param pep
	 * @param start_pos
	 */
	protected void addAmbiguousPeptides(HashSet<String> tryptics, String seq,
			String na_seq, String pep, int start_pos) throws InvalidIUPACException {
		
		int x_count = 0;
		for (int i=0; i<pep.length(); i++) {
			if (pep.charAt(i) == 'X')
				x_count++;
		}
		
		na_seq = na_seq.replaceAll("U", "T");
	
		// if nucleotide sequence not available, then nothing that can be done to resolve the ambiguity... so...
		if (na_seq == null)
			return;
		
		// otherwise look for IUPAC codes in the codons corresponding to the peptide and
		// try to perform all permutations
		int spos = start_pos * 3;
		StringBuffer na_codons = new StringBuffer(na_seq.substring(spos, spos+pep.length()*3));
		assert(na_codons.length() % 3 == 0);
				
		recurse(tryptics, na_codons, 0, na_codons.length());
	}

	protected void recurse(HashSet<String> tryptics, StringBuffer na_codons, int i, int length) throws InvalidIUPACException {
		if (i >= length) {
			// translate na_codons to AA and then add it since we've resolve IUPAC codes along the entire length
			StringBuffer pep = new StringBuffer();
			for (int j=0; j<na_codons.length(); j+=3) {
				String key = ""+na_codons.charAt(j)+na_codons.charAt(j+1)+na_codons.charAt(j+2);
				String aa = aa_map.get(key.toUpperCase());
				if (aa == null) 
					throw new InvalidIUPACException("Invalid codon: "+key.toUpperCase());
				pep.append(aa);
			}
			// stop codon probably indicates an invalid peptide, so we dont add these to the list
			if (pep.indexOf("*") < 0)
				tryptics.add(pep.toString());
			return;
		}
		
		char c = na_codons.charAt(i);
		
		if (c == 'A' || c == 'C' || c == 'T' || c == 'U' || c == 'G') {
			recurse(tryptics, na_codons, i+1, length);
			return;
		}
		// else need to resolve the code via recursion
		if (c == 'R') {
			na_codons.setCharAt(i, 'A');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'G');
			recurse(tryptics, na_codons, i, length);
		} else if (c == 'Y') {
			na_codons.setCharAt(i, 'C');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'T');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'M') {
			na_codons.setCharAt(i, 'C');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'A');
			recurse(tryptics, na_codons, i, length);
		} else if (c == 'K') {
			na_codons.setCharAt(i, 'T');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'G');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'W') {
			na_codons.setCharAt(i, 'T');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'A');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'S') {
			na_codons.setCharAt(i, 'C');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'G');
			recurse(tryptics, na_codons, i, length);
		} else if (c == 'B') {
			na_codons.setCharAt(i, 'T');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'C');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'G');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'D') {
			na_codons.setCharAt(i, 'A');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'T');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'G');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'H') {
			na_codons.setCharAt(i, 'A');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'T');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'C');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'V') {
			na_codons.setCharAt(i, 'A');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'C');
			recurse(tryptics, na_codons, i, length);
			na_codons.setCharAt(i, 'G');
			recurse(tryptics, na_codons, i, length);
			// no need to worry about U
		} else if (c == 'N') {
			for (char c2 : new char[] { 'A', 'C', 'G', 'T'} ) {
				na_codons.setCharAt(i, c2);
				recurse(tryptics, na_codons, i, length);
			}
		} else {
			throw new InvalidIUPACException("Unknown IUPAC code: "+c);
		}
	}

	@Override
	public DataTableSpec get_table_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[5];
		cols[0] = new DataColumnSpecCreator("Input Sequence",   StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Unambiguous Tryptic Peptides", ListCell.getCollectionType(StringCell.TYPE) ).createSpec();
		cols[2] = new DataColumnSpecCreator("Ambiguous Tryptic Peptides", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[3] = new DataColumnSpecCreator("Single missed cleavage Tryptic Peptides", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[4] = new DataColumnSpecCreator("Weak Ambiguous Tryptic Peptides", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public boolean isMerged() {
		return false;
	}

}


