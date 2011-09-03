package au.com.acpfg.misc.jemboss.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.emboss.jemboss.JembossParams;
import org.emboss.jemboss.programs.RunEmbossApplication2;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * This is the model implementation of JEmbossProcessor.
 * Runs a EMBOSS command on the local computer, based on the configure-dialog settings. Input data is taken from the input table and automatically converted into a suitable form for EMBOSS based on the chosen program.
 *
 * @author Andrew Cassin
 */
public class JEmbossProcessorNodeModel extends NodeModel implements ProgramSettingsListener {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(JEmbossProcessorNodeModel.class);
    
	// dialog configuration keys
    static final String CFGKEY_PROGRAM          = "DLG_EMBOSS_SELECTED_PROGRAM";		// which program does the user want to run (empty iff none)
    static final String CFGKEY_ACD              = "DLG_ACD";       // ACD file content (as string) which represents current program (empty iff none)
    static final String CFGKEY_SETTINGS         = "DLG_SETTINGS";
    static final String CFGKEY_BATCH_SIZE       = "DLG_BATCH_SIZE";
    	
    // state which is persisted via load/save/validate settings methods
	private final SettingsModelString m_acd       = new SettingsModelString(CFGKEY_ACD, "");
	private final SettingsModelString m_program   = new SettingsModelString(CFGKEY_PROGRAM, "");
	private final SettingsModelString m_input_ser = new SettingsModelString(CFGKEY_SETTINGS, "");
	private final SettingsModelInteger m_batch_size=new SettingsModelInteger(CFGKEY_BATCH_SIZE, 0);	// 0 denotes unlimited
	
    // state which is not persisted
    final static private JembossParams           m_je_params = new JembossParams();
    private ArrayList<String>                         m_args = null;
    final private HashMap<ProgramSetting,File> m_input_files = new HashMap<ProgramSetting,File>();		// map from input column names to input filenames for EMBOSS prog
    final private HashMap<ProgramSetting,File> m_output_files= new HashMap<ProgramSetting,File>();
    
    /**
     * Constructor for the node model.
     */
    protected JEmbossProcessorNodeModel() {
        super(1, 2);
        JembossParams.setStandaloneMode(true);		// always a local server for this node
    }

    public static JembossParams getSettings() {
    	return m_je_params;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	HashMap<Integer,ProgramSetting> idx2ps = new HashMap<Integer,ProgramSetting>();
    	
    	// 0. load key data from the dialog into internal form
    	String         input_ser = m_input_ser.getStringValue();
    	ProgramSettingsModel mdl = new ProgramSettingsModel();
    	
    	if (input_ser.length() > 0)	
    		mdl.addSettingsFrom(input_ser.split("\n"));
    	
    	String prog = m_program.getStringValue();
    	if (prog == null || prog.trim().length() < 1) {
    		throw new InvalidSettingsException("No EMBOSS program selected!");
    	}
    	
        m_args = new ArrayList<String>();
        m_args.add(prog);
        make_args(mdl);
  
        // compute environment variables based on KNIME with the emboss root specified
        Map<String,String> env = System.getenv();
        ArrayList<String> emboss_env = new ArrayList<String>();
        emboss_env.add("EMBOSS_ROOT="+getEmbossRoot());
        for (String key : env.keySet()) {
        	if (!key.equals("EMBOSS_ROOT")) {
        		emboss_env.add(key+"="+env.get(key));
        	}
        }
        logger.info(m_args.toString());
      
        // check that all the sequence columns are present in the input table
        DataTableSpec in_spec = inData[0].getDataTableSpec();
        HashMap<String,Integer> in_col2idx = new HashMap<String,Integer>();
        for (ProgramSetting ps : m_input_files.keySet()) {
        	String col_name = ps.getColumnName();
        	if (!in_spec.containsName(col_name)) 
        		throw new InvalidSettingsException("Column: "+ps.getColumnName()+" is not in input table - re-configure/reset the node?");
        	in_col2idx.put(col_name, new Integer(in_spec.findColumnIndex(col_name)));
        }
        
        // run giving data from the input table as required
    	RowIterator it = inData[0].iterator();
    	int     n_rows = inData[0].getRowCount();
    	double    done = 0.0;
    	final int        run = 0;
    	final int formatted_rows = 0;
		int n_in_batch = 0;
		int batch_size = m_batch_size.getIntValue();
		  
    	// compute second output port columns
		
    	// compute the output columns based on user settings and expected binary data (eg. PNG images)
        final RawAndFormattedTableMapper om = new RawAndFormattedTableMapper(null, null);
    	for (ProgramSetting ps : mdl) {
    		ps.addColumns(om);
    	}
    
    	// traverse the input data, invoking local emboss install as required
        final BufferedDataContainer container = exec.createDataContainer(om.getRawTableSpec());
		final BufferedDataContainer c2 = exec.createDataContainer(om.getFormattedTableSpec());
		om.setContainers(container, c2);
		
		try {
			
		
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		String rid= r.getKey().getString();
    		// marshal required values into required files
    		boolean skip = false;
    		for (ProgramSetting ps : m_input_files.keySet()) {
    			DataCell c = r.getCell(in_col2idx.get(ps.getColumnName()).intValue());
    			if (c == null || c.isMissing()) {
    				logger.warn("Skipping row "+rid+" as it is missing "+ps.getColumnName());
    				skip = true; 
    				break;
    			} else {
	    			File infile = m_input_files.get(ps);
	    			PrintWriter fw = new PrintWriter(new FileWriter(infile));
	    			try {
	    				ps.marshal(rid, c, fw);
	    			} catch (Exception e) {
	    				e.printStackTrace();
	    				throw e;
	    			}
	    			fw.close();
	    			if (infile.length() < 1) {
	    				throw new IOException("Marshalling failed (zero length) for "+ps.getColumnName()+"! Aborting...");
	    			}
    			}
    		}
    		if (skip)
    			continue;
    		
    		// run emboss program
    		File           tmp_folder = get_tmp_folder();
            RunEmbossApplication2 rea = new RunEmbossApplication2(m_args.toArray(new String[0]), emboss_env.toArray(new String[0]), tmp_folder);
            int                status = rea.getProcess().waitFor();
            String             stdout = rea.getProcessStdout();
            String             stderr = rea.getProcessStderr();
            rea.getProcess().destroy();
            
    		// load results of each batch run into output table
    		om.addRequiredCells(rid, status, stdout, stderr);
    		for (ProgramSetting ps : mdl) {
    			File out_file = m_output_files.get(ps);
    			ps.unmarshal(out_file, om);
    		}
    		
    		exec.checkCanceled();
    		exec.setProgress(done++/n_rows, "Processed row "+rid);
    	}
    	
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
    	container.close();
    	c2.close();
    	
    	// delete the temporary input & output files 
    	if (false) {
	    	for (File f  : m_input_files.values()) {
	    		f.delete();
	    	}
	    	for (File f : m_output_files.values()) {
	    		f.delete();
	    	}
    	}
    	
        return new BufferedDataTable[]{container.getTable(), c2.getTable()};
    }

    /**
     * Find the binary file, whose format is specified by <code>expected_file_format</code>
     * by examining the stdout of the emboss program invoked to find the name and then
     * attempting to locate the file in the tmp folder. If this cannot be done, the method
     * returns <code>DataType.getMissingCell()</code>
     * 
     * @param stdout
     * @param expected_file_format
     * @return
     */
    protected DataCell find_bin_file(String stdout, String expected_file_format, File tmpdir) throws Exception {
		if (stdout == null || stdout.length() < 1 || expected_file_format == null || expected_file_format.length() < 3)
			return DataType.getMissingCell();
		
		Pattern p = Pattern.compile("^Created\\s+(\\S+)$", Pattern.MULTILINE);
		Matcher m = p.matcher(stdout);
		while (m.find()) {
			String fname = m.group(1);
			if (fname.toLowerCase().endsWith(expected_file_format)) {
				File f = new File(tmpdir, fname);
				if (!f.exists() || !f.canRead())
					return DataType.getMissingCell();
				long len = f.length();
				FileInputStream fis = new FileInputStream(f);
				byte[] bytes = new byte[(int) len];
				int got = fis.read(bytes);
				fis.close();
				if (got < len)
					return DataType.getMissingCell();
				DataCell c = new PNGImageContent(bytes).toImageCell();
				// if we successfully load the PNG (without exception), then its ok to earmark the 
				// file for deletion at successful exit
				f.deleteOnExit();
				return c;
			}
		}
		return DataType.getMissingCell();
	}

	/**
     * Wrapper method which iterates thru a specified model, getting the listener (this) to
     * handle each argument.
     * 
     * @param psm the model to iterate over for every setting
     * @throws Exception
     */
    protected void make_args(ProgramSettingsModel psm) throws Exception {
    	m_input_files.clear();
    	m_output_files.clear();
    	for (ProgramSetting ps : psm) {
    		ps.getArguments(this);
    	}
    }
    
    /**
     * Returns the location of the tmp folder to use for temporary calculation files and results
     * of the EMBOSS program
     */
    public static File get_tmp_folder() {
    	return new File(System.getProperty("java.io.tmpdir"));
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
        return new DataTableSpec[]{null,null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_acd.saveSettingsTo(settings);
         m_input_ser.saveSettingsTo(settings);
         m_program.saveSettingsTo(settings);
         m_batch_size.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  m_acd.loadSettingsFrom(settings);
          m_input_ser.loadSettingsFrom(settings);
          m_program.loadSettingsFrom(settings);
          m_batch_size.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  m_acd.validateSettings(settings);
          m_input_ser.validateSettings(settings);
          m_program.validateSettings(settings);
          m_batch_size.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

    /**
     * TODO: this should be a knime emboss preference setting (and maybe other stuff too)
     * @return
     */
    public static String getEmbossRoot() {
    	return "c:/mEMBOSS";
    }
    
    /**
     * Runs the specified emboss command with only a single environment variable set: EMBOSS_ROOT
     * (used in the program tree code to update the help page when the user changes the selected program)
     * @param command_line
     * @return
     */
	public static String run_emboss_command(String command_line) {
		 RunEmbossApplication2 rea = new RunEmbossApplication2(command_line, new String[] { "EMBOSS_ROOT=" + getEmbossRoot() }, new File("c:/temp"));
		 rea.waitFor();
		 String stdout = rea.getProcessStderr();
		 return stdout;
	}

	public static String getACDText(String name) {
		 String      dir = getEmbossRoot() + File.separator + "acd";
		 File          f = new File(dir, name+".acd");
		 StringBuffer sb = new StringBuffer();
		 try {
			 BufferedReader rdr = new BufferedReader(new FileReader(f));
			 String line;
			 while ((line = rdr.readLine()) != null) {
				 sb.append(line);
				 sb.append('\n');
			 }
			 rdr.close();
			 return sb.toString();
		 } catch (Exception e) {
			 // BUG: close rdr if necessary
			 e.printStackTrace();
			 return "";
		 }
	}

	/**
	 * Returns the list of nucleotide and protein scoring matrices which are found in the "data"
	 * directory of the mEMBOSS distribution
	 * 
	 * @return the list of filenames of the scoring matrices
	 */
	public static String[] getMatrices() {
		  // this code, taken from jemboss, ignores SSSUB (but who cares about secondary structure prediction ;-) ???
		  File mfl = new File(getEmbossRoot() + File.separator + "data");
		  if (mfl.isDirectory()){
		      String[] keys = mfl.list(new FilenameFilter(){
		          public boolean accept(File dir, String name) {
		              if (name.startsWith("EPAM") ||
		                      name.startsWith("EBLOSUM") ||
		                      name.startsWith("EDNA"))
		                  return true;
		              return false;
		          }});
		      return keys;
		  }
		 
		  return new String[] {};
	}


	/**
	 * Returns the list of codons which are supported by a given EMBOSS installation. Guaranteed non-<code>null</code>.
	 */
	public static String[] getCodons() {		
		File mfl = new File(getEmbossRoot() + File.separator + "data" + File.separator + "CODONS");
		if (mfl.isDirectory()) {
			String[] keys = mfl.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					File fname = new File(dir, name);
					return !fname.isDirectory();
				}
				
			});
			if (keys == null) {
				return new String[] {};
			}
			return keys;
		}
		
		return new String[] {};
	}

	/**
	 * returns a File instance (guaranteed non-<code>null</code>) to the Emboss data folder. Not guaranteed to
	 * exist or be readable.
	 * 
	 * @return
	 */
	public static File getEmbossDataFolder() {
		return new File(getEmbossRoot() + File.separator + "data");
	}

	/********************** PROGRAMSETTINGSLISTENER INTERFACE METHODS (called during execute()) **************************************/
	
	@Override
	public void addArgument(final ProgramSetting ps, String[] str_list) {
		for (String s : str_list) {
			m_args.add(s);
		}
	}

	@Override
	public void addOutputFileArgument(final ProgramSetting ps, String opt, File out_file) {
		m_args.add(opt);
		if (ps.isFeatureOutput())
			m_args.add(out_file.getName());
		else
			m_args.add(out_file.getAbsolutePath());
		
		if (out_file.length() < 1) {			// SAFETY: dont delete anything with data which exists prior to execute()
			m_output_files.put(ps, out_file);
			//logger.debug("got output file: "+ps);
		}
	}

	@Override
	public void addInputFileArgument(final ProgramSetting ps, String opt, File in_file) {
		m_args.add(opt);
		// in_file comes from either a pre-existing file (no more work required) or from a column, in which case we must marshal it
		if (ps.isInputFromColumn()) {
			m_input_files.put(ps, in_file);	// placing a file in this map schedules it FOR DELETION: CAREFUL!
		}
		m_args.add(in_file.getAbsolutePath());
	}

}