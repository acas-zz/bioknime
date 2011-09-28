package au.com.acpfg.misc.jemboss.local;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * As per MyProgTreeModel, but supports filtering (at the leaves) using a user-supplied
 * set of keywords. The tree is only built at construction time and whenever the filter is changed.
 * 
 * @author andrew.cassin
 *
 */
public class MyFilteredProgTreeModel extends MyProgTreeModel {
	private MyTreeFilter m_filter;
	protected final Map<String,ProgsInCategory>    m_filtered_categories = new TreeMap<String,ProgsInCategory>();

	public MyFilteredProgTreeModel() throws IOException {
		super();
		setNoFilter();
	}
	
	public void setNoFilter() {
		setFilter(new MyTreeFilter() {

			@Override
			public boolean accepts(Object node) {
				return true;
			}
			
		});
	}
	
	public void setFilter(MyTreeFilter tf) {
		assert(tf != null);
		
		m_filter = tf;
		rebuild();
	}
	
	/**
	 * Uses the <code>m_categories</code> superclass member to rebuild the 
	 * filtered data since the filter has been changed. Notifies TreeModel listeners of the change
	 */
	protected void rebuild() {
		for (String category_name : m_categories.keySet()) {
			ProgsInCategory pic = m_categories.get(category_name);
			if (pic.hasAcceptablePrograms(m_filter))
				m_filtered_categories.put(category_name, new ProgsInCategory(pic, m_filter));
		}
		fireTreeStructureChanged();
	}
	
	
}
