package au.com.acpfg.io.genbank.reader;

public class SectionStart {
	private String m_title;
	private int m_start;
	private int m_len;
	private SectionStart m_next;
	
	public SectionStart(String key, int start_pos, int len, SectionStart next) {
		set_title(key);
		set_start(start_pos);
		set_len(len);
		set_next(next);
	}

	public SectionStart(String key, int start_pos, int len) {
		this(key, start_pos, len, null);
	}
	
	public SectionStart(String key, int start_pos) {
		this(key, start_pos, -1, null);
	}
	
	public void set_title(String m_title) {
		this.m_title = m_title;
	}

	public String get_title() {
		return m_title;
	}

	public void set_start(int m_start) {
		this.m_start = m_start;
	}

	public boolean isLengthKnown() {
		return (m_len >= 0);
	}
	
	public int get_start() {
		return m_start;
	}

	public void set_len(int m_len) {
		this.m_len = m_len;
	}

	public int get_len() {
		return m_len;
	}

	public void set_next(SectionStart m_next) {
		this.m_next = m_next;
	}

	public SectionStart get_next() {
		return m_next;
	}
}
