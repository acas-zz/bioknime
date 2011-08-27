package au.com.acpfg.misc.jemboss.local;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JScrollPane;

import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.settings.ListSetting;
import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * The runnable instance requires two parameters:
 * 1) the name of the emboss program to highlight and display results for
 * 2) the dialog instance to customise with the settings for the chosen program
 * 
 * @author andrew.cassin
 */
public class MyRunnable implements Runnable {
	private String m_prog;
	private JEmbossProcessorNodeDialog m_dlg;
	
	public MyRunnable(String prog, JEmbossProcessorNodeDialog dlg) {
		m_prog = prog;
		m_dlg  = dlg;
	}
	
	 private String find_params(Matcher m, String section) {
			String ret = "";
			Pattern  p = Pattern.compile("^\\s+\\]\\s*$", Pattern.MULTILINE);
			Matcher m2 = p.matcher(section);
			if (m2.find()) {
				return section.substring(0, m2.start());
			}
			return ret;
	 }

		private String extract_value(String trimmed) {
	    	String val= trimmed.substring(trimmed.indexOf('"')+1);
			if (val.endsWith("\"")) {
				return val.substring(0, val.length()-1);
			} // BUG: dont set value iff no close quote?
			
			return "";
	    }
	    
		public ProgramSettingsModel make_program_settings(String prog, String acd_text) throws InvalidSettingsException {
			ProgramSettingsModel model = new ProgramSettingsModel();
			model.setProgram(prog);
			
			/**
			 * Do a "poor mans parse" on the ACD file. Maybe we could re-use code from jemboss???
			 */
			String[] acd_sections = acd_text.split("endsection\\:\\s*\\w+");
			// BUG TODO: p doesnt match fuzznuc:pattern correctly due to lazy qualifier when looking for a ]
			Pattern p = Pattern.compile("^\\s+(\\w+):\\s+(\\w+)\\s+\\[\\s*$", Pattern.MULTILINE);
			for (String section : acd_sections) {
				Matcher m = p.matcher(section);
				String section_type = "unknown";
				while (m.find()) {
					String field_type = m.group(1);
					String field_name = m.group(2);
					String params     = find_params(m, section.substring(m.start()));
					if (field_type.equals("section")) {
						section_type = field_name;
						continue;
					}
					//Logger.getAnonymousLogger().info(section_type+":"+field_name+"\n"+params);
					
					HashMap<String,String> attrs = new HashMap<String,String>();
					attrs.put("type", field_type);
					attrs.put("name", field_name);
					
					boolean null_ok = false;
					for (String line : params.split("\n")) {
						String trimmed = line.trim();
						if (trimmed.startsWith("default:")) {
							attrs.put("default-value", extract_value(trimmed));
						} else if (trimmed.startsWith("minimum:")) {
							attrs.put("lowerbound", extract_value(trimmed));
						} else if (trimmed.startsWith("maximum:")) {
							attrs.put("upperbound", extract_value(trimmed));
						} else if (trimmed.startsWith("information:")) {
							attrs.put("description", extract_value(trimmed));
						} else if (trimmed.startsWith("size:")) {
							attrs.put("size", extract_value(trimmed));
						} else if (trimmed.startsWith("nullok:")) {
							String val = extract_value(trimmed);
							attrs.put("nullok", val);
							null_ok = val.toLowerCase().equals("y");
						}
					}
					
					// list values specified?
					int start_idx = params.indexOf("values:");
					if (start_idx >= 0) {
						String lv = params.substring(start_idx+8);
						attrs.put("list-values", lv.replaceAll("\n", ""));
					}
					if ((section_type.equals("required") || section_type.equals("input"))) {
						attrs.put("is-input", new Boolean(true).toString());
					} else if (section_type.equals("output")) {
						attrs.put("is-output", new Boolean(true).toString());
					} else {
						attrs.put("is-optional", new Boolean(true).toString());
					}
					
					// instantiate the setting via the factory method
					ProgramSetting ps = ProgramSetting.make(attrs);
					// only add once is-* attrs are set, so that the model counts setting types correctly
					model.add(ps);
				}
			}
			
			return model;
		}
		
	@Override
	public void run() {
		 String acd_text = JEmbossProcessorNodeModel.getACDText(m_prog);
		 if (acd_text.length() < 1) {
			 Logger.getAnonymousLogger().warning("No help text available for "+m_prog);
		 }
		 
		 ProgramSettingsModel model = null;
		 try {
			 model = make_program_settings(m_prog, acd_text);
			 ProgramSettingsModel dflt_mdl = m_dlg.getEmbossSettings();
			 if (model.isProgram(dflt_mdl)) {
				 model.assign(dflt_mdl);
			 }
		 } catch (InvalidSettingsException ise) {
			 Logger.getAnonymousLogger().warning("cannot load settings for "+m_prog);
		 }
		 
		 m_dlg.update_options(model);
		 String html_fragment = JEmbossProcessorNodeModel.run_emboss_command("acdtable "+m_prog);
		 m_dlg.update_html(m_prog, html_fragment);
	}

}
