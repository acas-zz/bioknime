package au.com.acpfg.tpp;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "PepXMLReader" Node.
 * Reads PepXML (as produced by the trans-proteomics pipeline) to enable processing of peptide/protein identifications and statistics using KNIME
 *
 * @author Andrew Cassin
 */
public class PepXMLReaderNodeFactory 
        extends NodeFactory<PepXMLReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PepXMLReaderNodeModel createNodeModel() {
        return new PepXMLReaderNodeModel();
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
    public NodeView<PepXMLReaderNodeModel> createNodeView(final int viewIndex,
            final PepXMLReaderNodeModel nodeModel) {
        return new PepXMLReaderNodeView(nodeModel);
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
        return new PepXMLReaderNodeDialog();
    }

}

