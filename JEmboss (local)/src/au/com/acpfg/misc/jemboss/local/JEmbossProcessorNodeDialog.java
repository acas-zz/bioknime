package au.com.acpfg.misc.jemboss.local;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.ScrollPane;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObjectSpec;

import au.com.acpfg.misc.jemboss.settings.ProgramSetting;


/**
 * <code>NodeDialog</code> for the "JEmbossProcessor" Node.
 * This is a detailed implementation since the KNIME classes dont have the flexibility to access the 
 * ACD data for the chosen program and tailor the interface to it. This class accesses static data in
 * the model class for key EMBOSS settings.
 * 
 * @author Andrew Cassin
 */
public class JEmbossProcessorNodeDialog extends DefaultNodeSettingsPane {
	/**
	 * parameter name is always large-ish bold to assist the user
	 */
	private static final Map<TextAttribute,Object> attrs = new HashMap<TextAttribute,Object>();
	static {
		attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
		attrs.put(TextAttribute.FAMILY, Font.SANS_SERIF);
		attrs.put(TextAttribute.SIZE, new Float(9.0));
	}
	
	// members (not persisted)
	private JPanel            m_prog_panel, m_help_panel;
	private final JScrollPane    m_options_panel = new JScrollPane();
	private final JTree             m_progs_tree = new JTree(); 
	private final JEditorPane m_html_help;
	private DataTableSpec     m_input_table;
	private MyTreeSelectionListener m_tree_sel;		// only the current program is persisted to support re-configure
	private String 					m_emboss_sel;	// only used to initialise the tree selection

	// data which must be KNIME-persisted
	private String                  m_acd;		// ACD of current selection (if a program is selected by the user)
	private final ProgramSettingsModel    model  = new ProgramSettingsModel();
	private final JSpinner          m_batch_size = new JSpinner();
	
    /**
     * New pane for configuring the JEmbossProcessor node.
     */
    protected JEmbossProcessorNodeDialog() {
		m_html_help   = new JEditorPane("text/html", "");
		m_input_table = null;
		m_tree_sel    = null;
		m_emboss_sel  = null;
		m_acd         = null;
		try {
			m_progs_tree.setModel(new MyProgTreeModel());
	    	m_tree_sel = new MyTreeSelectionListener(this);
	    	m_progs_tree.getSelectionModel().addTreeSelectionListener(m_tree_sel);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
    }

    protected void setup_main_panel(JPanel main_panel) {
    	JPanel c = new JPanel();
    	main_panel.setLayout(new BorderLayout());
    	JScrollPane sp = new JScrollPane(c, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    	main_panel.add(sp, BorderLayout.CENTER);
    	c.setLayout(new BorderLayout());
    	JPanel prog_panel = new JPanel();
    	prog_panel.setBorder(BorderFactory.createTitledBorder("Select the EMBOSS program"));
    	prog_panel.setLayout(new BorderLayout());
    	
    	m_progs_tree.setRootVisible(false);
    	
    	prog_panel.add(new JScrollPane(m_progs_tree));
    	prog_panel.setMaximumSize(new Dimension(640,400));
    	c.setMaximumSize(new Dimension(640,400));
    	c.add(prog_panel, BorderLayout.CENTER);
    	
    	JPanel misc_panel = new JPanel();
    	misc_panel.setBorder(BorderFactory.createTitledBorder("Miscellaneous"));
    	JPanel i1 = new JPanel();
    	i1.setLayout(new BoxLayout(i1, BoxLayout.X_AXIS));
    	i1.add(m_batch_size);
    	i1.add(new JLabel("Batch size (0 means unlimited where permitted)"));
    	misc_panel.add(i1);
    	
    	JPanel south_panel = new JPanel();
    	south_panel.setLayout(new BoxLayout(south_panel, BoxLayout.Y_AXIS));
    	south_panel.add(misc_panel);
    	c.add(south_panel, BorderLayout.SOUTH);
    }
    
    /**
     * Called from the tree selection listener, this method updates the HTML widget based on the 
     * user-chosen emboss program.
     * @param html_fragment2 
     */
    public void update_html(String prog, String html_fragment) {
    	 m_html_help.setContentType("text/html");
    	 Font font = UIManager.getFont("Label.font");
         String bodyRule = "body { font-family: " + font.getFamily() + "; " +  "font-size: 9pt; }";
         String trRule = "tr.even { background-color: #FFFFFF; } \n" +
         				 "tr.odd  { background-color: #E0E0E0; }";
         StyleSheet ss =  ((HTMLDocument) m_html_help.getDocument()).getStyleSheet();
         ss.addRule(bodyRule);
         ss.addRule(trRule);
         // remove ugly markup from ACDtable result
         html_fragment = html_fragment.replaceFirst("<table[^>]+?>", "<table bgcolor=\"#C0C0C0\">");
         Pattern p = Pattern.compile("<tr bgcolor=\"#[A-F0-9]+\">");
         Matcher m = p.matcher(html_fragment);
         StringBuffer html_sb = new StringBuffer(html_fragment.length());
         int row_id = 1;
         while (m.find()) {
        	 if (row_id++ % 2 == 0) {
        		 m.appendReplacement(html_sb, "<tr class=\"even\">");
        	 } else {
        		 m.appendReplacement(html_sb, "<tr class=\"odd\">");
        	 }
         }
         m.appendTail(html_sb);
         
         //Logger.getAnonymousLogger().info(html_sb.toString());
         String   descr = "";
         TreePath    tp = m_progs_tree.getSelectionPath();
         if (tp != null) {
        	 Object child = tp.getLastPathComponent();
        	 if (child instanceof EmbossProgramDescription) {
        		 descr = ((EmbossProgramDescription)child).getDescription();
        	 }
         }
		 m_html_help.setText("<html><body><b>"+prog+": "+descr+"</b><p>"+html_sb+"</p></body></html>");
    }
    
    /**
     * Returns the current emboss program settings
     */
    public final ProgramSettingsModel getEmbossSettings() {
    	return model;
    }
    
    /**
     * Removes all widgets from the options panel
     */
    public void remove_options() {
    	m_options_panel.getViewport().removeAll();
    }
    
    /**
     * Shows the settings in the specified model
     * @param new_mdl
     */
    public void update_options(final ProgramSettingsModel new_mdl) {
    	m_options_panel.setViewportView(add_acd_options(new_mdl));
    }
    
    /**
     * Returns a component with all the program options available for the user to set
     * @param acd_text Complete contents of current programs ACD file from EMBOSS eg. diffseq.acd 
     * @return the component with all the widgets (and listeners) activated (never returns <code>null</code>)
     */
    private Component add_acd_options(final ProgramSettingsModel new_mdl) {
		if (new_mdl == null || new_mdl.size() < 1) {
			return new JLabel("WARNING: no options to set!");
		}
		// else
		
		//  code in the emboss ParseAcd class cant be used as it doesnt know how to integrate
		// with KNIME persistence... but the core stuff is still ok
		JPanel sp_container = new JPanel();
		sp_container.setLayout(new BoxLayout(sp_container, BoxLayout.Y_AXIS));
		
		// update model to reflect users settings
		model.assign(new_mdl);
		
		/* construct the scroll panes for each parameter with the necessary java widgets to display/edit the value
		 * as specified by the ACD value
		 */
		JPanel child = new JPanel();
		child.setAlignmentX(0.5f);
		child.setBorder(BorderFactory.createTitledBorder("Input Settings"));
		child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));
		addSettings(child, model, "input");
	
		// every useful emboss program takes some input so no test for input settings...
		sp_container.add(child);
		sp_container.add(Box.createGlue());
		
		child = new JPanel();
		child.setAlignmentX(0.5f);
		child.setBorder(BorderFactory.createTitledBorder("Output Settings"));
		child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));
		int out_cnt = addSettings(child, model, "output");
		
		if (out_cnt > 0) {
			sp_container.add(child);
			sp_container.add(Box.createGlue());
		}
		
		child = new JPanel();
		child.setAlignmentX(0.5f);
		child.setBorder(BorderFactory.createTitledBorder("Optional/Advanced Settings"));
		child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));
		int opt_cnt = addSettings(child, model, "optional");
	
		if (opt_cnt > 0) {
			sp_container.add(child);
			sp_container.add(Box.createGlue());
		}
		return sp_container;
	}

	private int addSettings(JPanel p, ProgramSettingsModel mdl, String section_name) {
		int cnt = 0;
		for (ProgramSetting ps : mdl) {
			if ((section_name.equals("input") && ps.isInput()) ||
					(section_name.equals("output") && ps.isOutput()) ||
					(section_name.equals("optional") && ps.isOptional())) {
			
			JPanel setting_panel = new JPanel();
			setting_panel.setLayout(new FlowLayout());
			p.add(setting_panel);
			p.add(Box.createRigidArea(new Dimension(5,5)));
			p.add(Box.createVerticalGlue());
			
			JLabel name_lbl = new JLabel(ps.getName());
			name_lbl.setToolTipText(ps.getPrettyDescription());
			name_lbl.setFont(Font.getFont(attrs));
			name_lbl.setAlignmentY(JLabel.TOP_ALIGNMENT);
			name_lbl.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			
			setting_panel.add(name_lbl);
			setting_panel.add(Box.createRigidArea(new Dimension(5,5)));
			setting_panel.add(Box.createHorizontalGlue());
			
			JComponent widget = ps.make_widget(m_input_table);
			widget.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			widget.setAlignmentY(JComponent.TOP_ALIGNMENT);
			setting_panel.add(widget);
			cnt++;
			}
		}
		
		return cnt;
	}

	@Override 
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, 
    										final DataTableSpec[] specs) throws NotConfigurableException {
		// first get the input table for required program settings to use
    	for (PortObjectSpec pos : specs) {
    		if (pos instanceof DataTableSpec) {
    			// record table spec for other methods in this class to use to decide which columns can be 
    			// chosen by the user when setting program parameters
    			m_input_table = (DataTableSpec) pos;
    			break;		// process only first port
    		}
    	}
    	
    	// next load the internal dialog state from previous configure
    	try {
    		m_acd              = settings.getString(JEmbossProcessorNodeModel.CFGKEY_ACD);
    		
    		// these must be read in, since the user may have changed the settings...
    		m_emboss_sel       = settings.getString(JEmbossProcessorNodeModel.CFGKEY_PROGRAM);
    		String   settings_ser = settings.getString(JEmbossProcessorNodeModel.CFGKEY_SETTINGS);
    	
    		model.clear();
    		model.addSettingsFrom(settings_ser.split("\n"));
    		
    		m_batch_size.setValue(new Integer(settings.getInt(JEmbossProcessorNodeModel.CFGKEY_BATCH_SIZE)));
    		
    		MyProgTreeModel mdl = (MyProgTreeModel) m_progs_tree.getModel();
    		TreePath tp = mdl.findProgramTreePath(m_emboss_sel);
    		m_progs_tree.setExpandsSelectedPaths(true);	// ensure selection is visible to the user
    		m_progs_tree.setSelectionPath(tp);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		settings.addString(JEmbossProcessorNodeModel.CFGKEY_ACD, m_acd);
		String sel = m_tree_sel.getSelectedEmbossProgram();
		settings.addString(JEmbossProcessorNodeModel.CFGKEY_PROGRAM, sel);
		settings.addString(JEmbossProcessorNodeModel.CFGKEY_SETTINGS,    model.toString());
	
		settings.addInt(JEmbossProcessorNodeModel.CFGKEY_BATCH_SIZE, 
						((Integer)m_batch_size.getValue()).intValue());
	}

	/**
	 * HACK: abstraction violation, but the three selection listener needs it...
	 * @return
	 */
	public JTree getEmbossTree() {
		return m_progs_tree;
	}
	
	/**
	 * Establish the interface, taking into account the persisted state
	 */
	@Override
	public void onOpen() {
		m_prog_panel        = new JPanel();
		m_help_panel        = new JPanel();
		
		try {
			m_progs_tree.setShowsRootHandles(true);
			DefaultTreeCellRenderer rndr = new DefaultTreeCellRenderer();
			rndr.setOpenIcon(null);
			rndr.setClosedIcon(null);
			rndr.setLeafIcon(null);
			m_progs_tree.setCellRenderer(rndr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// NB: m_progs_tree must be setup before this call...
		setup_main_panel(m_prog_panel);
		m_html_help.setEditable(false);
		m_help_panel.setLayout(new BorderLayout());
		m_help_panel.add(new JScrollPane(m_html_help), BorderLayout.CENTER);
		this.removeTab("Options");
		JPanel tmp = new JPanel();
		tmp.setLayout(new BorderLayout());
		tmp.add(m_prog_panel, BorderLayout.WEST);
		tmp.add(m_help_panel, BorderLayout.CENTER);
		this.addTab("EMBOSS Program", tmp);
		this.addTab("Program Settings", m_options_panel);
		this.setSelected("EMBOSS Program");
	}
	
	/**
	 * Cleans up internal state when the dialog is supposed to be closed
	 */
	public void onClose() {
		super.onClose();
		m_prog_panel    = null;
		m_help_panel    = null;
		m_input_table   = null;
		m_acd           = null;
		this.removeTab("EMBOSS Program");
		this.removeTab("Program Settings");
	}
}

