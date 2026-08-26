package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.ui.UiKit;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class EffectsHudElement extends OffsetHudElement {

	private OpmConfig.HudLocation location;

	public EffectsHudElement() {
		super("effects", "Effects HUD", "🧪",
				OpmConfig.EFFECTS_HUD_ENABLED, OpmConfig.EFFECTS_HUD_SCALE,
				OpmConfig.EFFECTS_HUD_X_OFFSET, OpmConfig.EFFECTS_HUD_Y_OFFSET,
				Anchor.LEFT_RIGHT_TOP, 4);
		this.location = OpmConfig.EFFECTS_HUD_LOCATION.get();
	}

	@Override
	protected boolean isLeftAligned() {
		return location == OpmConfig.HudLocation.LEFT;
	}

	@Override
	protected void handleSideDrag(int mx, int targetX, int screenW) {
		if (mx < screenW / 2) {
			location = OpmConfig.HudLocation.LEFT;
			xOffset = targetX - edgePad;
		} else {
			location = OpmConfig.HudLocation.RIGHT;
			xOffset = targetX - (screenW - edgePad - getW());
		}
	}

	@Override
	public int getW() {
		return Math.max(24, (int) (48 * scale));
	}

	@Override
	public int getH() {
		return Math.max(20, (int) (20 * 2 * scale));
	}

	@Override
	public void reset() {
		super.reset();
		location = OpmConfig.HudLocation.LEFT;
	}

	@Override
	public void save() {
		super.save();
		OpmConfig.EFFECTS_HUD_LOCATION.set(location);
	}

	@Override
	protected int renderExtraOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		cz.maxtechnik.opm.client.ui.UiKit.drawEnumCycler(g, font, "Side", location.name(), x, y, w, mx, my);
		return y + cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
	}

	@Override
	protected int getExtraOptionsHeight(int startY) {
		return startY + cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
	}

	@Override
	protected boolean handleExtraOptionsClick(int mx, int my, int x, int startY, int w) {
		if (cz.maxtechnik.opm.client.ui.UiKit.isEnumHit(mx, my, x, startY, w)) {
			location = (location == OpmConfig.HudLocation.LEFT) ? OpmConfig.HudLocation.RIGHT : OpmConfig.HudLocation.LEFT;
			return true;
		}
		return false;
	}

	@Override
	protected void renderContent(GuiGraphics g, Font font, int x, int y, int screenW, int screenH) {
		Minecraft mc = Minecraft.getInstance();
		MobEffectTextureManager tm = mc.getMobEffectTextures();
		List<MobEffectInstance> list = new ArrayList<>();
		if (mc.player != null && !mc.player.getActiveEffects().isEmpty()) {
			list.addAll(mc.player.getActiveEffects());
		}

		if (list.isEmpty()) {
			list.add(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 1800, 1));
			list.add(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 300, 0));
		}

		int singleH = 20;
		boolean onRight = location != OpmConfig.HudLocation.LEFT;

		for (int i = 0; i < Math.min(2, list.size()); i++) {
			renderEffectWidget(g, mc, tm, list.get(i), i * singleH, onRight);
		}
	}

	private void renderEffectWidget(GuiGraphics g, Minecraft mc, MobEffectTextureManager tm, MobEffectInstance inst, int y, boolean onRight) {
		Holder<MobEffect> eh = inst.getEffect();
		boolean harmful = eh.value().getCategory() == MobEffectCategory.HARMFUL;
		int W = 48;
		int accentColor = harmful ? UiKit.C_DANGER : UiKit.C_ACCENT_BG;
		int textColor = harmful ? UiKit.C_DANGER_TEXT : UiKit.C_ACCENT_HOV;
		int iconX = onRight ? 3 : W - 18 - 3;

		g.fill(0, y, W, y + 18, harmful ? 0xAA450000 : 0xAA000000);
		if (onRight) g.fill(0, y, 2, y + 18, accentColor);
		else g.fill(W - 2, y, W, y + 18, accentColor);

		g.blit(iconX, y, 0, 18, 18, tm.get(eh));

		int amp = inst.getAmplifier() + 1;
		int textX = onRight ? W - mc.font.width(formatDuration(inst.getDuration())) - 3 : 3;

		if (amp > 1) {
			String ampText = String.valueOf(amp);
			int ampX = onRight ? W - mc.font.width(ampText) - 3 : 3;
			g.drawString(mc.font, ampText, ampX, y + 1, textColor, false);
		}

		String dur = formatDuration(inst.getDuration());
		g.drawString(mc.font, dur, textX, y + 18 - 8, textColor, false);
	}

	private String formatDuration(int ticks) {
		if (ticks <= 0 || ticks == Integer.MAX_VALUE) return "∞";
		int sec = ticks / 20;
		if (sec < 60) return sec + "s";
		int min = sec / 60;
		return min < 60 ? min + "m" : (min / 60) + "h";
	}
}
