package au.com.acpfg.misc.spectra;

import javax.swing.Icon;

import au.com.acpfg.misc.spectra.SpectralDataInterface;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.systemsbiology.jrap.stax.Scan;

public class SpectraUtilityFactory extends UtilityFactory {
	 private static int spectra_id = 1;
	 
     private static final Icon ICON =
             loadIcon(SpectralDataInterface.class, "spectra-icon-16x16.png");
     
     public static DataCell createCell(MyMGFPeakList mgf) {
    	 return new MGFSpectraCell(mgf);
     }
     
     public static DataCell createCell(Scan scn, String id) {
    	 if (id == null) {
    		 id = "Spectra" + spectra_id;
    		 spectra_id++;
    	 }
    	 return (scn.getHeader().getMsLevel() > 1) ? 
    				 new mzMLSpectraCell(scn, id) : DataType.getMissingCell();
     }
     
     /** {@inheritDoc} */
     @Override
     public Icon getIcon() {
         return ICON;
     }
     
     /** {@inheritDoc} */
     @Override
     protected DataValueRendererFamily getRendererFamily(
             final DataColumnSpec spec) {
         return new DefaultDataValueRendererFamily(
                 new SpectraStringRenderer(),
                 new SpectraTop10Renderer(),
                 new SpectraVisualRenderer(),
                 
                 new SpectraBitVectorRenderer("Spectra M/Z map (iTRAQ 8-plex region, no thres., 0.05u)", 113.0, 121.2, 0.0, 0.05),
                 new SpectraBitVectorRenderer("Spectra M/Z map (iTRAQ 8-plex region, thres. > 20, 0.05u)", 113.0, 121.2, 20.0, 0.05),
                 new SpectraBitVectorRenderer("Spectra M/Z map (low region, no thres., 0.1u)", 100.0, 600.0, 0.0, 0.1),
                 new SpectraBitVectorRenderer("Spectra M/Z map (low region, thres. > 20, 0.1u)", 100.0, 600.0, 20.0, 0.1),
                 new SpectraBitVectorRenderer("Spectra M/Z map (entire spectrum, no thres., 1u)", 0.0, 2000.0, 0.0, 1.0),
                 new SpectraBitVectorRenderer("Spectra M/Z map (entire spectrum, thres. > 20, 1u)", 0.0, 2000.0, 20.0, 1.0)
         );
                 
     }
 
     /** {@inheritDoc} */
     @Override
     protected DataValueComparator getComparator() {
         return new DataValueComparator() {
             /** {@inheritDoc} */
             @Override
             protected int compareDataValues(final DataValue v1,
                     final DataValue v2) {
                 // TODO... how to compare spectra... number of peaks?
            	 return 0;
             }
         };
     }
}
