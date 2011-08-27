package au.com.acpfg.misc.jemboss.settings;

import java.util.HashMap;

/**
 * A setting which requires a (comma separated) list of values to be input
 * @author andrew.cassin
 *
 */
public class ArraySetting extends StringSetting {
	private int n;		// required size of array
	
	public ArraySetting(HashMap<String,String> attrs) {
		super(attrs);
		n = 1;
		if (attrs.containsKey("size")) {
			n = new Integer(attrs.get("size")).intValue();
		}
	}
	
	@Override 
	public void copy_attributes(HashMap<String,String> atts) {
		super.copy_attributes(atts);
		atts.put("size", new Integer(n).toString());
	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("array"));
	}
}

