package au.com.acpfg.misc.StringMatcher;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "StringMatcher" Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
public class StringMatcherNodeFactory 
        extends NodeFactory<StringMatcherNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public StringMatcherNodeModel createNodeModel() {
        return new StringMatcherNodeModel();
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
    public NodeView<StringMatcherNodeModel> createNodeView(final int viewIndex,
            final StringMatcherNodeModel nodeModel) {
        return new StringMatcherNodeView(nodeModel);
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
        return new StringMatcherNodeDialog();
    }

}

