package cz.maxtechnik.opm.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public class FullbrightHandler{
	private static boolean active=false;
	/** Příznak, že se stav právě změnil a je potřeba vynutit přepočet light textury. */
	private static boolean dirty=false;
	public static void toggle(){
		active=!active;
		dirty=true;
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null){
			mc.player.displayClientMessage(Component.literal("Fullbright: "+(active?"ON":"OFF")),true);
		}
	}
	/** Bezpečné vypnutí – pokud je aktivní, deaktivuje a vynutí přepočet. */
	public static void disable(){
		if(active){
			active=false;
			dirty=true;
		}
	}
	public static boolean isActive(){
		return active;
	}
	/** Vrátí a resetuje dirty flag. */
	public static boolean consumeDirty(){
		if(dirty){
			dirty=false;
			return true;
		}
		return false;
	}
}