package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class DurabilityHudElement extends OffsetHudElement {

	public DurabilityHudElement() {
		super("durability", "Item Durability", "⚔",
				OpmConfig.ITEM_DURABILITY_IN_NAME, OpmConfig.ITEM_DURABILITY_SCALE,
				OpmConfig.ITEM_DURABILITY_X_OFFSET, OpmConfig.ITEM_DURABILITY_Y_OFFSET,
				Anchor.BOTTOM_CENTER, 2);
	}

	private String getText() {
		Minecraft mc = Minecraft.getInstance();
		ItemStack held = (mc.player != null) ? mc.player.getMainHandItem() : ItemStack.EMPTY;
		if (!held.isEmpty() && held.isDamageableItem()) {
			int cur = held.getMaxDamage() - held.getDamageValue();
			int max = held.getMaxDamage();
			return "[" + cur + "/" + max + "]";
		}
		return "[380/1561]";
	}

	@Override
	public int getW() {
		Minecraft mc = Minecraft.getInstance();
		return Math.max(20, (int) (mc.font.width(getText()) * scale));
	}

	@Override
	public int getH() {
		return Math.max(9, (int) (9 * scale));
	}

	@Override
	protected void renderContent(GuiGraphics g, Font font, int x, int y, int screenW, int screenH) {
		String durText = getText();
		Minecraft mc = Minecraft.getInstance();
		ItemStack held = (mc.player != null) ? mc.player.getMainHandItem() : ItemStack.EMPTY;
		int color = cz.maxtechnik.opm.client.ui.UiKit.C_SUCCESS_TEXT;
		if (!held.isEmpty() && held.isDamageableItem()) {
			int cur = held.getMaxDamage() - held.getDamageValue();
			int max = held.getMaxDamage();
			float f = (float) cur / max;
			color = f > 0.6f ? cz.maxtechnik.opm.client.ui.UiKit.C_SUCCESS_TEXT : (f > 0.3f ? 0xFFFFFF55 : cz.maxtechnik.opm.client.ui.UiKit.C_DANGER_TEXT);
		}

		int rawW = font.width(durText);
		g.fill(-2, -1, rawW + 2, 9, 0x55000000);
		g.drawString(font, durText, 0, 0, color, true);
	}
}
