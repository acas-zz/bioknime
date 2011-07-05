package au.com.acpfg.misc.StringMatcher;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "StringMatcher" Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
public class StringMatcherNodeView extends NodeView<StringMatcherNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link StringMatcherNodeModel})
     */
    protected StringMatcherNodeView(final StringMatcherNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        StringMatcherNodeModel nodeModel = 
            (StringMatcherNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

