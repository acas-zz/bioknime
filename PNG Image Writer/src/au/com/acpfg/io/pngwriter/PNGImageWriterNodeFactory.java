package au.com.acpfg.io.pngwriter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PNGImageWriter" Node.
 * Writes PNGImageCell's to disk as separate files, based on user configuration. Ideal for saving graphical results from other nodes to files which can then be edited...
 *
 * @author http://www.plantcell.unimelb.edu.au
 */
public class PNGImageWriterNodeFactory 
        extends NodeFactory<PNGImageWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PNGImageWriterNodeModel createNodeModel() {
        return new PNGImageWriterNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PNGImageWriterNodeModel> createNodeView(final int viewIndex,
            final PNGImageWriterNodeModel nodeModel) {
        return new PNGImageWriterNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new PNGImageWriterNodeDialog();
    }

}

