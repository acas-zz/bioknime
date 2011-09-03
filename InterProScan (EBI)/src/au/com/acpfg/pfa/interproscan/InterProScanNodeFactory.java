package au.com.acpfg.pfa.interproscan;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "InterProScan" Node.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * @author Andrew Cassin
 */
public class InterProScanNodeFactory 
        extends NodeFactory<InterProScanNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public InterProScanNodeModel createNodeModel() {
        return new InterProScanNodeModel();
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
    public NodeView<InterProScanNodeModel> createNodeView(final int viewIndex,
            final InterProScanNodeModel nodeModel) {
        return new InterProScanNodeView(nodeModel);
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
        return new InterProScanNodeDialog();
    }

}

