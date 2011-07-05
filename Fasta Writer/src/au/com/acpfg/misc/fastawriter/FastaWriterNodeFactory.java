package au.com.acpfg.misc.fastawriter;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "FastaWriter" Node.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * @author Andrew Cassin
 */
public class FastaWriterNodeFactory 
        extends NodeFactory<FastaWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FastaWriterNodeModel createNodeModel() {
        return new FastaWriterNodeModel();
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
    public NodeView<FastaWriterNodeModel> createNodeView(final int viewIndex,
            final FastaWriterNodeModel nodeModel) {
        return new FastaWriterNodeView(nodeModel);
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
        return new FastaWriterNodeDialog();
    }

}

