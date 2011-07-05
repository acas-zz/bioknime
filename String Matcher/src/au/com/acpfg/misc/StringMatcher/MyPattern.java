package au.com.acpfg.misc.StringMatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyPattern {
	private final Pattern m_p;
	private final String m_title;
	
	public MyPattern(String regex) {
		this(regex, 0, false);
	}
	
	public MyPattern(String regex, boolean is_literal) {
		this(regex, 0, is_literal);
	}
	
	public MyPattern(String regex, int flags, boolean is_literal) {
		m_title = regex;
		if (is_literal) {
			String tmp = "";
			for (int i=0; i<regex.length(); i++) {
				tmp += '\\' + '\\' + regex.charAt(i);
			}
			regex = tmp;
		}
		m_p = compile(regex, flags);
	}
	
	public final String getTitle() {
		return m_title;
	}
	
	public final Pattern getPattern() {
		return m_p;
	}
	
	public static Pattern compile(String regex) {
		return Pattern.compile(regex);
	}
	
	public static Pattern compile(String regex, int flags) {
		return Pattern.compile(regex, flags);
	}
	
	public Matcher matcher(String istr) {
		return m_p.matcher(istr);
	}
	
}
