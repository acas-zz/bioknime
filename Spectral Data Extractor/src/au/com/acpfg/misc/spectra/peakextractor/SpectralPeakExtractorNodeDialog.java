package au.com.acpfg.misc.spectra.peakextractor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

import au.com.acpfg.misc.spectra.SpectralDataInterface;



/**
 * <code>NodeDialog</code> for the "SpectralPeakExtractor" Node.
 * Extracts data defining a peak from any cell supporting SpectralDataInterface (defined in the SpectraReader node)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SpectralPeakExtractorNodeDialog extends NodeDialogPane {

	    /** The node logger for this class. */
	    private static final NodeLogger LOGGER = NodeLogger
	            .getLogger(SpectralPeakExtractorNodeDialog.class);

	    /** List of numeric columns. */
	    private final JList m_spectraList;

	    /** The spectra columns' model. */
	    private final DefaultListModel m_spectraMdl;

	    /** Keeps shows the currently selected peaks */
	    private final JPanel m_peaks_panel;

	    /** Keeps column data cell to interval panel settings. */
	    private final LinkedHashMap<String, PeakPanel> m_peaks;

	    /**
	     * Creates a new peak extraction dialog.
	     */
	    SpectralPeakExtractorNodeDialog() {
	        super();
	        m_peaks = new LinkedHashMap<String, PeakPanel>();

	        // peak panel in tab
	        final JPanel peakPanel = new JPanel(new GridLayout(1, 1));

	        // spectral column list
	        m_spectraMdl = new DefaultListModel();
	        m_spectraMdl.addElement("<empty>");
	        m_spectraList = new JList(m_spectraMdl);
	        
	        /**
	         * Override renderer to plot number of defined bins.
	         */
	        class SpectraListCellRenderer extends DataColumnSpecListCellRenderer {
	            /**
	             * {@inheritDoc}
	             */
	            @Override
	            public Component getListCellRendererComponent(final JList list,
	                    final Object value, final int index,
	                    final boolean isSelected, final boolean cellHasFocus) {
	                Component c = super.getListCellRendererComponent(list, value,
	                        index, isSelected, cellHasFocus);
	                String name = ((DataColumnSpec)value).getName();
	                PeakPanel p = m_peaks.get(name);
	                if (p != null) {
	                    int bins = p.getNumPeaks();
	                    if (bins > 0) {
	                        String text = getText() + " (";
	                        if (bins == 1) {
	                            text += bins + " peaks defined)";
	                        } else {
	                            text += bins + " peaks defined)";
	                        }
	                     
	                    }
	                }
	                return c;
	            }

	        }
	        m_spectraList.setCellRenderer(new SpectraListCellRenderer());
	        m_spectraList.addListSelectionListener(new ListSelectionListener() {
	            /**
	             * 
	             */
	            public void valueChanged(final ListSelectionEvent e) {
	                columnChanged();
	                peakPanel.validate();
	                peakPanel.repaint();
	            }
	        });
	        m_spectraList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	        final JScrollPane spectraScroll = new JScrollPane(m_spectraList);
	        spectraScroll.setMinimumSize(new Dimension(300, 155));
	        spectraScroll
	                .setBorder(BorderFactory.createTitledBorder(" Suitable Columns "));

	        // peaks to extract (they are permitted to overlap)
	        m_peaks_panel = new JPanel(new GridLayout(1, 1));
	        m_peaks_panel.setBorder(BorderFactory.createTitledBorder(" Peaks to be extracted "));
	        m_peaks_panel.setMinimumSize(new Dimension(350, 300));
	        m_peaks_panel.setPreferredSize(new Dimension(350, 300));
	        JSplitPane split = new JSplitPane(
	                JSplitPane.HORIZONTAL_SPLIT, spectraScroll, m_peaks_panel);
	        peakPanel.add(split);
	        super.addTab(" Select Spectra Column ", peakPanel);
	    }

	    private void columnChanged() {
	        m_peaks_panel.removeAll();
	        Object o = m_spectraList.getSelectedValue();
	        if (o == null) {
	            m_peaks_panel.setBorder(BorderFactory
	                    .createTitledBorder(" Select Peak "));
	        } else {
	            m_peaks_panel.setBorder(null);
	            m_peaks_panel.add(createPeakPanel((DataColumnSpec)o));
	        }
	    }

	    private PeakPanel createPeakPanel(final DataColumnSpec cspec) {
	        String name = cspec.getName();
	        PeakPanel p;
	        if (m_peaks.containsKey(name)) {
	            p = m_peaks.get(name);
	        } else {
	            p = new PeakPanel(name, m_spectraList, m_peaks_panel, cspec.getType());
	            m_peaks.put(name, p);
	        }
	        p.validate();
	        p.repaint();
	        return p;
	    }

	    /**
	     * @param settings to read intervals from
	     * @param specs The input table spec
	     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
	     * @throws NotConfigurableException if the spec contains no columns
	     */
	    @Override
	    protected void loadSettingsFrom(final NodeSettingsRO settings,
	            final DataTableSpec[] specs) throws NotConfigurableException {
	        // numeric columns' settings
	        m_peaks.clear();
	        m_spectraMdl.removeAllElements();
	        for (int i = 0; i < specs[0].getNumColumns(); i++) {
	            DataColumnSpec cspec = specs[0].getColumnSpec(i);
	            //LOGGER.info(cspec.getType());
	            if (cspec.getType().isCompatible(SpectralDataInterface.class)) {
	                m_spectraMdl.addElement(cspec);
	            }
	        }
	        // no column found for peak
	        if (m_spectraMdl.getSize() == 0) {
	            throw new NotConfigurableException(
	                    "No suitable column found to define peaks.");
	        }
	        String[] columns = settings.getStringArray(
	                SpectralPeakExtractorNodeModel.SPECTRA_COLUMNS, (String[])null);
	        
	        // if numeric columns in settings, select first
	        if (columns != null && columns.length > 0) {
	            for (int i = 0; i < columns.length; i++) {
	                if (!specs[0].containsName(columns[i])) {
	                    continue;
	                }
	                NodeSettingsRO col;

	                DataType type = specs[0].getColumnSpec(columns[i]).getType();
	                try {
	                	// TODO... always throws... maybe model bug?
	                    col = settings.getNodeSettings(columns[i]);
	                } catch (InvalidSettingsException ise) {
	                    LOGGER.warn("NodeSettings not available for column: "
	                            + columns[i]+" "+ise.getMessage());
	                    continue;
	                }
	            
	                PeakPanel p = new PeakPanel(columns[i], m_spectraList, m_peaks_panel, type);
	                m_peaks.put(columns[i], p);
	                for (String peakId : col.keySet()) {
	                   //LOGGER.info("loading got peak "+peakId);
	                   try {
	                	   NodeSettingsRO pset = col.getNodeSettings(peakId);
	                	   
	                	   p.add(new PeakWindow(pset));
	                   } catch (InvalidSettingsException e) {
	                	   e.printStackTrace();
	                   }
	                }
	                DataColumnSpec cspec = specs[0].getColumnSpec(columns[i]);
	                // select column and scroll to position
	                m_spectraList.setSelectedValue(cspec, true);
	            }
	        }
	        getPanel().validate();
	        getPanel().repaint();
	    }

	    /**
	     * @param settings write intervals to
	     * @throws InvalidSettingsException if a bin name is empty
	     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
	     */
	    @Override
	    protected void saveSettingsTo(final NodeSettingsWO settings)
	            throws InvalidSettingsException {
	        LinkedHashSet<String> colList = new LinkedHashSet<String>();
	        for (String cell : m_peaks.keySet()) {
	            PeakPanel p = m_peaks.get(cell);
	            // only if at least 1 peak is defined
	            if (p.getNumPeaks() > 0) {
	            	//LOGGER.info("saving peaks for column: <"+cell+">");
	                colList.add(cell);
	                
	                NodeSettingsWO set = settings.addNodeSettings(cell);
	                for (int j=0; j<p.getNumPeaks(); j++) {
	                	PeakItemPanel peak = p.getPeak(j);
	                	String peak_name = peak.getName() + "_" + j;
	                	//LOGGER.info("saving "+peak_name);
	                	NodeSettingsWO peak_settings = set.addNodeSettings(peak_name);
	                	peak.saveToSettings(peak_settings);
	                }
	                
	            }
	        }
	        // save spectra columns (usually only a single column in the data, but who knows?)
	        String[] columns = colList.toArray(new String[0]);
	        //LOGGER.info(columns.length+" "+columns[0]);
	        settings.addStringArray(SpectralPeakExtractorNodeModel.SPECTRA_COLUMNS, columns);
	  }
}	



