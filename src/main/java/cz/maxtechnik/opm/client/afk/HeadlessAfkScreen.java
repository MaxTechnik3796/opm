package cz.maxtechnik.opm.client.afk;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.ui.UiKit;
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
	private final long startTime = System.currentTimeMillis();
	private DynamicTexture dynamicTexture;
	public boolean forceClose = false;

	public HeadlessAfkScreen(com.mojang.blaze3d.platform.NativeImage nativeImage) {
		super(Component.literal("Headless AFK"));
		this.capturedImage = nativeImage;
	}

	@Override
	protected void init() {
		super.init();
		if (this.dynamicTexture == null) {
			this.dynamicTexture = new DynamicTexture(this.capturedImage);
			Minecraft.getInstance().getTextureManager().register(SCREENSHOT_LOC, this.dynamicTexture);
		}
		Minecraft.getInstance().getSoundManager().pause();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// 1. Zmražený screenshot a filtr pozadí
		guiGraphics.blit(SCREENSHOT_LOC, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
		guiGraphics.fill(0, 0, this.width, this.height, 0xDC0A0A0A);

		int centerX = this.width / 2;
		int centerY = this.height / 2;

		// 2. Nadpis v OPM stylu
		String title = "OPM ZERO PROFILER";
		float sc = 1.5f;
		int tx = centerX - (int) (this.font.width(title) * sc) / 2;
		int ty = centerY - 92;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(tx, ty, 0);
		guiGraphics.pose().scale(sc, sc, 1f);
		guiGraphics.drawString(this.font, title, 1, 1, 0xFF000000, false);
		guiGraphics.drawString(this.font, title, 0, 0, UiKit.C_ACCENT, false);
		guiGraphics.pose().popPose();

		// 3. Panel okna (UiKit styl)
		int boxW = 270;
		int boxH = 116;
		int boxX = centerX - boxW / 2;
		int boxY = centerY - 48;

		guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, UiKit.C_BG);
		UiKit.drawOutline(guiGraphics, boxX, boxY, boxW, boxH, UiKit.C_BORDER);

		guiGraphics.fill(boxX + 1, boxY + 1, boxX + boxW - 1, boxY + 18, UiKit.C_HEADER);
		guiGraphics.fill(boxX + 1, boxY + 18, boxX + boxW - 1, boxY + 19, UiKit.C_BORDER);

		guiGraphics.drawCenteredString(this.font, "System & Hardware Metrics", centerX, boxY + 5, UiKit.C_LABEL);

		// Získání metrik paměti
		long maxMem = Runtime.getRuntime().maxMemory() / 1024L / 1024L;
		long totalMem = Runtime.getRuntime().totalMemory() / 1024L / 1024L;
		long freeMem = Runtime.getRuntime().freeMemory() / 1024L / 1024L;
		long usedMem = totalMem - freeMem;

		// Entity
		Minecraft mc = Minecraft.getInstance();
		String entities = mc.level != null ? String.valueOf(mc.level.getEntityCount()) : "0";

		// AFK live timer
		long elapsedSec = Math.max(0, (System.currentTimeMillis() - startTime) / 1000);
		long h = elapsedSec / 3600;
		long m = (elapsedSec % 3600) / 60;
		long s = elapsedSec % 60;
		String liveTime = h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);

		// 4. Vykreslení řádků metrik
		int textX = boxX + 10;
		int valueX = boxX + boxW - 10;
		int statY = boxY + 24;
		int step = 14;

		drawStatRow(guiGraphics, "AFK Duration:", liveTime, textX, valueX, statY, UiKit.C_ACCENT);
		statY += step;
		drawStatRow(guiGraphics, "GPU 3D Engine:", "SUSPENDED (0%)", textX, valueX, statY, 0xFF55FF55);
		statY += step;
		drawStatRow(guiGraphics, "Target Framerate:", "1 FPS (Eco Mode)", textX, valueX, statY, 0xFF55FF55);
		statY += step;
		drawStatRow(guiGraphics, "RAM Allocation:", usedMem + " MB / " + maxMem + " MB", textX, valueX, statY, UiKit.C_TEXT);
		statY += step;
		drawStatRow(guiGraphics, "Active Entities:", entities, textX, valueX, statY, UiKit.C_TEXT);
		statY += step;
		drawStatRow(guiGraphics, "Audio Engine:", "MUTED", textX, valueX, statY, 0xFFFF5555);

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void drawStatRow(GuiGraphics g, String label, String value, int x1, int x2, int y, int valueColor) {
		g.drawString(this.font, label, x1, y, UiKit.C_LABEL, false);
		int valW = this.font.width(value);
		g.drawString(this.font, value, x2 - valW, y, valueColor, false);
	}

	public static String formatSummary(long sec) {
		long h = sec / 3600;
		long m = (sec % 3600) / 60;
		long s = sec % 60;
		if (h == 0) {
			if (m == 0) return s + "s";
			return m + "min " + s + "s";
		} else {
			return h + "h " + m + "min";
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((!HeadlessModeHandler.AFK_KEY.isUnbound() && keyCode == HeadlessModeHandler.AFK_KEY.getKey().getValue()) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
			HeadlessModeHandler.active = false;
			this.forceClose = true;
			Minecraft.getInstance().setScreen(HeadlessModeHandler.savedScreen);
			HeadlessModeHandler.savedScreen = null;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		Minecraft.getInstance().getSoundManager().resume();
		Minecraft.getInstance().getTextureManager().release(SCREENSHOT_LOC);
		if (this.dynamicTexture != null) {
			this.dynamicTexture.close();
		}

		// Zobrazení doby trvání AFK nad hotbarem
		long totalSec = Math.max(0, (System.currentTimeMillis() - startTime) / 1000);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.literal("AFK: " + formatSummary(totalSec)), true);
		}

		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
	}
}