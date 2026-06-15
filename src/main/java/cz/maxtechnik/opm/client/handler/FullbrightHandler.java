package cz.maxtechnik.opm.client.handler;

import net.minecraft.client.Minecraft;
public class FullbrightHandler{
	private static boolean active=false;
	public static void toggle(){
		active=!active; // Překlopí stav (true -> false / false -> true)
		Minecraft mc=Minecraft.getInstance();
		// Přenuť Minecraft, aby hned aktualizoval světelnou mapu a překreslil chunky
		mc.levelRenderer.allChanged();
	}
	// Tuto metodu volá Mixin, aby zjistil, zda má vynutit plné světlo
	public static boolean isActive(){
		return active;
	}
}