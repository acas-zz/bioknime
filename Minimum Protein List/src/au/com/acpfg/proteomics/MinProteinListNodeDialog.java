package au.com.acpfg.proteomics;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "MinProteinList" Node.
 * Uses a greedy set cover algorithm to identify the minimal set of proteins which can explain the observed peptides
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class MinProteinListNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the MinProteinList node.
     */
    protected MinProteinListNodeDialog() {
    	final SettingsModelString matches = new SettingsModelString(MinProteinListNodeModel.CFGKEY_PEPTIDES, "Peptides");
    	final SettingsModelString accsn   = new SettingsModelString(MinProteinListNodeModel.CFGKEY_PROTEIN, "Protein");
    	final String[] items = new String[] {
    		"Minimum Set Cover (all proteins equal cost)",
    		"Minimum Set Cover (Unique Peptide Weighting, experimental)"
    	};
        addDialogComponent(new DialogComponentColumnNameSelection(accsn,   "Accession Column", 0, true, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(matches, "Matching Peptides Column", 0, true, StringValue.class));
        addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(MinProteinListNodeModel.CFGKEY_SOLVER, "c:/cygwin/bin/glpsol.exe"), "glpk-solver-history"));
        addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MinProteinListNodeModel.CFGKEY_ALGO, items[0]), "Algorithm", items));
    }
}

