package au.com.acpfg.align.phobius;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PhobiusSource" Node.
 * Takes a list of sequences and appends the results of Phobius webservice invocations (text only for now) to the output port
 *
 * @author Andrew Cassin
 */
public class PhobiusSourceNodeFactory 
        extends NodeFactory<PhobiusSourceNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PhobiusSourceNodeModel createNodeModel() {
        return new PhobiusSourceNodeModel();
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
    public NodeView<PhobiusSourceNodeModel> createNodeView(final int viewIndex,
            final PhobiusSourceNodeModel nodeModel) {
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
        return new PhobiusSourceNodeDialog();
    }

}

