package au.com.acpfg.misc.jemboss.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

import au.com.acpfg.misc.jemboss.settings.DummySetting;
import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Responsible for holding the results from the EMBOSS program invocation and distributing
 * them amongst the various KNIME table cells as required to faithfully represent the results.
 * Collaborates with the node model to build up a list of available columns in the input and output tables
 * 
 * @author andrew.cassin
 *
 */
public abstract class AbstractTableMapper {
	private final List<DataColumnSpec> m_raw_cols = new ArrayList<DataColumnSpec>();
	private final List<DataColumnSpec> m_formatted_cols = new ArrayList<DataColumnSpec>();
	
	private final HashMap<DataColumnSpec,ProgramSetting> m_col2ps = new HashMap<DataColumnSpec,ProgramSetting>();
	private final HashMap<String,DataColumnSpec> m_ps2col = new HashMap<String,DataColumnSpec>();
	private final HashMap<String,Integer> m_ps2idx = new HashMap<String,Integer>();
	
	// formatted data columns may provide many columns for just a single setting, so that results in List<...> everywhere
	private final HashMap<DataColumnSpec,ProgramSetting> m_f_col2ps = new HashMap<DataColumnSpec,ProgramSetting>();
	private final HashMap<String,List<DataColumnSpec>> m_f_ps2cols = new HashMap<String,List<DataColumnSpec>>();
	private final HashMap<String,List<Integer>> m_f_ps2idx = new HashMap<String,List<Integer>>();
	
	private DataCell[] m_raw_cells;
	private DataCell[] m_formatted_cells;
	
	public AbstractTableMapper() {
		// always present - useful to use for join
		DataColumnSpec rowid_col = new DataColumnSpecCreator("RowID", StringCell.TYPE).createSpec();
		HashMap<String,String> ds_attrs = new HashMap<String,String>();
		ds_attrs.put("name", "RowID");
		ds_attrs.put("type", "RowID");
		DummySetting ds = new DummySetting(ds_attrs);
		List<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		cols.add(rowid_col);
		addFormattedColumns(ds, cols);
		
		m_raw_cols.add(new DataColumnSpecCreator("RowID", StringCell.TYPE).createSpec());
    	m_raw_cols.add(new DataColumnSpecCreator("Run status", IntCell.TYPE).createSpec());
    	m_raw_cols.add(new DataColumnSpecCreator("Runtime Output (if any)", StringCell.TYPE).createSpec());	// stdout
    	m_raw_cols.add(new DataColumnSpecCreator("Runtime Errors (includes description)", StringCell.TYPE).createSpec());	// stderr for each invocation ie. batch
    	m_raw_cells       = null;
    	m_formatted_cells = null;
	}
	
	public void addRawColumn(ProgramSetting ps, DataColumnSpec colspec) {
		assert(colspec != null && ps != null);
		m_col2ps.put(colspec, ps);
		// NB: these maps must use the name rather than the program setting as some settings
		// create new instances (ie. class) of setting depending on their configuration
		m_ps2col.put(ps.getName(),colspec);
		m_ps2idx.put(ps.getName(), new Integer(m_raw_cols.size()));
		m_raw_cols.add(colspec);
	}
	
	public void addFormattedColumns(ProgramSetting ps, List<DataColumnSpec> columns) {
		assert(ps != null && columns != null);
		for (DataColumnSpec col : columns) {
			m_f_col2ps.put(col, ps);
			m_formatted_cols.add(col);
		}
		m_f_ps2cols.put(ps.getName(), columns);
		List<Integer> indices = new ArrayList<Integer>();
		for (DataColumnSpec col : columns) {
			indices.add(new Integer(m_formatted_cols.indexOf(col)));
		}
		m_f_ps2idx.put(ps.getName(), indices);
	}

	public void setRawOutputCell(ProgramSetting ps, DataCell dc) {
		assert(ps != null && dc != null);
		int idx = m_ps2idx.get(ps.getName()).intValue();
		m_raw_cells[idx] = dc;
	}
 
	public DataTableSpec getRawTableSpec() {
		DataTableSpec dt = new DataTableSpec(m_raw_cols.toArray(new DataColumnSpec[0]));
		m_raw_cells      = new DataCell[m_raw_cols.size()];
		for (int i=0; i<m_raw_cells.length; i++) {
			m_raw_cells[i] = DataType.getMissingCell();
		}
		return dt;
	}
	
	public DataTableSpec getFormattedTableSpec() {
		DataTableSpec dt  = new DataTableSpec(m_formatted_cols.toArray(new DataColumnSpec[0]));
		m_formatted_cells = new DataCell[m_formatted_cols.size()];
		return dt;
	}
	
	public void addRequiredCells(String row_id, int exit_status, String stdout, String stderr) {
		m_raw_cells[0] = new StringCell(row_id);
		m_raw_cells[1] = new IntCell(exit_status);
		m_raw_cells[2] = new StringCell("<html><pre>"+stdout);
		m_raw_cells[3] = new StringCell("<html><pre>"+stderr);
		m_formatted_cells[0] = m_raw_cells[0];
	}
	
	protected DataCell[] getRawCells() {
		for (int i=0; i<m_raw_cells.length; i++) {
			if (m_raw_cells[i] == null)
				m_raw_cells[i] = DataType.getMissingCell();
		}
		return m_raw_cells;
	}
	
	protected DataCell[] getFormattedCells() {
		for (int i=0; i<m_formatted_cells.length; i++) {
			if (m_formatted_cells[i] == null)
				m_formatted_cells[i] = DataType.getMissingCell();
		}
		return m_formatted_cells;
	}
	
	/**
	 * Must emit the same number of cells as defined by <code>getRawTableSpec()</code>
	 * @return
	 */
	public abstract void emitRawRow();

	public void setFormattedCells(HashMap<String, DataCell> cellmap) {
		if (cellmap != null) {
			for (DataColumnSpec s : m_f_col2ps.keySet()) {
				if (cellmap.containsKey(s.getName())) {
					int idx = m_formatted_cols.indexOf(s);
					m_formatted_cells[idx] = cellmap.get(s.getName());
				} else {
					int idx = m_formatted_cols.indexOf(s);
					m_formatted_cells[idx] = DataType.getMissingCell();
				}
			}
		}
	}

	public abstract void emitFormattedRow();

	/**
	 * Returns the string representation of the current rowID
	 * @return
	 */
	public abstract String getCurrentRow();
}
