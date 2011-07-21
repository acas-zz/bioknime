package au.com.acpfg.align.local;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SequenceAligner" Node.
 * Performs an alignment, performed by http://jaligner.sourceforge.net of two sequences using the chosen parameters
 *
 * @author Andrew Cassin
 */
public class SequenceAlignerNodeFactory 
        extends NodeFactory<SequenceAlignerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SequenceAlignerNodeModel createNodeModel() {
        return new SequenceAlignerNodeModel();
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
    public NodeView<SequenceAlignerNodeModel> createNodeView(final int viewIndex,
            final SequenceAlignerNodeModel nodeModel) {
        return new SequenceAlignerNodeView(nodeModel);
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
        return new SequenceAlignerNodeDialog();
    }

}

