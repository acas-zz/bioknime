package au.com.acpfg.proteomics.mascotrdr;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "MascotReader" Node.
 * Using the MascotDatFile open-source java library, this node provides an interface to that, to provide convenient access to MatrixScience Mascot datasets
 *
 * @author Andrew Cassin
 */
public class MascotReaderNodeView extends NodeView<MascotReaderNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MascotReaderNodeModel})
     */
    protected MascotReaderNodeView(final MascotReaderNodeModel nodeModel) {
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
        MascotReaderNodeModel nodeModel = 
            (MascotReaderNodeModel)getNodeModel();
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

