package au.com.acpfg.misc.spectra.peakextractor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;

import au.com.acpfg.misc.spectra.peakextractor.PeakPanel;


/**
 * Creates a new panel holding one interval.
 */
final class PeakItemPanel extends JPanel {
    private final PeakPanel m_parent;
    private final JSpinner m_left;
    private final JSpinner m_right;
    private final JTextField m_mz, m_name;
    private PeakWindow m_pw;

    /**
     * @param parent the interval item's parent component
     * @param left initial left value
     * @param right initial right value
     * @param bin the name for this bin
     * @param type the column type of this interval
     */
    PeakItemPanel(final PeakPanel parent, final PeakWindow pw) {
        this(parent);
        assert(pw != null);
        
        m_pw = pw;
        update();
        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(m_name, BorderLayout.WEST);
        p1.add(m_mz, BorderLayout.CENTER);
        p1.add(new JLabel(" :  "), BorderLayout.EAST);
        super.add(p1);
        JPanel p2 = new JPanel(new BorderLayout());
        p2.add(m_left, BorderLayout.CENTER);
        p2.add(new JLabel(" Left Tol."), BorderLayout.WEST);
        super.add(p2);
        JPanel p3 = new JPanel(new BorderLayout());
        p3.add(new JLabel(" Right Tol. "), BorderLayout.WEST);
        p3.add(m_right, BorderLayout.CENTER);
        super.add(p3);
        this.setPreferredSize(new Dimension(150,25));
        
        initListener();
    }

    /*
     * @param parent the interval item's parent component
     */
    PeakItemPanel(final PeakPanel parent) {
        super(new GridLayout(1, 0));
        m_parent = parent;

        m_pw = null;
       
        m_mz   = new JTextField();
        m_name = new JTextField();
        
        m_name.setPreferredSize(new Dimension(50, 25));
        m_mz.setPreferredSize(new Dimension(50, 25));

        SpinnerNumberModel my_model = new SpinnerNumberModel(0.05, 0, 10000.0, 0.1);
        m_left = new JSpinner(my_model);
        JSpinner.DefaultEditor editorLeft = 
            new JSpinner.NumberEditor(m_left, "0.00#############");
        editorLeft.getTextField().setColumns(15);
       
        m_left.setEditor(editorLeft);
        m_left.setPreferredSize(new Dimension(125, 25));

        my_model = new SpinnerNumberModel(0.05, 0, 10000.0, 0.1);
        m_right = new JSpinner(my_model);
        JSpinner.DefaultEditor editorRight = 
            new JSpinner.NumberEditor(m_right, "0.00#############");
        editorRight.getTextField().setColumns(15);
        m_right.setEditor(editorRight);
        m_right.setPreferredSize(new Dimension(125, 25));

    }
    
    protected void update() {
    	   m_mz.setText(m_pw.getMZasString());
    	   m_name.setText(m_pw.getName());
    	   m_left.setValue(m_pw.getLeft());
    	   m_right.setValue(m_pw.getRight());
    }

    private void initListener() {
        m_left.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                double val = getLeftValue(true);
                m_pw.setLeft(val);
                m_parent.repaint();
            }
        });
        final JSpinner.DefaultEditor editorLeft = 
            (JSpinner.DefaultEditor)m_left.getEditor();
        editorLeft.getTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                getLeftValue(true);
            }

            @Override
            public void focusGained(final FocusEvent e) {
            }
        });

        m_right.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                m_pw.setRight(getRightValue(true));
                m_parent.repaint();
            }
        });
        final JSpinner.DefaultEditor editorRight = 
            (JSpinner.DefaultEditor)m_right.getEditor();
        editorRight.getTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                getRightValue(true);
            }

            @Override
            public void focusGained(final FocusEvent e) {
            }
        });


        m_mz.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
                myRepaint();
            }

            public void insertUpdate(final DocumentEvent e) {
                changedUpdate(e);
            }

            public void removeUpdate(final DocumentEvent e) {
                changedUpdate(e);
            }
        });

        m_name.getDocument().addDocumentListener(new DocumentListener() {
        	public void changedUpdate(final DocumentEvent e) {
        		myRepaint();
        	}
        	
        	public void insertUpdate(final DocumentEvent e) {
        		changedUpdate(e);
        	}
        	
        	public void removeUpdate(final DocumentEvent e) {
        		changedUpdate(e);
        	}
        });
    }

    @Override
    public String getName() {
    	return m_name.getText().trim();
    }
    
    /**
     * @return the name for this interval bin
     */
    public String getMZ() {
        return m_mz.getText().trim();
    }
    
    public Double getMZasDouble() throws InvalidSettingsException {
    	try {
    		Double d = Double.parseDouble(getMZ());
    		return d;
    	} catch (Exception e) {
    		throw new InvalidSettingsException(e.getMessage());
    	}
    }

    private void myRepaint() {
        m_parent.validate();
        m_parent.repaint();
    }
    
    /**
     * @return left value
     * @param commit if the value has to be committed first
     */
    public double getLeftValue(final boolean commit) {
        if (commit) {
            double old = ((Number)m_left.getValue()).doubleValue();
            try {
                m_left.commitEdit();
            } catch (ParseException pe) {
                return old;
            }
        }
        return ((Number)m_left.getValue()).doubleValue();
    }

    /**
     * @return right value
     * @param commit if the value has to be committed first
     */
    public double getRightValue(final boolean commit) {
        if (commit) {
            double old = ((Number)m_right.getValue()).doubleValue();
            try {
                m_right.commitEdit();
            } catch (ParseException pe) {
                return old;
            }
        }
        
        return ((Number)m_right.getValue()).doubleValue();
    }

    /**
     * @return string containing left and right border, and open/not open
     */
    @Override
    public String toString() {
    	String leftString, rightString;
    	
    	double left = getLeftValue(false);
    	double right = getRightValue(false);
    	
        JComponent editor = m_left.getEditor();
        if (editor instanceof JSpinner.NumberEditor) {
            JSpinner.NumberEditor numEdit = (JSpinner.NumberEditor)editor;
            leftString = numEdit.getFormat().format(left);
            rightString = numEdit.getFormat().format(right);
        } else {
            leftString = Double.toString(left);
            rightString = Double.toString(right);
        }
        
        PeakWindow pw = new PeakWindow(getName(), getMZ(), leftString, rightString);
        return pw.toString();
    }

	public void saveToSettings(final NodeSettingsWO peak_settings) throws InvalidSettingsException {
		assert(peak_settings != null);
		new PeakWindow(getName(), getMZasDouble(), getLeftValue(true), getRightValue(true)).saveToSettings(peak_settings);
	}

}