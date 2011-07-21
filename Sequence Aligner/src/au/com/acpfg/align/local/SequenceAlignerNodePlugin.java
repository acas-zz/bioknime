/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package au.com.acpfg.align.local;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * This is the eclipse bundle activator.
 * Note: KNIME node developers probably won't have to do anything in here, 
 * as this class is only needed by the eclipse platform/plugin mechanism.
 * If you want to move/rename this file, make sure to change the plugin.xml
 * file in the project root directory accordingly.
 *
 * @author Andrew Cassin
 */
public class SequenceAlignerNodePlugin extends Plugin {

    /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = "au.com.acpfg.align.local";

    // The shared instance.
    private static SequenceAlignerNodePlugin plugin;
    private static URL jaligner_jar_file;

    /**
     * The constructor.
     */
    public SequenceAlignerNodePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        jaligner_jar_file = FileLocator.find(context.getBundle(), new Path("lib/jaligner.jar"), null);
    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the Plugin
     */
    public static SequenceAlignerNodePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns a new InputStream which refers to the jaligner jar file. It is the callers
     * responsibility to close the stream at an appropriate time
     */
    public static InputStream getJAlignerJARStream() throws IOException {
    	return jaligner_jar_file.openStream();
    }
}

