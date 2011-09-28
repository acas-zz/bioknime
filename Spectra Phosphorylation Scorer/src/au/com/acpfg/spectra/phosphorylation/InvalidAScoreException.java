package au.com.acpfg.spectra.phosphorylation;

/**
 * thrown when the AScore calculation determines an invalid state, will halt execution of the node
 * 
 * @author andrew.cassin
 *
 */
public class InvalidAScoreException extends Exception {
	public InvalidAScoreException(String msg) {
		super(msg);
	}
}
