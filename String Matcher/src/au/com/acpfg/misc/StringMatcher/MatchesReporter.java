package au.com.acpfg.misc.StringMatcher;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

public class MatchesReporter implements MatchReporter {
	private boolean m_report_cnt;
	
	public MatchesReporter(boolean report_cnt) {
		m_report_cnt = report_cnt;
	}
	
	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		List<String> matches = m.getMatches();
		
		// return just the match count? We test this first to ensure it is never a missing cell (user convenience)
		if (m_report_cnt) {
			return (matches != null) ? new IntCell(matches.size()) : new IntCell(0);
		} else if (matches == null || matches.size() < 1) {
			return DataType.getMissingCell();
		}
		ArrayList<StringCell> ret = new ArrayList<StringCell>();
		for (String match : matches) {
			ret.add(new StringCell(match));
		}
		return CollectionCellFactory.createListCell(ret);
	}

}
