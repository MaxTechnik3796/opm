package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.handler.F1Handler;
import cz.maxtechnik.opm.client.ui.Scrollbar;
import cz.maxtechnik.opm.client.ui.UiKit;
import cz.maxtechnik.opm.client.ui.UiScale;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Moderní postranní panel umístěný vpravo, s dynamickou výškou podle obsahu,
 * čistým generátorovým designem a možností kompletního sbalení pro maximální přehlednost plátna.
 */
public final class ConfigSidebar {
	public enum SidebarTab { GENERAL, DATAPACK, ELEMENT }

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

	private final EditBox worldBox;
	private final EditBox datapackBox;
	private final EditBox namespaceBox;

	private final Scrollbar scrollbar = new Scrollbar();
	private SidebarTab activeTab = SidebarTab.GENERAL;
	private boolean collapsed = false;

	private record BoolOption(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {}

	public ConfigSidebar() {
		Font font = Minecraft.getInstance().font;
		this.worldBox = new EditBox(font, 0, 0, 100, 16, Component.literal("World"));
		this.datapackBox = new EditBox(font, 0, 0, 100, 16, Component.literal("Datapack"));
		this.namespaceBox = new EditBox(font, 0, 0, 100, 16, Component.literal("Namespace"));

		this.worldBox.setResponder(v -> { OpmConfig.WORLD_NAME.set(v); OpmConfig.SPEC.save(); });
		this.datapackBox.setResponder(v -> { OpmConfig.DATAPACK_NAME.set(v); OpmConfig.SPEC.save(); });
		this.namespaceBox.setResponder(v -> { OpmConfig.RECIPE_FOLDER.set(v); OpmConfig.SPEC.save(); });

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

		this.worldBox.setValue(OpmConfig.WORLD_NAME.get());
		this.datapackBox.setValue(OpmConfig.DATAPACK_NAME.get());
		this.namespaceBox.setValue(OpmConfig.RECIPE_FOLDER.get());
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

	private List<BoolOption> getBoolOptions() {
		return List.of(
				new BoolOption("Hide Recipe Book", () -> noRecipeBook, v -> noRecipeBook = v),
				new BoolOption("Hide Realms Button", () -> noRealmsButton, v -> noRealmsButton = v),
				new BoolOption("Custom Debug F3", () -> customDebugScreen, v -> customDebugScreen = v),
				new BoolOption("Hide Other Mods (F3)", () -> debugHideOtherMods, v -> debugHideOtherMods = v),
				new BoolOption("Hide Tutorial Toast", () -> hideTutorialToast, v -> hideTutorialToast = v),
				new BoolOption("3-Step F1 Toggle", () -> customF1, v -> customF1 = v)
		);
	}

	public SidebarTab getActiveTab() {
		return activeTab;
	}

	public void setActiveTab(SidebarTab tab) {
		this.activeTab = tab;
		this.scrollbar.scroll = 0;
		unfocusAll();
	}

	public void unfocusAll() {
		worldBox.setFocused(false);
		datapackBox.setFocused(false);
		namespaceBox.setFocused(false);
	}

	public void toggleCollapsed() {
		this.collapsed = !this.collapsed;
	}

	public int getPanelW(int screenW) {
		return Math.clamp((int) (screenW * 0.38f), 150, PANEL_W);
	}

	public int getPanelX(int screenW) {
		return screenW - getPanelW(screenW) - MARGIN;
	}

	public int getContentHeight(HudElement selectedElement) {
		if (activeTab == SidebarTab.GENERAL) {
			return (2 + getBoolOptions().size() + 1) * UiKit.ITEM_H;
		}
		if (activeTab == SidebarTab.DATAPACK) {
			return 170;
		}
		return selectedElement != null ? selectedElement.getInspectorHeight() : 0;
	}

	public int getPanelH(int screenH, HudElement selectedElement) {
		int contentH = getContentHeight(selectedElement);
		int desiredH = HEADER_H + contentH + FOOTER_H + 4;
		return Math.min(desiredH, screenH - (MARGIN * 2));
	}

	public void render(GuiGraphics g, Font font, int screenW, int screenH, int mx, int my, List<HudElement> elements, HudElement selectedElement) {
		if (collapsed) {
			int pillW = 24, pillH = 24;
			int pillX = screenW - pillW - 8;
			int pillY = MARGIN;
			boolean hov = UiKit.hit(mx, my, pillX, pillY, pillW, pillH);

			if (hov) {
				g.fill(pillX, pillY, pillX + pillW, pillY + pillH, UiKit.C_CARD_HOV);
				UiKit.drawOutline(g, pillX, pillY, pillW, pillH, UiKit.C_ACCENT);
			} else {
				g.fill(pillX, pillY, pillX + pillW, pillY + pillH, UiKit.C_POPUP_BG);
				UiKit.drawOutline(g, pillX, pillY, pillW, pillH, UiKit.C_BORDER);
			}
			g.drawCenteredString(font, "⚙", pillX + pillW / 2, pillY + (pillH - 8) / 2, hov ? UiKit.C_ACCENT_HOV : UiKit.C_LABEL);
			return;
		}

		int pw = getPanelW(screenW);
		int px = getPanelX(screenW);
		int py = MARGIN;
		int ph = getPanelH(screenH, selectedElement);

		// Frame
		UiKit.drawWindow(g, px, py, pw, ph, HEADER_H, FOOTER_H);

		// Header Tabs + Collapse Button
		int collapseBtnW = 18;
		int tabH = HEADER_H - 2;
		int totalTabW = pw - collapseBtnW - 2;
		int tabStartX = px + 1;
		int tabY = py + 1;
		int tabCount = elements.size() + 2;

		String[] labels = new String[tabCount];
		labels[0] = "⚙";
		labels[1] = "📦";
		for (int i = 0; i < elements.size(); i++) {
			labels[i + 2] = elements.get(i).icon();
		}
		int selIndex = (activeTab == SidebarTab.GENERAL) ? 0 : (activeTab == SidebarTab.DATAPACK ? 1 : (elements.indexOf(selectedElement) + 2));
		UiKit.drawTabs(g, font, tabStartX, tabY, totalTabW, tabH, labels, selIndex, mx, my);

		int cBtnX = px + pw - collapseBtnW - 1;
		boolean cHov = UiKit.hit(mx, my, cBtnX, tabY, collapseBtnW, tabH);
		g.fill(cBtnX, tabY, cBtnX + collapseBtnW, tabY + tabH, cHov ? UiKit.C_CARD_HOV : UiKit.C_HEADER);
		g.drawCenteredString(font, "▶", cBtnX + collapseBtnW / 2, tabY + (tabH - 8) / 2, cHov ? UiKit.C_ACCENT_HOV : UiKit.C_LABEL);

		// Content Area
		int bodyY = py + HEADER_H + 2;
		int bodyH = ph - HEADER_H - FOOTER_H - 4;
		int bodyW = pw - 8;
		int bodyX = px + 4;
		int contentH = getContentHeight(selectedElement);

		g.enableScissor(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -scrollbar.scroll, 0);

		int scrolledY = (int) (my + scrollbar.scroll);

		if (activeTab == SidebarTab.GENERAL) {
			renderGeneralOptions(g, font, bodyX, bodyY, bodyW, mx, scrolledY);
		} else if (activeTab == SidebarTab.DATAPACK) {
			renderDatapackOptions(g, font, bodyX, bodyY, bodyW, mx, scrolledY);
		} else if (selectedElement != null) {
			selectedElement.renderInspector(g, font, bodyX, bodyY, bodyW, mx, scrolledY);
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

		UiKit.drawButton(g, font, "Reset All", btn1X, btnY, btnW, 18, mx, my, UiKit.C_SLOT, UiKit.C_SLOT_HOV, UiKit.C_TEXT);
		UiKit.drawButton(g, font, "Done", btn2X, btnY, btnW, 18, mx, my, UiKit.C_SUCCESS, UiKit.C_SUCCESS_HOV, UiKit.C_WHITE);
	}

	private void renderGeneralOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		int curY = y;
		UiKit.drawSectionHeader(g, font, "General Options", x, curY, w);
		curY += UiKit.ITEM_H;

		Minecraft mc = Minecraft.getInstance();
		UiKit.drawStepper(g, font, "GUI Scale", UiScale.getLabel(mc), x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		for (var opt : getBoolOptions()) {
			UiKit.drawToggle(g, font, opt.label(), opt.getter().get(), x, curY, w, mx, my);
			curY += UiKit.ITEM_H;
		}

		UiKit.drawEnumCycler(g, font, "Pumpkin Overlay", pumpkinOverlay.name(), x, curY, w, mx, my);
	}

	private void renderDatapackOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		int curY = y;
		UiKit.drawSectionHeader(g, font, "Datapack Export", x, curY, w);
		curY += UiKit.ITEM_H;

		g.drawString(font, "World (saves folder):", x + 4, curY + 2, UiKit.C_TEXT, false);
		curY += 13;
		worldBox.setX(x + 2);
		worldBox.setY(curY);
		worldBox.setWidth(w - 4);
		UiKit.drawInputField(g, font, worldBox.getValue(), "e.g. New World", worldBox.getCursorPosition(), worldBox.isFocused(), x + 2, curY, w - 4, 16);
		curY += 22;

		g.drawString(font, "Datapack (folder):", x + 4, curY + 2, UiKit.C_TEXT, false);
		curY += 13;
		datapackBox.setX(x + 2);
		datapackBox.setY(curY);
		datapackBox.setWidth(w - 4);
		UiKit.drawInputField(g, font, datapackBox.getValue(), "e.g. dif_data", datapackBox.getCursorPosition(), datapackBox.isFocused(), x + 2, curY, w - 4, 16);
		curY += 22;

		g.drawString(font, "Namespace (data/):", x + 4, curY + 2, UiKit.C_TEXT, false);
		curY += 13;
		namespaceBox.setX(x + 2);
		namespaceBox.setY(curY);
		namespaceBox.setWidth(w - 4);
		UiKit.drawInputField(g, font, namespaceBox.getValue(), "e.g. dif (or empty)", namespaceBox.getCursorPosition(), namespaceBox.isFocused(), x + 2, curY, w - 4, 16);
		curY += 24;

		boolean hovClear = UiKit.hit(mx, my, x + 2, curY + 1, w - 4, UiKit.ITEM_H - 2);
		UiKit.drawGhostButton(g, font, "Clear Paths", x + 2, curY + 1, w - 4, UiKit.ITEM_H - 2, hovClear, UiKit.C_CARD_HOV, UiKit.C_DANGER_TEXT);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int screenW, int screenH, List<HudElement> elements, HudElement[] selectedRef, Runnable onResetAll, Runnable onClose) {
		if (button != 0) return false;
		int mx = (int) mouseX, my = (int) mouseY;

		if (collapsed) {
			int pillW = 24, pillH = 24;
			int pillX = screenW - pillW - 8;
            if (UiKit.hit(mx, my, pillX, MARGIN, pillW, pillH)) {
				collapsed = false;
				return true;
			}
			return false;
		}

		int pw = getPanelW(screenW);
		int px = getPanelX(screenW);
		int py = MARGIN;
		int ph = getPanelH(screenH, selectedRef[0]);

		if (!UiKit.hit(mx, my, px, py, pw, ph)) return false;

		// Header Tab clicks & Collapse button click
		int collapseBtnW = 18;
		int tabH = HEADER_H - 2;
		int totalTabW = pw - collapseBtnW - 2;
		int tabStartX = px + 1;
		int tabY = py + 1;
		int tabCount = elements.size() + 2;

		int cBtnX = px + pw - collapseBtnW - 1;
		if (UiKit.hit(mx, my, cBtnX, tabY, collapseBtnW, tabH)) {
			collapsed = true;
			return true;
		}

		int clickedTab = UiKit.getClickedTab(tabStartX, tabY, totalTabW, tabH, tabCount, mx, my);
		if (clickedTab == 0) {
			setActiveTab(SidebarTab.GENERAL);
			selectedRef[0] = null;
			return true;
		} else if (clickedTab == 1) {
			setActiveTab(SidebarTab.DATAPACK);
			selectedRef[0] = null;
			return true;
		} else if (clickedTab >= 2 && clickedTab < tabCount) {
			setActiveTab(SidebarTab.ELEMENT);
			selectedRef[0] = elements.get(clickedTab - 2);
			return true;
		}

		// Footer Button clicks
		int ftrY = py + ph - FOOTER_H;
		int btnW = (pw - 12) / 2;
		int btn1X = px + 4;
		int btn2X = btn1X + btnW + 4;
		int btnY = ftrY + (FOOTER_H - 18) / 2;

		if (UiKit.hit(mx, my, btn1X, btnY, btnW, 18)) {
			if (onResetAll != null) onResetAll.run();
			return true;
		}
		if (UiKit.hit(mx, my, btn2X, btnY, btnW, 18)) {
			if (onClose != null) onClose.run();
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
			if (activeTab == SidebarTab.GENERAL) {
				handleGeneralClick(mx, scrolledY, bodyX, bodyY, bodyW);
			} else if (activeTab == SidebarTab.DATAPACK) {
				handleDatapackClick(mx, scrolledY, bodyX, bodyY, bodyW);
			} else if (selectedRef[0] != null) {
				selectedRef[0].handleInspectorClick(mx, scrolledY, bodyX, bodyY, bodyW);
			}
			return true;
		}

		return true;
	}

	private boolean handleGeneralClick(int mx, int my, int x, int y, int w) {
		int curY = y + UiKit.ITEM_H; // skip header

		Minecraft mc = Minecraft.getInstance();
		int step = UiKit.getStepperClick(mx, my, x, curY, w);
		if (step != 0) {
			UiScale.adjustScale(mc, step);
			return true;
		}
		curY += UiKit.ITEM_H;

		for (var opt : getBoolOptions()) {
			if (UiKit.isToggleHit(mx, my, x, curY, w)) {
				opt.setter().accept(!opt.getter().get());
				saveGeneralConfig();
				return true;
			}
			curY += UiKit.ITEM_H;
		}

		if (UiKit.isEnumHit(mx, my, x, curY, w)) {
			OpmConfig.PumpkinMode[] vals = OpmConfig.PumpkinMode.values();
			pumpkinOverlay = vals[(pumpkinOverlay.ordinal() + 1) % vals.length];
			saveGeneralConfig();
			return true;
		}

		return false;
	}

	private boolean handleDatapackClick(int mx, int my, int x, int y, int w) {
		int curY = y + UiKit.ITEM_H; // skip header
		curY += 13;
		boolean hitW = UiKit.hit(mx, my, x + 2, curY, w - 4, 16);
		curY += 22 + 13;
		boolean hitD = UiKit.hit(mx, my, x + 2, curY, w - 4, 16);
		curY += 22 + 13;
		boolean hitN = UiKit.hit(mx, my, x + 2, curY, w - 4, 16);
		curY += 24;
		boolean hitClear = UiKit.hit(mx, my, x + 2, curY + 1, w - 4, UiKit.ITEM_H - 2);

		unfocusAll();
		if (hitW) { worldBox.setFocused(true); worldBox.mouseClicked(mx, my, 0); return true; }
		if (hitD) { datapackBox.setFocused(true); datapackBox.mouseClicked(mx, my, 0); return true; }
		if (hitN) { namespaceBox.setFocused(true); namespaceBox.mouseClicked(mx, my, 0); return true; }
		if (hitClear) {
			worldBox.setValue("");
			datapackBox.setValue("");
			namespaceBox.setValue("");
			OpmConfig.WORLD_NAME.set("");
			OpmConfig.DATAPACK_NAME.set("");
			OpmConfig.RECIPE_FOLDER.set("");
			OpmConfig.SPEC.save();
			return true;
		}
		return true;
	}

	public boolean keyPressed(int key, int scan, int mods) {
		if (activeTab == SidebarTab.DATAPACK) {
			if (worldBox.isFocused()) return worldBox.keyPressed(key, scan, mods);
			if (datapackBox.isFocused()) return datapackBox.keyPressed(key, scan, mods);
			if (namespaceBox.isFocused()) return namespaceBox.keyPressed(key, scan, mods);
		}
		return false;
	}

	public boolean charTyped(char chr, int mods) {
		if (activeTab == SidebarTab.DATAPACK) {
			if (worldBox.isFocused()) return worldBox.charTyped(chr, mods);
			if (datapackBox.isFocused()) return datapackBox.charTyped(chr, mods);
			if (namespaceBox.isFocused()) return namespaceBox.charTyped(chr, mods);
		}
		return false;
	}

	public boolean mouseDragged(double mouseY) {
		if (!collapsed && scrollbar.dragging) {
			scrollbar.dragTo((int) mouseY);
			return true;
		}
		return false;
	}

	public void mouseReleased() {
		scrollbar.stopDrag();
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int screenW, int screenH, HudElement selectedElement) {
		if (collapsed) return false;

		int pw = getPanelW(screenW);
		int px = getPanelX(screenW);
		int ph = getPanelH(screenH, selectedElement);

		if (UiKit.hit((int) mouseX, (int) mouseY, px, MARGIN, pw, ph)) {
			scrollbar.handleScroll(scrollY, 14);
			return true;
		}
		return false;
	}
}
