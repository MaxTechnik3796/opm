package cz.maxtechnik.opm;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class OpmModUtil{
	public static Component ON=Component.translatable("info.opm.on");
	public static Component OFF=Component.translatable("info.opm.off");
	public static Component textOnOff(String text,boolean condition){
		return Component.translatable(text).append(": ").append(condition?ON:OFF);
	}
	public static String keyExtractor(String input){
		if(input==null) return null;
		Matcher matcher=Pattern.compile("key='([^']+)'").matcher(input);
		if(matcher.find()) return matcher.group(1);
		return input;
	}
	public static void drawCentredWindow(GuiGraphics guiGraphics,int width,int height,int screenWidth,int screenHeight,int borderSize){
		drawWindow(guiGraphics,screenWidth/2-width/2,screenHeight/2-height/2,screenWidth/2+width/2,screenHeight/2+height/2,borderSize);
	}
	public static void drawWindow(GuiGraphics gui,int x1,int y1,int x2,int y2,int borderSize){
		gui.fill(x1,y1,x2,y2,Color.BLACK);
		gui.fill(x1+borderSize,y1+borderSize,x2-borderSize,y2-borderSize,Color.GRAY);
	}
	@SuppressWarnings("unused")
	public static class Color{
		public static int WHITE=0xFFFFFFFF;
		public static int WHITE2=0xFFEEEEEE;
		public static int BLACK=0xFF000000;
		public static int GRAY=0xFF181818;
		public static int GRAY2=0xFF121212;
		public static int GRAY3=0xFF656565;
		public static int BLUE=0xFF55AAFF;
		public static int RED=0xFFFF5555;
		public static int GREEN=0xFF55FF55;
		public static int YELLOW=0xFFF5c800;
	}
}
