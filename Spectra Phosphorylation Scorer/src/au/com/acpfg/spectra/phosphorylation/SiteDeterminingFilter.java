package au.com.acpfg.spectra.phosphorylation;

import java.util.logging.Logger;

/**
 * An <code>IonFilterInterface</code> which only accepts ions that are "site determining"
 * during the AScore calculation as described in the paper by Gygi et al.
 * 
 * @author andrew.cassin
 *
 */
public class SiteDeterminingFilter implements IonFilterInterface {
	private int m_begin, m_end;
	private int m_peptide_length;
	
	/**
	 * Construct an IonFilterInterface which removes all ions apart from b&y ions between
	 * the nominated residue indices. Order of the parameters does not matter.
	 * 
	 * @param score1 the PeptideScore after which the site determining ions commence
	 * @param score2 the PeptideScore before which the site determining ions end
	 */
	public SiteDeterminingFilter(PeptideScore score1, PeptideScore score2) {
		int begin = score1.getSite();
		int end   = score2.getSite();
		m_peptide_length = score1.get_peptide().length();
		if (end < begin) {
			m_begin = end;
			m_end   = begin;
		} else {
			m_begin = begin;
			m_end   = end;
		}
	}
	
	@Override
	public boolean accept(Ion i) {
		if (i.has_lost_h2o() || i.has_lost_nh3())		// neutral loss ions are ignored...
			return false;
		
		int idx = i.get_idx();
		if (i.is_B()) {
			return (idx >= m_begin && idx <= m_end);
		} else if (i.is_Y()){
			idx = m_peptide_length - idx;
			boolean ret = (idx > m_begin && idx <= m_end);
			//if (!ret) {
			//	Logger.getAnonymousLogger().info("rejecting y ion: "+i.toString()+" (begin,end)="+m_begin+" "+m_end);
			//}
			return ret;
		}
		return false;
	}

}
