package cz.maxtechnik.opm.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public class BeaconVisualizerHandler{
	private static boolean active=false;
	public static void toggle(){
		active=!active;
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null) mc.player.displayClientMessage(Component.literal("Beacon Visualizer: "+(active?"ON":"OFF")),true);
	}
	public static boolean isActive(){
		return active;
	}
}