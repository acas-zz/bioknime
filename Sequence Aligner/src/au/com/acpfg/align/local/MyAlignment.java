package au.com.acpfg.align.local;

import pal.alignment.SimpleAlignment;
import pal.misc.Identifier;

public class MyAlignment extends SimpleAlignment {
	private double m_score;
	private String m_tag_line;
	
	public MyAlignment(Identifier[] ids, String[] seqs) {
		super(ids, seqs, null);
		m_score = 0.0;
		m_tag_line = "";
	}
	
	public MyAlignment(Identifier[] ids, String[] seqs, double score) {
		this(ids, seqs);
		m_score = score;
	}

	public double getScore() {
		return m_score;
	}
	
	public void setTagLine(String tl) {
		m_tag_line= tl;
	}
	public String getTagLine() {
		return m_tag_line;
	}
}
