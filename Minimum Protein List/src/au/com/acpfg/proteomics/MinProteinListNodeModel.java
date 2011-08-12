package au.com.acpfg.proteomics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of MinProteinList.
 * Uses a greedy set cover algorithm to identify the minimal set of proteins which can explain the observed peptides
 *
 * @author Andrew Cassin
 */
public class MinProteinListNodeModel extends NodeModel {
    
	static final String CFGKEY_PEPTIDES = "peptides";
	static final String CFGKEY_PROTEIN  = "protein";
	static final String CFGKEY_ALGO     = "algorithm";
	
	private final SettingsModelString m_peptide_column = new SettingsModelString(CFGKEY_PEPTIDES, "Peptides");
	private final SettingsModelString m_accsn_column   = new SettingsModelString(CFGKEY_PROTEIN, "Protein");
	private final SettingsModelString m_algorithm      = new SettingsModelString(CFGKEY_ALGO, "ILP: Minimum Set Cover");
	
    /**
     * Constructor for the node model.
     */
    protected MinProteinListNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int pep_idx  = inData[0].getDataTableSpec().findColumnIndex(m_peptide_column.getStringValue());
    	int accsn_idx= inData[0].getDataTableSpec().findColumnIndex(m_accsn_column.getStringValue());
    	if (pep_idx < 0 || accsn_idx < 0 || pep_idx == accsn_idx) {
    		throw new Exception("Illegal columns: "+m_peptide_column+" "+m_accsn_column+", re-configure the node!");
    	}
    	DataTableSpec newSpec = new DataTableSpec(inData[0].getDataTableSpec(), make_output_spec());
    	BufferedDataContainer container = exec.createDataContainer(newSpec);
    	
    	RowIterator it = inData[0].iterator();
    	
    	HashMap<String,String> prot2pep = new HashMap<String,String>();
    	HashMap<String,String> pep2lp   = new HashMap<String,String>();
    	HashMap<String,String> prot2lp  = new HashMap<String,String>();
    	HashMap<String,Set<String>> pep2protkeys = new HashMap<String,Set<String>>();
    	
    	int peptide_idx = 1;
    	int prot_idx = 1;
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		DataCell pep_cell = r.getCell(pep_idx);
    		DataCell accsn_cell= r.getCell(accsn_idx);
    		
    		// rows with missing cells cannot be processed (no missing values in ILP...)
    		if (pep_cell.isMissing() || accsn_cell.isMissing()) {
    			continue;
    		}
    		String peptides_as_csv = ((StringCell)pep_cell).getStringValue();
    		String protein_accsn   = ((StringCell)accsn_cell).getStringValue();
    		
    		if (peptides_as_csv.trim().length() < 1 || protein_accsn.trim().length() < 1) {
    			throw new Exception("Must supply valid Protein ID (accession) and peptide list - no blank cells!");
    		}
    		String[] peptides = peptides_as_csv.split(",\\s+");
    		
    		prot2pep.put(protein_accsn, peptides_as_csv);
    		for (String pep : peptides) {
    			if (!pep2lp.containsKey(pep)) {
    				String key = "_p"+peptide_idx+"_";
    				peptide_idx++;
    				pep2lp.put(pep, key );
    			}
    		}
    		
    		if (prot2lp.containsKey(protein_accsn)) {
    			throw new Exception("Error at row "+r.getKey().getString()+": already seen peptides for protein ID "+protein_accsn);
    		}
    		String key = "_x"+prot_idx+"_";
    		prot_idx++;
    		prot2lp.put(protein_accsn, key);
    		
    		for (String pep : peptides) {
    			if (pep2protkeys.containsKey(pep)) {
    				Set<String> s = pep2protkeys.get(pep);
    				s.add(key);
    				pep2protkeys.put(pep, s);
    			} else {
    				Set<String> s = new HashSet<String>();
    				s.add(key);
    				pep2protkeys.put(pep, s);
    			}
    		}
    	}
    	
    	// create a cplex-style file with the necessary ILP formulation
    	File  tmp_file = File.createTempFile("minprotset", ".lp");
    	PrintWriter pw = new PrintWriter(new FileWriter(tmp_file));
    	pw.println("minimize");
    	pw.print(" cost: ");
    	Collection<String> c = prot2lp.values();
    	Iterator<String> it2 = c.iterator();
    	for (int i=0; i<c.size(); i++) {
    		pw.print(it2.next());
    		if (i<c.size()-1) {
    			pw.print(" + ");
    		}
    	}
    	pw.println();
    	pw.println("subject to");
    	for (String peptide : pep2lp.keySet()) {
    		pw.print(pep2lp.get(peptide)+": ");
    		Set<String> prots = pep2protkeys.get(peptide);
    		if (prots == null || prots.size() < 1)
    			throw new Exception("No proteins for peptide "+peptide);
    		Iterator<String> it3 = prots.iterator();
    		for (int i=0; i<prots.size(); i++) {
    			pw.print(it3.next());
    			pw.print(" ");
    			if (i<prots.size()-1)
    				pw.print("+ ");
    		}
    		pw.println(">= 1");
    	}
    	pw.println("binary");
    	for (String prot_id : prot2lp.values()) {
    		pw.println(" "+prot_id);
    	}
    	pw.println("end");
    	pw.close();
    	Logger l = Logger.getAnonymousLogger();
    	l.info("Created LP solver file: "+tmp_file.getAbsolutePath());
    	
    	// run cplex to compute a solution... (hopefully!)
    	List<String> args = new ArrayList<String>();
    	args.add("c:/cygwin/bin/glpsol.exe");
    	args.add("--min");
    	args.add("--lp");
    	args.add(tmp_file.getAbsolutePath());
    	args.add("-o");
    	File solution_file = File.createTempFile("minprotsol", ".txt");
    	args.add(solution_file.getAbsolutePath());
    	
    	ProcessBuilder pb = new ProcessBuilder(args);
    	pb.directory(tmp_file.getParentFile());
    	Process               p = pb.start();
    	BufferedReader error_br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    	BufferedReader out_br   = new BufferedReader(new InputStreamReader(p.getInputStream()));
    	out_br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    	StringBuffer stdout = new StringBuffer(10 * 1024);
    	String line;
    	while ((line = out_br.readLine()) != null) {
    		stdout.append(line);
    		stdout.append('\n');
    	}
    	StringBuffer stderr = new StringBuffer(10 * 1024);
    	while ((line = error_br.readLine()) != null) {
    		stderr.append(line);
    		stderr.append('\n');
    	}
    	int            exitCode = p.waitFor();
    	l.info("glpsol finished with code "+exitCode);
    	
    	// process the results from the ILP solver (GLPK specific and probably version specific)
    	BufferedReader soln_rdr  = new BufferedReader(new FileReader(solution_file));
    	Pattern pep_line_pattern = Pattern.compile("^\\s*\\d+\\s*(_p\\d+_)\\s+(\\d+)\\s+\\d+");
    	Pattern prot_line_pattern= Pattern.compile("^\\s*\\d+\\s*(_x\\d+_)\\s+\\S+\\s+(\\d+)\\s+");
    	HashMap<String,Integer> results_pep2cnt = new HashMap<String,Integer>();
    	HashMap<String,Integer> results_prot2cnt= new HashMap<String,Integer>();
    	while ((line = soln_rdr.readLine()) != null) {
    		Matcher pep_matcher = pep_line_pattern.matcher(line);
    		Matcher prot_matcher= prot_line_pattern.matcher(line);
    		if (pep_matcher.find()) {
    			String pep_key       = pep_matcher.group(1);
    			String pep_usage_cnt = pep_matcher.group(2);
    			results_pep2cnt.put(pep_key, new Integer(pep_usage_cnt));
    		} else if (prot_matcher.find()) {
    			String prot_key       = prot_matcher.group(1);
    			String prot_usage_cnt = prot_matcher.group(2);
    			results_prot2cnt.put(prot_key, new Integer(prot_usage_cnt));
    		}
    	}
    	soln_rdr.close();
    	l.info("Got results from solver for "+results_pep2cnt.size()+" peptides, "+results_prot2cnt.size()+" proteins.");
    	
    	// 3. output TRUE for those rows which are part of the minimum set, FALSE otherwise
    
    	it = inData[0].iterator();
    	while (it.hasNext()) {
    		DataRow      r = it.next();
    		String   accsn = ((StringCell)r.getCell(accsn_idx)).getStringValue();
    		boolean is_min = (results_prot2cnt.get(prot2lp.get(accsn)).intValue() == 1);
    		AppendedColumnRow new_r = new AppendedColumnRow(r, BooleanCell.get(is_min));
    		container.addRowToTable(new_r);
    	}
    	    
    	tmp_file.delete();
    	solution_file.delete();
    	
    	container.close();
    	return new BufferedDataTable[] { container.getTable() };
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

     
        return new DataTableSpec[]{new DataTableSpec(inSpecs[0], make_output_spec())};
    }

    private DataTableSpec make_output_spec() {
    	DataColumnSpec cols[] = new DataColumnSpec[1];
    	
    	cols[0] = new DataColumnSpecCreator("Is in minimum set?", BooleanCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_peptide_column.saveSettingsTo(settings);
         m_accsn_column.saveSettingsTo(settings);
         m_algorithm.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_peptide_column.loadSettingsFrom(settings);
        m_accsn_column.loadSettingsFrom(settings);
        m_algorithm.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  m_peptide_column.validateSettings(settings);
          m_accsn_column.validateSettings(settings);
          m_algorithm.validateSettings(settings);
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

}

