package au.com.acpfg.xml.reader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XMLReader" Node.
 * Loads XML documents (either in a folder or files) into XML cells for further processing by the XQuery Processor node
 *
 * @author Andrew Cassin
 */
public class XMLReaderNodeFactory 
        extends NodeFactory<XMLReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XMLReaderNodeModel createNodeModel() {
        return new XMLReaderNodeModel();
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
    public NodeView<XMLReaderNodeModel> createNodeView(final int viewIndex,
            final XMLReaderNodeModel nodeModel) {
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
        return new XMLReaderNodeDialog();
    }

}

