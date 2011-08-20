package au.com.acpfg.io.genbank.reader;

/**
 * Callers of GenbankRecord may provide a listener which is called when the features section are processed
 * The methods here will be called during a parse of the genbank record, during construction of the object
 * 
 * @author andrew.cassin
 *
 */
public interface GenbankFeatureListener {

	/*
	 * Responsible for handling a given section's content. Speed is important here.
	 * @param Title is the type of feature eg. source, CDS etc. (ALWAYS in lowercase though)
	 * @param Accsn is the genbank accession for the entry which has the section
	 * @param Content is the un-modified raw data from the feature for this type of feature (including newlines and all record formatting)
	 */
	public void parse_section(String title, String accsn, String content) throws InvalidGenbankRecordException;
}
