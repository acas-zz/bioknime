package au.com.acpfg.misc.jemboss.settings;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;

import au.com.acpfg.misc.jemboss.io.FastaUnmarshaller;
import au.com.acpfg.misc.jemboss.io.FormattedUnmarshallerInterface;
import au.com.acpfg.misc.jemboss.local.JEmbossProcessorNodeModel;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;

import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.MimeUtil;

/**
 * Simple implementation of a single program setting: name=value with slightly friendly improvements
 * (eg. default values etc.)
 * 
 * @author andrew.cassin
 *
 */
public abstract class ProgramSetting {
	
	// DATA MEMBERS
	private final HashMap<String,String> m_attrs;

	// FORMATTED UNMARSHALLERS for various emboss programs
	private final static Map<String, FormattedUnmarshallerInterface> m_um = new HashMap<String, FormattedUnmarshallerInterface>();
	static {
		m_um.put("seqoutall", new FastaUnmarshaller());
	}
	
	
	protected ProgramSetting(HashMap<String,String> attrs) {
		assert(attrs != null && attrs.containsKey("type") && attrs.containsKey("name") &&
				attrs.containsKey("default_value"));
		m_attrs = attrs;
	}

	public String getType() {
		return  m_attrs.get("type");
	}
	
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
	 * Returns the type of cell which supports the settings value (output settings only)
	 * Most will be a <code>StringCell.TYPE</code> but we dont provide a default to force
	 * subclasses to implement
	 * 
	 * @return
	 */
	public abstract DataType getCellType();

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
	 * Responsible for reading <code>out_file</code> which contains the results of the EMBOSS program
	 * for the specified ProgramSetting. 
	 * 
	 * @param out_file the file which contains the data to unmarshal (<code>this.m_type</code> provides clues as to expected content)
	 * @param c2 the container which will contain formatted row output from the raw data (if supported)
	 * @param rid the input row ID which produced the specified file via EMBOSS
	 * @return This method must not return <code>null</code> (return a missing cell or throw instead)
	 * @throws IOException
	 */
	public abstract DataCell unmarshal(File out_file, BufferedDataContainer c2, String rid) 
								throws IOException,InvalidSettingsException;
	/* {
		if (m_type.equals("outfile") || m_type.equals("seqout") || 
				m_type.equals("seqoutall") || m_type.equals("featout") || m_type.equals("report")) {
			StringBuffer    sb = new StringBuffer((int) out_file.length());
			sb.append("<html><pre>");
			BufferedReader bfr = null;
			try {
				bfr = new BufferedReader(new FileReader(out_file));
				String tmp;
				while ((tmp = bfr.readLine()) != null) {
					sb.append(tmp);
				}
			} catch (IOException ioe) {
				// close file to avoid file leak
				if (bfr != null)
					bfr.close();
				throw ioe;
			}
			
			// if cell has an entry in the formatted unmarshaller table, do that here now...
			if (m_um.containsKey(m_type)) {
				m_um.get(m_type).process(this, out_file, c2, rid);
			}
			return new StringCell(sb.toString());
		}
		throw new IOException("Cannot unmarshal "+m_name+": unknown format "+m_type);
	}*/


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
	
	public boolean isOutputFeatures() {
		return getType().equals("featout"); // && isOutput() ???
	}
	
	/**
	 * Factory method for instantiating the correct setting from the field=value pairs in the map...
	 */
	public static ProgramSetting make(HashMap<String,String> attrs) throws InvalidSettingsException {
		String dv= attrs.get("default-value");
		String n = attrs.get("name");
		String t = attrs.get("type").trim().toLowerCase();
		ProgramSetting ps = null;
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
	
	/**
	 * Responsible for adding the necessary columns for data associated with this setting in 
	 * the output table (if any). The columns must be appended to out_cols by the implementation.
	 * 
	 * @param out_cols guaranteed non-<code>null</code>
	 */
	public abstract void addFormattedColumns(List<DataColumnSpec> out_cols);
	

	public boolean isNumber() {
		return (this instanceof NumberSetting);
	}
	
	public void setMinMax(String min_bound, String max_bound) {
		// NO-OP: only NumberSetting overrides this method
	}
}
