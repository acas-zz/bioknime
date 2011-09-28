package au.com.acpfg.misc.biojava;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.ExecutionContext;

public interface BioJavaProcessorInterface {
	
	/**
	 * Returns the table spec needed by the processor to store the results it will produce. Must not return null.
	 * @return DataTableSpec
	 */
	public DataTableSpec get_table_spec();
	
	/**
	 * Is the result table merged with the input columns?
	 */
	public boolean isMerged();
	
	/**
	 * Processes the required task storing results into c, using the parameters as specified by m
	 * @param c the container to store results into
	 * @param m the model to use to identify which columns/data to use...
	 * @param inData[] only the first element ie. 0 will be available with the necessary data to perform the calculation
	 * @throws Exception
	 */
	public void execute(BioJavaProcessorNodeModel m, final ExecutionContext exec, NodeLogger l, final BufferedDataTable[] inData, BufferedDataContainer c) throws Exception;
}
