package au.com.acpfg.misc.spectra;

import org.knime.core.data.renderer.BitVectorValuePixelRenderer;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;

public class SpectraBitVectorRenderer extends BitVectorValuePixelRenderer
		implements DataValueRenderer {

	/**
	 *  distinguishes renderer data when serialized from all others
	 */
	private static final long serialVersionUID = 1143519816230556260L;
	
	private double m_threshold;
	private double m_window_left;		
	private double m_window_right;		
	private String m_title;
	private double m_bin_size = 0.1;
	
	/**
	 * Explicit constructor for how the bit vector should be constructed. 
	 * @param title Human-readable string to give to the renderer in the table view for the spectra column(s)
	 * @param left m/z where the bit vector is to start
	 * @param right m/z where the bit vector is to stop
	 * @param threshold peaks below this value will be ignored and not displayed in the map >= 0.0
	 */
	public SpectraBitVectorRenderer(String title, double left, double right, double threshold, double bin_size) {
		m_title = title;
		assert(left < right && left >= 0.0 && right >= 0.0 && threshold >= 0.0);
		m_window_left = left;
		m_window_right = right;
		m_threshold = threshold;
		m_bin_size = bin_size;
	}
	
	/**
	 * Displays the iTRAQ 8-plex region of the spectra only
	 */
	public SpectraBitVectorRenderer() {
		// 113.0 == 0.1u to the left of the small iTRAQ peak (8-plex)
		// 121.2 == 0.1u to the right of the largest iTRAQ peak (8-plex)
		this("Spectra M/Z map (iTRAQ 8-plex region, bins 0.1u)", 113.0, 121.2, 0.0, 0.1);
	}
	
	
	
	/** {@inheritDoc} */
    @Override
    protected void setValue(final Object val) {
    	if (val instanceof BitVectorValue) {
    		super.setValue(val);
    	} else if (val instanceof AbstractSpectraCell) {
    		// compute the bit vector for the super class to render
    		AbstractSpectraCell spectra = (AbstractSpectraCell) val;
    		double[] mz = spectra.getMZ();
    		
    		double max_mz = m_window_right - m_window_left;
    		int n_bits = (int)(max_mz / m_bin_size)+1;
    		DenseBitVectorCellFactory mybits = new DenseBitVectorCellFactory(n_bits);
    		double[] intensity = spectra.getIntensity();
    		
    		for (int i=0; i<mz.length; i++) {
    			if (intensity[i] > m_threshold && mz[i] >= m_window_left && mz[i] <= m_window_right) {
    				mybits.set(n_bits - 1 - (int) ((mz[i] - m_window_left) / m_bin_size));
    			}
    		}
    		super.setValue(mybits.createDataCell());
    	}
    }
	
	/** {@inheritDoc} */
    @Override
	public String getDescription() {
		return m_title;
	}
}
