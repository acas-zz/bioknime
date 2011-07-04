package au.com.acpfg.io.pngwriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageBlobCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of PNGImageWriter.
 * Writes PNGImageCell's to disk as separate files, based on user configuration. Ideal for saving graphical results from other nodes to files which can then be edited...
 *
 * @author http://www.plantcell.unimelb.edu.au
 */
public class PNGImageWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(PNGImageWriterNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_FOLDER       = "folder-to-save-to";
	static final String CFGKEY_FILENAME_COL = "filename-column";
	static final String CFGKEY_PNG_COL      = "png-image-column";
	
    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_folder = new SettingsModelString(CFGKEY_FOLDER, "c:/temp");
    private final SettingsModelColumnName m_filename_col = new SettingsModelColumnName(CFGKEY_FILENAME_COL, "");
    private final SettingsModelString m_png_col = new SettingsModelString(CFGKEY_PNG_COL, "");
    

    /**
     * Constructor for the node model.
     */
    protected PNGImageWriterNodeModel() {
         super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int n_rows   = inData[0].getRowCount();
    	File folder  = new File(m_folder.getStringValue()); 
    	int png_col_idx = inData[0].getDataTableSpec().findColumnIndex(m_png_col.getStringValue());
    	if (png_col_idx < 0) {
    		throw new Exception("Cannot find column: "+m_png_col.getStringValue()+", re-configure the node?");
    	}
    	boolean use_row_id = m_filename_col.useRowID();
    	int filename_idx = inData[0].getDataTableSpec().findColumnIndex(m_filename_col.getStringValue());
    	if (!use_row_id && filename_idx < 0) {
    		throw new Exception("Cannot find column: "+m_filename_col.getStringValue()+", re-configure the node?");
    	}
    	RowIterator it = inData[0].iterator();
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		DataCell png_cell = r.getCell(png_col_idx);
    		if (png_cell == null || png_cell.isMissing())
    			continue;
    		String fname;
    		if (use_row_id) {
    			fname = r.getKey().getString();
    		} else {
	    		DataCell fname_cell = r.getCell(filename_idx);
	    		if (fname_cell == null || fname_cell.isMissing()) {
	    			logger.warn("Ignoring row "+r.getKey().getString()+" as it has no filename");
	    			continue;
	    		}
	    		
	    		fname = fname_cell.toString();
    		}
    		if (!fname.toLowerCase().endsWith(".png")) {
    			fname += ".png";
    		}
    		byte[] img_bytes;
    		if (png_cell instanceof PNGImageBlobCell) {
    			PNGImageBlobCell  image_cell = (PNGImageBlobCell) png_cell;
        		img_bytes = image_cell.getImageContent().getByteArray();
    		} else if (png_cell instanceof PNGImageCell) {
    			PNGImageCell image_cell = (PNGImageCell) png_cell;
    			img_bytes = image_cell.getImageContent().getByteArray();
    		} else {
    			throw new Exception("Unknown image type: "+png_cell);
    		}
    		
    		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(m_folder.getStringValue(), fname)));
    		bos.write(img_bytes);
    		bos.close();
    		img_bytes = null;
    	}
        return new BufferedDataTable[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_folder.saveSettingsTo(settings);
    	m_filename_col.saveSettingsTo(settings);
    	m_png_col.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
     	m_folder.loadSettingsFrom(settings);
    	m_filename_col.loadSettingsFrom(settings);
    	m_png_col.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_folder.validateSettings(settings);
    	m_filename_col.validateSettings(settings);
    	m_png_col.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

}

