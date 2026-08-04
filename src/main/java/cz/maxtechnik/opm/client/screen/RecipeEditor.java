package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.RecipeFileManager;
import cz.maxtechnik.opm.client.recipe.RecipeFileManager.SaveResult;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;

import cz.maxtechnik.opm.client.util.ItemDragHandler;
import cz.maxtechnik.opm.client.util.Scrollbar;
import cz.maxtechnik.opm.client.screen.layout.StationLayoutEngine;
import cz.maxtechnik.opm.client.widget.BottomInventoryPanel;
import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import cz.maxtechnik.opm.client.widget.FileNameInputHandler;
import cz.maxtechnik.opm.client.widget.InlineNumberEditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecipeEditor extends Screen {

	private final Screen parent;
	final RecipeEditorData data;
	private final EditorRenderer renderer;
	final List<StationType> tabs = new ArrayList<>();
	private int tabIndex = 0;

	// Layout
	private int panelX, panelY, panelW, panelH;
	private int leftWidth, rightPanelX, rightWidth;
	private int editorTop, editorHeight, inventoryTop;
	private int saveBtnX, saveBtnY, clearBtnX, copyBtnX;

	// Splitter mezi editorem a inventářem
	private int inventoryPanelHeight = 150;
	private boolean isDraggingSplitter;

	// Widgety
	private final ItemDragHandler dragHandler = new ItemDragHandler();
	private final FileNameInputHandler fileInput = new FileNameInputHandler("my_recipe");
	private final InlineNumberEditor numEditor = new InlineNumberEditor();
	private final Scrollbar editorScrollbar = new Scrollbar();
	private long lastClickTime = 0;

	private String currentJson = "";
	private CodeViewerWidget codeViewer;
	private BottomInventoryPanel bottomPanel;

	public RecipeEditor(Screen parent) {
		super(Component.literal("Recipe Editor"));
		this.parent = parent;
		this.data = new RecipeEditorData();
		this.renderer = new EditorRenderer(null, data);
		this.tabs.addAll(StationType.getAvailableStations());
	}

	@Override
	protected void init() {
		super.init();
		panelX = 0;
		panelY = 0;
		panelW = width;
		panelH = height;

		renderer.font = font;
		data.loadConfig(minecraft, h -> inventoryPanelHeight = Math.clamp(h, 80, panelH - 100));
		updateLayout();

		data.loadFluids();
		data.loadAllItems();
		data.loadTags();
		data.loadFavorites(minecraft);
		data.scanSavedRecipes();

		bottomPanel = new BottomInventoryPanel(font, data);
		bottomPanel.init(panelX, inventoryTop);
	}

	private void updateLayout() {
		leftWidth = panelW * 55 / 100;
		rightPanelX = panelX + leftWidth + 4;
		rightWidth = panelW - leftWidth - 4;

		editorTop = panelY + EditorRenderer.TAB_H + 2;
		inventoryTop = panelY + panelH - inventoryPanelHeight;
		editorHeight = inventoryTop - editorTop - 10;

		editorScrollbar.update(editorHeight, 300);

		saveBtnX = panelX + 10;
		saveBtnY = panelY + EditorRenderer.TAB_H + 5;
		clearBtnX = saveBtnX + 95;
		copyBtnX = clearBtnX + 45;

		renderer.updateLayout(panelX, panelY, panelW, panelH, leftWidth, rightPanelX, rightWidth, editorTop, editorHeight, inventoryTop, saveBtnX, saveBtnY, clearBtnX, copyBtnX);
		if (bottomPanel != null) bottomPanel.updateLayout(panelX, inventoryTop);
		if (codeViewer != null) codeViewer.setBounds(rightPanelX, panelY, rightWidth, panelH);
	}

	@Override
	public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
		renderer.renderBg(g);
		renderer.renderTabs(g, mx, my, tabs, tabIndex);

		g.enableScissor(panelX, editorTop, panelX + leftWidth - 6, inventoryTop - 4);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -editorScrollbar.scroll, 0);

		int scrolledY = (int) (my + editorScrollbar.scroll);
		int contentH = renderer.renderStation(g, font, tabs.get(tabIndex), mx, scrolledY);
		editorScrollbar.update(editorHeight, contentH);

		pose.popPose();
		g.disableScissor();

		editorScrollbar.render(g, panelX + leftWidth - 6, editorTop);
		refreshCodeViewer();
		codeViewer.render(g, mx, my);
		renderer.renderBtnBar(g, mx, my, fileInput.getFileName(), fileInput.isFocused(), fileInput.getCursor());

		bottomPanel.render(g, panelX, panelY, panelH, leftWidth, inventoryTop, mx, my);

		dragHandler.render(g, font, mx, my);
		numEditor.render(g, mx, my, pt);

		if (!dragHandler.hasStack()) {
			ItemStack hovered = slotAt(mx, my);
			if (hovered != null && !hovered.isEmpty()) renderer.showTip(g, hovered, mx, my);
		}
		if (data.popupError != null) renderer.renderErrorPopup(g, mx, my, data.popupError, width, height);
		super.render(g, mx, my, pt);
	}

	private void refreshCodeViewer() {
		String json = data.buildJson(tabs, tabIndex);
		if (!json.equals(currentJson)) {
			currentJson = json;
			codeViewer = new CodeViewerWidget(font, currentJson);
			codeViewer.setBounds(rightPanelX, panelY, rightWidth, panelH);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int mx = (int) mouseX, my = (int) mouseY;

		// Chybový popup – musí se zavřít dřív
		if (data.popupError != null) {
			if (renderer.hit(mx, my, width / 2 - 40, height / 2 + 35, 80, 20)) data.popupError = null;
			return true;
		}

		if (editorScrollbar.mouseClicked(mx, my, button)) return true;

		// Táhlo splitteru mezi editorem a inventářem
		if (button == 0 && renderer.hit(mx, my, panelX, inventoryTop - 4, leftWidth, 8)) {
			isDraggingSplitter = true;
			return true;
		}

		// Dvojklik v editoru
		if (button == 0 && isInsideEditor(mx, my)) {
			long now = System.currentTimeMillis();
			if (now - lastClickTime < 400 && handleDoubleClick(mx, (int) (my + editorScrollbar.scroll))) {
				lastClickTime = 0;
				return true;
			}
			lastClickTime = now;
		}

		// Pole pro název souboru
		if (button == 0 && fileInput.handleClick(mx, my, panelX + 180, saveBtnY, font.width(fileInput.getFileName()) + 12, 16)) {
			return true;
		}

		// Záložky stanic
		if (button == 0 && my < editorTop && mx < panelX + leftWidth) {
			int tabW = leftWidth / tabs.size();
			int idx = (mx - panelX) / tabW;
			if (idx >= 0 && idx < tabs.size() && idx != tabIndex) {
				tabIndex = idx;
				editorScrollbar.scroll = 0;
				return true;
			}
		}

		// Tlačítka panelu
		if (button == 0 && renderer.hit(mx, my, saveBtnX, saveBtnY, 90, 16)) { save(); return true; }
		if (button == 0 && renderer.hit(mx, my, clearBtnX, saveBtnY, 40, 16)) {
			data.clear();
			data.selectedRecipeFile = null;
			data.selectedRecipeFiles.clear();
			fileInput.setFileName("my_recipe");
			return true;
		}
		if (button == 0 && renderer.hit(mx, my, copyBtnX, saveBtnY, 60, 16)) {
			if (minecraft != null) {
				minecraft.keyboardHandler.setClipboard(currentJson);
				data.status("Copied!", true);
			}
			return true;
		}

		// Spodní panel (inventář / oblíbené / recepty)
		boolean hitBottom = bottomPanel.mouseClicked(panelX, panelY, panelH, inventoryTop, mx, my, button, new BottomInventoryPanel.RecipeSelectionListener() {
			@Override
			public void onRecipeSelected(File file) {
				StationType loadedType = data.loadRecipeFile(file);
				if (loadedType != null) {
					data.selectedRecipeFile = file;
					String name = file.getName();
					if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
					fileInput.setFileName(name);
					int idx = tabs.indexOf(loadedType);
					if (idx >= 0) tabIndex = idx;
				} else {
					data.status("Could not load recipe file!", false);
				}
			}

			@Override
			public void onRecipeDeleted() {
				deleteRecipe();
			}
		});
		if (hitBottom) return true;
		else if (bottomPanel != null) bottomPanel.unfocusSearch();

		if (codeViewer != null && codeViewer.mouseClicked(mx, my, button)) return true;

		// Kliknutí na sloty receptu
		if (isInsideEditor(mx, my)) {
			int scrolledY = (int) (my + editorScrollbar.scroll);
			if (handleEditorClicks(mx, scrolledY)) return true;
			for (RecipeSlotManager.SlotPos slot : getSlots()) {
				if (renderer.hit(mx, scrolledY, slot.x(), slot.y(), slot.size(), slot.size())) {
					return handleSlotClick(slot, button);
				}
			}
		}

		// Kliknutí v inventáři (spodní oblast)
		if (!showRecipesList() && my >= inventoryTop) {
			return handleInventoryClick(mx, my, button);
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleSlotClick(RecipeSlotManager.SlotPos slot, int button) {
		ItemStack current = slot.get().get();
		if (dragHandler.hasStack()) {
			dragHandler.handleSlotClick(current, slot.set(), hasControlDown(), button == 1);
			return true;
		}
		if (button == 0 && !current.isEmpty()) {
			if (hasControlDown()) {
				addToFavorites(current);
			} else {
				dragHandler.pick(current);
				slot.set().accept(ItemStack.EMPTY);
			}
			return true;
		}
		if (button == 1) {
			slot.set().accept(ItemStack.EMPTY);
			return true;
		}
		return false;
	}


	private boolean handleInventoryClick(int mx, int my, int button) {
		ItemStack picked = itemAt(mx, my);
		if (!picked.isEmpty()) {
			if (button == 0) {
				if (hasControlDown()) addToFavorites(picked);
				else if (hasShiftDown()) removeFromFavorites(picked);
				else dragHandler.pick(picked);
				return true;
			} else if (button == 1 && dragHandler.hasStack()) {
				dragHandler.clear();
				return true;
			}
		} else if (button == 1 && dragHandler.hasStack()) {
			dragHandler.clear();
			return true;
		}
		return false;
	}

	private void addToFavorites(ItemStack stack) {
		if (data.favorites.stream().noneMatch(f -> ItemStack.isSameItemSameComponents(f, stack))) {
			data.favorites.add(stack.copy());
			if (minecraft != null) data.saveFavorites(minecraft);
		}
	}

	private void removeFromFavorites(ItemStack stack) {
		if (data.favorites.removeIf(f -> ItemStack.isSameItemSameComponents(f, stack))) {
			if (minecraft != null) data.saveFavorites(minecraft);
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		int mx = (int) mouseX, my = (int) mouseY;
		if (editorScrollbar.mouseDragged(my)) return true;
		if (bottomPanel != null && bottomPanel.mouseDragged(my)) return true;
		if (isDraggingSplitter) {
			inventoryPanelHeight = panelH - my;
			updateLayout();
			return true;
		}
		if (dragHandler.hasStack() && isInsideEditor(mx, my)) {
			int scrolledY = (int) (my + editorScrollbar.scroll);
			List<RecipeSlotManager.SlotPos> slots = getSlots();
			for (int i = 0; i < slots.size(); i++) {
				RecipeSlotManager.SlotPos slot = slots.get(i);
				if (renderer.hit(mx, scrolledY, slot.x(), slot.y(), slot.size(), slot.size())) {
					dragHandler.paintSlot(i, slot.set());
					return true;
				}
			}
		}
		if (codeViewer != null && codeViewer.mouseDragged(my)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		editorScrollbar.mouseReleased();
		if (bottomPanel != null) bottomPanel.mouseReleased();
		dragHandler.resetPaintIndex();
		if (isDraggingSplitter) {
			isDraggingSplitter = false;
			if (minecraft != null) data.saveConfig(minecraft, inventoryPanelHeight);
			return true;
		}
		if (codeViewer != null) codeViewer.mouseReleased();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double sy) {
		int mx = (int) mouseX, my = (int) mouseY;

		if (isInsideEditor(mx, my)) {
			int scrolledY = (int) (my + editorScrollbar.scroll);
			if (scrollOverSlot(mx, scrolledY, sy)) return true;
			if (StationLayoutEngine.handleScrollSpinners(tabs.get(tabIndex), data, panelX + leftWidth / 2, editorTop, mx, scrolledY, sy)) return true;
			editorScrollbar.handleScroll(sy, 12);
			return true;
		}

		if (bottomPanel.mouseScrolled(panelX, panelH, inventoryTop, mx, my, sy)) return true;
		if (codeViewer != null && codeViewer.mouseScrolled(sy, mx, my)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, sy);
	}

	private boolean scrollOverSlot(int mx, int scrolledY, double delta) {
		for (RecipeSlotManager.SlotPos slot : getSlots()) {
			if (renderer.hit(mx, scrolledY, slot.x(), slot.y(), slot.size(), slot.size())) {
				ItemStack stack = slot.get().get();
				if (!stack.isEmpty()) {
					stack.setCount(Math.clamp(stack.getCount() + (int) delta, 1, 64));
					return true;
				}
			}
		}
		return false;
	}

	private boolean handleEditorClicks(int mx, int scrolledY) {
		StationType type = tabs.get(tabIndex);
		int centerX = panelX + leftWidth / 2;
		var layout = StationLayoutEngine.getLayout(type, data);
		int cy = editorTop + 15;
		if (layout.getHeaderToggle() != null) {
			layout.getHeaderToggle().setAnchor(centerX, cy);
			if (layout.getHeaderToggle().handleClick(mx, scrolledY, font)) return true;
			cy += 25;
		}
		if (layout.getSubToggle() != null) {
			layout.getSubToggle().setAnchor(centerX, cy);
			if (layout.getSubToggle().handleClick(mx, scrolledY, font)) return true;
		}
		if (type == StationType.MECH_CRAFTING) {
			int gridY = editorTop + 50, sz = 16, pad = 1, gridW = 9 * (sz + pad);
			int gridStartX = centerX - gridW / 2 - 40;
			int arrowX = gridStartX + gridW + 15;
			int arrowY = gridY + (9 * (sz + pad)) / 2 - 4;
			int bx = arrowX + 20, by = arrowY + 20, bw = 14, bh = 12;
			if (renderer.hit(mx, scrolledY, bx, by, bw, bh))         { shiftMechGrid(0, -1); return true; }
			if (renderer.hit(mx, scrolledY, bx + bw + 2, by, bw, bh)) { shiftMechGrid(0,  1); return true; }
			if (renderer.hit(mx, scrolledY, bx, by + bh + 2, bw, bh)) { shiftMechGrid(-1, 0); return true; }
			if (renderer.hit(mx, scrolledY, bx + bw + 2, by + bh + 2, bw, bh)) { shiftMechGrid(1, 0); return true; }
		}
		return StationLayoutEngine.handleSpinnerClicks(type, data, centerX, editorTop, mx, scrolledY)
				|| StationLayoutEngine.handleFluidSpins(type, data, mx, scrolledY);
	}

	private boolean handleDoubleClick(int mx, int scrolledY) {
		int centerX = panelX + leftWidth / 2;
		return StationLayoutEngine.handleDoubleClick(tabs.get(tabIndex), data, centerX, editorTop, mx, scrolledY,
				(field, bx, by, bw, value, idx) -> numEditor.startEdit(font, field, bx, by, bw, value, idx, editorScrollbar.scroll));
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (numEditor.keyPressed(key, scan, mods, data, tabs.get(tabIndex))) return true;

		if (key == 256) { // ESC
			if (fileInput.isFocused()) { fileInput.setFocused(false); return true; }
			onClose();
			return true;
		}
		if (bottomPanel != null) {
			if (!showRecipesList() && bottomPanel.getBottomTab() != BottomInventoryPanel.BottomTab.INVENTORY && bottomPanel.getSearchBox().isFocused()) {
				bottomPanel.getSearchBox().keyPressed(key, scan, mods);
				return true;
			}
			if (showRecipesList() && bottomPanel.getRecipeSearchBox() != null && bottomPanel.getRecipeSearchBox().isFocused()) {
				bottomPanel.getRecipeSearchBox().keyPressed(key, scan, mods);
				return true;
			}
		}
		if (showRecipesList() && key == 261 && !fileInput.isFocused()
				&& (bottomPanel.getRecipeSearchBox() == null || !bottomPanel.getRecipeSearchBox().isFocused())) {
			deleteRecipe();
			return true;
		}
		if (fileInput.keyPressed(key)) return true;

		if (!numEditor.isActive() && (bottomPanel == null || !bottomPanel.isSearchFocused())) {
			if (tabs.get(tabIndex) == StationType.MECH_CRAFTING) {
				if (key == 87 || key == 265) { shiftMechGrid(0, -1); return true; }
				if (key == 83 || key == 264) { shiftMechGrid(0,  1); return true; }
				if (key == 65 || key == 263) { shiftMechGrid(-1, 0); return true; }
				if (key == 68 || key == 262) { shiftMechGrid(1,  0); return true; }
			}
		}

		if (codeViewer != null && codeViewer.keyPressed(key, mods)) return true;
		return super.keyPressed(key, scan, mods);
	}

	@Override
	public boolean charTyped(char chr, int mods) {
		if (numEditor.charTyped(chr, mods)) return true;
		if (bottomPanel != null) {
			if (!showRecipesList() && bottomPanel.getBottomTab() != BottomInventoryPanel.BottomTab.INVENTORY && bottomPanel.getSearchBox().isFocused()) {
				bottomPanel.getSearchBox().charTyped(chr, mods);
				return true;
			}
			if (showRecipesList() && bottomPanel.getRecipeSearchBox() != null && bottomPanel.getRecipeSearchBox().isFocused()) {
				bottomPanel.getRecipeSearchBox().charTyped(chr, mods);
				return true;
			}
		}
		if (fileInput.charTyped(chr)) return true;
		if (codeViewer != null && codeViewer.charTyped(chr)) return true;
		return super.charTyped(chr, mods);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(parent);
	}

	// ── Privátní helpery ──────────────────────────────────────────────────

	private void save() {
		String json = data.buildJson(tabs, tabIndex);
		SaveResult result = RecipeFileManager.saveRecipe(fileInput.getFileName(), json);
		if (result.success()) {
			data.scanSavedRecipes();
			data.selectedRecipeFile = result.savedFile();
			data.status("Saved!", true);
		} else {
			data.status("Save failed!", false);
		}
	}

	private void deleteRecipe() {
		if (RecipeFileManager.deleteRecipes(data.selectedRecipeFiles)) {
			if (data.selectedRecipeFile != null && data.selectedRecipeFiles.contains(data.selectedRecipeFile)) {
				data.selectedRecipeFile = null;
				fileInput.setFileName("");
				data.clear();
			}
			data.selectedRecipeFiles.clear();
			data.scanSavedRecipes();
		}
	}

	private void shiftMechGrid(int dx, int dy) {
		cz.maxtechnik.opm.client.screen.layout.GridShiftHelper.shiftGrid(data.mechGrid, 9, 9, dx, dy);
	}

	/** Všechny interaktivní sloty pro aktuální stanici. */
	private List<RecipeSlotManager.SlotPos> getSlots() {
		return RecipeSlotManager.getItemSlots(tabs.get(tabIndex), data, panelX, leftWidth, editorTop);
	}

	/** Vrátí ItemStack na dané pozici myši (editor slot nebo inventář). */
	private ItemStack slotAt(int mx, int my) {
		return RecipeSlotManager.getSlotItemAt(tabs.get(tabIndex), data, panelX, leftWidth, editorTop, inventoryTop, editorScrollbar.scroll, mx, my, bottomPanel, panelH);
	}

	private ItemStack itemAt(int mx, int my) {
		return bottomPanel.itemAt(panelX, panelH, inventoryTop, mx, my);
	}

	/** Vrátí true pokud je myš v oblasti editoru receptu (levý panel). */
	private boolean isInsideEditor(int mx, int my) {
		return my >= editorTop && my < inventoryTop - 20 && mx >= panelX && mx < panelX + leftWidth;
	}

	private boolean showRecipesList() {
		return bottomPanel != null && bottomPanel.isShowingRecipesList();
	}
}
