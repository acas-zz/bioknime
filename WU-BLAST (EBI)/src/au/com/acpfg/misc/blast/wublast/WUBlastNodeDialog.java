package au.com.acpfg.misc.blast.wublast;

import java.util.ArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "WUBlast" Node.
 * Performs a WU-Blast with the chosen parameters using the EBI webservices. Rate controlled so as not to overload EBI computer systems.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class WUBlastNodeDialog extends DefaultNodeSettingsPane {
	final String[] full_db_list = WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_DB);

    /**
     * New pane for configuring WUBlast node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected WUBlastNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_SEQUENCE_COL), 
        		"Blast sequence data from column:", 0, true, false, StringValue.class));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_STYPE), "Sequence Type", new String[] {"protein", "dna"}));
        addDialogComponent(new DialogComponentString(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_EMAIL), "Email address (required by EBI):"));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_PROGRAMS), "BLAST Program",
                WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_PROGRAMS)));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_MATRIX), "Scoring Matrix",
                WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_MATRIX)));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_EVAL_THRESHOLD), "Maximum Expectation Value", 
				new String[] {"1e-200", "1e-100", "1e-50", "1e-10", "1e-5", "1e-4", "1e-3", "1e-2", "1e-1", "1.0", "10", "100", "1000"}));

        this.createNewGroup("Blast Databases (filter by entering text into the box)");
        final SettingsModelString db_filter = WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_FILTERSTR);
        final DialogComponentString dcs = new DialogComponentString(db_filter, "Database Filter:");
        addDialogComponent(dcs);
        final DialogComponentStringSelection dcss = new DialogComponentStringSelection(
        		WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_DB), "Databases", full_db_list);
        addDialogComponent(dcss);
        db_filter.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				String filter = db_filter.getStringValue().toLowerCase().trim();
				ArrayList<String> matches = new ArrayList<String>();

				if (filter.length() >= 3) {		// minimum length requirement before attempting a filter
					for (String s : full_db_list) {
						if (s.toLowerCase().indexOf(filter) >= 0) {
							matches.add(s);
						}
					}
					if (matches.size() > 0) {
						dcss.replaceListItems(matches, "");
					}
				} else if (filter.length() == 0) {
					for (String s : full_db_list) {
						matches.add(s);
					}
					dcss.replaceListItems(matches, "");
				}
			}
        	
        });
      
        createNewTab("Advanced");
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_FILTER), "Filters",
                WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_FILTER)));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_SENSITIVITY), "Sensitivity",
                WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_SENSITIVITY)));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_SORT), "Sort Criteria",
                WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_SORT)));
        addDialogComponent(new DialogComponentStringSelection(WUBlastNodeModel.make_as_string(WUBlastNodeModel.CFGKEY_STATS), "Statistics",
        		WUBlastNodeModel.get_ebi(WUBlastNodeModel.CFGKEY_STATS)));
        addDialogComponent(new DialogComponentNumber((SettingsModelNumber)WUBlastNodeModel.make(WUBlastNodeModel.CFGKEY_NUM_ALIGNMENTS), "Number of reported alignments (per BLAST)", 10, 4));
        addDialogComponent(new DialogComponentNumber((SettingsModelNumber)WUBlastNodeModel.make(WUBlastNodeModel.CFGKEY_NUM_SCORES), "Number of reported scores (per BLAST)", 10, 4));
        addDialogComponent(new DialogComponentNumber((SettingsModelNumber)WUBlastNodeModel.make(WUBlastNodeModel.CFGKEY_EBI_BATCH_SIZE), "EBI Batch Limit", 10));
        addDialogComponent(new DialogComponentBoolean((SettingsModelBoolean) WUBlastNodeModel.make(WUBlastNodeModel.CFGKEY_SAVE_IMAGE), "Save image summary?"));
    }
}

