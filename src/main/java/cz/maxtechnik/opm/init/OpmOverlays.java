package cz.maxtechnik.opm.init;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.overlay.ArmorHudOverlay;
import cz.maxtechnik.opm.client.overlay.EffectsHudOverlay;
import cz.maxtechnik.opm.client.overlay.ItemDurabilityHudOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = OpmMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OpmOverlays {

	public static final ResourceLocation ARMOR_HUD =
			ResourceLocation.fromNamespaceAndPath(OpmMod.MODID, "armor_hud");

	public static final ResourceLocation EFFECTS_HUD =
			ResourceLocation.fromNamespaceAndPath(OpmMod.MODID, "effects_hud");

	public static final ResourceLocation ITEM_DURABILITY =
			ResourceLocation.fromNamespaceAndPath(OpmMod.MODID, "item_durability");

	@SubscribeEvent
	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.HOTBAR, ARMOR_HUD, new ArmorHudOverlay());
		event.registerAbove(VanillaGuiLayers.HOTBAR, EFFECTS_HUD, new EffectsHudOverlay());
		event.registerAbove(VanillaGuiLayers.HOTBAR, ITEM_DURABILITY, new ItemDurabilityHudOverlay());
	}
}