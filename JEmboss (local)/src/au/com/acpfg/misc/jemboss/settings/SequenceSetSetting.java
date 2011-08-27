package au.com.acpfg.misc.jemboss.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

/**
 * 
 * @author andrew.cassin
 *
 */
public class SequenceSetSetting extends DataFileSetting {
	
	public SequenceSetSetting(HashMap<String,String> attrs) {
		super(attrs);
	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("seqset"));
	}
	
	@Override 
	protected JPanel make_datafile_panel(final File initial_folder) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		final JButton      open_file_button = new JButton("   Select File...   ");
		open_file_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
					JFileChooser fc = (initial_folder == null) ? new JFileChooser() : new JFileChooser(initial_folder);
					fc.setDialogTitle("Please select a FASTA file...");
					fc.addChoosableFileFilter(new FileFilter() {

						@Override
						public boolean accept(File arg0) {
							if (arg0.isDirectory())
								return true;
							String name = arg0.getName().toLowerCase();
							return (name.endsWith(".fasta") || name.endsWith(".fsa"));
						}

						@Override
						public String getDescription() {
							return "FASTA Sequence File (.fsa, .fasta)";
						}
						
					});
					int   returnVal = fc.showOpenDialog(open_file_button);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						setFile(fc.getSelectedFile());
					    open_file_button.setText(getFileName());
					}
			}
		});
		
		p.add(open_file_button);
		return p;
	}
}
