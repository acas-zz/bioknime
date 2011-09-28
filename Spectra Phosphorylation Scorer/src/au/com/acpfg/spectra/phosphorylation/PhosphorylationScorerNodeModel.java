package au.com.acpfg.spectra.phosphorylation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.com.acpfg.misc.spectra.AbstractSpectraCell;
import au.com.acpfg.misc.spectra.MGFSpectraCell;


/**
 * This is the model implementation of PhosphorylationScorer which currently only implements
 * the AScore algorithm described by 
 *
 * @author Andrew Cassin
 */
public class PhosphorylationScorerNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(PhosphorylationScorerNodeModel.class);
        
    // settings model configuration data
    static final String CFGKEY_SPECTRA = "spectra-col";
    static final String CFGKEY_MODPEP  = "modified-peptide-col";		// NB: only supports mascot peptide identifications for now
    static final String CFGKEY_IONS    = "identified-ions-col";
    static final String CFGKEY_RESIDUES= "residues-to-consider-for-phosphoryltation";

	private static final double WINDOW_SIZE = 100.0;			// as specified in paper

    
    // persisted state
    private final SettingsModelString m_spectra         = new SettingsModelString(CFGKEY_SPECTRA, "Spectra");
    private final SettingsModelString m_modpep          = new SettingsModelString(CFGKEY_MODPEP, "Modified Peptide (list)");
    private final SettingsModelString m_identified_ions = new SettingsModelString(CFGKEY_IONS, "Identified Ions (list)");
    private final SettingsModelStringArray m_residues   = new SettingsModelStringArray(CFGKEY_RESIDUES, new String[] { "S", "T", "Y" });
    // private data members (not persisted)
    private int m_ascore_best_successes, m_ascore_sbest_successes;
    
    /**
     * Constructor for the node model.
     */
    protected PhosphorylationScorerNodeModel() {
            super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
try {
    	int spectra_idx = inData[0].getDataTableSpec().findColumnIndex(m_spectra.getStringValue());
    	int modpep_idx  = inData[0].getDataTableSpec().findColumnIndex(m_modpep.getStringValue());
    	int ions_idx    = inData[0].getDataTableSpec().findColumnIndex(m_identified_ions.getStringValue());
    	if (spectra_idx < 0 || modpep_idx < 0 || ions_idx < 0) {
    		throw new Exception("Cannot find columns: "+m_spectra.getStringValue()+" "+m_modpep.getStringValue()+" - reconfigure the node?");
    	}
    	
    	DataTableSpec[] out_tables = make_output_tables();
        BufferedDataContainer container = exec.createDataContainer(out_tables[0]);
        BufferedDataContainer c2 = exec.createDataContainer(out_tables[1]);
        
        // process rows
        RowIterator rows = inData[0].iterator();
        int rejected = 0;
        
    	HashSet<String> ok_residues = new HashSet<String>();
		for (String aa : m_residues.getStringArrayValue()) {
			ok_residues.add(aa.toUpperCase());
		}
		
		int done = 0;
		int n_rows = inData[0].getRowCount();
		int a_rk  = 1;
		int ps_rk = 1;
        while (rows.hasNext()) {
        	DataRow r = rows.next();
        	DataCell c_spectra = r.getCell(spectra_idx);
        	DataCell c_ions    = r.getCell(ions_idx);
        	DataCell c_peptide = r.getCell(modpep_idx);
        	
        	if (c_spectra.isMissing() || c_ions.isMissing() || c_peptide.isMissing()) {
        		logger.warn("Missing data (ions, spectra or peptide) on row: "+r.getKey().getString()+", skipping.");
        		rejected++;
        		continue;
        	}
        	AbstractSpectraCell spectra_cell = (AbstractSpectraCell) c_spectra;
        	String                   peptide = c_peptide.toString().trim();
        	
        	IonList identified_ions = make_ions(c_ions, null); // all mascot identified ions are to be in the list
			
        	// reject peptide identifications where they do not meet the algorithmic constraints
        	if ((peptide = ok_psm(ok_residues, peptide)) == null) {
        		rejected++;
        		continue;
        	}
        	
        	// iterate thru spectra using windows extracting only the i most intense peaks within each window
        	double[] mz        = spectra_cell.getMZ();
        	double[] intensity = spectra_cell.getIntensity();
        	int z              = 1;
        	if (mz.length != intensity.length) {
        		throw new Exception("Internal error - perhaps a bug or bad spectra data: mz != intensity!");
        	}
        	double precursor_mass = 0.0;			// NB: always calculated for z=1
        	if (spectra_cell instanceof MGFSpectraCell) {
        		MGFSpectraCell mgf = (MGFSpectraCell) spectra_cell;
        		String pepmass = mgf.getPepmass().trim();
        		try {
        			z = mgf.getProbableZ();		// looks at CHARGE header in MGF peak list
        			if (pepmass.length() > 0)
        				precursor_mass = new Double(pepmass).doubleValue() * z - z;
        		} catch (Exception e) {
        			logger.warn("Precursor mass not understood: "+pepmass+" results may be inaccurate");
        		}
        	}
        	
        	/*
        	if (first) {
        		logger.info(spectra_cell.getID());
        		logger.info(identified_ions);
        		first = false;
        	}*/
        	
        	/**
        	 * the scores[] array contains a series of PeptideScore objects, one for each phosphorylatable
        	 * residue in the specified peptide. Each score must hold the raw scores at each peak depth (10 of them)
        	 * and be able to compute the weighted average as described in the paper. Each PS also holds an ID
        	 * back to the actual spectra so you can examine the results by-hand if desired.
        	 */
        	PeptideScore[] scores = init_phospopeptide_sites(ok_residues, peptide, spectra_cell, 1, 10);
        
    		double min_mz         = spectra_cell.getMinMZ();
    		double max_mz         = spectra_cell.getMaxMZ();
    		min_mz                = min_mz - (min_mz%WINDOW_SIZE);
    		max_mz                = max_mz - (max_mz%WINDOW_SIZE) + WINDOW_SIZE;
    		
    		// compute peak list sorted by ascending m/z
    		Peak[] peaks = make_sorted_peak_list(spectra_cell.getMZ(), spectra_cell.getIntensity());
    		
    		// this modifies the objects within scores[] 
    		calc_peptide_score(peptide, min_mz, max_mz, precursor_mass, 
    				           scores, peaks, identified_ions);
    	
    		// put peptide scores into second output port
    		for (PeptideScore ps : scores) {
    			DataCell[] cells = new DataCell[3];
    			for (int i=0; i<10; i++) {
    				cells[0] = new StringCell(ps.getPhosphorylationSitePeptide());
    				cells[1] = new IntCell(i+1);
    				cells[2] = new DoubleCell(ps.getScore(i+1));
    				c2.addRowToTable(new DefaultRow("ps"+ps_rk++, cells));
    			}
    		}
    		
    		// pick the two best *weighted average* peptide scores for the a-score calculation
    		Arrays.sort(scores, new Comparator<PeptideScore>() {

				@Override
				public int compare(PeptideScore a, PeptideScore b) {
					double a_score = a.getFinalPeptideScore();
					double b_score = b.getFinalPeptideScore();
					if (a_score < b_score) {
						return 1;
					} else if (a_score > b_score) {
						return -1;
					} else {
						return 0;
					}
				}
    			
    		});
    		
    		logger.info("Best PS sites: "+scores[0].getPhosphorylationSitePeptide()+" "+scores[1].getPhosphorylationSitePeptide());
    		int best_depth = compute_best_depth(scores[0], scores[1]);
    		logger.info("Choosing peak depth: "+(best_depth+1)+": scores="+scores[0].getFinalPeptideScore()+","+scores[1].getFinalPeptideScore());
    		
    		double ascore = compute_ascore(scores[0], scores[1], 
    						peaks, min_mz, max_mz, max_mz, best_depth, identified_ions);
    		
    		DataCell[] cells = new DataCell[9];
    		cells[0] = new StringCell(spectra_cell.getID());
    		cells[1] = new StringCell((ascore < 3.0) ? "no" : "yes");
    		cells[2] = new DoubleCell(ascore);
    		cells[3] = new StringCell("<html><pre>"+scores[0].getPhosphoPeptideAsHTML());
    		cells[4] = new StringCell(scores[0].getPhosphorylationSitePeptide());
    		cells[5] = new StringCell(scores[1].getPhosphorylationSitePeptide());
    		cells[6] = new IntCell(new Integer(best_depth+1));
    		cells[7] = new IntCell(new Integer(m_ascore_best_successes));
    		cells[8] = new IntCell(new Integer(m_ascore_sbest_successes));
    		container.addRowToTable(new DefaultRow("Spectra"+a_rk++, cells));
    		
    		// user cancel?
    		done++;
        	if (done % 100 == 0) {
        		exec.checkCanceled();
        		exec.setProgress((((double)done)/n_rows));
        	}
        }
        container.close();
        c2.close();
        if (rejected > 0) {
        	logger.warn("Rejected "+rejected+" PSMs (ie. rows) as they did not meet algorithmic constraints");
        }
        BufferedDataTable out = container.getTable();
        BufferedDataTable out2= c2.getTable();
        return new BufferedDataTable[]{out, out2};
} catch (Exception e) {
	e.printStackTrace(); throw e;
}
    }

    /**
     * Compute the ascore based on the published procedure using best_score and second_best_score. The
     * peaks are from the processed spectrum being AScore'ed,
     * @param best_score
     * @param second_best_score
     * @param peaks
     * @param min_mz         a lower bound on m/z to use (currently not used)
     * @param max_mz         an upper bound on m/z (currently not used)
     * @param precursor_mass 
     * @param peak_depth     number of peaks to use from peptide score calculation
     * @param identified_ions
     * @return
     * @throws Exception
     */
    protected double compute_ascore(PeptideScore best_score, PeptideScore second_best_score, Peak[] peaks, double min_mz, double max_mz, 
    		double precursor_mass, int peak_depth, IonList identified_ions) throws Exception {
    	
    	// compute the site determining ions for best two phosphopeptides
    	IonList best_ions  = best_score.getTheoreticalPhosphorylatedIons(new SiteDeterminingFilter(best_score, second_best_score));
    	IonList sbest_ions = second_best_score.getTheoreticalPhosphorylatedIons(new SiteDeterminingFilter(best_score, second_best_score));
    	
    /*	logger.info("BEST IONS:\n"+best_ions.toString());
    	logger.info("NEXT BEST IONS:\n"+sbest_ions.toString());
    	
    	logger.info(best_score.getSpectraID());
    	logger.info("M/Z range for best ions list: "+best_ions.getMZRange());
    	logger.info("M/Z range for second best ions list: "+sbest_ions.getMZRange()); */
    	
    	// as outlined in the paper: compute peaks which survive after window processing and intensity selection
		Peak[] survivors = preprocess_peaks(peaks, min_mz, max_mz, peak_depth, precursor_mass);
		// NB: survivors MUST be in order of increasing m/z
		
    	//	According to the paper, for the *peptide score* the number of trials is the total number of mascot identified ions
		//  but for the ascore calculation it is the number of B&Y series site determining ions
    	int n         = Math.abs((best_score.getSite() - second_best_score.getSite()) * 2);
    	
    	// 1. compute ascore of best phosphopeptide
    	int successes = calc_successes(survivors, identified_ions, best_ions);
    	m_ascore_best_successes = successes;		// to save into output table after call completes
    	double p      = ((double)peak_depth) / 100.0;
    	double score_best    = 0.0;
    	double score_sbest   = 0.0;
    	
    	if (successes > n) {
    		throw new InvalidAScoreException("Got too many successes (>n): successes="+successes+", n="+n+" for spectra: "+best_score.getSpectraID());
    	}
    	
    	for (int k=successes; k<=n; k++) {
    		score_best += compute_binomial(n, k) * Math.pow(p, k) * Math.pow(1.0-p, n-k);
    	}
    	// 2. compute ascore of second best phosphopeptide
    	successes = calc_successes(survivors, identified_ions, sbest_ions);
    	m_ascore_sbest_successes = successes;		// to save into output table after call completes
    	for (int k=successes; k<=n; k++) {
    		score_sbest += compute_binomial(n,k) * Math.pow(p, k) * Math.pow(1.0-p, n-k);
    	}
    	
    	// 3. compute and return difference between best and second best (ie. the ascore value)
    	return (-10.0 * Math.log10(score_best)) - (-10.0*Math.log10(score_sbest)) ;
	}

	/**
     * Shared between configure() and execute, this method provides the tables specification
     * for all output ports
     */
    protected DataTableSpec[] make_output_tables() {
    	DataColumnSpec[] output_cols = new DataColumnSpec[9];
    	output_cols[0] = new DataColumnSpecCreator("Spectra ID", StringCell.TYPE).createSpec();
    	output_cols[1] = new DataColumnSpecCreator("Localised?", StringCell.TYPE).createSpec();
    	output_cols[2] = new DataColumnSpecCreator("AScore", DoubleCell.TYPE).createSpec();
    	output_cols[3] = new DataColumnSpecCreator("Phosphopeptide (html)", StringCell.TYPE).createSpec();
    	output_cols[4] = new DataColumnSpecCreator("Best candidate phosphopeptide", StringCell.TYPE).createSpec();
    	output_cols[5] = new DataColumnSpecCreator("Second best candidate phosphopeptide", StringCell.TYPE).createSpec();
    	output_cols[6] = new DataColumnSpecCreator("Chosen Peak Depth", IntCell.TYPE).createSpec();
    	output_cols[7] = new DataColumnSpecCreator("Best candidate matching peaks (at chosen depth)", IntCell.TYPE).createSpec();
    	output_cols[8] = new DataColumnSpecCreator("Second best candidate matching peaks (at chosen depth)", IntCell.TYPE).createSpec();
    	
	    DataColumnSpec[] ps_cols    = new DataColumnSpec[3];
        ps_cols[0] = new DataColumnSpecCreator("Possible Phosphorylation Site", StringCell.TYPE).createSpec();
        ps_cols[1] = new DataColumnSpecCreator("Peak Depth (n most intense peaks per 100 units)", IntCell.TYPE).createSpec();
        ps_cols[2] = new DataColumnSpecCreator("Raw Peptide Score at depth", DoubleCell.TYPE).createSpec();
    	return new DataTableSpec[] { new DataTableSpec(output_cols), new DataTableSpec(ps_cols) };
    }
    
    /**
     * Computes the number of peaks to use for the Ascore calculation
     * @param sites
     * @param best the peptide score which has the top-most PS at the chosen number of peaks
     * @param second_best the peptide score which has the next-best PS at the chosen number of peaks
     * @return the number of peaks per 100mz units which provide the best separation between top two PS's
     */
    public int compute_best_depth(PeptideScore best, PeptideScore second_best) {
    	double[] differentials = new double[10];
    	for (int i=0; i<differentials.length; i++) {
    		differentials[i] = Math.abs(best.getScore(i+1) - second_best.getScore(i+1));
    	}
    	int best_idx = -1;
    	double best_diff = -1.0;
    	for (int i=0; i<differentials.length; i++) {
    		if (differentials[i] > best_diff) {
    			best_diff = differentials[i];
    			best_idx = i;
    		}
    	}
    	assert(best_idx >= 0);
    	return best_idx;
	}

	protected double fac(int n) {
    	if (n <= 1)
    		return 1.0;
    	return ((double)n) * fac(n-1);
    }
    
    protected double compute_binomial(int trials, int successes) {
    	return fac(trials) / ( fac(successes) * fac(trials-successes));
    }
    
    protected void calc_peptide_score(String peptide, double min_mz, double max_mz, double precursor_mass,
    					PeptideScore[] scores, Peak[] peaks, 
    					IonList identified_ions) {
    	
    	// for each possible phosphorylated residue
    	for (PeptideScore ps : scores) {
    		IonList plausible_ions =  ps.getTheoreticalPhosphorylatedIons(new IonFilterInterface() {

				@Override
				public boolean accept(Ion i) {
					// reject those ions which include any neutral loss of water or ammonia as described in the paper
					if (i.has_lost_h2o() || i.has_lost_nh3())
						return false;
					// else
					return true;
				}
    			
    		});
    		
    		//logger.info(ps.getPhosphorylationSitePeptide());
    		//logger.info(plausible_ions);	
    		
	    	// as described in the paper: try a range of peak depths in each window
	    	for (int peak_depth=1; peak_depth<=10; peak_depth++) {
	    		
		    	// as outlined in the paper: compute peaks which survive after window processing and intensity selection
				Peak[] survivors = preprocess_peaks(peaks, min_mz, max_mz, peak_depth, precursor_mass);
				// NB: survivors MUST be in order of increasing m/z
				
		    	//	According to the paper, for the *peptide score* the number of trials is the total number of mascot identified ions
		    	int n         = identified_ions.size();
		    	int successes = calc_successes(survivors, null, plausible_ions);
		    	double p      = ((double)peak_depth) / 100.0;
		    	double p_x    = 0.0;
		    	
		    	for (int k=successes; k<=n; k++) {
		    		p_x += compute_binomial(n, k) * Math.pow(p, k) * Math.pow(1.0-p, n-k);
		    	}
		    	p_x = -10.0 * Math.log10(p_x);
		    	ps.set_raw_score(p_x, peak_depth);
		    	//logger.info("N, successes: "+n+" "+successes+" "+p_x);
	    	}
	    	
	    	//logger.info(ps.toString());
    	}
    	
	}

    /**
     * Returns a count of the number of peaks in survivors within the range of m/z values of <code>determining_ions</code>
     * which match a set of identified ions (iff not <code>null</code>)
     * @param survivors
     * @param identified_ions		Ions matched by mascot for the spectra (they appear bold red in Mascot 2.0 peptide view)
     * @param determining_ions		Site determining ion list as specified by paper written by Gygi et al.
     * @return
     */
	private int calc_successes(Peak[] survivors, IonList identified_ions, IonList determining_ions) {
		int cnt = 0;
		for (Peak p : survivors) {
				// if a peak (within tolerance) is found in the identified ions list (from mascot) and in the
				// determining_ion list (eg. theoretical site determining ions) we consider it a success
				if (identified_ions == null) {
					if (p.matches(determining_ions))
						cnt++;
				} else if (p.matches(identified_ions) && p.matches(determining_ions)) {
					cnt++;
				}
		}
		return cnt;
	}

	protected PeptideScore[] init_phospopeptide_sites(Set<String> ok_residues, String peptide, AbstractSpectraCell sc, int min, int max) {
		int n_phospho_sites = 0;
		ArrayList<Integer> sites = new ArrayList<Integer>();
		for (int i=0; i<peptide.length(); i++) {
			if (ok_residues.contains(new String(""+peptide.charAt(i)))) {
				n_phospho_sites++;
				sites.add(new Integer(i));
			}
		}
		
		// NB: first site *MUST* be scores[0], last site in peptide *MUST* be scores[n_phospho-1]
		PeptideScore[] scores = new PeptideScore[n_phospho_sites];
		for (int i=0; i<n_phospho_sites; i++) {
    		scores[i] = new PeptideScore(peptide, sc, min, max);
    		scores[i].setSite(sites.get(i).intValue());
    	}
		
		return scores;
	}

	/**
     * Makes an array of Peak objects each with a single (mz, intensity) pair sorted by increasing m/z (in case the spectra cell returns data in random order)
     * @param mz
     * @param intensity
     * @return sorted array of peaks
     */
    protected Peak[] make_sorted_peak_list(double[] mz, double[] intensity) {
    	assert(mz.length > 0 && intensity.length > 0 && mz.length == intensity.length);
		Peak[] ret = new Peak[mz.length];
		for (int i=0; i<mz.length; i++) {
			ret[i] = new Peak(mz[i], intensity[i]);
		}
		Arrays.sort(ret);
		return ret;
	}

	/**
     * Select the peaks which are suitable for peptide score based on the paper. It is required that the supplied peaks be in order
     * of increasing m/z (ie. smallest m/z first)
     * 
     * @param peaks
     * @param mz_min   the minimum bound on windowing (must be a multiple of the <code>WINDOW_SIZE</code>)
     * @param mz_max   the maximum bound on windowing (must be a multiple of the <code>WINDOW_SIZE</code>)
     * @param keep_n   the number of most intense peaks to return to the caller
     * @return
     */
    protected Peak[] preprocess_peaks(Peak[] peaks, double mz_min, double mz_max, int keep_n, double precursor_mass) {
    	ArrayList<Peak> ret = new ArrayList<Peak>();
    	int i = 0;
		while (mz_min < mz_max) {
			
			// add all peaks in current window
			ArrayList<Peak> windowed_peaks = new ArrayList<Peak>();
			while (i<peaks.length && peaks[i].get_mz() < mz_min + WINDOW_SIZE) {
				windowed_peaks.add(peaks[i]);
				i++;
			}
			
			// sort peaks by decreasing intensity
			Collections.sort(windowed_peaks, new Comparator<Peak>() {

				@Override
				public int compare(Peak o1, Peak o2) {
					if (o1.get_intensity() > o2.get_intensity()) {
						return -1;
					} else if (o1.get_intensity() < o2.get_intensity()) {
						return 1;
					} // else
					return 0;
				}
				
			});
			
			/**
			 *  add first n SUITABLE peaks to results list
			 *  1) only one peak per m/z unit (biggest is chosen)
			 *  2) removal of isotopic clusters -- TODO
			 *  3) removal of precursor ion-specific losses (water and phosphoric acid)
			 *  Its possible that there are not n peaks in the window. Oh well too bad.
			 */
			double min = 0.0;
			int found = 0;
			for (int j=0; found<keep_n && j<windowed_peaks.size(); j++) {
				Peak p = windowed_peaks.get(j);
				if (p.get_mz() >= min) {					      // 1)
					if (p.isPrecursorNeutralLoss(precursor_mass)) // 3)
						continue;
					ret.add(p);
					found++;
					min = Math.floor(p.get_mz()) + 1.0;           // 1)
				}
			}
			
			mz_min += WINDOW_SIZE;
		}
		
		return ret.toArray(new Peak[0]);
	}

	/**
     * Rejects peptides (technically PSM) which do not meet the algorithmic constraints. 
     * <code>null></code> here will cause the row to be supressed from the node output
     * 
     * @param peptide the modified peptide string 
     * @return null if the peptide does not meet algorithmic constraints, the AA string representing the peptide otherwise
     */
    private String ok_psm(HashSet<String> ok_residues, String peptide) {
		if (peptide == null || peptide.length() < 1)
			return null;
		
		// go thru the peptide sequence looking for suitable residues after removing information not of interest (for now)
		peptide = peptide.replaceAll("<[^>]+?>", "");
		peptide = peptide.replaceAll("^[A-Z]+\\d-", "");
		peptide = peptide.replaceAll("-\\w+$", "");
	
		int sites = 0;
		for (int i=0; i<peptide.length(); i++) {
			String c = ""+peptide.charAt(i);
			if (ok_residues.contains(c)) {
				sites++;
			}
		}
		
		return (sites >= 2) ? peptide : null;
	}

    /*
     * Return a list of ions from the textual description which are acceptable to <code>ifilt</code>
     * Unacceptable ions are skipped. If <code>ifilt</code> is null, all ions are accepted.
     */
	private IonList make_ions(DataCell c_ions, IonFilterInterface ifilt) throws InvalidIonException {
		IonList ions = new IonList();
		if (c_ions != null && !c_ions.isMissing()) {
			ListCell list_of_ions = (ListCell) c_ions;
			for (DataCell c : list_of_ions) {
				StringCell ion = (StringCell) c;
				String[] fv = ion.getStringValue().split("=");
				Ion i = new Ion(fv[0], new Double(fv[1]).doubleValue());
				
				if (ifilt != null && !ifilt.accept(i)) // eg. &y[9]++-H20
					continue;
				// else
				ions.add(i);
			}
		}
		return ions;
	}
	

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        return make_output_tables();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_spectra.saveSettingsTo(settings);
    	m_residues.saveSettingsTo(settings);
    	m_identified_ions.saveSettingsTo(settings);
    	m_modpep.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_spectra.loadSettingsFrom(settings);
    	m_residues.loadSettingsFrom(settings);
    	m_identified_ions.loadSettingsFrom(settings);
    	m_modpep.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_spectra.validateSettings(settings);
    	m_residues.validateSettings(settings);
    	m_identified_ions.validateSettings(settings);
    	m_modpep.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
  
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    }

}

