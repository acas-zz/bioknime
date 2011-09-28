package au.com.acpfg.misc.jemboss.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;

import au.com.acpfg.misc.jemboss.local.AbstractTableMapper;
import au.com.acpfg.misc.jemboss.settings.ProgramSetting;

/**
 * Responsible for decoding an emboss program which produces a FASTA style file (eg. seqoutall program setting)
 * 
 * @author andrew.cassin
 *
 */
public class FastaUnmarshaller implements UnmarshallerInterface {
	private static final Pattern accsn_pattern  = Pattern.compile("^(\\S+)\\b");
	private static final Pattern descr_pattern  = Pattern.compile("^\\S+\\s*(.*)$");
	private static int id = 0;
	
	public void addColumns(AbstractTableMapper atm, ProgramSetting for_this ) {
		ArrayList<DataColumnSpec> vec = new ArrayList<DataColumnSpec>();
		String               basename = for_this.getName()+":";
		vec.add(new DataColumnSpecCreator(basename+"Accession",   StringCell.TYPE).createSpec());
		vec.add(new DataColumnSpecCreator(basename+"Description", StringCell.TYPE).createSpec());
		vec.add(new DataColumnSpecCreator(basename+"Sequence",    StringCell.TYPE).createSpec());
		atm.addFormattedColumns(for_this, vec);
		atm.addRawColumn(for_this, new DataColumnSpecCreator(basename+"Raw output", StringCell.TYPE).createSpec());
	}

	@Override
	public void process(ProgramSetting for_this, 
			InputStream emboss_prog_output_stream,
			AbstractTableMapper atm) throws IOException,InvalidSettingsException {
		String basename = for_this.getName()+":";
		DataTableSpec spec_formatted = atm.getFormattedTableSpec();
		String name_row  = "RowID";
		String name_id   = basename+"Accession";
		String name_descr= basename+"Description";
		String name_seq  = basename+"Sequence";
		String rid       = atm.getCurrentRow();
		
		BufferedFileReader rseq = BufferedFileReader.createNewReader(emboss_prog_output_stream);
		String line = null;
		boolean done = false;
        boolean already_got_header = false;
        StringBuffer seq = null;
        String[] accsn = null;
        String[] descr = null;
        
        StringBuffer raw = new StringBuffer(10*1024);
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
    	    	  raw.append(line);
    	    	  raw.append('\n');
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
		            		  raw.append(tline);
		            		  raw.append('\n');
		            		  got_seq = true;
		            	  }
	            	  }
	              } while (tline_len == 0 || got_seq );
    	    }
            
    	    // save the sequence to the container
    	    HashMap<String,DataCell> cellmap = new HashMap<String,DataCell>();
    	    if (!done && seq != null && accsn != null && descr != null) {
    	    	cellmap.put(name_row,   new StringCell(rid));
	    	    cellmap.put(name_id,    new StringCell(accsn[0]));
	    	    cellmap.put(name_descr, new StringCell(descr[0]));
	    	    cellmap.put(name_seq,   new StringCell(seq.toString()));
	    	    atm.setFormattedCells(cellmap);
	    	    atm.emitFormattedRow();
    	    }
        }
	      
		rseq.close();
		atm.setRawOutputCell(for_this, new StringCell(raw.toString()));
		// NB: emitting of a raw row is done once all unmarshalling is done (ie. all settings processed)
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
