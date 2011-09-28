package au.com.acpfg.misc.spectra;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataValue;
import org.proteomecommons.io.Peak;

/**
 * Implements the supported data for an MGF-derived spectra, not all of this is visible
 * to users: as the renderers (currently) do not make use of some of the data. But eventually,
 * they will ;-)
 * 
 * @author andrew.cassin
 *
 */
public class MGFSpectraCell extends AbstractSpectraCell {
	/**
	 *  for serialisation
	 */
	private static final long serialVersionUID = 837426780080298388L;

	private MyMGFPeakList m_pl;
	
	private static final DataCellSerializer<MGFSpectraCell> SERIALIZER = new MGFSpectraCellSerializer();
	
	public MGFSpectraCell(MyMGFPeakList pl) {
		assert(pl != null);
		m_pl = pl;
	}
	
	public static final Class<? extends DataValue> getPreferredValueClass() {
        return SpectralDataInterface.class;
    }
	
	public static final DataCellSerializer<MGFSpectraCell> getCellSerializer() {
		return SERIALIZER;
    }

	@Override
	protected boolean equalsDataCell(DataCell dc) {
		return (this == dc);
	}
	
	@Override
	public String getID() {
		return m_pl.getTitle_safe();	// HACK: assumes title is unique!
	}

	@Override
	public String getCharge() {
		return m_pl.getCharge_safe();
	}
	
	@Override
	public double[] getIntensity() {
		if (getNumPeaks() == 0)
			return null;
		
		Peak[] p = m_pl.getPeaks();
		double[] ret = new double[p.length];
		for (int i=0; i<p.length; i++) {
			ret[i] = p[i].getIntensity();
		}
		return ret;
	}

	@Override
	public int getMSLevel() {
		return 2;		// TODO: no way to get this accurately!
	}

	@Override
	public double[] getMZ() {
		if (getNumPeaks() == 0)
			return null;
		
		Peak[] p = m_pl.getPeaks();
		double[] ret = new double[p.length];
		for (int i=0; i<p.length; i++) {
			ret[i] = p[i].getMassOverCharge();
		}
		return ret;
	}

	@Override
	public MGFSpectraCell getMyValue() {
		return this;
	}

	@Override
	public int getNumPeaks() {
		return m_pl.getNumPeaks();
	}

	@Override
	public int hashCode() {
		return m_pl.hashCode();
	}

	@Override
	public String asString(boolean round) {
		StringBuilder sb = new StringBuilder();
		Peak[] p = m_pl.getPeaks();
		for (int i=0; i<p.length; i++) {
			double mz = p[i].getMassOverCharge();
			if (round) {
				sb.append(Math.round(mz * 1000.0) / 1000.0);
			} else {
				sb.append(mz);
			}
			sb.append(' ');
			sb.append(p[i].getIntensity());
			sb.append('\n');
		}
		return sb.toString();
	}

	@Override
	public double getMaxMZ() {
		return m_pl.getMaxMZ();
	}

	@Override
	public double getMinMZ() {
		return m_pl.getMinMZ();
	}

	public String getPepmass() {
		return m_pl.getPepmass_safe();
	}
	
	/**
	 * Implement our own mechanism to persist MGF spectra objects, typically this is faster
	 * than using java.lang.Serializable but we do this not just for speed but for correct
	 * instantiation of the objects
	 * 
	 * @author andrew.cassin
	 *
	 */
	private static class MGFSpectraCellSerializer implements DataCellSerializer<MGFSpectraCell> {

		@Override
		public MGFSpectraCell deserialize(final DataCellDataInput input) throws IOException {
			MyMGFPeakList mgf = new MyMGFPeakList();
			
			// 1. load the peaklist
			int n_peaks = input.readInt();
			double[] mz = new double[n_peaks];
			double[] intensity = new double[n_peaks];
			for (int i=0; i<n_peaks; i++) {
				mz[i] = input.readDouble();
				intensity[i] = input.readDouble();
			}
			mgf.setPeaks(mz, intensity);
			
			// 2. load metadata
			String title = input.readUTF();
			mgf.setTitle(title);
			String pepmass = input.readUTF();
			mgf.setPepMass(pepmass);
			String charge = input.readUTF();
			mgf.setCharge(charge);
			int tc = input.readInt();
			mgf.setTandemCount(tc);
			
			return new MGFSpectraCell(mgf);
		}

		@Override
		public void serialize(final MGFSpectraCell spectra, final DataCellDataOutput output)
				throws IOException {
			if (spectra == null || output == null) 
				throw new IOException("Bad data given to MGFSpectraCellInitializer::serialize()");
			
			// 1. write output peaks (NB: same length arrays)
			int n_peaks = spectra.getNumPeaks();
			output.writeInt(n_peaks);
			double[] mz = spectra.getMZ();
			double[] intensity = spectra.getIntensity();
			for (int i=0; i<n_peaks; i++) {
				output.writeDouble(mz[i]);
				output.writeDouble(intensity[i]);
			}
			
			// 2. save header
			MyMGFPeakList pl = spectra.m_pl;
			output.writeUTF(pl.getTitle_safe());
			output.writeUTF(pl.getPepmass_safe());
			output.writeUTF(pl.getCharge_safe());
			output.writeInt(pl.getTandemCount());
		}
	}
}
