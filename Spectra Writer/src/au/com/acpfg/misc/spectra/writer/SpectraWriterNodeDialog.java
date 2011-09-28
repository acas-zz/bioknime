package au.com.acpfg.misc.spectra.writer;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.com.acpfg.misc.spectra.AbstractSpectraCell;
import au.com.acpfg.misc.spectra.SpectralDataInterface;

/**
 * <code>NodeDialog</code> for the "SpectraWriter" Node.
 * Writes a spectra column out to disk for processing with other Mass Spec. software. Supports MGF format but does not guarantee that all input data will be preserved in the created file.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SpectraWriterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring SpectraWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected SpectraWriterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(SpectraWriterNodeModel.CFGKEY_FILE, ""), "file-history", JFileChooser.SAVE_DIALOG, false, null));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SpectraWriterNodeModel.CFGKEY_OVERWRITE, false), "Overwrite?"));
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(SpectraWriterNodeModel.CFGKEY_COLUMN, ""), "Column to save: ", 0, SpectralDataInterface.class));
        addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(SpectraWriterNodeModel.CFGKEY_FORMAT, "Mascot Generic Format"), "Output format", "Mascot Generic Format"));
    }
}

