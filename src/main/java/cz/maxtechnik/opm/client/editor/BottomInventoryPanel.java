package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.recipe.RecipeFileManager;
import cz.maxtechnik.opm.client.ui.Scrollbar;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class BottomInventoryPanel {
	public enum BottomTab { INVENTORY, FLUIDS, ITEMS, TAGS, FAVORITES, RECIPES }

	private static final String[] TABS = {"Inv", "Fluid", "Item", "Tag", "Fav", "Rec"};
	private static final int GRID_W = 9 * (UiKit.SS + UiKit.SP) - UiKit.SP; // 178px
	private static final int ROW_H = UiKit.SS + UiKit.SP;

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
	public void setBottomTab(BottomTab tab) { this.bottomTab = tab; this.bottomSb.scroll = 0; }
	public EditBox getSearchBox() { return searchBox; }
	public void unfocusSearch() { if (searchBox != null) searchBox.setFocused(false); }
	public boolean isSearchFocused() { return bottomTab != BottomTab.INVENTORY && searchBox != null && searchBox.isFocused(); }

	public void render(GuiGraphics g, int pX, int pY, int pH, int leftW, int invY, int mx, int my) {
		g.fill(pX, invY, pX + leftW, pY + pH, UiKit.C_HEADER);
		g.fill(pX, invY, pX + leftW, invY + 1, UiKit.C_BORDER);
		g.fill(pX + leftW / 2 - 18, invY, pX + leftW / 2 + 18, invY + 2, 0xFF555555);

		int startX = pX + 8;
		UiKit.drawTabs(g, font, startX, invY + 4, GRID_W, 14, TABS, bottomTab.ordinal(), mx, my);

		int searchY = invY + 22;
		if (bottomTab != BottomTab.INVENTORY && searchBox != null) {
			int sw = (bottomTab == BottomTab.RECIPES) ? (GRID_W - 32) : GRID_W;
			searchBox.setWidth(sw);
			UiKit.drawInputField(g, font, searchBox.getValue(), "Search...", searchBox.getCursorPosition(), searchBox.isFocused(), startX, searchY, sw, 14);

			if (bottomTab == BottomTab.RECIPES) {
				int relX = startX + GRID_W - 30, delX = startX + GRID_W - 14;
				UiKit.drawGhostButton(g, font, "⟳", relX, searchY, 14, 14, UiKit.hit(mx, my, relX, searchY, 14, 14), UiKit.C_CARD_HOV, UiKit.C_WHITE);
				UiKit.drawGhostButton(g, font, "✕", delX, searchY, 14, 14, UiKit.hit(mx, my, delX, searchY, 14, 14), UiKit.C_DANGER, UiKit.C_WHITE);
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
			int contentH = renderBottomContent(g, pH, mx, (int) (my + bottomSb.scroll), startX, listY);
			pose.popPose();
			g.disableScissor();

			bottomSb.update(listH, contentH);
			bottomSb.render(g, startX + GRID_W + 3, listY);
		} else {
			renderRecipeList(g, mx, my, startX, listY, listH);
		}
	}

	private int renderBottomContent(GuiGraphics g, int pH, int mx, int mY, int startX, int listY) {
		Minecraft mc = Minecraft.getInstance();
		if (bottomTab == BottomTab.INVENTORY && mc.player != null) {
			Inventory inv = mc.player.getInventory();
			for (int r = 0; r < 4; r++) {
				int sy = listY + r * ROW_H + (r == 3 ? 6 : 0);
				for (int c = 0; c < 9; c++) {
					int slotIdx = r < 3 ? 9 + r * 9 + c : c;
					renderInvSlot(g, inv.getItem(slotIdx), startX + c * ROW_H, sy, mx, mY);
				}
			}
			return 4 * ROW_H + 6;
		}

		List<ItemStack> list = filteredList();
		int totalRows = (list.size() + 8) / 9;
		int startRow = Math.max(0, (int) (bottomSb.scroll / ROW_H));
		int endRow = Math.min(totalRows, (int) ((bottomSb.scroll + (pH - listY - 4)) / ROW_H) + 2);

		for (int i = startRow * 9; i < Math.min(list.size(), endRow * 9); i++) {
			renderInvSlot(g, list.get(i), startX + (i % 9) * ROW_H, listY + (i / 9) * ROW_H, mx, mY);
		}
		return totalRows * ROW_H;
	}

	public void invalidateFilter() {
		lastSearch = null;
		lastBottomTab = null;
		data.cachedFilteredItems.clear();
	}

	private List<ItemStack> filteredList() {
		String q = searchBox != null ? searchBox.getValue() : "";
		if (bottomTab == BottomTab.FAVORITES) {
			return q.isBlank() ? data.favorites : data.favorites.stream().filter(s -> SearchEngine.matches(s, q)).toList();
		}
		if (!q.equals(lastSearch) || bottomTab != lastBottomTab) {
			lastSearch = q;
			lastBottomTab = bottomTab;
			data.cachedFilteredItems.clear();
			List<ItemStack> source = switch (bottomTab) {
				case FLUIDS -> data.availableFluids;
				case TAGS -> data.cachedTags;
				case ITEMS -> data.allItems;
				default -> List.of();
			};
			data.cachedFilteredItems.addAll(q.isBlank() ? source : source.stream().filter(s -> SearchEngine.matches(s, q)).toList());
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
		int maxW = files.stream().mapToInt(f -> font.width(f.equals(data.selectedRecipeFile) ? "▶ " + getRelativeName(f) : getRelativeName(f))).max().orElse(0);
		int rowW = Math.max(GRID_W, maxW + 10);

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
			boolean isSel = data.selectedRecipeFiles.contains(f), isHov = UiKit.hit(mx, (int) (my + bottomSb.scroll), startX, ry, GRID_W, 14);
			boolean isActive = f.equals(data.selectedRecipeFile);

			if (isSel) g.fill(startX, ry, startX + rowW, ry + 14, UiKit.C_ACCENT_BG);
			else if (isHov) g.fill(startX, ry, startX + rowW, ry + 14, UiKit.C_CARD_HOV);
			g.drawString(font, isActive ? "▶ " + name : name, startX + 4, ry + 3, isSel || isHov ? UiKit.C_WHITE : (isActive ? 0xFF55FF55 : UiKit.C_LABEL), false);
		}
		pose.popPose();
		g.disableScissor();

		bottomSb.update(listH, files.size() * 14);
		bottomSb.render(g, startX + GRID_W + 3, listY);
	}

	private List<File> filteredSavedRecipes() {
		if (searchBox == null || searchBox.getValue().isBlank()) return data.savedRecipeFiles;
		Path base = RecipeFileManager.getRecipeDir();
		return data.savedRecipeFiles.stream().filter(f -> SearchEngine.matchesFile(f, base, searchBox.getValue())).toList();
	}

	private static String stripJson(String s) {
		return s.endsWith(".json") ? s.substring(0, s.length() - 5) : s;
	}

	private void renderInvSlot(GuiGraphics g, ItemStack stack, int sx, int sy, int mx, int my) {
		UiKit.slot(g, font, mx, my, stack, sx, sy, UiKit.C_SLOT, false);
	}

	public boolean mouseClicked(int pX, int pY, int pH, int invY, int mx, int my, int button, RecipeSelectionListener listener) {
		if (my < invY) return false;
		int startX = pX + 8;

		int clickedTab = UiKit.getClickedTab(startX, invY + 4, GRID_W, 14, TABS.length, mx, my);
		if (clickedTab >= 0) {
			setBottomTab(BottomTab.values()[clickedTab]);
			if (searchBox != null) searchBox.setValue("");
			return true;
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
				if (UiKit.hit(mx, my, startX + GRID_W - 30, searchY, 14, 14)) {
					data.scanSavedRecipes();
					data.status("Reloaded recipes", true);
					return true;
				}
				if (UiKit.hit(mx, my, startX + GRID_W - 14, searchY, 14, 14)) {
					if (listener != null) listener.onRecipeDeleted();
					return true;
				}
			}
		}

		if (bottomTab == BottomTab.RECIPES) {
			int listY = invY + 38, listH = (pY + pH) - listY - 4;
			if (UiKit.hit(mx, my, startX, listY, GRID_W, listH)) {
				List<File> files = filteredSavedRecipes();
				int idx = (int) (my + bottomSb.scroll - listY) / 14;
				if (idx >= 0 && idx < files.size()) {
					File f = files.get(idx);
					long now = System.currentTimeMillis();
					if (now - lastRecipeClickTime < 350 && f.equals(lastRecipeClickedFile)) {
						if (listener != null) listener.onRecipeUnloaded();
						lastRecipeClickTime = 0;
						lastRecipeClickedFile = null;
						return true;
					}
					lastRecipeClickTime = now;
					lastRecipeClickedFile = f;
					if (Screen.hasControlDown()) {
						if (!data.selectedRecipeFiles.remove(f)) data.selectedRecipeFiles.add(f);
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

	public boolean mouseDragged(int my) { return bottomSb.mouseDragged(my); }
	public void mouseReleased() { bottomSb.mouseReleased(); }

	public boolean mouseScrolled(int pX, int pH, int invY, int mx, int my, double sy) {
		if (my < invY) return false;
		int listY = (bottomTab == BottomTab.INVENTORY) ? (invY + 22) : (invY + 38);
		if (UiKit.hit(mx, my, pX + 8, listY, GRID_W + 10, pH - listY - 4)) {
			bottomSb.handleScroll(sy, 12);
			return true;
		}
		return false;
	}

	public ItemStack itemAt(int pX, int pH, int invY, int mx, int my) {
		if (my < invY || bottomTab == BottomTab.RECIPES) return ItemStack.EMPTY;
		int startX = pX + 8, listY = (bottomTab == BottomTab.INVENTORY) ? (invY + 22) : (invY + 38);
		int mY = (int) (my + bottomSb.scroll);

		if (bottomTab == BottomTab.INVENTORY && Minecraft.getInstance().player != null) {
			Inventory inv = Minecraft.getInstance().player.getInventory();
			for (int r = 0; r < 4; r++) {
				int sy = listY + r * ROW_H + (r == 3 ? 6 : 0);
				for (int c = 0; c < 9; c++) {
					if (UiKit.hit(mx, mY, startX + c * ROW_H, sy, UiKit.SS, UiKit.SS)) {
						return inv.getItem(r < 3 ? 9 + r * 9 + c : c);
					}
				}
			}
		} else {
			List<ItemStack> list = filteredList();
			int startRow = Math.max(0, (int) (bottomSb.scroll / ROW_H));
			int endRow = Math.min((list.size() + 8) / 9, (int) ((bottomSb.scroll + (pH - listY - 4)) / ROW_H) + 2);
			for (int i = startRow * 9; i < Math.min(list.size(), endRow * 9); i++) {
				if (UiKit.hit(mx, mY, startX + (i % 9) * ROW_H, listY + (i / 9) * ROW_H, UiKit.SS, UiKit.SS)) {
					return list.get(i);
				}
			}
		}
		return ItemStack.EMPTY;
	}

	public ItemStack itemAtFavorite(int pX, int pH, int invY, int mx, int my) {
		return bottomTab == BottomTab.FAVORITES ? itemAt(pX, pH, invY, mx, my) : ItemStack.EMPTY;
	}

	public boolean isInsideFavoritesArea(int pX, int pH, int invY, int mx, int my) {
		if (my < invY || bottomTab == BottomTab.RECIPES) return false;
		int startX = pX + 8, favX = startX + (4 * GRID_W) / TABS.length, favW = ((5 * GRID_W) / TABS.length) - ((4 * GRID_W) / TABS.length);
		return UiKit.hit(mx, my, favX, invY + 4, favW, 14) || (bottomTab == BottomTab.FAVORITES && UiKit.hit(mx, my, startX, invY, GRID_W, pH - invY));
	}
}