package au.com.acpfg.misc.jemboss.settings;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;

/**
 * Stores a pattern (regexp) as supported by EMBOSS
 * @author andrew.cassin
 *
 */
public class RegexpSetting extends StringSetting {

	public RegexpSetting(HashMap<String, String> attrs) {
		super(attrs);
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		JTextField tf = new JTextField(30);
		tf.setPreferredSize(new Dimension(100,25));
		tf.setText(getDefaultValue());
		tf.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setValue(((JTextField)arg0.getSource()).getText());
			}
			
		});
		return tf;
	}
	
	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("regexp") || acd_type.equals("pattern")) {
			return true;
		}
		return false;
	}
}
