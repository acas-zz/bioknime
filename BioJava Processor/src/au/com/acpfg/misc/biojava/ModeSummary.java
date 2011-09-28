package au.com.acpfg.misc.biojava;

public class ModeSummary {
	private int[] m;
	private int best;
	private int m_window_size;		// only 3 is supported for now
	
	public ModeSummary(char c1, char c2, char c3) {
		assert((c1 == '0' || c1 == '1' || c1 == '2') && 
				(c2 == '0'|| c2 == '1' || c2 == '2') &&
				(c3 == '0' || c3== '1' || c3 == '2'));
		m = new int[3];
		int v1 = c1 - '0';
		int v2 = c2 - '0';
		int v3 = c3 - '0';
		assert(v1 >= 0 && v1 <= 2);
		assert(v2 >= 0 && v2 <= 2);
		assert(v3 >= 0 && v3 <= 2);

		m[0] = 0;
		m[1] = 0;
		m[2] = 0;
		m[v1]++;
		m[v2]++;
		m[v3]++;
		best = m[0];
		if (m[1] > best) 
			best = m[1];
		if (m[2] > best) 
			best = m[2];
		m_window_size = 3;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<m_window_size; i++) {
			if (m[i] == best) 
				sb.append((char)((int)'0'+i));
		}
		return sb.toString();
	}
}
