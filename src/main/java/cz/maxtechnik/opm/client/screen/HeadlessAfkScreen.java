package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class HeadlessAfkScreen extends Screen {
	private static final ResourceLocation SCREENSHOT_LOC = ResourceLocation.fromNamespaceAndPath(OpmMod.MODID, "afk_screenshot");
	private final com.mojang.blaze3d.platform.NativeImage capturedImage;
	private DynamicTexture dynamicTexture;

	public HeadlessAfkScreen(com.mojang.blaze3d.platform.NativeImage nativeImage) {
		super(Component.literal("Headless AFK"));
		this.capturedImage = nativeImage;
	}

	@Override
	protected void init() {
		super.init();
		// Zaregistrujeme stažený screenshot do herního Texture Manažeru
		this.dynamicTexture = new DynamicTexture(this.capturedImage);
		Minecraft.getInstance().getTextureManager().register(SCREENSHOT_LOC, this.dynamicTexture);
		Minecraft.getInstance().getSoundManager().pause();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// 1. Vykreslíme zamrzlý screenshot z paměti přes celou herní plochu
		guiGraphics.blit(SCREENSHOT_LOC, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

		// 2. Hodíme přes něj tmavý filmový filtr (zabarvení), aby to vypadalo profesionálně
		guiGraphics.fill(0, 0, this.width, this.height, 0xCE0A0A0A);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		// 3. Vykreslení textových informací
		guiGraphics.drawCenteredString(this.font, "=== OPM HEADLESS AFK MODE ===", centerX, centerY - 30, 0xFFE04343);
		guiGraphics.drawCenteredString(this.font, "3D World rendering is suspended (~0% GPU)", centerX, centerY - 10, 0xFFAAAAAA);
		guiGraphics.drawCenteredString(this.font, "All game sounds are currently MUTED.", centerX, centerY + 5, 0xFFAAAAAA);
		guiGraphics.drawCenteredString(this.font, "Press 'K' or 'ESC' to return to game", centerX, centerY + 35, 0xFF55FF55);

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == HeadlessModeHandler.AFK_KEY.getKey().getValue() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().getSoundManager().resume();
		// FIX: Kompletně vymažeme texturu z VRAM a RAM paměti, jakmile odcházíme (brání lagování paměti při častém používání)
		Minecraft.getInstance().getTextureManager().release(SCREENSHOT_LOC);
		if (this.dynamicTexture != null) {
			this.dynamicTexture.close();
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}