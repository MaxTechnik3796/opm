package cz.maxtechnik.opm.client.ui;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Centrální UI knihovna a design systém pro OPM mod.
 * Poskytuje jednotnou barevnou paletu, tvary oken, tlačítka, přepínače, steppery a sloty pro všechny obrazovky.
 */
public final class UiKit {

	private UiKit() {}

	// ─── OPM Signature Color Palette ──────────────────────────────────────────
	public static final int C_BORDER          = 0xFF000000;

	// Backgrounds
	public static final int C_BG              = 0xFF181818;
	public static final int C_HEADER          = 0xFF121212;
	public static final int C_FOOTER          = 0xFF141414;
	public static final int C_POPUP_BG        = 0xEE121212;
	public static final int C_DIM_BG          = 0xAA000000;
	public static final int C_CARD_HOV        = 0xFF282828;
	public static final int C_ROW_HOV         = 0x12FFFFFF;

	// OPM Signature Electric Blue Accents
	public static final int C_ACCENT          = 0xFF55AAFF;
	public static final int C_ACCENT_HOV      = 0xFF77BBFF;
	public static final int C_ACCENT_BG       = 0xFF2A446A;
	public static final int C_ACCENT_BG_H     = 0xFF3A5E94;
	public static final int C_HOVER_ACCENT    = 0xFF8888FF;

	// Status & Feedback Colors
	public static final int C_SUCCESS         = 0xFF2E6B34;
	public static final int C_SUCCESS_HOV     = 0xFF3D8C46;
	public static final int C_SUCCESS_TEXT    = 0xFF55FF55;
	public static final int C_SUCCESS_BORDER  = 0x6600FF66;
	public static final int C_DANGER          = 0xFF6B2E2E;
	public static final int C_DANGER_HOV      = 0xFF8C3D3D;
	public static final int C_DANGER_TEXT     = 0xFFFF5555;
	public static final int C_WARNING_TEXT    = 0xFFFFAA00;
	public static final int C_WARNING_BG      = 0x22FFAA00;

	// Typography
	public static final int C_WHITE           = 0xFFFFFFFF;
	public static final int C_TEXT            = 0xFFEEEEEE;
	public static final int C_LABEL           = 0xFFAAAAAA;
	public static final int C_MUTED           = 0xFF666666;

	// Buttons & Interactive
	public static final int C_BTN             = 0xFF383838;
	public static final int C_BTN_H           = 0xFF585858;
	public static final int C_BTN_OFF         = 0x22FFFFFF;
	public static final int C_BTN_OFF_H       = 0x44FFFFFF;

	// Slots & Grids
	public static final int C_SLOT            = 0xFF242424;
	public static final int C_SLOT_HOV        = 0xFF3A3A3A;
	public static final int C_SLOT_DR         = 0xFF3A5A3A;
	public static final int C_SLOT_RES        = 0xFF1C381C;

	// ─── Dimensions ──────────────────────────────────────────────────────────
	public static final int SS = 18;
	public static final int SP = 2;
	public static final int SPIN_W = 10, SPIN_H = 8;
	public static final int MINI_SPIN = 9;
	public static final int ITEM_H = 22;

	// ─── Hit Testing ─────────────────────────────────────────────────────────
	public static boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
		return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
	}

	// ─── Window & Outline Drawing ────────────────────────────────────────────
	public static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int col) {
		g.fill(x, y, x + w, y + 1, col);
		g.fill(x, y + h - 1, x + w, y + h, col);
		g.fill(x, y + 1, x + 1, y + h - 1, col);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
	}

	public static void drawWindow(GuiGraphics g, int x, int y, int w, int h, int headerH, int footerH) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, C_BORDER);
		g.fill(x, y, x + w, y + h, C_BG);

		if (headerH > 0) {
			g.fill(x, y, x + w, y + headerH, C_HEADER);
			g.fill(x, y + headerH, x + w, y + headerH + 1, C_BORDER);
		}

		if (footerH > 0) {
			int ftrY = y + h - footerH;
			g.fill(x, ftrY, x + w, ftrY + 1, C_BORDER);
			g.fill(x, ftrY + 1, x + w, y + h, C_FOOTER);
		}
	}
	public static void drawSelectionBox(GuiGraphics g, Font font, int x, int y, int w, int h, String title, boolean hovered, boolean selected, boolean dragging) {
		int outlineCol = dragging ? C_WARNING_TEXT : (selected ? C_ACCENT : (hovered ? C_ACCENT_HOV : 0x6655AAFF));
		int bgFill = dragging ? C_WARNING_BG : (selected ? 0x1A55AAFF : (hovered ? 0x1255AAFF : 0x0855AAFF));

		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, bgFill);
		drawOutline(g, x - 2, y - 2, w + 4, h + 4, outlineCol);

		if (hovered || selected || dragging) {
			int tagW = font.width(title) + 8;
			int tagH = 11;
			int tagX = x - 2;
			int tagY = y - tagH - 4;
			if (tagY < 4) tagY = y + h + 4;

			g.fill(tagX, tagY, tagX + tagW, tagY + tagH, C_FOOTER);
			drawOutline(g, tagX, tagY, tagW, tagH, outlineCol);
			g.drawString(font, title, tagX + 4, tagY + 2, selected || dragging ? C_WHITE : C_TEXT, false);
		}
	}

	public static String truncate(Font font, String text, int maxW) {
		if (text == null || font.width(text) <= maxW) return text;
		while (text.length() > 1 && font.width(text + "…") > maxW) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "…";
	}

	// ─── Section Header ──────────────────────────────────────────────────────
	public static void drawSectionHeader(GuiGraphics g, Font font, String title, int x, int y, int w) {
		g.fill(x, y + 2, x + w, y + ITEM_H - 2, C_HEADER);
		g.fill(x, y + 4, x + 2, y + ITEM_H - 4, C_ACCENT);
		g.drawString(font, title.toUpperCase(), x + 8, y + 7, C_ACCENT, false);
	}

	// ─── Buttons ─────────────────────────────────────────────────────────────
	public static void drawButton(GuiGraphics g, Font font, String label, int x, int y, int w, int h, int mx, int my, int bg, int hbg, int textCol) {
		boolean hov = hit(mx, my, x, y, w, h);
		g.fill(x, y, x + w, y + h, hov ? hbg : bg);
		drawOutline(g, x, y, w, h, C_BORDER);
		g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hov ? C_WHITE : textCol);
	}

	// ─── Toggle Switch (ON / OFF Pill) ───────────────────────────────────────
	public static void drawToggle(GuiGraphics g, Font font, String label, boolean val, int x, int y, int w, int mx, int my) {
		boolean rowHov = hit(mx, my, x, y, w, ITEM_H);
		if (rowHov) g.fill(x, y + 1, x + w, y + ITEM_H - 1, C_ROW_HOV);

		g.drawString(font, label, x + 6, y + 7, rowHov ? C_WHITE : C_TEXT, false);
		int btnW = 38, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		boolean hov = hit(mx, my, bx, by, btnW, btnH);

		int bg = val ? (hov ? C_SUCCESS_HOV : C_SUCCESS) : (hov ? C_BTN_OFF_H : C_BTN_OFF);
		g.fill(bx, by, bx + btnW, by + btnH, bg);
		drawOutline(g, bx, by, btnW, btnH, val ? C_SUCCESS_BORDER : C_BORDER);
		g.drawCenteredString(font, val ? "ON" : "OFF", bx + btnW / 2, by + 3, val ? C_WHITE : C_LABEL);
	}

	public static boolean isToggleHit(int mx, int my, int x, int y, int w) {
		int btnW = 38, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		return hit(mx, my, bx, by, btnW, btnH);
	}

	// ─── Stepper (- / +) ─────────────────────────────────────────────────────
	public static void drawStepper(GuiGraphics g, Font font, String label, String valText, int x, int y, int w, int mx, int my) {
		boolean rowHov = hit(mx, my, x, y, w, ITEM_H);
		if (rowHov) g.fill(x, y + 1, x + w, y + ITEM_H - 1, C_ROW_HOV);

		g.drawString(font, label, x + 6, y + 7, rowHov ? C_WHITE : C_TEXT, false);
		int bw = 14, bh = 14;
		int bxL = x + w - 64, by = y + (ITEM_H - bh) / 2;
		int bxR = x + w - bw - 4;
		boolean hL = hit(mx, my, bxL, by, bw, bh);
		boolean hR = hit(mx, my, bxR, by, bw, bh);

		// Minus
		g.fill(bxL, by, bxL + bw, by + bh, hL ? C_BTN_H : C_BTN_OFF);
		g.drawCenteredString(font, "−", bxL + bw / 2, by + 3, hL ? C_ACCENT_HOV : C_LABEL);

		// Value
		int midX = (bxL + bw + bxR) / 2;
		g.drawCenteredString(font, valText, midX, by + 3, rowHov ? C_WHITE : C_TEXT);

		// Plus
		g.fill(bxR, by, bxR + bw, by + bh, hR ? C_BTN_H : C_BTN_OFF);
		g.drawCenteredString(font, "+", bxR + bw / 2, by + 3, hR ? C_ACCENT_HOV : C_LABEL);
	}

	public static int getStepperClick(int mx, int my, int x, int y, int w) {
		int bw = 14, bh = 14;
		int bxL = x + w - 64, by = y + (ITEM_H - bh) / 2;
		int bxR = x + w - bw - 4;
		if (hit(mx, my, bxL, by, bw, bh)) return -1;
		if (hit(mx, my, bxR, by, bw, bh)) return 1;
		return 0;
	}

	// ─── Enum / Option Cycler ────────────────────────────────────────────────
	public static void drawEnumCycler(GuiGraphics g, Font font, String label, String valText, int x, int y, int w, int mx, int my) {
		boolean rowHov = hit(mx, my, x, y, w, ITEM_H);
		if (rowHov) g.fill(x, y + 1, x + w, y + ITEM_H - 1, C_ROW_HOV);

		g.drawString(font, label, x + 6, y + 7, rowHov ? C_WHITE : C_TEXT, false);
		int btnW = 68, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		boolean hov = hit(mx, my, bx, by, btnW, btnH);

		g.fill(bx, by, bx + btnW, by + btnH, hov ? C_ACCENT_BG_H : C_ACCENT_BG);
		drawOutline(g, bx, by, btnW, btnH, hov ? C_ACCENT : C_BORDER);
		g.drawCenteredString(font, valText, bx + btnW / 2, by + 3, hov ? C_WHITE : C_TEXT);
	}

	public static boolean isEnumHit(int mx, int my, int x, int y, int w) {
		int btnW = 68, btnH = 14;
		int bx = x + w - btnW - 4, by = y + (ITEM_H - btnH) / 2;
		return hit(mx, my, bx, by, btnW, btnH);
	}

	// ─── Item & Fluid Slots ──────────────────────────────────────────────────
	public static void slot(GuiGraphics g, Font font, int mx, int my, ItemStack s, int sx, int sy, int bg, boolean isDragging) {
		boolean hov = hit(mx, my, sx, sy, SS, SS);
		boolean drop = isDragging && hov;
		g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, C_BORDER);
		g.fill(sx, sy, sx + SS, sy + SS, drop ? C_SLOT_DR : (hov ? C_SLOT_HOV : bg));
		if (s != null && !s.isEmpty()) {
			ItemStack rs = s.copy();
			rs.setCount(1);
			g.renderItem(rs, sx + 1, sy + 1);
			g.renderItemDecorations(font, rs, sx + 1, sy + 1);
		}
	}

	public static void slotFluid(GuiGraphics g, Font font, int mx, int my, FluidEntry f, int sx, int sy, boolean isDragging) {
		boolean hov = hit(mx, my, sx, sy, SS, SS);
		boolean drop = isDragging && hov;
		g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, 0xFF2255AA);
		g.fill(sx, sy, sx + SS, sy + SS, drop ? 0xFF2A5A6A : (hov ? 0xFF2A3A6A : 0xFF1A2A4A));
		if (!f.isEmpty()) {
			g.renderItem(f.proxy, sx + 1, sy + 1);
		} else {
			g.drawCenteredString(font, "~", sx + SS / 2, sy + (SS - 8) / 2, 0xFF4488CC);
		}

		int amtX = sx + SS + 4, amtY = sy + 4;
		g.drawString(font, f.amount + " mB", amtX, amtY, 0xFF66AAFF, false);

		boolean hP = hit(mx, my, amtX - 2, amtY + 12, SPIN_W, SPIN_H);
		boolean hM = hit(mx, my, amtX + 10, amtY + 12, SPIN_W, SPIN_H);
		if (hP) g.fill(amtX - 2, amtY + 12, amtX + 8, amtY + 20, 0xFF333333);
		if (hM) g.fill(amtX + 10, amtY + 12, amtX + 20, amtY + 20, 0xFF333333);

		g.drawCenteredString(font, "+", amtX + 3, amtY + 12, hP ? C_HOVER_ACCENT : C_LABEL);
		g.drawCenteredString(font, "-", amtX + 15, amtY + 12, hM ? C_HOVER_ACCENT : C_LABEL);
	}

	public static void spinner(GuiGraphics g, Font font, int mx, int my, int cx, int cy, int count) {
		g.drawString(font, String.valueOf(count), cx, cy + 2, C_TEXT, false);
		boolean hP = hit(mx, my, cx + 18, cy, SPIN_W, SPIN_H);
		boolean hM = hit(mx, my, cx + 18, cy + 8, SPIN_W, SPIN_H);
		if (hP) g.fill(cx + 18, cy, cx + 28, cy + 8, 0xFF333333);
		if (hM) g.fill(cx + 18, cy + 8, cx + 28, cy + 16, 0xFF333333);

		g.drawCenteredString(font, "+", cx + 23, cy, hP ? C_HOVER_ACCENT : C_LABEL);
		g.drawCenteredString(font, "-", cx + 23, cy + 8, hM ? C_HOVER_ACCENT : C_LABEL);
	}

	public static void valSpinner(GuiGraphics g, Font font, int mx, int my, int cx, int cy) {
		boolean hP = hit(mx, my, cx, cy, SPIN_W, SPIN_H);
		boolean hM = hit(mx, my, cx, cy + 8, SPIN_W, SPIN_H);
		if (hP) g.fill(cx, cy, cx + 10, cy + 8, 0xFF333333);
		if (hM) g.fill(cx, cy + 8, cx + 10, cy + 16, 0xFF333333);

		g.drawCenteredString(font, "+", cx + 5, cy, hP ? C_HOVER_ACCENT : C_LABEL);
		g.drawCenteredString(font, "-", cx + 5, cy + 8, hM ? C_HOVER_ACCENT : C_LABEL);
	}

	public static void drawMiniSpinner(GuiGraphics g, Font font, int mx, int my, int cx, int cy) {
		boolean hP = hit(mx, my, cx, cy, MINI_SPIN, MINI_SPIN);
		boolean hM = hit(mx, my, cx, cy + 9, MINI_SPIN, MINI_SPIN);
		if (hP) g.fill(cx, cy, cx + 9, cy + 9, 0xFF333333);
		if (hM) g.fill(cx, cy + 9, cx + 9, cy + 18, 0xFF333333);

		g.drawCenteredString(font, "+", cx + 4, cy, hP ? C_HOVER_ACCENT : C_LABEL);
		g.drawCenteredString(font, "-", cx + 4, cy + 9, hM ? C_HOVER_ACCENT : C_LABEL);
	}

	// ─── Tab Bar Helpers ─────────────────────────────────────────────────────
	public static void drawTabs(GuiGraphics g, Font font, int x, int y, int totalW, int tabH, String[] labels, int selectedIndex, int mx, int my) {
		int count = labels.length;
		if (count == 0) return;
		for (int i = 0; i < count; i++) {
			int tx = x + (i * totalW) / count;
			int nextX = x + ((i + 1) * totalW) / count;
			int tw = nextX - tx;
			boolean sel = (i == selectedIndex);
			boolean hov = hit(mx, my, tx, y, tw, tabH);

			if (sel) {
				g.fill(tx, y, tx + tw, y + tabH, C_ACCENT_BG);
				g.fill(tx, y + tabH - 2, tx + tw, y + tabH, C_ACCENT);
			} else if (hov) {
				g.fill(tx, y, tx + tw, y + tabH, C_CARD_HOV);
			}
			g.drawCenteredString(font, labels[i], tx + tw / 2, y + (tabH - 8) / 2, sel || hov ? C_WHITE : C_LABEL);
		}
	}

	public static int getClickedTab(int x, int y, int totalW, int tabH, int count, int mx, int my) {
		if (count <= 0 || !hit(mx, my, x, y, totalW, tabH)) return -1;
		for (int i = 0; i < count; i++) {
			int tx = x + (i * totalW) / count;
			int nextX = x + ((i + 1) * totalW) / count;
			if (mx >= tx && mx < nextX) return i;
		}
		return -1;
	}

	public static void drawButton(GuiGraphics g, Font font, String label, int x, int y, int w, int h, boolean hover, int bg, int hbg) {
		g.fill(x, y, x + w, y + h, hover ? hbg : bg);
		g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hover ? 0xFFFFFFFF : C_TEXT);
	}

	public static void drawGhostButton(GuiGraphics g, Font font, String label, int x, int y, int w, int h, boolean hover, int hoverBg, int hoverText) {
		if (hover) {
			g.fill(x, y, x + w, y + h, hoverBg);
		}
		g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hover ? hoverText : C_LABEL);
	}

	public static void drawInputField(GuiGraphics g, Font font, String text, String placeholder, int cursor, boolean focused, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0xFF181818);
		if (focused) {
			drawOutline(g, x, y, w, h, C_ACCENT);
		} else {
			drawOutline(g, x, y, w, h, C_BORDER);
		}

		if (text.isEmpty() && !focused) {
			g.drawString(font, placeholder != null ? placeholder : "", x + 4, y + (h - 8) / 2, C_MUTED, false);
		} else {
			g.enableScissor(x + 2, y, x + w - 2, y + h);
			g.drawString(font, text, x + 4, y + (h - 8) / 2, C_TEXT, false);
			if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
				int curX = x + 4 + font.width(text.substring(0, Math.min(cursor, text.length())));
				g.fill(curX, y + 2, curX + 1, y + h - 2, C_ACCENT);
			}
			g.disableScissor();
		}
	}

	public static void copyToClipboard(String text) {
		Minecraft mc = Minecraft.getInstance();
		if (text != null) {
			mc.keyboardHandler.setClipboard(text);
		}
	}

	public static class TextFieldState {
		private String text = "";
		private int cursor = 0;
		private boolean focused = false;

		public TextFieldState(String text) { setText(text); }

		public String getText() { return text; }
		public void setText(String text) {
			this.text = text != null ? text : "";
			this.cursor = this.text.length();
		}

		public int getCursor() { return cursor; }
		public boolean isFocused() { return focused; }
		public void setFocused(boolean focused) {
			this.focused = focused;
			if (focused) this.cursor = text.length();
		}

		public boolean handleClick(int mx, int my, int x, int y, int w, int h) {
			if (hit(mx, my, x, y, w, h)) {
				setFocused(true);
				return true;
			}
			setFocused(false);
			return false;
		}

		public boolean handleKey(int key) {
			if (!focused) return false;
			if (key == 256) { focused = false; return true; }
			if (key == 259) {
				if (!text.isEmpty() && cursor > 0) {
					text = text.substring(0, cursor - 1) + text.substring(cursor);
					cursor--;
				}
				return true;
			}
			if (key == 261) {
				if (cursor < text.length()) {
					text = text.substring(0, cursor) + text.substring(cursor + 1);
				}
				return true;
			}
			if (key == 263) { cursor = Math.max(0, cursor - 1); return true; }
			if (key == 262) { cursor = Math.min(text.length(), cursor + 1); return true; }
			return false;
		}

		public boolean handleChar(char chr) {
			if (!focused) return false;
			if (chr >= 32 && chr != 127) {
				text = text.substring(0, cursor) + chr + text.substring(cursor);
				cursor++;
				return true;
			}
			return false;
		}

		public void render(GuiGraphics g, Font font, String placeholder, int x, int y, int w, int h) {
			drawInputField(g, font, text, placeholder, cursor, focused, x, y, w, h);
		}
	}
}