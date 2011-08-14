package au.com.acpfg.misc.uniprot;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "UniProtAccessor" Node.
 * Accesses the UniProt data source (via webservices)
 *
 * @author Andrew Cassin
 */
public class UniProtAccessorNodeFactory 
        extends NodeFactory<UniProtAccessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public UniProtAccessorNodeModel createNodeModel() {
        return new UniProtAccessorNodeModel();
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
    public NodeView<UniProtAccessorNodeModel> createNodeView(final int viewIndex,
            final UniProtAccessorNodeModel nodeModel) {
        return new UniProtAccessorNodeView(nodeModel);
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
        return new UniProtAccessorNodeDialog();
    }

}

