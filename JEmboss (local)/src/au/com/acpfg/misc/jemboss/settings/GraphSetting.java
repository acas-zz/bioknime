package au.com.acpfg.misc.jemboss.settings;

import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataTableSpec;

import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

public class GraphSetting extends ListSetting {

	public GraphSetting(HashMap<String,String> attrs) {
		super(attrs);
		attrs.put("list-items", "PNG: Portable Network Graphics;"+
				"PDF: Adobe Portable Document Format;"+
				"SVG: Scalable Vector Graphics");
		setListItems(attrs);
	}
	
	@Override
	public void getArguments(ProgramSettingsListener l) {
		l.addArgument(this, new String[] { "-"+getName(), getSelectedValue().substring(0,3).toLowerCase() });
	}
	
	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("graph") || acd_type.equals("xgraph") || acd_type.equals("xygraph")) {
			return true;
		}
		return false;
	}
}
