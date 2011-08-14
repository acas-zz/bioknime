package au.com.acpfg.misc.picr;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * <code>NodeDialog</code> for the "PICRAccessor" Node.
 * Provides access to the Protein Identifier Cross Reference (PICR) web service at EBI
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class PICRAccessorNodeDialog extends DefaultNodeSettingsPane {
	private static List<String> databases;
	
    /**
     * New pane for configuring PICRAccessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PICRAccessorNodeDialog() {
        super();
        
        if (databases == null || databases.size() == 0) {
        	databases = PICRAccessorNodeModel.load_databases();
        }
        
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(PICRAccessorNodeModel.CFGKEY_ACCSNS, "Accessions"), 
        								"Accessions", 0, true, false, StringValue.class));
        addDialogComponent(new DialogComponentStringListSelection(new SettingsModelStringArray(PICRAccessorNodeModel.CFGKEY_DB, new String[] {"SwissProt"}), "Databases to map to:", databases.toArray(new String[0])));
        List<String> taxon_list = new ArrayList<String>();
        taxon_list.add("Any species");
        taxon_list.add("3702 Arabidopsis thaliana");
        taxon_list.add("9313 Bos Taurus");
        taxon_list.add("6239 Caenorhabditis elegans");
        taxon_list.add("3055 Chlamydomonas reinhardtii");
        taxon_list.add("7955 Danio rerio");
        taxon_list.add("44689 Dictyostelium discoideum");
        taxon_list.add("7227 Drosophila melanogaster");
        taxon_list.add("562 Escherichia coli");
        taxon_list.add("11103 Hepatitis C virus");
        taxon_list.add("9606 Homo Sapiens");
        taxon_list.add("148305 Magnaporthe grisea");
        taxon_list.add("10090 Mus musculus");
        taxon_list.add("2104 Mycoplasma pneumoniae");
        taxon_list.add("5141 Neurospora crassa");
        taxon_list.add("4530 Oryza sativa");
        taxon_list.add("5833 Plasmodium falciparum");
        taxon_list.add("4754 Pneumocystis carinii");
        taxon_list.add("10116 Rattus norvegicus");
        taxon_list.add("4932 Saccharomyces cerevisiae");
        taxon_list.add("4896 Schizosaccharomyces pombe");
        taxon_list.add("31033 Takifugu rubripes");
        taxon_list.add("8355 Xenopus laevis");
        taxon_list.add("4577 Zea mays");
        addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(PICRAccessorNodeModel.CFGKEY_TAXON, "9606 Homo Sapiens"), "NCBI Taxonomy ID", taxon_list, true));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(PICRAccessorNodeModel.CFGKEY_ACTIVE_ONLY, true), "Active only?"));
    }
}

