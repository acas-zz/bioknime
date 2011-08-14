package au.com.acpfg.misc.picr;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PICRAccessor" Node.
 * Provides access to the Protein Identifier Cross Reference (PICR) web service at EBI
 *
 * @author Andrew Cassin
 */
public class PICRAccessorNodeFactory 
        extends NodeFactory<PICRAccessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PICRAccessorNodeModel createNodeModel() {
        return new PICRAccessorNodeModel();
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
    public NodeView<PICRAccessorNodeModel> createNodeView(final int viewIndex,
            final PICRAccessorNodeModel nodeModel) {
        return new PICRAccessorNodeView(nodeModel);
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
        return new PICRAccessorNodeDialog();
    }

}

