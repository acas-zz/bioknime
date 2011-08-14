package au.com.acpfg.phylogeny;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "PhylogenyProcessor" Node.
 * Using the PAL library, as exported from MUSCLE node, this tree takes input data and performs tree construction, bootstrapping and other phylogenetic analyses as configured by the user.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class PhylogenyProcessorNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelString m_task = new SettingsModelString(PhylogenyProcessorNodeModel.CFG_TASK, "Calculate distance matrix");
	
    /**
     * New pane for configuring PhylogenyProcessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PhylogenyProcessorNodeDialog() {
        super();

        addDialogComponent(new DialogComponentStringSelection(m_task, "Task to perform:", 
        		new String[] { "Calculate Distance Matrix", 
        		               "DistTree: distance matrix methods" }));
        
        
        
    }
}

