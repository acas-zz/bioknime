package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.io.UnmarshallerInterface;
import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;

/**
 * Simple implementation of a single program setting: name=value with slightly friendly improvements
 * (eg. default values etc.)
 * 
 * @author andrew.cassin
 *
 */
public abstract class ProgramSetting implements UnmarshallerInterface {
	
	// DATA MEMBERS
	private final HashMap<String,String> m_attrs;

	// Handlers for the data formats produced by various emboss programs
	private final static Map<String, UnmarshallerInterface> m_um = new HashMap<String, UnmarshallerInterface>();
	
	
	protected ProgramSetting(HashMap<String,String> attrs) {
		assert(attrs != null && attrs.containsKey("type") && attrs.containsKey("name") &&
				attrs.containsKey("default_value"));
		m_attrs = attrs;
	}

	/**
	 * Returns the EMBOSS ACD type for the setting eg. integer, string, etc.
	 * @return may be <code>null</code> if something goes wrong with parsing an EMBOSS .acd file
	 */
	public String getType() {
		return  m_attrs.get("type");
	}
	
	/**
	 * returns the name for the EMBOSS ACD setting
	 * @return may be <code>null</code> if something goes wrong with parsing an EMBOSS .acd file
	 */
	public String getName() {
		return m_attrs.get("name");
	}
	
	/**
	 * Returns <code>true</code> if the specified key is present in the given settings attributes,
	 * <code>false</code> otherwise. Only intended to be accessible to settings-derived classes.
	 * 
	 * @param key standardised attribute name to test (always lowercase)
	 * @return
	 */
	protected boolean hasAttribute(String key) {
		return (m_attrs != null && key != null && m_attrs.containsKey(key));
	}
	
	public String getAttributeValue(String key) {
		if (!hasAttribute(key)) 
			return null;
		return m_attrs.get(key);
	}
	
	/**
	 * If no default value is present, returns an empty string but must not return <code>null</code>
	 * @return
	 */
	public String getDefaultValue() {
		String dv = m_attrs.get("default-value");
		if (dv == null)
			return "";
		return dv;
	}
	
	/**
	 * Returns true if the setting is to come from a table column (input setting), false otherwise
	 * @return
	 */
	public boolean isInputFromColumn() {
		return (getColumnName() != null);
	}
	
	/**
	 * Returns the name of the KNIME table column which the setting comes from. If the specified 
	 * setting is not a column-related setting, <code>null</code> is returned
	 * @param dt
	 * @return
	 */
	public abstract String getColumnName();
	
	/**
	 * Subclasses will want to override this to provide a more sensible implementation
	 * 
	 * @param dt
	 * @return
	 */
	public abstract JComponent make_widget(DataTableSpec dt);
	
	
	@Override
	public String toString() {
		HashMap<String,String> a = new HashMap<String,String>();
		copy_attributes(a);
		StringBuffer sb = new StringBuffer(10*1024);
		for (String k : a.keySet()) {
			sb.append(k);
			sb.append('=');
			sb.append(a.get(k));
			sb.append('\n');
		}
		return Base64.encode(sb.toString().getBytes());
	}
	
	protected final HashMap<String,String> getAttributes() {
		return m_attrs;
	}
	
	/**
	 * Subclasses must override these this methods to ensure their class is correctly persisted
	 */
	public void copy_attributes(HashMap<String,String> attrs) {
		for (String k : m_attrs.keySet()) {
			attrs.put(k, m_attrs.get(k));
		}
	}

	/**
	 * Human-readable description of a program setting
	 * @param descr
	 */
	public void setDescription(String descr) {
		m_attrs.put("description", descr);
	}
	
	public String getPrettyDescription() {
		String descr = m_attrs.get("description");
		if (descr == null || descr.length() < 1) {
			return "No help available.";
		} else if (descr.length() > 60) {
			return ""+descr.substring(0, 60)+"...";
		} else {
			return descr;
		}
	}

	/**
	 * Returns the value of the setting (after the option)
	 * @return
	 * @throws Exception 
	 */
	public abstract void getArguments(ProgramSettingsListener l) throws Exception;
	
	/**
	 * Pushes the content of DataCell c into the specified file for the specified program setting
	 * (ie. taking the type of setting into account). Any existing file is emptied before the marshalling
	 * process begins.
	 * 
	 * @param c	
	 * @param file
	 * @throws IOException
	 */
	public abstract void marshal(String id, DataCell c, PrintWriter fw) 
					throws IOException, InvalidSettingsException; 
	

	/**
	 * If <code>b</code> is true the setting will appear on the input settings tabbed pane. Only one of the input/output/optional 
	 * may be set at any one time. 
	 * @param b
	 */
	public void setIsInput(boolean b) {
		m_attrs.put("is-input", new Boolean(b).toString());
	}
	
	public void setIsOutput(boolean b) {
		m_attrs.put("is-output", new Boolean(b).toString());
	}
	
	public void setIsOptional(boolean b) {
		m_attrs.put("is-optional", new Boolean(b).toString());
	}
	
	public boolean isInput() {
		return (m_attrs.containsKey("is-input") && 
				new Boolean(m_attrs.get("is-input")).booleanValue());
	}
	
	public boolean isOutput() {
		return (m_attrs.containsKey("is-output") && 
				new Boolean(m_attrs.get("is-output")).booleanValue());
	}
	
	public boolean isOptional() {
		return (m_attrs.containsKey("is-optional") && 
				new Boolean(m_attrs.get("is-optional")).booleanValue());
	}
		
	public boolean isList() {
		return (this instanceof ListSetting);
	}
	
	/**
	 * Every subclass must implement this method so that they are instantiated for the
	 * given ACD types that are supported by the class. Must return <code>true</code> for an
	 * ACD type that is supported by the class, <code>false</code> otherwise.
	 * 
	 * @param acd_type
	 * @return
	 */
	public static boolean canEmboss(String acd_type) {
		return false;
	}
	
	/**
	 * Factory method for instantiating the correct setting from the field=value pairs in the map...
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProgramSetting make(HashMap<String,String> attrs) throws InvalidSettingsException {
		String t = attrs.get("type").trim().toLowerCase();
		Class[] classes = new Class[] {
				SequenceSetting.class, GraphSetting.class, BooleanSetting.class,
				CodonUsageTableSetting.class, DataFileSetting.class, ListSetting.class,
				MatrixSetting.class, NumberSetting.class, OutputFileSetting.class,
				RangeSetting.class, RegexpSetting.class, StringSetting.class, 
				SequenceSetSetting.class, AlignmentSetting.class, ArraySetting.class
				
				// must always be at the end of the list (always accepts any ACD type)
				,DummySetting.class
		};
		
		for (Class c : classes) {
			try {
				Method   m = c.getMethod("canEmboss", String.class);
				Boolean ok = (Boolean) m.invoke(null,t);
				if (ok.booleanValue()) {
					 Constructor cons = c.getDeclaredConstructor(HashMap.class);
					 return (ProgramSetting) cons.newInstance(attrs);
				}
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;		// try next class
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		return null;
	}
	
	public static ProgramSetting make(String ser_form) throws InvalidSettingsException, Base64DecodingException {
		HashMap<String,String> attrs = new HashMap<String,String>();
		String[] attr_lines = new String(Base64.decode(ser_form)).split("\n");
		for (String l : attr_lines) {
			int first_equal_idx = l.indexOf('=');
			if (first_equal_idx < 0)
				throw new InvalidSettingsException("Programmer Error: field=value expected!");
			String field = l.substring(0, first_equal_idx);
			String value = l.substring(first_equal_idx+1);
			attrs.put(field, value);
		}
		return make(attrs);
	}

	public boolean isNumber() {
		return (this instanceof NumberSetting);
	}
	
	public void setMinMax(String min_bound, String max_bound) {
		// NO-OP: only NumberSetting overrides this method
	}
	
	public void unmarshal(File f, AbstractTableMapper atm, String emboss_prog) throws InvalidSettingsException,IOException {
		String[] tries = new String[] {
				emboss_prog + ":" + getName(),
				emboss_prog + ":" + getType(),
				getName(),
				getType()
		};
		
		// try to find the best unmarshaller (in the above order) for the specified setting
		UnmarshallerInterface ui = null;
		for (String t : tries) {
			ui = m_um.get(t);
			if (ui != null) {
				FileInputStream fis = new FileInputStream(f);
				ui.process(this, fis, atm);
				fis.close();
				return;
			}
		}
	}
	
	
	/****************** FORMATTEDUNMARSHALLERINTERFACE METHODS ***********************/
	
	/**
	 * Returns the type of cell which supports the settings value (output settings only)
	 * Most will be a <code>StringCell.TYPE</code> but we dont provide a default to force
	 * subclasses to implement
	 * 
	 * @return
	 */
	public void addColumns(AbstractTableMapper atm, ProgramSetting ps) {
		UnmarshallerInterface ui = m_um.get(ps.getName());
		if (ui != null) {
			ui.addColumns(atm, ps);
		}
	}
	
	/**
	 * Used to add cells to both output ports for the node (raw and formatted). See 
	 * <code>UnmarshallerInterface</code> for details
	 */
	public void process(ProgramSetting ps, InputStream out_file, AbstractTableMapper atm) 
					throws IOException,InvalidSettingsException {	
	}

	public static void addUnmarshaller(String acd_type, UnmarshallerInterface um) {
		m_um.put(acd_type, um);
	}
	
	public static void addUnmarshaller(String[] acd_types, UnmarshallerInterface um) {
		for (String acd_type : acd_types) {
			addUnmarshaller(acd_type, um);
		}
	}
}
