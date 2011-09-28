package au.com.acpfg.misc.spectra.quality;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.com.acpfg.misc.spectra.AbstractSpectraCell;
import au.com.acpfg.misc.spectra.SpectralDataInterface;

/**
 * <code>NodeDialog</code> for the "SpectraQualityAssessor" Node.
 * Implements the 'Xrea' algorithm in the paper entitled "Quality Assessment of Tandem Mass Spectra Based on Cumulative Intensity Normalization" in the journal of proteome research. May implement other algorithms at a future date.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SpectraQualityAssessorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring SpectraQualityAssessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected SpectraQualityAssessorNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(SpectraQualityAssessorNodeModel.CFGKEY_SPECTRA, ""), "Spectra Column", 0, true, false, SpectralDataInterface.class));
        createNewGroup("Dominant Peak Score Adjustment");
        addDialogComponent(new DialogComponentNumber(new SettingsModelDoubleBounded(SpectraQualityAssessorNodeModel.CFGKEY_ADJUSTMENT_THRESHOLD, 0.85, 0.0, 1.0), "TIC Threshold", 0.1));
        addDialogComponent(new DialogComponentNumber(new SettingsModelIntegerBounded(SpectraQualityAssessorNodeModel.CFGKEY_ADJUSTMENT_PEAKS, 10, 0, 100), "Maximum number of peaks (including isotopic peaks)", 2));
        closeCurrentGroup();
    }
}

