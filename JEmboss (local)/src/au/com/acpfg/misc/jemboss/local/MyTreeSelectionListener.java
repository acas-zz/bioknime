package au.com.acpfg.misc.jemboss.local;

import java.awt.Container;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class MyTreeSelectionListener implements TreeSelectionListener {
	private JEmbossProcessorNodeDialog m_dlg;
	private Object m_cur_sel;
	
	public MyTreeSelectionListener(JEmbossProcessorNodeDialog dlg) {
		m_dlg = dlg;
		m_cur_sel = null;
	}

	
	/**
	 * If the current selection is an EMBOSS program this will return a String, otherwise <code>null</code>
	 * @return
	 */
	public String getSelectedEmbossProgram() {
		if (m_cur_sel instanceof EmbossProgramDescription) {
			return ((EmbossProgramDescription)m_cur_sel).getName();
		}
		return "";
	}

   
	
	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		 Object node =  m_dlg.getEmbossTree().getLastSelectedPathComponent();
		 
		 // remove obsolete parameters from previously selected program (if any)
		 m_dlg.remove_options();

		 /* if nothing is selected */ 
		 if (node == null) {
			 return;
		 }
	
		 // do nothing unless selection changes...
		 if (node == m_cur_sel)
			 return;
		 
		 /* display the necessary swing widgets for the ACD file of the program selected */
		 m_cur_sel = node;
		 if (node instanceof EmbossProgramDescription) {
			 EmbossProgramDescription epd = (EmbossProgramDescription) node;
			 
			 SwingUtilities.invokeLater(new MyRunnable(epd.getName(), m_dlg));
		 }
	}
	
}
