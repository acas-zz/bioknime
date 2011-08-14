package au.com.acpfg.misc.blast.wublast;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "WUBlast" Node.
 * Performs a WU-Blast with the chosen parameters using the EBI webservices. Rate controlled so as not to overload EBI computer systems.
 *
 * @author Andrew Cassin
 */
public class WUBlastNodeView extends NodeView<WUBlastNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link WUBlastNodeModel})
     */
    protected WUBlastNodeView(final WUBlastNodeModel nodeModel) {
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
        WUBlastNodeModel nodeModel = 
            (WUBlastNodeModel)getNodeModel();
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

