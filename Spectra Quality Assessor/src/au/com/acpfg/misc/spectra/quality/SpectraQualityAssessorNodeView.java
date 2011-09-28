package au.com.acpfg.misc.spectra.quality;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "SpectraQualityAssessor" Node.
 * Implements the 'Xrea' algorithm in the paper entitled "Quality Assessment of Tandem Mass Spectra Based on Cumulative Intensity Normalization" in the journal of proteome research. May implement other algorithms at a future date.
 *
 * @author Andrew Cassin
 */
public class SpectraQualityAssessorNodeView extends NodeView<SpectraQualityAssessorNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link SpectraQualityAssessorNodeModel})
     */
    protected SpectraQualityAssessorNodeView(final SpectraQualityAssessorNodeModel nodeModel) {
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
        SpectraQualityAssessorNodeModel nodeModel = 
            (SpectraQualityAssessorNodeModel)getNodeModel();
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

