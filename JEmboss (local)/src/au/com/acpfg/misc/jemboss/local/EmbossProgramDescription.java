package au.com.acpfg.misc.jemboss.local;

public class EmbossProgramDescription {
	private String m_name, m_descr;
	
	public EmbossProgramDescription(String name, String descr) {
		m_name = name;
		m_descr= descr;
	}
	
	public String getName() {
		return m_name;
	}
	
	public String getDescription() {
		return m_descr;
	}
	
	@Override 
	public String toString() {
		//return m_name+": "+m_descr;
		// now the tree only contains the program name (help has a short description)
		return m_name;
		
	}

	public boolean isProgram(String program_name) {
		return m_name.equalsIgnoreCase(program_name);
	}
}
