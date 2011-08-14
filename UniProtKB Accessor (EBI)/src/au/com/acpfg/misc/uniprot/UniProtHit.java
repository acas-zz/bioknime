package au.com.acpfg.misc.uniprot;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.fatdog.xmlEngine.DocItems;
import com.fatdog.xmlEngine.ResultList;
import com.fatdog.xmlEngine.XQEngine;
import com.fatdog.xmlEngine.exceptions.CantParseDocumentException;
import com.fatdog.xmlEngine.exceptions.InvalidQueryException;
import com.fatdog.xmlEngine.exceptions.MissingOrInvalidSaxParserException;

public class UniProtHit {
	private String m_recommended_name, m_organism, m_primary_gene, m_id, m_sequence, m_exist_evidence;
	private ArrayList<String> m_xrefs;	  // cross references to other databases
	private ArrayList<String> m_comments; // textual comments relating to entry (usually unspecified origin)
	private ArrayList<String> m_lineage;  // NCBI taxonomic lineage
	private ArrayList<String> m_keywords;
	private ArrayList<String> m_features;
	private ArrayList<String> m_citations;
	
	public UniProtHit() {
		m_xrefs = new ArrayList<String>();
		m_comments = new ArrayList<String>();
		m_lineage = new ArrayList<String>();
		m_keywords= new ArrayList<String>();
		m_features= new ArrayList<String>();
		m_citations= new ArrayList<String>();
	}
	
	/*
	public static List<UniProtHit> make_entries(InputStream xml_stream) throws Exception {
		return make_entries(xml2string(xml_stream, true));
	}*/
	
	public static List<UniProtHit> make_entries(String xml) throws Exception {
		ArrayList<UniProtHit> ret = new ArrayList<UniProtHit>();
		XQEngine  results_eng = new XQEngine();
		
		XMLReader xml_rdr = XMLReaderFactory.createXMLReader();
	
		results_eng.setXMLReader(xml_rdr);
		//Logger.getAnonymousLogger().info(xml);
		if (xml.trim().length() < 1) {
			// deleted entries have an XML length of 0 eg. http://www.uniprot.org/uniprot/A7PGM7.xml
			// so we throw and let the caller sort out what it wants to do...
			throw new DeletedEntryException("UniProt entry appears to have been deleted, no data available.");
		}
		int doc_id = results_eng.setExplicitDocument(xml);
		ResultList results = results_eng.setQuery("for $e in /uniprot/entry return <uniprot>{$e}</uniprot>");
		
		String hits_xml = results.emitXml();
		if (hits_xml != null && hits_xml.length() > 0) {
			// HACK: split the xml based on the end of the root tag and then run the fragments thru XQEngine to extract desired results
			String[] hits = hits_xml.split("</uniprot>");
			for (String hit : hits) {
				hit += "</uniprot>";
				XQEngine hit_eng = new XQEngine();
				hit_eng.setXMLReader(xml_rdr);
				hit_eng.setExplicitDocument(hit);
				
				UniProtHit uh = new UniProtHit();
				
				ResultList rl = hit_eng.setQuery("/uniprot/entry/protein/recommendedName/fullName/text()");
				uh.setRecommendedName(rl.emitXml());
				
				rl = hit_eng.setQuery("/uniprot/entry/organism/name[@type='scientific']/text()");
				uh.setOrganism(rl.asString());
				
				rl = hit_eng.setQuery("/uniprot/entry/name");
				uh.setName(rl.asString());
				
				rl = hit_eng.setQuery("/uniprot/entry/sequence");
				uh.setSequence(rl.asString());
				
				rl = hit_eng.setQuery("/uniprot/entry/gene/name[@type='primary']/text()");
				uh.setPrimaryGene(rl.asString());
				
				rl = hit_eng.setQuery("//uniprot/entry/dbReference");
				//Logger.getAnonymousLogger().info(rl.emitXml());
				uh.setXrefs(extract_attribute_combined_key(rl.emitXml(), new String[] { "type", "id" }));
			
				rl = hit_eng.setQuery("//comment/text");
				uh.setComments(extract_elements(rl.emitXml(), "text" ));
				
				rl = hit_eng.setQuery("/uniprot/entry/organism/lineage/taxon");
				uh.setLineage(extract_elements(rl.emitXml(), "taxon"));
				
				rl = hit_eng.setQuery("/uniprot/entry/keyword");
				uh.setKeywords(extract_elements(rl.emitXml(), "keyword"));
				
				rl = hit_eng.setQuery("/uniprot/entry/proteinExistence");
				uh.setExistEvidence(extract_attribute(rl.emitXml(), "type"));
				
				rl = hit_eng.setQuery("/uniprot/entry/feature");
				uh.extract_features(rl.emitXml());
				
				uh.extract_citations(hit_eng);
				ret.add(uh);
			}
		}
		
		return ret;
	}

	private void setExistEvidence(String extract_attribute) {
		m_exist_evidence = extract_attribute;
	}

	private void setKeywords(ArrayList<String> extract_elements) {
		m_keywords = extract_elements;
	}

	private void setLineage(ArrayList<String> extract_elements) {
		m_lineage = extract_elements;
	}

	private void setComments(ArrayList<String> extract_elements) {
		m_comments = extract_elements;
	}

	private void setXrefs(ArrayList<String> elements) {
		m_xrefs = elements;
	}

	private void setPrimaryGene(String asString) {
		m_primary_gene = asString;
	}

	private void setName(String asString) {
		m_id = asString;
	}

	private void setSequence(String asString) {
		m_sequence = asString;
	}

	private void setOrganism(String asString) {
		m_organism = asString;
	}

	private void setRecommendedName(String asString) {
		m_recommended_name = asString;
	}

	public static String xml2string(InputStream response_stream, boolean remove_default_ns) throws SAXException, IOException {
		String xml = "";
		Reader rdr = new InputStreamReader(response_stream);
		int n_chars;
		char[] buf = new char[10*1024];
		boolean first = true;
		int got = 0;
		try {
			while ((n_chars = rdr.read(buf)) >= 0) {
				//Logger.getAnonymousLogger().info("read "+n_chars);
				got += n_chars;
				if (got > 100 * 1024 * 1024) {
					throw new IOException("Ridiculously large UniProt record (>100MB)... ignoring!");
				}
				if (n_chars > 0) {
					char[] read = new char[n_chars];
					System.arraycopy(buf, 0, read, 0, n_chars);
					if (first && remove_default_ns) {
						// remove default namespace (simplifies XQueries) since only one is in results
						Pattern p = Pattern.compile("\\sxmlns=\"[^\"]*?\"");
						String  s = new String(read);
						Matcher m = p.matcher(s);
						
						xml += m.replaceFirst(" ");
					} else {
						xml += new String(read);
					}
				}
			}
		} finally {
			// BUG: seems to occassionally socket timeout (trying to exhaust input) so ignore it...
			try {
				rdr.close();
			} catch (Exception e) {
				// NO-OP
			}
		}
		return xml;
	}
	
	protected String extract_text(String xml_fragment) {
		Pattern p = Pattern.compile("<(\\w+)>([^>]+?)(?:</\\1>)?");
		Matcher m = p.matcher(xml_fragment);
		if (m.matches()) {
			return m.group(2);
		} else {
			return "";
		}
	}
	
	protected void extract_citations(XQEngine eng) throws Exception {
		m_citations.clear();
		ResultList rl = eng.setQuery("/uniprot/entry/reference/citation");
		String[] citations = rl.emitXml().split("</citation>");
		for (String citation : citations) {
			ArrayList<String> title = extract_elements(citation, "title");
			ArrayList<String> authors= extract_elements(citation, "person");
			String citation_tag = citation.substring(0, citation.indexOf(">"));
			String type = extract_attribute(citation_tag, "type");
			String date = extract_attribute(citation_tag, "date");
			String name = extract_attribute(citation_tag, "name");
			String start= extract_attribute(citation_tag, "first");
			String end  = extract_attribute(citation_tag, "last");
			String vol  = extract_attribute(citation_tag, "volume");
			
			if (type == null || title == null || title.size() < 1) {
				Logger.getAnonymousLogger().warning("Rejecting incomplete citation");
			}
			String out = type+": "+extract_text(title.get(0))+", ";
			if (authors != null && authors.size() > 0) {
				for (String person_xml : authors) {
					String person = extract_attribute(person_xml, "name");
					out += person+" ";
				}
				out = out.trim()+", ";
			}
			if (name != null) {
				out += name+", ";
			}
			if (date != null) {
				out += date+", ";
			}
			if (vol != null) {
				if (!vol.toLowerCase().startsWith("vol"))
					out += "Vol. ";
				out += vol+" ";
				if (start != null && end != null) {
					out += start + "-" + end;
				}
			}
			m_citations.add(out);
		}
	}
	
	protected void extract_features(String xml) {
		m_features.clear();
		String[]   features = xml.split("</feature>");
		ArrayList<String> f = new ArrayList<String>();
		for (String feature : features) {
			String type = extract_attribute(feature, "type");
			String descr= extract_attribute(feature, "description");
			String status=extract_attribute(feature, "status");
			
			String tmp = type + ":";
			if (descr != null) {
				tmp += " "+descr.trim();
				if (status != null) {
					tmp += ":";
				}
			}
			if (status != null) {
				tmp += status;
			}
			
			m_features.add(tmp);
		}
	}
	
	protected static String extract_attribute(String xml, String attrName) {
		int offset = xml.indexOf(" "+attrName+"=\"");
		if (offset < 0)
			return null;
		int end = offset + 3 + attrName.length();
		while (end < xml.length() && xml.charAt(end) != '"') {
			end++;
		}
		return xml.substring(offset+3+attrName.length(), end);
	}
	
	protected static ArrayList<String> extract_attributes(String xml, String elName, String attrName) {
		ArrayList<String> ret = new ArrayList<String>();
		for (String el : xml.split("</"+elName+">")) {
			if (el.trim().length() > 0) {
				ret.add(extract_attribute(el, attrName));
			}
		}
		return ret;
	}
	
	protected static ArrayList<String> extract_elements(String xml_fragment, String elName) {
		ArrayList<String> ret = new ArrayList<String>();
		String[] entries = xml_fragment.split("</"+elName+">");
		for (String entry : entries) {
			// skip start tag
			int offset = elName.length()+1;
			char c = ' ';
			while (offset < entry.length() && (c = entry.charAt(offset)) != '>') {
				offset++;
			}
			if (c == '>') {
				ret.add(entry.substring(offset+1));
			}
		}
		return ret;
	}
	
	protected static ArrayList<String> extract_attribute_combined_key(String xml_fragment, String[] attrNames) {
		ArrayList<String> ret = new ArrayList<String>();
		String[] lines = xml_fragment.split("/><");
		// only lines which contain ALL the attributes are added to ret
		for (String line : lines) {
			String xref = "";
			int added  = 0;
			for (String attrName : attrNames) {
				int offset = line.indexOf(attrName+"=\"");
				if ( offset >= 0) {
					int close_quote = offset + attrName.length()+3;
					boolean found = false;
					while (close_quote < line.length() && !found ) {
						found = (line.charAt(close_quote) == '"');
						close_quote++;
					}
					if (found) {
						xref += line.substring(offset+attrName.length()+2, close_quote-1);
						xref += ":";
						added++;
					}
				}
			}
			if (added == attrNames.length) {
				// remove trailing :
				ret.add(xref.substring(0, xref.length()-1));
			}
		}
		return ret;
	}

	public String getRecommendedName() {
		return m_recommended_name;
	}
	
	public String getOrganism() {
		return m_organism;
	}
	
	public String getID() {
		return m_id;
	}
	
	public String getSequence() {
		return m_sequence;
	}
	
	public String getGenePrimary() {
		return m_primary_gene;
	}
	
	public List<String> getXrefs() {
		return m_xrefs;
	}
	
	public List<String> getComments() {
		return m_comments;
	}
	
	public List<String> getLineage() {
		return m_lineage;
	}
	
	public List<String> getKeywords() {
		return m_keywords;
	}
	
	public String getExistenceEvidence() {
		return m_exist_evidence;
	}
	
	public List<String> getFeatures() {
		return m_features;
	}
	
	public List<String> getCitations() {
		return m_citations;
	}
}
