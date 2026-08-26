package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
public class F1Handler{
	private static int state=0;
	public static void setState(int s){
		state=s;
	}
	public static boolean shouldHideHUD(){
		if(!OpmConfig.CUSTOM_F1.get()) return false;
		return state==1||state==2;
	}
	public static boolean shouldShowHandInF1(){
		return OpmConfig.CUSTOM_F1.get()&&state==1;
	}
	public static void handleF1Press(){
		Minecraft mc=Minecraft.getInstance();
		if(mc.player==null||mc.screen!=null) return;
		if(!OpmConfig.CUSTOM_F1.get()){
			state=0;
			mc.options.hideGui=!mc.options.hideGui;
			return;
		}
		state=(state+1)%3;
		// Stav 0: HUD zapnutý, ruka zapnutá (hideGui = false)
		// Stav 1: HUD skrytý (přes vanilla hideGui = true), ale ruka je zviditelněná přes GameRendererMixin
		// Stav 2: Plný hide (hideGui = true), ruka skrytá (klasický vanilla F1)
		mc.options.hideGui=(state!=0);
	}
}
