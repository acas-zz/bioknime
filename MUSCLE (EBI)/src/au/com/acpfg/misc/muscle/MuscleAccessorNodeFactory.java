package au.com.acpfg.misc.muscle;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MuscleAccessor" Node.
 * Provides multiple alignment data from MUSCLE as implemented by EBI
 *
 * @author Andrew Cassin
 */
public class MuscleAccessorNodeFactory 
        extends NodeFactory<MuscleAccessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MuscleAccessorNodeModel createNodeModel() {
        return new MuscleAccessorNodeModel();
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
    public NodeView<MuscleAccessorNodeModel> createNodeView(final int viewIndex,
            final MuscleAccessorNodeModel nodeModel) {
        return new MuscleAccessorNodeView(nodeModel);
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
        return new MuscleAccessorNodeDialog();
    }

}

