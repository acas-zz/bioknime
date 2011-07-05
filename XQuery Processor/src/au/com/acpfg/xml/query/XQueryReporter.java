package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;

/**
 * Class which is used to traverse each raw XQuery output to extract the type of 
 * results desired by the user. A similar interface to <code>class java.lang.StringBuffer</code>
 * but there is no requirement that the traversal do anything with strings
 * 
 * @author andrew.cassin
 *
 */
public class XQueryReporter {
	
	/**
	 * XQuery result callbacks must respond to some/all of these as required
	 * @author andrew.cassin
	 *
	 */
	public enum QueryResponseFragmentType {
		RESP_STRING,
		RESP_INT,
		RESP_START_ELEMENT,			// <element
		RESP_END_ELEMENT_TAG,		// </element>
		RESP_END_TAG,				// >
		RESP_ATTRIBUTE,				// key=value
		RESP_INCOMPLETE_END_ELEMENT // />
	};
	
	private final int MAX_QLEN = 10;		// maximum length of callback vectors
	
	/* lists of callbacks, one array per QueryResponseFragmentType -- speed is important here! */
	private XQueryResponseInterface[] m_cb_string;
	private XQueryResponseInterface[] m_cb_int;
	private XQueryResponseInterface[] m_cb_element_start;
	private XQueryResponseInterface[] m_cb_element_end;
	private XQueryResponseInterface[] m_cb_end_tag;
	private XQueryResponseInterface[] m_cb_attribute;
	private XQueryResponseInterface[] m_cb_incomplete_end_tag;
	
	private final List<XQueryReporterInterface> m_reporters = new ArrayList<XQueryReporterInterface>();
	private final HashMap<String,XQueryReporterInterface> m_report_map = new HashMap<String,XQueryReporterInterface>();
	
	public XQueryReporter() {
		m_cb_string = new XQueryResponseInterface[MAX_QLEN];
		m_cb_int    = new XQueryResponseInterface[MAX_QLEN];
		m_cb_element_start = new XQueryResponseInterface[MAX_QLEN];
		m_cb_element_end = new XQueryResponseInterface[MAX_QLEN];
		m_cb_end_tag = new XQueryResponseInterface[MAX_QLEN];
		m_cb_attribute = new XQueryResponseInterface[MAX_QLEN];
		m_cb_incomplete_end_tag = new XQueryResponseInterface[MAX_QLEN];
		
		for (int i=0; i<MAX_QLEN; i++) {
			m_cb_string[i] = null;
			m_cb_int[i]    = null;
			m_cb_element_start[i] = null;
			m_cb_element_end[i]   = null;
			m_cb_end_tag[i]= null;
			m_cb_attribute[i] = null;
			m_cb_incomplete_end_tag[i] = null;
		}
	}	
	
	public void call(QueryResponseFragmentType type, String attrName, String attrVal) {
		call(type, attrName+"=\""+attrVal+"\"");
	}
	
	public void call(QueryResponseFragmentType type, Integer val) {
		call(type, val.toString());
	}
	
	public void call(String val) {
		call(QueryResponseFragmentType.RESP_STRING, val);
	}
	
	public void call(QueryResponseFragmentType type, String val) {
		XQueryResponseInterface[] vec = null;
		
		switch (type) {
		case RESP_STRING:
			vec = m_cb_string; break;
		case RESP_INT:
			vec = m_cb_int; break;
		case RESP_START_ELEMENT:
			vec = m_cb_element_start; break;
		case RESP_END_ELEMENT_TAG:
			vec = m_cb_element_end; break;
		case RESP_END_TAG:
			vec = m_cb_end_tag; break;
		case RESP_ATTRIBUTE:
			vec = m_cb_attribute; break;
		case RESP_INCOMPLETE_END_ELEMENT:
			vec = m_cb_incomplete_end_tag; break;
		}
		
		if (vec != null) {
			for (int i=0; i<vec.length; i++) {
				if (vec[i] != null) {
					vec[i].callback(type, val);
				} else {
					// vec[i] == null so that means the callbacks for this type have finished... so
					return;
				}
			}
		}
	}
	
	public void register_reporter(XQueryReporterInterface xqri, String colname) throws Exception {
		m_reporters.add(xqri);
		
		if (m_report_map.containsKey(colname)) {
			throw new Exception("Only one reporter can report a given cell -- programmer error!");
		}
		
		m_report_map.put(colname, xqri);
	}
	
	public void reset() {
		for (XQueryReporterInterface r : m_reporters) {
			r.reset();
		}
	}
	
	public DataCell getResultCell(String colname) {
		if (m_report_map.containsKey(colname))
			return m_report_map.get(colname).getCell(colname);
		return DataType.getMissingCell();
	}
	
	public void register_callback(QueryResponseFragmentType type, XQueryResponseInterface xqrc) throws Exception {
		switch (type) {
		case RESP_STRING:
			add(xqrc, m_cb_string); break;
		case RESP_INT:
			add(xqrc, m_cb_int); break;
		case RESP_START_ELEMENT:
			add(xqrc, m_cb_element_start); break;
		case RESP_END_ELEMENT_TAG:
			add(xqrc, m_cb_element_end); break;
		case RESP_END_TAG:
			add(xqrc, m_cb_end_tag); break;
		case RESP_ATTRIBUTE:
			add(xqrc, m_cb_attribute); break;
		case RESP_INCOMPLETE_END_ELEMENT:
			add(xqrc, m_cb_incomplete_end_tag);
			break;
		default:
			throw new Exception("Unknown response type: "+type);
		}
	}
	
	private void add(XQueryResponseInterface xqrc, XQueryResponseInterface[] vec) throws Exception {
		for (int i=0; i<MAX_QLEN; i++) {
			if (vec[i] == null) {
				vec[i] = xqrc;
				return;
			}
		}
		throw new Exception("XQueryReporter::add() vector full, increase MAX_QLEN!");
	}
}
