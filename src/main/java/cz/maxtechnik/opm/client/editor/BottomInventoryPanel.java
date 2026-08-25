package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.recipe.RecipeFileManager;
import cz.maxtechnik.opm.client.ui.Scrollbar;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class BottomInventoryPanel {
	public enum BottomTab {
		INVENTORY, FLUIDS, ITEMS, TAGS, FAVORITES, RECIPES
	}

	private static final String[] TABS = {"Inv", "Fluid", "Item", "Tag", "Fav", "Rece"};
	private static final int GRID_W = 9 * (UiKit.SS + UiKit.SP) - UiKit.SP; // 178px

	private final Font font;
	private final RecipeEditorData data;

	private BottomTab bottomTab = BottomTab.INVENTORY;
	private EditBox searchBox;
	private final Scrollbar bottomSb = new Scrollbar();

	private String lastSearch = null;
	private BottomTab lastBottomTab = null;

	private long lastRecipeClickTime = 0;
	private File lastRecipeClickedFile = null;

	public interface RecipeSelectionListener {
		void onRecipeSelected(File file);
		void onRecipeUnloaded();
		void onRecipeDeleted();
	}

	public BottomInventoryPanel(Font font, RecipeEditorData data) {
		this.font = font;
		this.data = data;
	}

	public void init(int x, int y) {
		searchBox = new EditBox(font, x + 8, y + 22, GRID_W, 14, Component.literal("Search..."));
		searchBox.setHint(Component.literal("Search..."));
		searchBox.setResponder(s -> bottomSb.scroll = 0);
	}

	public void updateLayout(int x, int y) {
		if (searchBox != null) {
			searchBox.setX(x + 8);
			searchBox.setY(y + 22);
			searchBox.setWidth(bottomTab == BottomTab.RECIPES ? GRID_W - 32 : GRID_W);
		}
	}

	public boolean isShowingRecipesList() { return bottomTab == BottomTab.RECIPES; }
	public BottomTab getBottomTab() { return bottomTab; }
	public void setBottomTab(BottomTab tab) {
		this.bottomTab = tab;
		this.bottomSb.scroll = 0;
	}

	public EditBox getSearchBox() { return searchBox; }

	public void unfocusSearch() {
		if (searchBox != null) searchBox.setFocused(false);
	}

	public boolean isSearchFocused() {
		return bottomTab != BottomTab.INVENTORY && searchBox != null && searchBox.isFocused();
	}

	private static final int TAB_GAP = 2;
	private static final int TAB_W = (GRID_W - 5 * TAB_GAP) / 6; // (178 - 10) / 6 = 28px

	private int getTabW(int i) {
		return TAB_W;
	}

	private int getTabX(int startX, int i) {
		return startX + i * (TAB_W + TAB_GAP);
	}

	public void render(GuiGraphics g, int pX, int pY, int pH, int leftW, int invY, int mx, int my) {
		// Panel pozadí
		g.fill(pX, invY, pX + leftW, pY + pH, UiKit.C_INV);
		g.fill(pX, invY, pX + leftW, invY + 1, UiKit.C_BORDER);

		// Splitter rukojeť
		g.fill(pX + leftW / 2 - 18, invY, pX + leftW / 2 + 18, invY + 2, 0xFF555555);

		int startX = pX + 8;

		// 6 Záložek spodního panelu
		for (int i = 0; i < TABS.length; i++) {
			int tx = getTabX(startX, i);
			int tw = getTabW(i);
			boolean sel = bottomTab.ordinal() == i;
			boolean hov = UiKit.hit(mx, my, tx, invY + 4, tw, 14);
			if (sel) {
				g.fill(tx, invY + 4, tx + tw, invY + 18, UiKit.C_TAB_SEL);
				g.fill(tx, invY + 17, tx + tw, invY + 18, UiKit.C_ACCENT);
			} else if (hov) {
				g.fill(tx, invY + 4, tx + tw, invY + 18, UiKit.C_BTN_H);
			}
			g.drawCenteredString(font, TABS[i], tx + tw / 2, invY + 7, sel || hov ? 0xFFFFFFFF : UiKit.C_LABEL);
		}

		// Inspector-style Search Bar & Akce
		int searchY = invY + 22;
		if (bottomTab != BottomTab.INVENTORY && searchBox != null) {
			int sw = (bottomTab == BottomTab.RECIPES) ? (GRID_W - 32) : GRID_W;
			searchBox.setWidth(sw);
			renderCustomSearch(g, searchBox, startX, searchY, sw);

			if (bottomTab == BottomTab.RECIPES) {
				int relX = startX + GRID_W - 30;
				int delX = startX + GRID_W - 14;
				boolean hRel = UiKit.hit(mx, my, relX, searchY, 14, 14);
				boolean hDel = UiKit.hit(mx, my, delX, searchY, 14, 14);

				if (hRel) g.fill(relX, searchY, relX + 14, searchY + 14, UiKit.C_BTN_H);
				g.drawCenteredString(font, "⟳", relX + 7, searchY + 3, hRel ? 0xFFFFFFFF : UiKit.C_LABEL);

				if (hDel) g.fill(delX, searchY, delX + 14, searchY + 14, 0xFF4A1A1A);
				g.drawCenteredString(font, "✕", delX + 7, searchY + 3, hDel ? 0xFFFF6666 : UiKit.C_LABEL);
			}
		}

		int listY = (bottomTab == BottomTab.INVENTORY) ? (invY + 22) : (invY + 38);
		int listH = (pY + pH) - listY - 4;
		if (listH <= 4) return;

		if (bottomTab != BottomTab.RECIPES) {
			g.enableScissor(startX, listY, startX + GRID_W + 1, listY + listH);
			var pose = g.pose();
			pose.pushPose();
			pose.translate(0, -bottomSb.scroll, 0);
			int mY2 = (int) (my + bottomSb.scroll);
			int contentH = renderBottomContent(g, pH, mx, mY2, startX, listY);
			pose.popPose();
			g.disableScissor();

			bottomSb.update(listH, contentH);
			bottomSb.render(g, startX + GRID_W + 3, listY);
		} else {
			renderRecipeList(g, mx, my, startX, listY, listH);
		}
	}

	private void renderCustomSearch(GuiGraphics g, EditBox box, int sx, int sy, int sw) {
		boolean focused = box.isFocused();
		g.fill(sx, sy, sx + sw, sy + 14, UiKit.C_BG);
		if (focused) UiKit.drawOutline(g, sx, sy, sw, 14, UiKit.C_ACCENT);
		else UiKit.drawOutline(g, sx, sy, sw, 14, UiKit.C_BORDER);

		String val = box.getValue();
		if (val.isEmpty() && !focused) {
			g.drawString(font, "Search...", sx + 4, sy + 3, UiKit.C_MUTED, false);
		} else {
			g.enableScissor(sx + 2, sy, sx + sw - 2, sy + 14);
			g.drawString(font, val, sx + 4, sy + 3, UiKit.C_TEXT, false);
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
				int cx = sx + 4 + font.width(val.substring(0, Math.min(box.getCursorPosition(), val.length())));
				g.fill(cx, sy + 2, cx + 1, sy + 12, UiKit.C_ACCENT);
			}
			g.disableScissor();
		}
	}

	private int renderBottomContent(GuiGraphics g, int pH, int mx, int mY, int startX, int listY) {
		Minecraft mc = Minecraft.getInstance();
		if (bottomTab == BottomTab.INVENTORY && mc.player != null) {
			Inventory inv = mc.player.getInventory();
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 9; col++) {
					renderInvSlot(g, inv.getItem(9 + row * 9 + col), startX + col * (UiKit.SS + UiKit.SP), listY + row * (UiKit.SS + UiKit.SP), mx, mY);
				}
			}
			for (int col = 0; col < 9; col++) {
				renderInvSlot(g, inv.getItem(col), startX + col * (UiKit.SS + UiKit.SP), listY + 3 * (UiKit.SS + UiKit.SP) + 6, mx, mY);
			}
			return 4 * (UiKit.SS + UiKit.SP) + 6;
		}

		List<ItemStack> list = filteredList();
		int totalRows = (list.size() + 8) / 9;
		int rowH = UiKit.SS + UiKit.SP;
		int listH = pH - listY - 4;
		int startRow = Math.max(0, (int) (bottomSb.scroll / rowH));
		int endRow = Math.min(totalRows, (int) ((bottomSb.scroll + listH) / rowH) + 2);
		int firstIdx = startRow * 9;
		int lastIdx = Math.min(list.size(), endRow * 9);

		for (int i = firstIdx; i < lastIdx; i++) {
			renderInvSlot(g, list.get(i), startX + (i % 9) * rowH, listY + (i / 9) * rowH, mx, mY);
		}
		return totalRows * rowH;
	}

	public void invalidateFilter() {
		lastSearch = null;
		lastBottomTab = null;
		data.cachedFilteredItems.clear();
	}

	private List<ItemStack> filteredList() {
		if (bottomTab == BottomTab.FAVORITES) {
			String q = (searchBox != null) ? searchBox.getValue() : "";
			if (q.isBlank()) return data.favorites;
			return data.favorites.stream().filter(s -> cz.maxtechnik.opm.client.editor.SearchEngine.matches(s, q)).toList();
		}

		String q = (searchBox != null) ? searchBox.getValue() : "";
		if (!q.equals(lastSearch) || bottomTab != lastBottomTab) {
			lastSearch = q;
			lastBottomTab = bottomTab;
			data.cachedFilteredItems.clear();

			List<ItemStack> source = switch (bottomTab) {
				case FLUIDS    -> data.availableFluids;
				case TAGS      -> data.cachedTags;
				case ITEMS     -> data.allItems;
				default        -> List.of();
			};

			if (q.isBlank()) {
				data.cachedFilteredItems.addAll(source);
			} else {
				data.cachedFilteredItems.addAll(source.stream().filter(s -> cz.maxtechnik.opm.client.editor.SearchEngine.matches(s, q)).toList());
			}
		}
		return data.cachedFilteredItems;
	}

	private String getRelativeName(File f) {
		try {
			Path rel = RecipeFileManager.getRecipeDir().relativize(f.toPath());
			return stripJson(rel.toString().replace('\\', '/'));
		} catch (Exception e) {
			return stripJson(f.getName());
		}
	}

	private void renderRecipeList(GuiGraphics g, int mx, int my, int startX, int listY, int listH) {
		List<File> files = filteredSavedRecipes();
		int maxNameW = files.stream().mapToInt(f -> {
			String name = getRelativeName(f);
			boolean isActive = data.selectedRecipeFile != null && data.selectedRecipeFile.getAbsolutePath().equals(f.getAbsolutePath());
			return font.width(isActive ? "▶ " + name : name);
		}).max().orElse(0);
		int rowW = Math.max(GRID_W, maxNameW + 10);

		g.enableScissor(startX, listY, startX + GRID_W, listY + listH);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -bottomSb.scroll, 0);
		int startIdx = Math.max(0, (int) (bottomSb.scroll / 14));
		int endIdx = Math.min(files.size(), (int) ((bottomSb.scroll + listH) / 14) + 2);

		for (int i = startIdx; i < endIdx; i++) {
			File f = files.get(i);
			String name = getRelativeName(f);
			int ry = listY + i * 14;
			boolean isSel = data.selectedRecipeFiles.contains(f);
			boolean isHov = UiKit.hit(mx, (int) (my + bottomSb.scroll), startX, ry, GRID_W, 14);
			boolean isActive = data.selectedRecipeFile != null && data.selectedRecipeFile.getAbsolutePath().equals(f.getAbsolutePath());
			String displayName = isActive ? "▶ " + name : name;

			if (isSel) g.fill(startX, ry, startX + rowW, ry + 14, UiKit.C_TAB_SEL);
			else if (isHov) g.fill(startX, ry, startX + rowW, ry + 14, UiKit.C_BTN_H);
			int color = isSel || isHov ? 0xFFFFFFFF : (isActive ? 0xFF55FF55 : UiKit.C_LABEL);
			g.drawString(font, displayName, startX + 4, ry + 3, color, false);
		}
		pose.popPose();
		g.disableScissor();

		bottomSb.update(listH, files.size() * 14);
		bottomSb.render(g, startX + GRID_W + 3, listY);
	}

	private List<File> filteredSavedRecipes() {
		if (searchBox == null) return data.savedRecipeFiles;
		String q = searchBox.getValue();
		if (q.isBlank()) return data.savedRecipeFiles;
		Path base = RecipeFileManager.getRecipeDir();
		return data.savedRecipeFiles.stream().filter(f -> cz.maxtechnik.opm.client.editor.SearchEngine.matchesFile(f, base, q)).toList();
	}

	private static String stripJson(String s) {
		return s.endsWith(".json") ? s.substring(0, s.length() - 5) : s;
	}

	private void renderInvSlot(GuiGraphics g, ItemStack stack, int sx, int sy, int mx, int my) {
		boolean hov = UiKit.hit(mx, my, sx, sy, UiKit.SS, UiKit.SS);
		g.fill(sx - 1, sy - 1, sx + UiKit.SS + 1, sy + UiKit.SS + 1, UiKit.C_BORDER);
		g.fill(sx, sy, sx + UiKit.SS, sy + UiKit.SS, hov ? UiKit.C_SLOT_HOV : UiKit.C_SLOT);
		if (stack != null && !stack.isEmpty()) {
			ItemStack rs = stack.copy();
			rs.setCount(1);
			g.renderItem(rs, sx + 1, sy + 1);
			g.renderItemDecorations(font, rs, sx + 1, sy + 1);
		}
	}

	public boolean mouseClicked(int pX, int pY, int pH, int leftW, int invY, int mx, int my, int button, RecipeSelectionListener listener) {
		if (my < invY) return false;
		int startX = pX + 8;

		// Klik na záložky
		for (int i = 0; i < TABS.length; i++) {
			int tx = getTabX(startX, i);
			int tw = getTabW(i);
			if (UiKit.hit(mx, my, tx, invY + 4, tw, 14)) {
				bottomTab = BottomTab.values()[i];
				bottomSb.scroll = 0;
				if (searchBox != null) searchBox.setValue("");
				return true;
			}
		}

		int searchY = invY + 22;

		if (bottomTab != BottomTab.INVENTORY && searchBox != null) {
			int sw = (bottomTab == BottomTab.RECIPES) ? (GRID_W - 32) : GRID_W;
			if (UiKit.hit(mx, my, startX, searchY, sw, 14)) {
				searchBox.setFocused(true);
				if (button == 1) searchBox.setValue("");
				else searchBox.mouseClicked(mx, my, button);
				return true;
			}

			if (bottomTab == BottomTab.RECIPES) {
				int relX = startX + GRID_W - 30;
				int delX = startX + GRID_W - 14;
				if (UiKit.hit(mx, my, relX, searchY, 14, 14)) {
					data.scanSavedRecipes();
					data.status("Reloaded recipes", true);
					return true;
				}
				if (UiKit.hit(mx, my, delX, searchY, 14, 14)) {
					if (listener != null) listener.onRecipeDeleted();
					return true;
				}
			}
		}

		if (bottomTab == BottomTab.RECIPES) {
			int listY = invY + 38;
			int listH = (pY + pH) - listY - 4;
			if (UiKit.hit(mx, my, startX, listY, GRID_W, listH)) {
				List<File> files = filteredSavedRecipes();
				int mY = (int) (my + bottomSb.scroll);
				int idx = (mY - listY) / 14;
				if (idx >= 0 && idx < files.size()) {
					File f = files.get(idx);
					long now = System.currentTimeMillis();
					if (now - lastRecipeClickTime < 350 && lastRecipeClickedFile != null && lastRecipeClickedFile.equals(f)) {
						// Dvojklik -> Unload recipe
						if (listener != null) listener.onRecipeUnloaded();
						lastRecipeClickTime = 0;
						lastRecipeClickedFile = null;
						return true;
					}
					lastRecipeClickTime = now;
					lastRecipeClickedFile = f;

					if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
						if (data.selectedRecipeFiles.contains(f)) data.selectedRecipeFiles.remove(f);
						else data.selectedRecipeFiles.add(f);
					} else {
						data.selectedRecipeFiles.clear();
						data.selectedRecipeFiles.add(f);
					}
					if (listener != null) listener.onRecipeSelected(f);
					return true;
				}
			}
		}

		return bottomSb.mouseClicked(mx, my, button);
	}

	public boolean mouseDragged(int my) {
		return bottomSb.mouseDragged(my);
	}

	public void mouseReleased() {
		bottomSb.mouseReleased();
	}

	public boolean mouseScrolled(int pX, int pH, int leftW, int invY, int mx, int my, double sy) {
		if (my < invY) return false;
		int startX = pX + 8;
		int listY = (bottomTab == BottomTab.INVENTORY) ? (invY + 22) : (invY + 38);

		if (UiKit.hit(mx, my, startX, listY, GRID_W + 10, pH - listY - 4)) {
			bottomSb.handleScroll(sy, 12);
			return true;
		}
		return false;
	}

	public ItemStack itemAt(int pX, int pH, int leftW, int invY, int mx, int my) {
		if (my < invY || bottomTab == BottomTab.RECIPES) return ItemStack.EMPTY;
		int startX = pX + 8;
		int listY = (bottomTab == BottomTab.INVENTORY) ? (invY + 22) : (invY + 38);
		int mY = (int) (my + bottomSb.scroll);

		if (bottomTab == BottomTab.INVENTORY && Minecraft.getInstance().player != null) {
			Inventory inv = Minecraft.getInstance().player.getInventory();
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 9; col++) {
					if (UiKit.hit(mx, mY, startX + col * (UiKit.SS + UiKit.SP), listY + row * (UiKit.SS + UiKit.SP), UiKit.SS, UiKit.SS)) {
						return inv.getItem(9 + row * 9 + col);
					}
				}
			}
			for (int col = 0; col < 9; col++) {
				if (UiKit.hit(mx, mY, startX + col * (UiKit.SS + UiKit.SP), listY + 3 * (UiKit.SS + UiKit.SP) + 6, UiKit.SS, UiKit.SS)) {
					return inv.getItem(col);
				}
			}
		} else {
			List<ItemStack> list = filteredList();
			int rowH = UiKit.SS + UiKit.SP;
			int totalRows = (list.size() + 8) / 9;
			int startRow = Math.max(0, (int) (bottomSb.scroll / rowH));
			int endRow = Math.min(totalRows, (int) ((bottomSb.scroll + (pH - listY - 4)) / rowH) + 2);
			int firstIdx = startRow * 9;
			int lastIdx = Math.min(list.size(), endRow * 9);

			for (int i = firstIdx; i < lastIdx; i++) {
				if (UiKit.hit(mx, mY, startX + (i % 9) * rowH, listY + (i / 9) * rowH, UiKit.SS, UiKit.SS)) {
					return list.get(i);
				}
			}
		}

		return ItemStack.EMPTY;
	}

	public ItemStack itemAtFavorite(int pX, int pH, int leftW, int invY, int mx, int my) {
		if (bottomTab == BottomTab.FAVORITES) {
			return itemAt(pX, pH, leftW, invY, mx, my);
		}
		return ItemStack.EMPTY;
	}

	public boolean isInsideFavoritesArea(int pX, int pH, int leftW, int invY, int mx, int my) {
		if (my < invY || bottomTab == BottomTab.RECIPES) return false;
		int startX = pX + 8;
		int favTabX = getTabX(startX, 4);
		int favTabW = getTabW(4);

		if (UiKit.hit(mx, my, favTabX, invY + 4, favTabW, 14)) return true;
		return bottomTab == BottomTab.FAVORITES && UiKit.hit(mx, my, startX, invY, GRID_W, pH - invY);
	}
}