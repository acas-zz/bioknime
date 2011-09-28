package au.com.acpfg.misc.spectra;

/**
 * Implements a unique rowkey sequence eg. Row1, Row2, Row3... bumped each call
 * @author andrew.cassin
 *
 */
public class RowSequence {
	private int m_id;
	private String m_prefix;
	
	public RowSequence() {
		m_id = 1;
		m_prefix = "Row";
	}
	
	public RowSequence(String prefix) {
		if (prefix != null) {
			m_prefix = prefix;
		}
		m_id = 1;
	}
	
	public String get() {
		return m_prefix+m_id++;
	}
}
