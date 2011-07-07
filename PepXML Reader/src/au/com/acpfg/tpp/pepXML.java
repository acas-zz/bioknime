package au.com.acpfg.tpp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.stream.XMLStreamReader;

/*
 * Class defining the pepXML object
 */
public class pepXML {
	//private String searchEngine;   // stores the name/type of search tool used
	private String srcFile;
	private String specId;
	private double mass;
	private int charge;
	private String peptide;
	private char prevAA;
	private char nextAA;
	private String modPeptide;
	private double iniProb;
	private double wt; // variable used in protXML files
	private double nsp; // variable used in protXML files
	private int ntt; // variable used in protXML files
	private int nspecs; // variable used in protXML files
	private PepXMLResultInterface globals;
	
	private HashMap<Integer, Integer> aaMods; // holds the AA modification
											  // positions
	private String m_protein;		// accession(s)
	private String m_protein_descr; // database description (if any)
	private int m_hit_rank;
	private int m_matched_ions, m_total_ions;
	
	// Variables for XTANDEM search results
	private double hyperscore;
	private double nextscore;
	private double xtandem_expect;
	
	// Variables for MASCOT search results
	private double mascot_ionscore;
	private double mascot_identityscore;
	private int mascot_star;
	private double mascot_homologyscore;
	private double mascot_expect;
	
	// Variables for SEQUEST search results
	private double sequest_xcorr;
	private double sequest_deltacn;
	private double sequest_deltacnstar;
	private double sequest_spscore;
	private double sequest_sprank;
	
	// peptide prophet assigned scores
	private double m_pp_ntt, m_pp_fval, m_pp_massd, m_pp_nmc;

	public pepXML() {
		globals = new PepXMLGlobals();
		srcFile = null;
	}

	public pepXML(String txt) {
		this();
		srcFile = txt;
	}

	public String getFilename() {
		return srcFile;
	}
	
	// public SET functions (need them for parsing protXML files)
	public void setPeptide(String txt) {
		this.peptide = txt;
	}
	
	public void setCharge(String txt) {
		this.charge = Integer.parseInt(txt);
	}

	public void setIniProb(String txt) {
		this.iniProb = Double.parseDouble(txt);
	}

	public void setNSP(String txt) {
		this.nsp = Double.parseDouble(txt);
	}

	public void setWt(String txt) {
		this.wt = Double.parseDouble(txt);
	}

	public void setMass(String txt) {
		this.mass = Double.parseDouble(txt);
	}

	public void setNTT(String txt) {
		this.ntt = Integer.parseInt(txt);
	}

	public void setNspecs(String txt) {
		this.nspecs = Integer.parseInt(txt);
	}

	// public GET functions
	public String getSpecId() {
		return specId;
	}

	public double getMass() {
		return mass;
	}

	public int getCharge() {
		return charge;
	}

	public String getPeptide() {
		return peptide;
	}

	public char getPrevAA() {
		return prevAA;
	}

	public char getNextAA() {
		return nextAA;
	}
	
	public String getProteinIds() {
		return m_protein;
	}

	public String getProteinDescr() {
		return m_protein_descr;
	}
	
	public int hitRank() {
		return m_hit_rank;
	}
	
	public int getMatchedIons() {
		return m_matched_ions;
	}
	
	public int getTotalIons() {
		return m_total_ions;
	}
	
	public String getModPeptide() {
		return modPeptide;
	}

	public double getHyperscore() {
		return hyperscore;
	}

	public double getNextscore() {
		return nextscore;
	}

	public double getXtandem_expect() {
		return xtandem_expect;
	}

	public double getIniProb() {
		return iniProb;
	}

	public double getWt() {
		return wt;
	}

	public double getNSP() {
		return nsp;
	}

	public int getNTT() {
		return ntt;
	}

	public int getNspecs() {
		return nspecs;
	}

	// MASCOT variables
	public double getMascot_ionscore() {
		return mascot_ionscore;
	}
	
	public double getMascot_identityscore() {
		return mascot_identityscore;
	}
	
	public int getMascot_star() {
		return mascot_star;
	}
	
	public double getMascot_homologyscore() {
		return mascot_homologyscore;
	}
	
	public double getMascot_expect() {
		return mascot_expect;
	}
	
	
	
	// SEQUEST variables
	public double getSequest_xcorr() {
		return sequest_xcorr;
	}
	
	public double getSequest_deltacn() {
		return sequest_deltacn;
	}
	
	public double getSequest_deltacnstar() {
		return sequest_deltacnstar;
	}
	
	public double getSequest_spscore() {
		return sequest_spscore;
	}
	
	public double getSequest_sprank() {
		return sequest_sprank;
	}
	
	public double getPP_fval() {
		return m_pp_fval;
	}
	
	public double getPP_ntt() {
		return m_pp_ntt;
	}
	
	public double getPP_nmc() {
		return m_pp_nmc;
	}
	
	public double getPP_massd() {
		return m_pp_massd;
	}
	
	/*
	 * Function parses the given XML stream and records the relevant information
	 * found in it.
	 */
	public void parse_pepXML_line(XMLStreamReader xmlStreamReader) {
		String attrName = null;
		String attrValue = null;

		for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
			attrName = xmlStreamReader.getAttributeLocalName(i);
			attrValue = xmlStreamReader.getAttributeValue(i);

			if (attrName.equals("spectrum"))
				this.specId = attrValue;
			else if (attrName.equals("assumed_charge"))
				this.charge = Integer.parseInt(attrValue);
			else if (attrName.equals("precursor_neutral_mass"))
				this.mass = Double.parseDouble(attrValue);
			else if (attrName.equals("peptide"))
				this.peptide = attrValue;
			else if (attrName.equals("peptide_prev_aa"))
				this.prevAA = attrValue.charAt(0);
			else if (attrName.equals("peptide_next_aa"))
				this.nextAA = attrValue.charAt(0);
			else if (attrName.equals("hit_rank")) {
				m_hit_rank = Integer.parseInt(attrValue);
			} else if (attrName.equals("protein_descr")) {
				m_protein_descr = attrValue;
			} else if (attrName.equals("protein")) {
				m_protein = attrValue;
			} else if (attrName.equals("num_matched_ions")) {
				m_matched_ions = Integer.parseInt(attrValue);
			} else if (attrName.equals("tot_num_ions")) {
				m_total_ions = Integer.parseInt(attrValue);
			}
		}
	}

	/*
	 * <search_hit hit_rank="1" peptide="VGQTLLK" peptide_prev_aa="K" peptide_next_aa="
P" protein="CD909945_5" num_tot_proteins="1" num_matched_ions="12" tot_num_ions=
"12" calc_neutral_pep_mass="1365.8805" massdiff="-0.1256" num_tol_term="2" num_m
issed_cleavages="0" is_rejected="0" protein_descr="UniRef100_Q0TCD1 Cluster: Pro
bable general secretion pathway protein I; n=4; Escherichia coli|Rep: Probable g
eneral secretion pathway protein I - Escherichia coli O6:K15:H31 (strain 536 \ U
PEC), partial (7%)">
<search_score name="ionscore" value="28.12"/>
<search_score name="identityscore" value="51.98"/>
<search_score name="star" value="1"/>
<search_score name="homologyscore" value="40.47"/>
<search_score name="expect" value="12.16"/>
<analysis_result analysis="peptideprophet">
<peptideprophet_result probability="0.0695" all_ntt_prob="(0.0000,0.0006,0.0695)
">
<search_score_summary>
<parameter name="fval" value="-1.6228"/>
<parameter name="ntt" value="2"/>
<parameter name="nmc" value="0"/>
<parameter name="massd" value="-0.126"/>
</search_score_summary>
</peptideprophet_result>
</analysis_result>
	 */
	
	/**
	 * Process peptideprophet-assigned score into member variables
	 */
	public void record_peptideprophet_scores(XMLStreamReader xmlStreamReader) {
		String attrName = null;
		String attrValue= null;
		String name = null;
		String val = null;
		
		for (int i=0; i<xmlStreamReader.getAttributeCount(); i++) {
			attrName = xmlStreamReader.getAttributeLocalName(i);
			attrValue= xmlStreamReader.getAttributeValue(i);
			
			if (attrName.equals("name")) {
				name = attrValue.trim().toLowerCase();
			} else if (attrName.equals("value")) {
				val = attrValue;
			}
		}
		
		if (name != null && val != null) { 
			if (name.equals("ntt")) {
				m_pp_ntt = Double.parseDouble(val);
			} else if (name.equals("nmc")) {
				m_pp_nmc = Double.parseDouble(val);
			} else if (name.equals("massd")) {
				m_pp_massd = Double.parseDouble(val);
			} else if (name.equals("fval")) {
				m_pp_fval = Double.parseDouble(val);
			}
		}
	}
	
	/*
	 * Function parses amino acid modifications into aaMods variable
	 */
	public void record_AA_mod(XMLStreamReader xmlStreamReader) {
		String attrName = null;
		String attrValue = null;
		int k = -1;
		int v = 0;

		if (this.aaMods == null)
			this.aaMods = new HashMap<Integer, Integer>();

		for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
			attrName = xmlStreamReader.getAttributeLocalName(i);
			attrValue = xmlStreamReader.getAttributeValue(i);

			if (attrName.equals("position"))
				k = Integer.parseInt(attrValue) - 1;
			if (attrName.equals("mass")) {
				v = (int) Math.round(Double.parseDouble(attrValue));

				if (k > -1 && v > 0)
					this.aaMods.put(k, v);
				else {
					System.err.printf("\nERROR: mod_aminoacid_mass line pepXML::record_AA_mod()\n");
					System.err.println(this.specId + "\n");
					System.exit(-1);
				}
			}
		}
	}

	/*
	 * Function parses search_score lines
	 */
	public void parse_search_score_line(XMLStreamReader xmlStreamReader) {
		String attrValue = null;

		for (int i = 0, j = 1; i < xmlStreamReader.getAttributeCount(); i++, j++) {
			attrValue = xmlStreamReader.getAttributeValue(i);

			/*
			 *   X!Tandem search scores
			 */
			if (attrValue.equals("hyperscore")) {
				globals.addSearchEngine("XTANDEM");
				this.hyperscore = Double.parseDouble(xmlStreamReader
						.getAttributeValue(j));
			}
			if (attrValue.equals("nextscore"))
				this.nextscore = Double.parseDouble(xmlStreamReader
						.getAttributeValue(j));
			if (attrValue.equals("expect"))
				this.xtandem_expect = Double.parseDouble(xmlStreamReader
						.getAttributeValue(j));
			
			
			/*
			 *   Mascot search scores
			 */
			if (attrValue.equals("ionscore")) {
				globals.addSearchEngine("MASCOT");
				this.mascot_ionscore = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			}
			if (attrValue.equals("identityscore"))
				this.mascot_identityscore = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			if (attrValue.equals("star"))
				this.mascot_star = Integer.parseInt(xmlStreamReader.
						getAttributeValue(j));
			if (attrValue.equals("homologyscore")) 
				this.mascot_homologyscore = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			if (attrValue.equals("expect"))
				this.mascot_expect = Double.parseDouble(xmlStreamReader
						.getAttributeValue(j));
			
			
			/*
			 *   Sequest search scores
			 */
			if (attrValue.equals("xcorr")) {
				globals.addSearchEngine("SEQUEST");
				this.sequest_xcorr = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			}
			if (attrValue.equals("deltacn"))
				this.sequest_deltacn = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			if (attrValue.equals("deltacnstar"))
				this.sequest_deltacnstar = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			if (attrValue.equals("spscore"))
				this.sequest_spscore = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
			if (attrValue.equals("sprank"))
				this.sequest_sprank = Double.parseDouble(xmlStreamReader.
						getAttributeValue(j));
		}
	}

	
	/*
	 * Function parses out peptide probability
	 */
	public void record_iniProb(XMLStreamReader xmlStreamReader) {
		String attrName = null;
		String attrValue = null;

		for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
			attrName = xmlStreamReader.getAttributeLocalName(i);
			attrValue = xmlStreamReader.getAttributeValue(i);

			if (attrName.equals("probability"))
				this.iniProb = Double.parseDouble(attrValue);
		}
	}

	/*
	 * Function annotates modPeptide
	 */
	public void annotate_modPeptide() {
		if (this.aaMods == null)
			this.modPeptide = this.peptide; // peptide has no modifications
		else {
			modPeptide = "";
			for (int i = 0; i < this.peptide.length(); i++) {
				this.modPeptide += this.peptide.charAt(i);
				if (this.aaMods.containsKey(i))
					this.modPeptide += "[" + this.aaMods.get(i) + "]";
			}
		}
	}

	public boolean hasProteinDescr() {
		return (m_protein_descr != null);
	}
}
