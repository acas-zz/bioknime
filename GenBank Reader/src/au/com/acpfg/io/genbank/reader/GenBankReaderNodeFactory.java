package au.com.acpfg.io.genbank.reader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "GenBankReader" Node.
 * Using BioJava, this node reads the specified files/folder for compressed genbank or .gb files and loads the sequences into a single table along with most of key metadata
 *
 * @author http://www.plantcell.unimelb.edu.au
 */
public class GenBankReaderNodeFactory 
        extends NodeFactory<FastGenbankNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FastGenbankNodeModel createNodeModel() {
        return new FastGenbankNodeModel();
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
    public NodeView<FastGenbankNodeModel> createNodeView(final int viewIndex,
            final FastGenbankNodeModel nodeModel) {
        return null;
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
        return new GenBankReaderNodeDialog();
    }

}

