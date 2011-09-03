package au.com.acpfg.misc.jemboss.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Implements an (unchecked) range of values. Enforcement of the user-entered data is left to EMBOSS
 * 
 * @author andrew.cassin
 *
 */
public class RangeSetting extends ProgramSetting {
	private String m_lower;
	private String m_upper;
	
	protected RangeSetting(HashMap<String, String> attrs) {
		super(attrs);
		if (attrs.containsKey("lower"))
			m_lower = attrs.get("lower");
		else
			m_lower = getDefaultValue();
		if (attrs.containsKey("upper"))
			m_upper = attrs.get("upper");
		else
			m_upper = getDefaultValue();
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		return make_range_panel();
	}
	
	private JComponent make_range_panel() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		JTextField f1 = new JTextField(5);
		JTextField f2 = new JTextField(5);
		f1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_lower = ((JTextField)e.getSource()).getText();
			}
			
		});
		f2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				m_upper = ((JTextField)e.getSource()).getText();
			}
			
		});
		p.add(f1);
		p.add(new JLabel(" - "));
		p.add(f2);
		p.add(Box.createHorizontalGlue());
		return p;
	}
	
	@Override
	public void copy_attributes(HashMap<String,String> attrs) {
		super.copy_attributes(attrs);
		attrs.put("lowerbound", m_lower);
		attrs.put("upperbound", m_upper);
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("range"))
			return true;
		return false;
	}

	@Override
	public void addColumns(AbstractTableMapper om) {
		// NO-OP
	}

	@Override
	public void unmarshal(File out_file, AbstractTableMapper om) throws IOException,
			InvalidSettingsException {
		// NO-OP
	}

}
