package au.com.acpfg.misc.muscle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;

import pal.alignment.Alignment;
import pal.alignment.AlignmentReaders;
import pal.alignment.AlignmentUtils;
import pal.datatype.AminoAcids;
import pal.misc.Identifier;


public class MultiAlignmentCell extends DataCell implements AlignmentValue, Serializable {
	/**
	 * This class uses java.io.Serializable rather than the KNIME serialisation code. Although
	 * somewhat slower, the PAL library uses java.io.Serializable so we reuse their code 
	 */
	private static final long serialVersionUID = -3581196464617337119L;
	
	/** 
	 * Convenience for users of this cell type
	 */
	public static final DataType TYPE = DataType.getType(MultiAlignmentCell.class);

	private Alignment             m_a;
  
	public MultiAlignmentCell(String fasta) throws IOException {
		this(fasta, new AminoAcids());
	}
	
	/**
	 * Constructs an alignment cell from the specified aligned sequences (in FASTA format)
	 * and using the specified type of sequence (which is usually an instance of either 
	 * @ref{SpecificAminoAcids} or @ref{IUPACNucleotides}) from PAL
	 * 
	 * @param  aligned sequences in FASTA format
	 * @param dt    codon table and sequence type, amongst other information to use to interpret the sequences
	 * @throws IOException
	 */
	public MultiAlignmentCell(String fasta, pal.datatype.DataType dt) throws IOException {
		m_a = AlignmentReaders.readFastaSequences(new StringReader(fasta), dt);
	}

	/**
	 * Persists the internal state regardless of the class of Alignment and/or DataType
	 * @param out
	 * @throws IOException
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(m_a);
	}
	
	/**
	 * Loads the internal state regardless of the class of Alignment and/or DataType. Order must be the
	 * same as the objects are persisted (see writeObject())
	 * 
	 * @param in
	 * @throws IOException
	 */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    	m_a              = (Alignment) in.readObject();
    }
	
	@Override
	public int hashCode() {
		return m_a.hashCode();
	}
	
	public String getFormattedAlignment(FormattedRenderer.FormatType format) {
		if (m_a == null)
			return "";
		StringWriter sw = new StringWriter(100 * 1024);
		PrintWriter pw = new PrintWriter(sw);
		if (format == FormattedRenderer.FormatType.F_CLUSTALW) {
			AlignmentUtils.printCLUSTALW(m_a, pw);
		} else if (format == FormattedRenderer.FormatType.F_PHYLIP_SEQUENTIAL) {
			AlignmentUtils.printSequential(m_a, pw);
		} else if (format == FormattedRenderer.FormatType.F_PHYLIP_INTERLEAVED) {
			AlignmentUtils.printInterleaved(m_a, pw);
		} else {
			AlignmentUtils.printPlain(m_a, pw);
		}
		pw.close();
		return sw.toString();
	}
	
	@Override
	public String toString() {
		StringWriter sw = new StringWriter(100 * 1024);
		PrintWriter pw = new PrintWriter(sw);
		AlignmentUtils.report(m_a, pw);
		pw.close();
		return sw.toString();
	}
	
	@Override
	protected boolean equalsDataCell(DataCell dc) {
		return (this == dc);
	}

	/******** ALIGNMENT INTERFACE METHODS **********/
	
	@Override
	public int getIdCount() {
		return m_a.getIdCount();
	}

	@Override
	public Identifier getIdentifier(int arg0) {
		return m_a.getIdentifier(arg0);
	}

	@Override
	public int whichIdNumber(String arg0) {
		return m_a.whichIdNumber(arg0);
	}

	@Override
	public String getAlignedSequenceString(int arg0) {
		return m_a.getAlignedSequenceString(arg0);
	}

	@Override
	public char getData(int arg0, int arg1) {
		return m_a.getData(arg0, arg1);
	}

	@Override
	public pal.datatype.DataType getDataType() {
		return m_a.getDataType();
	}

	@Override
	public int getSequenceCount() {
		return m_a.getSequenceCount();
	}

	@Override
	public int getSiteCount() {
		return m_a.getSiteCount();
	}

}
