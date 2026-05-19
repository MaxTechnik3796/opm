package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EffectsHudOverlay implements LayeredDraw.Layer {

    private static final int ICON_SIZE    = 18;
    private static final int GAP          = 2;
    private static final int EDGE_PADDING = 4;
    private static final int WIDGET_WIDTH = 40;
    private static final int BG_NEGATIVE  = 0xAA8B0000;
    private static final int BG_NEUTRAL   = 0xAA000000;

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!OpmConfig.EFFECTS_HUD_ENABLED.get()) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) return;

        int screenWidth  = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        OpmConfig.HudLocation loc = OpmConfig.EFFECTS_HUD_LOCATION.get();
        boolean onRight = loc != OpmConfig.HudLocation.LEFT;

        double scale = OpmConfig.EFFECTS_HUD_SCALE.get();
        int effW = (int) (WIDGET_WIDTH * scale);
        int effWidgetH = (int) ((ICON_SIZE + GAP) * scale);

        int startY = EDGE_PADDING + OpmConfig.EFFECTS_HUD_Y_OFFSET.get();
        int startX;
        if (loc == OpmConfig.HudLocation.RIGHT) {
            startX = screenWidth - EDGE_PADDING - effW + OpmConfig.EFFECTS_HUD_X_OFFSET.get();
        } else {
            startX = EDGE_PADDING + OpmConfig.EFFECTS_HUD_X_OFFSET.get();
        }

        int maxY = screenHeight - EDGE_PADDING;
        int availableHeight = maxY - startY;
        int maxVisible = availableHeight / effWidgetH;

        if (maxVisible <= 0) return;

        List<MobEffectInstance> effectList = new ArrayList<>(effects);

        int hiddenCount   = Math.max(0, effectList.size() - maxVisible);
        int actualVisible = hiddenCount > 0 ? maxVisible - 1 : maxVisible;
        actualVisible = Math.min(actualVisible, effectList.size());

        MobEffectTextureManager textureManager = mc.getMobEffectTextures();
        int currentY = startY;

        var pose = graphics.pose();
        pose.pushPose();
        if (scale != 1.0) {
            pose.scale((float) scale, (float) scale, 1.0f);
        }

        for (int i = 0; i < actualVisible; i++) {
            int rx = startX;
            int ry = currentY;
            if (scale != 1.0) {
                rx = (int) (rx / scale);
                ry = (int) (ry / scale);
            }
            renderEffectWidget(graphics, mc, textureManager,
                    effectList.get(i), rx, ry, onRight);
            currentY += effWidgetH;
        }

        if (hiddenCount > 0) {
            boolean hasNegative = false;
            for (int i = actualVisible; i < effectList.size(); i++) {
                if (effectList.get(i).getEffect().value().getCategory()
                        == MobEffectCategory.HARMFUL) {
                    hasNegative = true;
                    break;
                }
            }
            int rx = startX;
            int ry = currentY;
            if (scale != 1.0) {
                rx = (int) (rx / scale);
                ry = (int) (ry / scale);
            }
            renderPlusWidget(graphics, mc, "+" + hiddenCount, rx, ry, hasNegative);
        }

        pose.popPose();
    }

    private void renderEffectWidget(GuiGraphics graphics, Minecraft mc,
                                    MobEffectTextureManager textureManager,
                                    MobEffectInstance instance,
                                    int x, int y, boolean onRight) {
        Holder<MobEffect> effectHolder = instance.getEffect();
        boolean isHarmful = effectHolder.value().getCategory() == MobEffectCategory.HARMFUL;
        int bgColor = isHarmful ? BG_NEGATIVE : BG_NEUTRAL;

        // Pozadí
        graphics.fill(x, y, x + WIDGET_WIDTH, y + ICON_SIZE, bgColor);

        // Ikona
        TextureAtlasSprite sprite = textureManager.get(effectHolder);
        int iconX = onRight ? x + 1 : x + WIDGET_WIDTH - ICON_SIZE - 1;

        // Vykreslení ikony pomocí sprite přímo
        graphics.blit(iconX, y, 0, ICON_SIZE, ICON_SIZE, sprite);

        // Text — zarovnaný k okraji
        String durationText = formatDuration(instance.getDuration());
        int amplifier = instance.getAmplifier() + 1;

        if (onRight) {
            // Pravá strana: Ikona vlevo, text vpravo (zarovnaný doprava)
            if (amplifier > 1) {
                String ampText = String.valueOf(amplifier);
                graphics.drawString(mc.font, ampText, x + WIDGET_WIDTH - mc.font.width(ampText) - 2, y + 1, 0xFFFFFF, false);
                graphics.drawString(mc.font, durationText, x + WIDGET_WIDTH - mc.font.width(durationText) - 2, y + 10, 0xAAAAAA, false);
            } else {
                graphics.drawString(mc.font, durationText, x + WIDGET_WIDTH - mc.font.width(durationText) - 2, y + 5, 0xAAAAAA, false);
            }
        } else {
            // Levá strana: Text vlevo (zarovnaný doleva), ikona vpravo
            if (amplifier > 1) {
                graphics.drawString(mc.font, String.valueOf(amplifier), x + 2, y + 1, 0xFFFFFF, false);
                graphics.drawString(mc.font, durationText, x + 2, y + 10, 0xAAAAAA, false);
            } else {
                graphics.drawString(mc.font, durationText, x + 2, y + 5, 0xAAAAAA, false);
            }
        }
    }

    private void renderPlusWidget(GuiGraphics graphics, Minecraft mc,
                                  String text, int x, int y, boolean negative) {
        int halfHeight = ICON_SIZE / 2;
        int bgColor = negative ? BG_NEGATIVE : BG_NEUTRAL;
        graphics.fill(x, y, x + WIDGET_WIDTH, y + halfHeight, bgColor);
        int textX = x + WIDGET_WIDTH / 2 - mc.font.width(text) / 2;
        int textY = y + halfHeight / 2 - 4;
        graphics.drawString(mc.font, text, textX, textY, 0xFFFFFF, false);
    }

    private String formatDuration(int ticks) {
        if (ticks <= 0 || ticks == Integer.MAX_VALUE) return "∞";
        int seconds = ticks / 20;
        if (seconds < 60)  return seconds + "s";
        int minutes = seconds / 60;
        if (minutes < 60)  return minutes + "m";
        int hours = minutes / 60;
        if (hours < 24)    return hours + "h";
        return (hours / 24) + "d";
    }
}