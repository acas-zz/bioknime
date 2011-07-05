package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.Stack;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;

/**
 * Implements the RAW_XML_COLLECTION reporter, which separates each hit into a separate StringCell
 * which is inserted into the table as a single KNIME list collection cell
 * 
 * @author andrew.cassin
 *
 */
public class ElementCollectionReporter extends StringReporter {
	private ArrayList<DataCell> m_results;
	private Stack<String>         m_tags;		// determines when to start a new StringCell
	
	public ElementCollectionReporter(XQueryReporter xr, String colname) throws Exception {
		super(xr, colname);
		m_results = new ArrayList<DataCell>();
		m_tags    = new Stack<String>();
	}
	
	@Override
	public void callback(QueryResponseFragmentType type, String s) {
		super.callback(type, s);
		switch (type) {
		case RESP_START_ELEMENT:
			m_tags.push(s);
			break;
		case RESP_INCOMPLETE_END_ELEMENT:
		case RESP_END_ELEMENT_TAG:
			m_tags.pop();
			if (m_tags.isEmpty()) {
				m_results.add(super.getCell(""));
				super.reset();
			}
			break;
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		m_results.clear();
	}

	@Override
	public DataCell getCell(String colname) {
		if (m_results.size() < 1) 
			return DataType.getMissingCell();
		return CollectionCellFactory.createListCell(m_results);
	}

}
