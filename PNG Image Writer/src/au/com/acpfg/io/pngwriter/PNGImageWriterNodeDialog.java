package au.com.acpfg.io.pngwriter;

import javax.swing.JFileChooser;

import org.knime.core.data.StringValue;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "PNGImageWriter" Node.
 * Writes PNGImageCell's to disk as separate files, based on user configuration. Ideal for saving graphical results from other nodes to files which can then be edited...
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au
 */
public class PNGImageWriterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring PNGImageWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PNGImageWriterNodeDialog() {
        super();
       
        addDialogComponent(new DialogComponentFileChooser(
        				new SettingsModelString(PNGImageWriterNodeModel.CFGKEY_FOLDER, ""),
        				"folder-history", JFileChooser.OPEN_DIALOG, true));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        				new SettingsModelColumnName(PNGImageWriterNodeModel.CFGKEY_FILENAME_COL, ""),
        				"Column with Filenames", 0, true, false, StringValue.class));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(PNGImageWriterNodeModel.CFGKEY_PNG_COL, ""),
				"Column with PNG images", 0, true, false, PNGImageCell.getPreferredValueClass()));
    }
}

