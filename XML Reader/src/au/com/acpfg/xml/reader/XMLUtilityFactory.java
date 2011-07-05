package au.com.acpfg.xml.reader;

import java.io.File;
import java.io.IOException;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;



public class XMLUtilityFactory extends UtilityFactory {
		 private static long xml_id = 1;
		 
	     private static final Icon ICON =
	             loadIcon(XMLCell.class, "xml-icon-16x16.png");
	     
	     public static DataCell createCell(Object data, String id) {
	    	 if (id == null) {
	    		 id = "Document" + xml_id;
	    		 xml_id++;
	    	 }
	    
	    	 if (data instanceof File) {
	    		 try {
	    			 return new XMLCell((File) data, XMLCell.MAX_INCORE_XML_SIZE);
	    		 } catch (Exception e) {
	    			 e.printStackTrace();
	    		 }
	    	 } else {
	    		 return new XMLCell(data.toString());
	    	 }
	    	 
	    	 // hmmm... wierd data so assume missing (maybe should be null?)
	    	 return DataType.getMissingCell();
	     }
	     
	     /** {@inheritDoc} */
	     @Override
	     public Icon getIcon() {
	         return ICON;
	     }
	     
	    /* 
	     @Override
	     protected DataValueRendererFamily getRendererFamily(
	             final DataColumnSpec spec) {
	         return new DefaultDataValueRendererFamily(
	                 
	         );
	                 
	     }*/
	 
	     /** {@inheritDoc} */
	     @Override
	     protected DataValueComparator getComparator() {
	         return new DataValueComparator() {
	             /** {@inheritDoc} */
	             @Override
	             protected int compareDataValues(final DataValue v1,
	                     final DataValue v2) {
	                 // TODO... XML comparison?
	            	 return 0;
	             }
	         };
	     }
	
}
