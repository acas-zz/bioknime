package au.com.acpfg.phylogeny;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * Computes the distance matrix for the specified (assumed aligned) sequences
 * @author andrew.cassin
 *
 */
public class CalcDistanceMatrixTask implements RunnableTask {

	@Override
	public void run(DataRow r, ExecutionContext ec, BufferedDataContainer container)
			throws Exception {
		
	}

	@Override
	public DataTableSpec getOutputSpec(DataTableSpec inSpec) {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Distance Matrix", DistanceMatrixCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

}
