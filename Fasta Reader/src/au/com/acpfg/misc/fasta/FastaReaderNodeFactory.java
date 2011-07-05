package au.com.acpfg.misc.fasta;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "FastaReader" Node.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: * n1) Accession * n2) Description - often not accurate in practice * n3) Sequence data * n * nNo line breaks are preserved.
 *
 * @author Andrew Cassin
 */
public class FastaReaderNodeFactory 
        extends NodeFactory<FastaReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FastaReaderNodeModel createNodeModel() {
        return new FastaReaderNodeModel();
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
    public NodeView<FastaReaderNodeModel> createNodeView(final int viewIndex,
            final FastaReaderNodeModel nodeModel) {
        return new FastaReaderNodeView(nodeModel);
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
        return new FastaReaderNodeDialog();
    }

}

