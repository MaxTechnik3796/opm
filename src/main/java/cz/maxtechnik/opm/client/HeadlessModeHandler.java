package cz.maxtechnik.opm.client;

import com.mojang.blaze3d.platform.InputConstants;
import cz.maxtechnik.opm.client.screen.HeadlessAfkScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = "opm", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HeadlessModeHandler {

	public static final KeyMapping AFK_KEY = new KeyMapping(
			"key.opm.toggle_headless",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			"key.categories.opm"
	);

	// Kontrola, zda jsme v headless režimu (podle otevřené obrazovky)
	public static boolean isHeadlessMode() {
		return Minecraft.getInstance().screen instanceof HeadlessAfkScreen;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		while (AFK_KEY.consumeClick()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				if (mc.screen instanceof HeadlessAfkScreen) {
					mc.screen.onClose(); // Vypnutí přes opětovné stisknutí 'K'
				} else if (mc.screen == null) {
					mc.setScreen(new HeadlessAfkScreen()); // Zapnutí
				}
			}
		}
	}

	@EventBusSubscriber(modid = "opm", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ModBusEvents {
		@SubscribeEvent
		public static void registerKeys(RegisterKeyMappingsEvent event) {
			event.register(AFK_KEY);
		}
	}
}