package au.com.acpfg.misc.uniprot;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Abstract specification of tasks to support the configure dialog parameter. Each task must implement
 * the following methods to ensure correct execution with the NodeModel and will be invoked as described
 * below.
 * 
 * @author andrew.cassin
 *
 */
public interface UniProtTaskInterface {
	/*
	 * Called during NodeModel execute(), this method must perform the task for a single accession
	 * and add all data to the specified container. As accsns[] can contain multiple accessions it is
	 * up to the implementation to determine how the batch is to be handled. Most implementations invoke
	 * UniProt for each accsn individually, except for the AccessionMapTask which is the fastest way to do it.
	 */
	public int run(String[] accsns, DataRow[] in, DataContainer out) throws Exception;
	
	/**
	 * Returns the colspec required by the task
	 */
	public DataTableSpec getTableSpec(boolean wants_xml);
	
	/**
	 * Called for each accession, during execute() this method must correct for any known defects 
	 * eg. wrong case, wrong format etc. Returning an empty string or null will cause the accession to be skipped.
	 * Throwing an exception will cause execution to stop. At a bare minimum, this method should remove whitespace surrounding the accession
	 */
	public String fix_accsn(String in_accsn) throws Exception;
	
	/**
	 * Cleanup after all <code>run()</code>'s have been done
	 */
	public void cleanup() throws Exception;
	
	/**
	 * Pause for a duration (determined by the task) based on current task state. Also responsible for
	 * updating the progress and checking for user-cancellation.
	 * 
	 */
	public void pause(ExecutionContext exec, double progress, String msg) throws InterruptedException, CanceledExecutionException;
}
