package au.com.acpfg.align.muscle;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;

/**
 * Prints a summary of the alignment as a JLabel
 * @author andrew.cassin
 *
 */
public class AlignmentSummaryRenderer implements DataValueRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int arg4, int arg5) {
		return make_comp(arg1);
	}

	private Component make_comp(Object arg1) {
		if (arg1 == null || !(arg1 instanceof MultiAlignmentCell)) {
			return new JLabel();
		}
		return new JLabel("<html><pre>"+arg1.toString());
	}
	
	@Override
	public Component getListCellRendererComponent(JList arg0, Object arg1,
			int arg2, boolean arg3, boolean arg4) {
		return make_comp(arg1);
	}

	@Override
	public String getDescription() {
		return "Alignment Summary";
	}

	@Override
	public Component getRendererComponent(Object val) {
		return make_comp(val);
	}

	@Override
	public boolean accepts(DataColumnSpec spec) {
		return (spec != null && spec.getType().isCompatible(AlignmentValue.class));
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 300);
	}

}
