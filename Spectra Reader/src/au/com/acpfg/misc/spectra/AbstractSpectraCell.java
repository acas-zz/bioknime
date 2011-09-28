package au.com.acpfg.misc.spectra;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;

public abstract class AbstractSpectraCell extends DataCell implements SpectralDataInterface {
	/**
	 * for serialisation
	 */
	private static final long serialVersionUID = 1034645173992398749L;
	
	/**
	 * Convenience method
	 */
    public static final DataType TYPE = DataType.getType(AbstractSpectraCell.class);

	protected boolean equalsDataCell(DataCell dc) {
		return (this == dc);
	}

	public abstract int hashCode();

	public String toString() { 
		// NB: default must be not to round, as this would cause serialized spectra on disk to be rounded
		return asString(false); 
	}

	public String asString() {
		return getID() + "\n"+"Peaks: "+getNumPeaks()+"\nMS Level: "+getMSLevel();
		
	}
	public abstract String getID();

	public abstract double[] getIntensity();

	public abstract int getMSLevel();

	public abstract double[] getMZ();

	public abstract AbstractSpectraCell getMyValue();

	public abstract int getNumPeaks();

	protected double scanMostIntense(boolean return_mz) {
		double[] mz = getMZ();
		double[] i  = getIntensity();
		
		double ci = Double.NEGATIVE_INFINITY;
		double ret= Double.NaN;
		
		for (int j=0; j<mz.length; j++) {
			if (i[j] > 0.0 && i[j] > ci) {
				ci = i[j];
				ret= return_mz ? mz[j] : ci;
			}
		}
		return ret;
	}
	
	public double getMZMostIntense() {
		return scanMostIntense(true);
	}
	
	public double getIntensityMostIntense() {
		return scanMostIntense(false);
	}
	
	protected double scanLeastIntense(boolean return_mz) {
		double[] mz = getMZ();
		double[] i  = getIntensity();
		
		double ci = Double.POSITIVE_INFINITY;
		double ret= Double.NaN;
		
		for (int j=0; j<mz.length; j++) {
			if (i[j] > 0.0 && i[j] < ci) {
				ci = i[j];
				ret= return_mz ? mz[j] : ci;
			}
		}
		return ret;
	}
	
	public double getMZLeastIntense() {
		return scanLeastIntense(true);
	}
	
	public double getIntensityLeastIntense() {
		return scanLeastIntense(false);
	}

	/**
	 * This method must be overridden in subclasses to ensure the possible charge states for the 
	 * precursor ion are listed. Although the format is free-form, it should be  similar to the
	 * Mascot Generic Format (MGF) as described on www.matrixscience.com for ease of processing.
	 * 
	 * @return the emtpy string is returned by this implementation as the baseclass does not know the possible charge states
	 */
	public String getCharge() {
		return "";
	}

	/**
	 * Template method (which may be overriden if required) to calculate Z based on the state of
	 * the spectra. It may not be 100% accurate, depending on available spectra state, but it 
	 * guarantees to returns a small integer >= 1
	 * 
	 * @return
	 */
	public int getProbableZ() {
		String charge = getCharge();
		if (charge.indexOf("2+") >= 0)
			return 2;
		else if (charge.indexOf("3+") >= 0) {
			return 3;
		}
		Pattern p = Pattern.compile("^\\s*(\\d+)\\+*\\s*$");
		Matcher m = p.matcher(charge);
		if (m.matches()) {
			Integer ch = new Integer(m.group(1));
			if (ch.intValue() >= 1 && ch.intValue() < 20) // believable charge state?
				return ch.intValue();
			// probably bogus charge state... so lie... and
			return 1;
		}
		// else
		return 1;
	}
}
