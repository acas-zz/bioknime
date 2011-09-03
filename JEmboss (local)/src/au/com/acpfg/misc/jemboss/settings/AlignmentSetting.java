package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;

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
	public void unmarshal(File out_file, AbstractTableMapper om) {
		
	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("align"));
	}

}
