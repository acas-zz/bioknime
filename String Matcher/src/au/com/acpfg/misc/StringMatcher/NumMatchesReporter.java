package au.com.acpfg.misc.StringMatcher;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell;

public class NumMatchesReporter implements MatchReporter {

	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		return new IntCell(m.getNumMatches());
	}

}
