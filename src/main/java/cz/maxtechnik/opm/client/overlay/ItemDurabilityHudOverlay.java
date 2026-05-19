package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemDurabilityHudOverlay implements LayeredDraw.Layer {

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!OpmConfig.ITEM_DURABILITY_IN_NAME.get()) return;

        Player player = mc.player;
        ItemStack stack = player.getMainHandItem();

        // Zobraz pouze pro damageable itemy které jsou poškozené nebo config chce vždy
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        int current = stack.getMaxDamage() - stack.getDamageValue();
        int max     = stack.getMaxDamage();

        // Formát: [current/max]
        String durText = "[" + current + "/" + max + "]";

        // Barva podle procenta durability
        float fraction = (float) current / max;
        int color;
        if (fraction > 0.6f) {
            color = 0xFFAAFFAA; // zelená
        } else if (fraction > 0.3f) {
            color = 0xFFFFFF55; // žlutá
        } else {
            color = 0xFFFF5555; // červená
        }

        // Pozice — pod názvem itemu v ruce
        // Vanilla název itemu se zobrazuje uprostřed nad hotbarem
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        // Název itemu je na y = screenH - 59 (nad hotbarem)
        // Durabilita bude pod názvem
        int textW = mc.font.width(durText);
        int x     = (screenW - textW) / 2 + OpmConfig.ITEM_DURABILITY_X_OFFSET.get();
        int y     = screenH - 72 + OpmConfig.ITEM_DURABILITY_Y_OFFSET.get();

        // Poloprůhledné pozadí
        graphics.fill(x - 2, y - 1, x + textW + 2, y + 9, 0x55000000);
        graphics.drawString(mc.font, durText, x, y, color, true);
    }
}