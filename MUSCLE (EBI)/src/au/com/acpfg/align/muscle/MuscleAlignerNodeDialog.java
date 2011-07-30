package au.com.acpfg.align.muscle;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.data.def.StringCell;

/**
 * <code>NodeDialog</code> for the "MuscleAccessor" Node.
 * Provides multiple alignment data from MUSCLE as implemented by EBI
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class MuscleAlignerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring MuscleAccessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected MuscleAlignerNodeDialog() {
        super();
        ColumnFilter cf = new ColumnFilter() {
			@Override
			public String allFilteredMsg() {
				return "No suitable collection columns found - MUSCLE requires a collection of sequences (see help)!";
			}

			@Override
			public boolean includeColumn(
					DataColumnSpec colSpec) {
				DataType dt = colSpec.getType();
				if (dt.isCollectionType()) {
					return true;
				}
				return false;
			}
        };
        
        addDialogComponent(new DialogComponentString(MuscleAlignerNodeModel.make_as_string(MuscleAlignerNodeModel.CFGKEY_EMAIL), 
        		           "Email Address", true, 30));
        addDialogComponent(new DialogComponentColumnNameSelection(MuscleAlignerNodeModel.make_as_string(MuscleAlignerNodeModel.CFGKEY_SEQ_COL),
        					"Sequence Collection", 0, cf));
        addDialogComponent(new DialogComponentColumnNameSelection(MuscleAlignerNodeModel.make_as_string(MuscleAlignerNodeModel.CFGKEY_ACCSN_COL),
    						"Accession Collection", 0, cf));
        addDialogComponent(new DialogComponentStringSelection(MuscleAlignerNodeModel.make_as_string(MuscleAlignerNodeModel.CFGKEY_ALIGNMENT_TYPE),
        		"Aligned Sequences are...", new String[] {"Protein Sequences", "Nucleotide (incl. IUPAC codes)"}
        ));
    }
}

