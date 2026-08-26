package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.handler.F1Handler;
import cz.maxtechnik.opm.client.ui.Scrollbar;
import cz.maxtechnik.opm.client.ui.UiKit;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Moderní postranní panel umístěný vpravo, s dynamickou výškou podle obsahu.
 * Využívá jednotný design systém UiKit.
 */
public final class ConfigSidebar {

	public static final int PANEL_W = 210;
	private static final int HEADER_H = 30;
	private static final int FOOTER_H = 30;
	private static final int MARGIN = 12;

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
		return MARGIN;
	}

	public int getPanelH(int screenH) {
		int desiredH = HEADER_H + (8 * UiKit.ITEM_H + 6) + FOOTER_H;
		return Math.min(desiredH, screenH - (MARGIN * 2));
	}

	public void render(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my, List<HudElement> elements, HudElement selectedElement, Runnable onResetAll, Runnable onClose) {
		int px = getPanelX(screenW);
		int py = MARGIN;
		int pw = PANEL_W;
		int ph = getPanelH(screenH);

		// Window frame from unified UiKit
		UiKit.drawWindow(g, px, py, pw, ph, HEADER_H, FOOTER_H);

		// Quick Tab Bar in Header (Icons for General + HUD Elements)
		int tabCount = elements.size() + 1;
		int tabStartX = px + 4;
		int totalTabW = pw - 8;
		int tabY = py + (HEADER_H - 18) / 2;

		String[] labels = new String[tabCount];
		labels[0] = "⚙";
		for (int i = 0; i < elements.size(); i++) {
			labels[i + 1] = elements.get(i).icon();
		}
		int selIndex = (showGeneral || selectedElement == null) ? 0 : (elements.indexOf(selectedElement) + 1);
		UiKit.drawTabs(g, font, tabStartX, tabY, totalTabW, 18, labels, selIndex, mx, my);

		// Content Area
		int bodyY = py + HEADER_H + 2;
		int bodyH = ph - HEADER_H - FOOTER_H - 4;
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
			contentH = 8 * UiKit.ITEM_H + 4;
		}

		pose.popPose();
		g.disableScissor();

		scrollbar.update(bodyH, contentH);
		if (contentH > bodyH) {
			scrollbar.render(g, px + pw - 5, bodyY);
		}

		// Footer Buttons
		int ftrY = py + ph - FOOTER_H;
		int btnW = (pw - 12) / 2;
		int btn1X = px + 4;
		int btn2X = btn1X + btnW + 4;
		int btnY = ftrY + (FOOTER_H - 18) / 2;

		UiKit.drawButton(g, font, "Reset All", btn1X, btnY, btnW, 18, mx, my, UiKit.C_DANGER, UiKit.C_DANGER_HOV, 0xFFFFFFFF);
		UiKit.drawButton(g, font, "Done", btn2X, btnY, btnW, 18, mx, my, UiKit.C_SUCCESS, UiKit.C_SUCCESS_HOV, 0xFFFFFFFF);
	}

	private int renderGeneralOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		int curY = y;
		UiKit.drawSectionHeader(g, font, "General Options", x, curY, w);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "Hide Recipe Book", noRecipeBook, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "Hide Realms Button", noRealmsButton, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "Custom Debug F3", customDebugScreen, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "Hide Other Mods (F3)", debugHideOtherMods, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "Hide Tutorial Toast", hideTutorialToast, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawToggle(g, font, "3-Step F1 Toggle", customF1, x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawEnumCycler(g, font, "Pumpkin Overlay", pumpkinOverlay.name(), x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		return curY - y;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int screenW, int screenH, List<HudElement> elements, HudElement[] selectedRef, Runnable onResetAll, Runnable onClose) {
		if (button != 0) return false;
		int mx = (int) mouseX, my = (int) mouseY;

		int px = getPanelX(screenW);
		int py = MARGIN;
		int pw = PANEL_W;
		int ph = getPanelH(screenH);

		if (!UiKit.hit(mx, my, px, py, pw, ph)) return false;

		// Header Tab clicks
		int tabCount = elements.size() + 1;
		int tabStartX = px + 4;
		int totalTabW = pw - 8;
		int tabY = py + (HEADER_H - 18) / 2;

		int clickedTab = UiKit.getClickedTab(tabStartX, tabY, totalTabW, 18, tabCount, mx, my);
		if (clickedTab == 0) {
			showGeneral = true;
			selectedRef[0] = null;
			return true;
		} else if (clickedTab > 0 && clickedTab <= elements.size()) {
			showGeneral = false;
			selectedRef[0] = elements.get(clickedTab - 1);
			return true;
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
		int bodyY = py + HEADER_H + 2;
		int bodyH = ph - HEADER_H - FOOTER_H - 4;
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
		int curY = y + UiKit.ITEM_H; // skip header

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			noRecipeBook = !noRecipeBook;
			saveGeneralConfig();
			return true;
		}
		curY += UiKit.ITEM_H;

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			noRealmsButton = !noRealmsButton;
			saveGeneralConfig();
			return true;
		}
		curY += UiKit.ITEM_H;

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			customDebugScreen = !customDebugScreen;
			saveGeneralConfig();
			return true;
		}
		curY += UiKit.ITEM_H;

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			debugHideOtherMods = !debugHideOtherMods;
			saveGeneralConfig();
			return true;
		}
		curY += UiKit.ITEM_H;

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			hideTutorialToast = !hideTutorialToast;
			saveGeneralConfig();
			return true;
		}
		curY += UiKit.ITEM_H;

		if (UiKit.isToggleHit(mx, my, x, curY, w)) {
			customF1 = !customF1;
			saveGeneralConfig();
			return true;
		}
		curY += UiKit.ITEM_H;

		if (UiKit.isEnumHit(mx, my, x, curY, w)) {
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
		int py = MARGIN;
		int pw = PANEL_W;
		int ph = getPanelH(screenH);

		if (UiKit.hit((int) mouseX, (int) mouseY, px, py, pw, ph)) {
			scrollbar.handleScroll(scrollY, 14);
			return true;
		}
		return false;
	}
}
