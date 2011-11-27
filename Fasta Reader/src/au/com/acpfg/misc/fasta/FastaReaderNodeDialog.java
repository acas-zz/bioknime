package au.com.acpfg.misc.fasta;

import org.knime.core.node.defaultnodesettings.*;
import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <code>NodeDialog</code> for the "FastaReader" Node.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: * n1) Accession * n2) Description - often not accurate in practice * n3) Sequence data * n * nNo line breaks are preserved.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class FastaReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Establish the configurable parameters associated with reading the FASTA file. Note how we can
     * tailor the regular expressions to match the description line as we see fit. If any fail to match,
     * no sequence will be output - so you can use this to select just sequences of interest.
     */
    protected FastaReaderNodeDialog() {
        super();
        
        this.createNewGroup("FASTA files to load:");
        final SettingsModelBoolean     is_dir = new SettingsModelBoolean(FastaReaderNodeModel.CFGKEY_ISDIR, false);
        final SettingsModelString single_file = FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_FASTA);
        single_file.setEnabled(!is_dir.getBooleanValue());
        final SettingsModelString single_dir  = FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_FASTADIR);
        single_dir.setEnabled(is_dir.getBooleanValue());
        addDialogComponent(new DialogComponentBoolean(is_dir, "Load entire folder?"));
        addDialogComponent(new DialogComponentFileChooser(single_file,"fasta-history",JFileChooser.OPEN_DIALOG,
        				".fasta|.fa|.txt|.seq",
        				".fasta.gz|.fa.gz|.txt.gz|.seq.gz",
        				".fasta.z|.fa.z|.txt.z|.seq.z",
        				".fsa|.fsa.gz|.fsa.z"
        ));
        addDialogComponent(new DialogComponentFileChooser(single_dir, "fasta-dir-history", JFileChooser.OPEN_DIALOG, true, ""));
        this.closeCurrentGroup();
        
        is_dir.addChangeListener(new ChangeListener() {
        	public void stateChanged(final ChangeEvent e) {
        		single_file.setEnabled(!is_dir.getBooleanValue());
        		single_dir.setEnabled(is_dir.getBooleanValue());
        	}
        });
        
        addDialogComponent(new DialogComponentString(FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_ACCSN_RE), "Accession Regular Expression:"));
        addDialogComponent(new DialogComponentString(FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_DESCR_RE), "Description Regular Expression:"));
        
        String labels[] = new String[] {"First entry only", "All entries (as collection)"};
        String actions[]= new String[] {"single", "collection"};
        addDialogComponent(new DialogComponentButtonGroup(FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_ENTRY_HANDLER), "Entry Handler", false, labels, actions));
      
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(FastaReaderNodeModel.CFGKEY_MAKESTATS, 
        		Boolean.FALSE), "Compute stats for sequences (slow & memory intensive)?"));
    }
}
