package au.com.acpfg.phylogeny.writer;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.com.acpfg.align.muscle.AlignmentValue;

/**
 * <code>NodeDialog</code> for the "AlignmentWriter" Node.
 * Saves one or more alignments to disk
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class AlignmentWriterNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelString m_column = new SettingsModelString(AlignmentWriterNodeModel.CFGKEY_COLUMN, "Alignment");
	private final SettingsModelString m_folder = new SettingsModelString(AlignmentWriterNodeModel.CFGKEY_FOLDER, "c:/temp");
	private final SettingsModelString m_format = new SettingsModelString(AlignmentWriterNodeModel.CFGKEY_FORMAT, "Clustal");
	
    /**
     * New pane for configuring AlignmentWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected AlignmentWriterNodeDialog() {
        super();
   
        addDialogComponent(new DialogComponentFileChooser(m_folder, "folder-history", JFileChooser.SAVE_DIALOG, true));
        addDialogComponent(new DialogComponentColumnNameSelection(m_column, AlignmentWriterNodeModel.CFGKEY_COLUMN, 
        						0, true, false, AlignmentValue.class));
        addDialogComponent(new DialogComponentStringSelection(m_format, "Alignment format to save to", new String[] { "Clustal", "NEXUS" }));
    }
}

