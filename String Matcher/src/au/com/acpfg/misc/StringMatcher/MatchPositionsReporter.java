package au.com.acpfg.misc.StringMatcher;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.StringCell;

public class MatchPositionsReporter implements MatchReporter {

	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		List<Extent> match_pos = m.getMatchPos();
		if (match_pos == null || match_pos.size() < 1) 
			return DataType.getMissingCell();
		ArrayList<StringCell> vec = new ArrayList<StringCell>();
		for (Extent e : match_pos) {
			vec.add(new StringCell(e.m_start+"-"+e.m_end));
		}
		return CollectionCellFactory.createListCell(vec);
	}

}
