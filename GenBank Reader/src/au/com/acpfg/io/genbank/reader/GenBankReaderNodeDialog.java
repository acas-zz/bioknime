package au.com.acpfg.io.genbank.reader;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * <code>NodeDialog</code> for the "GenBankReader" Node.
 * Using BioJava, this node reads the specified files/folder for compressed genbank or .gb files and loads the sequences into a single table along with most of key metadata
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au
 */
public class GenBankReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring GenBankReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected GenBankReaderNodeDialog() {
        super();
        
        
        this.createNewGroup("Genbank file/folder to load:");
        final SettingsModelBoolean     is_file = new SettingsModelBoolean(FastGenbankNodeModel.CFGKEY_ISFILE, true);
        final SettingsModelString single_file = new SettingsModelString(FastGenbankNodeModel.CFGKEY_FILE, "");
        single_file.setEnabled(is_file.getBooleanValue());
        final SettingsModelString single_dir  = new SettingsModelString(FastGenbankNodeModel.CFGKEY_FOLDER, "c:/temp");
        single_dir.setEnabled(!is_file.getBooleanValue());
        addDialogComponent(new DialogComponentBoolean(is_file, "Load single file?"));
        is_file.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				single_dir.setEnabled(!is_file.getBooleanValue());
				single_file.setEnabled(is_file.getBooleanValue());
			}
        	
        });
        addDialogComponent(new DialogComponentFileChooser(single_file,"gb-history",JFileChooser.OPEN_DIALOG,
        				".gb|.seq|.gbk",
        				".gb.gz|.seq.gz|.gbk.gz"
        ));
        addDialogComponent(new DialogComponentFileChooser(single_dir, "gb-history", JFileChooser.OPEN_DIALOG, true, ""));
        this.closeCurrentGroup();
          
        createNewGroup("Data Selection");
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(FastGenbankNodeModel.CFGKEY_TAXONOMY_FILTER, ""), 
        		"Filter by taxonomy (space separated)", false, 80)
        );
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(FastGenbankNodeModel.CFGKEY_SOURCE_FEATURES, true), "Sample Source Features"));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(FastGenbankNodeModel.CFGKEY_CDS_FEATURES, true), "Coding Sequence Features"));
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(FastGenbankNodeModel.CFGKEY_FILENAME_FILTER, ""),
        		"Filter by filename (space separated)", false, 80)
        );
    }
}

