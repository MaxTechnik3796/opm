package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.LayeredDraw;
import org.jetbrains.annotations.NotNull;


public class ArmorHudOverlay implements LayeredDraw.Layer {

    private static final int SLOT_SIZE   = 16;
    private static final int GAP         = 4;
    private static final int EDGE_PAD    = 2;
    private static final int OFFHAND_W   = 29;
    private static final int DUR_BAR_H   = 1;
    private static final int DUR_BAR_PAD = 1;

    // ── slot order helpers ────────────────────────────────────────────────────

    /** Returns the 4 armour slots in canonical order (helmet first, boots last). */
    private static final EquipmentSlot[] CANONICAL =
            { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private EquipmentSlot[] buildOrder(boolean inverted) {
        if (!inverted) return CANONICAL;
        return new EquipmentSlot[]{
                EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD
        };
    }

    // ── main render ───────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!OpmConfig.ARMOR_HUD_ENABLED.get()) return;

        Player player    = mc.player;
        int screenWidth  = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        int rotate   = OpmConfig.ARMOR_HUD_ROTATE.get();
        boolean inv  = OpmConfig.ARMOR_HUD_INVERTED.get();
        boolean locked = OpmConfig.ARMOR_HUD_LOCKED.get();

        // Build slot order.
        // rotate 0/2 (horizontal): inverted swaps helmet↔boots direction;
        // rotate 1/3 (vertical):   same.
        // The "inverted" flag flips the array.
        EquipmentSlot[] slots = buildOrder(inv);

        // For rotate 2 (H right-to-left) and 3 (V bottom-to-top) we additionally
        // reverse the array so that helmet ends up on the "far" side.
        if (rotate == 2 || rotate == 3) {
            // reverse in place
            for (int i = 0, j = slots.length - 1; i < j; i++, j--) {
                EquipmentSlot tmp = slots[i]; slots[i] = slots[j]; slots[j] = tmp;
            }
        }

        // Count non-empty slots
        int count = 0;
        for (EquipmentSlot s : slots) if (!player.getItemBySlot(s).isEmpty()) count++;
        if (count == 0) return;

        boolean horizontal = (rotate == 0 || rotate == 2);

        // Total pixel span of armour strip (without extra bar space)
        int totalSpan = count * SLOT_SIZE + (count - 1) * GAP;

        // ── Determine anchor position ──────────────────────────────────────────
        int startX, startY;

        if (locked) {
            // Anchored to hotbar
            int hotbarWidth = 182;
            int hotbarX     = (screenWidth - hotbarWidth) / 2;
            int itemY       = screenHeight - 22;

            boolean hasOffhand   = !player.getOffhandItem().isEmpty();
            boolean offhandLeft  = (player.getMainArm() == HumanoidArm.RIGHT);
            boolean offhandRight = !offhandLeft;

            if (horizontal) {
                // Place horizontally beside hotbar on left or right side
                if (OpmConfig.ARMOR_HUD_LOCATION.get() == OpmConfig.HudLocation.LEFT) {
                    startX = hotbarX - GAP - totalSpan;
                    if (hasOffhand && offhandLeft) startX -= OFFHAND_W;
                } else {
                    startX = hotbarX + hotbarWidth + GAP;
                    if (hasOffhand && offhandRight) startX += OFFHAND_W;
                }
                startY = itemY;
            } else {
                // Place vertically beside hotbar on left or right side
                // Vertically, the strip goes upward from hotbar level
                int vertTotal = count * SLOT_SIZE + (count - 1) * GAP;
                if (OpmConfig.ARMOR_HUD_LOCATION.get() == OpmConfig.HudLocation.LEFT) {
                    startX = hotbarX - GAP - SLOT_SIZE;
                    if (hasOffhand && offhandLeft) startX -= OFFHAND_W;
                } else {
                    startX = hotbarX + hotbarWidth + GAP;
                    if (hasOffhand && offhandRight) startX += OFFHAND_W;
                }
                // rotate 1: top slot (helmet) is highest → start at top
                // rotate 3: bottom slot (helmet) is lowest → start at bottom of strip
                if (rotate == 3) {
                    startY = itemY; // helmet will be at bottom (drawn first in reversed order)
                } else {
                    startY = itemY - vertTotal + SLOT_SIZE;
                }
            }
        } else {
            // Free position
            startX = OpmConfig.ARMOR_HUD_FREE_X.get();
            startY = OpmConfig.ARMOR_HUD_FREE_Y.get();
        }

        // ── Clamp to screen (at least EDGE_PAD from every edge) ───────────────
        int hudW = horizontal ? totalSpan : SLOT_SIZE;
        int hudH = horizontal ? (SLOT_SIZE + DUR_BAR_PAD + DUR_BAR_H + 2) : totalSpan;

        startX = Math.clamp(startX, EDGE_PAD, screenWidth  - hudW - EDGE_PAD);
        startY = Math.clamp(startY, EDGE_PAD, screenHeight - hudH - EDGE_PAD);

        // ── Render ────────────────────────────────────────────────────────────
        int curX = startX, curY = startY;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            graphics.renderItem(stack, curX, curY);

            // Durability bar below / beside item
            if (stack.isDamageableItem() && stack.isDamaged()) {
                float fraction = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
                int barWidth   = Math.round(fraction * 13);
                int barX = curX + 2;
                int barY = curY + SLOT_SIZE + DUR_BAR_PAD;
                graphics.fill(barX - 1, barY - 1, barX + 14, barY + DUR_BAR_H + 1, 0xFF000000);
                graphics.fill(barX, barY, barX + barWidth, barY + DUR_BAR_H, getDurabilityColor(fraction));
            }

            if (horizontal) {
                curX += SLOT_SIZE + GAP;
            } else {
                curY += SLOT_SIZE + GAP;
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int getDurabilityColor(float fraction) {
        int r = Math.round(255 * (1.0f - fraction));
        int g = Math.round(255 * fraction);
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}