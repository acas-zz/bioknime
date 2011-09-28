package au.com.acpfg.misc.biojava;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;

import au.com.acpfg.misc.biojava.AlternateTranslationProcessor;
import au.com.acpfg.misc.biojava.BioJavaProcessorInterface;
import au.com.acpfg.misc.biojava.FrameTranslationProcessor;
import au.com.acpfg.misc.biojava.HydrophobicityProcessor;
import au.com.acpfg.misc.biojava.SequenceTranslationProcessor;


/**
 * This is the model implementation of BioJavaProcessor.
 * Analyses the specified data using BioJava (see http://www.biojava.org) and produces the result at output
 *
 * @author Andrew Cassin
 */
public class BioJavaProcessorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(BioJavaProcessorNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_TASK         = "task";
	static final String CFGKEY_SEQUENCE_COL = "sequence-column";
	static final String CFGKEY_SEQTYPE      = "sequence-type";
	static final String CFGKEY_MAXLEN       = "max-seq-length";

    /** initial default task */
    private static final String DEFAULT_TASK         = getTasks()[0];
    private static final String DEFAULT_SEQUENCE_COL = "Sequence";
    private static final String DEFAULT_SEQTYPE      = "Protein";
    private static final int    DEFAULT_MAXLEN       = 75;			// for Illumina short reads

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_task            = make_as_string(CFGKEY_TASK);
    private final SettingsModelString m_sequence_column = make_as_string(CFGKEY_SEQUENCE_COL);
    private final SettingsModelString m_seqtype         = make_as_string(CFGKEY_SEQTYPE);
    private final SettingsModelInteger m_maxlen         = (SettingsModelInteger) make(CFGKEY_MAXLEN);
    
    // state which is not persisted
    private int     m_sequence_idx;
    private boolean is_protein;
    private boolean is_dna;
    private boolean is_rna;
    private boolean m_warned_bad_chars;		// a warning is logged if likely non-NA/AA letters are encountered during processing
    private int     m_bad_char_count;
    
    /**
     * Constructor for the node model.
     */
    protected BioJavaProcessorNodeModel() {
        super(1, 1);
        m_sequence_idx = -1;
    }

    public static SettingsModel make(String cfgkey) {
    	if (cfgkey.equals(CFGKEY_TASK)) {
    		return new SettingsModelString(CFGKEY_TASK, DEFAULT_TASK);
    	} else if (cfgkey.equals(CFGKEY_SEQUENCE_COL)) {
    		return new SettingsModelString(CFGKEY_SEQUENCE_COL, DEFAULT_SEQUENCE_COL);
    	} else if (cfgkey.equals(CFGKEY_SEQTYPE)) {
    		return new SettingsModelString(CFGKEY_SEQTYPE, DEFAULT_SEQTYPE);
    	} else if (cfgkey.equals(CFGKEY_MAXLEN)) {
    		SettingsModel sm = new SettingsModelIntegerBounded(CFGKEY_MAXLEN, DEFAULT_MAXLEN, 1, 10000000);
    		sm.setEnabled(false);	// since the default task is not the residue-by-position task
    		return sm;
    	}
    	return null;
    }
    
    public static SettingsModelString make_as_string(String cfgkey) {
    	return (SettingsModelString) make(cfgkey);
    }
    
    public boolean areSequencesProtein() {
    	return m_seqtype.getStringValue().equals("Protein");
    }
    
    public boolean areSequencesDNA() {
    	return m_seqtype.getStringValue().equals("DNA");
    }
    
    public boolean areSequencesRNA() {
    	return m_seqtype.getStringValue().equals("RNA");
    }
    
    public static String[] getTasks() {
    	String[] ret = new String[] {"Hydrophobicity, pI and total mass",  
    						 "Convert DNA to RNA (Universal translation only)", 
    			             "Convert RNA to Protein Sequence", 
    			             "Convert DNA to Protein Sequence", 
    			             "Alternate translation of DNA to Protein (all built-in tables)",
    			             "Count Residues", 
    			             "Count Di-mers (overlapping)", 
    			             "Residue Frequency by Position",
    			             "Longest reading frame (all 6 frames, DNA)", 
    			             "Longest reading frame (3 forward frames, DNA)",
    			             "Longest reading frame (3 reverse frames, DNA)",
    			             "Longest reading frame (all 6 frames, AA)",
    			             "Longest reading frame (3 forward frames, AA)",
    			             "Longest reading frame (3 reverse frames, AA)",
    			             "Weighted Homopolymer Rate (WHR)",
    			             "SNP-assisted frameshift detection",
    			             "Tryptic Peptide Extraction (all 6 frames iff DNA/RNA, supports IUPAC code conversion)",
    			             "Six-Frame nucleotide translation (excl. NA frames)",
    			             "Six-Frame nucleotide translation (incl. NA frames)"
    			             
    	};
    	Arrays.sort(ret);
    	return ret;
    }
    
    public BioJavaProcessorInterface make_biojava_processor(String task) throws Exception {
    	if (task.startsWith("Hydrophobicity")) {
    		return new HydrophobicityProcessor();
    	} else if (task.startsWith("Six")) {
    		return new FrameTranslationProcessor(task);
    	} else if (task.startsWith("Convert")) {
    		return new SequenceTranslationProcessor(this, task);
    	} else if (task.startsWith("Alternate translation")) {
    		return new AlternateTranslationProcessor();
    	} else if (task.startsWith("Count")) {
    		return new ResidueFrequencyProcessor(this, task);
    	} else if (task.equals("Residue Frequency by Position")) {
    		return new PositionByResidueProcessor(this, task, m_maxlen.getIntValue());
    	} else if (task.startsWith("Longest reading frame")) {
    		return new LongestFrameProcessor(this, task);
    	} else if (task.startsWith("Weighted")) {
    		return new WeightedHomopolymerRateProcessor(this, task);
    	} else if (task.startsWith("SNP")) {
    		return new SNPFrameshiftDetector(this, task);
    	} else if (task.startsWith("Tryptic")) {
    		return new TrypticPeptideExtractor_v2(this, task);
    	}
    	throw new Exception("Unknown BioJava task to perform! Probably a bug...");
    }
    
    /**
     * Retrieve the sequence as letters only in the user-configured cell. Other characters
     * are removed as this would upset biojava conversion (which would silently fail)
     * 
     * @param r
     * @return the codes for the 
     */
    public String getSequence(DataRow r) {
    	assert m_sequence_idx >= 0;
    	String val = r.getCell(m_sequence_idx).toString();
    	StringBuffer sb = new StringBuffer(val.length());
    	int len = val.length();
    	for (int i=0; i<len; i++) {
    		char c = val.charAt(i);
    		if (Character.isLetter(c) || c == '-' || c == '*') {
    			sb.append(c);
    		} else {
    			m_bad_char_count++;
    			if (!m_warned_bad_chars) {
    				logger.warn("Encountered non-letter symbol: "+c+" results may be incorrect (character ignored)");
    				m_warned_bad_chars = true;
    			} 
    		}
    	}
    	return sb.toString();
    }
    
    public SymbolList getSequenceAsSymbol(String seq) throws Exception {    
    	if (seq == null || seq.length() < 1) 
    		throw new InvalidSettingsException("Encountered a non-existant sequence - please fix!");
    	
    	// SPEED: use is_* members rather than the slower areSequences*() methods
    	if (is_dna)
    		return DNATools.createDNA(seq);
    	else if (is_rna) 
    		return RNATools.createRNA(seq);
    	else
    		return ProteinTools.createProtein(seq);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       
        // the data table spec of the single output table, 
        // the table will have three columns:
        BioJavaProcessorInterface bjpi = make_biojava_processor(m_task.getStringValue());
        DataTableSpec result_spec= bjpi.get_table_spec();
        
        DataTableSpec outputSpec;
        if (bjpi.isMerged())
        	outputSpec = new DataTableSpec("BioJava Processor Specification", inData[0].getDataTableSpec(), result_spec);
        else 
        	outputSpec = result_spec;
        
        is_protein = areSequencesProtein(); // cache answers for speed
        is_dna     = areSequencesDNA();
        is_rna     = areSequencesRNA();
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        m_sequence_idx = inData[0].getDataTableSpec().findColumnIndex(m_sequence_column.getStringValue());
        if (m_sequence_idx < 0) {
        	throw new Exception("Cannot find column: "+m_sequence_column.getStringValue());
        }
        m_warned_bad_chars = false;
        m_bad_char_count   = 0;
        bjpi.execute(this, exec, logger, inData, container);
        
        if (m_bad_char_count > 0) {
        	logger.warn("WARNING: encountered "+m_bad_char_count+" non-residue symbols during processing. Results may be incorrect!");
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

        m_task.saveSettingsTo(settings);
        m_sequence_column.saveSettingsTo(settings);
        m_seqtype.saveSettingsTo(settings);
        m_maxlen.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
         
        m_task.loadSettingsFrom(settings);
        m_sequence_column.loadSettingsFrom(settings);
        m_seqtype.loadSettingsFrom(settings);
        m_maxlen.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_task.validateSettings(settings);
        m_sequence_column.validateSettings(settings);
        m_seqtype.validateSettings(settings);
        m_maxlen.validateSettings(settings);
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

}

