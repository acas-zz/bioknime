package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.JEmbossProcessorNodeModel;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Setting which presents a list of scoring matrices to the user from EMBOSS (eg. during alignments)
 * 
 * @author andrew.cassin
 *
 */
public class MatrixSetting extends ProgramSetting {
	private String m_val;
	
	protected MatrixSetting(HashMap<String, String> attrs) {
		super(attrs);
		if (attrs.containsKey("current-matrix")) 
			m_val = attrs.get("current-matrix");
		else
			m_val = getDefaultValue();
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		JList jl = new JList(JEmbossProcessorNodeModel.getMatrices());
		jl.setVisibleRowCount(5);
		jl.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				m_val = ((JList)arg0.getSource()).getSelectedValue().toString();
			}
			
		});
		jl.setSelectedIndex(0);
		return new JScrollPane(jl);
	}

	@Override
	public void copy_attributes(HashMap<String,String> atts) {
		super.copy_attributes(atts);
		atts.put("current-matrix", m_val);
	}

	@Override
	public DataType getCellType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws Exception {
		// TODO Auto-generated method stub

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

	@Override
	public String getColumnName() {
		// reading a matrix from a column is not (yet) supported
		return null;
	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("matrixf") || acd_type.equals("matrixfile"))
			return true;
		return false;
	}

}
