package au.com.acpfg.misc.StringMatcher;

import org.knime.core.data.DataCell;

public interface MatchReporter {
	
	/**
	 * Called during the execute() method, this must provide the report based on the
	 * match results (current state of m) and the string being matched (str). If the 
	 * reporter encounters a problem it may throw an exception to stop executing the node.
	 * 
	 * @param m
	 * @param str
	 * @throws Exception
	 */
	public DataCell report(StringMatcherNodeModel m, String str) throws Exception;
}
