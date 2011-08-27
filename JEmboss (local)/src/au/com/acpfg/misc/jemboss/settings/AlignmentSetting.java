package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Setting to represent a multiple-sequence alignment (or pairwise) in the usual formats eg. CLUSTAL 
 * @author andrew.cassin
 *
 */
public class AlignmentSetting extends OutputFileSetting {
	public AlignmentSetting(HashMap<String,String> attrs) {
		super(attrs);
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub

	}

	@Override
	public DataCell unmarshal(File out_file, BufferedDataContainer c2,
			String rid) throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addFormattedColumns(List<DataColumnSpec> out_cols) {
		// TODO Auto-generated method stub

	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("align"));
	}

}
