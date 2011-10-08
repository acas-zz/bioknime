package au.com.acpfg.phylogeny;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * All tasks supported by the node must implement this interface and provide a default constructor
 * 
 * @author andrew.cassin
 *
 */
public interface RunnableTask {
	/**
	 * Perform the desired phylogenetic task filling the specified container as per
	 * the table specification returned from <code>this.getOutputSpec()</code>
	 * @param ec
	 * @param container
	 * @throws Exception
	 */
	public void run(DataRow r, ExecutionContext ec, BufferedDataContainer container) throws Exception;
	
	/**
	 * Returns the output spec for the task. It is given the input spec so appending
	 * is possible via <code>new DataTableSpec(inSpec, my_spec)</code>
	 */
	public DataTableSpec getOutputSpec(DataTableSpec inSpec);
}
