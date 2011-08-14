package au.com.acpfg.misc.uniprot;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

public class AccessionMapTask implements UniProtTaskInterface {
	  private static final String base = "http://www.uniprot.org";
	  private String m_from_db, m_to_db;
	  private static  boolean m_warn_again = true;
	  // XML is not available for this service, so no support is required
	  
	  private static HashMap<String, String> m_db = new HashMap<String, String>();
	  static {
		  // only bi-directional maps are statically initialized, one-way maps are only available via get_db_list()
		  m_db.put("UniParc", "UPARC");
		  m_db.put("UniRef50", "NF50");
		  m_db.put("UniRef90", "NF90");
		  m_db.put("UniRef100", "NF100");
		  m_db.put("EMBL/GenBank/DDBJ", "EMBL_ID");
		  m_db.put("EMBL/GenBank/DDBJ CDS", "EMBL");
		  m_db.put("PIR", "PIR");
		  m_db.put("UniGene", "UNIGENE_ID");
		  m_db.put("Entrez Gene", "P_ENTREZ_GENEID");
		  m_db.put("GI number", "P_GI");
		  m_db.put("IPI", "P_IPI");
		  m_db.put("RefSeq", "P_REFSEQ_AC");
		  m_db.put("PDB", "PDB_ID");
		  m_db.put("DisProt", "DISPROT_ID");
		  m_db.put("HSSP", "HSSP_ID");
		  m_db.put("DIP", "DIP_ID");
		  m_db.put("MINT", "MINT_ID");
		  m_db.put("MEROPS", "MEROPS_ID");
		  m_db.put("PeroxiBase", "PEROXIBASE_ID");
		  m_db.put("PptaseDB", "PPTASEDB_ID");
		  m_db.put("REBASE", "REBASE_ID");
		  m_db.put("TCDB", "TCDB_ID");
		  m_db.put("Aarhus/Ghent-2DPAGE", "AARHUS_GHENT_2DPAGE_ID");
		  m_db.put("ECO2DBASE_ID", "ECO2DBASE_ID");
		  m_db.put("World-2DPAGE", "WORLD_2DPAGE_ID");
		  m_db.put("Ensembl", "ENSEMBL_ID");
		  m_db.put("Ensembl Protein", "ENSEMBL_PRO_ID");
		  m_db.put("Ensembl Transcript", "ENSEMBL_TRS_ID");
		  m_db.put("Ensemble Genomes", "ENSEMBLGENOME_ID");
		  m_db.put("Ensemble Genomes Protein", "ENSEMBLGENOME_PRO_ID");
		  m_db.put("Ensemble Genomes Transcript", "ENSEMBLEGENOME_TRS_ID");
		  m_db.put("GeneID", "P_ENTREZGENEID");
		  m_db.put("GenomeReviews", "GENOMEREVIEWS_ID");
		  m_db.put("KEGG", "KEGG_ID");
		  m_db.put("TIGR", "TIGR_ID");
		  m_db.put("UCSC", "UCSC_ID");
		  m_db.put("VectorBase", "VECTORBASE_ID");
		  m_db.put("AGD", "AGD_ID");
		  m_db.put("ArachnoServer", "ARACHNOSERVER_ID");
		  m_db.put("CGD", "CGD");
		  m_db.put("ConoServer", "CONOSERVER_ID");
		  m_db.put("CYGD", "CYGD_ID");
		  m_db.put("dictyBase", "DICTYBASE_ID");
		  m_db.put("EchoBASE", "ECHOBASE_ID");
		  m_db.put("EcoGene", "ECOGENE_ID");
		  m_db.put("euHCVdb", "EUHCVDB_ID");
		  m_db.put("EuPathDB", "EUPATHDB_ID");
		  m_db.put("FlyBase", "FLYBASE_ID");
		  m_db.put("GeneCards", "GENECARDS_ID");
		  m_db.put("GeneDB_Spombe", "GENEDB_SPOMBE_ID");
		  m_db.put("GeneFarm", "GENEFARM_ID");
		  m_db.put("GenoList", "GENOLIST_ID");
		  m_db.put("H-InvDB", "H_INVDB_ID");
		  m_db.put("HGNC", "HGNC_ID");
		  m_db.put("HPA", "HPA_ID");
		  m_db.put("LegioList", "LEGIOLIST_ID");
		  m_db.put("Leproma", "LEPROMA_ID");
		  m_db.put("MaizeGDB", "MAIZEGDB_ID");
		  m_db.put("MIM", "MIM_ID");
		  m_db.put("MGI", "MGI_ID");
		  m_db.put("NMPDR", "NMPDR_ID");
		  m_db.put("Orphanet", "ORPHANET_ID");
		  m_db.put("PharmGKB", "PHARMGKB_ID");
		  m_db.put("PseudoCAP", "PSEUDOCAP_ID");
		  m_db.put("RGD", "RGD_ID");
		  m_db.put("SGD", "SGD_ID");
		  m_db.put("TAIR", "TAIR_ID");
		  m_db.put("TubercuList", "TUBERCULIST_ID");
		  m_db.put("WormBase", "WORMBASE_ID");
		  m_db.put("WormBase Transcript", "WORMBASE_TRS_ID");
		  m_db.put("WormBase Protein", "WORMBASE_PRO_ID");
		  m_db.put("Xenbase", "XENBASE_ID");
		  m_db.put("ZFIN", "ZFIN_ID");
		  m_db.put("eggNOG", "EGGNOG_ID");
		  m_db.put("HOGENOM", "HOGENOM_ID");
		  m_db.put("HOVERGEN", "HOVERGEN_ID");
		  m_db.put("OMA", "OMA_ID");
		  m_db.put("OrthoDB", "ORTHODB_ID");
		  m_db.put("ProtClustDB", "PROTCLUSTDB_ID");
		  m_db.put("BioCyc", "BIOCYC_ID");
		  m_db.put("Reactome", "REACTOME_ID");
		  m_db.put("CleanEx", "CLEANEX_ID");
		  m_db.put("GermOnline", "GERMONLINE_ID");
		  m_db.put("DrugBank", "DRUGBANK_ID");
		  m_db.put("NextBio", "NEXTBIO_ID");
	  };
	  
	  public AccessionMapTask(String from_db, String to_db) throws Exception {
		  if (!from_db.equals("UniProtKB AC/ID") && !m_db.containsKey(from_db) ) {
			  throw new InvalidSettingsException("From database is not valid: "+from_db);
		  } else {
			  m_from_db = m_db.get(from_db);
			  if (m_from_db == null) {	// uniprot one-way database?
				  if (from_db.equals("UniProtKB AC/ID")) {
					  m_from_db = "ACC+ID";
				  } else {
					  throw new InvalidSettingsException("From database is not valid: "+from_db);
				  }
			  }
		  }
		  if (!to_db.startsWith("UniProt") && !m_db.containsKey(to_db)) {
			  throw new InvalidSettingsException("To database is not valid: "+to_db);
		  } else {
			  m_to_db   = m_db.get(to_db);
			  if (m_to_db == null) {
				  if (to_db.equals("UniProtKB AC")) {
					  m_to_db = "ACC";
				  } else if (to_db.equals("UniProtKB ID")) {
					  m_to_db = "ID";
				  } else {
					  throw new InvalidSettingsException("To database is not valid: "+to_db);
				  }
			  }
		  }
		  
		 
	  }
	  
	  public static List<String> get_db_list(boolean from_db) {
		  ArrayList<String> l = new ArrayList<String>();
		  l.addAll(m_db.keySet());
		  if (from_db) {
			  l.add("UniProtKB AC/ID");
		  } else {
			  l.add("UniProtKB AC");
			  l.add("UniProtKB ID");
		  }
		  Collections.sort(l);
		  return l;
	  }
	  
	  private String execute(String tool, NameValuePair[] params) throws Exception {
	    HttpClient client = new HttpClient();
	    
	    String location = base + '/' + tool + '/';
	    
	    try {
	    	HttpMethod method = new PostMethod(location);
	    	((PostMethod) method).addParameters(params);
	    	method.setFollowRedirects(false);
	  
	   
		    int status = client.executeMethod(method);
		    //Logger.getAnonymousLogger().info(HttpStatus.getStatusText(status));
		    
		    if (status == HttpStatus.SC_MOVED_TEMPORARILY)  {
		      location = method.getResponseHeader("Location").getValue();
		      method.releaseConnection();
		      method = new GetMethod(location);
		      status = client.executeMethod(method);
		    }
		    
		    while (true)
		    {
		      int wait = 0;
		      Header header = method.getResponseHeader("Retry-After");
		      if (header != null)
		        wait = Integer.valueOf(header.getValue());
		      if (wait == 0)
		        break;
		      Thread.sleep(wait * 1000);
		      method.releaseConnection();
		      method = new GetMethod(location);
		      status = client.executeMethod(method);
		    }
		    
		    if (status == HttpStatus.SC_OK) {
			      String ret = method.getResponseBodyAsString();
			      method.releaseConnection();
			      return ret;
		    }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	throw e;
	    }
	    return null;
	  }
	  
	
	@Override
	public DataTableSpec getTableSpec(boolean want_xml) {
		DataColumnSpec[] cols = new DataColumnSpec[2];
		cols[1] = new DataColumnSpecCreator("UniProt: Output Accession ("+m_to_db+")", StringCell.TYPE).createSpec();
		cols[0] = new DataColumnSpecCreator("UniProt: Input Accession ("+m_from_db+")", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	@Override
	public int run(String[] accsns, DataRow[] in_rows, DataContainer out) throws Exception {
		int batch_size = 20;
		HashMap<String,String> map = new HashMap<String,String>();
		 for (int i=0; i<accsns.length; i += batch_size) {
			 String query_str = "";
			 for (int j=0; j<batch_size; j++) {
				if (i+j < accsns.length) {
					query_str += accsns[i+j] + " ";
				}
			 };
			 
			 String txt = execute("mapping", new NameValuePair[] {
				      new NameValuePair("from", m_from_db),
				      new NameValuePair("to", m_to_db),
				      new NameValuePair("format", "tab"),
				      new NameValuePair("query", query_str) }
			 );
			// Logger.getAnonymousLogger().info(txt);
			 BufferedReader sr = new BufferedReader(new StringReader(txt));
			 String line;
			 boolean first = true;
			 while ((line = sr.readLine()) != null) {
				 String[] tokens = line.split("\\s+");
				 if (!first && tokens.length == 2) {
					 map.put(tokens[0], tokens[1]);
				 }
				 first = false;
			 }
		 }
		
		 int n_hits = 0;
		 if (in_rows != null) {
			 	int idx = 0;
			 	for (DataRow in : in_rows) {
			 		DataCell[] cells = new StringCell[2];
			 		boolean has_map = map.containsKey(accsns[idx]);
			 		
			 		if (has_map) {
			 			cells[0] = new StringCell(accsns[idx]);
			 			cells[1] = new StringCell(map.get(accsns[idx]));
			 			n_hits++;
			 		} else {
			 			cells[0] = DataType.getMissingCell();
			 			cells[1] = DataType.getMissingCell();
			 		}
 			 		
			 		DataRow r = new DefaultRow(in_rows[idx].getKey(), cells);	// cells must match tablespec
			 		out.addRowToTable(new JoinedRow(in_rows[idx++], r));
			 		n_hits++;
			 	}
		 } else {
				int hit = 1;
				for (String key : map.keySet()) {
					DataCell[] cells = new StringCell[2];
					cells[0] = new StringCell(key);
					cells[1] = new StringCell(map.get(key));
					out.addRowToTable(new DefaultRow("Hit"+hit++, cells));
					n_hits++;
				}
		}
		 
		 return n_hits;
	}

	@Override
	public String fix_accsn(String in_accsn) throws Exception {
		return in_accsn.trim();
	}

	@Override
	public void cleanup() throws Exception {
		// NO-OP
	}

	@Override
	public void pause(ExecutionContext exec, double progress, String msg)
			throws InterruptedException, CanceledExecutionException {
		
		// TODO: no cache for this task, so mandatory pause for now....
		exec.checkCanceled();
		exec.setProgress(progress, "Pause to be nice to UniProt servers (20sec. delay)");
		Thread.sleep(20 * 1000);
	}

}
