package au.com.acpfg.proteomics.mascotrdr;

import javax.swing.JFileChooser;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "MascotReader" Node.
 * Using the MascotDatFile open-source java library, this node provides an interface to that, to provide convenient access to MatrixScience Mascot datasets
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class MascotReaderNodeDialog extends DefaultNodeSettingsPane {
	private static SettingsModelString f_resulttype = MascotReaderNodeModel.make_as_string(MascotReaderNodeModel.CFGKEY_RESULTTYPE);
	private static SettingsModelNumber f_ci = (SettingsModelNumber)MascotReaderNodeModel.make(MascotReaderNodeModel.CFGKEY_CONFIDENCE);
	
	public static void set_controls() {
		f_ci.setEnabled(f_resulttype.getStringValue().trim().toLowerCase().startsWith("confident"));
	}
	
    /**
     * New pane for configuring MascotReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected MascotReaderNodeDialog() {
        super();
        
        set_controls();
        addDialogComponent(new DialogComponentFileChooser(MascotReaderNodeModel.make_as_string(MascotReaderNodeModel.CFGKEY_FOLDER),
        		"folder-history",
        		JFileChooser.OPEN_DIALOG,
        		true, // directories only
        		".dat" ) );
        
        DialogComponentButtonGroup bg = new DialogComponentButtonGroup(f_resulttype, true, "Result Selection per query", new String[] { "all hits", "best hit only", "confident hits only (identity threshold)"});
        bg.setToolTipText("Which peptide identifications per spectra do you want to see?");
        addDialogComponent(bg);
        f_resulttype.addChangeListener(new ChangeListener() {
        	public void stateChanged(ChangeEvent ce) {
        		set_controls();
        	}
        });
        
        addDialogComponent(new DialogComponentNumberEdit(f_ci,"Identity Threshold Confidence", 5));
    }
}

