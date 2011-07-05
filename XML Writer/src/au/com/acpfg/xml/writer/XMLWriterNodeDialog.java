package au.com.acpfg.xml.writer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.com.acpfg.xml.reader.XMLCell;

/**
 * <code>NodeDialog</code> for the "XMLWriter" Node.
 * Saves XMLcell's to disk as separate XML documents. 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class XMLWriterNodeDialog extends DefaultNodeSettingsPane {

    protected XMLWriterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(XMLWriterNodeModel.CFGKEY_XML_COL, "XML Data"), "XML Column", 0, true, false, new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				if (colSpec != null && colSpec.getType().equals(XMLCell.TYPE)) {
					return true;
				}
				return false;
			}

			@Override
			public String allFilteredMsg() {
				return "No XML columns available for selection!";
			}
        	
        }));
        
        createNewGroup("Filename conventions");
        final SettingsModelBoolean sb_use_rowid = new SettingsModelBoolean(XMLWriterNodeModel.CFGKEY_USE_ROWID, true);
        final SettingsModelString sb_basename = new SettingsModelString(XMLWriterNodeModel.CFGKEY_BASENAME, "document");
        sb_basename.setEnabled(!sb_use_rowid.getBooleanValue());
        addDialogComponent(new DialogComponentString(new SettingsModelString(XMLWriterNodeModel.CFGKEY_EXTN, ".xml"), "Filename extension"));
        addDialogComponent(new DialogComponentBoolean(sb_use_rowid, "Use Row ID?"));
        addDialogComponent(new DialogComponentString(sb_basename, "Basename"));
        sb_use_rowid.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				sb_basename.setEnabled(!sb_use_rowid.getBooleanValue());
			}
        	
        });
        
        createNewGroup("Save XML files to folder...");
        addDialogComponent(new DialogComponentString(new SettingsModelString(XMLWriterNodeModel.CFGKEY_FOLDER, "c:/temp"), "Folder to save to..."));
    }
}

