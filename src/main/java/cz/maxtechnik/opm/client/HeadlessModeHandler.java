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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@SuppressWarnings({"removal", "unused"})
@EventBusSubscriber(modid = OpmMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HeadlessModeHandler {

	public static final KeyMapping AFK_KEY = new KeyMapping(
			"key.opm.toggle_headless",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			"key.categories.opm"
	);

	public static boolean active = false;
	public static net.minecraft.client.gui.screens.Screen savedScreen = null;

	public static boolean isHeadlessMode() {
		return active;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (AFK_KEY.consumeClick()) {
			toggleAfk();
		}
	}

	@SubscribeEvent
	public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
		if (!AFK_KEY.isUnbound() && event.getKeyCode() == AFK_KEY.getKey().getValue()) {
			toggleAfk();
			event.setCanceled(true);
		}
	}

	public static void toggleAfk() {
		Minecraft mc = Minecraft.getInstance();
		if (!active) {
			active = true;
			savedScreen = mc.screen;

			int width = mc.getMainRenderTarget().width;
			int height = mc.getMainRenderTarget().height;
			NativeImage nativeImage = new NativeImage(width, height, false);

			RenderSystem.bindTexture(mc.getMainRenderTarget().getColorTextureId());
			nativeImage.downloadTexture(0, false);
			nativeImage.flipY();

			mc.setScreen(new HeadlessAfkScreen(nativeImage));
		} else {
			active = false;
			if (mc.screen instanceof HeadlessAfkScreen afk) {
				afk.forceClose = true;
			}
			mc.setScreen(savedScreen);
			savedScreen = null;
		}
	}

	@SubscribeEvent
	public static void onPlaySound(PlaySoundEvent event) {
		if (isHeadlessMode()) {
			event.setSound(null);
		}
	}

	@EventBusSubscriber(modid = OpmMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ModBusEvents {
		@SubscribeEvent
		public static void registerKeys(RegisterKeyMappingsEvent event) {
			event.register(AFK_KEY);
		}
	}
}