package au.com.acpfg.misc.jemboss.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Implements an object capable of representing ACD boolean or toggle settings
 * @author andrew.cassin
 *
 */
public class BooleanSetting extends ProgramSetting {
	private Boolean m_val;
	
	protected BooleanSetting(HashMap<String,String> attrs) {
		super(attrs);
		if (attrs.containsKey("current-value")) {
			m_val = new Boolean(attrs.get("current-value"));
		} else {
			m_val = new Boolean(true);
		}
	}

	@Override
	public String getColumnName() {
		// never taken from a column (yet)
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		boolean bv = false;
		String dv = getDefaultValue();
		if (dv.length()>0) {
			char c = dv.toLowerCase().charAt(0);
			if (c == 'y' || c == 't'|| c == '1')
				bv = true;
		}
		JCheckBox jcb = new JCheckBox("", bv);
		jcb.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_val = new Boolean(((JCheckBox)e.getSource()).isSelected());
			}
			
		});
		m_val = new Boolean(bv);
		return jcb;
	}

	@Override
	public void copy_attributes(HashMap<String,String> atts) {
		super.copy_attributes(atts);
		atts.put("current-value", m_val.toString());
	}
	
	@Override
	public void getArguments(ProgramSettingsListener l) {
		String t = getType();
		if (t.equals("bool") || t.equals("toggle")) {
			String head = "";
			if (m_val.equals("false"))
				head = "no";
			l.addArgument(this, new String[] { "-"+head+getName() });
		} else if (t.equals("boolean")) {
			l.addArgument(this, new String[] { "-"+getName(), m_val.booleanValue() ? "Y" : "N" });
		} 
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unmarshal(File out_file, AbstractTableMapper om) 
						throws IOException, InvalidSettingsException {
	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("boolean") || acd_type.equals("toggle"))
			return true;
		return false;
	}

	@Override
	public void addColumns(AbstractTableMapper om) {
		// TODO Auto-generated method stub
		
	}

}
