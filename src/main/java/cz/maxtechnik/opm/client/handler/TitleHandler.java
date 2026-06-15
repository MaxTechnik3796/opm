package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = cz.maxtechnik.opm.OpmMod.MODID, value = Dist.CLIENT)
public class TitleHandler {

	@SubscribeEvent
	public static void onRenderPre(RenderGuiLayerEvent.Pre event) {
		if (event.getName().equals(VanillaGuiLayers.TITLE)) {
			if (!OpmConfig.TITLE_ENABLED.get()) {
				event.setCanceled(true);
				return;
			}
			double scale = OpmConfig.TITLE_SCALE.get();
			int xOffset = OpmConfig.TITLE_X_OFFSET.get();
			int yOffset = OpmConfig.TITLE_Y_OFFSET.get();
			if (scale != 1.0 || xOffset != 0 || yOffset != 0) {
				Minecraft mc = Minecraft.getInstance();
				float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
				float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;
				event.getGuiGraphics().pose().pushPose();
				event.getGuiGraphics().pose().translate(xOffset, yOffset, 0);
				if (scale != 1.0) {
					event.getGuiGraphics().pose().translate(cx, cy, 0);
					event.getGuiGraphics().pose().scale((float) scale, (float) scale, 1.0f);
					event.getGuiGraphics().pose().translate(-cx, -cy, 0);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onRenderPost(RenderGuiLayerEvent.Post event) {
		if (event.getName().equals(VanillaGuiLayers.TITLE)) {
			if (!OpmConfig.TITLE_ENABLED.get()) return;
			double scale = OpmConfig.TITLE_SCALE.get();
			int xOffset = OpmConfig.TITLE_X_OFFSET.get();
			int yOffset = OpmConfig.TITLE_Y_OFFSET.get();
			if (scale != 1.0 || xOffset != 0 || yOffset != 0) {
				event.getGuiGraphics().pose().popPose();
			}
		}
	}
}
