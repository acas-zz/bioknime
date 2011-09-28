package au.com.acpfg.misc.jemboss.settings;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.local.ProgramSettingsListener;

/**
 * Implements a widget for sequence related data eg. ACD type sequence or seqall. Subclasses
 * may provide an implementation for output of sequence data eg. outseq, outseqall and so on...
 * 
 * @author andrew.cassin
 *
 */
public class SequenceSetting extends StringSetting {
	private static final int FASTA_LINE_LENGTH = 80;		// how many chars per line in FASTA files
	
	// persisted state
	private boolean m_from_column = true;
	private final JCheckBox m_ignore = new JCheckBox("ignore?");
	
	public SequenceSetting(HashMap<String,String> attrs) {
		super(attrs);	// WILL save the value eg. column name
		if (hasAttribute("from-column?")) {
			m_from_column = new Boolean(attrs.get("from-column?")).booleanValue();
		} else {
			m_from_column = false;
		}
		if (hasAttribute("ignore?")) {
			m_ignore.setSelected(new Boolean(getAttributeValue("ignore?")));
		} else {
			m_ignore.setSelected(false);
		}
	}
	
	@Override
	public boolean isInputFromColumn() {
		if (m_ignore.isSelected())
			return false;
		return m_from_column;
	}

	@Override
	public String getColumnName() {
		if (isInputFromColumn())
			return getValue();
		return null;
	}

	/**
	 * Supports the <code>nullok</code> attribute where a setting may be omitted from the emboss invocation.
	 * 
	 */
	@Override
	public JComponent make_widget(DataTableSpec dt) {
		String t = getType();
		JPanel ret = new JPanel();
		ret.setBorder(BorderFactory.createEmptyBorder());
		ret.setLayout(new FlowLayout());
		
		if (t.equals("sequence")) {
			ColumnSelectionPanel csp = make_col_panel(dt);
			m_from_column = true;
			setValue(csp.getSelectedColumn());
			csp.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					Object o = ((JComboBox)arg0.getSource()).getSelectedItem();
					if (o instanceof DataColumnSpec) {
						setValue(((DataColumnSpec)o).getName());
					}
				}
				
			});
			ret.add(csp);
		} else {
			ret.add(make_sequence_panel(dt));
		}
		
		
		boolean do_ignore = hasAttribute("nullok") && getAttributeValue("nullok").toLowerCase().startsWith("y");
		if (do_ignore) {
			ret.add(Box.createRigidArea(new Dimension(5,5)));
			m_ignore.setSelected(true);
			ret.add(m_ignore);
		}
		return ret;
	}
	
	/**
	 * Make a widget which permits the user to choose the KNIME column to take data from. The
	 * widget responds to default values and records the changes in the column to the specified ProgramSetting
	 * @param dt - tablespec which provides the initial list of suitable columns
	 * @return
	 */
	private ColumnSelectionPanel make_col_panel(DataTableSpec dt) {
        final ArrayList<String> ok_cols = new ArrayList<String>();
        
		ColumnSelectionPanel csp = new ColumnSelectionPanel((Border)null, new ColumnFilter() {
			
			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				boolean ok = (colSpec.getType().isCompatible(StringValue.class));
				if (ok) {
					//HACK BUG: reliant on column traversal order...
					ok_cols.add(colSpec.getName());
				}
				return ok;
			}

			@Override
			public String allFilteredMsg() {
				return "ERROR: no suitable String columns available!";
			}
			
		}, false, false);
	
		String default_value = getDefaultValue();
		if (hasAttribute("value") && isInputFromColumn()) {
			default_value = getValue();
		}
		
		try {
			if (dt != null) {
				csp.update(dt, "");
				if (csp.getNrItemsInList() > 0) {
					if (default_value.length() > 0) {
						int idx = ok_cols.indexOf(default_value);
						if (idx < 0)
							idx = 0;
						csp.setSelectedIndex(idx);
					} else {
						// last (ie. most recent) column is selected
						csp.setSelectedIndex(csp.getNrItemsInList()-1);
					}
				}
			}
		} catch (NotConfigurableException nce) {
			nce.printStackTrace();
		}
		return  csp;
	}

	/**
	 * Returns a "sequence panel" which lets the user elect to provide data from a file (FASTA) or
	 * from an input column (KNIME table input). Provides all the logic to handle default values and
	 * updating of the setting as the user changes it.
	 * 
	 * @param dt Input DataTableSpec to the node (used to display the required columns for the user)
	 * @return
	 */
	private JPanel make_sequence_panel(DataTableSpec dt) {
		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS));
		String          where = isInput() ? "from" : "to";
		final JRadioButton b1 = new JRadioButton(where+" file");
		final JRadioButton b2 = new JRadioButton(where+" column");
		final JButton      open_file_button = new JButton("   Select File...   ");
		
		final ColumnSelectionPanel csp = make_col_panel(dt);		// csp is always constructed but not always added (eg. output settings)
		if (! m_from_column) {
			File f = new File(getValue());
			if (!f.exists() || !f.canRead()) {
				Logger.getAnonymousLogger().warning("File '"+f.getName()+"' is not accessible anymore. Re-configure.");
			} else {
				open_file_button.setText(f.getName());
			}
			b1.setSelected(true);
			b2.setSelected(false);
			csp.setEnabled(false);
			open_file_button.setEnabled(true);
		} else {
			b1.setSelected(false);
			b2.setSelected(true);
			csp.setEnabled(true);
			open_file_button.setEnabled(false);
		}
	
		open_file_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
					JFileChooser fc = new JFileChooser();
					int returnVal = fc.showSaveDialog(open_file_button);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File f = fc.getSelectedFile();
						m_from_column = false;
					    setValue(f.getAbsolutePath());
					    open_file_button.setText(f.getName());
					}
			}
		});
		
		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JRadioButton b = (JRadioButton) e.getSource();
				boolean is_b1 = b.equals(b1);	// file radio button?
				m_from_column = !is_b1;
				b1.setSelected(is_b1);
				b2.setSelected(!is_b1);
				if (is_b1) {
					open_file_button.setEnabled(true);
					if (csp != null)
						csp.setEnabled(false);
				} else {
					if (csp != null)
						csp.setEnabled(true);
					open_file_button.setEnabled(false);
				}
			}
			
		};
		b1.addActionListener(al);
		jp.add(b1);
		jp.add(open_file_button);
		b2.addActionListener(al);
		jp.add(b2);

		// csp is only added if the user must be given the choice ie. input setting
		if (isInput()) {
			csp.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					Object o = ((JComboBox)arg0.getSource()).getSelectedItem();
					if (o instanceof DataColumnSpec) {
						setValue(((DataColumnSpec)o).getName());
					}
				}
				
			});
			jp.add(csp);
		}
		
		return jp;
	}
	
	@Override
	public void marshal(String id, DataCell c, PrintWriter fw) 
					throws IOException, InvalidSettingsException {
		fw.println(">"+id);
		if (c instanceof StringCell) {
			StringCell sc = (StringCell) c;
			char[]    seq = sc.getStringValue().toCharArray();
			for (int offset=0; offset < seq.length; offset += FASTA_LINE_LENGTH) {
				int len = seq.length - offset;
				if (len > FASTA_LINE_LENGTH)
					len = FASTA_LINE_LENGTH;
				char[] out = new char[len];
				System.arraycopy(seq, offset, out, 0, len);
				fw.println(out);
			}
		}
	}
	
	@Override
	public void copy_attributes(HashMap<String,String> attrs) {
		super.copy_attributes(attrs);
		attrs.put("from-column?", new Boolean(m_from_column).toString());
		attrs.put("ignore?", new Boolean(m_ignore.isSelected()).toString());
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws InvalidSettingsException,IOException {
	    String t = getType();
	    
	    // input-by-file specified but no file chosen?
	    String v = getValue();
	    
	    if (!m_from_column && (v== null  || v.length()<1))
	    	throw new InvalidSettingsException("No file chosen for: "+getName());
	    
	    // if ignore is chosen, it does not appear on the emboss command line... so...
	    if (m_ignore.isSelected())
	    	return;
	    
	    if (t.equals("sequence") || t.equals("seqall")) {
	    	File f = File.createTempFile("infile", ".fasta");
	    	l.addInputFileArgument(this, "-"+getName(), f);
	    } else if (t.equals("outseq") || t.equals("seqoutall") || t.equals("seqoutseq")) {
	    	// HACK BUG: "cast" this to OutputFileSetting since it is the most appropriate now
	    	OutputFileSetting ops = new OutputFileSetting(this.getAttributes());
	    	ops.getArguments(l);
	    } else {
	    	throw new InvalidSettingsException("Invalid argument type: "+t+" for "+getName());
	    }
	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("sequence") || acd_type.equals("seqall") ||  
				acd_type.equals("outseq") || acd_type.equals("seqoutall") || 
			    acd_type.equals("seqoutseq") || acd_type.equals("seqout")) {
			return true;
		}
		return false;
	}
}
