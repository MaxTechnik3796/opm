package cz.maxtechnik.opm.client.screen.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Znovupoužitelný preset pro libovolný textový HUD prvek (např. Title, Actionbar, FPS, souřadnice atd.).
 * Automaticky obsluhuje výpočet šířky/výšky z textu, měřítko písma, stínování a posun.
 */
public class SimpleTextHudElement extends OffsetHudElement {

	private final String text;
	private final float textScale;
	private final int color;

	public SimpleTextHudElement(String id, String title, String icon,
	                            ModConfigSpec.BooleanValue configEnabled,
	                            ModConfigSpec.DoubleValue configScale,
	                            ModConfigSpec.IntValue configXOffset,
	                            ModConfigSpec.IntValue configYOffset,
	                            Anchor anchor, int edgePad,
	                            String text, float textScale, int color,
	                            double minScale, double maxScale) {
		super(id, title, icon, configEnabled, configScale, configXOffset, configYOffset, anchor, edgePad);
		this.text = text;
		this.textScale = textScale;
		this.color = color;
		this.minScale = minScale;
		this.maxScale = maxScale;
	}

	public SimpleTextHudElement(String id, String title, String icon,
	                            ModConfigSpec.BooleanValue configEnabled,
	                            ModConfigSpec.DoubleValue configScale,
	                            ModConfigSpec.IntValue configXOffset,
	                            ModConfigSpec.IntValue configYOffset,
	                            Anchor anchor, int edgePad,
	                            String text, float textScale) {
		this(id, title, icon, configEnabled, configScale, configXOffset, configYOffset, anchor, edgePad, text, textScale, 0xFFFFFFFF, 0.5, 2.0);
	}

	@Override
	public int getW() {
		Minecraft mc = Minecraft.getInstance();
		return Math.max(20, (int) (mc.font.width(text) * textScale * scale));
	}

	@Override
	public int getH() {
		return Math.max(9, (int) (9 * textScale * scale));
	}

	@Override
	protected void renderContent(GuiGraphics g, Font font, int x, int y, int screenW, int screenH) {
		var pose = g.pose();
		pose.pushPose();
		if (textScale != 1.0f) {
			pose.scale(textScale, textScale, 1f);
		}
		g.drawString(font, text, 0, 0, color, true);
		pose.popPose();
	}
}
