package au.com.acpfg.xml.query;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;

/**
 * Displays all the properties of the XMLQueryEntry object in a single component 
 * which is contained in a scrollable, vertical list for the user
 * 
 * @author andrew.cassin
 *
 */
public class MyQueryEntryRenderer implements ListCellRenderer {
	private static final Color HIGHLIGHT_COLOR = new Color(155, 192, 255);
	
	@Override
	public Component getListCellRendererComponent(JList arg0, Object arg1,
			int arg2, boolean is_selected, boolean has_focus) {
		JPanel parent = new JPanel();
		
		parent.setLayout(new BorderLayout());
		JTextPane t_label = new JTextPane();
		t_label.setContentType("text/html");
		if (is_selected) {
			t_label.setBackground(HIGHLIGHT_COLOR);
		} else {
			t_label.setBackground(Color.WHITE);
		}
		parent.add(t_label, BorderLayout.CENTER);
		XMLQueryEntry xqe = (XMLQueryEntry) arg1;
		String[] lines = xqe.getQuery().trim().split("\n");
		String atts = "";
		if (xqe.getFailEmpty()) {
			atts += "(<i>abort if empty</i>)";
		}
		String fontspec = "black";
		if (!xqe.isEnabled()) {
			fontspec = "red";
		}
		
		ResultsType[] wanted = xqe.getWantedResults();
		StringBuffer rt_wanted = new StringBuffer(256);
		int idx =0;
		for (ResultsType rt : wanted) {
			rt_wanted.append(rt);
			if (idx++ < wanted.length-1)
				rt_wanted.append(", ");
		}
		int left = xqe.getQuery().length() - lines[0].length();
		String text = "<html><b>"+xqe.getName()+"</b>: "+rt_wanted+" "+atts+"\n<br/><tt color=\""+fontspec+"\">"+lines[0]+"</tt>";
		if (left > 0) {
			text += "... (+"+left+" more characters)";
		}
		t_label.setText(text);
		return parent;
	}

}
