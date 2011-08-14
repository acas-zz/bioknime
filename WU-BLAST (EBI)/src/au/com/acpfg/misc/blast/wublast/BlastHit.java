package au.com.acpfg.misc.blast.wublast;

public class BlastHit {
	private int m_idx;
	private String m_db, m_accsn, m_descr, m_seq_query, m_seq_match, m_seq_pattern;
	private double m_score, m_bits, m_eval;
	private int m_identity, m_positives;
	private int m_query_start, m_query_end, m_match_start, m_match_end;
	
	public BlastHit(int idx, String database, String accsn, String descr) {
		m_idx   = idx;
		m_db    = database;
		m_accsn = accsn;
		m_descr = descr;
		m_score = -1.0;
		m_bits  = -1.0;
		m_eval  = -1.0;
		m_identity = -1;
		m_positives= -1;
		m_query_start = -1;
		m_query_end = -1;
		m_match_start = -1;
		m_match_end = -1;
	}
	
	public String getDatabase() {return m_db; }
	public String getAccession() { return m_accsn; }
	public String getDescription() { return m_descr; }
	public double getAlignScore() { return m_score; }
	public double getAlignBits() { return m_bits; }
	public double getAlignEval() { return m_eval; }
	public int getAlignIdentity() { return m_identity; }
	public int getAlignPositives() { return m_positives; }
	public String getQuerySequence() { return m_seq_query; }
	public String getPatternSequence() { return m_seq_pattern; }
	public String getMatchSequence() { return m_seq_match; }
	public int getQueryStart() { return m_query_start; }
	public int getQueryEnd() { return m_query_end; }
	public int getMatchStart() { return m_match_start; }
	public int getMatchEnd() { return m_match_end; }
	
	public void setScore(String new_score) {
		//System.err.println(new_score);
		m_score = new Double(new_score).doubleValue();
	}
	
	public void setBits(String new_score) {
		//System.err.println(new_score);
		m_bits = new Double(new_score).doubleValue();
	}
	
	public void setEval(String new_score) {
		//System.err.println(new_score);
		m_eval = new Double(new_score).doubleValue();
	}
	
	public void setIdentity(String new_identity_count) {
		m_identity = new Integer(new_identity_count).intValue();
	}
	
	public void setPositives(String new_count) {
		m_positives = new Integer(new_count).intValue();
	}
	
	public void setQuery(String new_query) {
		m_seq_query = new_query;
	}
	
	public void setPattern(String new_pat) {
		m_seq_pattern = new_pat;
	}
	
	public void setMatch(String new_match) {
		m_seq_match = new_match;
	}
	
	public void setMatchPos(String start, String end) {
		m_match_start = new Integer(start).intValue();
		m_match_end = new Integer(end).intValue();
	}
	
	public void setQueryPos(String start, String end) {
		m_query_start = new Integer(start).intValue();
		m_query_end = new Integer(end).intValue();
	}
}
