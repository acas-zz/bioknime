package au.com.acpfg.misc.jemboss.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Interface for adding rows to the output ports of the JEmboss Processor node. This code
 * is quite tricky and stupid. but for now its all there is. Each class which implements this
 * interface must (currently) be manually registered with the NodeModel class in order to be invoked
 * for a given EMBOSS result.
 * 
 * @author andrew.cassin
 *
 */
public interface UnmarshallerInterface {
	
	/**
	 * Returns the columns which the unmarshaller requires during <code>process()</code>,
	 * tailoring the column names based on <code>for_this_setting</code> to ensure unique column names
	 * 
	 * @return
	 */
	public void addColumns(AbstractTableMapper atm, ProgramSetting for_this_setting);
	
	/**
	 * Responsible for processing data stored by EMBOSS in the <code>out_file</code> and the
	 * specified program setting into (some of) the columns permitted by <code>c</code>.
	 * The routine may add no rows if it wishes or throw an exception if processing fails.
	 * 
	 * @param for_this  the program setting which has some information about the out_file and its contents
	 * @param out_file the output file produced by EMBOSS
	 * @param atm  the table mapping instance to save results to (abstraction of KNIME output ports)
	 * @throws IOException
	 */
	public void process(ProgramSetting for_this, InputStream emboss_output_data_stream, AbstractTableMapper atm) 
		throws IOException,InvalidSettingsException;
}
