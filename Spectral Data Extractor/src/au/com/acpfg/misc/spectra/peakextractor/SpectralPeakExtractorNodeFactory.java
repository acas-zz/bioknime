package au.com.acpfg.misc.spectra.peakextractor;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SpectralPeakExtractor" Node.
 * Extracts data defining a peak from any cell supporting SpectralDataInterface (defined in the SpectraReader node)
 *
 * @author Andrew Cassin
 */
public class SpectralPeakExtractorNodeFactory 
        extends NodeFactory<SpectralPeakExtractorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectralPeakExtractorNodeModel createNodeModel() {
        return new SpectralPeakExtractorNodeModel();
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
    public NodeView<SpectralPeakExtractorNodeModel> createNodeView(final int viewIndex,
            final SpectralPeakExtractorNodeModel nodeModel) {
        return new SpectralPeakExtractorNodeView(nodeModel);
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
        return new SpectralPeakExtractorNodeDialog();
    }

}

