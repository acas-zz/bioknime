package au.com.acpfg.xml.query;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;
import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;

/**
 * Returns the serialised XML representation of the current query. The overridden
 * methods are hooks for the model code to call to build up the result string for the user.
 * The implementation must be capable of doing its calculations regardless of the cell type as
 * it may be overriden
 * 
 * @author andrew.cassin
 *
 */
public class StringReporter implements XQueryReporterInterface, XQueryResponseInterface {
	public final static int INITIAL_BUFFER_CAPACITY = 1024;
	private StringBuffer m_sb;
	
	public StringReporter() {
		m_sb = new StringBuffer(INITIAL_BUFFER_CAPACITY);	
	}
	
	public StringReporter(XQueryReporter xr, String colname) throws Exception {
		this();
	
		// some types are registered with this so they can be overriden by subclasses (see TextReporter)
		xr.register_callback(QueryResponseFragmentType.RESP_START_ELEMENT, this);
		xr.register_callback(QueryResponseFragmentType.RESP_END_ELEMENT_TAG, this);
		xr.register_callback(QueryResponseFragmentType.RESP_END_TAG, this);
		xr.register_callback(QueryResponseFragmentType.RESP_INT, this);
		xr.register_callback(QueryResponseFragmentType.RESP_STRING, this);
		xr.register_callback(QueryResponseFragmentType.RESP_ATTRIBUTE, this);
		xr.register_callback(QueryResponseFragmentType.RESP_INCOMPLETE_END_ELEMENT, this);
		
		xr.register_reporter(this, colname);
	}

	@Override
	public void reset() {
		m_sb = new StringBuffer(1024);
	}

	@Override
	public DataCell getCell(String colname) {
		return new StringCell(m_sb.toString());
	}

	@Override
	public void callback(QueryResponseFragmentType type, String s) {
		switch (type) {
		case RESP_START_ELEMENT:
			m_sb.append("<"+s);
			break;
		case RESP_END_ELEMENT_TAG:
			m_sb.append("</"+s+">");
			break;
		case RESP_END_TAG:
			m_sb.append(">");
			break;
		case RESP_INCOMPLETE_END_ELEMENT:
			m_sb.append("/>");
			break;
		case RESP_ATTRIBUTE:
			m_sb.append(" "+s);
			break;
		case RESP_INT:
		case RESP_STRING:
			m_sb.append(s);
			break;
		default:
			// be silent & do nothing
			break;
		}
	}

}
