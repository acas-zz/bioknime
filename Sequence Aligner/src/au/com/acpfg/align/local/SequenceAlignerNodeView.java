package au.com.acpfg.align.local;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "SequenceAligner" Node.
 * Performs an alignment, performed by http://jaligner.sourceforge.net of two sequences using the chosen parameters
 *
 * @author Andrew Cassin
 */
public class SequenceAlignerNodeView extends NodeView<SequenceAlignerNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link SequenceAlignerNodeModel})
     */
    protected SequenceAlignerNodeView(final SequenceAlignerNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        SequenceAlignerNodeModel nodeModel = 
            (SequenceAlignerNodeModel)getNodeModel();
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

