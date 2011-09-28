package au.com.acpfg.spectra.phosphorylation;

import java.util.Comparator;

public class Peak implements Comparable {
	private static final double FRAGMENT_MZ_TOL = 0.6;			// symettric m/z error used during ion comparison
	private static final double H2O_LOSS = 18.01;
	private static final double AMMONIA_LOSS = 17.03;
	
	private double m_mz, m_intensity;
	
	public Peak(double mz, double intensity) {
		set_mz(mz);
		set_intensity(intensity);
	}

	public void set_intensity(double m_intensity) {
		this.m_intensity = m_intensity;
	}

	public double get_intensity() {
		return m_intensity;
	}

	public void set_mz(double m_mz) {
		this.m_mz = m_mz;
	}

	public double get_mz() {
		return m_mz;
	}

	@Override
	public String toString() {
		return m_mz + "=" + m_intensity;
	}

	@Override
	public int compareTo(Object arg0) {
		Peak p2 = (Peak) arg0;
		if (this.get_mz() < p2.get_mz()) {
			return -1;
		} else if (this.get_mz() > p2.get_mz()) {
			return 1;
		}
		return 0;
	}

	/**
	 * Returns true if the specified ion is within <code>FRAGMENT_MZ_TOL</code> of this peak, false otherwise.
	 * 
	 * @param i the ion to compare
	 * @return
	 */
	public boolean matches(Ion i) {
		return (Math.abs(i.get_mz() - m_mz) < FRAGMENT_MZ_TOL); 
	}
	
	/**
	 * Same as <code>matches(Ion)</code> except this attempts to match any Ion in the specified list
	 * @param i the ion list to search (each ion is searched in the iterator's order)
	 * @param filter ignores all ions which <code>filter.accept()</code> returns false. If <code>null</code> no filtering is done
	 * @return true if any ion matches <code>this</code> peak, false otherwise
	 */
	public boolean matches(IonList i, IonFilterInterface filter) {
		assert(i != null);
		
		for (Ion i2 : i) {
			// eg. ignore ions with either loss of ammonia or water?
			if (filter != null && !filter.accept(i2))
				continue;
			// ion match peak?
			if (matches(i2))
				return true;
		}
		// not found
		return false;
	}
	
	/**
	 * Skip over ions in i which have lost ammonia or water (B and Y ion series only)
	 * @param i ion list to search (must not be null)
	 * @return
	 */
	public boolean matches(IonList i) {
		return matches(i, new IonFilterInterface() {

			@Override
			public boolean accept(Ion i) {
				return ((i.is_B() || i.is_Y()) && 
						!(i.has_lost_h2o() || i.has_lost_nh3()));
			}
			
		});
	}

	/**
	 * 
	 * @param precursor_mass singly charged (ie. z=1) precursor mass eg. as calculated from spectra header eg. PEPMASS from mgf spectra
	 * @return
	 */
	public boolean isPrecursorNeutralLoss(double precursor_mass) {
		double mz_diff = precursor_mass - this.m_mz;
		if ((mz_diff - H2O_LOSS)     < FRAGMENT_MZ_TOL)
			return true;
		if ((mz_diff - AMMONIA_LOSS) < FRAGMENT_MZ_TOL)
			return true;
		// else
		return false;
	}
}
