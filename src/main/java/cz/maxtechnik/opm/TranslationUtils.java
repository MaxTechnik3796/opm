package cz.maxtechnik.opm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class TranslationUtils{
	//Předkompilovaný vzor pro výkon
	private static final Pattern KEY_PATTERN=Pattern.compile("key='([^']+)'");
	public static String extractKey(String input){
		if(input==null) return null;
		Matcher matcher=KEY_PATTERN.matcher(input);
		if(matcher.find()) return matcher.group(1);
		return input;
	}
}