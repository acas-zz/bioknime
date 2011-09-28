package au.com.acpfg.misc.jemboss.settings;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Represents an integer or floating-point number and provides an type-safe interface specific to the
 * number being edited
 * 
 * @author andrew.cassin
 *
 */
public class NumberSetting extends StringSetting {
	private String  m_lowerbound, m_upperbound;

	public NumberSetting(HashMap<String,String> attrs) {
		super(attrs);
		m_lowerbound = "";
		m_upperbound = "";
		if (attrs.containsKey("lowerbound") || attrs.containsKey("upperbound")) {
			setMinMax(attrs.get("lowerbound"), attrs.get("upperbound"));
		}
	}

	// TODO: provide a way to remove bound?
	protected void setMinimum(String min_val) {
		if (min_val != null && min_val.length() > 0) {
			m_lowerbound = min_val;
		} else {
			m_lowerbound = "";
		}
	}

	// TODO: provide a way to remove bound?
	protected void setMaximum(String max_val) {
		if (max_val != null && max_val.length() > 0) {
			m_upperbound = max_val;
		} else {
			m_upperbound = "";
		}
	}
	
	public final void setMinMax(String min_bound, String max_bound) {
		setMinimum(min_bound);
		setMaximum(max_bound);
	}
	
	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		JPanel    p = new JPanel();
		JSpinner sp = new JSpinner();
		sp.setPreferredSize(new Dimension(100,25));
		
		sp.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				JSpinner sp = ((JSpinner) arg0.getSource());
				SpinnerNumberModel mdl = ((SpinnerNumberModel) sp.getModel());
				setValue(mdl.getValue().toString());
			}
			
		});
		String t = getType();
		boolean is_real = t.equals("float") || t.equals("double");
		sp.setModel(make_number_model(is_real, getDefaultValue(), m_lowerbound, m_upperbound));
		setValue(sp.getValue().toString());
		p.add(sp);
		p.add(Box.createVerticalGlue());
		return p;
	}

	@Override
	public void copy_attributes(HashMap<String,String> attrs) {
		super.copy_attributes(attrs);
		attrs.put("lowerbound", m_lowerbound);
		attrs.put("upperbound", m_upperbound);
	}

	@Override
	public void getArguments(ProgramSettingsListener l) {
		l.addArgument(this, new String[] { "-"+getName(), getValue() });
	}

	/**
	 * Returns a JSpinner number model instance which satisfies the specified constraints
	 * @param default: default value (if any)
	 * @param lower: lowerbound value (if any)
	 * @param upper: upperbound value (if any)
	 * This routine ignores, but logs, number constraints which cannot be resolved at configure-time. This routine is carefully
	 * written so that the model has the correct type for the type of ProgramSetting (eg. float or integer)
	 */
	protected SpinnerNumberModel make_number_model(boolean is_real, String default_val, String lower_val, String upper_val) {
		//Logger.getAnonymousLogger().info(is_real+" <"+default_val+"> <"+lower_val+"> <"+upper_val+">");
		
		Number val   = new Integer(0);
		Number lower = new Integer(Integer.MIN_VALUE);
		Number upper = new Integer(Integer.MAX_VALUE);
		Number step_size = new Integer(1);
		boolean has_lower = false;
		boolean has_upper = false;

		if (is_real) {
			val   = new Double(0.0);
			lower = new Double(-Double.MAX_VALUE);
			upper = new Double(Double.MAX_VALUE);
			step_size = new Double(1.0);
		}
		
		if (default_val.length() > 0) {
			try {
				if (is_real)
					val = new Double(default_val);
				else 
					val = new Integer(default_val);
			} catch (NumberFormatException nfe) {
				Logger.getAnonymousLogger().warning("Cannot set default: "+default_val);	// fallthru for bounds anyway...
			}
		}
		
		if (lower_val.length() > 0) {
			try {
				if (is_real) 
					lower = new Double(lower_val);
				else 
					lower = new Integer(lower_val);
				has_lower = true;
			} catch (NumberFormatException nfe) {
				Logger.getAnonymousLogger().warning("Cannot set lower bound: "+lower_val);
			}
		}
		
		if (upper_val.length() > 0) {
			try {
				if (is_real)
					upper = new Double(upper_val);
				else 
					upper = new Integer(upper_val);
				has_upper = true;
			} catch (NumberFormatException nfe) {
				Logger.getAnonymousLogger().warning("Cannot set upper bound: "+upper_val);
			}
		}
		
		// TODO: if an ACD variable is specified, we cant resolve them at this time, so for safety...
		//Logger.getAnonymousLogger().info("val: <"+val+"> lower<"+lower+"> upper <"+upper+">");
		if (has_lower && ((Comparable) val).compareTo(lower) < 0 ) {		// val less than lower bound?
			val = lower;
		}
		if (has_upper && ((Comparable) val).compareTo(upper) > 0) {			// val greater than upper bound?
			val = upper;
		}
		return new SpinnerNumberModel(val, (Comparable) lower, (Comparable) upper, step_size);
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub
		
	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("integer") || acd_type.equals("float") || acd_type.equals("real")) {
			return true;
		}
		return false;
	}

}
