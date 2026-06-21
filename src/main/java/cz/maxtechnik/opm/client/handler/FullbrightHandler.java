package cz.maxtechnik.opm.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public class FullbrightHandler{
	private static boolean active=false;
	public static void toggle(){
		active=!active;
		Minecraft mc=Minecraft.getInstance();
		mc.levelRenderer.allChanged();
		if(mc.player!=null){
			if(active) mc.player.displayClientMessage(Component.literal("Fullbright: ON"),true);
			else mc.player.displayClientMessage(Component.literal("Fullbright: OFF"),true);
		}
	}
	public static boolean isActive(){
		return active;
	}
}