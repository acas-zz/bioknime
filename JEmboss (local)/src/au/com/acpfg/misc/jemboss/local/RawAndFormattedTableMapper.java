package au.com.acpfg.misc.jemboss.local;

import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;

public class RawAndFormattedTableMapper extends AbstractTableMapper {
	private BufferedDataContainer m_raw, m_formatted;
	private int m_r_cnt, m_f_cnt;
	
	public RawAndFormattedTableMapper(BufferedDataContainer raw_container, BufferedDataContainer formatted_container) {
		m_r_cnt     = 1;
		m_f_cnt     = 1;
		setContainers(raw_container, formatted_container);
	}
	
	public void setContainers(BufferedDataContainer raw_container, BufferedDataContainer formatted_container) {
		m_raw       = raw_container;
		m_formatted = formatted_container;
	}
	
	@Override
	public void emitFormattedRow() {
		m_formatted.addRowToTable(new DefaultRow("f"+m_f_cnt++, getFormattedCells()));
	}

	@Override
	public void emitRawRow() {
		m_raw.addRowToTable(new DefaultRow("Invocation"+m_r_cnt++, getRawCells()));
	}
		
}
