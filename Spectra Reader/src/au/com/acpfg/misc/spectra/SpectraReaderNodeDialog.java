package au.com.acpfg.misc.spectra;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "MzXMLReader" Node.
 * Using the jrap-stax library, this node reads mzXML/mzML
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SpectraReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring SpectraReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected SpectraReaderNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
                new SettingsModelString(
                    SpectraReaderNodeModel.CFGKEY_SPECTRA_FOLDER,
                    SpectraReaderNodeModel.DEFAULT_SPECTRA_FOLDER), "spectra-folder-history", JFileChooser.OPEN_DIALOG, true));
        
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_LOAD_SPECTRA, false), "Load Spectra?"));
    
        createNewGroup("File Formats to load");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_MZML, true), "mzML/mzXML"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_MGF, true), "Mascot Generic Format (MGF)")); 
        
    }
}

