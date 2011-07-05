package au.com.acpfg.xml.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.def.StringCell;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;


public class XMLCell extends DataCell {
	/**
	 * Serial ID
	 */
	private static final long serialVersionUID = -8748457485351154080L;
	
	/**
	 * internal state which must be serialised
	 */
	private boolean m_is_fragment;
	private boolean m_is_well_formed;
	private boolean m_is_ref;
	private String  m_xml; // overloaded field: either a filename (large XML) or content otherwise
	
	/**
	 * Maximum XML size to store in the cell, otherwise the cell keeps a reference and loads the file
	 * on demand. That is the only practical way to keep track of 100MB XML files where the in-core tree is larger
	 * due to overheads.
	 */
	public static final long MAX_INCORE_XML_SIZE = (50 * 1024);		// 50KB
	/**
	 * The maximum number of lines which will be displayed in an reference XMLCell, 
	 * does NOT apply for XMLCell's where !m_is_ref
	 */
	private static final int MAX_CELL_LINES = 10;		// first 10 lines in table cell with '...' to signify more to come
	
	/**
	 * Convenience method
	 */
    public static final DataType TYPE = DataType.getType(XMLCell.class);
    /**
     * Ensure correct instantiation for XML cells
     */
    public static final UtilityFactory UTILITY = new XMLUtilityFactory();
   
    
    public XMLCell(File input_file) throws IOException {
    	this(input_file, MAX_INCORE_XML_SIZE);
    }
    
    /**
     * Constructor which supports a per-object XML size to keep XML (string) in-core
     * @param input_file
     * @param max_xml_size
     * @throws IOException
     */
    public XMLCell(File input_file, long max_xml_size) throws IOException {
    	if (input_file.length() > max_xml_size) {
    		m_is_ref = true;
    		m_xml = input_file.getAbsolutePath();
    	}
    	
    	BufferedFileReader rdr = BufferedFileReader.createNewReader(new FileInputStream(input_file));
    	String line;
    	StringBuffer sb = new StringBuffer((int) input_file.length());
    	while ((line = rdr.readLine()) != null) {
    		sb.append(line);
    	}
    	m_xml = sb.toString();
    }
    
	public XMLCell(String xml, boolean is_frag, boolean is_well_formed) {
		super();
		m_xml            = xml;
		m_is_fragment    = is_frag;		    // only part of a document ie. no XML declaration?
		m_is_well_formed = is_well_formed;	// syntactically valid XML without ANY modifications?
		m_is_ref         = false;
	}
	
	public XMLCell(String xml, boolean is_fragment) {
		this(xml, is_fragment, false);
	}
	
	public XMLCell(String xml) {
		this(xml, true, false);
	}
	
	@Override
	protected boolean equalsDataCell(DataCell dc) {
		return (this == dc);
	}

	
	@Override
	public int hashCode() {
		return m_xml.hashCode();
	}

	public boolean isReference() {
		return m_is_ref;
	}
	
	/**
	 * Returns a file reference to the XML content within this cell. If the amount of
	 * XML is large, this will be a reference to the data source - but callers must not rely on this behavior.
	 * 
	 * @return
	 * @throws IOException
	 * @throws SAXException if the XML is not well-formed 
	 */
	public File asFile() throws IOException, SAXException {
		if (m_is_ref) {
			return new File(m_xml); 
		}
		
		// small XML documents are not kept by reference... so...
		File temp_file = java.io.File.createTempFile("xml-temp", ".xml");
		PrintWriter pw = null;
		try {
			OutputStream os = new FileOutputStream(temp_file);
			pw = new PrintWriter(os);
			pw.print(m_xml);
			pw.close();
		} catch (IOException e) {
			if (pw != null)
				pw.close();
			throw e;
		}
		return temp_file;
	}
	
	
	/**
	 * This code does <em>NOT</em> obey the stripNamespaces property, it just returns a printable
	 * version of the XML instead. Use <code>asFile()</code> if you want to get the cell content
	 * with namespaces removed to aid in XQuery processing.
	 */
	@Override
	public String toString() {
		if (m_is_ref) {
			try {
				BufferedFileReader rdr = BufferedFileReader.createNewReader(new FileInputStream(new File(m_xml)));
				String line;
				int cnt = 0;
				StringBuffer sb = new StringBuffer();
				while ((line = rdr.readLine()) != null && cnt++ < MAX_CELL_LINES) {
					sb.append(line);
				}
				sb.append("...");
				rdr.close();
				return sb.toString();
			} catch (Exception e) {
				return "?";
			}
		}
		return m_xml;
	}

	/**
	 * Returns a SAXParser object which can parse XML according to the state of the cell.
	 * If the cell has NS stripping on, the return object will do this during its <code>parse()</code>
	 * 
	 * @return
	 * @throws SAXException
	 */
	public XMLReader getReader(boolean strip_ns) throws SAXException {
		XMLReader rf = XMLReaderFactory.createXMLReader();
		if (strip_ns) {
			return new MyNSRemover(rf);
		} else {
			return rf;
		}
	}

	
}
