package cz.maxtechnik.opm.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
public class FullbrightHandler{
	private static boolean active=false;
	/** Uložená původní hodnota gamma před zapnutím fullbrightu. */
	private static double savedGamma=0.5;
	public static void toggle(){
		active=!active;
		Minecraft mc=Minecraft.getInstance();
		OptionInstance<Double> gamma=mc.options.gamma();
		if(active){
			// Uložíme aktuální gamma a nastavíme na max
			savedGamma=gamma.get();
			gamma.set(15.0);
		}else{
			// Obnovíme uloženou gamma
			gamma.set(savedGamma);
		}
		if(mc.player!=null){
			mc.player.displayClientMessage(Component.literal("Fullbright: "+(active?"ON":"OFF")),true);
		}
	}
	/** Bezpečné vypnutí – pokud je aktivní, vrátí gammu a deaktivuje. */
	public static void disable(){
		if(active){
			active=false;
			Minecraft mc=Minecraft.getInstance();
			mc.options.gamma().set(savedGamma);
		}
	}
	public static boolean isActive(){
		return active;
	}
}