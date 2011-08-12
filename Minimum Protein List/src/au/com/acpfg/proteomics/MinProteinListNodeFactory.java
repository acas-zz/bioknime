package au.com.acpfg.proteomics;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MinProteinList" Node.
 * Uses a greedy set cover algorithm to identify the minimal set of proteins which can explain the observed peptides
 *
 * @author Andrew Cassin
 */
public class MinProteinListNodeFactory 
        extends NodeFactory<MinProteinListNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MinProteinListNodeModel createNodeModel() {
        return new MinProteinListNodeModel();
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
    public NodeView<MinProteinListNodeModel> createNodeView(final int viewIndex,
            final MinProteinListNodeModel nodeModel) {
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
        return new MinProteinListNodeDialog();
    }

}

