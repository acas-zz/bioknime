package au.com.acpfg.misc.spectra;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JList;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;

public class SpectraVisualRenderer implements DataValueRenderer {

	@Override
	public boolean accepts(DataColumnSpec spec) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDescription() {
		return "Spectra Graphical Plot";
	}

	@Override
	public Dimension getPreferredSize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component getRendererComponent(Object val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int arg4, int arg5) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component getListCellRendererComponent(JList arg0, Object arg1,
			int arg2, boolean arg3, boolean arg4) {
		// TODO Auto-generated method stub
		return null;
	}

}
