package au.com.acpfg.align.muscle;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MuscleAccessor" Node.
 * Provides multiple alignment data from MUSCLE as implemented by EBI
 *
 * @author Andrew Cassin
 */
public class MuscleAlignerNodeFactory 
        extends NodeFactory<MuscleAlignerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MuscleAlignerNodeModel createNodeModel() {
        return new MuscleAlignerNodeModel();
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
    public NodeView<MuscleAlignerNodeModel> createNodeView(final int viewIndex,
            final MuscleAlignerNodeModel nodeModel) {
        return new MuscleAlignerNodeView(nodeModel);
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
        return new MuscleAlignerNodeDialog();
    }

}

