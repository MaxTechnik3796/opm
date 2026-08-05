package cz.maxtechnik.opm.client.widget;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class UiKit {
	private UiKit() {}

	// Color Constants
	public static final int C_BORDER = 0xFF000000;
	public static final int C_TAB_SEL = 0xFF4A4A6A;
	public static final int C_SLOT = 0xFF3A3A3A;
	public static final int C_SLOT_HOV = 0xFF5A5A5A;
	public static final int C_SLOT_DR = 0xFF3A5A3A;
	public static final int C_SLOT_RES = 0xFF224422;
	public static final int C_INV = 0xFF141414;
	public static final int C_TEXT = 0xFFEEEEEE;
	public static final int C_LABEL = 0xFFAAAAAA;
	public static final int C_BTN = 0xFF383838;
	public static final int C_BTN_H = 0xFF585858;
	public static final int C_HOVER_ACCENT = 0xFF8888FF;

	// Dimensions
	public static final int SS = 18;
	public static final int SP = 2;
	public static final int SPIN_W = 10, SPIN_H = 8;
	public static final int MINI_SPIN = 9;

	public static boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
		return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
	}

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
}