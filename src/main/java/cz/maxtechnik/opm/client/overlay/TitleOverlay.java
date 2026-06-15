package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.client.handler.HudTransformUtils;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = cz.maxtechnik.opm.OpmMod.MODID, value = Dist.CLIENT)
public class TitleOverlay {

	@SubscribeEvent
	public static void onRenderPre(RenderGuiLayerEvent.Pre event) {
		if (event.getName().equals(VanillaGuiLayers.TITLE)) {
			if (!OpmConfig.TITLE_ENABLED.get()) {
				event.setCanceled(true);
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
			float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;
			HudTransformUtils.pushTransform(event.getGuiGraphics().pose(), cx, cy,
					OpmConfig.TITLE_SCALE.get(), OpmConfig.TITLE_X_OFFSET.get(), OpmConfig.TITLE_Y_OFFSET.get());
		}
	}

	@SubscribeEvent
	public static void onRenderPost(RenderGuiLayerEvent.Post event) {
		if (event.getName().equals(VanillaGuiLayers.TITLE) && OpmConfig.TITLE_ENABLED.get()) {
			HudTransformUtils.popTransform(event.getGuiGraphics().pose(), 
					OpmConfig.TITLE_SCALE.get(), OpmConfig.TITLE_X_OFFSET.get(), OpmConfig.TITLE_Y_OFFSET.get());
		}
	}
}
