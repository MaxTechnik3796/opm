package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
public class F1Handler{
	private static int state=0;
	public static void setState(int s){
		state=s;
	}
	public static boolean shouldHideHUD(){
		if(!OpmConfig.CUSTOM_F1.get()) return false;
		return state==1||state==2;
	}
	public static void handleF1Press(){
		Minecraft mc=Minecraft.getInstance();
		if(mc.player==null||mc.screen!=null) return;
		if(!OpmConfig.CUSTOM_F1.get()){
			state=0;
			return;
		}
		state=(state+1)%3;
		// Synchronizace s vanilla hideGui nastavením
		if(state==0||state==1){
			mc.options.hideGui=false;
		}else if(state==2){
			mc.options.hideGui=true;
		}
	}
	@SubscribeEvent
	public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event){
		// Pokud jsme ve stavu 1 (schovat HUD), zrušíme vykreslování vrstvy
		if(OpmConfig.CUSTOM_F1.get()&&state==1){
			event.setCanceled(true);
		}
	}
}
