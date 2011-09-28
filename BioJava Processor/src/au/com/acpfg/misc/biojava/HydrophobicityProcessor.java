package au.com.acpfg.misc.biojava;

import java.io.*;
import java.net.URL;
import org.osgi.framework.Bundle;
import org.eclipse.core.runtime.*;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.proteomics.*;
import org.biojava.bio.symbol.*;
import org.biojava.bio.proteomics.aaindex.*;

/**
 * Computes the Mass, pI and hydrophobicity of the specified sequences, using the
 * parameters as specified by the model given to execute()
 * 
 * @author acassin
 *
 */
public class HydrophobicityProcessor implements BioJavaProcessorInterface {

	public void execute(BioJavaProcessorNodeModel m, final ExecutionContext exec, NodeLogger logger, final BufferedDataTable[] inData, BufferedDataContainer c)
			throws Exception {
		
		int outPI       = c.getTableSpec().findColumnIndex("pI");
		int outMass_avg = c.getTableSpec().findColumnIndex("Mass (Average, Da, +MH)");
		int outMass_mi  = c.getTableSpec().findColumnIndex("Mass (Monoisotopic, Da, +MH)");
		int outHyd      = c.getTableSpec().findColumnIndex("Average AA Hydrophobicity (aaindex1/CIDH920105)");
		
		if (outPI < 0 || outMass_avg < 0 || outMass_mi < 0 || outHyd < 0) {
			throw new Exception("Cannot find output columns! Bug...");
		}
		RowIterator it = inData[0].iterator();
		MassCalc mc, mc_mi;
		IsoelectricPointCalc ic;
		try {
			mc    = new MassCalc(SymbolPropertyTable.AVG_MASS, true);
			mc_mi = new MassCalc(SymbolPropertyTable.MONO_MASS, true);
			ic    = new IsoelectricPointCalc();
		} catch (Throwable th) {
			//System.err.println(th);
			throw new Exception("Unable to compute calculators... aborting execution!");
		}
		
		Bundle plugin = BioJavaProcessorNodePlugin.getDefault().getBundle();
		IPath p = new Path("lib/aaindex1");
		
		SimpleSymbolPropertyTableDB db = new SimpleSymbolPropertyTableDB(new AAindexStreamReader(new InputStreamReader(FileLocator.openStream(plugin, p, false))));
		AAindex hydrophobicity = (AAindex) db.table("CIDH920105");
		boolean is_protein = m.areSequencesProtein();
		while (it.hasNext()) {
			DataRow r = it.next();
			
			double pI       = 0.0;
			double mass_avg = 0.0;
			double mass_mi  = 0.0;
			double hyd      = 0.0;
			
			String str = m.getSequence(r);
			if (str == null || str.length() < 1)
				continue;
			SymbolList syms  = m.getSequenceAsSymbol(str);
			
			if (! is_protein) {
				// need to translate it (by default assume 5' to 3' orientation)
				if (syms.getAlphabet() != RNATools.getRNA()) {
					syms = DNATools.transcribeToRNA(syms);
				}
				// truncate if not divisible by 3
				if (syms.length() % 3 != 0) {
					syms = syms.subList(1, syms.length() - (syms.length() % 3));
				}
				
				syms = RNATools.translate(syms);
			}
			
			// remove * if necessary
			if (syms.symbolAt(syms.length()) == ProteinTools.ter()) {
				syms = syms.subList(1, syms.length()-1);
			}
			
			
			// unknown residues? Dont calculate, leave user to figure it out...
			if (syms.seqString().indexOf("X") >= 0) {
				DataCell[] cells = new DataCell[4];
				for (int i=0; i<cells.length; i++) {
					cells[i] = DataType.getMissingCell();
				}
				DataRow row = new DefaultRow(r.getKey(), cells);
				c.addRowToTable(new JoinedRow(r, row));
				continue;
			}
			mass_avg = mc.getMass(syms);
			mass_mi  = mc_mi.getMass(syms);
			pI   = ic.getPI(syms, true, true); // assume a free NH and COOH
			
			for (int i=1; i<= syms.length(); i++) {
				hyd += hydrophobicity.getDoubleValue(syms.symbolAt(i));
			}
			hyd /= syms.length();
			DataCell[] cells = new DataCell[4];
			cells[0]         = new DoubleCell(pI); 
			cells[1]         = new DoubleCell(mass_avg);
			cells[2]         = new DoubleCell(mass_mi);
			cells[3]         = new DoubleCell(hyd);
			DataRow      row = new DefaultRow(r.getKey(), cells);
			c.addRowToTable(new JoinedRow(r, row));
		}
	}

	public DataTableSpec get_table_spec() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[4];
        allColSpecs[0] = 
            new DataColumnSpecCreator("pI", DoubleCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Mass (Average, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("Mass (Monoisotopic, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[3] = 
            new DataColumnSpecCreator("Average AA Hydrophobicity (aaindex1/CIDH920105)", DoubleCell.TYPE).createSpec();
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
