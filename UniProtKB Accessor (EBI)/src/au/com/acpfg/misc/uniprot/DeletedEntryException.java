package au.com.acpfg.misc.uniprot;

public class DeletedEntryException extends Exception {

	/**
	 * for serialisation, not for normal use
	 */
	private static final long serialVersionUID = -5537482140312037794L;

	public DeletedEntryException(String msg) {
		super(msg);
	}
}
