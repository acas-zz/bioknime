package au.com.acpfg.misc.spectra;

import javax.swing.Icon;

import org.knime.core.data.DataValue;

public interface SpectralDataInterface extends DataValue {
    
    /** Derived locally. */
    public static final UtilityFactory UTILITY = new SpectraUtilityFactory();
    
    /** The interface methods. */
    AbstractSpectraCell getMyValue();

    /* return a human-readable form of the spectra */
    String asString(boolean round_mz);
    String asString();		// this method returns a summary: required to be fast AND memory efficient
    
    /* return the ID (should be unique for all spectra in a given table) */
    String getID();
    
    /* return the list of m/z's with a peak (any size >0) */
    double[] getMZ();
    
    /* return the list of intensities - always same length as getMZ() */
    double[] getIntensity();
    
    /* how many peaks in the spectra? */
    int getNumPeaks();
    
    /* what level of scan eg. 2 == MS/MS */
    int getMSLevel();
    
    /* return the minimum mz cited in the spectra (constant time method) */
    double getMinMZ();
    
    /* return the maximum mz cited in the spectra (constant time method) */
    double getMaxMZ();
    
    /* return the most intense MZ (results are unpredictable if more than one MZ has maximum intensity) */
    double getMZMostIntense(); 
    double getIntensityMostIntense(); // returns the intensity of the most intense peak rather than M/Z
    
    /* return the least intense MZ (results are unpredictable if more than one MZ has minimum intensity) */
    double getMZLeastIntense();
    double getIntensityLeastIntense();
}
