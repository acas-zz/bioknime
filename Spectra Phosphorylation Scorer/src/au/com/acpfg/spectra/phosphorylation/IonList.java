package au.com.acpfg.spectra.phosphorylation;

import java.util.ArrayList;

/**
 * Implements a list of ions which keep track of minimum and maximum m/z present amongst all
 * ions in the list. These extremes are only guaranteed to be accurate if add/clear are the only
 * list ops used, otherwise results may be inaccurate.
 * 
 * @author andrew.cassin
 *
 */
public class IonList extends ArrayList<Ion> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8602347235372272003L;
	// members
	private double m_b_min_mz, m_b_max_mz, m_y_min_mz, m_y_max_mz;
	
	public IonList() {
		this(16);
	}
	
	public IonList(int initial_capacity) {
		super(initial_capacity);
		clear();		// just to reset the counters
	}
	
	@Override
	public boolean add(Ion i) {
		double mz = i.get_mz();
		if (i.is_B()) {
			if (mz < m_b_min_mz) {
				m_b_min_mz = mz;
			}
			if (mz > m_b_max_mz) {
				m_b_max_mz = mz;
			}
		} else if (i.is_Y()) {
			if (mz < m_y_min_mz) {
				m_y_min_mz = mz;
			} 
			if (mz > m_y_max_mz) {
				m_y_max_mz = mz;
			}
		}
		return super.add(i);
	}
	
	@Override 
	public void clear() {
		m_b_min_mz = Double.MAX_VALUE;
		m_b_max_mz = Double.MIN_VALUE;
		m_y_min_mz = Double.MAX_VALUE;
		m_y_max_mz = Double.MIN_VALUE;
	}
	
	/**
	 * Returns a new IonList with <code>ifilt</code> applied to each ion in <code>this</code>.
	 * Only those which are accepted are returned. Useful for removing Ions from an existing list (and forming a new list).
	 * 
	 * @param ifilt
	 * @return
	 */
	public IonList make_ions(IonFilterInterface ifilt) {
		IonList ret = new IonList();
		for (Ion i : this) {
			if (ifilt.accept(i))
				ret.add(i);
		}
		return ret;
	}
	
	/**
	 * Returns the M/Z range for the current elements in the list (as a String) 
	 */
	public String getMZRange() {
		return "B: "+m_b_min_mz+"-"+m_b_max_mz+", "+
				"Y: "+m_y_min_mz+"-"+m_y_max_mz;
	}
	
	/**
	 * returns a String with each ion in list order on a separate (newline separated) line
	 */
	@Override
	public String toString() {
		String out = "";
		for (Ion i : this) {
			out += i.toString() + "\n";
		}
		return out;
	}
}
