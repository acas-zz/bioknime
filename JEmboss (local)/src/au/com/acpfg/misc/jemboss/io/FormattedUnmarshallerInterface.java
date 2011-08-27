package au.com.acpfg.misc.jemboss.io;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Interface for adding rows to the formatted output port of this node
 * @author andrew.cassin
 *
 */
public interface FormattedUnmarshallerInterface {
	/**
	 * Returns the columns which the unmarshaller requires during <code>process()</code>
	 * 
	 * @return
	 */
	public DataColumnSpec[] add_columns();
	
	/**
	 * Responsible for processing data stored into the <code>out_file</code> and the
	 * specified program setting into (some of) the columns permitted by <code>c</code>.
	 * The routine may add no rows if it wishes or throw an exception if processing fails.
	 * 
	 * @param ps  the program setting which has some information about the out_file and its contents
	 * @param out_file the output file produced by EMBOSS
	 * @param c   the container to save results to
	 * @param rid the input Row ID (KNIME) which produced the specified file via EMBOSS
	 * @throws IOException
	 */
	public void process(ProgramSetting ps, File out_file, BufferedDataContainer c, String rid) 
		throws IOException,InvalidSettingsException;
}
