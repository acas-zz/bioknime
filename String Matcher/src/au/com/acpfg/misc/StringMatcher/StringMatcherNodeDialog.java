package au.com.acpfg.misc.StringMatcher;

import java.util.Arrays;

import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.data.StringValue;

/**
 * <code>NodeDialog</code> for the "StringMatcher" Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class StringMatcherNodeDialog extends DefaultNodeSettingsPane {
	
    // NB: must match the node model code... HACK!
	private static String[] output_vec = new String[] { 
		"Matches (collection)", "Match Positions (collection)", "Unique Match Count", "Unique Matches", "Match Count", 
		"Start Positions (collection)", "Extent of matches (collection)",
		"Match Extent (substring)", "Match Extent (position)", "Patterns (successful, distinct)", "Non-overlapping matches (collection)",
		"Match Start Position Density (Bit Vector)", "Match Position Density (Bit Vector)", "Non-overlapping match count",
		"Number of matches per position (collection)", "Unique Match Distribution", "Pattern distribution (successful only)",
		"Input String Coverage (%)", "Highlight Matches (HTML, single colour)"
	};
	
  
    protected StringMatcherNodeDialog() {
        super();
        
        Arrays.sort(output_vec);
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(StringMatcherNodeModel.CFG_ONLY_ROWS, false), "Only output matching rows"));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(StringMatcherNodeModel.CFG_AS_REGEXP, false), "Treat search strings as regular expression?"));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(StringMatcherNodeModel.CFG_INPUT_STRINGS, ""), "Column to search:", 0, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(StringMatcherNodeModel.CFG_MATCHER_STRINGS, ""), "Column with match strings:", 1, StringValue.class));
        addDialogComponent(new DialogComponentStringListSelection(
        		new SettingsModelStringArray(StringMatcherNodeModel.CFG_OUTPUT_FORMAT, new String[] {"Matches (collection)"}), "Required output", output_vec));
    }
}

