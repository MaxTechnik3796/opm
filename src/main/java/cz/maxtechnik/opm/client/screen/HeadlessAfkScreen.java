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
import org.jetbrains.annotations.NotNull;

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
		// 1. Zmražený screenshot a silnější tmavý filtr pro moderní vzhled
		guiGraphics.blit(SCREENSHOT_LOC, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
		guiGraphics.fill(0, 0, this.width, this.height, 0xDC0A0A0A);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		// 2. Moderní nadpis s modrým OPM stylem a stínem
		String title = "OPM ZERO PROFILER";
		float sc = 1.5f;
		int tx = centerX - (int) (this.font.width(title) * sc) / 2;
		int ty = centerY - 90;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(tx, ty, 0);
		guiGraphics.pose().scale(sc, sc, 1f);
		guiGraphics.drawString(this.font, title, 1, 1, 0xFF000000, false);
		guiGraphics.drawString(this.font, title, 0, 0, 0xFF55AAFF, false); // Modrý akcent
		guiGraphics.pose().popPose();

		// 3. Pozadí okna (Benchmark style)
		int boxW = 260;
		int boxH = 100;
		int boxX = centerX - boxW / 2;
		int boxY = centerY - 45;

		guiGraphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFF000000); // Okraj
		guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A); // Tělo
		guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 16, 0xFF111111); // Hlavička
		guiGraphics.fill(boxX, boxY + 16, boxX + boxW, boxY + 17, 0xFF000000); // Linka pod hlavičkou

		guiGraphics.drawCenteredString(this.font, "System & Hardware Metrics", centerX, boxY + 4, 0xFF888888);

		// Získání metrik paměti
		long maxMem = Runtime.getRuntime().maxMemory() / 1024L / 1024L;
		long totalMem = Runtime.getRuntime().totalMemory() / 1024L / 1024L;
		long freeMem = Runtime.getRuntime().freeMemory() / 1024L / 1024L;
		long usedMem = totalMem - freeMem;

		// Získání entit
		Minecraft mc = Minecraft.getInstance();
		String entities = mc.level != null ? String.valueOf(mc.level.getEntityCount()) : "0";

		// 4. Vykreslení řádků metrik
		int textX = boxX + 12;
		int valueX = boxX + boxW - 12;
		int statY = boxY + 24;
		int step = 14;

		drawStatRow(guiGraphics, "GPU 3D Engine:", "SUSPENDED (0%)", textX, valueX, statY, 0xFF55FF55);
		statY += step;
		drawStatRow(guiGraphics, "Target Framerate:", "1 FPS (Eco Mode)", textX, valueX, statY, 0xFF55FF55);
		statY += step;
		drawStatRow(guiGraphics, "RAM Allocation:", usedMem + " MB / " + maxMem + " MB", textX, valueX, statY, 0xFFEEEEEE);
		statY += step;
		drawStatRow(guiGraphics, "Active Entities:", entities, textX, valueX, statY, 0xFFEEEEEE);
		statY += step;
		drawStatRow(guiGraphics, "Audio Engine:", "MUTED", textX, valueX, statY, 0xFFFF5555);

		// 5. Instrukce pro návrat
		guiGraphics.drawCenteredString(this.font, "Press 'K' or 'ESC' to resume playing", centerX, boxY + boxH + 20, 0xFF555555);

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void drawStatRow(GuiGraphics g, String label, String value, int x1, int x2, int y, int valueColor) {
		g.drawString(this.font, label, x1, y, 0xFFAAAAAA, false);
		int valW = this.font.width(value);
		g.drawString(this.font, value, x2 - valW, y, valueColor, false);
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

	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
	}
}