package au.com.acpfg.proteomics;

/**
 * Thrown when a peptide is encountered which has not been entered into the peptide universe ie.
 * the unique peptides are not all known as the dataset has been incorrectly enterered into the universe
 * 
 * @author andrew.cassin
 *
 */
public class UnknownPeptideException extends Exception {

	/**
	 * Serializable interface
	 */
	private static final long serialVersionUID = -7846285188890300696L;

	public UnknownPeptideException(String msg) {
		super(msg);
	}
}
