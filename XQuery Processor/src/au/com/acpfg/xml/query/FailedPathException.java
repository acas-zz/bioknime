package au.com.acpfg.xml.query;

import java.io.File;

/**
 * Thrown when an XQuery which is required to match content does not.
 * @author andrew.cassin
 *
 */
public class FailedPathException extends Exception {

	public FailedPathException(File fname, String message) {
		this(message+": "+fname.getAbsolutePath());
	}
	
	public FailedPathException(String id, String message) {
		this(message+": "+id);
	}
	
	public FailedPathException(String msg) {
		super(msg);
	}
}
