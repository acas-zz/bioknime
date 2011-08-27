package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

public class DummySetting extends ProgramSetting {

	public DummySetting(HashMap<String,String> attrs) {
		super(attrs);
	}
	
	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public DataType getCellType() {
		return null;
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws Exception {
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
	}

	@Override
	public DataCell unmarshal(File out_file, BufferedDataContainer c2,
			String rid) throws IOException, InvalidSettingsException {
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		return new JLabel(getName()+": "+getType());
	}
	
	@Override
	public void addFormattedColumns(List<DataColumnSpec> out_cols) {
	}
	
	public static boolean canEmboss(String acd_type) {
		return true;
	}

}
