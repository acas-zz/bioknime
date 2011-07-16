package au.com.acpfg.misc.muscle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.RowKey;
import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "MuscleAccessor" Node.
 * Provides multiple alignment data from MUSCLE as implemented by EBI
 *
 * @author Andrew Cassin
 */
public class MuscleAccessorNodeView extends NodeView<MuscleAccessorNodeModel> {
	private final HashMap<String,RowKey> m_map = new HashMap<String,RowKey>();
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MuscleAccessorNodeModel})
     */
    protected MuscleAccessorNodeView(final MuscleAccessorNodeModel nodeModel) {
        super(nodeModel);
        JPanel parent = new JPanel();
        setComponent(parent);
        parent.setLayout(new BorderLayout());
        
        final JTextArea     jt = new JTextArea();
        jt.setEditable(false);
        jt.setMinimumSize(new Dimension(400,300));
        jt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        
        Set<String> rowids = nodeModel.getMuscleMapIDs();
        final String[] muscles = rowids.toArray(new String[0]);
        Arrays.sort(muscles);
      
        JScrollPane   sp = new JScrollPane(jt);
        parent.add(sp, BorderLayout.CENTER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        jt.setText(muscles.length > 0 ? nodeModel.getFormattedAlignment(muscles[0]) : "");

        JTabbedPane tabs = new JTabbedPane();
        JPanel cluster_panel = new JPanel();
        final JList alignments = new JList(muscles);        
        alignments.setSelectedIndex(0);
        alignments.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				String selAlignment = alignments.getSelectedValue().toString();
				String new_text = nodeModel.getFormattedAlignment(selAlignment);
				if (new_text == null) {
					new_text = "";
				}
				jt.setText(new_text);
				jt.setCaretPosition(0);
				jt.scrollRectToVisible(new Rectangle(0,0,0,0));
			}
        	
        });
       
        cluster_panel.add(alignments);
        tabs.addTab("Alignment to show", cluster_panel);
        parent.add(tabs, BorderLayout.SOUTH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        MuscleAccessorNodeModel nodeModel = 
            (MuscleAccessorNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

