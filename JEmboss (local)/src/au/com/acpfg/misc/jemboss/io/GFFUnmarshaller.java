package au.com.acpfg.misc.jemboss.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.biojava.bio.BioException;
import org.biojava.bio.program.gff.GFFEntrySet;
import org.biojava.bio.program.gff.GFFTools;
import org.biojava.utils.ParserException;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Unmarshalls a Generic Feature Format (GFF) annotated file. See
 * http://en.wikipedia.org/wiki/GFF for more details.
 * Uses biojava to do the actual reading, so it must be compatible with
 * the underlying biojava implementation (which is current v1.8 based until bj3 is feature complete).
 * 
 * @author andrew.cassin
 *
 */
public class GFFUnmarshaller implements UnmarshallerInterface {

	@Override
	public void addColumns(AbstractTableMapper atm,
			ProgramSetting for_this_setting) {

	}

	@Override
	public void process(ProgramSetting for_this,
			InputStream emboss_output_data_stream, AbstractTableMapper atm)
			throws IOException, InvalidSettingsException {
		BufferedReader br = new BufferedReader(new InputStreamReader(emboss_output_data_stream));
		try {
			GFFEntrySet es = GFFTools.readGFF(br);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BioException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
