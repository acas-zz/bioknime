package au.com.acpfg.xml.writer;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XMLWriter" Node.
 * Saves XMLcell's to disk as separate XML documents. 
 *
 * @author Andrew Cassin
 */
public class XMLWriterNodeFactory 
        extends NodeFactory<XMLWriterNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XMLWriterNodeModel createNodeModel() {
        return new XMLWriterNodeModel();
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
    public NodeView<XMLWriterNodeModel> createNodeView(final int viewIndex,
            final XMLWriterNodeModel nodeModel) {
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
        return new XMLWriterNodeDialog();
    }

}

