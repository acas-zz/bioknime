package au.com.acpfg.misc.jemboss.settings;

import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;

import au.com.acpfg.misc.jemboss.local.JEmbossProcessorNodeModel;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Output files produced by emboss programs eg. featout are handled by this setting.
 * An output file, unlike a datafile setting, has three modes of operation:
 * 1) concatenation of each result to a single file (as specified by the user)
 * 2) discard the result file
 * 3) output to a column in the KNIME table, using a cell type appropriate to the data
 * This class uses the superclass(es) where it can and overrides the rest.
 * 
 * @author andrew.cassin
 *
 */
public class OutputFileSetting extends DataFileSetting {
	private File m_file;		// NB: this is NOT persisted, but calculated when executing
	private boolean m_to_file, m_to_discard, m_to_col;
	
	public OutputFileSetting(HashMap<String,String> attrs) {
		super(attrs);
		// for output files, we dont want the default folder to be an EMBOSS directory,
		// so override that
		try {
			File tmp_file = File.createTempFile("prefix", "suffix");
			setFolder(tmp_file.getParentFile());
			tmp_file.delete();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	@Override 
	public JComponent make_widget(DataTableSpec dt) {
		JComponent file_selector = super.make_widget(dt);
		JPanel ret = new JPanel();
		ret.setLayout(new FlowLayout());
		JPanel file_panel = new JPanel();
		file_panel.setLayout(new FlowLayout());
		final JRadioButton rb_file = new JRadioButton("to file");
		final JRadioButton rb_discard = new JRadioButton("discard output");
		final JRadioButton rb_col = new JRadioButton("to column");

		ChangeListener cl = new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent src) {
				if (src.getSource().equals(rb_file)) {
					rb_file.setSelected(true);
					rb_discard.setSelected(false);
					rb_col.setSelected(false);
				} else if (src.getSource().equals(rb_discard)) {
					rb_file.setSelected(false);
					rb_discard.setSelected(true);
					rb_col.setSelected(false);
				} else { // must be rb_col
					rb_file.setSelected(false);
					rb_discard.setSelected(false);
					rb_col.setSelected(true);
				}
				m_to_file = rb_file.isSelected();
				m_to_discard = rb_discard.isSelected();
				m_to_col  = rb_col.isSelected();
			}
			
		};
		rb_file.addChangeListener(cl);
		rb_discard.addChangeListener(cl);
		rb_col.addChangeListener(cl);
		
		file_panel.add(rb_file);
		file_panel.add(file_selector);
		ret.add(file_panel);
		ret.add(rb_discard);
		ret.add(rb_col);
		return ret;
	}
	
	@Override 
	public void getArguments(ProgramSettingsListener l) throws IOException {
		m_file = null;
		// cpgplot is very fussy about the -outfeat argument, so we cater to some of its whims here...
		if (getType().equals("featout")) {
			m_file = File.createTempFile("output-features", ".gff3", JEmbossProcessorNodeModel.get_tmp_folder());
	    } else if (getType().indexOf("seq") >= 0) {
	    	// HACK BUG: the only sequence format currently supported is FASTA
	    	m_file = File.createTempFile(getType(), ".fasta");
		} else {
			m_file = File.createTempFile("jemboss-node", "."+getName()+"out");
		}
		l.addOutputFileArgument(this, "-"+getName());
	}
	

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("featout") || acd_type.equals("outfile") || acd_type.equals("report"))
			return true;
		return false;
	}

	/**
	 * Returns true if the file specified by the setting for output does not exist at this time
	 * or it exists but has no data. Subclasses are free to override.
	 * BUG: this code has an inherent race condition given the time between being invoked and
	 *      the time the io operation takes place. 
	 *      
	 * @return <code>true</code> if the file is safe to delete, <code>false</code> otherwise
	 */
	public boolean isSafeToDelete() {
		assert(m_file != null);
		if (m_file.exists() && m_file.length() > 0) {
			return false;
		}
		return true;
	}
	
	/**
	 * This method should only be called when building an emboss command line and returns the
	 * <code>java.io.File</code> instance which represents the output file
	 * @return
	 */
	public File getFile() {
		assert(m_file != null);
		return m_file;
	}
	
	/**
	 * Returns the filename appropriate for the setting. Some programs require
	 * an absolute path, others just the name and this method tailors the name based on
	 * the program being invoked
	 */
	@Override
	public String getFileName() {
		if (getType().equals("featout")) {
			return m_file.getName();
		}
		return m_file.getAbsolutePath();
	}
}
