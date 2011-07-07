package au.com.acpfg.tpp;

import java.util.ArrayList;

/**
 * Implementation of the PepXMLResultInterface which records key global settings for
 * the .pep.xml being parsed (its lifetime is during the parse only)
 * 
 * @author andrew.cassin
 *
 */
public final class PepXMLGlobals implements PepXMLResultInterface {
	private ArrayList<String> m_engines;
	
	public PepXMLGlobals() {
		m_engines = new ArrayList<String>();
	}
	
	@Override
	public void addSearchEngine(String new_engine) {
		String tmp = new_engine.trim().toLowerCase();
		if (!hasSearchEngine(tmp)) {
			m_engines.add(tmp);
		}
	}

	@Override
	public boolean hasSearchEngine(String engine) {
		return m_engines.contains(engine);
	}

	@Override
	public boolean hasOtherSearchEngine() {
		for (String engine : m_engines) {
			if (!engine.startsWith("mascot") && !engine.startsWith("xtandem") && !engine.startsWith("sequest"))
				return true;
		}
		return false;
	}


}
