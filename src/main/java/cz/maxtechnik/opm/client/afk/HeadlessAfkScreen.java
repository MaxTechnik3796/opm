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
		int ty = centerY - 96;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(tx, ty, 0);
		guiGraphics.pose().scale(sc, sc, 1f);
		guiGraphics.drawString(this.font, title, 1, 1, UiKit.C_BORDER, false);
		guiGraphics.drawString(this.font, title, 0, 0, UiKit.C_ACCENT, false);
		guiGraphics.pose().popPose();

		// 3. Panel okna (UiKit styl shodný s Configem)
		int boxW = 280;
		int boxH = 4 + UiKit.ITEM_H + 6 * UiKit.ITEM_H + 6;
		int boxX = centerX - boxW / 2;
		int boxY = centerY - boxH / 2 + 10;
		int bodyX = boxX + 4;
		int bodyY = boxY + 4;
		int bodyW = boxW - 8;

		UiKit.drawWindow(guiGraphics, boxX, boxY, boxW, boxH, 0, 0);
		UiKit.drawSectionHeader(guiGraphics, this.font, "System & Hardware Metrics", bodyX, bodyY, bodyW);

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
		int statY = bodyY + UiKit.ITEM_H;

		drawStatRow(guiGraphics, "AFK Duration:", liveTime, bodyX, statY, bodyW, UiKit.C_ACCENT);
		statY += UiKit.ITEM_H;
		drawStatRow(guiGraphics, "GPU 3D Engine:", "SUSPENDED (0%)", bodyX, statY, bodyW, UiKit.C_SUCCESS_TEXT);
		statY += UiKit.ITEM_H;
		drawStatRow(guiGraphics, "Target Framerate:", "1 FPS (Eco Mode)", bodyX, statY, bodyW, UiKit.C_SUCCESS_TEXT);
		statY += UiKit.ITEM_H;
		drawStatRow(guiGraphics, "RAM Allocation:", usedMem + " MB / " + maxMem + " MB", bodyX, statY, bodyW, UiKit.C_TEXT);
		statY += UiKit.ITEM_H;
		drawStatRow(guiGraphics, "Active Entities:", entities, bodyX, statY, bodyW, UiKit.C_TEXT);
		statY += UiKit.ITEM_H;
		drawStatRow(guiGraphics, "Audio Engine:", "MUTED", bodyX, statY, bodyW, UiKit.C_DANGER_TEXT);

		// 5. Nápověda pod panelem
		guiGraphics.drawCenteredString(this.font, "Press ESC or AFK key to resume", centerX, boxY + boxH + 12, UiKit.C_MUTED);

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void drawStatRow(GuiGraphics g, String label, String value, int x, int y, int w, int valueColor) {
		g.drawString(this.font, label, x + 6, y + 7, UiKit.C_TEXT, false);
		int valW = this.font.width(value);
		g.drawString(this.font, value, x + w - 6 - valW, y + 7, valueColor, false);
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