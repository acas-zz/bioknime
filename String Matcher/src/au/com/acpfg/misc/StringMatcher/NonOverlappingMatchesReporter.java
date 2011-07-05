package au.com.acpfg.misc.StringMatcher;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

/**
 * This implementation offers no guarantees about which overlapping match is reported, but it works
 * based on a "left-most" match is likely to be reported first.
 * 
 * @author andrew.cassin
 *
 */
public class NonOverlappingMatchesReporter implements MatchReporter {
	private boolean m_report_cnt;
	
	public NonOverlappingMatchesReporter(boolean report_count) {	
		m_report_cnt = report_count;
	}
	
	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		List<Extent> match_pos = m.getMatchPos();
		List<String> matches   = m.getMatches();
		if (matches == null || match_pos == null) {
			return DataType.getMissingCell();
		}
		if (matches.size() < 1) {
			return m_report_cnt ? new IntCell(0) : DataType.getMissingCell();
		}
		assert(matches.size() == match_pos.size());	// every match must have a corresponding position!
		DenseBitVector         bv = new DenseBitVector(str.length());
		ArrayList<StringCell> vec = new ArrayList<StringCell>();
		for (int i=0; i<matches.size(); i++) {
			Extent e = match_pos.get(i);
			String s = matches.get(i);
			long next_set_bit = bv.nextSetBit(e.m_start);
			if (next_set_bit >= 0 && next_set_bit < (long) e.m_end) {
				// skip match as its an overlapping one
			} else {
				bv.set(e.m_start, e.m_end);
				vec.add(new StringCell(s));
			}
		}
		return m_report_cnt ? new IntCell(vec.size()) : CollectionCellFactory.createListCell(vec);
	}

}
