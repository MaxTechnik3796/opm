package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.LayeredDraw;
import org.jetbrains.annotations.NotNull;

public class ArmorHudOverlay implements LayeredDraw.Layer {

    private static final int SLOT_SIZE = 16;
    private static final int GAP = 4;

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!OpmConfig.ARMOR_HUD_ENABLED.get()) return;

        Player player = mc.player;

        // Pořadí armorů — konfigurovatelné
        EquipmentSlot[] slots;
        if (OpmConfig.ARMOR_HUD_INVERTED.get()) {
            slots = new EquipmentSlot[]{
                    EquipmentSlot.FEET, EquipmentSlot.LEGS,
                    EquipmentSlot.CHEST, EquipmentSlot.HEAD
            };
        } else {
            slots = new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET
            };
        }

        // Zkontroluj jestli má vůbec nějaký armor
        boolean hasArmor = false;
        for (EquipmentSlot slot : slots) {
            if (!player.getItemBySlot(slot).isEmpty()) { hasArmor = true; break; }
        }
        if (!hasArmor) return;

        int screenWidth  = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        // Hotbar — vanilla je 182px centrovaný
        int hotbarWidth = 182;
        int hotbarX = (screenWidth - hotbarWidth) / 2;
        
        // Posunuto o 2px výš oproti hotbaru (původně -19)
        int itemY = screenHeight - 22;

        net.minecraft.world.entity.HumanoidArm mainArm = player.getMainArm();
        boolean hasOffhand = !player.getOffhandItem().isEmpty();
        boolean offhandOnLeft = mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT;
        boolean offhandOnRight = !offhandOnLeft;
        
        int offhandWidth = 29;

        // Spočítej šířku armor HUDu
        int armorCount = 0;
        for (EquipmentSlot slot : slots) {
            if (!player.getItemBySlot(slot).isEmpty()) armorCount++;
        }
        int totalArmorWidth = armorCount * SLOT_SIZE + (armorCount - 1) * GAP;

        int startX;

        if (OpmConfig.ARMOR_HUD_LOCATION.get() == OpmConfig.HudLocation.LEFT) {
            // Armor vlevo
            startX = hotbarX - GAP - totalArmorWidth;
            // Pokud je offhand vlevo, posuneme ho ještě víc doleva
            if (hasOffhand && offhandOnLeft) {
                startX -= offhandWidth;
            }
        } else {
            // Armor vpravo od hotbaru (default)
            startX = hotbarX + hotbarWidth + GAP;
            // Pokud je offhand vpravo, posuneme ho ještě víc doprava
            if (hasOffhand && offhandOnRight) {
                startX += offhandWidth;
            }
        }

        // Vykresli
        int currentX = startX;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            // Renderuje pouze ikonu (bez renderItemDecorations, abychom neměli vanilla damage bar a čísla)
            graphics.renderItem(stack, currentX, itemY);

            // Vlastní durabilita bar pod itemem
            if (stack.isDamageableItem() && stack.isDamaged()) {
                float fraction = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
                int barWidth = Math.round(fraction * 13);
                int barX = currentX + 2;
                int barY = itemY + 18; // Kousek pod itemem
                
                // Pozadí (černé)
                graphics.fill(barX - 1, barY - 1, barX + 14, barY + 2, 0xFF000000);
                // Barva (zelená -> červená)
                graphics.fill(barX, barY, barX + barWidth, barY + 1, getDurabilityColor(fraction));
            }

            currentX += SLOT_SIZE + GAP;
        }
    }

    private int getDurabilityColor(float fraction) {
        int r = Math.round(255 * (1.0f - fraction));
        int g = Math.round(255 * fraction);
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}