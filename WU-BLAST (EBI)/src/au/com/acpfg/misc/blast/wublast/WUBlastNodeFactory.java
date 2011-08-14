package au.com.acpfg.misc.blast.wublast;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "WUBlast" Node.
 * Performs a WU-Blast with the chosen parameters using the EBI webservices. Rate controlled so as not to overload EBI computer systems.
 *
 * @author Andrew Cassin
 */
public class WUBlastNodeFactory 
        extends NodeFactory<WUBlastNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public WUBlastNodeModel createNodeModel() {
        return new WUBlastNodeModel();
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
    public NodeView<WUBlastNodeModel> createNodeView(final int viewIndex,
            final WUBlastNodeModel nodeModel) {
        return new WUBlastNodeView(nodeModel);
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
        return new WUBlastNodeDialog();
    }

}

