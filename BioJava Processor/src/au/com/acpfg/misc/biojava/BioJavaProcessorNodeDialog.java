package au.com.acpfg.misc.biojava;

import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.data.StringValue;
import java.util.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <code>NodeDialog</code> for the "BioJavaProcessor" Node.
 * Analyses the specified data using BioJava (see http://www.biojava.org) and produces the result at output
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class BioJavaProcessorNodeDialog extends DefaultNodeSettingsPane {
	private final static String POSITION_TASK = "Residue Frequency by Position";
	
    /**
     * New pane for configuring BioJavaProcessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected BioJavaProcessorNodeDialog() {
        super();
        Collection<String> c = new ArrayList<String>();
        for (String s : BioJavaProcessorNodeModel.getTasks()) {
        	c.add(s);
        }
        
        SettingsModelString sm = BioJavaProcessorNodeModel.make_as_string(BioJavaProcessorNodeModel.CFGKEY_TASK);
        final SettingsModelIntegerBounded sm_maxlen = (SettingsModelIntegerBounded)BioJavaProcessorNodeModel.make(BioJavaProcessorNodeModel.CFGKEY_MAXLEN);
        boolean  is_position_task = sm.getStringValue().equals(POSITION_TASK);
        sm_maxlen.setEnabled(is_position_task);
        
        sm.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				SettingsModelString sm = ((SettingsModelString) arg0.getSource());
				String task = sm.getStringValue();
				sm_maxlen.setEnabled(task.equals(POSITION_TASK));
			}
        });
        DialogComponent dc = new DialogComponentStringSelection(sm, "Task:", c);
        
        addDialogComponent(dc);
        addDialogComponent(new DialogComponentColumnNameSelection(BioJavaProcessorNodeModel.make_as_string(BioJavaProcessorNodeModel.CFGKEY_SEQUENCE_COL), "Sequence:", 0, StringValue.class));
        addDialogComponent(new DialogComponentStringSelection(BioJavaProcessorNodeModel.make_as_string(BioJavaProcessorNodeModel.CFGKEY_SEQTYPE), "Sequence Type:", "DNA", "RNA", "Protein"));
        addDialogComponent(new DialogComponentNumber(sm_maxlen, "Maximum Sequence Length:", 75));
    }
}


