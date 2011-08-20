package au.com.acpfg.io.genbank.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 * Responsible for storage of all the state from the input file as scanned during the execute method.
 * Probably not a thread-safe implementation, improvements welcome...
 * 
 * @author andrew.cassin
 *
 */
public class GenbankRecord {
	// locus header fields
	private String m_locus_name;
	private String m_locus_length;
	private String m_molecule_type;
	private String m_last_modified;
	private String m_category;
	private String m_definition;
	private String m_taxonomy;		// and lineage
	private String m_sequence;
	private String m_comment;
	private String m_accession;		// from ACCESSION section
	private String m_version;
	
	// regular expressions for key parts of each record
	private final static Pattern splitter = Pattern.compile("[\\r\\n]([A-Z]+)\\s", Pattern.MULTILINE);
	private final static Pattern locus_match = Pattern.compile("^LOCUS\\s+(\\w+)\\s+(\\d+\\s+[a-z]+)\\s+(\\w+)\\s+(.*?)\\s+(\\S+)\\s*$", Pattern.MULTILINE);
	
	/**
	 * Processes the string record (in multi-line genbank format) from the file into the member
	 * variables. Code here must be fast rather than pretty since genbank is pretty large ;-)
	 * Code here can assume the first line begins with 'LOCUS' and the record terminator (//) has been removed
	 * but should make no other assumptions about the record
	 * 
	 * @param rec
	 * @throws InvalidGenbankRecordException
	 */
	public GenbankRecord(StringBuffer rec_sb, GenbankFeatureListener l) throws InvalidGenbankRecordException {
		String rec = rec_sb.toString();
		String locus_tok[] = rec.substring(0, rec.indexOf('\n')).split("\\s+");
		if (locus_tok.length < 7) {
			throw new InvalidGenbankRecordException("Cannot match locus: <"+rec.substring(0, 80)+">");
		} else {
			set_locus_name(locus_tok[1]);
			if (locus_tok[3].equals("aa") || locus_tok[3].equals("bp")) {
				set_locus_length(locus_tok[2]+locus_tok[3]);
			} else {
				set_locus_length(locus_tok[2]);
			}
			set_molecule_type(locus_tok[4]+' '+locus_tok[5]);
			set_division(locus_tok[6]);
			set_last_modified(locus_tok[locus_tok.length-1]);
		}
		Matcher m = splitter.matcher(rec);
		while (m.find()) {
			String tag = m.group(1);
			int end_of_tag = m.end(1);
			if (tag.equals("DEFINITION")) {
				int end_tag = find_end_tag(rec, m.start(1));
				if (end_tag > 0) 
					set_definition(rec.substring(end_of_tag, end_tag));
			} else if (tag.equals("SOURCE")) {
				int end_tag = find_end_tag(rec, m.start(1));
				if (end_tag > 0)
					set_taxonomy(rec.substring(end_of_tag, end_tag));
			} else if (tag.equals("ORIGIN")) {
				int end_tag = find_end_tag(rec, m.start(1));
				if (end_tag > 0)
					set_sequence(rec.substring(end_of_tag, end_tag));
			} else if (tag.equals("COMMENT")) {
				int end_tag = find_end_tag(rec, m.start(1));
				if (end_tag > 0) {
					set_comment(rec.substring(end_of_tag, end_tag));
				}
			} else if (tag.equals("ACCESSION")) {
				int end_tag = find_end_tag(rec, m.start(1));
				if (end_tag > 0) {
					set_accession(rec.substring(end_of_tag, end_tag).trim());
				}
			} else if (tag.equals("VERSION")) {
				int end_tag = find_end_tag(rec, m.start(1));
				if (end_tag > 0) {
					set_version(rec.substring(end_of_tag, end_tag).trim());
				}
			} else if (tag.equals("FEATURES")) {
				if (l != null)
					process_features(rec, end_of_tag, find_end_tag(rec, m.start(1)), l);
			} else {
				// BUG: silent ignore for now...
			}
		}
	}
	

	/**
	 * Need to be fast here
	 * This method must invoke the listener once for each sub-section eg. source/CDS/...
	 * 
	 * @param rec
	 * @param end_of_tag
	 * @param find_end_tag
	 * @throws InvalidGenbankRecordException
	 */
	private void process_features(String rec, int end_of_tag, int find_end_tag, GenbankFeatureListener l) throws InvalidGenbankRecordException {
		assert(end_of_tag < find_end_tag && find_end_tag > 0);
		String feature_section = rec.substring(end_of_tag, find_end_tag);
		final int feature_len = feature_section.length();
		int offset = 0; 
		SectionStart first = null;
		SectionStart last  = null;
		
		while ((offset = feature_section.indexOf('\n', offset)) >= 0) {
			int n_spaces = 0;
			
			// find a series of whitespaces followed by a word
			while (offset < feature_len && Character.isWhitespace(feature_section.charAt(offset))) {
				n_spaces++;
				offset++;
			}

			if (n_spaces > 0 && n_spaces < 10) {		// probably a start of a sub-section
				StringBuffer tag = new StringBuffer();
				while (offset < feature_len && Character.isLetter(feature_section.charAt(offset))) {
					tag.append(feature_section.charAt(offset));
					offset++;
				}
				String key = tag.toString().toLowerCase();
				SectionStart ss = new SectionStart(key, offset, -1, null);
				// keep the headings as a forward-only linked list
				if (first == null)
					first = ss;
				if (last != null) {
					last.set_len(offset - last.get_start() - key.length());
					last.set_next(ss);
				}
				last = ss;
			}
			offset++;
		}
		// this is not setup by the above code, so...
		last.set_len(feature_len - last.get_start());
		
		// now invoke the listener with the results of the parse
		for (SectionStart ss = first; ss != null; ss = ss.get_next()) {
			int start = ss.get_start();
			l.parse_section(ss.get_title(), get_accession(), feature_section.substring(start,  start+ss.get_len()));
		}
		
	}


	/** 
	 * Responsible for quickly computing where the current tag ends (character offset in <code>rec</code>)
	 * the current section, based on the supplied record. In other words looks for the next tag or the 
	 * end of the record and returns the minimum of the two
	 * 
	 * @param rec
	 * @param start
	 * @return
	 */
	private int find_end_tag(String rec, int start) {
		// a section might look like this:
		// DESCRIPTION .... blah blah blah...
		//             more comments here
		// NEXT TAG
		// we must compute the position of NEXT TAG or the end of the record
		int end = start;
		int len = rec.length();
		while (end < len) {
			end  = rec.indexOf('\n', end);
			if ((end < 0) || (end+1 >= len)) {
				return len;			// end of record is the end of the section
			} else {
				char c = rec.charAt(end+1);
				if (Character.isLetter(c) && Character.isUpperCase(c)) {	// new section starting?
					return end;
				}
				end = end+1;
			}
		}
		
		return len;
	}

	public void set_locus_name(String m_locus_name) {
		this.m_locus_name = m_locus_name;
	}

	public String get_locus_name() {
		return m_locus_name;
	}

	public void set_locus_length(String m_locus_length) {
		this.m_locus_length = m_locus_length;
	}

	public String get_locus_length() {
		return m_locus_length;
	}

	public void set_molecule_type(String m_molecule_type) {
		this.m_molecule_type = m_molecule_type;
	}

	public String get_molecule_type() {
		return m_molecule_type;
	}

	public void set_last_modified(String m_last_modified) {
		this.m_last_modified = m_last_modified;
	}

	public String get_last_modified() {
		return m_last_modified;
	}

	public void set_division(String m_category) {
		this.m_category = m_category;
	}

	public String get_division() {
		return m_category;
	}


	public void set_definition(String m_definition) {
		this.m_definition = m_definition;
	}


	public String get_definition() {
		return m_definition;
	}


	public void set_taxonomy(String m_taxonomy) {
		this.m_taxonomy = m_taxonomy;
	}


	public String get_taxonomy() {
		return m_taxonomy;
	}


	public void set_sequence(String m_sequence) {
		this.m_sequence = m_sequence;
	}

	/** 
	 * Returns the raw "unedited" sequence data from the record
	 * @return
	 */
	public String get_sequence() {
		return m_sequence;
	}
	
	/**
	 * Returns only residues comprising the sequence. No case conversion is performed.
	 * See <code>get_sequence()</code> for the getting at the raw "origin" entry in the genbank record
	 * 
	 * @return null if there is no sequence in the genbank record, residues only otherwise
	 */
	public String get_filtered_sequence() {
		StringBuffer ret = new StringBuffer(1024);
		if (m_sequence == null)
			return null;
		for (int i=0; i<m_sequence.length(); i++) {
			char c = m_sequence.charAt(i);
			if (Character.isLetter(c)) 
				ret.append(c);
		}
		return ret.toString();
	}


	public void set_comment(String m_comment) {
		this.m_comment = m_comment;
	}


	public String get_comment() {
		return m_comment;
	}


	public void set_accession(String m_accession) {
		this.m_accession = m_accession;
	}


	public String get_accession() {
		return m_accession;
	}


	public void set_version(String m_version) {
		this.m_version = m_version;
	}


	public String get_version() {
		return m_version;
	}
}
