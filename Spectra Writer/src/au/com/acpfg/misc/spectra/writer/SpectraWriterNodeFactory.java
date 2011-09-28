package au.com.acpfg.misc.spectra.writer;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SpectraWriter" Node.
 * Writes a spectra column out to disk for processing with other Mass Spec. software. Supports MGF format but does not guarantee that all input data will be preserved in the created file.
 *
 * @author Andrew Cassin
 */
public class SpectraWriterNodeFactory 
        extends NodeFactory<SpectraWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectraWriterNodeModel createNodeModel() {
        return new SpectraWriterNodeModel();
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
    public NodeView<SpectraWriterNodeModel> createNodeView(final int viewIndex,
            final SpectraWriterNodeModel nodeModel) {
        return new SpectraWriterNodeView(nodeModel);
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
        return new SpectraWriterNodeDialog();
    }

}

