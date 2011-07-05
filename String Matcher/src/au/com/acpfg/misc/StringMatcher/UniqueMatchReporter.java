package au.com.acpfg.misc.StringMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

public class UniqueMatchReporter implements MatchReporter {
	private boolean m_report_cnt;
	private boolean m_report_distribution;		// frequency of occurrence of each unique matching string
	
	public UniqueMatchReporter(boolean report_cnt) {
		m_report_cnt          = report_cnt;
		m_report_distribution = false;
	}
	
	public UniqueMatchReporter(String task) {
		m_report_cnt          = false;
		m_report_distribution = task.equals("Unique Match Distribution");
	}
	
	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		List<String> matches = m.getMatches();
		if (matches == null)
			return DataType.getMissingCell();
		HashMap<String,Integer> hm = new HashMap<String,Integer>();
		int cnt = 0;
		for (String match : matches) {
			if (!hm.containsKey(match)) {
				cnt++;
				hm.put(match, new Integer(1));
			} else {
				Integer i = hm.get(match);
				hm.put(match, new Integer(i.intValue()+1));
			}
		}
		
		if (m_report_cnt) {
			return new IntCell(cnt);
		} else {
			ArrayList<StringCell> ret = new ArrayList<StringCell>();
			for (String match : hm.keySet()) {
				if (!m_report_distribution) {
					ret.add(new StringCell(match));
				} else {
					ret.add(new StringCell(match+"="+hm.get(match).intValue()));
				}
			}
			return CollectionCellFactory.createSetCell(ret);
		}
	}

}
