package au.com.acpfg.misc.StringMatcher;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

public class ExtentReporter implements MatchReporter {
	public boolean m_as_string;
	
	public ExtentReporter(boolean as_string) {	
		m_as_string = as_string;
	}
	
	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		List<Extent> match_pos = m.getMatchPos();
		if (match_pos == null || match_pos.size() < 1)
			return DataType.getMissingCell();
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Extent e : match_pos) {
			min = Math.min(min, e.m_start);
			max = Math.max(max, e.m_end);
		}
		return (m_as_string) ? new StringCell(str.substring(min, max)) : new StringCell(""+min+"-"+max);
	}

}
