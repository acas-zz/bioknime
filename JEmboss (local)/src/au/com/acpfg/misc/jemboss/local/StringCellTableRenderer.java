package au.com.acpfg.misc.jemboss.local;

import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Displays string cells as JLabel's
 * @author andrew.cassin
 *
 */
public class StringCellTableRenderer implements TableCellRenderer {
	private static final Map<TextAttribute,Object> attrs = new HashMap<TextAttribute,Object>();

	// parameter name is always large-ish bold
	static {
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		attrs.put(TextAttribute.FAMILY, Font.SANS_SERIF);
		attrs.put(TextAttribute.SIZE, new Float(14.0));
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable tbl, Object val,
			boolean isSelected, boolean hasFocus, int row, int col) {
		JLabel lbl = new JLabel(val.toString());
		if (col == 0) {
			Font f = Font.getFont(attrs);
			lbl.setFont(f);
		}
		return lbl;
	}

}
