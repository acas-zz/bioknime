package au.com.acpfg.align.phobius;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;


/**
 * <code>NodeDialog</code> for the "PhobiusSource" Node.
 * Takes a list of sequences and appends the results of Phobius webservice invocations (text only for now) to the output port
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class PhobiusSourceNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring PhobiusSource node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PhobiusSourceNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(PhobiusSourceNodeModel.make_as_string(PhobiusSourceNodeModel.CFGKEY_SEQUENCE_COL), 
        		"Protein Sequence column:", 0, true, false, StringValue.class));
        
        addDialogComponent(new DialogComponentString(PhobiusSourceNodeModel.make_as_string(PhobiusSourceNodeModel.CFGKEY_EMAIL), "Email address (required by EBI):"));
        
    }
}

