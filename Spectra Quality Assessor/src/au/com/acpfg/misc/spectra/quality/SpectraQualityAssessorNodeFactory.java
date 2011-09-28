package au.com.acpfg.misc.spectra.quality;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SpectraQualityAssessor" Node.
 * Implements the 'Xrea' algorithm in the paper entitled "Quality Assessment of Tandem Mass Spectra Based on Cumulative Intensity Normalization" in the journal of proteome research. May implement other algorithms at a future date.
 *
 * @author Andrew Cassin
 */
public class SpectraQualityAssessorNodeFactory 
        extends NodeFactory<SpectraQualityAssessorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SpectraQualityAssessorNodeModel createNodeModel() {
        return new SpectraQualityAssessorNodeModel();
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
    public NodeView<SpectraQualityAssessorNodeModel> createNodeView(final int viewIndex,
            final SpectraQualityAssessorNodeModel nodeModel) {
        return new SpectraQualityAssessorNodeView(nodeModel);
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
        return new SpectraQualityAssessorNodeDialog();
    }

}

