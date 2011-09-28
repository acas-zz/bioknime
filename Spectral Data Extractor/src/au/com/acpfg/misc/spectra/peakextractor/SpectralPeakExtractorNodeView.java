package au.com.acpfg.misc.spectra.peakextractor;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "SpectralPeakExtractor" Node.
 * Extracts data defining a peak from any cell supporting SpectralDataInterface (defined in the SpectraReader node)
 *
 * @author Andrew Cassin
 */
public class SpectralPeakExtractorNodeView extends NodeView<SpectralPeakExtractorNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link SpectralPeakExtractorNodeModel})
     */
    protected SpectralPeakExtractorNodeView(final SpectralPeakExtractorNodeModel nodeModel) {
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
        SpectralPeakExtractorNodeModel nodeModel = 
            (SpectralPeakExtractorNodeModel)getNodeModel();
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

