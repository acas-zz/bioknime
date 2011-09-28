package au.com.acpfg.spectra.phosphorylation;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.ColumnFilter;

import au.com.acpfg.misc.spectra.AbstractSpectraCell;
import au.com.acpfg.misc.spectra.SpectralDataInterface;

/**
 * <code>NodeDialog</code> for the "PhosphorylationScorer" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class PhosphorylationScorerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring PhosphorylationScorer node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PhosphorylationScorerNodeDialog() {
        super();
        
       addDialogComponent(new DialogComponentColumnNameSelection(
    		   					new SettingsModelString(PhosphorylationScorerNodeModel.CFGKEY_SPECTRA, "Spectra"),
    		   					"Spectra Column", 0, new ColumnFilter() {

									@Override
									public boolean includeColumn(
											DataColumnSpec colSpec) {
										return (colSpec.getType().isCompatible(SpectralDataInterface.class));
									}

									@Override
									public String allFilteredMsg() {
										return "There must be one column of Mascot-search spectra (see Mascot Reader node)!";
									}
    		   						
    		   					}));
       
       addDialogComponent(new DialogComponentColumnNameSelection(		
    		   					new SettingsModelString(PhosphorylationScorerNodeModel.CFGKEY_IONS, "List of matched ions"),
    		   					"Matched ions (B&Y series only) column", 0, new ColumnFilter() {

									@Override
									public boolean includeColumn(
											DataColumnSpec colSpec) {
										return (colSpec.getType().isCollectionType() && 
												colSpec.getType().getCollectionElementType().equals(StringCell.TYPE));
									}

									@Override
									public String allFilteredMsg() {
										return "Must provide a KNIME collection with list of B&Y matching ions (see Mascot Reader node)!";
									}
       							}));
       
       
       addDialogComponent(new DialogComponentColumnNameSelection(
    		   				new SettingsModelString(PhosphorylationScorerNodeModel.CFGKEY_MODPEP, "Modified Peptide"),
    		   				"Peptide Sequence (including modifications)", 0, new ColumnFilter() {

								@Override
								public boolean includeColumn(
										DataColumnSpec colSpec) {
									return (colSpec.getType().equals(StringCell.TYPE));
								}

								@Override
								public String allFilteredMsg() {
									return "No compatible String column available (see Mascot Reader node)!";
								}
    		   					
    		   				}));
     
       addDialogComponent(new DialogComponentStringListSelection(
    		   				new SettingsModelStringArray(PhosphorylationScorerNodeModel.CFGKEY_RESIDUES, new String[] {"S", "T", "Y"}),
    		   				"Residues to consider for phosphorylation", new String[] {"S", "T", "Y", "H", "K", "R" }
    		   ));
    }
}

