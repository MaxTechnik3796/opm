package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Rozšíření BaseHudElement pro prvky, které mají relativní nebo absolutní X a Y offset.
 * Automaticky spravuje dragging, clamping, offset steppery v inspektoru a ukládání do configu.
 */
public abstract class OffsetHudElement extends BaseHudElement {

	public enum Anchor {
		CENTER,
		BOTTOM_CENTER,
		LEFT_RIGHT_TOP
	}

	protected final ModConfigSpec.IntValue configXOffset;
	protected final ModConfigSpec.IntValue configYOffset;
	protected final Anchor anchor;
	protected final int edgePad;

	protected int xOffset;
	protected int yOffset;

	public OffsetHudElement(String id, String title, String icon,
	                        ModConfigSpec.BooleanValue configEnabled,
	                        ModConfigSpec.DoubleValue configScale,
	                        ModConfigSpec.IntValue configXOffset,
	                        ModConfigSpec.IntValue configYOffset,
	                        Anchor anchor, int edgePad) {
		super(id, title, icon, configEnabled, configScale);
		this.configXOffset = configXOffset;
		this.configYOffset = configYOffset;
		this.anchor = anchor;
		this.edgePad = edgePad;
		this.xOffset = configXOffset != null ? configXOffset.get() : 0;
		this.yOffset = configYOffset != null ? configYOffset.get() : 0;
	}

	protected int getBaseX(int screenW) {
		return switch (anchor) {
			case CENTER, BOTTOM_CENTER -> (screenW - getW()) / 2;
			case LEFT_RIGHT_TOP -> isLeftAligned() ? edgePad : (screenW - edgePad - getW());
		};
	}

	protected int getBaseY(int screenH) {
		return switch (anchor) {
			case CENTER -> (screenH - getH()) / 2;
			case BOTTOM_CENTER -> getBottomCenterBaseY(screenH);
			case LEFT_RIGHT_TOP -> edgePad;
		};
	}

	protected int getBottomCenterBaseY(int screenH) {
		return screenH - 72;
	}

	protected boolean isLeftAligned() {
		return true;
	}

	@Override
	public int getX(int screenW, int screenH) {
		int w = getW();
		int base = getBaseX(screenW);
		return Math.clamp(base + xOffset, edgePad, screenW - w - edgePad);
	}

	@Override
	public int getY(int screenW, int screenH) {
		int h = getH();
		int base = getBaseY(screenH);
		return Math.clamp(base + yOffset, edgePad, screenH - h - edgePad);
	}

	@Override
	public void onDrag(int mx, int my, int grabX, int grabY, int screenW, int screenH) {
		int targetX = mx - grabX;
		int targetY = my - grabY;

		if (anchor == Anchor.LEFT_RIGHT_TOP) {
			handleSideDrag(mx, targetX, screenW);
		} else {
			int baseX = getBaseX(screenW);
			xOffset = targetX - baseX;
		}

		int baseY = getBaseY(screenH);
		yOffset = targetY - baseY;
		clamp(screenW, screenH);
	}

	protected void handleSideDrag(int mx, int targetX, int screenW) {
		int baseX = getBaseX(screenW);
		xOffset = targetX - baseX;
	}

	@Override
	public void clamp(int screenW, int screenH) {
		int w = getW(), h = getH();
		int baseX = getBaseX(screenW);
		int baseY = getBaseY(screenH);
		xOffset = Math.clamp(xOffset, edgePad - baseX, screenW - edgePad - w - baseX);
		yOffset = Math.clamp(yOffset, edgePad - baseY, screenH - edgePad - h - baseY);
	}

	@Override
	public void reset() {
		xOffset = 0;
		yOffset = 0;
		scale = 1.0;
	}

	@Override
	public void save() {
		super.save();
		if (configXOffset != null) configXOffset.set(xOffset);
		if (configYOffset != null) configYOffset.set(yOffset);
	}

	@Override
	protected int renderCustomInspectorOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		int curY = y;
		curY = renderExtraOptions(g, font, x, curY, w, mx, my);

		UiKit.drawStepper(g, font, "X Offset", String.valueOf(xOffset), x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		UiKit.drawStepper(g, font, "Y Offset", String.valueOf(yOffset), x, curY, w, mx, my);
		curY += UiKit.ITEM_H;

		return curY;
	}

	protected int renderExtraOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		return y;
	}

	@Override
	protected int getCustomInspectorHeight(int startY) {
		return getExtraOptionsHeight(startY) + 2 * UiKit.ITEM_H;
	}

	protected int getExtraOptionsHeight(int startY) {
		return startY;
	}

	@Override
	protected boolean handleCustomInspectorClick(int mx, int my, int x, int startY, int w) {
		int curY = startY;
		if (handleExtraOptionsClick(mx, my, x, curY, w)) return true;
		curY = getExtraOptionsHeight(curY);

		int xStep = UiKit.getStepperClick(mx, my, x, curY, w);
		if (xStep != 0) {
			xOffset += xStep * 5;
			return true;
		}
		curY += UiKit.ITEM_H;

		int yStep = UiKit.getStepperClick(mx, my, x, curY, w);
		if (yStep != 0) {
			yOffset += yStep * 5;
			return true;
		}

		return false;
	}

	protected boolean handleExtraOptionsClick(int mx, int my, int x, int startY, int w) {
		return false;
	}
}
