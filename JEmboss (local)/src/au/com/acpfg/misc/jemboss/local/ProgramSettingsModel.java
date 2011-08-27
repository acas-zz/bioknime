package au.com.acpfg.misc.jemboss.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

/**
 * Implements a single row in the table for each program setting that needs to be set
 * by the user OF A GIVEN type (eg. input, output or advanced option). Adding a new
 * ProgramSetting with the same name as one already in the model, will replace the old one.
 * 
 * @author andrew.cassin
 *
 */
public class ProgramSettingsModel implements Iterable<ProgramSetting> {
	private HashMap<String,ProgramSetting> m_settings;
	private String m_prog_name;			// name of emboss program which the settings relate to
	private int m_input_cnt, m_output_cnt, m_optional_cnt;
	
	public ProgramSettingsModel() {
		m_settings = new HashMap<String,ProgramSetting>();
		m_input_cnt= 0;
		m_output_cnt = 0;
		m_optional_cnt = 0;
		m_prog_name = "";
	}
	
	/**
	 * Constructs a model from the serialised programsetting (one setting per array element in <code>ser</code>)
	 * @param ser
	 */
	public void addSettingsFrom(String[] ser) {
		try {
			m_prog_name = ser[0];
			for (int i=1; i<ser.length; i++) {
				String ps_ser = ser[i];
				if (ps_ser.length() > 0) {
						ProgramSetting ps = ProgramSetting.make(ps_ser);
						add(ps);
				}
			}
		} catch (Base64DecodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidSettingsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Removes all ProgramSetting's from the model
	 */
	public void clear() {
		m_settings.clear();
		m_input_cnt = 0;
		m_output_cnt = 0;
		m_optional_cnt = 0;
		m_prog_name = "";
	}
	
	/**
	 * Adds the specified ProgramSetting to the model, but DOES not fire the listeners to reflect the change.
	 * The caller must arrange to do this in a separate call.
	 */
	public void add(ProgramSetting ps) {
		if (ps != null) {
			m_settings.put(ps.getName(),ps);
			if (ps.isInput()) 
				m_input_cnt++;
			else if (ps.isOutput())
				m_output_cnt++;
			else
				m_optional_cnt++;
		}
	}
	
	/**
	 * Returns the number of settings in the model
	 */
	public int size() {
		return m_settings.size();
	}

	/**
	 * Returns an iterator which returns the settings ordered by (1) type and (2) name
	 */
	@Override
	public Iterator<ProgramSetting> iterator() {
		Collection<ProgramSetting> c = m_settings.values();
		ArrayList<ProgramSetting> a = new ArrayList<ProgramSetting>();
		a.addAll(c);
		Collections.sort(a, new Comparator<ProgramSetting>() {

			@Override
			public int compare(ProgramSetting a, ProgramSetting b) {
				// sort by type and then by alphabetical name
				String a_type = a.getType();
				String b_type = b.getType();
				int c_val = a_type.compareTo(b_type);
				if (c_val != 0)
					return c_val;
				String a_name = a.getName();
				String b_name = b.getName();
				return a_name.compareTo(b_name);
			}
			
		});
		return a.iterator();
	}
	
	/**
	 * Compute the serialised form of this class
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(1024);
		sb.append(m_prog_name);
		sb.append("\n");
		for (ProgramSetting ps : this) {
			sb.append(ps.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * Returns the number of input settings in the model
	 */
	public int getInputCount() {
		return m_input_cnt;
	}
	
	public final boolean hasOutputSettings() {
		return (getOutputCount() > 0);
	}
	
	public int getOutputCount() {
		return m_output_cnt;
	}
	
	public final boolean hasOptionalSettings() {
		return (getOptionalCount() > 0);
	}
	
	public int getOptionalCount() {
		return m_optional_cnt;
	}
	
	/**
	 * returns the name of the emboss program which the settings represent
	 * @return
	 */
	public String getProgram() {
		return m_prog_name;
	}
	
	/**
	 * Change the specified settings to reflect the specified program
	 */
	public void setProgram(String new_prog) {
		m_prog_name = new_prog;
	}
	
	/**
	 * Returns true if the specified settings reflect the given program, false otherwise
	 * 
	 * @param prog
	 * @return
	 */
	public boolean isProgram(final ProgramSettingsModel mdl) {
		if (m_prog_name == null || m_prog_name.length() < 1 || 
				mdl == null || mdl.m_prog_name == null || mdl.m_prog_name.length() < 1)
			return false;
		return m_prog_name.equalsIgnoreCase(mdl.m_prog_name);
	}
	
	/**
	 * Applys the current values from <code>stgs</code> into <code>this</code>. The
	 * settings specified by <code>stgs</code> are not changed.
	 * 
	 */
	public void assign(final ProgramSettingsModel stgs) {
		this.clear();
		this.setProgram(stgs.getProgram());
		for (ProgramSetting ps : stgs.m_settings.values()) {
			this.add(ps);
		}
	}

}
