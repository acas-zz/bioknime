package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.Stack;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;

public class TextCollectionReporter extends TextReporter {
	private Stack<String>         m_tags;		// determines when to start a new StringCell
	private ArrayList<DataCell> m_results = new ArrayList<DataCell>();
	
	public TextCollectionReporter(XQueryReporter xr, String colname) throws Exception {
		super(xr, colname);
	}
	
	@Override
	public void reset() {
		m_results.clear();
		super.reset();
	}
	
	@Override
	public void callback(QueryResponseFragmentType type, String s) {
		switch (type) {
		case RESP_START_ELEMENT:
			m_tags.push(s);
			break;
		
		case RESP_END_ELEMENT_TAG:
			String tag = m_tags.pop();
			if (m_tags.empty()) {
				m_results.add(super.getCell(""));
				super.reset();
			}
			break;
		default:
			super.callback(type, s);
			break;
		}
	}
	
	@Override
	public DataCell getCell(String colname) {
		if (m_results.size() < 1) 
			return DataType.getMissingCell();
		return CollectionCellFactory.createListCell(m_results);
	}
}
