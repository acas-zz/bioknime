package au.com.acpfg.io.genbank.reader;

/**
 * Wrapper class which is thrown when a record is encountered which does not
 * meet mandatory genbank format requirements eg. missing accession in a given record
 * 
 * @author andrew.cassin
 *
 */
public class InvalidGenbankRecordException extends Exception {
	public InvalidGenbankRecordException(String msg) {
		super(msg);
	}
}
