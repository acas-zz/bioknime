package au.com.acpfg.misc.uniprot;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.xml.sax.helpers.XMLReaderFactory;

import au.com.acpfg.xml.reader.XMLCell;

import com.fatdog.xmlEngine.ResultList;
import com.fatdog.xmlEngine.XQEngine;

public class UniRefEntryTask extends RetrieveEntryTask {
	private final int NUM_COLUMNS = 6;
	
	
	public UniRefEntryTask(UniProtAccessorNodeModel m, String db) {
		super(m, "/uniref/"+db+"_");
	}
	
	@Override
	public DataTableSpec getTableSpec(boolean want_xml) {
		DataColumnSpec[] cols = new DataColumnSpec[NUM_COLUMNS];
		DataType dt = ListCell.getCollectionType(StringCell.TYPE);

		cols[0] = new DataColumnSpecCreator("UniRef Member Count", IntCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("UniRef Common Taxon", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("UniRef Member UniProtKB Accessions", dt).createSpec();
		cols[3] = new DataColumnSpecCreator("UniRef Member Proteins", dt).createSpec();
		cols[4] = new DataColumnSpecCreator("UniRef Source Organisms", dt).createSpec();
		cols[5] = new DataColumnSpecCreator("UniRef XML Output", XMLCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}
	
	@Override
	protected DataCell[] grok_entry(String xml) throws Exception {		
		DataCell[] cells = new DataCell[NUM_COLUMNS];
		XQEngine eng = new XQEngine();
		eng.setXMLReader(XMLReaderFactory.createXMLReader());
		//Logger.getAnonymousLogger().info("XML is: "+xml);
		eng.setExplicitDocument(xml);
		ResultList rl = eng.setQuery("//property[@type='member count']");
		if (rl.getNumTotalItems() == 1) {
			Integer mem_cnt = new Integer(UniProtHit.extract_attribute(rl.emitXml(), "value"));
			cells[0] = new IntCell(mem_cnt.intValue());
		} else {
			cells[0] = DataType.getMissingCell();
		}
		rl = eng.setQuery("//property[@type='common taxon']");
		if (rl.getNumTotalItems() == 1) {
			cells[1] = safe_string(UniProtHit.extract_attribute(rl.emitXml(), "value"));
		} else {
			cells[1] = DataType.getMissingCell();
		}
		
		List<String> accsns = add_members(eng, "UniProtKB accession");
		cells[2] = list2listcell(accsns);
		List<String> proteins = add_members(eng, "protein name");
		cells[3] = list2listcell(proteins);
		List<String> organisms= add_members(eng, "source organism");
		cells[4] = list2listcell(organisms);
		cells[5] = new XMLCell(xml);
		return cells;
	}
	
	protected List<String> add_members(XQEngine eng, String attName) throws Exception {
		ResultList rl = eng.setQuery("/UniRef/entry/member/dbReference/property[@type='"+attName+"']");
		String xml = rl.emitXml();
		//Logger.getAnonymousLogger().info(xml);
		String[] members = xml.split("/>");
		ArrayList<String> ret = new ArrayList<String>();
		for (String member : members) {
			if (member.trim().length() > 0) {
				ret.add(UniProtHit.extract_attribute(member, "value"));
			}
		}
		
		// representative member data MUST always be first in results
		rl = eng.setQuery("/UniRef/entry/representativeMember/dbReference/property[@type='"+attName+"']");
		ret.add(0, UniProtHit.extract_attribute(rl.emitXml(), "value"));
		return ret;
	}
}
