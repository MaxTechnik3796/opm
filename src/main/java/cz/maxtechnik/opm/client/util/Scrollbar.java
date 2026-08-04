package cz.maxtechnik.opm.client.util;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Samostatná a znovupoužitelná stavová třída pro skrolování (Scrollbar).
 * Obsluhuje renderování, hit-testing, dragování i kolečko myši s ořezáváním hranic.
 */
public final class Scrollbar {
	public static final int SB_W = 4;
	public static final int C_SB_BG = 0xFF111111;
	public static final int C_SB_THUMB = 0xFF666666;

	public float scroll;
	public int x, y, h;
	public int viewportH;
	public int max;
	public boolean dragging;
	private float dragOffset;

	public void update(int viewportH, int contentH) {
		this.viewportH = viewportH;
		this.max = Math.max(0, contentH - viewportH);
		if (scroll > max) scroll = max;
		if (scroll < 0) scroll = 0;
	}

	public void render(GuiGraphics g, int sbX, int sbY) {
		this.x = sbX;
		this.y = sbY;
		this.h = viewportH;
		if (max <= 0) return;
		g.fill(sbX, sbY, sbX + SB_W, sbY + viewportH, C_SB_BG);
		int th = Math.max(20, viewportH * viewportH / (viewportH + max));
		int ty = sbY + (int) ((viewportH - th) * (scroll / (float) max));
		g.fill(sbX, ty, sbX + SB_W, ty + th, C_SB_THUMB);
	}

	public boolean hitTrack(int mx, int my) {
		return max > 0 && mx >= x && mx <= x + SB_W && my >= y && my <= y + h;
	}

	public boolean startDragIfHit(int mx, int my) {
		if (hitTrack(mx, my)) {
			dragging = true;
			int th = Math.max(20, viewportH * viewportH / (viewportH + max));
			int ty = y + (int) ((viewportH - th) * (scroll / (float) max));
			if (my >= ty && my <= ty + th) {
				dragOffset = my - ty;
			} else {
				dragOffset = th / 2f;
				dragTo(my);
			}
			return true;
		}
		return false;
	}

	public void dragTo(int my) {
		if (!dragging || max <= 0 || h <= 0) return;
		int th = Math.max(20, h * h / (h + max));
		int trackH = h - th;
		if (trackH <= 0) {
			scroll = 0;
			return;
		}
		float targetTy = my - dragOffset;
		float t = (targetTy - y) / (float) trackH;
		scroll = Math.clamp(t * max, 0, max);
	}

	public void stopDrag() {
		dragging = false;
	}

	public void handleScroll(double deltaY, int stepPx) {
		if (max <= 0) {
			scroll = 0;
			return;
		}
		scroll = (float) Math.clamp(scroll - deltaY * stepPx, 0, max);
	}

	public boolean mouseClicked(int mx, int my, int button) {
		if (button == 0) {
			return startDragIfHit(mx, my);
		}
		return false;
	}

	public boolean mouseDragged(int my) {
		if (dragging) {
			dragTo(my);
			return true;
		}
		return false;
	}
	public void mouseReleased() {
		stopDrag();
	}
}
