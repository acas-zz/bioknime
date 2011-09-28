package au.com.acpfg.misc.jemboss.local;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents all emboss programs in a single category eg. UTILS
 * @author andrew.cassin
 *
 */
public class ProgsInCategory implements Iterable<EmbossProgramDescription>,Comparable<ProgsInCategory> {
	private final Map<String,EmbossProgramDescription> m_progs = new TreeMap<String,EmbossProgramDescription>();
	private String m_category_name;
	
	public ProgsInCategory(String category_name) {
		m_category_name = category_name;
	}
	
	/**
	 * Constructor which only puts programs from <code>pic</code> that are acceptable to the specified filter
	 * in the newly constructed instance
	 * @param pic
	 */
	public ProgsInCategory(ProgsInCategory pic, MyTreeFilter filter) {
		m_category_name = pic.m_category_name;
		for (String name : pic.m_progs.keySet()) {
			EmbossProgramDescription epd = pic.getProgram(name);
			assert(epd != null);
			if (filter.accepts(epd)) {
				m_progs.put(name, epd);
			}
		}
	}

	public void add(String emboss_prog_name, String descr) {
		m_progs.put(emboss_prog_name, new EmbossProgramDescription(emboss_prog_name, descr));
	}
	
	public Set<String> keys() {
		return m_progs.keySet();
	}
	
	public boolean hasName(String cname) {
		return m_category_name.equals(cname);
	}
	
	public String getCategory() {
		return m_category_name;
	}
	
	public EmbossProgramDescription getProgram(String k) {
		return m_progs.get(k);
	}
	
	/**
	 * Returns <code>true</code> if any program in this category passes the 
	 * specified filter, <code>false</code> otherwise.
	 * 
	 * @param tf
	 * @return
	 */
	public boolean hasAcceptablePrograms(MyTreeFilter tf) {
		for (EmbossProgramDescription epd : m_progs.values()) {
			if (tf.accepts(epd)) 
				return true;
		}
		return false;
	}
	
	public int getIndexOfChild(Object k) {
		
		if (k instanceof EmbossProgramDescription) {
			k = ((EmbossProgramDescription)k).getName();
		}
		if (k instanceof String) {
			Set<String> keys = m_progs.keySet();
			int idx = 0;
			for (String key : keys) {
				if (key.equals(k)) {
					return idx;
				}
				idx++;
			}
		}
		return -1;
	}
	
	public Iterator<EmbossProgramDescription> iterator() {
		return m_progs.values().iterator();
	}

	@Override
	public int compareTo(ProgsInCategory o) {
		return m_category_name.compareTo(o.m_category_name);
	}
	
	@Override
	public String toString() {
		int cnt = m_progs.size();
		if (cnt == 1) {
			return m_category_name + "[one item]";
		}
		return m_category_name + "["+m_progs.size()+" items]";
	}

	public int getNumPrograms() {
		return m_progs.size();
	}

	public String getName() {
		return m_category_name;
	}
}
