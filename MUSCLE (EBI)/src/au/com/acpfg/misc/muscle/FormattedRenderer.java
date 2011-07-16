package au.com.acpfg.misc.muscle;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;

/*
 * Displays an alignment in CLUSTALW format inside a KNIME table cell (HTML pre-formatted) 
 */
public class FormattedRenderer implements DataValueRenderer {
	public enum FormatType { F_CLUSTALW, F_PLAIN, F_PHYLIP_INTERLEAVED, F_PHYLIP_SEQUENTIAL};
	
	private FormatType m_format;
	
	
	public FormattedRenderer(FormatType format) {
		m_format = format;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (!(value instanceof MultiAlignmentCell)) {
			return new JLabel();
		}
		MultiAlignmentCell mac = (MultiAlignmentCell) value;
		String alignment = mac.getFormattedAlignment(m_format);
		return new JLabel("<html><pre>"+alignment);
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		switch (m_format) {
		case F_CLUSTALW:
			return "CLUSTALW";
		case F_PHYLIP_INTERLEAVED:
			return "Phylip (Interleaved)";
		case F_PHYLIP_SEQUENTIAL:
			return "Phylip (Sequential)"; 
		default:
			return "Plain";
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400,300);
	}

	@Override
	public Component getRendererComponent(Object val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean accepts(DataColumnSpec spec) {
		return (spec != null && spec.getType().isCompatible(AlignmentValue.class));
	}

}
