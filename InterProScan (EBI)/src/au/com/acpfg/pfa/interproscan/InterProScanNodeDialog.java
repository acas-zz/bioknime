package au.com.acpfg.pfa.interproscan;

import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "InterProScan" Node.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class InterProScanNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring InterProScan node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected InterProScanNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentString(new SettingsModelString(InterProScanNodeModel.CFGKEY_EMAIL, ""), "Email address (required):"));
       
        // only string compatible columns are selectable for the protein sequence
        addDialogComponent(
        		new DialogComponentColumnNameSelection(new SettingsModelString(InterProScanNodeModel.CFGKEY_SEQ, ""),
        				"Protein Sequence column:", 0,  true, false, new ColumnFilter() {

							@Override
							public boolean includeColumn(DataColumnSpec colSpec) {
								if (colSpec.getType().isCompatible(StringValue.class))
									return true;
								return false;
							}

							@Override
							public String allFilteredMsg() {
								return "No String compatible column to use for Protein Sequence column!";
							}
        			
        		}));
        
         // algorithms to permit the user to select
         // HACK: must correspond to EBI IPRScan list:
         // blastprodom, fprintscan, hmmpir, hmmpfam, hmmsmart, 
         // hmmtigr, profilescan, hamap, patternscan, superfamily, signalp, tmhmm, hmmpanther, gene3d

         ArrayList<String> appl_list = new ArrayList<String>();
         appl_list.add("BlastProDom");
         appl_list.add("FPrintscan");
         appl_list.add("HMMPIR");
         appl_list.add("HMMPfam");
         appl_list.add("HMMSmart");
         appl_list.add("HMMTigr");
         appl_list.add("ProfileScan");
         appl_list.add("HAMAP");
         appl_list.add("patternScan");
         appl_list.add("SuperFamily");
         appl_list.add("SignalP");
         appl_list.add("TMHMM");
         appl_list.add("HMMPanther");
         appl_list.add("Gene3D");
         addDialogComponent(new DialogComponentStringListSelection(
        		 					new SettingsModelStringArray(InterProScanNodeModel.CFGKEY_USE_APPL, 
        		 					appl_list.toArray(new String[0])), "Algorithms to search via: ", appl_list, true, 5));
         // permit the use of CRC methods during scan? (If so, its faster)
         addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(InterProScanNodeModel.CFGKEY_USE_CRC, true), "Use CRC methods?"));
    
         createNewGroup("Graphical Results Summary");
         final SettingsModelString         sms = new SettingsModelString(InterProScanNodeModel.CFGKEY_IMGDIR, "c:/temp");
         final SettingsModelBoolean b_saveimgs = new SettingsModelBoolean(InterProScanNodeModel.CFGKEY_SAVEIMGS, false);
         b_saveimgs.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				sms.setEnabled(b_saveimgs.getBooleanValue());
			}
        	 
         });
         addDialogComponent(new DialogComponentBoolean(b_saveimgs, "Save graphical results to folder?"));
         addDialogComponent(new DialogComponentFileChooser(sms, "image-folder-history",
        		 JFileChooser.OPEN_DIALOG, true, ""));
         
    }
}

