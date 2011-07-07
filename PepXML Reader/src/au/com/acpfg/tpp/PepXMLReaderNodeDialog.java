package au.com.acpfg.tpp;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "PepXMLReader" Node.
 * Reads PepXML (as produced by the trans-proteomics pipeline) to enable processing of peptide/protein identifications and statistics using KNIME
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class PepXMLReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring PepXMLReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PepXMLReaderNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(PepXMLReaderNodeModel.CFGKEY_FILE, ""), "xml-file-history", JFileChooser.OPEN_DIALOG, ".xml"));
    }
}

