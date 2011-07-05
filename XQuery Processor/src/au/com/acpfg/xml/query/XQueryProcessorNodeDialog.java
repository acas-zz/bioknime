package au.com.acpfg.xml.query;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.codec.binary.Base64;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.com.acpfg.xml.query.XMLQueryEntry.ResultsType;
import au.com.acpfg.xml.reader.XMLCell;

/**
 * <code>NodeDialog</code> for the "XMLreader" Node.
 * Provides an XPath knime api & XML "blob" cell type and data processing. Useful for many life science XML formats (PepXML, ProtXML, BLAST XML etc. etc.)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class XQueryProcessorNodeDialog extends DefaultNodeSettingsPane {
	
    /**
	 * The currently selected XMLQueryEntry
	 */
	private XMLQueryEntry m_cur_edit;
	private int m_cur_idx;
	private final JList query_list;			// contains a custom model which can serialise the XMLQueryEntry's for KNIME

	
    /**
     * New pane for configuring XMLreader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected XQueryProcessorNodeDialog() {
        super();
        
        m_cur_edit = null;
        m_cur_idx  = -1;
         
        final JPanel xqueries_panel = new JPanel();
        xqueries_panel.setLayout(new BorderLayout());
        final JPanel button_panel = new JPanel();
        button_panel.setLayout(new GridLayout(5,1));
        MyQueryEntryListModel mdl = new MyQueryEntryListModel();
        query_list = new JList(mdl);
    
        query_list.setPreferredSize(new Dimension(300,200));
        final JButton b_add = new JButton("Add");
        final JButton b_load_template = new JButton("Load template...");
        b_load_template.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String filename = choose_template();
				if (filename != null) {
					load_queries(query_list, filename, false);
				}
			}
        	
        });
        final JButton b_append_template = new JButton("Append template...");
        b_append_template.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent arg0) {
        		String filename = choose_template();
        		if (filename != null) {
        			load_queries(query_list, filename, true);
        		}
        	}
        });
        final JButton b_save_template = new JButton("Save template...");
        b_save_template.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String filename = save_template();
				if (filename != null) {
					save_queries(query_list, filename);
				}
			}
        	
        });
        final JTextArea t_query = new JTextArea(5,80);
        final JTextField t_name = new JTextField(20);
        final JList t_result_types = new JList(XMLQueryEntry.rt2items());
        t_result_types.setVisibleRowCount(5);
        t_result_types.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JCheckBox t_fail_empty = new JCheckBox("Abort iff no match");
        final JCheckBox t_enabled = new JCheckBox("Enabled?");

        query_list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				Object sel = query_list.getSelectedValue();
				m_cur_idx  = query_list.getSelectedIndex();

				if (sel == null) {
					m_cur_edit = null;
					m_cur_idx = -1;
					return;
				}
				XMLQueryEntry xqe = (XMLQueryEntry) sel;
				m_cur_edit = xqe;
				t_name.setText(xqe.getName());
				t_query.setText(xqe.getQuery());
				t_fail_empty.setSelected(xqe.getFailEmpty());
				t_enabled.setSelected(xqe.isEnabled());
				t_result_types.setSelectedIndices(XMLQueryEntry.rt2idx(xqe.getWantedResults()));
			}
        	
        });
        b_add.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				MyQueryEntryListModel lm = (MyQueryEntryListModel) query_list.getModel();
				if (lm != null)
					lm.add(new XMLQueryEntry(lm.getSize()+1));
			}
        	
        });
    
        final JButton b_load_builtin = new JButton("Load Builtin Template");
        b_load_builtin.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				new BuiltinTemplateListDialog();
			}
        });
        final JButton b_remove = new JButton("Remove");
        b_remove.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				MyQueryEntryListModel lm = (MyQueryEntryListModel) query_list.getModel();
				Object cur = query_list.getSelectedValue();
				if (cur == null || lm == null)
					return;
				lm.remove((XMLQueryEntry)cur);
				if (cur == m_cur_edit) {
					m_cur_edit = null;
					m_cur_idx  = -1;
				}
			}
        	
        });
        button_panel.add(b_add);
        button_panel.add(b_load_builtin);
        button_panel.add(b_load_template);
        button_panel.add(b_append_template);
        button_panel.add(b_save_template);
        button_panel.add(b_remove);
        query_list.setCellRenderer(new MyQueryEntryRenderer());
        xqueries_panel.add(new JScrollPane(query_list), BorderLayout.CENTER);
        xqueries_panel.add(button_panel, BorderLayout.WEST);
        final JPanel text_panel = new JPanel();
        text_panel.setBorder(BorderFactory.createTitledBorder("Edit XQuery"));
        text_panel.setLayout(new BorderLayout());
        final JPanel left_panel = new JPanel();
        left_panel.setLayout(new BorderLayout());
        left_panel.add(t_name, BorderLayout.NORTH);
        
        t_name.getDocument().addDocumentListener(new DocumentListener() {

        	private void do_update(Document doc) {
        		try {
        			if (m_cur_edit != null && doc != null) {
        				m_cur_edit.setName(doc.getText(0, doc.getLength()));
        				query_list.repaint();
        			}
        		} catch (Exception e) {
        			// silent
        		}
        	}
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				do_update(arg0.getDocument());
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				do_update(arg0.getDocument());				
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				do_update(arg0.getDocument());
			}
        	
        });
        final JPanel checkbox_panel = new JPanel();
        checkbox_panel.setLayout(new GridLayout(2,1));
        checkbox_panel.add(t_fail_empty);
        checkbox_panel.add(t_enabled);
        t_enabled.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (m_cur_edit != null) {
					m_cur_edit.setEnabled(t_enabled.isSelected());
    				query_list.repaint();
				}
			}
        	
        });
        t_fail_empty.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (m_cur_edit != null) {
					m_cur_edit.setFailEmpty(t_fail_empty.isSelected());
					query_list.repaint();
				}
			}
        	
        });
        left_panel.add(checkbox_panel, BorderLayout.CENTER);
        left_panel.add(t_result_types, BorderLayout.SOUTH);
        t_result_types.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				ResultsType[] sel = XMLQueryEntry.item2rt(t_result_types.getSelectedValues());
				if (m_cur_edit != null) {
					m_cur_edit.setResults(sel);
					query_list.repaint();		
				}
			}
        	
        });
        text_panel.add(left_panel, BorderLayout.WEST);
        text_panel.add(new JScrollPane(t_query), BorderLayout.CENTER);
        t_query.getDocument().addDocumentListener(new DocumentListener() {

        	private void do_update(Document doc) {
        		try {
        			if (m_cur_edit != null && doc != null) {
        				m_cur_edit.setQuery(doc.getText(0, doc.getLength()));
        				query_list.repaint();
        			}
				} catch (BadLocationException e) {
					// be silent (should not normally occur)
				}
        	}
        	
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				do_update(arg0.getDocument());
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				do_update(arg0.getDocument());
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				do_update(arg0.getDocument());
			}
        	
        });
        xqueries_panel.add(text_panel, BorderLayout.SOUTH);
        
        this.removeTab("Options");
        this.addTabAt(0, "XQueries", xqueries_panel);
        createNewTab("Data Source");
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(XQueryProcessorNodeModel.CFGKEY_XML_COL, "XML Data"), "XML Data Column", 0, new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				boolean result = colSpec.getType().equals(XMLCell.TYPE);
				return result;
			}

			@Override
			public String allFilteredMsg() {
				return "No XML columns (please use an XML Reader node to load the data)!";
			}
        	
        }));
    }
  
    
    protected static String choose_template() {
    	JFileChooser jfc = new JFileChooser();
    	jfc.addChoosableFileFilter(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				if (arg0.isDirectory() || arg0.getAbsolutePath().toLowerCase().endsWith(".xmlr")) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return "XML Query Templates (.xmlr)";
			}
    		
    	});
    	jfc.setDialogTitle("Select an XML query template...");
    	int action = jfc.showOpenDialog(null);
    	if (action == JFileChooser.APPROVE_OPTION && jfc.getSelectedFile() != null) {
    		return jfc.getSelectedFile().getAbsolutePath();
    	}
    	return null;
    }
    
    protected static void load_queries(JList query_list, String filename, boolean append) {
    	ListModel lm = query_list.getModel();
    	
    	try {
    		if (!append) {
        		lm = new MyQueryEntryListModel();
        		query_list.setModel(lm);
        	}
    		
    		BufferedReader br = new BufferedReader(new FileReader(filename));
    		String line;
    		MyQueryEntryListModel qlm = (MyQueryEntryListModel) lm;
    		while ((line = br.readLine()) != null) {
    			byte[] vec = Base64.decodeBase64(line.getBytes());
    			String xqe_serialised = new String(vec);
    			if (xqe_serialised.trim().length() > 0) {
	    			XMLQueryEntry xqe = new XMLQueryEntry(xqe_serialised);
	    			qlm.add(xqe);
    			}
    		}
    		br.close();
    	} catch (Exception e) {
    		Logger.getAnonymousLogger().warning(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    protected static void save_queries(JList query_list, String filename) {
    	ListModel lm = query_list.getModel();
    	try {
    		
    	PrintWriter os = new PrintWriter(filename);
    	for (int i=0; i<lm.getSize(); i++) {
    		Object cur = lm.getElementAt(i);
    		if (cur instanceof XMLQueryEntry) {
    			String xqe_serialised = cur.toString();
    			byte[] vec = xqe_serialised.getBytes();
    			os.println(new String(Base64.encodeBase64(vec)));
    		}
    	}
    	
    	os.close();
    	} catch (Exception e) {
    		Logger.getAnonymousLogger().warning(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    protected static String save_template() {
    	JFileChooser jfc = new JFileChooser();
    	int action = jfc.showSaveDialog(null);
    	if (action == JFileChooser.APPROVE_OPTION && jfc.getSelectedFile() != null) {
    		String sel_file = jfc.getSelectedFile().getAbsolutePath();
    		if (!sel_file.endsWith(".xmlr")) {
    			sel_file += ".xmlr";
    		}
    		return sel_file;
    	}
    	return null;
    }
    
    @Override 
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException   {
		try {
			String[] vec = settings.getStringArray(XQueryProcessorNodeModel.CFGKEY_QUERIES);
	    	query_list.setModel(new MyQueryEntryListModel(vec));
		} catch (InvalidSettingsException e) {
			// use default constructor, rather than loaded from model
			e.printStackTrace();
			query_list.setModel(new MyQueryEntryListModel());
		}
    }
    
    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {  
    	ListModel lm = query_list.getModel();
    	if (lm instanceof MyQueryEntryListModel) {
    		MyQueryEntryListModel my_lm = (MyQueryEntryListModel) lm;
    		settings.addStringArray(XQueryProcessorNodeModel.CFGKEY_QUERIES, my_lm.getStringArrayValue());
    	} else {
    		throw new InvalidSettingsException("Unknown list model: cannot save state!");
    	}
    }
}

