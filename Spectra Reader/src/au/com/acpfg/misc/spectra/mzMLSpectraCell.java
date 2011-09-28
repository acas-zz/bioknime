package au.com.acpfg.misc.spectra;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Logger;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.systemsbiology.jrap.stax.Scan;

/**
 * A cell which works with mzXML/mzML data as read by the JRAP-stax library
 * Callers should use only the AbstractSpectraCell interface as this is supported
 * across all file-formats and libraries doing the loading. Code which relies on
 * a particular loader or format will break KNIME users.
 * 
 * @author andrew.cassin
 *
 */
public class mzMLSpectraCell extends AbstractSpectraCell {
	/**
	 * for serialisation
	 */
	private static final long serialVersionUID = -8852081501521274502L;
	
	
	private Scan m_scan;    // peak list
	private String m_id;	// spectrum id required by constructor
	private double m_mz_min, m_mz_max;
	
    /**
     * Dont invoke this constructor directly: use SpectraUtilityFactory.createCell() instead
     * 
     * @param s
     * @param id
     */
	public mzMLSpectraCell(Scan s, String id) {
		m_scan = s;
		m_id = id;
		double[] mz = s.getDoubleMassList();
		m_mz_min = Double.POSITIVE_INFINITY;
		m_mz_max = Double.NEGATIVE_INFINITY;
		for (int i=0; i<mz.length; i++) {
			if (mz[i] < m_mz_min)
				m_mz_min = mz[i];
			if (mz[i] > m_mz_max) 
				m_mz_max = mz[i];
		}
	}
	
	public static final Class<? extends DataValue> getPreferredValueClass() {
        return SpectralDataInterface.class;
    }
    
	/*
    public static final DataCellSerializer<mzMLSpectraCell> getCellSerializer() {
        return new DataCellSerializer() {
          
			@Override
			public DataCell deserialize(DataCellDataInput input)
					throws IOException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void serialize(DataCell cell, DataCellDataOutput output)
					throws IOException {
				// TODO Auto-generated method stub
				
			}
        };
    }*/

	@Override
	protected boolean equalsDataCell(DataCell dc) {
		return (this == dc);
	}

	@Override
	public int hashCode() {
		// same as hashcode for the scan instance
		return m_scan.hashCode();
	}

	/*@Override
	public String toString() {
		return asString(false);
	}*/

	@Override
	public mzMLSpectraCell getMyValue() {
		return this;
	}

	/**
	 * NB: mz values are round to three decimal places if round is true, no rounding otherwise
	 */
	public String asString(boolean round) {
		StringBuilder sb = new StringBuilder();
		double[] mz = getMZ();
		double[] intensity = getIntensity();
		assert (mz.length == intensity.length);
		for (int i=0; i<getNumPeaks(); i++) {
			if (round) {
				sb.append(Math.round(mz[i] * 1000.0) / 1000.0);
			} else {
				sb.append(mz[i]);
			}
			sb.append(' ');
			sb.append(intensity[i]);
			sb.append('\n');
		}
		return sb.toString();
	}

	@Override
	public String getID() {
		return m_id;
	}

	@Override
	public int getNumPeaks() {
		return m_scan.getHeader().getPeaksCount();
	}
	
	@Override
	public int getMSLevel() {
		return m_scan.getHeader().getMsLevel();
	}
	
	@Override
	public double[] getIntensity() {
		return m_scan.getDoubleIntensityList();
	}

	@Override
	public double[] getMZ() {
		return m_scan.getDoubleMassList();
	}

	@Override
	public double getMaxMZ() {
		return m_mz_max;
	}

	@Override
	public double getMinMZ() {
		return m_mz_min;
	}

}
