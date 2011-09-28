package au.com.acpfg.misc.jemboss.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.emboss.jemboss.programs.RunEmbossApplication;
import org.knime.base.node.util.BufferedFileReader;

/**
 * Implements a TreeModel interface for displaying the list of programs with categories and helpful
 * text about each supported program by the EMBOSS installation.
 * 
 * @author andrew.cassin
 *
 */
public class MyProgTreeModel implements TreeModel {
	private final ArrayList<TreeModelListener>    m_listeners = new ArrayList<TreeModelListener>();
	protected final Map<String,ProgsInCategory>   m_categories = new TreeMap<String,ProgsInCategory>();
	
	public MyProgTreeModel() throws IOException {
		File    outfile = File.createTempFile("wossname", "progs");
		String     prog = new String("wossname -search \"\" -outfile "+outfile.getAbsolutePath());
    	File   proj_dir = new File("c:\\temp");
    	Pattern       p = Pattern.compile("^(\\w+)\\s+(.*)$");
 
    	
    	RunEmbossApplication rea = new RunEmbossApplication(prog, null, proj_dir);
    	try {
    		int status = rea.getProcess().waitFor();
    		//Logger.getAnonymousLogger().info("wossname result status "+status+" output size(bytes): "+outfile.length());
    		BufferedReader br = new BufferedReader(new FileReader(outfile));
    		String line;
    		String category = null;
    		while ((line = br.readLine()) != null) {
    			if (line.trim().length() < 1)
    				continue;
    			
    			if (Character.isUpperCase(line.charAt(0))) {
    				category = line.trim();
    				ProgsInCategory pic = m_categories.get(category);
    				if (pic == null) {
    					pic = new ProgsInCategory(category);
    					m_categories.put(category, pic);
    				}
    				continue;
    			}
    			
    			if (Character.isLowerCase(line.charAt(0)) && category != null) {
    				ProgsInCategory pic = m_categories.get(category);
    				Matcher m = p.matcher(line.trim());
    				if (m.matches())
    					pic.add(m.group(1), m.group(2));
    			}
    		}
    		br.close();
    		outfile.delete();
    	} catch (Exception e) {
    		e.printStackTrace();
    		outfile.deleteOnExit();
    	}
	}

	@Override
	public void addTreeModelListener(TreeModelListener arg0) {
		if (arg0 != null)
			m_listeners.add(arg0);
	}
	
	@Override
	public Object getChild(Object parent, int child_idx) {
		int idx = 0;
		if (parent.equals(getRoot())) {
			Set<String> categories = m_categories.keySet();
			for (String category : categories) {
				if (child_idx == idx++)
					return m_categories.get(category);
			}
		} else if (parent instanceof ProgsInCategory) {
			ProgsInCategory pic = (ProgsInCategory) parent;
			Set<String> categories = pic.keys();
			for (String category : categories) {
				if (child_idx == idx++) {
					return pic.getProgram(category);
				}
			}
		} // else { parent instanceof EmbossProgramDescription
			
		return parent;
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent.equals(getRoot())) {
			return m_categories.size();
		} else if (parent instanceof ProgsInCategory) {
			ProgsInCategory pic = (ProgsInCategory) parent;
			return pic.getNumPrograms();
		} else {
			return 0;
		}
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent.equals(getRoot())) {
			assert(child instanceof ProgsInCategory);
			Iterator<String> it = m_categories.keySet().iterator();
			ProgsInCategory pic = (ProgsInCategory) child;
			for (int i=0; i<m_categories.size(); i++) {
				if (pic.hasName(it.next())) 
						return i;
			}
		} else if (parent instanceof ProgsInCategory){
			ProgsInCategory pic = (ProgsInCategory) parent;
			return pic.getIndexOfChild(child);
		}
		return 0;
	}

	@Override
	public Object getRoot() {
		return "/";
	}

	@Override
	public boolean isLeaf(Object node) {
		if (node.equals(getRoot()))
			return false;
		else if (m_categories.containsValue(node)) {
			// BUG: maybe should check to see if category has any programs
			return false;
		} else {
			// BUG: maybe should check to see if node is in tree?
			return true;
		}
	}

	/**
	 * Returns the TreePath (for a MyProgTreeModel) which represents the given emboss program name
	 * 
	 * @param program_name
	 * @return
	 */
	TreePath findProgramTreePath(String program_name) {
		if (program_name != null && program_name.length() > 0) {
			for (String c1 : m_categories.keySet()) {
				ProgsInCategory pic = m_categories.get(c1);
				for (EmbossProgramDescription c2 : pic) {
					if (c2.isProgram(program_name)) {
						return new TreePath(new Object[] {getRoot(), pic, c2});
					}
				}
			}
		}
		
		// not found: return root
		return new TreePath(getRoot());
	}
	
	@Override
	public void removeTreeModelListener(TreeModelListener arg0) {
		if (arg0 != null)
			m_listeners.remove(arg0);
	}

	@Override
	public void valueForPathChanged(TreePath arg0, Object arg1) {
		// NO-OP this node model does nothing with this message
	}
	
	protected void fireTreeStructureChanged() {
		TreeModelEvent ev = new TreeModelEvent(this, new TreePath(getRoot()));
		for (TreeModelListener tml : m_listeners) {
			tml.treeStructureChanged(ev);
		}
	}

}
