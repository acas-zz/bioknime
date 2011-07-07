package au.com.acpfg.tpp;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "PepXMLReader" Node.
 * Reads PepXML (as produced by the trans-proteomics pipeline) to enable processing of peptide/protein identifications and statistics using KNIME
 *
 * @author Andrew Cassin
 */
public class PepXMLReaderNodeView extends NodeView<PepXMLReaderNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link PepXMLReaderNodeModel})
     */
    protected PepXMLReaderNodeView(final PepXMLReaderNodeModel nodeModel) {
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
        PepXMLReaderNodeModel nodeModel = 
            (PepXMLReaderNodeModel)getNodeModel();
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

