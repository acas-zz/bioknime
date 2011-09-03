package au.com.acpfg.pfa.interproscan;

/**
 * This gets thrown when a job fails (perhaps due to the input data) at EBI
 * @author andrew.cassin
 *
 */
public class FailedJobException extends Exception {
	public FailedJobException(String s) {
		super(s);
	}
}
