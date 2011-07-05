package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.Stack;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;

/**
 * Reports only attributes and their values as a list of FIELD=VALUE StringCell's (as a list). Doesn't matter
 * the owning element, they are all reported.
 * 
 * @author andrew.cassin
 *
 */
public class AttributeCollectionReporter implements XQueryReporterInterface {
	private ArrayList<StringCell> m_results;
	
	public AttributeCollectionReporter(XQueryReporter xr, String colname) throws Exception {
		m_results = new ArrayList<StringCell>();
		xr.register_callback(QueryResponseFragmentType.RESP_ATTRIBUTE, new XQueryResponseInterface() {

			@Override
			public void callback(QueryResponseFragmentType type, String s) {
				if (s != null && s.length() > 0)
					m_results.add(new StringCell(s));
			}
			
		});
		xr.register_reporter(this, colname);
	}
	
	@Override
	public void reset() {
		m_results.clear();
	}

	@Override
	public DataCell getCell(String colname) {
		if (m_results.size() < 1) {
			return DataType.getMissingCell();
		}
		return CollectionCellFactory.createListCell(m_results);
	}

}
