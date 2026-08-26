package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.editor.layout.StationLayoutEngine;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Zjednodušený a efektivní renderer pro Recipe Editor obrazovku.
 */
public class EditorRenderer {
	public static final int TAB_H = 22;

	Font font;
	private final RecipeEditorData data;

	public int pX, pY, pW, pH, leftW, rightX;
	public int editorY, invY, btnSaveY;

	public EditorRenderer(Font font, RecipeEditorData data) {
		this.font = font;
		this.data = data;
	}

	public void updateLayout(int pX, int pY, int pW, int pH, int leftW, int rightX, int editorY, int invY, int btnSaveY) {
		this.pX = pX; this.pY = pY; this.pW = pW; this.pH = pH;
		this.leftW = leftW; this.rightX = rightX;
		this.editorY = editorY; this.invY = invY; this.btnSaveY = btnSaveY;
	}

	public void renderBg(GuiGraphics g) {
		g.fill(pX, pY, pX + pW, pY + pH, UiKit.C_BG);
		g.fill(pX, editorY, pX + leftW, invY, UiKit.C_BG);
		g.fill(pX + leftW, pY, rightX, pY + pH, UiKit.C_HEADER);
	}

	public void renderTabs(GuiGraphics g, int mx, int my, List<StationType> tabs, int tabIdx) {
		g.fill(pX, pY, pX + leftW, pY + TAB_H, UiKit.C_HEADER);
		for (int i = 0; i < tabs.size(); i++) {
			StationType t = tabs.get(i);
			int tx = pX + (i * leftW) / tabs.size();
			int nextX = pX + ((i + 1) * leftW) / tabs.size();
			int tw = nextX - tx;
			boolean sel = (i == tabIdx);
			boolean hov = UiKit.hit(mx, my, tx, pY, tw, TAB_H);

			int bg = sel ? UiKit.C_ACCENT_BG : (hov ? UiKit.C_CARD_HOV : UiKit.C_HEADER);
			g.fill(tx, pY, tx + tw, pY + TAB_H, bg);
			if (sel) {
				g.fill(tx, pY + TAB_H - 2, tx + tw, pY + TAB_H, UiKit.C_ACCENT);
			}

			int iconSz = 16;
			int icx = tx + (tw - iconSz) / 2;
			try {
				ResourceLocation loc = ResourceLocation.tryParse(t.stationItemId);
				if (loc != null) {
					var opt = BuiltInRegistries.ITEM.getOptional(loc);
					opt.ifPresent(item -> g.renderItem(new ItemStack(item), icx, pY + (TAB_H - iconSz) / 2));
				}
			} catch (Exception ignored) {}
		}
	}

	public int renderStation(GuiGraphics g, Font font, StationType type, int mx, int my) {
		int cx = pX + leftW / 2;
		Font useFont = font != null ? font : (this.font != null ? this.font : Minecraft.getInstance().font);
		return StationLayoutEngine.render(g, useFont, type, data, cx, leftW, editorY, mx, my);
	}

	public void renderBtnBar(GuiGraphics g, int mx, int my, String fileName, boolean fnFocused, int fnCursor) {
		int genW = Math.min(80, Math.max(60, (leftW - 30) / 4));
		int clearW = Math.min(42, Math.max(32, (leftW - 30) / 7));

		int x = pX + 8;
		int y = btnSaveY;

		boolean hS = UiKit.hit(mx, my, x, y, genW, 16);
		UiKit.drawButton(g, font, "Generate", x, y, genW, 16, hS, UiKit.C_SUCCESS, UiKit.C_SUCCESS_HOV);
		x += genW + 4;

		boolean hC = UiKit.hit(mx, my, x, y, clearW, 16);
		UiKit.drawButton(g, font, "Clear", x, y, clearW, 16, hC, UiKit.C_SLOT, UiKit.C_SLOT_HOV);
		x += clearW + 6;

		int labelW = font.width("File:");
		g.drawString(font, "File:", x, y + 4, UiKit.C_LABEL, false);
		x += labelW + 4;

		int ffw = Math.max(40, (pX + leftW - 8) - x);
		UiKit.drawInputField(g, font, fileName, "my_recipe", fnCursor, fnFocused, x, y, ffw, 16);

		if (!data.statusMsg.isEmpty() && System.currentTimeMillis() < data.statusUntil)
			g.drawCenteredString(font, data.statusMsg, pX + leftW / 2, btnSaveY - 14, data.statusOk ? 0xFF88FF88 : 0xFFFF6666);
	}

	public void renderErrorPopup(GuiGraphics g, int mx, int my, String error, int width, int height) {
		g.fill(0, 0, width, height, 0xAA000000);
		int pw = 260, ph = 100, px2 = (width - pw) / 2, py2 = (height - ph) / 2;
		g.fill(px2, py2, px2 + pw, py2 + ph, UiKit.C_BG);
		UiKit.drawOutline(g, px2, py2, pw, ph, UiKit.C_DANGER_HOV);
		g.drawString(font, "Error", px2 + (pw - font.width("Error")) / 2, py2 + 12, UiKit.C_DANGER_HOV, false);
		g.drawString(font, error, px2 + (pw - font.width(error)) / 2, py2 + 36, UiKit.C_TEXT, false);
		int bx = px2 + (pw - 60) / 2, by = py2 + 65, bw = 60, bh = 18;
		boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
		UiKit.drawButton(g, font, "OK", bx, by, bw, bh, hov, UiKit.C_BTN, UiKit.C_BTN_H);
	}

	public void showTip(GuiGraphics g, ItemStack stack, int mx, int my) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null)
			g.renderComponentTooltip(font, stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL), mx, my);
	}

	public boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
		return UiKit.hit(mx, my, hx, hy, hw, hh);
	}
}