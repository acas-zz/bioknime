package au.com.acpfg.phylogeny;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PhylogenyProcessor" Node.
 * Using the PAL library, as exported from MUSCLE node, this tree takes input data and performs tree construction, bootstrapping and other phylogenetic analyses as configured by the user.
 *
 * @author Andrew Cassin
 */
public class PhylogenyProcessorNodeFactory 
        extends NodeFactory<PhylogenyProcessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PhylogenyProcessorNodeModel createNodeModel() {
        return new PhylogenyProcessorNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<PhylogenyProcessorNodeModel> createNodeView(final int viewIndex,
            final PhylogenyProcessorNodeModel nodeModel) {
        return new PhylogenyProcessorNodeView(nodeModel);
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
        return new PhylogenyProcessorNodeDialog();
    }

}

