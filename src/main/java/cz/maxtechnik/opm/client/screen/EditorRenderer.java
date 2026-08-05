package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.screen.layout.StationLayoutEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EditorRenderer {
	public static final int C_BG = 0xFF181818;
	public static final int C_BORDER = 0xFF000000;
	public static final int C_TAB = 0xFF282828;
	public static final int C_TAB_SEL = 0xFF4A4A6A;
	public static final int C_TAB_CR = 0xFF352010;
	public static final int C_TAB_CRS = 0xFF603810;
	public static final int C_TEXT = 0xFFEEEEEE;
	public static final int C_LABEL = 0xFFAAAAAA;
	public static final int C_BTN = 0xFF383838;
	public static final int C_BTN_H = 0xFF585858;
	public static final int C_BTN_G = 0xFF1E4A1E;
	public static final int C_BTN_GH = 0xFF2A6A2A;

	public static final int TAB_H = 22;

	Font font;
	private final RecipeEditorData data;

	public int pX, pY, pW, pH, leftW, rightX, rightW;
	public int editorY, editorH, invY;
	public int btnSaveX, btnSaveY, btnClearX, btnCopyX;
	public boolean isDragging;

	public EditorRenderer(Font font, RecipeEditorData data) {
		this.font = font;
		this.data = data;
	}

	public void renderBg(GuiGraphics g) {
		g.fill(pX, pY, pX + pW, pY + pH, C_BG);
		g.fill(pX, editorY, pX + leftW, invY, 0xFF222222);
		g.fill(pX + leftW, pY, rightX, pY + pH, 0xFF111111);
	}

	public void updateLayout(int pX, int pY, int pW, int pH, int leftW, int rightX, int rightW, int editorY, int editorH, int invY, int btnSaveX, int btnSaveY, int btnClearX, int btnCopyX) {
		this.pX = pX; this.pY = pY; this.pW = pW; this.pH = pH;
		this.leftW = leftW; this.rightX = rightX; this.rightW = rightW;
		this.editorY = editorY; this.editorH = editorH; this.invY = invY;
		this.btnSaveX = btnSaveX; this.btnSaveY = btnSaveY;
		this.btnClearX = btnClearX; this.btnCopyX = btnCopyX;
	}

	public void renderTabs(GuiGraphics g, int mx, int my, List<StationType> tabs, int tabIdx) {
		for (int i = 0; i < tabs.size(); i++) {
			StationType t = tabs.get(i);
			int tx = pX + (i * leftW) / tabs.size();
			int nextX = pX + ((i + 1) * leftW) / tabs.size();
			int tw = nextX - tx;
			boolean sel = i == tabIdx, hov = hit(mx, my, tx, pY, tw, TAB_H), cr = t.isCreate();

			int bg = sel ? (cr ? C_TAB_CRS : C_TAB_SEL) : hov ? (cr ? C_TAB_CR : 0xFF353535) : (cr ? C_TAB_CR : C_TAB);
			g.fill(tx, pY, tx + tw, pY + TAB_H, bg);
			if (sel) g.fill(tx, pY + TAB_H - 2, tx + tw, pY + TAB_H, 0xFF8888FF);
			if (i < tabs.size() - 1) g.fill(tx + tw - 1, pY + 2, tx + tw, pY + TAB_H - 2, 0xFF444444);
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
		return StationLayoutEngine.render(g, useFont, type, data, cx, leftW, editorY, mx, my, isDragging);
	}

	public void drawBtn(GuiGraphics g, String lbl, int bx, int by, int bw, boolean hov, int bg, int hbg) {
		g.fill(bx, by, bx + bw, by + 16, hov ? hbg : bg);
		g.fill(bx, by, bx + bw, by + 1, 0x44FFFFFF);
		g.drawCenteredString(font, lbl, bx + bw / 2, by + 4, C_TEXT);
	}

	public float getBtnScale(int leftW) {
		int reqW = 340;
		int availW = leftW - 16;
		if (availW < reqW && availW > 0) {
			return Math.max(0.5f, (float) availW / reqW);
		}
		return 1.0f;
	}

	public void renderBtnBar(GuiGraphics g, int mx, int my, String fileName, boolean fnFocused, int fnCursor) {
		float scale = getBtnScale(leftW);
		boolean scaled = scale < 0.99f;
		if (scaled) {
			g.pose().pushPose();
			g.pose().translate(btnSaveX, btnSaveY, 0);
			g.pose().scale(scale, scale, 1.0f);
			g.pose().translate(-btnSaveX, -btnSaveY, 0);
			mx = (int) (btnSaveX + (mx - btnSaveX) / scale);
			my = (int) (btnSaveY + (my - btnSaveY) / scale);
		}

		boolean hS = hit(mx, my, btnSaveX, btnSaveY, 92, 16);
		boolean hC = hit(mx, my, btnClearX, btnSaveY, 40, 16);
		boolean hP = hit(mx, my, btnCopyX, btnSaveY, 60, 16);
		drawBtn(g, "Generate", btnSaveX, btnSaveY, 92, hS, C_BTN_G, C_BTN_GH);
		drawBtn(g, "Clear", btnClearX, btnSaveY, 40, hC, C_BTN, C_BTN_H);
		drawBtn(g, "Copy", btnCopyX, btnSaveY, 60, hP, C_BTN, C_BTN_H);
		int fx = btnCopyX + 65, fy = btnSaveY;
		g.drawString(font, "File:", fx, fy + 4, C_LABEL, false);
		int ffx = fx + font.width("File:") + 5;
		int ffw = 80;
		g.fill(ffx - 1, fy - 1, ffx + ffw + 1, fy + 17, C_BORDER);
		g.fill(ffx, fy, ffx + ffw, fy + 16, fnFocused ? 0xFF3D3D3D : 0xFF303030);
		String dn = truncate(fileName, ffw - 6);
		g.drawString(font, dn, ffx + 4, fy + 4, C_TEXT, false);
		if (fnFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
			int cx = ffx + 4 + font.width(dn.substring(0, Math.min(fnCursor, dn.length())));
			g.fill(cx, fy + 3, cx + 1, fy + 13, C_TEXT);
		}

		if (scaled) {
			g.pose().popPose();
		}

		if (!data.statusMsg.isEmpty() && System.currentTimeMillis() < data.statusUntil)
			g.drawCenteredString(font, data.statusMsg, leftW / 2, btnSaveY - 14, data.statusOk ? 0xFF88FF88 : 0xFFFF6666);
	}

	public void renderErrorPopup(GuiGraphics g, int mx, int my, String error, int width, int height) {
		g.fill(0, 0, width, height, 0xAA000000);
		int pw = 260, ph = 100, px2 = (width - pw) / 2, py2 = (height - ph) / 2;
		g.fill(px2, py2, px2 + pw, py2 + ph, 0xFF222222);
		g.fill(px2, py2, px2 + pw, py2 + 2, 0xFFFF3333);
		g.fill(px2, py2 + ph - 2, px2 + pw, py2 + ph, 0xFFFF3333);
		g.fill(px2, py2, px2 + 2, py2 + ph, 0xFFFF3333);
		g.fill(px2 + pw - 2, py2, px2 + pw, py2 + ph, 0xFFFF3333);
		g.drawString(font, "Error", px2 + (pw - font.width("Error")) / 2, py2 + 12, 0xFFFF3333, false);
		g.drawString(font, error, px2 + (pw - font.width(error)) / 2, py2 + 36, C_TEXT, false);
		int bx = px2 + (pw - 60) / 2, by = py2 + 65, bw = 60, bh = 18;
		boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
		g.fill(bx, by, bx + bw, by + bh, hov ? 0xFF666666 : 0xFF444444);
		g.fill(bx, by, bx + bw, by + 1, 0xFF888888);
		g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF888888);
		g.drawString(font, "OK", bx + (bw - font.width("OK")) / 2, by + 5, C_TEXT, false);
	}

	public void showTip(GuiGraphics g, ItemStack stack, int mx, int my) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null)
			g.renderComponentTooltip(font, stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL), mx, my);
	}

	public boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
		return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
	}

	private String truncate(String text, int maxW) {
		if (font.width(text) <= maxW) return text;
		while (font.width(text + "…") > maxW && !text.isEmpty()) text = text.substring(0, text.length() - 1);
		return text + "…";
	}
}