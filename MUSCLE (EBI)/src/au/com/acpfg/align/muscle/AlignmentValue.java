package au.com.acpfg.align.muscle;

import org.knime.core.data.DataValue;

import pal.misc.Identifier;
import pal.datatype.DataType;

/**
 * These methods correspond to the Alignment interface in PAL to enable the cell to use
 * delegate to the alignment value. See the PAL source code for the methods, pretty self-explanatory.
 * set*() methods in class Alignment are not supported as DataCell subclasses must not implement set methods
 * 
 * @author andrew.cassin
 *
 */
public interface AlignmentValue extends DataValue {
	/* ensures right icon, renderers etc. */
    public static final UtilityFactory UTILITY = new AlignmentCellFactory();

    public enum AlignmentType { AL_AA, // amino acid alignment
    		AL_NA					   // nucleotide alignment
    		// other types to follow...
    };
    
    /**
     * Returns the type of alignment. Although PAL defines a DataType for this we cannot
     * expose the type as it creates ClassLoader problems within the KNIME framework. So
     * instead we make an enum and instantiate accordingly. For now, this method supports
     * far fewer alignment types than PAL does, but we'll fix that one day...
     * @return
     */
    public AlignmentType getAlignmentType();
    
	public abstract int getIdCount();

	public Identifier getIdentifier(int arg0);

	public int whichIdNumber(String arg0);

	public String getAlignedSequenceString(int arg0);

	public char getData(int arg0, int arg1);

	public int getSequenceCount();

	public int getSiteCount();
}
