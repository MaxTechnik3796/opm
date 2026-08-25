package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * UI pomocník pro konfigurační obrazovku, který čerpá ze sdíleného design systému UiKit.
 */
public final class ConfigUiHelper {

	private ConfigUiHelper() {}

	public static final int ITEM_H = UiKit.ITEM_H;

	public static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int col) {
		UiKit.drawOutline(g, x, y, w, h, col);
	}

	public static void drawSelectionBox(GuiGraphics g, Font font, int x, int y, int w, int h, String title, boolean hovered, boolean selected, boolean dragging) {
		int outlineCol = dragging ? 0xFFFFAA00 : (selected ? UiKit.C_ACCENT : (hovered ? UiKit.C_ACCENT_HOV : 0x6655AAFF));
		int bgFill = dragging ? 0x22FFAA00 : (selected ? 0x1A55AAFF : (hovered ? 0x1255AAFF : 0x0855AAFF));

		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, bgFill);
		UiKit.drawOutline(g, x - 2, y - 2, w + 4, h + 4, outlineCol);

		if (hovered || selected || dragging) {
			int tagW = font.width(title) + 8;
			int tagH = 11;
			int tagX = x - 2;
			int tagY = y - tagH - 4;
			if (tagY < 4) tagY = y + h + 4;

			g.fill(tagX, tagY, tagX + tagW, tagY + tagH, 0xEE141414);
			UiKit.drawOutline(g, tagX, tagY, tagW, tagH, outlineCol);
			g.drawString(font, title, tagX + 4, tagY + 2, selected || dragging ? 0xFFFFFFFF : 0xFFCCCCCC, false);
		}
	}

	public static void drawToggle(GuiGraphics g, Font font, String label, boolean val, int x, int y, int w, int mx, int my) {
		UiKit.drawToggle(g, font, label, val, x, y, w, mx, my);
	}

	public static boolean isToggleHit(int mx, int my, int x, int y, int w) {
		return UiKit.isToggleHit(mx, my, x, y, w);
	}

	public static void drawStepper(GuiGraphics g, Font font, String label, String valText, int x, int y, int w, int mx, int my) {
		UiKit.drawStepper(g, font, label, valText, x, y, w, mx, my);
	}

	public static int getStepperClick(int mx, int my, int x, int y, int w) {
		return UiKit.getStepperClick(mx, my, x, y, w);
	}

	public static void drawEnumCycler(GuiGraphics g, Font font, String label, String valText, int x, int y, int w, int mx, int my) {
		UiKit.drawEnumCycler(g, font, label, valText, x, y, w, mx, my);
	}

	public static boolean isEnumHit(int mx, int my, int x, int y, int w) {
		return UiKit.isEnumHit(mx, my, x, y, w);
	}

	public static void drawButton(GuiGraphics g, Font font, String label, int x, int y, int w, int h, int mx, int my, int bg, int hbg, int textCol) {
		UiKit.drawButton(g, font, label, x, y, w, h, mx, my, bg, hbg, textCol);
	}

	public static void drawSectionHeader(GuiGraphics g, Font font, String title, int x, int y, int w) {
		UiKit.drawSectionHeader(g, font, title, x, y, w);
	}
}
