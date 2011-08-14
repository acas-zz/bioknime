package au.com.acpfg.phylogeny.writer;

/**
 * Thrown when saving an alignment, but we cannot understand the data in the required format.
 * Usually it will mean something has gone wrong with the calculation of the alignment in the first place
 */
public class UnsupportedAlignmentException extends Exception {
    /**
	 *  for Serializable
	 */
	private static final long serialVersionUID = -6841593315921855099L;

	public UnsupportedAlignmentException(String msg) {
		super(msg);
	}
}
