package au.com.acpfg.xml.query;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;

/**
 * Stores the list of XMLQueryEntry objects to be displayed in the configure dialog
 * 
 * @author andrew.cassin
 *
 */
public class MyQueryEntryListModel implements ListModel {
	private ArrayList<XMLQueryEntry> m_items;
	private Vector<ListDataListener> m_listeners;
	
	public MyQueryEntryListModel() {
		m_items = new ArrayList<XMLQueryEntry>();
		m_listeners = new Vector<ListDataListener>();
	}
	
	public MyQueryEntryListModel(String[] xqe_serialised_array) {
		m_items = new ArrayList<XMLQueryEntry>();
		for (String xqes : xqe_serialised_array) {
			XMLQueryEntry xqe = new XMLQueryEntry(xqes);
			m_items.add(xqe);
		}
		m_listeners = new Vector<ListDataListener>();
	}
	
	protected void signalAppend(Object o) {
		int last_item_idx = getSize() - 1;
		ListDataEvent ev = new ListDataEvent(o, ListDataEvent.INTERVAL_ADDED, last_item_idx, last_item_idx);
		for (ListDataListener l : m_listeners) {
			l.intervalAdded(ev);
		}
	}
	
	protected void signalRemove(XMLQueryEntry xqe, int idx) {
		ListDataEvent ev = new ListDataEvent(xqe, ListDataEvent.INTERVAL_REMOVED, idx, idx+1);
		for (ListDataListener l : m_listeners) {
			l.intervalRemoved(ev);
		}
	}
	
	public void add(XMLQueryEntry xqe) {
		assert(xqe != null);
		m_items.add(xqe);
		signalAppend(xqe);
	}
	
	@Override
	public void addListDataListener(ListDataListener arg0) {
		m_listeners.add(arg0);
	}

	@Override
	public Object getElementAt(int index) {
		return m_items.get(index);
	}

	@Override
	public int getSize() {
		return m_items.size();
	}
	
	public void remove(XMLQueryEntry xqe) {
		assert(xqe != null);
		int idx = m_items.indexOf(xqe);
		if (idx < 0)
			return;
		m_items.remove(idx);
		signalRemove(xqe, idx);
	}

	/**
	 * Computes a SettingsModel which is suitable for the list of queries set by the user, with
	 * the specified SettingsModel key (which must not be <code>null</code>).
	 * @param key
	 * @return
	 */
	public String[] getStringArrayValue() {
		String[] vec = new String[getSize()];
		for (int i=0; i<vec.length; i++) {
			vec[i] = getElementAt(i).toString();
		}
		return vec;
	}
	
	@Override
	public void removeListDataListener(ListDataListener arg0) {
		m_listeners.remove(arg0);
	}

}
