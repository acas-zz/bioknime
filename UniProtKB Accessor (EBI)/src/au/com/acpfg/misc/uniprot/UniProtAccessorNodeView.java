package au.com.acpfg.misc.uniprot;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "UniProtAccessor" Node.
 * Accesses the UniProt data source (via webservices)
 *
 * @author Andrew Cassin
 */
public class UniProtAccessorNodeView extends NodeView<UniProtAccessorNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link UniProtAccessorNodeModel})
     */
    protected UniProtAccessorNodeView(final UniProtAccessorNodeModel nodeModel) {
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
        UniProtAccessorNodeModel nodeModel = 
            (UniProtAccessorNodeModel)getNodeModel();
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

