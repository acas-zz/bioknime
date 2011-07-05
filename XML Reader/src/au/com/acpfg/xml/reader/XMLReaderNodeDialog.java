package au.com.acpfg.xml.reader;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "XMLReader" Node.
 * Loads XML documents (either in a folder or files) into XML cells for further processing by the XQuery Processor node
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class XMLReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring XMLReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected XMLReaderNodeDialog() {
        super();
        
        createNewGroup("Data Source");
        final SettingsModelBoolean load_folder = new SettingsModelBoolean(XMLReaderNodeModel.CFGKEY_LOAD_FOLDER, false);
        addDialogComponent(new DialogComponentBoolean(load_folder, "Load all XML files in folder?"));
       
        final SettingsModelString file_name   = new SettingsModelString(XMLReaderNodeModel.CFGKEY_FILE, "");
        final SettingsModelString folder_name = new SettingsModelString(XMLReaderNodeModel.CFGKEY_FOLDER, "");
        folder_name.setEnabled(false);
        file_name.setEnabled(true);
        addDialogComponent(new DialogComponentFileChooser(file_name, "filename-history", JFileChooser.OPEN_DIALOG, false));
        addDialogComponent(new DialogComponentFileChooser(folder_name, "folder-history", JFileChooser.OPEN_DIALOG, true));
        load_folder.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				boolean enabled = load_folder.getBooleanValue();
				file_name.setEnabled(!enabled);		// these two are always opposing each other
				folder_name.setEnabled(enabled);
			}
        	
        });
        
        createNewGroup("XML Processing Options");
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(XMLReaderNodeModel.CFGKEY_SINGLE_NS, true), "ignore namespaces?"));

    }
}

