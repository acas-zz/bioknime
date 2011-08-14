package au.com.acpfg.misc.uniprot;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * Records the essential state for a uniprot record (at this time only retrieval tasks are cache but this may change in future)
 * 
 * @author andrew.cassin
 *
 */
public class CacheableUniProtRecord implements Serializable {
	/**
	 * for Serializable
	 */
	private static final long serialVersionUID = 7136109438552699967L;

	public enum UniProtDatabase { UNIPROT_KB, UNIPARC, UNIREF, UNKNOWN };

	private UniProtDatabase m_db;
	private String m_accsn;		// primary key
	private String m_xml;			// complete (usually well-formed, but not guaranteed) XML
	
	public CacheableUniProtRecord(UniProtDatabase db, String accsn, String xml) {
		m_db             = db;
		m_accsn          = accsn;
		m_xml            = xml;
	}
	
	public final static String makeKey(UniProtDatabase db, String accsn) {
		return ""+db+"-"+accsn;
	}
	
	public final String getKey() {
		return makeKey(m_db, m_accsn);
	}
	
	public final UniProtDatabase getUniProtDB() {
		return m_db;
	}
	
	public final String getAccession() {
		return m_accsn;
	}
	
	public final String getXML() {
		return m_xml;
	}
	
	/********************************* SERIALIZABLE INTERFACE METHODS ******************************/
	 protected void writeObject(java.io.ObjectOutputStream out) throws IOException {
		 out.writeUTF(m_db.toString());
		 out.writeUTF(m_accsn);
		 out.writeUTF(m_xml);
	 }
     
     protected void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    	 String enum_const = in.readUTF();
    	 m_db    = UniProtDatabase.valueOf(enum_const);
    	 m_accsn = in.readUTF();
    	 m_xml   = in.readUTF();
     }
 
}
