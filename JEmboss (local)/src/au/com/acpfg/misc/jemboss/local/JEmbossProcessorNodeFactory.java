package au.com.acpfg.misc.jemboss.local;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "JEmbossProcessor" Node.
 * Runs a EMBOSS command on the local computer, based on the configure-dialog settings. Input data is taken from the input table and automatically converted into a suitable form for EMBOSS based on the chosen program.
 *
 * @author Andrew Cassin
 */
public class JEmbossProcessorNodeFactory 
        extends NodeFactory<JEmbossProcessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public JEmbossProcessorNodeModel createNodeModel() {
        return new JEmbossProcessorNodeModel();
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
    public NodeView<JEmbossProcessorNodeModel> createNodeView(final int viewIndex,
            final JEmbossProcessorNodeModel nodeModel) {
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
    	JEmbossProcessorNodeDialog d = new JEmbossProcessorNodeDialog();
    	
    	return d;
    }

}

