package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.RecipeFileManager;
import cz.maxtechnik.opm.client.recipe.RecipeFileManager.SaveResult;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;

import cz.maxtechnik.opm.client.util.ItemDragHandler;
import cz.maxtechnik.opm.client.util.Scrollbar;
import cz.maxtechnik.opm.client.screen.layout.SlotSpec;
import cz.maxtechnik.opm.client.screen.layout.StationLayoutEngine;
import cz.maxtechnik.opm.client.widget.BottomInventoryPanel;
import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import cz.maxtechnik.opm.client.widget.UiKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RecipeEditorScreen extends Screen {
	private final Screen parent;
	final RecipeEditorData d;
	private final EditorRenderer r;
	final List<StationType> tabs = new ArrayList<>();
	private int tabIdx = 0;

	private int invPanelHeight = 150;
	private boolean isDraggingSplitter;
	private final ItemDragHandler dragHandler = new ItemDragHandler();

	private String fileName = "my_recipe";
	private boolean fnFocused = false;
	private int fnCursor = fileName.length();

	private final Scrollbar editorSb = new Scrollbar();
	private EditBox activeNumEditBox = null;
	private String activeFieldName = null;
	private int activeFieldIdx = -1;
	private long lastClickTime = 0;

	private String curJson = "";
	private CodeViewerWidget codeViewer;
	private BottomInventoryPanel bottomPanel;

	private int pX, pY, pW, pH, leftW, rightX, rightW;
	private int editorY, editorH, invY;
	private int btnSaveX, btnSaveY, btnClearX, btnCopyX;

	public RecipeEditorScreen(Screen parent) {
		super(Component.literal("Recipe Editor"));
		this.parent = parent;
		this.d = new RecipeEditorData();
		this.r = new EditorRenderer(null, d);
		this.tabs.addAll(StationType.getAvailableStations());
	}

	@Override
	protected void init() {
		super.init();
		pX = 0;
		pY = 0;
		pW = width;
		pH = height;

		d.loadConfig(minecraft, h -> invPanelHeight = Math.clamp(h, 80, pH - 100));
		updateLayout();

		d.loadFluids();
		d.loadAllItems();
		d.loadTags();
		d.loadFavorites(minecraft);
		d.scanSavedRecipes();

		bottomPanel = new BottomInventoryPanel(font, d);
		bottomPanel.init(pX, invY, leftW, invPanelHeight);
	}

	private void updateLayout() {
		leftW = pW * 55 / 100;
		rightX = pX + leftW + 4;
		rightW = pW - leftW - 4;

		editorY = pY + EditorRenderer.TAB_H + 2;
		invY = pY + pH - invPanelHeight;
		editorH = invY - editorY - 10;

		editorSb.update(editorH, 300);

		btnSaveX = pX + 10;
		btnSaveY = pY + EditorRenderer.TAB_H + 5;
		btnClearX = btnSaveX + 95;
		btnCopyX = btnClearX + 45;

		r.updateLayout(pX, pY, pW, pH, leftW, rightX, rightW, editorY, editorH, invY, btnSaveX, btnSaveY, btnClearX, btnCopyX);
		if (bottomPanel != null) bottomPanel.updateLayout(pX, invY);
		if (codeViewer != null) codeViewer.setBounds(rightX, pY, rightW, pH);
	}

	@Override
	public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
		r.renderBg(g, mx, my);
		r.renderTabs(g, mx, my, tabs, tabIdx);

		g.enableScissor(pX, editorY, pX + leftW - 6, invY - 4);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -editorSb.scroll, 0);

		int mY = (int) (my + editorSb.scroll);
		int contentH = r.renderStation(g, tabs.get(tabIdx), mx, mY);
		editorSb.update(editorH, contentH);

		pose.popPose();
		g.disableScissor();

		editorSb.render(g, pX + leftW - 6, editorY);
		updateJson();
		codeViewer.render(g, mx, my);
		r.renderBtnBar(g, mx, my, fileName, fnFocused, fnCursor);

		bottomPanel.render(g, pX, pY, pW, pH, leftW, invY, mx, my);

		dragHandler.render(g, font, mx, my);
		if (activeNumEditBox != null) activeNumEditBox.render(g, mx, my, pt);

		if (!dragHandler.hasStack()) {
			ItemStack hs = slotAt(mx, my);
			if (hs != null && !hs.isEmpty()) r.showTip(g, hs, mx, my);
		}
		if (d.popupError != null) r.renderErrorPopup(g, mx, my, d.popupError, width, height);
		super.render(g, mx, my, pt);
	}

	private void updateJson() {
		String j = d.buildJson(tabs, tabIdx);
		if (!j.equals(curJson)) {
			curJson = j;
			codeViewer = new CodeViewerWidget(font, curJson);
			codeViewer.setBounds(rightX, pY, rightW, pH);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int mx = (int) mouseX, my = (int) mouseY;

		if (d.popupError != null) {
			if (r.hit(mx, my, width / 2 - 40, height / 2 + 35, 80, 20)) d.popupError = null;
			return true;
		}

		if (editorSb.mouseClicked(mx, my, button)) {
			return true;
		}

		if (button == 0 && r.hit(mx, my, pX, invY - 4, leftW, 8)) {
			isDraggingSplitter = true;
			return true;
		}

		if (button == 0 && my >= editorY && my < invY - 20 && mx >= pX && mx < pX + leftW) {
			long now = System.currentTimeMillis();
			if (now - lastClickTime < 400 && handleDoubleClick(mx, (int) (my + editorSb.scroll))) {
				lastClickTime = 0;
				return true;
			}
			lastClickTime = now;
		}

		if (button == 0 && r.hit(mx, my, pX + 180, btnSaveY, font.width(fileName) + 12, 16)) {
			fnFocused = true;
			fnCursor = fileName.length();
			return true;
		} else {
			fnFocused = false;
		}

		if (button == 0 && my < editorY && mx < pX + leftW) {
			int tabW = leftW / tabs.size();
			int idx = (mx - pX) / tabW;
			if (idx >= 0 && idx < tabs.size() && idx != tabIdx) {
				tabIdx = idx;
				editorSb.scroll = 0;
				return true;
			}
		}

		if (button == 0 && r.hit(mx, my, btnSaveX, btnSaveY, 90, 16)) {
			save();
			return true;
		}
		if (button == 0 && r.hit(mx, my, btnClearX, btnSaveY, 40, 16)) {
			d.clear();
			d.selectedRecipeFile = null;
			d.selectedRecipeFiles.clear();
			fileName = "my_recipe";
			fnCursor = fileName.length();
			return true;
		}
		if (button == 0 && r.hit(mx, my, btnCopyX, btnSaveY, 60, 16)) {
			if (minecraft != null) {
				minecraft.keyboardHandler.setClipboard(curJson);
				d.status("Copied!", true);
			}
			return true;
		}

		boolean bottomHit = bottomPanel.mouseClicked(pX, pY, pW, pH, leftW, invY, mx, my, button, new BottomInventoryPanel.RecipeSelectionListener() {
			@Override
			public void onRecipeSelected(File file) {
				StationType loadedType = d.loadRecipeFile(file);
				if (loadedType != null) {
					d.selectedRecipeFile = file;
					String name = file.getName();
					if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
					fileName = name;
					fnCursor = fileName.length();
					int idx = tabs.indexOf(loadedType);
					if (idx >= 0) tabIdx = idx;
				} else {
					d.status("Could not load recipe file!", false);
				}
			}

			@Override
			public void onRecipeDeleted() {
				deleteRecipe();
			}
		});

		if (bottomHit) {
			return true;
		} else {
			if (bottomPanel != null) bottomPanel.unfocusSearch();
		}

		if (codeViewer != null && codeViewer.mouseClicked(mx, my, button)) return true;

		if (my >= editorY && my < invY - 20 && mx >= pX && mx < pX + leftW) {
			int mY = (int) (my + editorSb.scroll);
			if (handleEditorClicks(mx, mY)) return true;
			List<SlotPos> slots = itemSlots(tabs.get(tabIdx));
			for (int i = 0; i < slots.size(); i++) {
				SlotPos slot = slots.get(i);
				if (r.hit(mx, mY, slot.x(), slot.y(), slot.size(), slot.size())) {
					ItemStack current = slot.get().get();
					if (dragHandler.hasStack()) {
						dragHandler.handleSlotClick(current, slot.set(), hasControlDown(), button == 1);
						return true;
					}
					if (button == 0) {
						if (!current.isEmpty()) {
							if (hasControlDown()) {
								if (d.favorites.stream().noneMatch(f -> ItemStack.isSameItemSameComponents(f, current))) {
									d.favorites.add(current.copy());
									if (minecraft != null) d.saveFavorites(minecraft);
								}
							} else {
								dragHandler.pick(current);
								slot.set().accept(ItemStack.EMPTY);
							}
						}
					} else if (button == 1) {
						slot.set().accept(ItemStack.EMPTY);
					}
					return true;
				}
			}
		}

		if (!showRecipesList() && my >= invY) {
			ItemStack picked = itemAt(mx, my);
			if (!picked.isEmpty()) {
				if (button == 0) {
					if (hasControlDown()) {
						if (d.favorites.stream().noneMatch(f -> ItemStack.isSameItemSameComponents(f, picked))) {
							d.favorites.add(picked.copy());
							if (minecraft != null) d.saveFavorites(minecraft);
						}
					} else if (hasShiftDown()) {
						if (d.favorites.removeIf(f -> ItemStack.isSameItemSameComponents(f, picked))) {
							if (minecraft != null) d.saveFavorites(minecraft);
						}
					} else {
						dragHandler.pick(picked);
					}
					return true;
				} else if (button == 1 && dragHandler.hasStack()) {
					dragHandler.clear();
					return true;
				}
			} else if (button == 1 && dragHandler.hasStack()) {
				dragHandler.clear();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		int mx = (int) mouseX, my = (int) mouseY;
		if (editorSb.mouseDragged(my)) {
			return true;
		}
		if (bottomPanel != null && bottomPanel.mouseDragged(mx, my)) {
			return true;
		}
		if (isDraggingSplitter) {
			invPanelHeight = pH - my;
			updateLayout();
			return true;
		}
		if (dragHandler.hasStack() && my >= editorY && my < invY - 20 && mx >= pX && mx < pX + leftW) {
			int mY = (int) (my + editorSb.scroll);
			List<SlotPos> slots = itemSlots(tabs.get(tabIdx));
			for (int i = 0; i < slots.size(); i++) {
				SlotPos slot = slots.get(i);
				if (r.hit(mx, mY, slot.x(), slot.y(), slot.size(), slot.size())) {
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
		editorSb.mouseReleased();
		if (bottomPanel != null) bottomPanel.mouseReleased();
		dragHandler.resetPaintIndex();
		if (isDraggingSplitter) {
			isDraggingSplitter = false;
			if (minecraft != null) d.saveConfig(minecraft, invPanelHeight);
			return true;
		}
		if (codeViewer != null) codeViewer.mouseReleased();
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double sy) {
		int mx = (int) mouseX, my = (int) mouseY;

		if (my >= editorY && my < invY - 20 && mx >= pX && mx < pX + leftW) {
			int mY = (int) (my + editorSb.scroll);
			if (handleScrollOverSlot(tabs.get(tabIdx), mx, mY, sy)) return true;
			if (StationLayoutEngine.handleScrollSpinners(tabs.get(tabIdx), d, pX + leftW / 2, editorY, mx, mY, sy)) return true;
			editorSb.handleScroll(sy, 12);
			return true;
		}

		if (bottomPanel.mouseScrolled(pX, pY, pW, pH, leftW, invY, mx, my, sy)) return true;
		if (codeViewer != null && codeViewer.mouseScrolled(sy, mx, my)) return true;

		return super.mouseScrolled(mouseX, mouseY, scrollX, sy);
	}

	private boolean handleScrollOverSlot(StationType t, int mx, int mY, double sy) {
		for (SlotPos slot : itemSlots(t)) {
			if (r.hit(mx, mY, slot.x(), slot.y(), slot.size(), slot.size())) {
				ItemStack s = slot.get().get();
				if (!s.isEmpty()) {
					s.setCount(Math.clamp(s.getCount() + (int) sy, 1, 64));
					return true;
				}
			}
		}
		return false;
	}

	private boolean handleEditorClicks(int mx, int mY) {
		StationType t = tabs.get(tabIdx);
		int cx = pX + leftW / 2;
		var layout = StationLayoutEngine.getLayout(t, d);
		int cy = editorY + 15;
		if (layout.getHeaderToggle() != null) {
			layout.getHeaderToggle().setAnchor(cx, cy);
			if (layout.getHeaderToggle().handleClick(mx, mY, font)) return true;
			cy += 25;
		}
		if (layout.getSubToggle() != null) {
			layout.getSubToggle().setAnchor(cx, cy);
			if (layout.getSubToggle().handleClick(mx, mY, font)) return true;
		}
		if (t == StationType.MECH_CRAFTING) {
			int gridCy = editorY + 50, sz = 16, pad = 1, gridW = 9 * (sz + pad), sx = cx - gridW / 2 - 40;
			int ax = sx + gridW + 15, ay = gridCy + (9 * (sz + pad)) / 2 - 4, bx = ax + 20, by = ay + 20, bw = 14, bh = 12;
			if (r.hit(mx, mY, bx, by, bw, bh)) { shiftMechGrid(0, -1); return true; }
			if (r.hit(mx, mY, bx + bw + 2, by, bw, bh)) { shiftMechGrid(0, 1); return true; }
			if (r.hit(mx, mY, bx, by + bh + 2, bw, bh)) { shiftMechGrid(-1, 0); return true; }
			if (r.hit(mx, mY, bx + bw + 2, by + bh + 2, bw, bh)) { shiftMechGrid(1, 0); return true; }
		}
		return StationLayoutEngine.handleSpinnerClicks(tabs.get(tabIdx), d, font, cx, editorY, mx, mY)
				|| StationLayoutEngine.handleFluidSpins(tabs.get(tabIdx), d, cx, editorY, mx, mY);
	}



	private boolean handleDoubleClick(int mx, int mY) {
		StationType t = tabs.get(tabIdx);
		int cx = pX + leftW / 2;
		return StationLayoutEngine.handleDoubleClick(t, d, font, cx, editorY, mx, mY, (field, bx, by, bw, val, idx) -> startActiveNumEdit(field, bx, by, bw, val, idx));
	}

	private void startActiveNumEdit(String field, int bx, int by, int bw, String value, int idx) {
		activeFieldName = field;
		activeFieldIdx = idx;
		activeNumEditBox = new EditBox(font, bx, by - (int) editorSb.scroll, bw, 12, Component.empty());
		activeNumEditBox.setValue(value);
		activeNumEditBox.setFocused(true);
		activeNumEditBox.setMaxLength(8);
	}

	private void applyActiveNumEdit() {
		if (activeNumEditBox == null || activeFieldName == null) return;
		String v = activeNumEditBox.getValue().trim();
		try {
			int num = v.isEmpty() ? 0 : Integer.parseInt(v);
			StationType cur = tabs.get(tabIdx);
			if (activeFieldName.endsWith("_out_count")) {
				applyOutCount(StationLayoutEngine.getCrushingOutputsForGroup(d, cur), num);
			} else if (activeFieldName.endsWith("_out_chance")) {
				applyOutChance(StationLayoutEngine.getCrushingOutputsForGroup(d, cur), num);
			} else if (activeFieldName.endsWith("Time")) {
				var pt = StationLayoutEngine.getLayout(cur, d).getProcessingTime();
				if (pt != null) pt.setValue(num);
			} else if (activeFieldName.endsWith("Count")) {
				StationLayoutEngine.setResultCount(d, cur, Math.clamp(num, 1, 64));
			} else switch (activeFieldName) {
				case "furnXp" -> d.furnXp = Float.parseFloat(v);
				case "fluid_mix_in" -> { if (activeFieldIdx >= 0) d.mixFluidIng.get(activeFieldIdx).amount = Math.clamp(num, 1, 1000); }
				case "fluid_mix_out" -> { if (activeFieldIdx >= 0) d.mixFluidOuts.get(activeFieldIdx).amount = Math.clamp(num, 1, 1000); }
				case "fluid_fill_in" -> d.fillFluid.amount = Math.clamp(num, 1, 1000);
				case "grid_count" -> {
					if (activeFieldIdx >= 0) {
						List<ItemStack> gl = cur == StationType.MIXING ? d.mixIng : cur == StationType.MECH_CRAFTING ? d.mechGrid : d.craftGrid;
						if (activeFieldIdx < gl.size() && !gl.get(activeFieldIdx).isEmpty()) gl.get(activeFieldIdx).setCount(Math.clamp(num, 1, 64));
					}
				}
			}
		} catch (Exception ignored) {}
		activeNumEditBox = null;
		activeFieldName = null;
		activeFieldIdx = -1;
	}

	private void applyOutCount(List<StationType.CrushingOutput> list, int v) {
		if (activeFieldIdx >= 0 && activeFieldIdx < list.size()) list.get(activeFieldIdx).count = Math.clamp(v, 1, 64);
	}

	private void applyOutChance(List<StationType.CrushingOutput> list, int pct) {
		if (activeFieldIdx >= 0 && activeFieldIdx < list.size()) list.get(activeFieldIdx).chance = Math.clamp(pct, 1, 100) / 100F;
	}

	private void shiftMechGrid(int dx, int dy) {
		cz.maxtechnik.opm.client.screen.layout.GridShiftHelper.shiftGrid(d.mechGrid, 9, 9, dx, dy);
	}

	private void addInputSlots(List<SlotPos> out, cz.maxtechnik.opm.client.screen.layout.SlotGroup inG, List<ItemStack> inItems, StationType t) {
		for (int i = 0; i < inG.getTotalSlots(); i++) {
			int idx = i;
			out.add(new SlotPos(inG.getSlotX(i), inG.getSlotY(i), inG.getSlotSize(),
					() -> idx < inItems.size() ? inItems.get(idx) : ItemStack.EMPTY,
					s -> StationLayoutEngine.setInputItem(d, t, idx, s)));
		}
	}

	private List<SlotPos> itemSlots(StationType t) {
		List<SlotPos> out = new ArrayList<>();
		int cx = pX + leftW / 2;
		var layout = StationLayoutEngine.getLayout(t, d);
		int cy = editorY + 15 + (layout.getHeaderToggle() != null ? 25 : 0) + (layout.getSubToggle() != null ? 30 : 0);
		var inG = layout.getInputSlots();
		var outG = layout.getOutputSlots();
		if (inG != null) {
			List<ItemStack> inItems = StationLayoutEngine.getItemListForGroup(d, t, true);
			if (outG != null) {
				int sx = t == StationType.MECH_CRAFTING ? cx - inG.getWidth() / 2 - 40 : cx - 120;
				inG.setAnchor(sx, cy);
				int arrowX = sx + inG.getWidth() + 10;
				int arrowY = cy + inG.getHeight() / 2 - 4;
				int rx = arrowX + 20;
				outG.setAnchor(rx, t == StationType.MECH_CRAFTING ? arrowY - 4 : cy);
				addInputSlots(out, inG, inItems, t);

				List<StationType.CrushingOutput> crushOuts = StationLayoutEngine.getCrushingOutputsForGroup(d, t);
				if (crushOuts != null) {
					for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
						int idx = i;
						out.add(new SlotPos(outG.getSlotX(i), outG.getSlotY(i), UiKit.SS, () -> crushOuts.get(idx).stack, s -> crushOuts.get(idx).stack = s));
					}
				} else {
					out.add(new SlotPos(rx, outG.getAnchorY(), UiKit.SS, () -> StationLayoutEngine.getResultItem(d, t), s -> StationLayoutEngine.setOutputItem(d, t, s)));
				}
			} else {
				int sx = cx - inG.getWidth() / 2;
				inG.setAnchor(sx, cy);
				addInputSlots(out, inG, inItems, t);
			}
		}
		return out;
	}


	private ItemStack slotAt(int mx, int my) {
		if (my >= editorY && my < invY - 20 && mx >= pX && mx < pX + leftW) {
			int mY = (int) (my + editorSb.scroll);
			for (SlotPos s : itemSlots(tabs.get(tabIdx))) {
				if (r.hit(mx, mY, s.x(), s.y(), s.size(), s.size())) return s.get().get();
			}
		}
		return itemAt(mx, my);
	}

	private ItemStack itemAt(int mx, int my) {
		return bottomPanel.itemAt(pX, pY, pW, pH, leftW, invY, mx, my);
	}

	private boolean showRecipesList() {
		return bottomPanel != null && bottomPanel.isShowingRecipesList();
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (activeNumEditBox != null && activeNumEditBox.isFocused()) {
			if (key == 257 || key == 335) {
				applyActiveNumEdit();
				return true;
			}
			if (key == 256) {
				activeNumEditBox = null;
				activeFieldName = null;
				activeFieldIdx = -1;
				return true;
			}
			activeNumEditBox.keyPressed(key, scan, mods);
			return true;
		}
		if (key == 256) {
			if (fnFocused) {
				fnFocused = false;
				return true;
			}
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
		if (showRecipesList() && key == 261 && !fnFocused && (bottomPanel.getRecipeSearchBox() == null || !bottomPanel.getRecipeSearchBox().isFocused())) {
			deleteRecipe();
			return true;
		}
		if (fnFocused) {
			if (key == 259 && !fileName.isEmpty() && fnCursor > 0) {
				fileName = fileName.substring(0, fnCursor - 1) + fileName.substring(fnCursor);
				fnCursor--;
			} else if (key == 261 && fnCursor < fileName.length()) {
				fileName = fileName.substring(0, fnCursor) + fileName.substring(fnCursor + 1);
			} else if (key == 263) fnCursor = Math.max(0, fnCursor - 1);
			else if (key == 262) fnCursor = Math.min(fileName.length(), fnCursor + 1);
			return true;
		}

		if (activeNumEditBox == null && !fnFocused && (bottomPanel == null || !bottomPanel.isSearchFocused())) {
			if (tabs.get(tabIdx) == StationType.MECH_CRAFTING) {
				if (key == 87 || key == 265) { shiftMechGrid(0, -1); return true; }
				if (key == 83 || key == 264) { shiftMechGrid(0, 1); return true; }
				if (key == 65 || key == 263) { shiftMechGrid(-1, 0); return true; }
				if (key == 68 || key == 262) { shiftMechGrid(1, 0); return true; }
			}
		}

		if (codeViewer != null && codeViewer.keyPressed(key, mods)) return true;
		return super.keyPressed(key, scan, mods);
	}

	@Override
	public boolean charTyped(char chr, int mods) {
		if (activeNumEditBox != null && activeNumEditBox.isFocused()) {
			if (Character.isDigit(chr) || (activeFieldName != null && activeFieldName.equals("furnXp") && chr == '.')) {
				return activeNumEditBox.charTyped(chr, mods);
			}
			return true;
		}
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
		if (fnFocused) {
			if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '/') {
				fileName = fileName.substring(0, fnCursor) + chr + fileName.substring(fnCursor);
				fnCursor++;
			}
			return true;
		}
		if (codeViewer != null && codeViewer.charTyped(chr)) return true;
		return super.charTyped(chr, mods);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	private void save() {
		String j = d.buildJson(tabs, tabIdx);
		SaveResult res = RecipeFileManager.saveRecipe(fileName, j);
		if (res.success()) {
			d.scanSavedRecipes();
			d.selectedRecipeFile = res.savedFile();
			d.status("Saved!", true);
		} else {
			d.status("Save failed!", false);
		}
	}

	private void deleteRecipe() {
		if (RecipeFileManager.deleteRecipes(d.selectedRecipeFiles)) {
			if (d.selectedRecipeFile != null && d.selectedRecipeFiles.contains(d.selectedRecipeFile)) {
				d.selectedRecipeFile = null;
				fileName = "";
				fnCursor = 0;
				d.clear();
			}
			d.selectedRecipeFiles.clear();
			d.scanSavedRecipes();
		}
	}

	private record SlotPos(int x, int y, int size, Supplier<ItemStack> get, Consumer<ItemStack> set) {}
}