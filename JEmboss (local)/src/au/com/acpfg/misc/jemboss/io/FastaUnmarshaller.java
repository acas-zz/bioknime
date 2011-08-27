package au.com.acpfg.misc.jemboss.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Responsible for decoding an emboss program which produces a FASTA style file (eg. seqoutall program setting)
 * 
 * @author andrew.cassin
 *
 */
public class FastaUnmarshaller implements FormattedUnmarshallerInterface {
	private static final Pattern accsn_pattern  = Pattern.compile("^(\\S+)\\b");
	private static final Pattern descr_pattern  = Pattern.compile("^\\S+\\s*(.*)$");
	private static int id = 0;
	
	@Override
	public DataColumnSpec[] add_columns() {
		ArrayList<DataColumnSpec> vec = new ArrayList<DataColumnSpec>();
		vec.add(new DataColumnSpecCreator("FASTA Accession", StringCell.TYPE).createSpec());
		vec.add(new DataColumnSpecCreator("FASTA Description", StringCell.TYPE).createSpec());
		vec.add(new DataColumnSpecCreator("FASTA Sequence", StringCell.TYPE).createSpec());
		return vec.toArray(new DataColumnSpec[0]);
	}

	@Override
	public void process(ProgramSetting ps, File out_file,
			BufferedDataContainer c, String rid) throws IOException,InvalidSettingsException {
		int rid_idx   = c.getTableSpec().findColumnIndex("RowID");
		int accsn_idx = c.getTableSpec().findColumnIndex("FASTA Accession");
		int descr_idx = c.getTableSpec().findColumnIndex("FASTA Description");
		int seq_idx   = c.getTableSpec().findColumnIndex("FASTA Sequence");
		BufferedFileReader rseq = BufferedFileReader.createNewReader(new FileInputStream(out_file));
		String line = null;
		boolean done = false;
        boolean already_got_header = false;
        StringBuffer seq = null;
        String[] accsn = null;
        String[] descr = null;
        
          
       while (!done) {
    	   
    	    // get header line
    	    if (!already_got_header) {
	    	    do {
	    	    	line = rseq.readLine();
	    	    	if (line == null) {
	    	    		done = true;
	    	    		break;
	    	    	}
	    	    } while (!line.startsWith(">"));
    	    }
    	    
    	    if (!done) {
    	    	  String[] entries = line.split("\\x01");
	              if (entries.length > 0 && entries[0].startsWith(">")) {
	                	entries[0] = entries[0].substring(1);	// skip over > for parse_accession()
	              }
	              accsn = parse_accession(accsn_pattern,entries);
	              descr = parse_description(descr_pattern,entries);
	              String tline;
	              seq = new StringBuffer(10 * 1024);
	              boolean got_seq = false;
	              already_got_header = false;
	              int tline_len = 0;
	              do {
	            	  if ((line = rseq.readLine()) == null) {
	            		  already_got_header = false;
	            		  break;
	            	  }
	            	  tline         = line.trim();
	            	  tline_len     = tline.length();
	            	  if (tline_len > 0) {
		            	  char first_c  = tline.charAt(0);
		            	  if (first_c == '>') {
		            		  got_seq = false;
		            		  already_got_header = true;
		            		  break;
		            	  } 
		            	  
		            	  if (Character.isLetter(first_c) || first_c == '*' || first_c == '-') {
		            		  seq.append(tline);
		            		  got_seq = true;
		            	  }
	            	  }
	              } while (tline_len == 0 || got_seq );
    	    }
            
    	    // save the sequence to the container
    	    int n_cells = c.getTableSpec().getNumColumns();
    	    DataCell[] cells = new DataCell[n_cells];
    	    for (int i=0; i<n_cells; i++) {
    	    	cells[i] = DataType.getMissingCell();
    	    }
    	    
    	    if (seq != null && accsn != null && descr != null) {
    	    	cells[rid_idx]   = new StringCell(rid);
	    	    cells[accsn_idx] = new StringCell(accsn[0]);
	    	    cells[descr_idx] = new StringCell(descr[0]);
	    	    cells[seq_idx]   = new StringCell(seq.toString());
	    	    c.addRowToTable(new DefaultRow("ID"+id++, cells));
    	    }
        }
	      
		rseq.close();
		
	}
	
	 protected String[] parse_accession(Pattern matcher, String[] entries) throws InvalidSettingsException  {
	    	int cnt = 0;
	    	String[] accsns = new String[entries.length];
	    	for (String entry : entries) {
	    		Matcher m = matcher.matcher(entry);
		    	if (m.find()) {
		    		if (m.groupCount() != 1) {
		    			throw new InvalidSettingsException("You must use capturing parentheses () to match an accession only once!");
		    		}
		    		accsns[cnt] = m.group(1);
		    		cnt++;
		    	} 
	    	}
	    	if (cnt < entries.length) {
	    		accsns[cnt] = null; // make sure array has null after last match
	    	}
	     	return (cnt > 0) ? accsns : null;
	    }
	    
	    protected String[] parse_description(Pattern matcher, String[] entries) throws InvalidSettingsException {
	    	int cnt = 0;
	    	String[] descrs = new String[entries.length];
	    	for (String entry : entries) {
	    		Matcher m = matcher.matcher(entry);
	    		if (m.find()) {
	    			if (m.groupCount() != 1) {
	        			throw new InvalidSettingsException("You must use capturing parentheses() to match a sequence description only once!");
	        		}
	    			descrs[cnt] = m.group(1);
	    			cnt++;
	    		}
	    	}
	    	if (cnt < entries.length) {
	    		descrs[cnt] = null;
	    	}
	    	return (cnt > 0) ? descrs : null;
	    }
	    

}
