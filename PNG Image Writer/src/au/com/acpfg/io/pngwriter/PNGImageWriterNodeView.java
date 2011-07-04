package au.com.acpfg.io.pngwriter;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "PNGImageWriter" Node.
 * Writes PNGImageCell's to disk as separate files, based on user configuration. Ideal for saving graphical results from other nodes to files which can then be edited...
 *
 * @author http://www.plantcell.unimelb.edu.au
 */
public class PNGImageWriterNodeView extends NodeView<PNGImageWriterNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link PNGImageWriterNodeModel})
     */
    protected PNGImageWriterNodeView(final PNGImageWriterNodeModel nodeModel) {
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
        PNGImageWriterNodeModel nodeModel = 
            (PNGImageWriterNodeModel)getNodeModel();
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

