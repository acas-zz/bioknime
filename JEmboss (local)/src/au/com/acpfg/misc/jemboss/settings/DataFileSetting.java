package au.com.acpfg.misc.jemboss.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.JEmbossProcessorNodeModel;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Emboss provides a data folder with many data files in it, for use by various programs. This
 * setting models that and provides a suitable file selection interface to be able to specify these files
 * (or something outside of emboss if desired)
 * 
 * @author andrew.cassin
 *
 */
public class DataFileSetting extends ProgramSetting {
	private File m_file;
	private File m_folder;
	
	public DataFileSetting(HashMap<String,String> attrs) {
		this(attrs, JEmbossProcessorNodeModel.getEmbossDataFolder());
	}
	
	public DataFileSetting(HashMap<String,String> attrs, File default_folder) {
		super(attrs);
		m_file   = null;
		setFolder(default_folder);
		if (attrs.containsKey("file")) {
			m_file = new File(attrs.get("file"));
		}
		if (attrs.containsKey("initial-folder")) {
			setFolder(new File(attrs.get("initial-folder")));
		}
	}
	
	protected void setFolder(File new_folder) {
		m_folder = new_folder;
	}
	
	protected void setFile(File new_file) {
		m_file = new_file;
	}
	
	public String getFileName() {
		if (m_file == null)
			return "No file selected";
		return m_file.getName();
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		String t = getType();
		if (t.equals("datafile")) {
			return make_datafile_panel(JEmbossProcessorNodeModel.getEmbossDataFolder());
		} else {	// eg. infile
			return make_datafile_panel(m_folder);
		}
	}
	
	protected JPanel make_datafile_panel(final File initial_folder) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		final JButton      open_file_button = new JButton("   Select File...   ");
		open_file_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
					JFileChooser fc = (initial_folder == null) ? new JFileChooser() : new JFileChooser(initial_folder);
					fc.setDialogTitle("Save "+getName()+" to...");
					int   returnVal = fc.showSaveDialog(open_file_button);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						m_file = fc.getSelectedFile();
					    open_file_button.setText(m_file.getName());
					}
			}
		});
		
		p.add(open_file_button);
		return p;
	}

	@Override
	public void copy_attributes(HashMap<String,String> atts) {
		super.copy_attributes(atts);
		if (m_file != null)		// any file chosen?
			atts.put("file", m_file.getAbsolutePath());
		atts.put("initial-folder", m_folder.getAbsolutePath());
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws IOException {
		if (m_file != null)
			l.addInputFileArgument(this, "-"+getName(), m_file);
		// else do nothing since user has not selected a file ie. omit argument from command line as it is optional
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub
		
	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("datafile") || acd_type.equals("infile"))
			return true;
		return false;
	}
}
