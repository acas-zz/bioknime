package au.com.acpfg.spectra.phosphorylation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Logger;

import org.expasy.jpl.core.mol.polymer.pept.JPLPeptide;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.JPLFragmentationType;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.JPLPeptideFragmenter;
import org.expasy.jpl.core.ms.spectrum.JPLIMSPeakList;

import au.com.acpfg.misc.spectra.AbstractSpectraCell;

/**
 * Holds the peptide scores for a single peptide over the entire range of peak depths (1..10 by the paper)
 * and permits the caller to easily identify the best two candidates for the AScore calculation and the 
 * peak depth at which this occurs.
 * 
 * @author andrew.cassin
 *
 */
public class PeptideScore {
	/**
	 * list of AA masses, sourced from http://www.matrixscience.com/help/aa_help.html
	 */
	private final static HashMap<String,Double> aa_masses = new HashMap<String,Double>();
	static {
		aa_masses.put(new String("A"), new Double(71.037114));
		aa_masses.put(new String("C"), new Double(103.009185));
		aa_masses.put(new String("D"), new Double(115.026943));
		aa_masses.put(new String("E"), new Double(129.042593));
		
		aa_masses.put(new String("F"), new Double(147.068414));
		aa_masses.put(new String("G"), new Double(57.021464));
		aa_masses.put(new String("H"), new Double(137.058912));
		aa_masses.put(new String("I"), new Double(113.084064));
		
		aa_masses.put(new String("K"), new Double(128.094963));
		aa_masses.put(new String("L"), new Double(113.084064));
		aa_masses.put(new String("M"), new Double(131.040485));
		aa_masses.put(new String("N"), new Double(114.042927));
		
		aa_masses.put(new String("P"), new Double(97.052764));
		aa_masses.put(new String("Q"), new Double(128.058578));
		aa_masses.put(new String("R"), new Double(156.10111));
		aa_masses.put(new String("S"), new Double(87.032028));
		aa_masses.put(new String("T"), new Double(101.047679));
		
		aa_masses.put(new String("U"), new Double(150.95363));		// selenocystine
		aa_masses.put(new String("V"), new Double(99.068414));
		aa_masses.put(new String("W"), new Double(186.079313));
		aa_masses.put(new String("Y"), new Double(163.063332));
		
	};
	
	/**
	 * This algorithm matches only ions with m/z <= 2000.0 (as described in the paper)
	 */
	private static final double MAX_MZ_UNDER_CONSIDERATION = 2000.0;
	
	// members
	private String m_peptide;
	private AbstractSpectraCell m_spectra;
	private int m_min, m_max;
	private double[] m_scores;			// max - min + 1 elements
	private int m_site;
	
	/**
	 * Only useful for partially initializing the state of the object, when setter method(s) will be used
	 * later on. Not recommended for non-expert users of this class.
	 */
	public PeptideScore() {
		m_peptide = "";
		m_spectra = null;
		m_min = 1;
		m_max = 10;
		m_site= -1;
	}
	
	
	public PeptideScore(String peptide, AbstractSpectraCell sc) {
		this(peptide, sc, 1, 10);
	}
	
	public PeptideScore(String pep, AbstractSpectraCell sc, int min, int max) {
		assert(sc != null && pep != null && pep.length() > 2);		// ascore cant possibly work with really short peptides
		m_peptide = pep;
		m_spectra = sc;
		m_min     = min;
		m_max     = max;
		assert(max > min && min > 0 && max > 0);
		m_scores = new double[max - min + 1];
		m_site = -1;
	}
	
	public void set_peptide(String m_peptide) {
		this.m_peptide = m_peptide;
	}

	public String get_peptide() {
		return m_peptide;
	}

	public boolean hasSite() {
		return (m_site >= 0);
	}
	
	/**
	 * Returns the zero-relative site of the proposed phosphorylation (must be in the range <code>0..length(peptide)-1</code> inclusive)
	 * @return
	 */
	public int getSite() {
		return m_site;
	}
	
	public void setSite(int loc) {
		assert(loc >= 0 && loc < m_peptide.length());
		m_site = loc;
	}

	/**
	 * Set the raw score for the given phosphorylation site and <code>peak_depth</code>
	 * to the specified value. A score of <code>0.0</code> will be assigned for any negative
	 * value.
	 * 
	 * @param p_x
	 * @param peak_depth must be a value between 1 and 10 
	 */
	public void set_raw_score(double p_x, int peak_depth) {
		assert(peak_depth >= 1 && peak_depth <= m_scores.length);
		if (p_x < 0.0)
			p_x = 0.0;
		m_scores[peak_depth-1] = p_x;
	}
	
	/**
	 * Return the weight final peptide score as described in the paper, based on scores
	 * currently held in <code>this</code>
	 */
	public double getFinalPeptideScore() {
		assert(m_scores.length == 10);		// only correct when scores vector is ten elements
		
		// NB: scores[0] equals peak depth 1, scores[1] equals peak depth 2 etc...
		return (0.5 * m_scores[0] +
					 0.75 * m_scores[1] +
					 m_scores[2] + m_scores[3] + m_scores[4] + m_scores[5] + 
					 0.75 * m_scores[6] + 
					 0.5  * m_scores[7] +
					 0.25 * m_scores[8] +
					 0.25 * m_scores[9] ) / 10.0;
	}
	
	/**
	 * returns the unimod.org phoshphorylation delta (monoisotopic) in Da for the specified AA. 
	 * Does not check to see if the AA is phosphorylatable or not.
	 * 
	 * @param aa uppercase single-letter amino acid being considered for phospho (eg. "S" or "T")
	 * @return the mass delta for a phosphorylation possibly including neutral loss (if it is most likely for the residue)
	 */
	protected double phospho_site(String aa) {
			double delta = 79.966331;
			if (aa.equals("S") || aa.equals("T"))
				delta += Ion.MASS_H2O;
			return delta;
	}
	
	public String getPhosphorylationSitePeptide() {
		String pep = m_peptide.substring(0, getSite());
		pep += 'p';
		pep += m_peptide.substring(getSite());
		return pep;
	}
	
	/**
	 * Returns the ID of the spectra (format-specific)
	 * @return
	 */
	public String getSpectraID() {
		return m_spectra.getID();
	}
	
	/**
	 * Returns a nicely formatted final (weighted average) peptide score from all the peak depths
	 */
	@Override
	public String toString() {
		String score_str = "";
		for (double d : m_scores) {
			score_str += d + " ";
		}
		return getSpectraID()+"("+m_spectra.getCharge()+"): "+getPhosphorylationSitePeptide() + " " + getFinalPeptideScore()+"["+score_str+"]";
	}

	/**
	 * Returns the raw (unweighted) peptide score  (as described in the paper) at the chosen
	 * peak depth. This method will provide undefined results if <code>set_raw_score(..., peaks)</code> has not
	 * been called beforehand.
	 * 
	 * @param peaks must be in the range <tt>[1,10]</tt>
	 * @return the computed peptide score
	 */
	public double getScore(int peaks) {
		assert(peaks >= 1 && peaks <= m_scores.length);
		return m_scores[peaks-1];
	}

	private void add_ion(IonFilterInterface filter, IonList il, Ion i) {
		if (filter == null || filter.accept(i))
			il.add(i);
	}

	/**
	 * Returns a new list of ions, based on <code>theoretical_ions</code>, with the assumption of a 
	 * phosphorylation modification on the residue whose index is given by <code>this</code> PeptideScore's
	 * site of modification ie. <code>getSite()</code>. This routine will only return B&Y ions.
	 * 
	 * @param theoretical_ions
	 * @param filter the desired ion filtering procedure eg. removal of neutral loss ions (if not <code>null</code>)
	 * @return
	 */
	public IonList getTheoreticalPhosphorylatedIons(IonFilterInterface filter)  {
		IonList il = new IonList();
		
		// compute modification based on probable z (usually this is the determined charge 
		// state from your program which produces MS/MS spectra in MGF format) eg. wiff2dta or ProteinPilot or msconvert...
		int z        = m_spectra.getProbableZ();
		
		// calculate the theoretic ions to be used to during score calculation and return them
		double b_sum = Ion.MASS_PROTON;
		double y_sum = 0.0;
		for (int i=0; i<m_peptide.length(); i++) {
			
			// sum is calculated in terms of singly-charged ions with no ammonia/water neutral loss
			try { 
				// 1. compute the b-ion series
				if (i<m_peptide.length() - 1) {
				String aa = ""+m_peptide.charAt(i);
				if (i == getSite()) {
					b_sum += phospho_site(aa);
				} 
				b_sum += aa_masses.get(aa).doubleValue();
				Ion b_ion = new Ion("b"+(i+1), b_sum);
				add_ion(filter, il, b_ion);
			
				// triply charged spectra?
				if (z >= 3) {
					// doubly-charged b-ion
					Ion b_ion_pp = new Ion(b_ion);
					b_ion_pp.set_mz((b_ion.get_mz()+Ion.MASS_PROTON) / 2.0);
					b_ion_pp.set_charge("++");
					add_ion(filter, il, b_ion_pp);
					// quadruply charge spectra?
					if (z >= 4) {
						Ion b_ion_ppp = new Ion(b_ion);
						b_ion_ppp.set_mz((b_ion.get_mz()+2*Ion.MASS_PROTON) / 3.0);
						b_ion_ppp.set_charge("+++");
						add_ion(filter, il, b_ion_ppp);
					}
				}
				}
				
				// 2. and now the y-ion series
				int y_site = m_peptide.length()-1-i;
				if (y_site > 0) {
					String aa         = ""+m_peptide.charAt(y_site);
					if (i == 0) {			// TODO: no loss of water at C-terminii???
						y_sum += Ion.MASS_H2O + Ion.MASS_PROTON;
					}
					y_sum += aa_masses.get(""+aa).doubleValue();
					if (y_site == getSite()) {
						y_sum += phospho_site(aa);
					}
					Ion y_ion = new Ion("y"+(i+1), y_sum);
					add_ion(filter, il, y_ion);
					
					// triply charged spectra?
					if (z >= 3) {
						// doubly-charged b-ion
						Ion y_ion_pp = new Ion(y_ion);
						y_ion_pp.set_mz((y_ion.get_mz()+Ion.MASS_PROTON) / 2.0);
						y_ion_pp.set_charge("++");
						add_ion(filter, il, y_ion_pp);
						// quadruply charged precursor?
						if (z >= 4) {
							Ion y_ion_ppp = new Ion(y_ion);
							y_ion_ppp.set_mz((y_ion.get_mz()+2*Ion.MASS_PROTON) / 3.0);
							y_ion_ppp.set_charge("+++");
							add_ion(filter, il, y_ion_ppp);
						}
					}
				}
				
			} catch (InvalidIonException ev) {
				// TODO: be silent for now...
				continue;
			} catch (NullPointerException npe) {
				Logger.getAnonymousLogger().warning("Non-standard AA residue in: "+m_peptide);
			}
		}
		
		// validate using JPL (albeit a 1.0pre release) -- from the FAQ from JPL at sourceforge.net 
		/*
		JPLPeptideFragmenter fragmenter =
			new JPLPeptideFragmenter.Builder(EnumSet.of(
				JPLFragmentationType.BY, JPLFragmentationType.PRECURSOR)).build();

		// the peptide to fragment incl. proposed phospho site mod
		String pep = get_peptide();
		String tmp_pep = pep.substring(0, getSite());
		String mod_aa  = ""+pep.charAt(getSite());
		double mod     = phospho_site(mod_aa);
		tmp_pep       += mod_aa + "({"+mod+"})";
		tmp_pep       += pep.substring(this.getSite()+1);
		JPLPeptide aas = new JPLPeptide.Builder(tmp_pep).build();
		
		try {
			
			// set the precursor peptide with its charge
			fragmenter.setFragmentablePrecursor(aas, z);
	
			// just make the job
			fragmenter.generateFragments();
	
			// get the fragment list
			JPLIMSPeakList pl = fragmenter.getPeakList();
			
			String tmp = "";
			int idx = 0;
			for (double d : pl.getMzs()) {
				tmp += d + " ";
				if (idx++ % 4 == 0)
					tmp += "\n";
			}
			Logger.getAnonymousLogger().info("Theoretical B-ion peaks for "+tmp_pep+": "+tmp);
		} catch (Exception e) {
			e.printStackTrace();
			return il;
		} */
		
		Logger.getAnonymousLogger().info(this.getPhosphorylationSitePeptide() +"\n"+il.toString());
		return il;
	}


	/**
	 * Returns the phosphopeptide with the phosphorylation site indicated in red for ease of viewing
	 * @return
	 */
	public String getPhosphoPeptideAsHTML() {
		String pep = m_peptide.substring(0, getSite());
		pep += "<font color=\"red\"><b>";
		pep += m_peptide.charAt(getSite());
		pep += "</b></font>";
		if (getSite()+1 < m_peptide.length())
			pep += m_peptide.substring(getSite()+1);
		return pep;
	}
	
}
