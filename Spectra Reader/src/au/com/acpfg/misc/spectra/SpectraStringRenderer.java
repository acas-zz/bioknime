package au.com.acpfg.misc.spectra;

import java.awt.Component;
import java.awt.Dimension;
import java.util.logging.Logger;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/** 
 * Displays the peak list as a list of m/z intensity pairs as a text box
 * 
 * @author andrew.cassin
 *
 */
public class SpectraStringRenderer extends DefaultDataValueRenderer {

	@Override
	public boolean accepts(DataColumnSpec spec) {
		return (spec != null && spec.getType() == AbstractSpectraCell.TYPE);
	}

	@Override
	public String getDescription() {
		return "Peak List (text)";
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(100,40);
	}
	
	@Override
    protected void setValue(final Object value) {	
		if (value instanceof SpectralDataInterface) {
			SpectralDataInterface si = (SpectralDataInterface) value;
			//Logger.getAnonymousLogger().info("found peaks "+si.getNumPeaks());
			// Here, we use a method designed for speed and memory efficiency. The string value
			// of the spectra is commonly used by other nodes which filter the data, so using this
			// method ensures calculations involving spectra proceed at maximum speed
			super.setValue("<html><pre>"+si.asString());
		} else {
			super.setValue(value);
		}
	}
}
