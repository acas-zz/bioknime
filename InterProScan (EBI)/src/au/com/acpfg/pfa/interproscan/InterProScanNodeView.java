package au.com.acpfg.pfa.interproscan;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "InterProScan" Node.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * @author Andrew Cassin
 */
public class InterProScanNodeView extends NodeView<InterProScanNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link InterProScanNodeModel})
     */
    protected InterProScanNodeView(final InterProScanNodeModel nodeModel) {
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
        InterProScanNodeModel nodeModel = 
            (InterProScanNodeModel)getNodeModel();
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

