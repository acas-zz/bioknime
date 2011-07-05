package au.com.acpfg.xml.query;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.fatdog.xmlEngine.DocItems;
import com.fatdog.xmlEngine.IntList;
import com.fatdog.xmlEngine.NodeTree;
import com.fatdog.xmlEngine.QueryDocumentTree;
import com.fatdog.xmlEngine.ResultList;
import com.fatdog.xmlEngine.XQEngine;
import com.fatdog.xmlEngine.exceptions.CantParseDocumentException;
import com.fatdog.xmlEngine.exceptions.MissingOrInvalidSaxParserException;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;
import au.com.acpfg.xml.query.XQueryReporter.QueryResponseFragmentType;
import au.com.acpfg.xml.reader.XMLCell;




/**
 * This is the model implementation of XMLreader.
 * Provides an XPath knime api & XML "blob" cell type and data processing. Useful for many life science XML formats (PepXML, ProtXML, etc. etc.)
 *
 * @author Andrew Cassin
 */
public class XQueryProcessorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(XQueryProcessorNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */

	static final String CFGKEY_QUERIES      = "dialog-xqueries";		// NB: must match Configure-Dialog class code!
	static final String CFGKEY_XML_CELL_OUT = "xml-cell-output";
	static final String CFGKEY_XML_COL      = "xml-column";

   

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
  
    private final SettingsModelString m_xml_col = new SettingsModelString(CFGKEY_XML_COL, "XML Data");
    private final SettingsModelStringArray m_queries = new SettingsModelStringArray(CFGKEY_QUERIES, new String[] {});
    //private final SettingsModelBoolean m_xml_out = new SettingsModelBoolean(CFGKEY_XML_CELL_OUT, false);
   
    /* private state which does not require persistence */
    private HashMap<Integer,DataColumnSpec> m_extra_cols;
    private HashMap<ResultsType,String>     m_rt_set;		// maps between the type of column and the column name
    
    /**
     * Constructor for the node model.
     */
    protected XQueryProcessorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
try {
        
        ArrayList<XMLQueryEntry> enabled_queries = new ArrayList<XMLQueryEntry>();
        for (String xqes : m_queries.getStringArrayValue()) {
        	XMLQueryEntry xqe = new XMLQueryEntry(xqes);
        	if (xqe.isEnabled()) {
        		enabled_queries.add(xqe);
        	}
        }
        
        // make output columns and ensure reporters are bound to each column for results desired by user
    	DataColumnSpec[] output_cols = make_output_cols(enabled_queries);
        DataTableSpec outputSpec = new DataTableSpec(output_cols);
		XQueryReporter r = new XQueryReporter();
		if (m_rt_set.containsKey(ResultsType.RAW_XML))
			new StringReporter(r, m_rt_set.get(ResultsType.RAW_XML));
		if (m_rt_set.containsKey(ResultsType.ELEMENT_DISTRIBUTION))
			new ElementDistributionReporter(r, m_rt_set.get(ResultsType.ELEMENT_DISTRIBUTION));
		if (m_rt_set.containsKey(ResultsType.RAW_XML_COLLECTION))
			new ElementCollectionReporter(r, m_rt_set.get(ResultsType.RAW_XML_COLLECTION));
		if (m_rt_set.containsKey(ResultsType.XMLATTR_COLLECTION))
			new AttributeCollectionReporter(r, m_rt_set.get(ResultsType.XMLATTR_COLLECTION));
		if (m_rt_set.containsKey(ResultsType.TEXT))
			new TextReporter(r, m_rt_set.get(ResultsType.TEXT));
		
		// make output container
        BufferedDataContainer container = exec.createDataContainer(outputSpec, true, 100);
        // and find the column id for the XML data to query...
        int xml_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_xml_col.getStringValue());
        if (xml_col_idx < 0) {
        	throw new Exception("Cannot locate column with XML data: "+m_xml_col.getStringValue());
        }
        
        int hit = 0;
   
        
        if (enabled_queries.size() < 1) {
        	throw new Exception("Nothing to search - no enabled user queries specified... please re-configure!");
        }
        
        RowIterator it = inData[0].iterator();
        while (it.hasNext()) {
        	DataRow row = it.next();
        	DataCell xml_cell = row.getCell(xml_col_idx);
        	if (xml_cell == null || xml_cell.isMissing())
        		continue;
        	
        	// this should not happen in practice -- programmer error!
        	if (!(xml_cell instanceof XMLCell)) {
        		continue;
        	}
        	
        	// need to create a new engine each time, so java GC can release the AST from the previous file...
            XMLCell xc = (XMLCell) xml_cell;
        	XQEngine e = new XQEngine();
            e.setXMLReader(xc.getReader(true));		// always strip namespaces for now..
            e.setDocument(xc.asFile().getAbsolutePath());
           
	        for (XMLQueryEntry xqe : enabled_queries) {
	        	String path = xqe.getQuery();
	        	if (path.length() > 0) {
		        	ResultList results = e.setQuery(path);
    				DocItems di = results.nextDocument();
    				if (di == null)
    					continue;
    				
		        	logger.info("Got "+results.getNumValidItems()+" valid hits (total " + results.getNumTotalItems()+") for "+row.getKey().getString()+ ", query="+path);
		        	if (xqe.getFailEmpty() && results.getNumValidItems() == 0) {
		        		throw new FailedPathException(row.getKey().getString(), "No matches for "+xqe.getName());
		        	} else {
		        		Set<ResultsType> wanted_set = xqe.getWantedResultsSet();
		        		int num_valid_items = results.getNumValidItems();
		        		String rkey = row.getKey().getString();
		        		
		        		// user only want one row for the query?
		        		if (wanted_set.size() == 1 && wanted_set.contains(ResultsType.RESULTS_COUNT)) {
		        			DataCell[] cells = getResultCount(rkey, path, num_valid_items, output_cols.length);
		        			container.addRowToTable(new DefaultRow("Hit"+hit++, cells));
		        		} else {
		        			// report each XQuery result, for every hit in the current file
	        				getXML(di, results, r);
	        				DataCell[] r2 = getRowCells(r, rkey, path, num_valid_items, output_cols.length);
		        			container.addRowToTable(new DefaultRow("Hit"+hit++, r2)); 
		        			r.reset();		// reset traversal between hits
		        		}
		        		logger.info("Completed processing query: "+path);
		        	}
		        	//results.removeDocument(m_di);
		        	results = null;
	        	} 
	        }
	        e = null;	// let GC do its thing...
	    }
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
} catch (Exception e) {
	e.printStackTrace();
	throw e;
}
    }
    protected DataCell[] getResultCount(String filename, String path, int valid_items, int ncols) {
		DataCell[] cells = new DataCell[ncols];
		for (int i=0; i<ncols; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[0] = new StringCell(filename);
		cells[1] = new StringCell(path);
		for (Integer colidx : m_extra_cols.keySet()) {
			String col_name = m_extra_cols.get(colidx).getName();

			if (col_name.equals("Result Count")) {
				cells[colidx.intValue()] = new IntCell(valid_items);
			} 
			// else... do nothing since it will have a missing value if not supported
		}
		return cells;
	}
    protected DataCell[] getRowCells(XQueryReporter r, String filename,
			String path, int valid_items, int ncols) {
		DataCell[] cells = new DataCell[ncols];
		for (int i=0; i<ncols; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[0] = new StringCell(filename);
		cells[1] = new StringCell(path);
		for (Integer colidx : m_extra_cols.keySet()) {
			String col_name = m_extra_cols.get(colidx).getName();
			
			cells[colidx.intValue()] = r.getResultCell(col_name);
		}
		return cells;
	}

	protected DataColumnSpec[] make_output_cols(List<XMLQueryEntry> enabled_queries) throws Exception {
    	 // compute m_rt_set and m_extra_cols based on supplied queries
    	 m_rt_set     = new HashMap<ResultsType,String>();
    	 m_extra_cols = new HashMap<Integer,DataColumnSpec>();
    	 int cols = 2;
    	
    	 
    	 DataType dt = ListCell.getCollectionType(StringCell.TYPE);
    	 for (XMLQueryEntry xqe : enabled_queries) {
    		 ResultsType[] wanted = xqe.getWantedResults();
    		 DataColumnSpec col;
    		 
    		 for (ResultsType rt : wanted) {
	    		 if (m_rt_set.containsKey(rt))
	    			 continue;
	    		 
	    		 String colname = XMLQueryEntry.colname(rt);
	    		 m_rt_set.put(rt,colname);

		    	 if (rt == ResultsType.RAW_XML) {
		    		 col = new DataColumnSpecCreator(colname, XMLCell.TYPE).createSpec();
		    	 } else if (rt == ResultsType.RAW_XML_COLLECTION) {
		    		 col = new DataColumnSpecCreator(colname, dt).createSpec();
		    	 } else if (rt == ResultsType.RESULTS_COUNT) {
		    		 col = new DataColumnSpecCreator(colname, IntCell.TYPE).createSpec();
		    	 } else if (rt == ResultsType.TEXT) {
		    		 col = new DataColumnSpecCreator(colname, StringCell.TYPE).createSpec();
		    	 } else if (rt == ResultsType.TEXT_COLLECTION) {
		    		 col = new DataColumnSpecCreator(colname, dt).createSpec();
		    	 } else if (rt == ResultsType.XMLATTR_COLLECTION) {
		    		 col = new DataColumnSpecCreator(colname, dt).createSpec();
		    	 } else if (rt == ResultsType.ELEMENTS_AS_COLUMNS) {
		    		 // throw... need to compute this
		    		 throw new Exception("TODO... not implemented!");
		    	 } else if (rt == ResultsType.ELEMENT_DISTRIBUTION) {
		    		 col = new DataColumnSpecCreator(colname,dt).createSpec();
		    	 } else {
		    		 throw new Exception("Unsupported result type: "+rt);
		    	 }
	    		 m_extra_cols.put(new Integer(cols++), col);
    		 }
    	 }
    	 
    	 DataColumnSpec[] allColSpecs = new DataColumnSpec[cols];
         allColSpecs[0] = new DataColumnSpecCreator("Filename",  StringCell.TYPE).createSpec();
         allColSpecs[1] = new DataColumnSpecCreator("XQuery",    StringCell.TYPE).createSpec();
      	 for (Integer col_idx : m_extra_cols.keySet()) {
      		 allColSpecs[col_idx.intValue()] = m_extra_cols.get(col_idx);
      	 }
      	 
      	 return allColSpecs;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_xml_col.saveSettingsTo(settings);
    	settings.addStringArray(CFGKEY_QUERIES, m_queries.getStringArrayValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	String[] vec = settings.getStringArray(CFGKEY_QUERIES);
    	m_queries.setStringArrayValue(vec);
    	m_xml_col.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_xml_col.validateSettings(settings);
    	
    	String[] queries = settings.getStringArray(CFGKEY_QUERIES);
    	XQEngine engine = new XQEngine();
    	try {
        	XMLReader rdr = XMLReaderFactory.createXMLReader();
    		engine.setXMLReader(rdr);
			engine.setExplicitDocument("<?xml version=\"1.0\" encoding=\"utf-8\"?><test></test>");
		} catch (CantParseDocumentException e1) {
			e1.printStackTrace();
		} catch (MissingOrInvalidSaxParserException e1) {
			throw new InvalidSettingsException("No SAX compliant XML parser available! Check your KNIME/Java installation!");
		} catch (SAXException e) {
			throw new InvalidSettingsException(e.getMessage());
		}
    	for (String e : queries) {
    		XMLQueryEntry xqe = new XMLQueryEntry(e);
    		try {
    			engine.setQuery(xqe.getQuery());
    		} catch (Exception exc) {
    			throw new InvalidSettingsException("Invalid query: "+xqe.getName()+", please fix the query."+
    					exc.getMessage()+
    					xqe.getQuery()
    					);
    		}
    	}
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

    public void getXML(DocItems di, ResultList rl, XQueryReporter sb )    {
    	assert(di != null && rl != null && sb != null);
      	if ( di.getNumValidItems() == 0 )
    		return;
    		
    	IntList nodes 	= di.getIntList();
    	NodeTree tree	= di.getTree();
    	
    	for( int i = 0; i < nodes.count(); i++ )
    	{
    		int type = nodes.getRef_2( i );    		
    		if ( type == DocItems.VOIDED_NODE )
    			continue;
    			
    		int value = nodes.getRef_1( i );
    		
    		if ( type >= DocItems.ELEM ) {   	
    			getTreeXML(tree, value, sb);
    		}
    		else
    			switch( type )
    			{
    				case DocItems.DOC_NODE : 			
    								getTreeXML(tree, 0, sb);
									break;
							
    				case DocItems.INT :		
    								sb.call(QueryResponseFragmentType.RESP_INT, value);
						    		break;
    				
    				case DocItems.STRING :   				
    								// BUG: needs to use di.m_indexer but thats not accessible, so use rl.getIndexer() instead...
		    						String string = rl.getIndexer().getCurrTreeWalker().getStringResult( value );
		    						sb.call(string);
		    						break;
    						
					case DocItems.BOOLEAN :
									sb.call((value == 0)? "false" : "true");
									break;
						
					case DocItems.ATTR_TEXT :						
									sb.call(tree.getAttributeText( value ));		 
									break;
				
					case DocItems.TEXT_AS_STRING : // was TEXT, but string() function changed it to this
						
									sb.call(tree.getElementText( value ));								
									break;
					/*				
					case -8 :	
									// BUG: should also use DocItems.m_indexer
									sb.call(rl.getIndexer().getCurrTreeWalker().getDouble( value ));
									break;
									
					case -9 :	
									// BUG: should also use DocItems.m_indexer
									sb.call( rl.getIndexer().getCurrTreeWalker().getDecimal( value ));
									break;
					*/		
    				default :
			    					throw new IllegalArgumentException("\nDocumentItems:emitXml(): unknown item type " + type );
    			}
    			
    	}
    }

	protected void getTreeXML(NodeTree self, int node, XQueryReporter sb) {
		assert(self != null && sb != null);
		
		switch( self.getType( node ))
    	{
    		case DocItems.DOC_NODE :	
    						emitElementNode( self, node + 1, sb, false);
    						break;
    		
    		case NodeTree.ELEM :		
			case QueryDocumentTree.ELEMENT_CTOR :	
							boolean hoist = hoistAttributeNodesInEnclosedContent(self, node+1);
    						emitElementNode( self, node, sb, hoist);
    						break;
    		
    		case NodeTree.ATTR :		
    						emitAttributeNode( self, node, sb );
    						break;
    		
    		case NodeTree.TEXT :		
    						emitTextNode( self, node, sb );
    						break;
    						
    		default :
    		
					throw new IllegalArgumentException( "getTreeXML() unknown toplevel nodetype: " + self.getType( node ));
    	}  	
    }

	protected boolean hoistAttributeNodesInEnclosedContent(NodeTree self, int attrNode) {
        if ( attrNode < self.getNodeCount() )
            if ( self.getType( attrNode ) == QueryDocumentTree. ENCLOSED_RESULTS )
                    if ( ( (QueryDocumentTree)self).getEnclosedResults( attrNode ).isAttributesOnly() )
                            return true;
        return false;
	}
	
	protected void emitAttributes(ResultList rl, XQueryReporter sb) {
        DocItems doc;
        while (( doc = rl.nextDocument()) != null ) {
                NodeTree tree = doc.getTree();

                IntList list = doc.getIntList();
                for( int i = 0; i < list.count(); i++ ) {
                        int node = list.getRef_1(i);

                        if ( tree.getType( node ) == NodeTree.ATTR ) {
                                rl.updateValidItemCount( -1 );
                                doc.updateValidItemCount( -1 );

                                list.setRef_2( i, DocItems.VOIDED_NODE );

                                sb.call(QueryResponseFragmentType.RESP_ATTRIBUTE, 
                                			tree.getAttributeName( node ), tree.getAttributeText( node ));
                        }
                }
        }
	}
	
	protected  int emitElementNode( NodeTree self, int node, XQueryReporter sb, boolean hoistAttributes)
    {
    	int myself 			= node;
    	String elementName 	= self.getElementName( node );
    	int num_nodes       = self.getNodeCount();
    	sb.call(XQueryReporter.QueryResponseFragmentType.RESP_START_ELEMENT, elementName);
	
		while( ++ node < num_nodes && self.getType( node ) == NodeTree.ATTR )
		{
			emitAttributeNode( self, node, sb );			
		}
    	if (hoistAttributes) {
    		emitAttributes(((QueryDocumentTree)self).getEnclosedResults(node), sb);
    	}
    	
    	if ( node >= num_nodes || self.getParent( node ) != myself )
    	{
    		sb.call(XQueryReporter.QueryResponseFragmentType.RESP_INCOMPLETE_END_ELEMENT, "");
			return node;
    	}
		else
    		sb.call(XQueryReporter.QueryResponseFragmentType.RESP_END_TAG, "");		// just the closing angle bracket to the start tag

    	while( node < num_nodes && self.getParent( node ) == myself )
    	{
    		int nodeType = self.getType( node );
    		switch( nodeType )
    		{
    			case NodeTree.ELEM :					
    						boolean hoist = hoistAttributeNodesInEnclosedContent(self, node+1);
    						node = emitElementNode( self, node, sb, hoist ); 
    						break;
				case NodeTree.TEXT : 
							node = emitTextNode( self, node, sb ); 
							break;
							
				case QueryDocumentTree. ELEMENT_CTOR :	// skip if encountered in element content
			    										// (only emitted indirectly via ENCLOSED_RESULTS below)
							int parent = node;
			    						
							while( ++ node < num_nodes && self.getParent(node) == parent ) { }
							
							break;	

				case QueryDocumentTree. RESERVED :	
				case QueryDocumentTree. ENCLOSED_RESULTS :
				
							if ( ! (self instanceof QueryDocumentTree) )
								throw new IllegalArgumentException("emitElementNode(): unknown nodetype " + self.getType( node ));
									
							if ( nodeType == QueryDocumentTree. RESERVED )
								sb.call( "Reserved" );
							else
								getNestedResults(((QueryDocumentTree) self).getEnclosedResults( node ), sb);
							
							++ node;
							break;
    			
    			default :
    			
    				throw new IllegalArgumentException("emitElementNode(): unknown nodetype " + self.getType( node ));
    		}	
    	}
    	
    	sb.call(QueryResponseFragmentType.RESP_END_ELEMENT_TAG, elementName);
	
		return node;
    }

	// trying to be equivalent to ResultList.emitXml() in XQEngine codebase
	private void getNestedResults(ResultList enclosedResults, XQueryReporter sb) {
		if (enclosedResults.getNumValidItems() < 0)
			return;
		
		enclosedResults.resetDocumentIterator();
		DocItems doc;
		
		while ((doc = enclosedResults.nextDocument()) != null) {
			
			// this call trying to be equivalent to DocItems.emitXml()
			getXML(doc, enclosedResults, sb );
		}
	}

	private int emitTextNode(NodeTree self, int node, XQueryReporter sb) {
		String text = self.getElementText( node );
		sb.call( text );
		return ++ node;
	}

	private void emitAttributeNode(NodeTree self, int node, XQueryReporter sb) {
		String name = self.getAttributeName( node );
		String attrValue = self.getAttributeText( node );
		sb.call(XQueryReporter.QueryResponseFragmentType.RESP_ATTRIBUTE, name, attrValue);
	}
	
	
}

