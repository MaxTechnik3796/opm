package cz.maxtechnik.opm.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.screen.HeadlessAfkScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("removal")
@EventBusSubscriber(modid =OpmMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HeadlessModeHandler {

	public static final KeyMapping AFK_KEY = new KeyMapping(
			"key.opm.toggle_headless",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			"key.categories.opm"
	);

	public static boolean isHeadlessMode() {
		return Minecraft.getInstance().screen instanceof HeadlessAfkScreen;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		while (AFK_KEY.consumeClick()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) {
				if (mc.screen instanceof HeadlessAfkScreen) {
					mc.screen.onClose();
				} else if (mc.screen == null) {
					// BLESKOVÝ SCREENSHOT PŘÍMO Z RENDER THREADU
					int width = mc.getMainRenderTarget().width;
					int height = mc.getMainRenderTarget().height;
					NativeImage nativeImage = new NativeImage(width, height, false);

					// Připojíme se na texturu Minecraft okna a stáhneme pixely
					RenderSystem.bindTexture(mc.getMainRenderTarget().getColorTextureId());
					nativeImage.downloadTexture(0, false);
					nativeImage.flipY(); // OpenGL framebuffers jsou vertikálně otočené, vrátíme zpět

					mc.setScreen(new HeadlessAfkScreen(nativeImage)); // Zapnutí s obrázkem
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlaySound(PlaySoundEvent event) {
		if (isHeadlessMode()) {
			event.setSound(null);
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