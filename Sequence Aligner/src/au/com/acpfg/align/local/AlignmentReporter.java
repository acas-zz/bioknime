package au.com.acpfg.align.local;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.muscle.MultiAlignmentCell;

import pal.alignment.AlignmentUtils;
import pal.alignment.SimpleAlignment;

import neobio.alignment.PairwiseAlignment;
import jaligner.Alignment;
import jaligner.formats.CLUSTAL;
import jaligner.formats.FASTA;
import jaligner.formats.FormatFactory;
import jaligner.formats.Pair;

public class AlignmentReporter {
	private String m_a1, m_a2;			   // accessions for the pair of sequences
	private String m_s1, m_s2;			   // sequences to be aligned
	private MyAlignment m_alignment;
	
	/**
	 * Constructor to get all non-provider specific fields initialised...
	 * @param a1
	 * @param s1
	 * @param a2
	 * @param s2
	 */
	protected AlignmentReporter(String a1, String s1, String a2, String s2) {
		m_a1 = a1;
		m_a2 = a2;
		m_s1 = s1;
		m_s2 = s2;
	}
	
	/**
	 * Use this constructor if JAligner is the provider being used.
	 * @param a
	 * @param a1
	 * @param s1
	 * @param a2
	 * @param s2
	 */
	public AlignmentReporter(Alignment a, String a1, String s1, String a2, String s2) {
		this(a1, s1, a2, s2);
		assert(a != null);
		
		// gotta be careful with namespace pollution argh...
		pal.misc.Identifier[] ids = new pal.misc.Identifier[] { 
				new pal.misc.Identifier(a1),
				new pal.misc.Identifier(a2)	};
		
		m_alignment = new MyAlignment(ids, 
				new String[] {new String(a.getSequence1()), new String(a.getSequence2())},
				a.getScore());
		m_alignment.setTagLine(convert2neobio(a.getMarkupLine()));
	}
	
	public AlignmentReporter(Alignment a, String s1, String s2) {
		this(a, "s1", s1, "s2", s2);
	}
	
	/**
	 * Use this constructor if NeoBio is the provider being used
	 * @param a
	 * @param a1
	 * @param s1
	 * @param a2
	 * @param s2
	 */
	public AlignmentReporter(PairwiseAlignment a, String a1, String s1, String a2, String s2) {
		this(a1, s1, a2, s2);
		assert(a != null);
		// gotta be careful with namespace pollution argh...
		pal.misc.Identifier[] ids = new pal.misc.Identifier[] {
				new pal.misc.Identifier(a1), 
				new pal.misc.Identifier(a2)	};
		m_alignment = new MyAlignment(ids, 
				new String[] { a.getGappedSequence1(), a.getGappedSequence2() },
				a.getScore());
		m_alignment.setTagLine(a.getScoreTagLine());
	}
	
	public AlignmentReporter(PairwiseAlignment a, String s1, String s2) {
		this(a, "s1", s1, "s2", s2);
	}
	
	public static DataTableSpec getTableSpec(String[] wanted) throws InvalidSettingsException {
		assert(wanted != null && wanted.length > 0);
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		for (String want : wanted) {
			if (want.equals("Alignment Cell")) {
				cols.add(new DataColumnSpecCreator("Alignment Cell", MultiAlignmentCell.TYPE).createSpec());
			} else if (want.equals("Accessions")) {
				cols.add(new DataColumnSpecCreator("Accession #1", StringCell.TYPE).createSpec());
				cols.add(new DataColumnSpecCreator("Accession #2", StringCell.TYPE).createSpec());
			} else if (want.equals("Original Sequences")) {
				cols.add(new DataColumnSpecCreator("Original Sequence #1", StringCell.TYPE).createSpec());
				cols.add(new DataColumnSpecCreator("Original Sequence #2", StringCell.TYPE).createSpec());
			} else if (want.equals("Gapped Sequences")) {
				cols.add(new DataColumnSpecCreator("Gapped Sequence #1", StringCell.TYPE).createSpec());
				cols.add(new DataColumnSpecCreator("Gapped Sequence #2", StringCell.TYPE).createSpec());
			} else if (want.equals("Tag Line")) {
				cols.add(new DataColumnSpecCreator("Tag Line", StringCell.TYPE).createSpec());
			} else if (want.equals("Gap count (Sequence1)") || want.equals("Gap count (Sequence2)")
					   || want.equals("Gap count (sum of both sequences)")) {
				cols.add(new DataColumnSpecCreator(want, IntCell.TYPE).createSpec());
			} else if (want.equals("Identities (%)") || want.startsWith("Similarities ")) {
				cols.add(new DataColumnSpecCreator(want, DoubleCell.TYPE).createSpec());
			} else if (want.equals("Total Gaps (Sequence1)") ||
					   want.equals("Total Gaps (Sequence2)")) {
				cols.add(new DataColumnSpecCreator(want, IntCell.TYPE).createSpec());
			} else if (want.equals("Score")) {
				cols.add(new DataColumnSpecCreator(want, DoubleCell.TYPE).createSpec());
			} else if (want.endsWith("(BitVector)") || want.startsWith("Gap regions")) {
				cols.add(new DataColumnSpecCreator(want, DenseBitVectorCell.TYPE).createSpec());
			} else if (want.startsWith("Extent")) {
				cols.add(new DataColumnSpecCreator(want, DoubleCell.TYPE).createSpec());
			} else if (want.startsWith("Alignment in")) {
				cols.add(new DataColumnSpecCreator(want, StringCell.TYPE).createSpec());
			} else {
				throw new InvalidSettingsException("Unknown alignment datum: "+want);
			}
		}
		return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
	}
	
	public DataRow getRow(int align_no, DataTableSpec spec) throws IOException {
		DataCell[] cells = new DataCell[spec.getNumColumns()];
		for (int i=0; i<spec.getNumColumns(); i++) {
			DataColumnSpec col = spec.getColumnSpec(i);
			String task = col.getName().toLowerCase();
			if (task.equals("accession #1")) {
				cells[i] = new StringCell(m_a1);
			} else if (task.equals("accession #2")) {
				cells[i] = new StringCell(m_a2);
			} else if (task.equals("original sequence #1")) {
				cells[i] = new StringCell(getSequence1());
			} else if (task.equals("original sequence #2")) {
				cells[i] = new StringCell(getSequence2());
			} else if (task.equals("gapped sequence #1")) {
				cells[i] = new StringCell(getGappedSequence(true));
			} else if (task.equals("gapped sequence #2")) {
				cells[i] = new StringCell(getGappedSequence(false));
			} else if (task.equals("score")) {
				cells[i] = new DoubleCell(getScore());
			} else if (task.equals("tag line")) {
				cells[i] = new StringCell(getTagLine());
			} else if (task.equals("total gaps (sequence1)")) {
				cells[i] = new IntCell(getGapLength(true));
			} else if (task.equals("total gaps (sequence2)")) {
				cells[i] = new IntCell(getGapLength(false));
			} else if (task.equals("identities (%)")) {
				cells[i] = new DoubleCell(((double)getIdentityMatches()) / getTagLine().length() * 100.0);
			} else if (task.startsWith("similarities ")) {
				cells[i] = new DoubleCell(((double)getSimilarityMatches()) / getTagLine().length()*100.0);
			} else if (task.equals("extent (%sequence1)")) {
				cells[i] = new DoubleCell(getExtent(true));
			} else if (task.equals("extent (%sequence2)")) {
				cells[i] = new DoubleCell(getExtent(false));
			} else if (task.startsWith("identical regions")) {
				cells[i] = getIdentityBitVectorCell();
			} else if (task.startsWith("similar regions")) {
				cells[i] = getSimilarBitVectorCell();
			} else if (task.startsWith("gap regions")) {
				cells[i] = getGappedBitVectorCell(task);
			} else if (task.startsWith("gap regions")) {
				if (task.endsWith("sequence1)")) {
					cells[i] = new IntCell(getGapStarts(true));
				} else if (task.endsWith("sequence2)")) {
					cells[i] = new IntCell(getGapStarts(false));
				} else {
					int sum = getGapStarts(true);
					sum    += getGapStarts(false);
					cells[i]= new IntCell(sum);
				}
			} else if (task.startsWith("alignment in blast")) {
				cells[i] = new StringCell(get_alignment("blast"));
			} else if (task.startsWith("alignment in clustal")) {
				cells[i] = new StringCell(get_alignment("clustalw"));
			} else if (task.startsWith("alignment in fasta")) {
				cells[i] = new StringCell(get_alignment("fasta"));
			} else if (task.startsWith("alignment cell")) {
				cells[i] = new MultiAlignmentCell(get_alignment("fasta"));
			} else {
				cells[i] = DataType.getMissingCell();
			}
		}
		return new DefaultRow("A"+align_no, cells);
	}

	/**
	 * Returns the alignment in the requested format. TODO: incomplete
	 * 
	 * @param required_format one of "blast", "clustalw" and "fasta" for now
	 * @return string representation of this alignment in desired format
	 */
	private String get_alignment(String required_format) {
		StringWriter sw = new StringWriter(100 * 1024);
		PrintWriter pw = new PrintWriter(sw);
		if (required_format.equals("clustalw")) {
			AlignmentUtils.printCLUSTALW(m_alignment, pw);
		} else if (required_format.equals("blast")) {
			// TODO... produce a proper blast format
			AlignmentUtils.printPlain(m_alignment, pw);
		} else if (required_format.equals("fasta")) {
			for (int i=0; i<m_alignment.getSequenceCount(); i++) {
				pw.println(">"+m_alignment.getIdentifier(i).getName());
				pw.println(m_alignment.getAlignedSequenceString(i));
			}
		}
		pw.close();
		return sw.toString();
	}

	public double getScore() {
		return m_alignment.getScore();
	}

	public String getGappedSequence(boolean is_1) {
		if (is_1) {
			return m_alignment.getAlignedSequenceString(0);
		} else {
			return m_alignment.getAlignedSequenceString(1);
		}
	}
	
	public String getSequence1() {
		return m_s1;	
	}
	
	public String getSequence2() {
		return m_s2;
	}
	
	public int getIdentityMatches() {
		String tags = getTagLine();
		int cnt = 0;
		for (int i=0; i<tags.length(); i++) {
			if (Character.isLetter(tags.charAt(i))) {
				cnt++;
			}
		}
		return cnt;
	}
	
	public int getSimilarityMatches() {
		String tags = getTagLine();
		int cnt = 0;
		for (int i=0; i<tags.length(); i++) {
			if (tags.charAt(i) == '+')
				cnt++;
		}
		return cnt;
	}
	
	public int getGapStarts(boolean is_1) {
		String seq = getGappedSequence(is_1);
		Pattern p = Pattern.compile("[A-Z]\\-");
		Matcher m = p.matcher(seq);
		int base = 0;
		int cnt = 0;
		while (m.find(base)) {
			cnt++;
			base = m.start() + 1;
		}
		return cnt;
	}
	
	public double getExtent(boolean from_seq1) {
		int length = getGappedSequence(from_seq1).length();
		int gaps   = getGapLength(from_seq1);
		assert(gaps < length && gaps >= 0 && length > 0);
		int seq_len= from_seq1 ? m_s1.length() : m_s2.length();
		return ((double) length - gaps) / seq_len * 100.0;
	}
	
	public int getGapLength(boolean from_seq1) {
		String gapped_seq = getGappedSequence(from_seq1);
		int cnt = 0;
		for (int i=0; i<gapped_seq.length(); i++) {
			if (gapped_seq.charAt(i) == '-')
				cnt++;
		}
		return cnt;
	}
	
	public String getTagLine() {
		return m_alignment.getTagLine();
	}
	
	protected String convert2neobio(char[] jalign_markup) {
		StringBuffer sb = new StringBuffer();
		String gseq = getGappedSequence(true);
		for (int i=0; i<jalign_markup.length; i++) {
			if (jalign_markup[i] == '|') { // identity? if so, replace with letter from gapped sequence
				sb.append(gseq.charAt(i));
			} else if (jalign_markup[i] == ':') { // similarity? replace with '+'
				sb.append('+');
			} else if (jalign_markup[i] == '.') { // mismatch? for consistency with neobio we make it blank
				sb.append(' ');
			} else if (jalign_markup[i] == ' ') { // gap?
				sb.append(' ');
			} else {							  // leave untouched, but should not happen!
				sb.append(jalign_markup[i]);
			}
		}
		assert(sb.length() == gseq.length());
		return sb.toString();
	}
	
	public DenseBitVectorCell getIdentityBitVectorCell() {
		String tags = getTagLine();
		DenseBitVector bv = new DenseBitVector(tags.length());
		for (int i=0; i<tags.length(); i++) {
			if (Character.isLetter(tags.charAt(i))) 
				bv.set(i);
		}
		DenseBitVectorCellFactory f = new DenseBitVectorCellFactory(bv);
		return f.createDataCell();
	}
	
	public DenseBitVectorCell getSimilarBitVectorCell() {
		String tags = getTagLine();
		DenseBitVector bv = new DenseBitVector(tags.length());
		for (int i=0; i<tags.length(); i++) {
			if (tags.charAt(i) == '+') 
				bv.set(i);
		}
		DenseBitVectorCellFactory f = new DenseBitVectorCellFactory(bv);
		return f.createDataCell();
	}
	
	public DenseBitVectorCell getGappedBitVectorCell(String task) {
		String seq1 = getGappedSequence(true);
		String seq2 = getGappedSequence(false);
		DenseBitVector bv = new DenseBitVector(seq1.length());
		assert(seq1.length() == seq2.length());
		
		if (task.endsWith("(sequence1)")) {
			for (int i=0; i<seq1.length(); i++) {
				if (seq1.charAt(i) == '-') 
					bv.set(i);
			}
		} else if (task.endsWith("(sequence2)")) {
			for (int i=0; i<seq2.length(); i++) {
				if (seq2.charAt(i) == '-') 
					bv.set(i);
			}
		} else if (task.endsWith("(union)")) {
			for (int i=0; i<seq2.length(); i++) {
				if (seq2.charAt(i) == '-' || seq1.charAt(i) == '-') 
					bv.set(i);
			}
		} else if (task.endsWith("(intersection)")) {
			for (int i=0; i<seq2.length(); i++) {
				if (seq2.charAt(i) == '-' && seq1.charAt(i) == '-') 
					bv.set(i);
			}
		}
		DenseBitVectorCellFactory f = new DenseBitVectorCellFactory(bv);
		return f.createDataCell();
	}
}
