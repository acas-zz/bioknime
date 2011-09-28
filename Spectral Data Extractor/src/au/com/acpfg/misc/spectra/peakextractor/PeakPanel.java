package au.com.acpfg.misc.spectra.peakextractor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataType;


/**
 * Creates new panel holding one peak column. 
 */
class PeakPanel extends JPanel {
    /** List of peaks. */
    private final JList m_peakList;

    /** The intervals' model. */
    private final DefaultListModel m_peakMdl;

    private Component m_parent, m_peak_panel;
    
    /**
     * Create new interval panel.
     * 
     * @param column the current column name
     * @param appendColumn if a new peak column is append, otherwise the
     *            column is replaced
     * @param parent used to refresh column list is number of bins has
     *            changed
     * @param type the type for the spinner model
     * 
     */
    PeakPanel(final String column, final Component parent, final Component peak_parent, final DataType type) {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(" Peaks to be extracted "));
        m_peakMdl = new DefaultListModel();
        m_peakList = new JList(m_peakMdl);
        m_parent = parent;
        m_peak_panel = peak_parent;
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        m_peakList.setFont(font);
        final JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            
            public void actionPerformed(final ActionEvent e) {
            	int n = m_peakMdl.getSize() + 1;
                m_peakMdl.addElement(new PeakItemPanel(PeakPanel.this, new PeakWindow("Peak"+n, 0.0)));
                parent.validate();
                parent.repaint();
            }
        });
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            /**
             * 
             */
            public void actionPerformed(final ActionEvent e) {
                PeakItemPanel p = (PeakItemPanel)m_peakList
                        .getSelectedValue();
                if (p != null) {
                    int i = m_peakMdl.indexOf(p);
                    m_peakMdl.removeElement(p);
                    int size = m_peakMdl.getSize();
                    if (size > 0) {
                        if (size == 1 || size == i) {
                            m_peakList.setSelectedIndex(size - 1);
                        } else {
                            m_peakList.setSelectedIndex(i);
                        }
                      
                    }
                    parent.validate();
                    parent.repaint();
                }
            }
        });
        final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        super.add(buttonPanel, BorderLayout.NORTH);

        //
        // editing a peak
        //

        final JPanel selPeak = new JPanel(new GridLayout(1, 1));
        selPeak
                .add(new PeakItemPanel(this));
        selPeak.validate();
        selPeak.repaint();

        m_peakList
                .addListSelectionListener(new ListSelectionListener() {
                  
                    public void valueChanged(final ListSelectionEvent e) {
                        selPeak.removeAll();
                        Object o = m_peakList.getSelectedValue();
                        if (o == null) {
                            selPeak.add(new PeakItemPanel(PeakPanel.this, new PeakWindow("Peak1", 0.0)));
                        } else {
                            selPeak.add((PeakItemPanel)o);
                        }
                        m_peak_panel.validate();
                        m_peak_panel.repaint();
                    }
                });
        m_peakList
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JScrollPane peakScroll = new JScrollPane(m_peakList);
        peakScroll.setMinimumSize(new Dimension(200, 155));
        peakScroll.setPreferredSize(new Dimension(200, 155));
        super.add(peakScroll, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(selPeak, BorderLayout.CENTER);

        super.add(southPanel, BorderLayout.SOUTH);
    }


    /**
     * @return number of peaks specified for extraction of their data
     */
    public int getNumPeaks() {
        return m_peakMdl.getSize();
    }

    /**
     * @param i index for interval
     * @return the interval item
     */
    public PeakItemPanel getPeak(final int i) {
        return (PeakItemPanel)m_peakMdl.get(i);
    }

    /**
     * Appends the specified Peak to the list in the panel (at the end of the list)
     * @param peakWindow
     */
	public void add(PeakWindow peakWindow) {
		assert(peakWindow != null && m_peakMdl != null);
		m_peakMdl.addElement(new PeakItemPanel(this, peakWindow));
	}
}
