package cz.maxtechnik.opm.client.screen.config;

import cz.maxtechnik.opm.client.widget.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class ConfigUiHelper {

	private ConfigUiHelper() {}

	public static final int C_BG = 0xF0181818;
	public static final int C_HEADER = 0xFF141414;
	public static final int C_CARD = 0xFF1F1F1F;
	public static final int C_CARD_HOV = 0xFF272727;
	public static final int C_BORDER = 0xFF000000;
	public static final int C_ACCENT = 0xFF4A68AA;
	public static final int C_ACCENT_HOV = 0xFF5D80D0;
	public static final int C_SUCCESS = 0xFF2E6B34;
	public static final int C_SUCCESS_HOV = 0xFF3D8C46;
	public static final int C_DANGER = 0xFF6B2E2E;
	public static final int C_DANGER_HOV = 0xFF8C3D3D;
	public static final int C_TEXT = 0xFFEEEEEE;
	public static final int C_LABEL = 0xFFAAAAAA;
	public static final int C_MUTED = 0xFF666666;

	public static final int ITEM_H = 22;

	public static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int col) {
		g.fill(x, y, x + w, y + 1, col);
		g.fill(x, y + h - 1, x + w, y + h, col);
		g.fill(x, y + 1, x + 1, y + h - 1, col);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
	}

	public static void drawSelectionBox(GuiGraphics g, Font font, int x, int y, int w, int h, String title, boolean hovered, boolean selected, boolean dragging) {
		int outlineCol = dragging ? 0xFFFFAA00 : (selected ? 0xFF55AAFF : (hovered ? 0xFF88CCFF : 0x6655AAFF));
		int bgFill = dragging ? 0x22FFAA00 : (selected ? 0x1A55AAFF : (hovered ? 0x1255AAFF : 0x0855AAFF));

		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, bgFill);
		drawOutline(g, x - 2, y - 2, w + 4, h + 4, outlineCol);

		if (hovered || selected || dragging) {
			int tagW = font.width(title) + 8;
			int tagH = 11;
			int tagX = x - 2;
			int tagY = y - tagH - 4;
			if (tagY < 4) tagY = y + h + 4;

			g.fill(tagX, tagY, tagX + tagW, tagY + tagH, 0xEE141414);
			drawOutline(g, tagX, tagY, tagW, tagH, outlineCol);
			g.drawString(font, title, tagX + 4, tagY + 2, selected || dragging ? 0xFFFFFFFF : 0xFFCCCCCC, false);
		}
	}

	public static void drawToggle(GuiGraphics g, Font font, String label, boolean val, int x, int y, int w, int mx, int my) {
		g.drawString(font, label, x + 4, y + 6, C_TEXT, false);
		int btnW = 38, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		boolean hov = UiKit.hit(mx, my, bx, by, btnW, btnH);

		int bg = val ? (hov ? C_SUCCESS_HOV : C_SUCCESS) : (hov ? 0xFF444444 : 0xFF2A2A2A);
		g.fill(bx, by, bx + btnW, by + btnH, bg);
		drawOutline(g, bx, by, btnW, btnH, C_BORDER);
		g.drawCenteredString(font, val ? "ON" : "OFF", bx + btnW / 2, by + 3, val ? 0xFFFFFFFF : C_LABEL);
	}

	public static boolean isToggleHit(int mx, int my, int x, int y, int w) {
		int btnW = 38, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		return UiKit.hit(mx, my, bx, by, btnW, btnH);
	}

	public static void drawStepper(GuiGraphics g, Font font, String label, String valText, int x, int y, int w, int mx, int my) {
		g.drawString(font, label, x + 4, y + 6, C_TEXT, false);
		int bw = 14, bh = 14;
		int bxL = x + w - 64, by = y + (ITEM_H - bh) / 2;
		int bxR = x + w - bw - 4;
		boolean hL = UiKit.hit(mx, my, bxL, by, bw, bh);
		boolean hR = UiKit.hit(mx, my, bxR, by, bw, bh);

		// Minus button
		g.fill(bxL, by, bxL + bw, by + bh, hL ? 0xFF505050 : 0xFF303030);
		drawOutline(g, bxL, by, bw, bh, C_BORDER);
		g.drawCenteredString(font, "−", bxL + bw / 2, by + 3, hL ? 0xFFFFFFFF : C_LABEL);

		// Value in middle
		int midX = (bxL + bw + bxR) / 2;
		g.drawCenteredString(font, valText, midX, by + 3, C_TEXT);

		// Plus button
		g.fill(bxR, by, bxR + bw, by + bh, hR ? 0xFF505050 : 0xFF303030);
		drawOutline(g, bxR, by, bw, bh, C_BORDER);
		g.drawCenteredString(font, "+", bxR + bw / 2, by + 3, hR ? 0xFFFFFFFF : C_LABEL);
	}

	public static int getStepperClick(int mx, int my, int x, int y, int w) {
		int bw = 14, bh = 14;
		int bxL = x + w - 64, by = y + (ITEM_H - bh) / 2;
		int bxR = x + w - bw - 4;
		if (UiKit.hit(mx, my, bxL, by, bw, bh)) return -1;
		if (UiKit.hit(mx, my, bxR, by, bw, bh)) return 1;
		return 0;
	}

	public static void drawEnumCycler(GuiGraphics g, Font font, String label, String valText, int x, int y, int w, int mx, int my) {
		g.drawString(font, label, x + 4, y + 6, C_TEXT, false);
		int btnW = 68, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		boolean hov = UiKit.hit(mx, my, bx, by, btnW, btnH);

		g.fill(bx, by, bx + btnW, by + btnH, hov ? C_ACCENT_HOV : C_ACCENT);
		drawOutline(g, bx, by, btnW, btnH, C_BORDER);
		g.drawCenteredString(font, valText, bx + btnW / 2, by + 3, 0xFFFFFFFF);
	}

	public static boolean isEnumHit(int mx, int my, int x, int y, int w) {
		int btnW = 68, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		return UiKit.hit(mx, my, bx, by, btnW, btnH);
	}

	public static void drawButton(GuiGraphics g, Font font, String label, int x, int y, int w, int h, int mx, int my, int bg, int hbg, int textCol) {
		boolean hov = UiKit.hit(mx, my, x, y, w, h);
		g.fill(x, y, x + w, y + h, hov ? hbg : bg);
		drawOutline(g, x, y, w, h, C_BORDER);
		g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hov ? 0xFFFFFFFF : textCol);
	}

	public static void drawSectionHeader(GuiGraphics g, Font font, String title, int x, int y, int w) {
		g.fill(x, y + 2, x + w, y + ITEM_H - 2, 0xFF141414);
		g.fill(x, y + 4, x + 2, y + ITEM_H - 4, C_ACCENT);
		g.drawString(font, title.toUpperCase(), x + 7, y + 6, C_ACCENT, false);
	}
}
