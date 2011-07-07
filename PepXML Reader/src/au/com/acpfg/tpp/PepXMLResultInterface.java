/**
 * 
 */
package au.com.acpfg.tpp;

/**
 * @author andrew.cassin
 *
 * Contains the necessary state for search/retrieval of key attributes of a given PepXML document,
 * without specifying how the data is represented by an object.
 */
public interface PepXMLResultInterface {
	
	// which search engine produced the results?
	public boolean hasSearchEngine(String engine);
	
	/**
	 * If an engine exists in the results which is not one of (Mascot, XTandem or Sequest) this function
	 * will return true, false otherwise. The code supports more output for these three "main" search engines
	 * than others. If you are using something else, consider modifying the code
	 * @return
	 */
	public boolean hasOtherSearchEngine();
	
	// add a new engine (if not already present) to the list of search engines comprising the results
	public void addSearchEngine(String new_engine);
}
