package au.com.acpfg.misc.StringMatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * This is the model implementation of StringMatcher.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * @author Andrew Cassin
 */
public class StringMatcherNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(StringMatcherNodeModel.class);
    
    /**
     *  Configuration keys and default values for model values
     */
   static final String  CFG_AS_REGEXP   = "As regular expression?";
   static final boolean DEF_AS_REGEXP   = false;
   static final String  CFG_ONLY_ROWS    = "Keep only matching rows?";
   static final boolean DEF_ONLY_ROWS   = false;
   static final String  CFG_INPUT_STRINGS= "Match columns";
   static final String  CFG_MATCHER_STRINGS="Strings to match";
   static final String  CFG_OUTPUT_FORMAT  = "Required Output";

   private final SettingsModelBoolean m_as_regexp   = new SettingsModelBoolean(CFG_AS_REGEXP, DEF_AS_REGEXP);
   private final SettingsModelString  m_input       = new SettingsModelString(CFG_INPUT_STRINGS, "");
   private final SettingsModelString  m_matcher     = new SettingsModelString(CFG_MATCHER_STRINGS, "");
   private final SettingsModelBoolean m_keep_only   = new SettingsModelBoolean(CFG_ONLY_ROWS, DEF_ONLY_ROWS);
   private final SettingsModelStringArray m_outformat   = new SettingsModelStringArray(CFG_OUTPUT_FORMAT, new String[] { "Matches (collection)" } );
   
    // internal state during execute()
    private Pattern[] m_matchers;
    private int m_match_col_idx, m_cnt;
    private DenseBitVector m_bv;
    private ArrayList<String> m_matches;
    private ArrayList<Extent> m_match_pos;
    private boolean m_want_matches;
    private boolean m_want_pos;
    private boolean m_unique_count;
    private boolean m_unique_matches;
    private boolean m_match_count;
    private boolean m_start_pos;
    private boolean m_extents;
    private boolean m_overall_extent;
    private boolean m_startpos_density;
    private boolean m_numposmatches;
    private boolean m_coverage;
    private boolean m_search_strings;
    private boolean m_highlight_single_colour;
    private ArrayList<MatchReporter> m_reporters;
    private HashMap<Integer,String>  m_orig_patterns;
    private List<String> m_matching_search_strings;		  // only created if m_search_strings is true (performance)
    private HashMap<String,Integer> m_search_string_freq; // only created if m_search_strings is true
    
    /**
     * Constructor for the node model.
     */
    protected StringMatcherNodeModel() {
        // two incoming ports and one outgoing port
        super(2, 1);
        m_matches   = new ArrayList<String>();
        m_match_pos = new ArrayList<Extent>();
        m_reporters = new ArrayList<MatchReporter>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {


        m_match_col_idx   = inData[0].getDataTableSpec().findColumnIndex(m_input.getStringValue());
        int matching_col_idx= inData[1].getDataTableSpec().findColumnIndex(m_matcher.getStringValue());
        
        if (m_match_col_idx < 0 || matching_col_idx < 0) {
        	logger.fatal("Unable to find input columns: " + m_input.getStringValue() + " " + m_matcher.getStringValue());
        }
        
        // inData[0] is the data to be matched, inData[1] is the strings to use for matching (or RE's)
    	DataTableSpec new_cols = make_output_columns(inData[0].getDataTableSpec(), 
    															m_outformat.getStringArrayValue());	
        
        m_matchers = compile_patterns(inData[1], matching_col_idx);
        
        // warn if probably not feasible...
        if (inData[0].getRowCount() * inData[1].getRowCount() > 10000*1000*1000) {
        	logger.warn("Probably infeasible search (will take a long time to complete): "+inData[0].getRowCount()+" "+inData[1].getRowCount());
        }
        BufferedDataContainer container = exec.createDataContainer(new DataTableSpec(inData[0].getDataTableSpec(),new_cols));
        RowIterator it = inData[0].iterator();
        int n_rows = inData[0].getRowCount();
        int done = 0;
        while (it.hasNext()) {
        	DataRow r = it.next();
        	String str = ((StringCell)r.getCell(m_match_col_idx)).getStringValue();
        	match_string(str, container, r);
        	done++;
        	if (done % 10 == 0) {
        		exec.checkCanceled();
        		exec.setProgress(((double)done/n_rows), "");
        	}
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

	private void match_string(String str, BufferedDataContainer c, DataRow r) throws Exception {
		assert(str != null && c != null && r != null);
		int len = str.length();
		m_bv = new DenseBitVector(len);
		m_cnt = 0;
		m_matches.clear();
		m_match_pos.clear();
		if (m_search_strings) {
			if (m_matching_search_strings == null)
				m_matching_search_strings = new ArrayList<String>();
			if (m_search_string_freq == null)
				m_search_string_freq = new HashMap<String,Integer>();
			m_matching_search_strings.clear();
			m_search_string_freq.clear();
		}
		for (int i=0; i<m_matchers.length; i++) {
    		Matcher m = m_matchers[i].matcher(str);
    		int base = 0;
    		while (m.find(base)) {
    			m_cnt++;
    			m_matches.add(m.group(0));
    			int start = m.start();
    			int end   = m.end();
    			m_bv.set(start, end);
    			m_match_pos.add(new Extent(start,end));
    			base      = m.start() + 1;
    			if (m_search_strings) {
    				String pat = m_orig_patterns.get(new Integer(i));
    				m_matching_search_strings.add(pat);
    				if (m_search_string_freq.containsKey(pat)) {
    					Integer j = m_search_string_freq.get(pat);
    					m_search_string_freq.put(pat, new Integer(j.intValue()+1));
    				} else {
    					m_search_string_freq.put(pat, new Integer(1));
    				}
    			}
    		}
    	}
		
		int n_rpt = m_reporters.size();
		if (n_rpt > 0 && (!m_keep_only.getBooleanValue() || (m_keep_only.getBooleanValue() && m_cnt > 0))) {
			int idx=0;
			DataCell[] cells = new DataCell[n_rpt];
			for (MatchReporter rpt : m_reporters) {
				cells[idx++] = rpt.report(this, str);
			}
			c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey(), cells)));
		}
	}
	
    private DataTableSpec make_output_columns(DataTableSpec inSpec, String[] wanted) throws InvalidSettingsException {
    	ColumnRearranger c = new ColumnRearranger(inSpec);
    	
		DataType list_of_string_type = ListCell.getCollectionType(StringCell.TYPE);
    	ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
    	
    	m_want_matches = false;
    	m_want_pos = false;
    	m_unique_count = false;
    	m_unique_matches = false;
    	m_match_count = false;
    	m_start_pos = false;
    	m_extents = false;
    	m_overall_extent = false;
    	m_startpos_density = false;
    	m_numposmatches = false;
    	m_coverage = false;
    	m_search_strings = false;
    	m_highlight_single_colour = false;
    	m_reporters.clear();
		for (String want : wanted) {
			if (want.equals("Matches (collection)")) {
				cols.add(new DataColumnSpecCreator("Matches", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_want_matches = true;
				m_reporters.add(new MatchesReporter(false));
			} else if (want.startsWith("Match Positions")) {
				m_want_pos = true;
				cols.add(new DataColumnSpecCreator("Match Positions", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_reporters.add(new MatchPositionsReporter());
			} else if (want.equals("Unique Match Count")) {
				m_unique_count = true;
				cols.add(new DataColumnSpecCreator("Unique Match Count", IntCell.TYPE).createSpec());
				m_reporters.add(new UniqueMatchReporter(true));
			} else if (want.equals("Unique Matches")) {
				m_unique_matches = true;
				cols.add(new DataColumnSpecCreator("Unique Matches", SetCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_reporters.add(new UniqueMatchReporter(false));
			} else if (want.equals("Unique Match Distribution")) {
				m_unique_matches = true;
				cols.add(new DataColumnSpecCreator("Unique Match Distribution", SetCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_reporters.add(new UniqueMatchReporter(want));
			} else if (want.equals("Match Count")) {
				m_match_count = true;
				cols.add(new DataColumnSpecCreator("Match Count", IntCell.TYPE).createSpec());
				m_reporters.add(new MatchesReporter(true));
			} else if (want.startsWith("Start Positions")) {
				m_start_pos = true;
				cols.add(new DataColumnSpecCreator("Match Start Positions", ListCell.getCollectionType(IntCell.TYPE)).createSpec());
				m_reporters.add(new StartPositionsReporter());
			} else if (want.startsWith("Extent of matches")) {
				m_extents = true;
				cols.add(new DataColumnSpecCreator("Match Lengths", ListCell.getCollectionType(IntCell.TYPE)).createSpec());
				m_reporters.add(new MatchLengthsReporter());
			} else if (want.startsWith("Highlight Matches (HTML, single colour)")) {
				m_highlight_single_colour = true;
				cols.add(new DataColumnSpecCreator("Highlighted (HTML, single colour)", StringCell.TYPE).createSpec());
				m_reporters.add(new HighlightMatchReporter(true));	// true: use single colour for all matches
			} else if (want.startsWith("Match Extent")) {
				m_overall_extent = true;
				// NB: KNIME wont allow duplicate column names, so we must adjust the title depending on want
				String title = "Matched Region";
				if (want.endsWith("(substring)")) {
					title += " (substring)";
				} else {
					title += " (position)";
				}
				cols.add(new DataColumnSpecCreator(title, StringCell.TYPE).createSpec());
				m_reporters.add(new ExtentReporter(want.endsWith("(substring)")));
			} else if (want.startsWith("Patterns (successful, distinct") || want.startsWith("Pattern distribution")) {
				m_search_strings = true;
				String title = "Successful Patterns (distinct)";
				if (want.startsWith("Pattern distribution")) 
					title = "Successful Pattern Frequency";
				cols.add(new DataColumnSpecCreator(title, SetCell.getCollectionType(StringCell.TYPE)).createSpec());
				m_orig_patterns = new HashMap<Integer,String>();
				m_search_string_freq = new HashMap<String,Integer>();
				m_reporters.add(new SearchStringReporter(want));
			} else if (want.startsWith("Non-overlapping matches")) {
				cols.add(new DataColumnSpecCreator("Non-overlapping matches", list_of_string_type).createSpec());
				m_reporters.add(new NonOverlappingMatchesReporter(false));
			} else if (want.startsWith("Non-overlapping match count")) {
				cols.add(new DataColumnSpecCreator("Non-overlapping match count", IntCell.TYPE).createSpec());
				m_reporters.add(new NonOverlappingMatchesReporter(true));
			} else if (want.startsWith("Match Start Position Density")) {
				m_startpos_density = true;
				cols.add(new DataColumnSpecCreator("Match Start Position Density", DenseBitVectorCell.TYPE).createSpec());
				m_reporters.add(new StartDensityPositionReporter());
			} else if (want.startsWith("Match Position Density")) {
				cols.add(new DataColumnSpecCreator("Match Position Density", DenseBitVectorCell.TYPE).createSpec());
				m_reporters.add(new MatchDensityPositionReporter());
			} else if (want.startsWith("Number of matches per position")) {
				m_numposmatches = true;
				cols.add(new DataColumnSpecCreator("Match count by string length", ListCell.getCollectionType(IntCell.TYPE)).createSpec());
				m_reporters.add(new MatchesPerPositionReporter());
			} else if (want.startsWith("Input String Coverage")) {
				cols.add(new DataColumnSpecCreator("Input Coverage (%)", DoubleCell.TYPE).createSpec());
				m_coverage = true;
				m_reporters.add(new CoverageReporter());
			} else {
				throw new InvalidSettingsException("Unknown match datum: "+want);
			}
		}
	
		return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
	}
    
    
    public int getNumMatches() {
    	return (m_matches != null) ? m_matches.size() : 0;
    }
    
    
	protected Pattern[] compile_patterns(BufferedDataTable in_data, int col_idx) throws Exception {
    	int n_rows = in_data.getRowCount();
    	if (n_rows < 1) {
    		throw new Exception("No regular expressions provided to match!");
    	}
    	Pattern[] ret = new Pattern[n_rows];
    	RowIterator i = in_data.iterator();
    	int idx = 0;
    	String s = null;
    	try {
    		while (i.hasNext()) {
    			DataRow r = i.next();
    			s = r.getCell(col_idx).toString();
    			logger.info("Compiling pattern: "+s);
    			if (m_search_strings)
    				m_orig_patterns.put(new Integer(idx), s);
    			ret[idx++] = Pattern.compile(s);
    		}
    	} catch (Exception e) {
    		logger.warn("Unable to compile: "+s+" as a regular expression - bad syntax?");
    		throw(e);
    	}
    	
    	return ret;
    }
      
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	// NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	DataTableSpec spec = new DataTableSpec(inSpecs[0],
    			make_output_columns(inSpecs[0], m_outformat.getStringArrayValue()));
		return new DataTableSpec[]{spec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_as_regexp.saveSettingsTo(settings);
        m_input.saveSettingsTo(settings);
        m_matcher.saveSettingsTo(settings);
        m_outformat.saveSettingsTo(settings);
        m_keep_only.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_as_regexp.loadSettingsFrom(settings);
        m_input.loadSettingsFrom(settings);
        m_matcher.loadSettingsFrom(settings);
        m_outformat.loadSettingsFrom(settings);
        m_keep_only.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_as_regexp.validateSettings(settings);
        m_input.validateSettings(settings);
        m_matcher.validateSettings(settings);
        m_outformat.validateSettings(settings);
        m_keep_only.validateSettings(settings);
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

	public DenseBitVector getResultsBitVector() {
		return m_bv;
	}
	
	List<Extent> getMatchPos() {
		return m_match_pos;
	}

	public List<String> getMatches() {
		return m_matches;
	}

	public List<String> getMatchingPatterns() {
		assert(m_search_strings);		// logical error: these results are only available when chosen by the user
		return m_matching_search_strings;
	}
	
	public Map<String,Integer> getMatchPatternFrequency() {
		assert(m_search_strings);
		return m_search_string_freq;
	}
}

