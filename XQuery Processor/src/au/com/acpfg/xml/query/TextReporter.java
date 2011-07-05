package au.com.acpfg.xml.query;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;

/**
 * A reporter which eliminates all XML markup from the string leaving only the textual content of the
 * query result
 * 
 * @author andrew.cassin
 *
 */
public class TextReporter extends StringReporter {
	private StringBuffer m_sb = new StringBuffer(StringReporter.INITIAL_BUFFER_CAPACITY);
	
	public TextReporter(XQueryReporter xr, String colname) throws Exception {
		super();
		
		xr.register_callback(QueryResponseFragmentType.RESP_INT, this);
		xr.register_callback(QueryResponseFragmentType.RESP_STRING, this);
		xr.register_reporter(this, colname);
	}
	
	public void callback(QueryResponseFragmentType type, String s) {
		switch (type) {
		case RESP_INT:
		case RESP_STRING:
			m_sb.append(s);
			break;
		default:
			break;
		}
	}
	
	@Override
	public void reset() {
		m_sb = new StringBuffer(StringReporter.INITIAL_BUFFER_CAPACITY);
	}

	@Override
	public DataCell getCell(String colname) {
		if (m_sb.length() < 1) 
			return DataType.getMissingCell();
		return new StringCell(m_sb.toString());
	}

}
