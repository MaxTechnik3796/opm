package cz.maxtechnik.opm.client.screen.config;

import cz.maxtechnik.opm.client.handler.F1Handler;
import cz.maxtechnik.opm.client.util.Scrollbar;
import cz.maxtechnik.opm.client.widget.UiKit;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ConfigSidebar {

	public static final int PANEL_W = 210;
	private static final int HEADER_H = 32;
	private static final int FOOTER_H = 34;

	// UI Options State
	private boolean noRecipeBook;
	private boolean noRealmsButton;
	private boolean customDebugScreen;
	private boolean debugHideOtherMods;
	private boolean hideTutorialToast;
	private boolean customF1;
	private OpmConfig.PumpkinMode pumpkinOverlay;

	private final Scrollbar scrollbar = new Scrollbar();
	private boolean showGeneral = false;

	public ConfigSidebar() {
		loadGeneralConfig();
	}

	public void loadGeneralConfig() {
		this.noRecipeBook = OpmConfig.NO_RECIPE_BOOK.get();
		this.noRealmsButton = OpmConfig.NO_REALMS_BUTTON.get();
		this.customDebugScreen = OpmConfig.CUSTOM_DEBUG_SCREEN.get();
		this.debugHideOtherMods = OpmConfig.DEBUG_HIDE_OTHER_MODS.get();
		this.hideTutorialToast = OpmConfig.HIDE_TUTORIAL_TOAST.get();
		this.customF1 = OpmConfig.CUSTOM_F1.get();
		this.pumpkinOverlay = OpmConfig.PUMPKIN_OVERLAY.get();
	}

	public void saveGeneralConfig() {
		OpmConfig.NO_RECIPE_BOOK.set(noRecipeBook);
		OpmConfig.NO_REALMS_BUTTON.set(noRealmsButton);
		OpmConfig.CUSTOM_DEBUG_SCREEN.set(customDebugScreen);
		OpmConfig.DEBUG_HIDE_OTHER_MODS.set(debugHideOtherMods);
		OpmConfig.HIDE_TUTORIAL_TOAST.set(hideTutorialToast);
		OpmConfig.CUSTOM_F1.set(customF1);
		if (!customF1) {
			F1Handler.setState(0);
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null) mc.options.hideGui = false;
		}
		OpmConfig.PUMPKIN_OVERLAY.set(pumpkinOverlay);
	}

	public boolean isShowGeneral() {
		return showGeneral;
	}

	public void setShowGeneral(boolean showGeneral) {
		this.showGeneral = showGeneral;
	}

	public int getPanelX(int screenW) {
		return screenW - PANEL_W - 10;
	}

	public int getPanelY() {
		return 10;
	}

	public int getPanelH(int screenH) {
		return screenH - 20;
	}

	public void render(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my, List<HudElement> elements, HudElement selectedElement, Runnable onResetAll, Runnable onClose) {
		int px = getPanelX(screenW);
		int py = getPanelY();
		int pw = PANEL_W;
		int ph = getPanelH(screenH);

		// Background & Borders
		g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, ConfigUiHelper.C_BORDER);
		g.fill(px, py, px + pw, py + ph, ConfigUiHelper.C_BG);

		// Header Background
		g.fill(px, py, px + pw, py + HEADER_H, ConfigUiHelper.C_HEADER);
		g.fill(px, py + HEADER_H, px + pw, py + HEADER_H + 1, ConfigUiHelper.C_BORDER);

		// Quick Tab Bar in Header (Icons for General + HUD Elements)
		int tabCount = elements.size() + 1;
		int tabW = (pw - 8) / tabCount;
		int tabStartX = px + 4;
		int tabY = py + (HEADER_H - 18) / 2;

		// General Tab Button
		boolean hovGen = UiKit.hit(mx, my, tabStartX, tabY, tabW, 18);
		boolean selGen = showGeneral || selectedElement == null;
		int genBg = selGen ? ConfigUiHelper.C_ACCENT : (hovGen ? 0xFF353535 : 0xFF222222);
		g.fill(tabStartX, tabY, tabStartX + tabW, tabY + 18, genBg);
		ConfigUiHelper.drawOutline(g, tabStartX, tabY, tabW, 18, ConfigUiHelper.C_BORDER);
		g.drawCenteredString(font, "⚙", tabStartX + tabW / 2, tabY + 5, selGen ? 0xFFFFFFFF : ConfigUiHelper.C_LABEL);

		// HUD Elements Tabs
		for (int i = 0; i < elements.size(); i++) {
			HudElement el = elements.get(i);
			int tx = tabStartX + (i + 1) * tabW;
			boolean hovEl = UiKit.hit(mx, my, tx, tabY, tabW, 18);
			boolean selEl = !showGeneral && selectedElement == el;
			int elBg = selEl ? ConfigUiHelper.C_ACCENT : (hovEl ? 0xFF353535 : 0xFF222222);
			g.fill(tx, tabY, tx + tabW, tabY + 18, elBg);
			ConfigUiHelper.drawOutline(g, tx, tabY, tabW, 18, ConfigUiHelper.C_BORDER);
			g.drawCenteredString(font, el.icon(), tx + tabW / 2, tabY + 5, selEl ? 0xFFFFFFFF : (el.isEnabled() ? ConfigUiHelper.C_TEXT : ConfigUiHelper.C_MUTED));
		}

		// Content Area
		int bodyY = py + HEADER_H + 4;
		int bodyH = ph - HEADER_H - FOOTER_H - 8;
		int bodyW = pw - 8;
		int bodyX = px + 4;

		g.enableScissor(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -scrollbar.scroll, 0);

		int scrolledY = (int) (my + scrollbar.scroll);
		int contentH;

		if (showGeneral || selectedElement == null) {
			contentH = renderGeneralOptions(g, font, bodyX, bodyY, bodyW, mx, scrolledY);
		} else {
			selectedElement.renderInspector(g, font, bodyX, bodyY, bodyW, mx, scrolledY);
			contentH = 7 * ConfigUiHelper.ITEM_H + 10;
		}

		pose.popPose();
		g.disableScissor();

		scrollbar.update(bodyH, contentH);
		scrollbar.render(g, px + pw - 5, bodyY);

		// Footer Background
		int ftrY = py + ph - FOOTER_H;
		g.fill(px, ftrY, px + pw, ftrY + 1, ConfigUiHelper.C_BORDER);
		g.fill(px, ftrY + 1, px + pw, py + ph, ConfigUiHelper.C_HEADER);

		// Footer Buttons
		int btnW = (pw - 12) / 2;
		int btn1X = px + 4;
		int btn2X = btn1X + btnW + 4;
		int btnY = ftrY + (FOOTER_H - 18) / 2;

		ConfigUiHelper.drawButton(g, font, "Reset All", btn1X, btnY, btnW, 18, mx, my, ConfigUiHelper.C_DANGER, ConfigUiHelper.C_DANGER_HOV, 0xFFFFFFFF);
		ConfigUiHelper.drawButton(g, font, "Done", btn2X, btnY, btnW, 18, mx, my, ConfigUiHelper.C_SUCCESS, ConfigUiHelper.C_SUCCESS_HOV, 0xFFFFFFFF);
	}

	private int renderGeneralOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		int curY = y;
		ConfigUiHelper.drawSectionHeader(g, font, "General Options", x, curY, w);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawToggle(g, font, "Hide Recipe Book", noRecipeBook, x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawToggle(g, font, "Hide Realms Button", noRealmsButton, x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawToggle(g, font, "Custom Debug F3", customDebugScreen, x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawToggle(g, font, "Hide Other Mods (F3)", debugHideOtherMods, x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawToggle(g, font, "Hide Tutorial Toast", hideTutorialToast, x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawToggle(g, font, "3-Step F1 Toggle", customF1, x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		ConfigUiHelper.drawEnumCycler(g, font, "Pumpkin Overlay", pumpkinOverlay.name(), x, curY, w, mx, my);
		curY += ConfigUiHelper.ITEM_H;

		return curY - y + 10;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int screenW, int screenH, List<HudElement> elements, HudElement[] selectedRef, Runnable onResetAll, Runnable onClose) {
		if (button != 0) return false;
		int mx = (int) mouseX, my = (int) mouseY;

		int px = getPanelX(screenW);
		int py = getPanelY();
		int pw = PANEL_W;
		int ph = getPanelH(screenH);

		if (!UiKit.hit(mx, my, px, py, pw, ph)) return false;

		// Header Tab clicks
		int tabCount = elements.size() + 1;
		int tabW = (pw - 8) / tabCount;
		int tabStartX = px + 4;
		int tabY = py + (HEADER_H - 18) / 2;

		if (UiKit.hit(mx, my, tabStartX, tabY, tabW, 18)) {
			showGeneral = true;
			return true;
		}

		for (int i = 0; i < elements.size(); i++) {
			int tx = tabStartX + (i + 1) * tabW;
			if (UiKit.hit(mx, my, tx, tabY, tabW, 18)) {
				showGeneral = false;
				selectedRef[0] = elements.get(i);
				return true;
			}
		}

		// Footer Button clicks
		int ftrY = py + ph - FOOTER_H;
		int btnW = (pw - 12) / 2;
		int btn1X = px + 4;
		int btn2X = btn1X + btnW + 4;
		int btnY = ftrY + (FOOTER_H - 18) / 2;

		if (UiKit.hit(mx, my, btn1X, btnY, btnW, 18)) {
			onResetAll.run();
			return true;
		}
		if (UiKit.hit(mx, my, btn2X, btnY, btnW, 18)) {
			onClose.run();
			return true;
		}

		// Scrollbar drag
		if (scrollbar.startDragIfHit(mx, my)) return true;

		// Body clicks
		int bodyY = py + HEADER_H + 4;
		int bodyH = ph - HEADER_H - FOOTER_H - 8;
		int bodyW = pw - 8;
		int bodyX = px + 4;

		if (UiKit.hit(mx, my, bodyX, bodyY, bodyW, bodyH)) {
			int scrolledY = (int) (my + scrollbar.scroll);
			if (showGeneral || selectedRef[0] == null) {
				return handleGeneralClick(mx, scrolledY, bodyX, bodyY, bodyW);
			} else {
				return selectedRef[0].handleInspectorClick(mx, scrolledY, bodyX, bodyY, bodyW);
			}
		}

		return true;
	}

	private boolean handleGeneralClick(int mx, int my, int x, int y, int w) {
		int curY = y + ConfigUiHelper.ITEM_H; // skip header

		if (ConfigUiHelper.isToggleHit(mx, my, x, curY, w)) {
			noRecipeBook = !noRecipeBook;
			saveGeneralConfig();
			return true;
		}
		curY += ConfigUiHelper.ITEM_H;

		if (ConfigUiHelper.isToggleHit(mx, my, x, curY, w)) {
			noRealmsButton = !noRealmsButton;
			saveGeneralConfig();
			return true;
		}
		curY += ConfigUiHelper.ITEM_H;

		if (ConfigUiHelper.isToggleHit(mx, my, x, curY, w)) {
			customDebugScreen = !customDebugScreen;
			saveGeneralConfig();
			return true;
		}
		curY += ConfigUiHelper.ITEM_H;

		if (ConfigUiHelper.isToggleHit(mx, my, x, curY, w)) {
			debugHideOtherMods = !debugHideOtherMods;
			saveGeneralConfig();
			return true;
		}
		curY += ConfigUiHelper.ITEM_H;

		if (ConfigUiHelper.isToggleHit(mx, my, x, curY, w)) {
			hideTutorialToast = !hideTutorialToast;
			saveGeneralConfig();
			return true;
		}
		curY += ConfigUiHelper.ITEM_H;

		if (ConfigUiHelper.isToggleHit(mx, my, x, curY, w)) {
			customF1 = !customF1;
			saveGeneralConfig();
			return true;
		}
		curY += ConfigUiHelper.ITEM_H;

		if (ConfigUiHelper.isEnumHit(mx, my, x, curY, w)) {
			OpmConfig.PumpkinMode[] vals = OpmConfig.PumpkinMode.values();
			pumpkinOverlay = vals[(pumpkinOverlay.ordinal() + 1) % vals.length];
			saveGeneralConfig();
			return true;
		}

		return false;
	}

	public boolean mouseDragged(double mouseY) {
		if (scrollbar.dragging) {
			scrollbar.dragTo((int) mouseY);
			return true;
		}
		return false;
	}

	public void mouseReleased() {
		scrollbar.stopDrag();
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int screenW, int screenH) {
		int px = getPanelX(screenW);
		int py = getPanelY();
		int pw = PANEL_W;
		int ph = getPanelH(screenH);

		if (UiKit.hit((int) mouseX, (int) mouseY, px, py, pw, ph)) {
			scrollbar.handleScroll(scrollY, 14);
			return true;
		}
		return false;
	}
}
