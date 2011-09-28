package au.com.acpfg.spectra.phosphorylation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds a single mascot identified or theoretical ion, 
 * with attributes to represent key data from Mascot(tm).
 * 
 * 
 * @author andrew.cassin
 *
 */
public class Ion {
	// constants
	public final static double MASS_H2O = 18.010;		// in Da (to two decimal places)
	public final static double MASS_NH3 = 17.0;			// in Da (to one decimal place)
	public final static double MASS_PROTON = 1.008;		// in Da (to 3 decimal places)
	
	// data members
	private double m_mz;
	private char m_type;		// ie. a, b, c, x, y, z
	private int m_idx;
	private boolean m_lost_nh3, m_lost_h2o;
	private String m_charge;
	private double m_delta;		// monoisotopic shift as the result of a modification eg. phosphorylation
	private static final Pattern p = Pattern.compile("^&([by])\\[(\\d+)\\](\\+*)(\\-\\w+)?$");
	private static final Pattern p2= Pattern.compile("^&?([by])(\\d+)$");

	/**
	 * Copy constructor
	 * @param i must not be <code>null</code>
	 */
	public Ion(Ion i) {
		m_mz       = i.m_mz;
		m_idx      = i.m_idx;
		m_charge   = i.m_charge;
		m_type     = i.m_type;
		m_lost_h2o = i.m_lost_h2o;
		m_lost_nh3 = i.m_lost_nh3;
	}
	
	public Ion(String id, double mz) throws InvalidIonException {
		set_mz(mz);
		// eg. b2
		Matcher m = p2.matcher(id);
		if (m.matches()) {
			Integer idx = new Integer(m.group(2));
			set_idx(idx.intValue());
			set_type(m.group(1).toLowerCase().charAt(0));
			set_lost_nh3(false);
			set_lost_h2o(false);
			set_charge("");
		} else if (id.startsWith("&")) { // eg. &b[2]-NH3 or &y[3]-H2O
			m = p.matcher(id);
			if (!m.matches()) {	
				throw new InvalidIonException("Unknown Ion identifier: "+id);
			} else {
				// extract key data for object state
				set_type(m.group(1).toLowerCase().charAt(0));	// b or y ion?
				Integer idx = new Integer(m.group(2));			// site determining ion?
				String loss = m.group(4);						// ion the result of loss of ammonium or water?
				if (loss == null)								// optional group
					loss = "";
				loss = loss.toLowerCase();
				if (loss.length() > 0 && !(loss.equals("-nh3") || loss.equals("-h2o"))) {
					throw new InvalidIonException("Unknown loss: "+loss);
				}
				set_idx(idx.intValue());
				set_lost_nh3(loss.equals("-nh3"));
				set_lost_h2o(loss.equals("-h2o"));
				set_charge(m.group(3));
			}
		} else {
			throw new InvalidIonException("Unknown Ion identifier: "+id);
		}
	}
	
	public void set_mz(double m_mz) {
		this.m_mz = m_mz;
	}

	public double get_mz() {
		double nl = 0.0;
		if (has_lost_h2o()) {
			nl += MASS_H2O;
		} 
		if (has_lost_nh3()) {
			nl += MASS_NH3;
		}
		return m_mz+m_delta+nl;
	}

	public void set_type(char m_type) {
		this.m_type = m_type;
	}

	public char get_type() {
		return m_type;
	}

	public boolean is_B() {
		return (m_type == 'b');
	}
	
	public boolean is_Y() {
		return (m_type == 'y');
	}
	
	public void set_idx(int m_idx) {
		this.m_idx = m_idx;
	}

	/**
	 * NB: Mascot uses nomenclature which is reversed to most publications showing the peptide backbone
	 * ie. B1 and Y1 are at the same position in the peptide backbone (N-terminal residue)
	 */
	public int get_idx() {
		return m_idx;
	}

	public boolean hasNeutralLoss() {
		return (has_lost_nh3() || has_lost_h2o());
	}
	
	public void set_lost_nh3(boolean m_lost_nh3) {
		this.m_lost_nh3 = m_lost_nh3;
	}

	public boolean has_lost_nh3() {
		return m_lost_nh3;
	}

	public void set_lost_h2o(boolean m_lost_h2o) {
		this.m_lost_h2o = m_lost_h2o;
	}

	public boolean has_lost_h2o() {
		return m_lost_h2o;
	}

	public void set_charge(String m_charge) {
		this.m_charge = m_charge;
	}

	public String get_charge() {
		return m_charge;
	}
	
	@Override public String toString() {
		String id = "";
		if (is_B()) 
			id = "B"+get_idx();
		else if (is_Y()) 
			id = "Y"+get_idx();
		
		String losses = "";
		if (has_lost_h2o())
			losses += "-H2O ";
		if (has_lost_nh3()) 
			losses += "-NH3 ";
		return "" + get_mz() + "("+m_charge+"): "+id+" "+losses;
	}

}
