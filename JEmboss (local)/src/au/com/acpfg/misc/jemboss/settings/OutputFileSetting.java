package au.com.acpfg.misc.jemboss.settings;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import au.com.acpfg.misc.jemboss.local.JEmbossProcessorNodeModel;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Output files produced by emboss programs eg. featout are handled by this setting
 * 
 * @author andrew.cassin
 *
 */
public class OutputFileSetting extends DataFileSetting {
	
	public OutputFileSetting(HashMap<String,String> attrs) {
		super(attrs);
		// for output files, we dont want the default folder to be an EMBOSS directory,
		// so override that
		try {
			File tmp_file = File.createTempFile("prefix", "suffix");
			setFolder(tmp_file.getParentFile());
			tmp_file.delete();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	@Override 
	public void getArguments(ProgramSettingsListener l) throws IOException {
		File f = null;
		// cpgplot is very fussy about the -outfeat argument, so we cater to some of its whims here...
		if (getType().equals("featout")) 
			f = File.createTempFile("output-features", ".gff3", JEmbossProcessorNodeModel.get_tmp_folder());
		else 
			f = File.createTempFile("jemboss-node", "out");
		l.addOutputFileArgument(this, "-"+getName(), f);
	}
	

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("featout") || acd_type.equals("outfile") || acd_type.equals("report"))
			return true;
		return false;
	}
}
