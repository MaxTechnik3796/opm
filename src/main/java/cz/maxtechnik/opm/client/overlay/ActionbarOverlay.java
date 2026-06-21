package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.client.handler.HudTransformUtils;
import cz.maxtechnik.opm.init.OpmConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
@EventBusSubscriber(modid=cz.maxtechnik.opm.OpmMod.MODID, value=Dist.CLIENT)
public class ActionbarOverlay{
	@SubscribeEvent
	public static void onRenderPre(RenderGuiLayerEvent.Pre event){
		if(event.getName().equals(VanillaGuiLayers.OVERLAY_MESSAGE)){
			if(!OpmConfig.ACTIONBAR_ENABLED.get()){
				event.setCanceled(true);
				return;
			}
			net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getInstance();
			float cx=mc.getWindow().getGuiScaledWidth()/2.0f;
			float cy=mc.getWindow().getGuiScaledHeight()-68.0f+4.0f;
			HudTransformUtils.pushTransform(event.getGuiGraphics().pose(),cx,cy,
					OpmConfig.ACTIONBAR_SCALE.get(),OpmConfig.ACTIONBAR_X_OFFSET.get(),OpmConfig.ACTIONBAR_Y_OFFSET.get());
		}
	}
	@SubscribeEvent
	public static void onRenderPost(RenderGuiLayerEvent.Post event){
		if(event.getName().equals(VanillaGuiLayers.OVERLAY_MESSAGE)&&OpmConfig.ACTIONBAR_ENABLED.get()){
			HudTransformUtils.popTransform(event.getGuiGraphics().pose(),
					OpmConfig.ACTIONBAR_SCALE.get(),OpmConfig.ACTIONBAR_X_OFFSET.get(),OpmConfig.ACTIONBAR_Y_OFFSET.get());
		}
	}
}
