package au.com.acpfg.misc.StringMatcher;

public class Extent {
	public int m_start;
	public int m_end;
	
	public Extent(int start, int end) {
		assert(end >= start);
		m_start = start;
		m_end = end;
	}
	
	public String toString() {
		return m_start + "-" + m_end;
	}
}
