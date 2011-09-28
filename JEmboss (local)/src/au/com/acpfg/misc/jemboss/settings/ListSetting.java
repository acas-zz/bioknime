package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Provides a setting which permits the user to select a single value from a list
 * @author andrew.cassin
 *
 */
public class ListSetting extends ProgramSetting {
	private String m_list_items;
	private String m_val;
	
	public ListSetting(HashMap<String,String> attrs) {
		super(attrs);
		
		if (attrs.containsKey("current-selection")) {
			m_val = attrs.get("current-selection");
		} else {
			m_val = getDefaultValue();
		}
		
		setListItems(attrs);
	}

	protected void setListItems(HashMap<String,String> attrs) {
		// ACD parser called us?
		if (attrs.containsKey("list-values")) {
			Pattern p2 = Pattern.compile("\\s*\"([^\"]+?)\"");
			Matcher m2 = p2.matcher(attrs.get("list-values"));
			if (m2.find()) {
				m_list_items = m2.group(1);
			}
		} else {
			assert(attrs.containsKey("list-items"));
			m_list_items = attrs.get("list-items");
		}
	}
	
	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		// TODO: support list selection min & max values (and default selection)
		String[] values = m_list_items.split("[\\;,]");
		for (int i=0; i<values.length; i++) {
			values[i] = values[i].replaceAll("\\s\\s+", " ").trim();
		}
		JList jl = new JList(values);
		jl.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				m_val = ((JList)arg0.getSource()).getSelectedValue().toString();
			}
			
		});
		jl.setSelectedIndex(0);
		return new JScrollPane(jl);
	}

	@Override
	public void copy_attributes(HashMap<String,String> attrs) {
		super.copy_attributes(attrs);
		attrs.put("list-items", m_list_items);
		attrs.put("current-selection", m_val);
	}

	protected void setValue(String new_val) {
		m_val = new_val;
	}
	
	protected String getSelectedValue() {
		return m_val;
	}
	
	@Override
	public void getArguments(ProgramSettingsListener l) throws InvalidSettingsException {
		String t = getType();
		if (t.equals("list")) {
			int idx = m_val.indexOf(':');
			if (idx < 1) {
				throw new InvalidSettingsException("Cannot find list value for "+getName()+": "+m_val );
			}
			String val = m_val.substring(0, idx);
			l.addArgument(this, new String[] {"-"+getName(), val});
		}
				
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("list") || acd_type.equals("selection"))
			return true;
		return false;
	}

}
