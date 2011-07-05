package au.com.acpfg.misc.fastawriter;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "FastaWriter" Node.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class FastaWriterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring FastaWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected FastaWriterNodeDialog() {
        super();
        
        SettingsModelString filename = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_FILE);
        SettingsModelString accsn    = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_ACCSN);
        SettingsModelString descr    = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_DESCR);
        SettingsModelString seq      = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_SEQ);
        SettingsModelBoolean overwrite= (SettingsModelBoolean) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_OVERWRITE);
        SettingsModelIntegerBounded maxll = (SettingsModelIntegerBounded) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_MAXLINELEN);
        
        addDialogComponent(new DialogComponentFileChooser(filename, "file-history", ".fasta|.fa|.fas"));
        addDialogComponent(new DialogComponentBoolean(overwrite, "Overwrite OK?"));
        addDialogComponent(new DialogComponentColumnNameSelection(accsn, "Accessions from:", 0, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(descr, "Annotation (description) from:", 0,  StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(seq, "Sequence (AA or DNA) from:", 0, StringValue.class));
        addDialogComponent(new DialogComponentNumber(maxll, "Maximum Sequence Line Length (characters)", 10));
    }
}

