package au.com.acpfg.misc.StringMatcher;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

/**
 * Highlights matched regions in red, otherwise black. TODO: colour should really be a KNIME pref....
 * 
 * @author andrew.cassin
 *
 */
public class HighlightMatchReporter implements MatchReporter {
	private boolean m_single_colour;
	
	public HighlightMatchReporter(boolean use_single_colour) {
		m_single_colour = use_single_colour;
	}
	
	@Override
	public DataCell report(StringMatcherNodeModel m, String str)
			throws Exception {
		StringBuffer sb = new StringBuffer(str.length());
		sb.append("<html><pre>");		// KNIME Table will display HTML (partial standards compliance)
		DenseBitVector bv = m.getResultsBitVector();
		assert(str.length() == bv.length());
		for (int i=0; i<bv.length(); i++) {
			if (bv.get(i)) {
				sb.append("<font color=\"red\">");
				sb.append(str.charAt(i));
				sb.append("</font>");
			} else {
				sb.append(str.charAt(i));
			}
		}
		return new StringCell(sb.toString());
	}

}
