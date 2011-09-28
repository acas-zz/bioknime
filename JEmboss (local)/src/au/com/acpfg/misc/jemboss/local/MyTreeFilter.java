package au.com.acpfg.misc.jemboss.local;

public interface MyTreeFilter {
	/**
	 * Sole method in the interface: accept the specified tree node? 
	 * Returns <code>true</code> if yes, <code>false</code> otherwise
	 */
	public boolean accepts(Object node);
}
