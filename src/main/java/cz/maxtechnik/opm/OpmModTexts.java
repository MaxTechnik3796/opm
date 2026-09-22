package cz.maxtechnik.opm;

import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class OpmModTexts{
	public static Component ON=Component.translatable("info.opm.on");
	public static Component OFF=Component.translatable("info.opm.off");
	public static Component textOnOff(String text,boolean condition){
		return Component.translatable(text).append(": ").append(condition?ON:OFF);
	}
	private static final Pattern KEY_PATTERN=Pattern.compile("key='([^']+)'");
	public static String keyExtractor(String input){
		if(input==null) return null;
		Matcher matcher=KEY_PATTERN.matcher(input);
		if(matcher.find()) return matcher.group(1);
		return input;
	}
}
