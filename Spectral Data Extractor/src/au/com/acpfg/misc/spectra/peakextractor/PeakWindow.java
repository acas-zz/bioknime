package au.com.acpfg.misc.spectra.peakextractor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Represents all state for a peak: left and right window tolerance, mz and a name.
 * Provides a configuration set/get for KNIME
 * 
 * The peak window is defined as all intensities in the range [mz - left_tol, mz+right_tol)
 * where left_tol and right_tol need not be the same. This is by-design a unitless class:
 * it is up to the user to ensure that the same units are used for all spectra which the node is applied
 * to
 * 
 * @author andrew.cassin
 *
 */
public class PeakWindow {
	private String m_name;
	private Double m_mz;
	private double m_ltol, m_rtol;
	
	public PeakWindow(String name, String mz, String ltol, String rtol) {
		this(name);
		
		try {
			m_mz = new Double(mz).doubleValue();
		} catch (Exception e) {
		}
		double d_ltol = new Double(ltol).doubleValue();
		double d_rtol = new Double(rtol).doubleValue();
		m_ltol = d_ltol;
		m_rtol = d_rtol;
	}
	
	public PeakWindow(String name, Double mz, double left_tol, double right_tol) {
		this(name);

		assert(mz.doubleValue() >= 0.0);
		assert(left_tol >= 0.0 && right_tol >= 0.0);
		
		m_mz = mz;
		m_ltol = left_tol;
		m_rtol = right_tol;
	}
	
	public PeakWindow(String name, Double mz) {
		this(name);
		assert(mz.doubleValue() >= 0.0);
		
		m_mz = mz;
	}
	
	public PeakWindow(String name) {
		assert(name != null);
		m_name = name;
		m_mz = 0.0;
		m_ltol = 0.05;
		m_rtol = 0.05;
	}

	public PeakWindow(NodeSettingsRO pw) throws InvalidSettingsException {
		this("Peak1");		// ensure all fields initialized even if exception thrown
		assert(pw != null);
	
		m_name = pw.getString("Peak_Name");
		m_mz   = pw.getDouble("MZ");
		m_ltol = pw.getDouble("left_tolerance");
		m_rtol = pw.getDouble("right_tolerance");
	}

	public String getName() {
		return m_name;
	}
	
	public double getMZ() {
		return m_mz.doubleValue();
	}
	
	public Double getMZasDouble() {
		return m_mz;
	}
	
	public double getLeft() {
		return m_ltol;
	}
	
	public String getLeftasString() {
		return new Double(m_ltol).toString();
	}
	
	public String getRightasString() {
		return new Double(m_rtol).toString();
	}
	
	public double getRight() {
		return m_rtol;
	}

	public String toString() {
		  return m_name + " " + m_mz + " : " + "["+m_ltol+", "+m_rtol+")";
	}
	
	/**
	 *  Configuration settings code for KNIME
	 */
	public void saveToSettings(NodeSettingsWO peak_settings) throws InvalidSettingsException {
		peak_settings.addDouble("MZ",              getMZasDouble());
		peak_settings.addDouble("left_tolerance",  getLeft());
		peak_settings.addDouble("right_tolerance", getRight());
		peak_settings.addString("Peak_Name",       getName());
	}

	public String getMZasString() {
		return new String(getMZasDouble().toString());
	}

	public void setLeft(double val) {
		m_ltol = val;
	}
	
	public void setRight(double val) {
		m_rtol = val;
	}
}
