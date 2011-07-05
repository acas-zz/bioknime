package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComboBox;

/**
 * Displays a row in the configure dialog which represents a single XQuery and how the results are
 * to be displayed to the user
 * 
 * @author andrew.cassin
 *
 */
public class XMLQueryEntry {
	
	/**
	 * Any change to this enum must be reflected in the methods below so that the configure dialog and user interaction
	 * is correct. Be careful, since any change must be backwards compatible with existing workflows as the queries are saved!
	 * @author andrew.cassin
	 *
	 */
	public enum ResultsType { 
		RAW_XML, 			// XQuery result fragment (string cell) with one row per result per query per file
		RAW_XML_COLLECTION,	// KNIME LIST cell with all results in the collection for a given XQuery for a given file
		TEXT, 				// xml with tags removed
		TEXT_COLLECTION, 	// each element in results as a separation list item (KNIME collection cell)
		XMLATTR_COLLECTION,	// list of attributes in the selected element(s) (KNIME collection cell)
		RESULTS_COUNT, 		// number of matching elements
		ELEMENTS_AS_COLUMNS,// direct childen elements within the matching element are turned into KNIME columns (may add many columns!)
		ELEMENT_DISTRIBUTION // how many times each element occurs within the hit
		// more to come in future...
	};
	
	private String      m_query;            // multi-line supported
	private String      m_name;	            // name for the query (human assigned)
	private ResultsType[] m_types;	        // what results are expected for the query
	private boolean     m_fail_if_empty;	// fail parsing if no records match query (0 hits)
	private boolean     m_disabled;			// do not execute() query if true, but show in list
	
	
	public XMLQueryEntry(String name, String xquery, ResultsType[] rt, boolean fail_if_empty) {
		m_name          = name;
		m_query         = xquery;
		m_types         = rt;
		m_fail_if_empty = fail_if_empty;
		m_disabled      = false;
	}
	
	public XMLQueryEntry(String name, String xquery, ResultsType result_format, boolean fail_if_empty) {
		this(name, xquery, new ResultsType[] { result_format }, fail_if_empty);
	}
	
	public XMLQueryEntry(String name, String xquery, ResultsType result_format) {
		this(name, xquery, result_format, true);
	}
	
	public XMLQueryEntry(String name, String xquery) {
		this(name, xquery, ResultsType.RAW_XML);
	}
	
	public XMLQueryEntry(int n) {
		this("Query"+n, "//some/element");
	}
	
	/**
	 * Returns the list of combobox items in the same order as defined in enum ResultTypes
	 * 
	 * @return guaranteed non-null
	 */
	public static String[] rt2items() {
		ResultsType[] vals = ResultsType.values();
		String[] vec = new String[vals.length];
		int idx = 0;
		for (ResultsType rt : vals) {
			vec[idx++] = colname(rt);
		}
		return vec;
	}

	/** 
	 * Returns the enum value which corresponds to the combobox item in the supplied parameter
	 * 
	 * @param  selectedItem the english text which appears in the combobox
	 * @return 
	 */
	public static ResultsType[] item2rt(Object[] selectedItems) {
		HashSet<ResultsType> wanted = new HashSet<ResultsType>();
		
		for (Object o : selectedItems) {
			String s = o.toString();
			if (s.equals("XQuery Result")) {
				wanted.add(ResultsType.RAW_XML);
			} else if (s.equals("XQuery Result (list)")) {
				wanted.add(ResultsType.RAW_XML_COLLECTION);
			} else if (s.equals("Result Count")) {
				wanted.add(ResultsType.RESULTS_COUNT);
			} else if (s.equals("Text only")) {
				wanted.add(ResultsType.TEXT);
			} else if (s.equals("Text only (list)")) {
				wanted.add(ResultsType.TEXT_COLLECTION);
			} else if (s.startsWith("Elements as columns")) {
				wanted.add(ResultsType.ELEMENTS_AS_COLUMNS);
			} else if (s.startsWith("Element Distribution")) {
				wanted.add(ResultsType.ELEMENT_DISTRIBUTION);
			} else {
				wanted.add(ResultsType.XMLATTR_COLLECTION);
			}
		}
		return wanted.toArray(new ResultsType[0]);
	}
	 
	/**
	 * Returns the index (combobox list position, zero relative) which corresponds to the specified
	 * <code>enum ResultsType</code> value
	 * 
	 * @param t_result_types
	 * @param results
	 * @return
	 */
    protected static int[] rt2idx(ResultsType[] wanted_results) {
    	HashSet<Integer> vec = new HashSet<Integer>();
    	
    	// NB: this return value must correspond to the defined <code>enum ResultsType</code> value order
    	for (ResultsType rt : wanted_results) {
			if (rt == ResultsType.RESULTS_COUNT) {
				vec.add(new Integer(5));
			} else if (rt == ResultsType.TEXT) {
				vec.add(new Integer(2));
			} else if (rt == ResultsType.TEXT_COLLECTION) {
				vec.add(new Integer(3));
			} else if (rt == ResultsType.XMLATTR_COLLECTION) {
				vec.add(new Integer(4));
			} else if (rt == ResultsType.RAW_XML_COLLECTION) {
				vec.add(new Integer(1));
			} else if (rt == ResultsType.ELEMENTS_AS_COLUMNS) {
				vec.add(new Integer(6));
			} else if (rt == ResultsType.ELEMENT_DISTRIBUTION) {
				vec.add(new Integer(7));
			} else {
				vec.add(new Integer(0));
			}
    	}
    	
    	int[] ret = new int[vec.size()];
    	int idx = 0;
    	for (Integer i : vec) {
    		ret[idx++] = i.intValue();
    	}
    	return ret;
	}
    
    /**
     * Return the name for the column (which corresponds to the combobox entry in the configure dialog)
     * which corresponds to the specified <code>enum ResultsType</code>
     *  
     * @param rt
     * @return null is returned for unknown enum values (should not happen in normal circumstances)
     */
	public static String colname(ResultsType rt) {
		if (rt == ResultsType.RAW_XML) {
			return "XQuery Result";
		} else if (rt == ResultsType.ELEMENTS_AS_COLUMNS) {
			return "Elements as columns";		// column names are the XML names of the elements in the result so this wont appear in the output table
		} else if (rt == ResultsType.RAW_XML_COLLECTION) {
			return "XQuery Result (list)";
		} else if (rt == ResultsType.RESULTS_COUNT) {
			return "Result Count";
		} else if (rt == ResultsType.TEXT) {
			return "Text only";
		} else if (rt == ResultsType.TEXT_COLLECTION) {
			return "Text only (list)";
		} else if (rt == ResultsType.XMLATTR_COLLECTION) {
			return "Attributes (list)";
		} else if (rt == ResultsType.ELEMENT_DISTRIBUTION) {
			return "Element Distribution";
		}
		return null;
	}
	
	/**
	 * constructor which reverse the serialisation performed by toString()
	 * @param xqe_serialised
	 */
	public XMLQueryEntry(String xqe_serialised) {
		String[] lines = xqe_serialised.split("\n");
		StringBuffer sb = new StringBuffer();
		for (int i=4; i<lines.length; i++) {
			sb.append(lines[i]);
			sb.append("\n");
		}
		m_name          = lines[0];
		m_types         = map2results(lines[1].split(","));
		m_fail_if_empty = lines[2].equals("true");
		m_disabled      = lines[3].equals("true");
		m_query         = sb.toString();
	}
	
	private static ResultsType[] map2results(String[] wanted_list) {
		HashSet<ResultsType> vec = new HashSet<ResultsType>();
		for (String s : wanted_list) {
			if (s.equals("RAW_XML")) 
				vec.add(ResultsType.RAW_XML);
			else if (s.equals("RAW_XML_COLLECTION")) 
				vec.add(ResultsType.RAW_XML_COLLECTION);
			else if (s.equals("TEXT")) 
				vec.add(ResultsType.TEXT);
			else if (s.equals("TEXT_COLLECTION")) 
				vec.add(ResultsType.TEXT_COLLECTION);
			else if (s.equals("XMLATTR_COLLECTION"))
				vec.add(ResultsType.XMLATTR_COLLECTION);
			else if (s.equals("ELEMENTS_AS_COLUMNS"))
				vec.add(ResultsType.ELEMENTS_AS_COLUMNS);
			else if (s.equals("ELEMENT_DISTRIBUTION")) 
				vec.add(ResultsType.ELEMENT_DISTRIBUTION);
			else 
				vec.add(ResultsType.RESULTS_COUNT);
		}
		return vec.toArray(new ResultsType[0]);
	}

	public String getName() {
		return m_name;
	}
	
	public void setName(String new_name) {
		m_name = new_name;
	}
	
	public String getQuery() {
		return m_query;
	}
	
	public void setQuery(String new_query) {
		m_query = new_query;
	}
	
	public ResultsType[] getWantedResults() {
		return m_types;
	}
	
	public Set<ResultsType> getWantedResultsSet() {
		HashSet<ResultsType> ret = new HashSet<ResultsType>();
		
		for (ResultsType rt : m_types) {
			ret.add(rt);
		}
		
		return ret;
	}
	
	public void setResults(ResultsType[] new_results) {
		m_types = new_results;
	}
	
	
	@Override
	public String toString() {
		ResultsType[] wanted = getWantedResults();
		StringBuffer sb = new StringBuffer(1024);
		int idx = 0;
		for (ResultsType w : wanted) {
			sb.append(w.toString());
			if (idx++ < wanted.length-1)
				sb.append(',');				// NB: must correspond to de-serialisation constructor (see above)
		}
		return getName()+"\n"+sb.toString()+"\n"+m_fail_if_empty+"\n"+m_disabled+"\n"+getQuery();
	}
	
	public boolean isEnabled() {
		return !m_disabled;
	}
	
	public void setEnabled(boolean new_val) {
		m_disabled = !new_val;
	}

	public boolean getFailEmpty() {
		return m_fail_if_empty;
	}
	
	/**
	 * Set to true if the node should abort execution if the XQuery does not match anything (may indicate
	 * XML format has changed)
	 * @param new_fail if set to true, node will abort if the XQuery matches nothing in the current document
	 */
	public void setFailEmpty(boolean new_fail) {
		m_fail_if_empty = new_fail;
	}
}
