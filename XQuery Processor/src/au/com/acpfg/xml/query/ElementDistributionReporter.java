package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;
import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;

public class ElementDistributionReporter implements XQueryReporterInterface {
	private final HashMap<String,Integer> m_start_tag_freq = new HashMap<String,Integer>();
	
	public ElementDistributionReporter(XQueryReporter r, String colname) throws Exception {
		r.register_callback(QueryResponseFragmentType.RESP_START_ELEMENT, new XQueryResponseInterface() {

			@Override
			public void callback(QueryResponseFragmentType type, String element_name) {
				if (!m_start_tag_freq.containsKey(element_name)) {
					m_start_tag_freq.put(element_name, new Integer(1));
				} else {
					Integer i = m_start_tag_freq.get(element_name);
					m_start_tag_freq.put(element_name, new Integer(i.intValue()+1));
				}
			}
			
		});
		r.register_reporter(this, colname);
	}

	@Override
	public void reset() {
		m_start_tag_freq.clear();		
	}

	@Override
	public DataCell getCell(String colname) {
		if (m_start_tag_freq == null || m_start_tag_freq.size() < 1) {
			return DataType.getMissingCell();
		}
		// else...
		List<StringCell> l = new ArrayList<StringCell>();
		for (String key : m_start_tag_freq.keySet()) {
			l.add(new StringCell(key+"="+m_start_tag_freq.get(key).toString()));
		}
		return CollectionCellFactory.createListCell(l);
	}
	
	

}
