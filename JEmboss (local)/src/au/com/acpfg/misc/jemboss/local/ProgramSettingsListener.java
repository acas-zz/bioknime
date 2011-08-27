package au.com.acpfg.misc.jemboss.local;

import java.io.File;

import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Responsible for building a command line to run an emboss program and commencing marshalling of input/output data
 * 
 * @author andrew.cassin
 *
 */
public interface ProgramSettingsListener {
	/**
	 *	Invoked for constant arguments (same for each emboss program invocation)
	 *  @param arg guaranteed not-null fully formed command line argument
	 */
	public void addArgument(final ProgramSetting ps, String[] arg);
	
	/**
	 *  Invoked for an output file argument, the framework calls this method instead of <code>addArgument()</code>
	 *  @param opt the argument name (fully formed command line argument)
	 *  @param file the output file (which is not yet created)
	 */
	public void addOutputFileArgument(final ProgramSetting ps, String opt, File out_file);
	
	/**
	 *  Invoked for an input file argument, the framework calls this method instead of <code>addArgument()</code>
	 *  @param opt the argument name (fully formed command line argument)
	 *  @param file the input file (which is not yet created)
	 */
	public void addInputFileArgument(final ProgramSetting ps, String opt, File in_file);
}
