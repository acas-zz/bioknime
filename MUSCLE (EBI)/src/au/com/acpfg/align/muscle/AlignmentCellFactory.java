package au.com.acpfg.align.muscle;

import java.io.IOException;
import java.io.StringReader;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

import pal.alignment.Alignment;

/**
 * Responsible for giving the polished appearance of the cells (renderers, icons etc.)
 * @author andrew.cassin
 *
 */
public class AlignmentCellFactory extends UtilityFactory {
	 private static final Icon ICON =
         loadIcon(AlignmentValue.class, "alignment-icon-16x16.png");
	 
	 /** {@inheritDoc} */
     @Override
     public Icon getIcon() {
         return ICON;
     }
     
     public static DataCell createCell(String fasta, AlignmentValue.AlignmentType dt) throws IOException {
    	 return new MultiAlignmentCell(fasta, dt); 
     }
     
     /** {@inheritDoc} */
     @Override
     protected DataValueRendererFamily getRendererFamily(
             final DataColumnSpec spec) {
         return new DefaultDataValueRendererFamily(
                 new AlignmentSummaryRenderer(),
                 new FormattedRenderer(FormattedRenderer.FormatType.F_CLUSTALW),
                 new FormattedRenderer(FormattedRenderer.FormatType.F_PHYLIP_INTERLEAVED),
                 new FormattedRenderer(FormattedRenderer.FormatType.F_PHYLIP_SEQUENTIAL),
                 new FormattedRenderer(FormattedRenderer.FormatType.F_PLAIN)
                );
                 
     }
}
