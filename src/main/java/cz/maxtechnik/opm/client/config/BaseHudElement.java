package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Bázová abstraktní třída pro všechny HUD prvky.
 * Zajišťuje sdílenou logiku pro zapnutí, měřítko (scale), ohraničení náhledu a strukturu bočního inspectoru.
 */
public abstract class BaseHudElement implements HudElement {

	protected final String id;
	protected final String title;
	protected final String icon;
	protected final ModConfigSpec.BooleanValue configEnabled;
	protected final ModConfigSpec.DoubleValue configScale;

	protected boolean enabled;
	protected double scale;
	protected double minScale = 0.5;
	protected double maxScale = 2.0;
	protected double scaleStep = 0.05;

	public BaseHudElement(String id, String title, String icon, ModConfigSpec.BooleanValue configEnabled, ModConfigSpec.DoubleValue configScale) {
		this.id = id;
		this.title = title;
		this.icon = icon;
		this.configEnabled = configEnabled;
		this.configScale = configScale;
		this.enabled = configEnabled != null ? configEnabled.get() : true;
		this.scale = configScale != null ? configScale.get() : 1.0;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String title() {
		return title;
	}

	@Override
	public String icon() {
		return icon;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public double getScale() {
		return scale;
	}

	@Override
	public void setScale(double scale) {
		this.scale = Math.round(Math.clamp(scale, minScale, maxScale) * 100.0) / 100.0;
	}

	@Override
	public void adjustScale(double delta) {
		setScale(scale + delta);
	}

	@Override
	public void save() {
		if (configEnabled != null) configEnabled.set(enabled);
		if (configScale != null) configScale.set(scale);
	}

	@Override
	public void renderPreview(GuiGraphics g, Font font, int screenW, int screenH, boolean hovered, boolean selected, boolean dragging) {
		if (!enabled) return;
		int x = getX(screenW, screenH);
		int y = getY(screenW, screenH);
		int w = getW();
		int h = getH();

		UiKit.drawSelectionBox(g, font, x, y, w, h, getBadgeText(), hovered, selected, dragging);

		var pose = g.pose();
		pose.pushPose();
		pose.translate(x, y, 0);
		if (scale != 1.0) pose.scale((float) scale, (float) scale, 1f);

		renderContent(g, font, x, y, screenW, screenH);

		pose.popPose();
	}

	protected String getBadgeText() {
		return title;
	}

	/** Vykreslení vlastního obsahu prvku (již posunuto a škálováno). */
	protected abstract void renderContent(GuiGraphics g, Font font, int x, int y, int screenW, int screenH);

	@Override
	public void renderInspector(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		int curY = y;
		UiKit.drawSectionHeader(g, font, title, x, curY, w);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "Enabled", enabled, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawStepper(g, font, "Scale", String.format("%.2f", scale), x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		curY = renderCustomInspectorOptions(g, font, x, curY, w, mx, my);

		UiKit.drawButton(g, font, "Reset Position", x + 4, curY + 2, w - 8, 16, mx, my, UiKit.C_BTN, UiKit.C_BTN_H, UiKit.C_TEXT);
	}

	/** Možnost pro potomky vykreslit specifická nastavení v inspektoru. */
	protected int renderCustomInspectorOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		return y;
	}

	@Override
	public boolean handleInspectorClick(int mx, int my, int x, int y, int w) {
		int curY = y + UiKit.ITEM_H; // skip header

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			enabled = !enabled;
			return true;
		}
		curY += UiKit.ITEM_H;

		int step = UiKit.getStepperClick(mx, my, x, curY, w);
		if (step != 0) {
			adjustScale(step * scaleStep);
			return true;
		}
		curY += UiKit.ITEM_H;

		int beforeCustomY = curY;
		int customYEnd = getCustomInspectorHeight(curY);
		if (handleCustomInspectorClick(mx, my, x, beforeCustomY, w)) {
			return true;
		}
		curY = customYEnd;

		// Reset button
		if (UiKit.hit(mx, my, x + 4, curY + 2, w - 8, 16)) {
			reset();
			return true;
		}

		return false;
	}

	protected int getCustomInspectorHeight(int startY) {
		return startY;
	}

	protected boolean handleCustomInspectorClick(int mx, int my, int x, int startY, int w) {
		return false;
	}
}
