package au.com.acpfg.phylogeny;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "PhylogenyProcessor" Node.
 * Using the PAL library, as exported from MUSCLE node, this tree takes input data and performs tree construction, bootstrapping and other phylogenetic analyses as configured by the user.
 *
 * @author Andrew Cassin
 */
public class PhylogenyProcessorNodeView extends NodeView<PhylogenyProcessorNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link PhylogenyProcessorNodeModel})
     */
    protected PhylogenyProcessorNodeView(final PhylogenyProcessorNodeModel nodeModel) {
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
        PhylogenyProcessorNodeModel nodeModel = 
            (PhylogenyProcessorNodeModel)getNodeModel();
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

