package au.com.acpfg.misc.biojava;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "BioJava" Node.
 * Provides ready-made access to sequence conversion and other basic bioinformatics tasks
 *
 * @author Andrew Cassin
 */
public class BioJavaProcessorNodeFactory 
        extends NodeFactory<BioJavaProcessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public BioJavaProcessorNodeModel createNodeModel() {
        return new BioJavaProcessorNodeModel();
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
    public NodeView<BioJavaProcessorNodeModel> createNodeView(final int viewIndex,
            final BioJavaProcessorNodeModel nodeModel) {
        return new BioJavaProcessorNodeView(nodeModel);
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
        return new BioJavaProcessorNodeDialog();
    }

}

