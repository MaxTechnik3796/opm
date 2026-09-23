package cz.maxtechnik.opm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

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
	public static void drawCentredWindow(GuiGraphics gui,int width,int height,int screenWidth,int screenHeight,int borderSize,int color){
		drawWindow(gui,screenWidth/2-width/2,screenHeight/2-height/2,screenWidth/2+width/2,screenHeight/2+height/2,borderSize,color);
	}
	public static void drawCentredWindow(GuiGraphics gui,int width,int height,int screenWidth,int screenHeight,int borderSize){
		drawWindow(gui,screenWidth/2-width/2,screenHeight/2-height/2,screenWidth/2+width/2,screenHeight/2+height/2,borderSize);
	}
	public static void drawWindowWithSize(GuiGraphics gui,int x,int y,int width,int height,int borderSize,int color){
		drawWindow(gui,x,y,x+width,y+height,borderSize,color);
	}
	public static void drawWindow(GuiGraphics gui,int x1,int y1,int x2,int y2,int borderSize){
		gui.fill(x1,y1,x2,y2,Color.BLACK);
		gui.fill(x1+borderSize,y1+borderSize,x2-borderSize,y2-borderSize,Color.GRAY);
	}
	public static void drawWindow(GuiGraphics gui,int x1,int y1,int x2,int y2,int borderSize,int color){
		gui.fill(x1,y1,x2,y2,Color.BLACK);
		gui.fill(x1+borderSize,y1+borderSize,x2-borderSize,y2-borderSize,color);
	}
	public static void drawBoxWithSize(GuiGraphics gui,int x,int y,int width,int height,int color){
		gui.fill(x,y,x+width,y+height,color);
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
	public static boolean drawClickableText(GuiGraphics gui,Font font,String text,int x,int y,int mouseX,int mouseY,int color){
		return drawClickableText(gui,font,text,x,y,mouseX,mouseY,color,color,color);
	}
	public static boolean drawClickableText(GuiGraphics gui,Font font,String text,int x,int y,int mouseX,int mouseY,int normalColor,int hoverColor,int underlineColor){
		return drawClickableText(gui,font,text,x,y,mouseX,mouseY,normalColor,hoverColor,underlineColor,-1,-1);
	}
	public static boolean drawClickableText(GuiGraphics gui,Font font,String text,int x,int y,int mouseX,int mouseY,int normalColor,int hoverColor,int underlineColor,int buttonColor,int buttonColor2){
		return drawClickableText(gui, font, text, x, y, mouseX, mouseY, normalColor, hoverColor, underlineColor,buttonColor,buttonColor2,false,0);
	}
	public static boolean drawClickableText(GuiGraphics gui,Font font,String text,int x,int y,int mouseX,int mouseY,int normalColor,int hoverColor,int underlineColor,int buttonColor,int buttonColor2,boolean isCentredButton,int buttonSize){
		int width=font.width(text);
		boolean hover=mouseX>=x&&mouseX<=x+width&&mouseY>=y&&mouseY<=y+9;
		if(buttonColor!=-1||buttonColor2!=-1){
			int buttonWidth=isCentredButton?buttonSize:width;
			drawBoxWithSize(gui,x-6,y-4,buttonWidth+12,16,buttonColor2);
			drawBoxWithSize(gui,x-5,y-3,buttonWidth+10,14,buttonColor);
		}
		if(isCentredButton)x=x+buttonSize/2-width/2;
		gui.drawString(font,text,x,y,hover?hoverColor:normalColor,false);
		if(hover&&!(underlineColor==-1)) gui.fill(x,y+9,x+width,y+10,underlineColor);
		return hover;
	}
	public static void copyToClipboard(String text){
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
	}
	public static class Mover{
		private static int[] mover={0,0};
		public static void update(int key){
			switch(key){
				case GLFW.GLFW_KEY_UP -> mover[1]=mover[1]-1;
				case GLFW.GLFW_KEY_DOWN -> mover[1]=mover[1]+1;
				case GLFW.GLFW_KEY_LEFT -> mover[0]=mover[0]-1;
				case GLFW.GLFW_KEY_RIGHT -> mover[0]=mover[0]+1;
				case GLFW.GLFW_KEY_KP_0 -> mover=new int[]{0,0};
			}
			OpmMod.LOGGER.info("Mover pos [{}, {}]; Key pressed: {}",mover[0],mover[1],key);
		}
		public static int getX(){
			return mover[0];
		}
		public static int getY(){
			return mover[1];
		}
	}
}

