package au.com.acpfg.spectra.phosphorylation;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PhosphorylationScorer" Node.
 * 
 *
 * @author Andrew Cassin
 */
public class PhosphorylationScorerNodeFactory 
        extends NodeFactory<PhosphorylationScorerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PhosphorylationScorerNodeModel createNodeModel() {
        return new PhosphorylationScorerNodeModel();
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
    public NodeView<PhosphorylationScorerNodeModel> createNodeView(final int viewIndex,
            final PhosphorylationScorerNodeModel nodeModel) {
        return new PhosphorylationScorerNodeView(nodeModel);
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
        return new PhosphorylationScorerNodeDialog();
    }

}

