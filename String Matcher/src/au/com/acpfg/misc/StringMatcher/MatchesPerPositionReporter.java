package au.com.acpfg.misc.StringMatcher;

import java.util.List;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

public class MatchesPerPositionReporter implements MatchReporter {
	public MatchesPerPositionReporter() {	
	}
	
	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		DenseBitVector bv = m.getResultsBitVector();
		List<Extent> m_match_pos = m.getMatchPos();
		
		if (bv == null || m_match_pos == null || bv.length() < 1)
			return DataType.getMissingCell();
		long len = bv.length();
		ArrayList<IntCell> vec = new ArrayList<IntCell>();
		int[] cnt = new int[(int)len];
		for (Extent e : m_match_pos) {
			for (int i=e.m_start; i<e.m_end; i++) {
				cnt[i]++;
			}
		}
		for (int j=0; j<cnt.length; j++) {
			vec.add(new IntCell(cnt[j]));
		}
		return CollectionCellFactory.createListCell(vec);
	}

}
