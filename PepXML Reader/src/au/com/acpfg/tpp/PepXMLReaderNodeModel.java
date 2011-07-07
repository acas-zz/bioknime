package au.com.acpfg.tpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.CollectionDataValue.CollectionUtilityFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of PepXMLReader.
 * Reads PepXML (as produced by the trans-proteomics pipeline) to enable processing of peptide/protein identifications and statistics using KNIME
 *
 * @author Andrew Cassin
 */
public class PepXMLReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(PepXMLReaderNodeModel.class);
        
   
    static final String CFGKEY_FILE = "xml-filename";
    
    private static final String DEFAULT_FILE = "/tmp/peptide-identifications.pep.xml";
    
    private SettingsModelString m_file = new SettingsModelString(CFGKEY_FILE, DEFAULT_FILE);
    
    /**
     * xml state parsing members
     */
    private boolean m_xml_in_score_summary;

    /**
     * Constructor for the node model.
     */
    protected PepXMLReaderNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
    	
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[18];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Spectrum ID", StringCell.TYPE).createSpec();
        allColSpecs[2] = 
            new DataColumnSpecCreator("Mass", DoubleCell.TYPE).createSpec();
        allColSpecs[3] = new DataColumnSpecCreator("Charge", IntCell.TYPE).createSpec();
        allColSpecs[4] = new DataColumnSpecCreator("Peptide Sequence", StringCell.TYPE).createSpec();
        allColSpecs[5] = new DataColumnSpecCreator("Previous AA", StringCell.TYPE).createSpec();
        allColSpecs[6] = new DataColumnSpecCreator("Next AA", StringCell.TYPE).createSpec();
        allColSpecs[7] = new DataColumnSpecCreator("Modified Peptide Sequence", StringCell.TYPE).createSpec();
        allColSpecs[8] = new DataColumnSpecCreator("Peptide Identification Probability", DoubleCell.TYPE).createSpec();
        allColSpecs[9] = new DataColumnSpecCreator("X!Tandem Scores (expect,hyper,next)", DataType.getType(ListCell.class, DoubleCell.TYPE)).createSpec();
        allColSpecs[10] = new DataColumnSpecCreator("Mascot Scores (expect,homology,identity,ion,star)",  DataType.getType(ListCell.class,DoubleCell.TYPE)).createSpec();
        allColSpecs[11] = new DataColumnSpecCreator("Sequest Scores (xcorr,spscore,sprank,deltacn,deltacnstar)", DataType.getType(ListCell.class, DoubleCell.TYPE)).createSpec();
        allColSpecs[12] = new DataColumnSpecCreator("PeptideProphet Scores (fval,ntt,nmc,massd)", DataType.getType(ListCell.class, DoubleCell.TYPE)).createSpec();
        allColSpecs[13] = new DataColumnSpecCreator("Protein Accession(s)", StringCell.TYPE).createSpec();
        allColSpecs[14] = new DataColumnSpecCreator("Protein Description(s)", StringCell.TYPE).createSpec();
        allColSpecs[15] = new DataColumnSpecCreator("Matched Ions", IntCell.TYPE).createSpec();
        allColSpecs[16] = new DataColumnSpecCreator("Total Ions", IntCell.TYPE).createSpec();
        allColSpecs[17] = new DataColumnSpecCreator("Hit Rank", IntCell.TYPE).createSpec();
        
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        try {
    		FileInputStream fis = new FileInputStream(m_file.getStringValue());
    		XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(fis);
    		if (!this.getXMLFileType(xsr).equals("pepXML")) {
    			throw new Exception("PepXML document expected, but not in a compatible format. Aborting.");
    		}
    		parsePepXML(xsr, exec, container);
    	} catch (Exception e) {
    		logger.error("Unable to process Pep/ProtXML file: "+m_file.getStringValue());
    		
    		throw e;
    	}
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
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
    	m_file.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_file.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_file.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }

public void parsePepXML(XMLStreamReader xmlStreamReader, ExecutionContext exec, BufferedDataContainer cont) throws XMLStreamException, CanceledExecutionException {
	pepXML curPSM = null;  // current peptide-to-specturm match
	int row = 1;
	
	while( xmlStreamReader.hasNext() ) {
		int event = xmlStreamReader.next();
		
		if(event == XMLStreamConstants.START_ELEMENT) { //beginning of new element
			String elementName = xmlStreamReader.getLocalName();
			
			if(elementName.equals("peptideprophet_summary")) 
				xmlStreamReader.next();
			else if(elementName.equals("spectrum_query")) { // new peptide record starts
				curPSM = new pepXML(m_file.getStringValue());
				curPSM.parse_pepXML_line(xmlStreamReader);
			}
			
			if(elementName.equals("search_hit")) 
				curPSM.parse_pepXML_line(xmlStreamReader);
			
			if(elementName.equals("mod_aminoacid_mass")) 
				curPSM.record_AA_mod(xmlStreamReader);
			
			if(elementName.equals("search_score")) 
				curPSM.parse_search_score_line(xmlStreamReader);
			
			if(elementName.equals("peptideprophet_result")) 
				curPSM.record_iniProb(xmlStreamReader);
			
			if (elementName.equals("search_score_summary")) {
				m_xml_in_score_summary = true;
			} else if (elementName.equals("parameter") && m_xml_in_score_summary) {
				curPSM.record_peptideprophet_scores(xmlStreamReader);
			}
			
		}	else if(event == XMLStreamConstants.END_ELEMENT) { // end of element
			String elementName = xmlStreamReader.getLocalName();
			
			if(elementName.equals("spectrum_query")) { // end of peptide record
				curPSM.annotate_modPeptide();
				
				
				DataCell[] cells = new DataCell[18];
				cells[0] = new StringCell(curPSM.getFilename());
				cells[1] = new StringCell(curPSM.getSpecId());
				cells[2] = new DoubleCell(curPSM.getMass());
				cells[3] = new IntCell(curPSM.getCharge());
				cells[4] = new StringCell(curPSM.getPeptide());
				StringBuffer sb = new StringBuffer();
				sb.append(curPSM.getPrevAA());
				cells[5] = new StringCell(sb.toString());
				sb.setCharAt(0, curPSM.getNextAA());
				cells[6] = new StringCell(sb.toString());
				cells[7] = new StringCell(curPSM.getModPeptide());
				cells[8] = new DoubleCell(curPSM.getIniProb());
				
				cells[9]  = DataType.getMissingCell();
				cells[10] = getMascotScores(curPSM);
				cells[11] = DataType.getMissingCell();
				cells[12] = getPeptideProphetScores(curPSM);
				cells[13] = new StringCell(curPSM.getProteinIds());
				cells[14] = curPSM.hasProteinDescr() ? new StringCell(curPSM.getProteinDescr()) : DataType.getMissingCell();
				cells[15] = new IntCell(curPSM.getMatchedIons());
				cells[16] = new IntCell(curPSM.getTotalIons());
				cells[17] = new IntCell(curPSM.hitRank());

				if (row % 200 == 0) {
					exec.checkCanceled();
				}
				cont.addRowToTable(new DefaultRow("Hit"+row, cells));
				row++;
				curPSM = null;
			} else if (elementName.equals("search_score_summary")) {
				m_xml_in_score_summary = false;
			}
		}
	} 		
	}

	/**
	 * Given the current state of the pepXML instance (which cannot be null) compute the collection
	 * cell for insertion into the current KNIME row. This method should only be called if mascot results are available
	 * 
	 * @param i
	 * @return
	 */
	protected ListCell getMascotScores(pepXML i) {
		ArrayList<DoubleCell> scores = new ArrayList<DoubleCell>();
	
		scores.add(new DoubleCell(i.getMascot_expect()));
		scores.add(new DoubleCell(i.getMascot_homologyscore()));
		scores.add(new DoubleCell(i.getMascot_identityscore()));
		scores.add(new DoubleCell(i.getMascot_ionscore()));
		scores.add(new DoubleCell(i.getMascot_star()));
		
		return CollectionCellFactory.createListCell(scores);
	}

	protected ListCell getXTandemScores(pepXML i) {
		ArrayList<DoubleCell> scores = new ArrayList<DoubleCell>();
		
		scores.add(new DoubleCell(i.getXtandem_expect()));
		scores.add(new DoubleCell(i.getHyperscore()));
		scores.add(new DoubleCell(i.getNextscore()));
		return CollectionCellFactory.createListCell(scores);
	}
	
	protected ListCell getSequestScores(pepXML i) {
		ArrayList<DoubleCell> scores = new ArrayList<DoubleCell>();
		
		scores.add(new DoubleCell(i.getSequest_xcorr()));
		scores.add(new DoubleCell(i.getSequest_spscore()));
		scores.add(new DoubleCell(i.getSequest_sprank()));
		scores.add(new DoubleCell(i.getSequest_deltacn()));
		scores.add(new DoubleCell(i.getSequest_deltacnstar()));
		
		return CollectionCellFactory.createListCell(scores);
	}
	
	protected ListCell getPeptideProphetScores(pepXML i) {
		ArrayList<DoubleCell> scores = new ArrayList<DoubleCell>();
		
		scores.add(new DoubleCell(i.getPP_fval()));
		scores.add(new DoubleCell(i.getPP_ntt()));
		scores.add(new DoubleCell(i.getPP_nmc()));
		scores.add(new DoubleCell(i.getPP_massd()));
		return CollectionCellFactory.createListCell(scores);
	}
	
	/*
	 * Function runs through given file to determine if its a pepXML or protXML file
	 */
	private String getXMLFileType(XMLStreamReader xmlStreamReader) throws XMLStreamException {
		String ret = null;
		
		while( xmlStreamReader.hasNext() ) {
			int event = xmlStreamReader.next(); //get type of next event in file
			
			if(event == XMLStreamConstants.START_ELEMENT) { //beginning of new element
				String elementName = xmlStreamReader.getLocalName();
				if(elementName.equals("peptideprophet_summary")) {
					ret = "pepXML";
					break;
				}
				if(elementName.equals("protein_summary")) {
					ret = "protXML";
					break;
				}
			}
		}
	
		return ret;
	}
}

