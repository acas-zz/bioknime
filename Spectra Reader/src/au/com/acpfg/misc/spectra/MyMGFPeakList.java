package au.com.acpfg.misc.spectra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.proteomecommons.io.GenericPeak;
import org.proteomecommons.io.Peak;
import org.proteomecommons.io.PeakList;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

/**
 * 
 * @author andrew.cassin
 *
 */
public class MyMGFPeakList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4897834331580715693L;
	
	private HashMap<String,String> m_headers;	// all header eg. TITLE, CHARGE, PEPMASS etc with their values
	private double[] m_mz;
	private double[] m_intensity;
	private double m_mz_min, m_mz_max;
	private int m_tc;
	
	public MyMGFPeakList() {
		m_headers = new HashMap<String,String>();
		m_mz = null;
		m_intensity = null;
		m_tc = 2;		// assume MS/MS
	}
	
	public void setPeaks(double[] mz, double[] intensity) {
		assert(mz.length == intensity.length);
		m_mz = mz;
		m_intensity = intensity;
		m_mz_min = Double.POSITIVE_INFINITY;
		m_mz_max = Double.NEGATIVE_INFINITY;
		for (int i=0; i<mz.length; i++) {
			if (mz[i] < m_mz_min)
				m_mz_min = mz[i];
			if (mz[i] > m_mz_max) 
				m_mz_max = mz[i];
		}
	}
	
	public Peak[] getPeaks() {
		int n_peaks = m_mz.length;
		if (n_peaks < 1) 
			return null;
		GenericPeak[] p = new GenericPeak[n_peaks];
		for (int i=0; i<n_peaks; i++) {
			p[i] = new GenericPeak();
			p[i].setMassOverCharge(m_mz[i]);
			p[i].setIntensity(m_intensity[i]);
		}
		return p;
	}

	public int getNumPeaks() {
		if (m_mz == null)
			return 0;
		return m_mz.length;
	}
	
	public List<Peak> getPeaksAsList() {
		Peak[] p = getPeaks();
		ArrayList<Peak> ret = new ArrayList<Peak>();
		for (Peak tmp : p) {
			ret.add(tmp);
		}
		return ret;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("MyMGFPeakList does not support clone yet!");
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj);
	}

	@Override
	public int hashCode() {
		return m_headers.hashCode();
	}

	public void addHeader(String key, String val) {
		m_headers.put(key, val);
	}
	
	public String getHeader(String key) {
		if (hasHeader(key)) {
			return m_headers.get(key); 
		}
		return null;
	}

	public boolean hasHeader(String key) {
		if (m_headers != null && m_headers.containsKey(key))
			return true;
		return false;
	}
	
	public void setCharge(String ch) {
		addHeader("CHARGE", ch);
	}
	
	public void setTitle(String title) {
		addHeader("TITLE", title);
	}
	
	public void setPepMass(String pm) {
		addHeader("PEPMASS", pm);
	}
	
	public void setTandemCount(int tc) {
		m_tc = tc;
	}
	
	public double getMinMZ() {
		return m_mz_min;
	}
	
	public double getMaxMZ() {
		return m_mz_max;
	}

	public String getTitle_safe() {
		String title = getHeader("TITLE");
		return (title != null) ? title : "";
	}
	
	public String getPepmass_safe() {
		String pepmass = getHeader("PEPMASS");
		return (pepmass != null) ? pepmass : "";
	}
	
	public String getCharge_safe() {
		String charge = getHeader("CHARGE");
		return (charge != null) ? charge : "";
	}

	public int getTandemCount() {
		return m_tc;
	}
	
}
