package au.com.acpfg.xml.query;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XMLreader" Node.
 * Provides an XPath knime api & XML "blob" cell type and data processing. Useful for many life science XML formats (PepXML, ProtXML, etc. etc.)
 *
 * @author Andrew Cassin
 */
public class XQueryProcessorNodeFactory 
        extends NodeFactory<XQueryProcessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XQueryProcessorNodeModel createNodeModel() {
        return new XQueryProcessorNodeModel();
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
    public NodeView<XQueryProcessorNodeModel> createNodeView(final int viewIndex,
            final XQueryProcessorNodeModel nodeModel) {
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
        return new XQueryProcessorNodeDialog();
    }

}

