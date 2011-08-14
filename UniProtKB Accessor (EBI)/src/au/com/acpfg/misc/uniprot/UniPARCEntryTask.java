package au.com.acpfg.misc.uniprot;

import java.io.InputStream;
import java.util.logging.Logger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.xml.sax.helpers.XMLReaderFactory;

import au.com.acpfg.xml.reader.XMLCell;

import com.fatdog.xmlEngine.ResultList;
import com.fatdog.xmlEngine.XQEngine;

public class UniPARCEntryTask extends RetrieveEntryTask {
	private final int NUM_COLS = 4;	// 5 if want_xml
	private boolean m_want_xml = false;
	
	public UniPARCEntryTask(UniProtAccessorNodeModel m, String db) {
		super(m, db);
	}
	
	@Override
	public DataTableSpec getTableSpec(boolean want_xml) {
		int ncols = NUM_COLS;
		m_want_xml = want_xml;
		if (m_want_xml)
			ncols++;
		DataColumnSpec[] cols = new DataColumnSpec[ncols];
		DataType dt = ListCell.getCollectionType(StringCell.TYPE);

		cols[0] = new DataColumnSpecCreator("UniPARC: Reference Count", IntCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("UniPARC: References", dt).createSpec();
		cols[2] = new DataColumnSpecCreator("UniPARC: Sequence", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("UniPARC: Copyright", StringCell.TYPE).createSpec();
		if (m_want_xml) 
			cols[4] = new DataColumnSpecCreator("XML", XMLCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override 
	public String fix_accsn(String in) throws Exception {
		String tmp = in.trim().toUpperCase();
		if (!tmp.startsWith("UPI")) 
			throw new Exception("Invalid UniPARC accession ("+tmp+") - must begin with 'UPI'");
		return tmp;
	}
	
	@Override
	protected DataCell[] grok_entry(String xml) throws Exception {		
		int ncols = NUM_COLS;
		if (m_want_xml) 
			ncols++;
		DataCell[] cells = new DataCell[ncols];
		XQEngine eng = new XQEngine();
		eng.setXMLReader(XMLReaderFactory.createXMLReader());
		eng.setExplicitDocument(xml);
		ResultList rl = eng.setQuery("count( /uniparc/entry/dbReference[@last] )");
		cells[0] = new IntCell(rl.evaluateAsInteger());
		rl = eng.setQuery("//dbReference[@id]");
		cells[1] = list2listcell(UniProtHit.extract_attribute_combined_key(rl.emitXml(), new String[] {"type", "id"}));
		rl = eng.setQuery("/uniparc/entry/sequence/text()");
		cells[2] = new StringCell(rl.asString());
		rl = eng.setQuery("/uniparc/copyright/text()");
		cells[3] = new StringCell(rl.asString());
		if (m_want_xml)
			cells[4] = new XMLCell(xml);
		return cells;
	}

}
