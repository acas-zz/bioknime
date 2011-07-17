package au.com.acpfg.proteomics.mascotrdr;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MascotReader" Node.
 * Using the MascotDatFile open-source java library, this node provides an interface to that, to provide convenient access to MatrixScience Mascot datasets
 *
 * @author Andrew Cassin
 */
public class MascotReaderNodeFactory 
        extends NodeFactory<MascotReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MascotReaderNodeModel createNodeModel() {
        return new MascotReaderNodeModel();
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
    public NodeView<MascotReaderNodeModel> createNodeView(final int viewIndex,
            final MascotReaderNodeModel nodeModel) {
        return new MascotReaderNodeView(nodeModel);
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
        return new MascotReaderNodeDialog();
    }

}

