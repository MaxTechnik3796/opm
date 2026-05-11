package cz.maxtechnik.opm.init;

import cz.maxtechnik.opm.OpmMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.ResourceLocation;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class OpmOverlays{
	// ResourceLocation pro každý overlay (ID)
	public static final ResourceLocation ARMOR_HUD=
			ResourceLocation.fromNamespaceAndPath(OpmMod.MODID,"armor_hud");
	public static final ResourceLocation EFFECTS_HUD=
			ResourceLocation.fromNamespaceAndPath(OpmMod.MODID,"effects_hud");
	public static final ResourceLocation SPAWN_OVERLAY=
			ResourceLocation.fromNamespaceAndPath(OpmMod.MODID,"spawn_overlay");
	@SubscribeEvent
	public static void registerOverlays(RegisterGuiLayersEvent event){
	}
}