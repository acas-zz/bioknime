package au.com.acpfg.xml.query;

import org.knime.core.data.DataCell;

/**
 * Ensures an object provides the necessary methods to integrate with a traversal of 
 * XQuery hits.
 * 
 * @author andrew.cassin
 *
 */
public interface XQueryReporterInterface {
	
	public void reset();
	
	public DataCell getCell(String colname);
}
