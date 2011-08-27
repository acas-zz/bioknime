package au.com.acpfg.misc.jemboss.settings;

import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataTableSpec;

import au.com.acpfg.misc.jemboss.local.JEmbossProcessorNodeModel;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * A setting naming a codon usage table as provided by emboss. This is a simple-minded implementation.
 * Just like me ;-)
 * 
 * @author andrew.cassin
 *
 */
public class CodonUsageTableSetting extends StringSetting {
	/**
	 * Default codon usage table should one not be specified by the setting instance. Emboss specific value.
	 */
	public final static String DEFAULT_CUT = "Eyeast_cai.cut";

	
	public CodonUsageTableSetting(HashMap<String,String> attrs) {
		super(attrs);
	}
	
	@Override 
	public JComponent make_widget(DataTableSpec dt) {
		JList jl = new JList(JEmbossProcessorNodeModel.getCodons());
		jl.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				setValue(((JList)e.getSource()).getSelectedValue().toString());
			}
			
		});
		jl.setSelectedIndex(0);
		return new JScrollPane(jl);
	}
	
	@Override
	public void getArguments(ProgramSettingsListener l) {
		String v = getValue();
		
		String codon = DEFAULT_CUT;
		if (v != null && v.length() > 0) {
			codon = v;
		}
		l.addArgument(this, new String[] { "-"+getName(), codon });
	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("codon"));
	}
}
