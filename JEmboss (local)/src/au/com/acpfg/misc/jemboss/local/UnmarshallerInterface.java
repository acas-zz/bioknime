package au.com.acpfg.misc.jemboss.local;

import org.knime.core.data.DataCell;

public interface UnmarshallerInterface {
	public void emitFormattedRow(DataCell[] cells);
	
	public void emitRawRow(DataCell[] cells);
}
