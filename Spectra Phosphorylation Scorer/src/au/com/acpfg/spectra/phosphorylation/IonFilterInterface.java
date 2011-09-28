package au.com.acpfg.spectra.phosphorylation;

/**
 * Implementations of this interface will be called by <code>make_ions()</code>
 * during ion selection to determine if the ion should be returned to the caller.
 * Since the PeptideScore and AScore use the similar code exception for ions used to match against,
 * this permits code reuse
 * 
 * 
 * @author andrew.cassin
 *
 */
public interface IonFilterInterface {
	/**
	 * Return <code>true</code> if the ion is to be returned, <code>false</code> otherwise
	 * 
	 * @param i guaranteed to be non-null
	 * @return
	 */
	public abstract boolean accept(Ion i);
	
}
