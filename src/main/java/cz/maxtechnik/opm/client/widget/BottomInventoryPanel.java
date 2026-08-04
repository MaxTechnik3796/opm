package cz.maxtechnik.opm.client.widget;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.RecipeFileWriter;
import cz.maxtechnik.opm.client.util.Scrollbar;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
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
		INVENTORY, FLUIDS, ITEMS, TAGS
	}

	private final Font font;
	private final RecipeEditorData data;

	private BottomTab bottomTab = BottomTab.INVENTORY;
	private boolean showRecipesList = false;

	private EditBox searchBox;
	private EditBox recipeSearchBox;

	private final Scrollbar bottomSb = new Scrollbar();
	private final Scrollbar favSb = new Scrollbar();
	private final Scrollbar recipeSb = new Scrollbar();

	private String lastSearch = null;
	private BottomTab lastBottomTab = null;

	public interface RecipeSelectionListener {
		void onRecipeSelected(File file);
		void onRecipeDeleted();
	}

	public BottomInventoryPanel(Font font, RecipeEditorData data) {
		this.font = font;
		this.data = data;
	}

	public void init(int x, int y) {
		searchBox = new EditBox(font, x + 10, y + 22, 176, 12, Component.literal("Search..."));
		searchBox.setHint(Component.literal("Search..."));
		searchBox.setResponder(s -> bottomSb.scroll = 0);
		recipeSearchBox = new EditBox(font, x + 10, y + 22, 176, 12, Component.literal("Search..."));
		recipeSearchBox.setHint(Component.literal("Search..."));
		recipeSearchBox.setResponder(s -> recipeSb.scroll = 0);
	}

	public void updateLayout(int x, int y) {
		if (searchBox != null) {
			searchBox.setX(x + 10);
			searchBox.setY(y + 22);
		}
		if (recipeSearchBox != null) {
			recipeSearchBox.setX(x + 10);
			recipeSearchBox.setY(y + 22);
		}
	}

	public BottomTab getBottomTab() { return bottomTab; }
	public boolean isShowingRecipesList() { return showRecipesList; }
	public EditBox getSearchBox() { return searchBox; }
	public EditBox getRecipeSearchBox() { return recipeSearchBox; }

	public void unfocusSearch() {
		if (searchBox != null) searchBox.setFocused(false);
		if (recipeSearchBox != null) recipeSearchBox.setFocused(false);
	}

	public boolean isSearchFocused() {
		return (!showRecipesList && bottomTab != BottomTab.INVENTORY && searchBox != null && searchBox.isFocused()) ||
		       (showRecipesList && recipeSearchBox != null && recipeSearchBox.isFocused());
	}

	private int calcTabsEnd(int startX) {
		int end = startX;
		for (String s : new String[]{"Inventory", "Fluids", "Items", "Tags"}) end += font.width(s) + 14;
		return end;
	}

	public int getFavCols(int pX, int leftW) {
		int startX = pX + 10;
		int itemGridW = 9 * (UiKit.SS + UiKit.SP);
		int availFavW = (pX + leftW - 6) - (startX + itemGridW + 12);
		return Math.clamp(availFavW / (UiKit.SS + UiKit.SP), 3, 5);
	}

	public void render(GuiGraphics g, int pX, int pY, int pH, int leftW, int invY, int mx, int my) {
		g.fill(pX, invY, pX + leftW, pY + pH, UiKit.C_INV);
		g.fill(pX, invY, pX + leftW, invY + 2, UiKit.C_BORDER);
		g.fill(pX + leftW / 2 - 20, invY, pX + leftW / 2 + 20, invY + 3, 0xFF666666);

		int startX = pX + 10;
		int favCols = getFavCols(pX, leftW);
		int favX = startX + 9 * (UiKit.SS + UiKit.SP) + 12;

		String[] bTabs = {"Inventory", "Fluids", "Items", "Tags"};
		int recBtnX = Math.min(calcTabsEnd(startX), pX + leftW - (font.width(showRecipesList ? "◀ Items" : "Recipes ▶") + 14));
		int recBtnW = font.width(showRecipesList ? "◀ Items" : "Recipes ▶") + 10;

		if (!showRecipesList) {
			int tx = startX;
			for (int i = 0; i < bTabs.length; i++) {
				int tw = font.width(bTabs[i]) + 10;
				boolean sel = bottomTab.ordinal() == i;
				boolean hov = UiKit.hit(mx, my, tx, invY + 4, tw, 14);
				g.fill(tx, invY + 4, tx + tw, invY + 18, sel ? UiKit.C_TAB_SEL : (hov ? UiKit.C_BTN_H : UiKit.C_BTN));
				g.drawCenteredString(font, bTabs[i], tx + tw / 2, invY + 7, sel ? 0xFFCCCCFF : UiKit.C_TEXT);
				tx += tw + 4;
			}
		}

		if (!showRecipesList && bottomTab != BottomTab.INVENTORY && searchBox != null) {
			searchBox.render(g, mx, my, 0);
		}
		if (showRecipesList && recipeSearchBox != null) {
			recipeSearchBox.render(g, mx, my, 0);
		}

		boolean hRec = UiKit.hit(mx, my, recBtnX, invY + 4, recBtnW, 14);
		g.fill(recBtnX, invY + 4, recBtnX + recBtnW, invY + 18, showRecipesList ? UiKit.C_TAB_SEL : (hRec ? UiKit.C_BTN_H : UiKit.C_BTN));
		g.drawCenteredString(font, showRecipesList ? "◀ Items" : "Recipes ▶", recBtnX + recBtnW / 2, invY + 7, showRecipesList ? 0xFFCCCCFF : UiKit.C_TEXT);

		int listY = getBottomListY(invY);
		int listH = (pY + pH) - listY - 5;

		if (!showRecipesList) {
			g.enableScissor(startX, listY, startX + 9 * (UiKit.SS + UiKit.SP), listY + listH);
			var pose = g.pose();
			pose.pushPose();
			pose.translate(0, -bottomSb.scroll, 0);
			int mY2 = (int) (my + bottomSb.scroll);
			int contentH = renderBottomContent(g, pH, mx, mY2, startX, listY);
			pose.popPose();
			g.disableScissor();

			bottomSb.update(listH, contentH);
			bottomSb.render(g, startX + 9 * (UiKit.SS + UiKit.SP) + 2, listY);

			int favListY = listY + 12;
			int favListH = (pY + pH) - favListY - 5;
			g.drawString(font, "Favorite", favX, favListY - 11, 0xFFFFFFFF, false);
			renderFavorites(g, mx, my, favX, favCols, favListY, favListH, pX, leftW);

		} else {
			boolean hDel = UiKit.hit(mx, my, startX, invY + 4, 50, 14);
			boolean hUnl = UiKit.hit(mx, my, startX + 54, invY + 4, 50, 14);
			boolean hRel = UiKit.hit(mx, my, startX + 108, invY + 4, 50, 14);
			drawActionBtn(g, font, "Delete", startX, invY + 4, hDel, 0xFF4A1A1A, 0xFF6A2222);
			drawActionBtn(g, font, "Unload", startX + 54, invY + 4, hUnl, UiKit.C_BTN, UiKit.C_BTN_H);
		}
	}

	private int getBottomListY(int invY) {
		if (showRecipesList) return invY + 38;
		return bottomTab != BottomTab.INVENTORY ? invY + 38 : invY + 22;
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
				renderInvSlot(g, inv.getItem(col), startX + col * (UiKit.SS + UiKit.SP), listY + 3 * (UiKit.SS + UiKit.SP) + 8, mx, mY);
			}
			return 4 * (UiKit.SS + UiKit.SP) + 8;
		}

		List<ItemStack> list = filteredList();
		int totalRows = (list.size() + 8) / 9;
		int rowH = UiKit.SS + UiKit.SP;
		int listH = pH - listY - 5;
		int startRow = Math.max(0, (int) (bottomSb.scroll / rowH));
		int endRow = Math.min(totalRows, (int) ((bottomSb.scroll + listH) / rowH) + 2);
		int firstIdx = startRow * 9;
		int lastIdx = Math.min(list.size(), endRow * 9);

		for (int i = firstIdx; i < lastIdx; i++) {
			renderInvSlot(g, list.get(i), startX + (i % 9) * rowH, listY + (i / 9) * rowH, mx, mY);
		}
		return totalRows * rowH;
	}

	private List<ItemStack> filteredList() {
		String q = (searchBox != null) ? searchBox.getValue() : "";
		if (!q.equals(lastSearch) || bottomTab != lastBottomTab) {
			lastSearch = q;
			lastBottomTab = bottomTab;
			data.cachedFilteredItems.clear();

			List<ItemStack> source = switch (bottomTab) {
				case FLUIDS -> data.availableFluids;
				case TAGS   -> data.cachedTags;
				case ITEMS  -> data.allItems;
				default     -> List.of();
			};

			if (q.isBlank()) {
				data.cachedFilteredItems.addAll(source);
			} else {
				data.cachedFilteredItems.addAll(source.stream().filter(s -> cz.maxtechnik.opm.client.util.SearchEngine.matches(s, q)).toList());
			}
		}
		return data.cachedFilteredItems;
	}


	private void renderFavorites(GuiGraphics g, int mx, int my, int favX, int favCols, int listY, int listH, int pX, int leftW) {
		int favScissorRight = Math.min(favX + favCols * (UiKit.SS + UiKit.SP), pX + leftW - 4);
		g.enableScissor(favX, listY, favScissorRight, listY + listH);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -favSb.scroll, 0);
		int mY = (int) (my + favSb.scroll);
		int favCount = Math.max(25, ((data.favorites.size() + favCols - 1) / favCols + 1) * favCols);
		int rowH = UiKit.SS + UiKit.SP;
		int startRow = Math.max(0, (int) (favSb.scroll / rowH));
		int endRow = Math.min((favCount + favCols - 1) / favCols, (int) ((favSb.scroll + listH) / rowH) + 2);
		int firstIdx = startRow * favCols;
		int lastIdx = Math.min(favCount, endRow * favCols);

		for (int i = firstIdx; i < lastIdx; i++) {
			int sx = favX + (i % favCols) * rowH, sy = listY + (i / favCols) * rowH;
			ItemStack s = i < data.favorites.size() ? data.favorites.get(i) : ItemStack.EMPTY;
			renderInvSlot(g, s, sx, sy, mx, mY);
		}
		int favContentH = ((favCount + favCols - 1) / favCols) * rowH;
		pose.popPose();
		g.disableScissor();

		favSb.update(listH, favContentH);
		favSb.render(g, Math.min(favX + favCols * (UiKit.SS + UiKit.SP) + 2, pX + leftW - 4), listY);
	}


	private String getRelativeName(File f) {
		try {
			Path rel = RecipeFileWriter.getRecipeDir().relativize(f.toPath());
			return stripJson(rel.toString().replace('\\', '/'));
		} catch (Exception e) {
			return stripJson(f.getName());
		}
	}

	private void renderRecipeList(GuiGraphics g, int mx, int my, int startX, int listY, int listH) {
		int recW = 9 * (UiKit.SS + UiKit.SP);
		List<File> files = filteredSavedRecipes();
		int maxNameW = files.stream().mapToInt(f -> {
			String name = getRelativeName(f);
			boolean isActive = data.selectedRecipeFile != null && data.selectedRecipeFile.getAbsolutePath().equals(f.getAbsolutePath());
			return font.width(isActive ? "▶ " + name : name);
		}).max().orElse(0);
		int rowW = Math.max(recW, maxNameW + 10);

		g.enableScissor(startX, listY, startX + recW, listY + listH);
		var pose = g.pose();
		pose.pushPose();
		pose.translate(0, -recipeSb.scroll, 0);
		int startIdx = Math.max(0, (int) (recipeSb.scroll / 14));
		int endIdx = Math.min(files.size(), (int) ((recipeSb.scroll + listH) / 14) + 2);

		for (int i = startIdx; i < endIdx; i++) {
			File f = files.get(i);
			String name = getRelativeName(f);
			int ry = listY + i * 14;
			boolean isSel = data.selectedRecipeFiles.contains(f);
			boolean isHov = UiKit.hit(mx, (int) (my + recipeSb.scroll), startX, ry, recW, 14);
			boolean isActive = data.selectedRecipeFile != null && data.selectedRecipeFile.getAbsolutePath().equals(f.getAbsolutePath());
			String displayName = isActive ? "▶ " + name : name;

			if (isSel) g.fill(startX, ry, startX + rowW, ry + 14, 0xFF2255AA);
			else if (isHov) g.fill(startX, ry, startX + rowW, ry + 14, 0xFF333333);
			int color = isSel || isHov ? 0xFFFFFFFF : (isActive ? 0xFF55FF55 : 0xFFAAAAAA);
			g.drawString(font, displayName, startX + 4, ry + 3, color, false);
		}
		pose.popPose();
		g.disableScissor();

		recipeSb.update(listH, files.size() * 14);
		recipeSb.render(g, startX + recW + 2, listY);
	}

	private List<File> filteredSavedRecipes() {
		if (recipeSearchBox == null) return data.savedRecipeFiles;
		String q = recipeSearchBox.getValue();
		if (q.isBlank()) return data.savedRecipeFiles;
		Path base = RecipeFileWriter.getRecipeDir();
		return data.savedRecipeFiles.stream().filter(f -> cz.maxtechnik.opm.client.util.SearchEngine.matchesFile(f, base, q)).toList();
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

	private static void drawActionBtn(GuiGraphics g, Font font, String label, int bx, int by, boolean hover, int bg, int bgHov) {
		g.fill(bx, by, bx + 50, by + 14, hover ? bgHov : bg);
		g.fill(bx, by, bx + 50, by + 1, 0x44FFFFFF);
		g.drawCenteredString(font, label, bx + 50 / 2, by + 3, UiKit.C_TEXT);
	}

	public boolean mouseClicked(int pX, int pY, int pH, int leftW, int invY, int mx, int my, int button, RecipeSelectionListener listener) {
		if (my < invY) return false;
		int startX = pX + 10;
		int favCols = getFavCols(pX, leftW);
		int favX = startX + 9 * (UiKit.SS + UiKit.SP) + 12;


		String[] bTabs = {"Inventory", "Fluids", "Items", "Tags"};
		int recBtnX = calcTabsEnd(startX);
		int recBtnW = font.width(showRecipesList ? "◀ Items" : "Recipes ▶") + 10;

		if (!showRecipesList) {
			int tx = startX;
			for (int i = 0; i < bTabs.length; i++) {
				int tw = font.width(bTabs[i]) + 10;
				if (UiKit.hit(mx, my, tx, invY + 4, tw, 14)) {
					bottomTab = BottomTab.values()[i];
					bottomSb.scroll = 0;
					return true;
				}
				tx += tw + 4;
			}
		}


		if (!showRecipesList && bottomTab != BottomTab.INVENTORY && searchBox != null) {
			if (UiKit.hit(mx, my, searchBox.getX(), searchBox.getY(), searchBox.getWidth(), searchBox.getHeight()) || searchBox.mouseClicked(mx, my, button)) {
				searchBox.setFocused(true);
				return true;
			}
		}



		if (showRecipesList && recipeSearchBox != null) {
			if (recipeSearchBox.mouseClicked(mx, my, button)) return true;
		}

		if (UiKit.hit(mx, my, recBtnX, invY + 4, recBtnW, 14)) {
			showRecipesList = !showRecipesList;
			return true;
		}

		// Recipes list actions
		if (showRecipesList) {
			if (UiKit.hit(mx, my, startX, invY + 4, 50, 14)) {
				if (listener != null) listener.onRecipeDeleted();
				return true;
			}
			if (UiKit.hit(mx, my, startX + 54, invY + 4, 50, 14)) {
				data.clear();
				data.selectedRecipeFile = null;
				data.selectedRecipeFiles.clear();
				return true;
			}
			if (UiKit.hit(mx, my, startX + 108, invY + 4, 50, 14)) {
				data.scanSavedRecipes();
				return true;
			}

			// Recipe item click in list
			int listY = getBottomListY(invY);
			int listH = (pY + pH) - listY - 5;
			int recW = 9 * (UiKit.SS + UiKit.SP);
			if (UiKit.hit(mx, my, startX, listY, recW, listH)) {
				List<File> files = filteredSavedRecipes();
				int mY = (int) (my + recipeSb.scroll);
				int idx = (mY - listY) / 14;
				if (idx >= 0 && idx < files.size()) {
					File f = files.get(idx);
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

		if (!showRecipesList) {
			if (bottomSb.mouseClicked(mx, my, button)) return true;
            return favSb.mouseClicked(mx, my, button);
		} else {
            return recipeSb.mouseClicked(mx, my, button);
		}
    }

	public boolean mouseDragged(int my) {
		if (!showRecipesList) {
			if (bottomSb.mouseDragged(my)) return true;
            return favSb.mouseDragged(my);
		} else {
            return recipeSb.mouseDragged(my);
		}
    }

	public void mouseReleased() {
		bottomSb.mouseReleased();
		favSb.mouseReleased();
		recipeSb.mouseReleased();
	}

	public boolean mouseScrolled(int pX, int pH, int leftW, int invY, int mx, int my, double sy) {
		if (my < invY) return false;
		int startX = pX + 10;
		int favCols = getFavCols(pX, leftW);
		int favX = startX + 9 * (UiKit.SS + UiKit.SP) + 12;


		int listY = getBottomListY(invY);

		if (!showRecipesList) {
			if (UiKit.hit(mx, my, startX, listY, 9 * (UiKit.SS + UiKit.SP), pH - listY - 5)) {
				bottomSb.handleScroll(sy, 12);
				return true;
			}
			if (UiKit.hit(mx, my, favX, listY, favCols * (UiKit.SS + UiKit.SP), pH - listY - 5)) {
				favSb.handleScroll(sy, 12);
				return true;
			}
		} else {
			if (UiKit.hit(mx, my, startX, listY, 9 * (UiKit.SS + UiKit.SP), pH - listY - 5)) {
				recipeSb.handleScroll(sy, 12);
				return true;
			}
		}
		return false;
	}

	public ItemStack itemAt(int pX, int pH, int leftW, int invY, int mx, int my) {
		if (my < invY || showRecipesList) return ItemStack.EMPTY;
		int startX = pX + 10;
		int favCols = getFavCols(pX, leftW);
		int favX = startX + 9 * (UiKit.SS + UiKit.SP) + 12;


		int listY = getBottomListY(invY);
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
				if (UiKit.hit(mx, mY, startX + col * (UiKit.SS + UiKit.SP), listY + 3 * (UiKit.SS + UiKit.SP) + 8, UiKit.SS, UiKit.SS)) {
					return inv.getItem(col);
				}
			}
		} else {
			List<ItemStack> list = filteredList();
			int rowH = UiKit.SS + UiKit.SP;
			int totalRows = (list.size() + 8) / 9;
			int startRow = Math.max(0, (int) (bottomSb.scroll / rowH));
			int endRow = Math.min(totalRows, (int) ((bottomSb.scroll + (pH - listY - 5)) / rowH) + 2);
			int firstIdx = startRow * 9;
			int lastIdx = Math.min(list.size(), endRow * 9);

			for (int i = firstIdx; i < lastIdx; i++) {
				if (UiKit.hit(mx, mY, startX + (i % 9) * rowH, listY + (i / 9) * rowH, UiKit.SS, UiKit.SS)) {
					return list.get(i);
				}
			}
		}

		// Favorites list
		int favListY = listY + 12;
		int mY3 = (int) (my + favSb.scroll);
		if (UiKit.hit(mx, my, favX, favListY, favCols * (UiKit.SS + UiKit.SP), pH - favListY - 5)) {
			for (int i = 0; i < data.favorites.size(); i++) {
				if (UiKit.hit(mx, mY3, favX + (i % favCols) * (UiKit.SS + UiKit.SP), favListY + (i / favCols) * (UiKit.SS + UiKit.SP), UiKit.SS, UiKit.SS)) {
					return data.favorites.get(i);
				}
			}
		}

		return ItemStack.EMPTY;
	}

	public ItemStack itemAtFavorite(int pX, int pH, int leftW, int invY, int mx, int my) {
		if (my < invY || showRecipesList) return ItemStack.EMPTY;
		int startX = pX + 10;
		int favCols = getFavCols(pX, leftW);
		int favX = startX + 9 * (UiKit.SS + UiKit.SP) + 12;

		int listY = getBottomListY(invY);
		int favListY = listY + 12;
		int mY3 = (int) (my + favSb.scroll);
		if (UiKit.hit(mx, my, favX, favListY, favCols * (UiKit.SS + UiKit.SP), pH - favListY - 5)) {
			for (int i = 0; i < data.favorites.size(); i++) {
				if (UiKit.hit(mx, mY3, favX + (i % favCols) * (UiKit.SS + UiKit.SP), favListY + (i / favCols) * (UiKit.SS + UiKit.SP), UiKit.SS, UiKit.SS)) {
					return data.favorites.get(i);
				}
			}
		}
		return ItemStack.EMPTY;
	}

	public boolean isInsideFavoritesArea(int pX, int pH, int leftW, int invY, int mx, int my) {
		if (my < invY || showRecipesList) return false;
		int startX = pX + 10;
		int favCols = getFavCols(pX, leftW);
		int favX = startX + 9 * (UiKit.SS + UiKit.SP) + 12;


		int listY = getBottomListY(invY);
		int favListY = listY + 12;
		return UiKit.hit(mx, my, favX - 4, favListY - 14, favCols * (UiKit.SS + UiKit.SP) + 24, pH - (favListY - 14));

	}
}