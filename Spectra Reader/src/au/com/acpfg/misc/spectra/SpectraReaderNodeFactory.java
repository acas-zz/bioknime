package au.com.acpfg.misc.spectra;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MzXMLReader" Node.
 * Using the jrap-stax library, this node reads mzXML/mzML
 *
 * @author Andrew Cassin
 */
public class SpectraReaderNodeFactory 
        extends NodeFactory<SpectraReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectraReaderNodeModel createNodeModel() {
        return new SpectraReaderNodeModel();
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
    public NodeView<SpectraReaderNodeModel> createNodeView(final int viewIndex,
            final SpectraReaderNodeModel nodeModel) {
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
        return new SpectraReaderNodeDialog();
    }

}

