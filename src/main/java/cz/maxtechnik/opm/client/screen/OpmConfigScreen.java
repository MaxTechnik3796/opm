package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OpmConfigScreen extends Screen {

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int BG        = 0xF0222222, HEADER_BG = 0xFF1A1A1A, FOOTER_BG = 0xFF1A1A1A;
    private static final int BORDER    = 0xFF000000, DIVIDER   = 0xFF000000;
    private static final int TEXT      = 0xFFDDDDDD, LABEL_COL = 0xFF888888, CAT_COL   = 0xFF55AAFF;
    private static final int HOV_ROW   = 0x15FFFFFF;
    private static final int BTN_OFF   = 0xFF383838, BTN_OFF_H = 0xFF585858;
    private static final int BTN_ON    = 0xFF1E4A1E, BTN_ON_H  = 0xFF2A6A2A;
    private static final int BTN_ENUM  = 0xFF333355, BTN_ENUM_H = 0xFF4444AA;
    private static final int EDGE_PAD  = 2;

    // ── State ────────────────────────────────────────────────────────────────
    private final Screen parent;
    private boolean noRecipeBook, noRealmsButton, customDebugScreen, hideTutorialToast;
    private OpmConfig.PumpkinMode pumpkinOverlay;
    private boolean durabilityEnabled;
    private int durabilityXOffset, durabilityYOffset;
    private boolean armorEnabled, armorInverted, armorLocked;
    private OpmConfig.HudLocation armorLocation;
    private int armorRotate, armorFreeX, armorFreeY;
    private boolean effectsEnabled;
    private OpmConfig.HudLocation effectsLocation;
    private double effectsScale;
    private int effectsXOffset, effectsYOffset;

    // ── Panel layout ─────────────────────────────────────────────────────────
    private int pX, pY, pW, pH, hdrH, ftrH;
    private boolean panelHidden = false;

    // ── Options list ─────────────────────────────────────────────────────────
    private final List<ConfigItem> configItems = new ArrayList<>();
    private float scroll = 0;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;
    private static final int ITEM_H = 22;

    // ── Drag ─────────────────────────────────────────────────────────────────
    private enum Drag { NONE, DURABILITY, EFFECTS, ARMOR }
    private Drag drag = Drag.NONE;
    private int dragGrabX, dragGrabY;

    // ── Armor ────────────────────────────────────────────────────────────────
    private static final EquipmentSlot[] CANONICAL =
            { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final int SLOT_SIZE = 16, GAP = 4, OFFHAND_W = 29;
    private static final int EYE_BTN_W = 18;
    private static final ItemStack[] MOCK_ARMOR = {
            new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
            new ItemStack(Items.IRON_LEGGINGS), new ItemStack(Items.IRON_BOOTS)
    };

    // ── Constructor ──────────────────────────────────────────────────────────
    public OpmConfigScreen(Screen parent) {
        super(Component.literal("OPM Config"));
        this.parent = parent;
        noRecipeBook      = OpmConfig.NO_RECIPE_BOOK.get();
        noRealmsButton    = OpmConfig.NO_REALMS_BUTTON.get();
        customDebugScreen = OpmConfig.CUSTOM_DEBUG_SCREEN.get();
        hideTutorialToast = OpmConfig.HIDE_TUTORIAL_TOAST.get();
        pumpkinOverlay    = OpmConfig.PUMPKIN_OVERLAY.get();
        durabilityEnabled = OpmConfig.ITEM_DURABILITY_IN_NAME.get();
        durabilityXOffset = OpmConfig.ITEM_DURABILITY_X_OFFSET.get();
        durabilityYOffset = OpmConfig.ITEM_DURABILITY_Y_OFFSET.get();
        armorEnabled  = OpmConfig.ARMOR_HUD_ENABLED.get();
        armorInverted = OpmConfig.ARMOR_HUD_INVERTED.get();
        armorLocation = OpmConfig.ARMOR_HUD_LOCATION.get();
        armorRotate   = OpmConfig.ARMOR_HUD_ROTATE.get();
        armorLocked   = OpmConfig.ARMOR_HUD_LOCKED.get();
        armorFreeX    = OpmConfig.ARMOR_HUD_FREE_X.get();
        armorFreeY    = OpmConfig.ARMOR_HUD_FREE_Y.get();
        effectsEnabled  = OpmConfig.EFFECTS_HUD_ENABLED.get();
        effectsLocation = OpmConfig.EFFECTS_HUD_LOCATION.get();
        effectsScale    = OpmConfig.EFFECTS_HUD_SCALE.get();
        effectsXOffset  = OpmConfig.EFFECTS_HUD_X_OFFSET.get();
        effectsYOffset  = OpmConfig.EFFECTS_HUD_Y_OFFSET.get();
        buildItemList();
    }

    private void buildItemList() {
        configItems.clear();
        configItems.add(new CategoryItem("UI Options"));
        configItems.add(new BooleanItem("Hide Recipe Book",    () -> noRecipeBook,      v -> noRecipeBook      = v));
        configItems.add(new BooleanItem("Hide Realms Button",  () -> noRealmsButton,    v -> noRealmsButton    = v));
        configItems.add(new BooleanItem("Custom Debug Screen", () -> customDebugScreen, v -> customDebugScreen = v));
        configItems.add(new BooleanItem("Hide Tutorial Toast", () -> hideTutorialToast, v -> hideTutorialToast = v));
        configItems.add(new WideEnumItem<>("Pumpkin Overlay", () -> pumpkinOverlay, OpmConfig.PumpkinMode.values(), v -> pumpkinOverlay = v));

        configItems.add(new CategoryItem("Durability HUD"));
        configItems.add(new BooleanItem("Enabled", () -> durabilityEnabled, v -> durabilityEnabled = v));
        configItems.add(new IntItem("X Offset", () -> durabilityXOffset, -10000, 10000, 1, v -> { durabilityXOffset = v; clampOffsets(); }));
        configItems.add(new IntItem("Y Offset", () -> durabilityYOffset, -10000, 10000, 1, v -> { durabilityYOffset = v; clampOffsets(); }));

        configItems.add(new CategoryItem("Armor HUD"));
        configItems.add(new BooleanItem("Enabled",        () -> armorEnabled,  v -> armorEnabled  = v));
        configItems.add(new EnumItem<>("Side (locked)",   () -> armorLocation, OpmConfig.HudLocation.values(), v -> armorLocation = v));
        configItems.add(new CycleArrowItem("Rotate", () -> armorRotate, 0, 3, v -> armorRotate = v));
        configItems.add(new BooleanItem("Locked to Hotbar", () -> armorLocked, v -> armorLocked = v));

        configItems.add(new CategoryItem("Effects HUD"));
        configItems.add(new BooleanItem("Enabled", () -> effectsEnabled, v -> effectsEnabled = v));
        configItems.add(new EnumItem<>("Side", () -> effectsLocation, OpmConfig.HudLocation.values(), v -> effectsLocation = v));
        configItems.add(new IntItem("X Offset", () -> effectsXOffset, -10000, 10000, 1, v -> { effectsXOffset = v; clampOffsets(); }));
        configItems.add(new IntItem("Y Offset", () -> effectsYOffset, -10000, 10000, 1, v -> { effectsYOffset = v; clampOffsets(); }));
        configItems.add(new DoubleItem("Scale", () -> effectsScale, 1.0, 2.0, 0.05, v -> effectsScale = v));
    }

    // ── Init / lifecycle ──────────────────────────────────────────────────────
    @Override protected void init() {
        super.init();
        pW = Math.min(228, width - 40); pH = height - 40;
        pX = (width - pW) / 2; pY = 20;
        hdrH = 28; ftrH = 28;
        clampOffsets();
    }

    @Override public void tick() { super.tick(); clampOffsets(); }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        saveAll();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    // ── Save ─────────────────────────────────────────────────────────────────
    private void saveAll() {
        clampOffsets();
        OpmConfig.NO_RECIPE_BOOK.set(noRecipeBook);
        OpmConfig.NO_REALMS_BUTTON.set(noRealmsButton);
        OpmConfig.CUSTOM_DEBUG_SCREEN.set(customDebugScreen);
        OpmConfig.HIDE_TUTORIAL_TOAST.set(hideTutorialToast);
        OpmConfig.PUMPKIN_OVERLAY.set(pumpkinOverlay);
        OpmConfig.ITEM_DURABILITY_IN_NAME.set(durabilityEnabled);
        OpmConfig.ITEM_DURABILITY_X_OFFSET.set(durabilityXOffset);
        OpmConfig.ITEM_DURABILITY_Y_OFFSET.set(durabilityYOffset);
        OpmConfig.ARMOR_HUD_ENABLED.set(armorEnabled);
        OpmConfig.ARMOR_HUD_INVERTED.set(armorInverted);
        OpmConfig.ARMOR_HUD_LOCATION.set(armorLocation);
        OpmConfig.ARMOR_HUD_ROTATE.set(armorRotate);
        OpmConfig.ARMOR_HUD_LOCKED.set(armorLocked);
        OpmConfig.ARMOR_HUD_FREE_X.set(armorFreeX);
        OpmConfig.ARMOR_HUD_FREE_Y.set(armorFreeY);
        OpmConfig.EFFECTS_HUD_ENABLED.set(effectsEnabled);
        OpmConfig.EFFECTS_HUD_LOCATION.set(effectsLocation);
        OpmConfig.EFFECTS_HUD_SCALE.set(effectsScale);
        OpmConfig.EFFECTS_HUD_X_OFFSET.set(effectsXOffset);
        OpmConfig.EFFECTS_HUD_Y_OFFSET.set(effectsYOffset);
        OpmConfig.SPEC.save();
    }

    // ── Clamping ─────────────────────────────────────────────────────────────
    private void clampOffsets() {
        int dw = getDurabilityWidth();
        int baseX = (width - dw) / 2, baseY = height - 72;
        durabilityXOffset = Math.clamp(durabilityXOffset, EDGE_PAD - baseX, width - EDGE_PAD - dw - baseX);
        durabilityYOffset = Math.clamp(durabilityYOffset, EDGE_PAD - baseY, height - EDGE_PAD - 9 - baseY);

        int ew = getEffectsWidth(), eh = getEffectsHeight();
        int baseEX = (effectsLocation == OpmConfig.HudLocation.RIGHT) ? (width - EDGE_PAD - ew) : EDGE_PAD;
        effectsXOffset = Math.clamp(effectsXOffset, EDGE_PAD - baseEX, width  - EDGE_PAD - ew - baseEX);
        effectsYOffset = Math.clamp(effectsYOffset, 0, height - EDGE_PAD - eh - EDGE_PAD);

        int[] d = getArmorHudDimensions();
        armorFreeX = Math.clamp(armorFreeX, EDGE_PAD, width  - d[0] - EDGE_PAD);
        armorFreeY = Math.clamp(armorFreeY, EDGE_PAD, height - d[1] - EDGE_PAD);
    }

    // ── HUD helpers ───────────────────────────────────────────────────────────
    private int getDurabilityWidth() { return font.width("[380/1561]"); }
    private int getDurabilityX()     { return (width - getDurabilityWidth()) / 2 + durabilityXOffset; }
    private int getDurabilityY()     { return height - 72 + durabilityYOffset; }
    private int getEffectsWidth()    { return (int)(40 * effectsScale); }
    private int getEffectsHeight()   { return (int)((18 + GAP) * effectsScale) * 2; }
    private int getEffectsX() {
        return (effectsLocation == OpmConfig.HudLocation.RIGHT)
                ? width - EDGE_PAD - getEffectsWidth() + effectsXOffset
                : EDGE_PAD + effectsXOffset;
    }
    private int getEffectsY() { return EDGE_PAD + effectsYOffset; }

    private int[] getArmorHudDimensions() {
        int span = 4 * SLOT_SIZE + 3 * GAP;
        return (armorRotate == 0 || armorRotate == 2) ? new int[]{ span, SLOT_SIZE } : new int[]{ SLOT_SIZE, span };
    }

    private int[] getArmorHudPos() {
        if (!armorLocked) return new int[]{ armorFreeX, armorFreeY };
        int[] dim = getArmorHudDimensions();
        int hotbarX = (width - 182) / 2, itemY = height - 22;
        boolean horiz = (armorRotate == 0 || armorRotate == 2);
        int startX = (armorLocation == OpmConfig.HudLocation.LEFT)
                ? hotbarX - GAP - (horiz ? dim[0] : SLOT_SIZE)
                : hotbarX + 182 + GAP;
        int startY = horiz ? itemY : (armorRotate == 3 ? itemY : itemY - dim[1] + SLOT_SIZE);
        return new int[]{
                Math.clamp(startX, EDGE_PAD, width  - dim[0] - EDGE_PAD),
                Math.clamp(startY, EDGE_PAD, height - dim[1] - EDGE_PAD)
        };
    }

    private int getFooterBtnY() {
        return panelHidden
                ? pY + hdrH + 1 + (ftrH - 16) / 2
                : pY + pH - ftrH + (ftrH - 16) / 2;
    }

    private EquipmentSlot[] buildSlotOrder() {
        EquipmentSlot[] slots = armorInverted
                ? new EquipmentSlot[]{ EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }
                : CANONICAL.clone();
        if (armorRotate == 2 || armorRotate == 3) {
            for (int i = 0, j = slots.length - 1; i < j; i++, j--) {
                EquipmentSlot t = slots[i]; slots[i] = slots[j]; slots[j] = t;
            }
        }
        return slots;
    }

    private int slotIndex(EquipmentSlot s) {
        return switch (s) { case HEAD -> 0; case CHEST -> 1; case LEGS -> 2; case FEET -> 3; default -> 0; };
    }

    private int durColor(float f) {
        return 0xFF000000 | (Math.round(255*(1-f)) << 16) | (Math.round(255*f) << 8);
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x88000000);
        String hint = "Drag HUD elements - Esc to save & exit";
        g.drawString(font, hint, (width - font.width(hint)) / 2, 6, LABEL_COL, false);

        renderDurabilityPreview(g, mx, my);
        renderEffectsPreview(g, mx, my);
        renderArmorPreview(g, mx, my);

        // ── Panel chrome ──────────────────────────────────────────────────────
        if (!panelHidden) {
            // Full panel when visible
            g.fill(pX - 1, pY - 1, pX + pW + 1, pY + pH + 1, BORDER);
            g.fill(pX, pY, pX + pW, pY + pH, BG);
            g.fill(pX, pY, pX + pW, pY + hdrH, HEADER_BG);
            g.fill(pX, pY + hdrH, pX + pW, pY + hdrH + 1, DIVIDER);
            g.fill(pX, pY + pH - ftrH - 1, pX + pW, pY + pH - ftrH, DIVIDER);
            g.fill(pX, pY + pH - ftrH, pX + pW, pY + pH, FOOTER_BG);
        } else {
            // Compact: only header + slim footer bar, no body fill
            int slimH = hdrH + 1 + ftrH + 2;
            g.fill(pX - 1, pY - 1, pX + pW + 1, pY + slimH + 1, BORDER);
            g.fill(pX, pY, pX + pW, pY + hdrH, HEADER_BG);
            g.fill(pX, pY + hdrH, pX + pW, pY + hdrH + 1, DIVIDER);
            g.fill(pX, pY + hdrH + 1, pX + pW, pY + slimH, FOOTER_BG);
        }

        // Title 1.5× scaled with bold shadow
        {
            String title = "OPM CONFIG";
            float sc = 1.5f;
            int tx = pX + (pW - (int)(font.width(title) * sc)) / 2;
            int ty = pY + (hdrH - (int)(8 * sc)) / 2;
            g.pose().pushPose();
            g.pose().translate(tx, ty, 0);
            g.pose().scale(sc, sc, 1f);
            g.drawString(font, title, 1, 1, 0xFF000000, false);
            g.drawString(font, title, 0, 0, 0xFFEEEEEE, false);
            g.pose().popPose();
        }

        if (!panelHidden) renderOptions(g, mx, my);
        renderFooter(g, mx, my);
        super.render(g, mx, my, pt);
    }

    // ── Options list ─────────────────────────────────────────────────────────
    private void renderOptions(GuiGraphics g, int mx, int my) {
        int vx = pX + 6, vy = pY + hdrH + 1;
        int vw = pW - 10, vh = pH - hdrH - 1 - ftrH - 1;

        g.enableScissor(vx, vy, vx + vw, vy + vh);
        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);

        int curY = vy + 2;
        for (ConfigItem item : configItems) {
            boolean rowHov = !(item instanceof CategoryItem)
                    && mx >= vx && mx <= vx + vw
                    && (my + scroll) >= curY && (my + scroll) < curY + ITEM_H;
            if (rowHov && my >= vy && my < vy + vh) g.fill(vx, curY, vx + vw, curY + ITEM_H, HOV_ROW);
            item.render(g, vx, curY, vw, mx, (int)(my + scroll));
            curY += ITEM_H;
        }

        g.pose().popPose();
        g.disableScissor();

        int totalH = configItems.size() * ITEM_H + 4;
        maxScroll = Math.max(0, totalH - vh);
        if (scroll > maxScroll) scroll = maxScroll;

        if (maxScroll > 0) {
            int sbX = pX + pW - 5;
            int th = Math.max(14, vh * vh / totalH);
            int ty = vy + (int)((vh - th) * (scroll / (float) maxScroll));
            g.fill(sbX, vy, sbX + 4, vy + vh, 0xFF111111);
            g.fill(sbX, ty, sbX + 4, ty + th, 0xFF555555);
        }
    }

    // ── Footer ───────────────────────────────────────────────────────────────
    private void renderFooter(GuiGraphics g, int mx, int my) {
        int btnY  = getFooterBtnY();
        int eyeX  = pX + pW - EYE_BTN_W - 4;
        int half  = (pX + pW - EYE_BTN_W - 8 - (pX + 6) - 4) / 2;
        int bx1   = pX + 6, bx2 = bx1 + half + 4;

        if (!panelHidden) {
            drawBtn(g, "Reset Positions", bx1, btnY, half, hit(mx,my,bx1,btnY,half,16), BTN_OFF, BTN_OFF_H);
            drawBtn(g, "Done",                bx2, btnY, half, hit(mx,my,bx2,btnY,half,16), BTN_ON,  BTN_ON_H);
        }

        boolean hEye = hit(mx, my, eyeX, btnY, EYE_BTN_W, 16);
        int ebg = panelHidden ? 0xFF4A2020 : (hEye ? BTN_OFF_H : BTN_OFF);
        g.fill(eyeX, btnY, eyeX + EYE_BTN_W, btnY + 16, ebg);
        drawOutline(g, eyeX, btnY, EYE_BTN_W, 16, BORDER);
        g.drawCenteredString(font, panelHidden ? "◉" : "⊗", eyeX + EYE_BTN_W / 2, btnY + 4,
                panelHidden ? 0xFFFF8888 : 0xFFAAAAAA);
    }

    // ── HUD previews ─────────────────────────────────────────────────────────
    private void renderDurabilityPreview(GuiGraphics g, int mx, int my) {
        if (!durabilityEnabled) return;
        int dx = getDurabilityX(), dy = getDurabilityY(), dw = getDurabilityWidth();
        boolean active = drag == Drag.DURABILITY;
        boolean hov    = hit(mx, my, dx - 4, dy - 2, dw + 8, 13);
        int boxCol     = active ? 0xFFFFFF55 : (hov ? 0xFF55FFFF : 0x8855FFFF);

        g.fill(dx - 4, dy - 2, dx + dw + 4, dy + 11, 0x2200FFFF);
        drawOutline(g, dx - 4, dy - 2, dw + 8, 13, boxCol);

        Minecraft mc = Minecraft.getInstance();
        ItemStack held = (mc.player != null) ? mc.player.getMainHandItem() : ItemStack.EMPTY;
        String durText; int color;
        if (!held.isEmpty() && held.isDamageableItem()) {
            int cur = held.getMaxDamage() - held.getDamageValue(), max = held.getMaxDamage();
            durText = "[" + cur + "/" + max + "]";
            float f = (float) cur / max;
            color = f > 0.6f ? 0xFFAAFFAA : (f > 0.3f ? 0xFFFFFF55 : 0xFFFF5555);
        } else {
            durText = "[380/1561]"; color = 0xFFAAFFAA;
        }

        g.fill(dx - 2, dy - 1, dx + dw + 2, dy + 9, 0x55000000);
        g.drawString(font, durText, dx, dy, color, true);

        if (!held.isEmpty() && held.isDamageableItem() && held.isDamaged()) {
            float f = 1.0f - (float) held.getDamageValue() / held.getMaxDamage();
            int barX = dx - 2, barY = dy + 10;
            g.fill(barX - 1, barY - 1, barX + dw + 4, barY + 2, 0xFF000000);
            g.fill(barX, barY, barX + Math.round(f * (dw - 4)), barY + 1, durColor(f));
        }

        if (hov || active) g.drawString(font, "⠿ Durability HUD", dx, dy - 12, LABEL_COL, false);
    }

    private void renderEffectsPreview(GuiGraphics g, int mx, int my) {
        if (!effectsEnabled) return;
        int ex = getEffectsX(), ey = getEffectsY(), ew = getEffectsWidth(), eh = getEffectsHeight();
        boolean active = drag == Drag.EFFECTS;
        boolean hov    = hit(mx, my, ex, ey, ew, eh);
        int boxCol     = active ? 0xFFFFFF55 : (hov ? 0xFF55FFFF : 0x8855FFFF);

        g.fill(ex - 1, ey - 1, ex + ew + 1, ey + eh + 1, 0x2200FFFF);
        drawOutline(g, ex - 1, ey - 1, ew + 2, eh + 2, boxCol);
        renderMockEffects(g, ex, ey);
        if (hov || active) g.drawString(font, "⠿ Effects HUD", ex, ey - 10, LABEL_COL, false);
    }

    private void renderArmorPreview(GuiGraphics g, int mx, int my) {
        if (!armorEnabled) return;
        int[] pos = getArmorHudPos(), dim = getArmorHudDimensions();
        int ax = pos[0], ay = pos[1], aw = dim[0], ah = dim[1];
        boolean active = drag == Drag.ARMOR;
        boolean hov    = hit(mx, my, ax - 2, ay - 2, aw + 4, ah + 4);
        int boxCol     = active ? 0xFFFFFF55
                : (hov ? (armorLocked ? 0xFF88FF88 : 0xFF55FFFF) : (armorLocked ? 0x5588FF88 : 0x8855FFFF));

        g.fill(ax - 2, ay - 2, ax + aw + 2, ay + ah + 2, 0x2200FF00);
        drawOutline(g, ax - 2, ay - 2, aw + 4, ah + 4, boxCol);

        boolean horiz = (armorRotate == 0 || armorRotate == 2);
        EquipmentSlot[] slots = buildSlotOrder();
        int curX = ax, curY = ay;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        for (EquipmentSlot slot : slots) {
            ItemStack stack = (player != null && !player.getItemBySlot(slot).isEmpty())
                    ? player.getItemBySlot(slot) : MOCK_ARMOR[slotIndex(slot)];
            g.renderItem(stack, curX, curY);
            if (stack.isDamageableItem() && stack.isDamaged()) {
                float f = 1.0f - (float) stack.getDamageValue() / stack.getMaxDamage();
                int bx = curX + 2, barY = curY + SLOT_SIZE + 1;
                g.fill(bx - 1, barY - 1, bx + 14, barY + 2, 0xFF000000);
                g.fill(bx, barY, bx + Math.round(f * 13), barY + 1, durColor(f));
            }
            if (horiz) curX += SLOT_SIZE + GAP; else curY += SLOT_SIZE + GAP;
        }

        if (hov || active) {
            String lbl = "⠿ Armor HUD [" + (armorLocked ? "LOCKED" : "FREE") + "] Rotate:" + armorRotate;
            g.drawString(font, lbl, ax, ay - 10, armorLocked ? 0xFF88FF88 : 0xFF55FFFF, false);
        }
    }

    // ── Mock effects ─────────────────────────────────────────────────────────
    private void renderMockEffects(GuiGraphics g, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        MobEffectTextureManager tm = mc.getMobEffectTextures();
        List<MobEffectInstance> list = new ArrayList<>();

        if (mc.player != null && !mc.player.getActiveEffects().isEmpty()) {
            var it = mc.player.getActiveEffects().iterator();
            for (int i = 0; i < 2 && it.hasNext(); i++) list.add(it.next());
        }
        if (list.isEmpty() && mc.level != null) {
            try {
                var holders = mc.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).holders().toList();
                if (!holders.isEmpty()) list.add(new MobEffectInstance(holders.get(0), 1800, 1));
                if (holders.size() > 1)  list.add(new MobEffectInstance(holders.get(1), 3000, 0));
            } catch (Exception ignored) {}
        }
        if (list.isEmpty()) return;

        int singleH = (int)((18 + GAP) * effectsScale);
        boolean onRight = effectsLocation != OpmConfig.HudLocation.LEFT;

        g.pose().pushPose();
        if (effectsScale != 1.0) g.pose().scale((float) effectsScale, (float) effectsScale, 1f);
        for (int i = 0; i < list.size(); i++) {
            int rx = effectsScale != 1.0 ? (int)(x / effectsScale) : x;
            int ry = effectsScale != 1.0 ? (int)((y + i * singleH) / effectsScale) : (y + i * singleH);
            renderEffectWidget(g, mc, tm, list.get(i), rx, ry, onRight);
        }
        g.pose().popPose();
    }

    private void renderEffectWidget(GuiGraphics g, Minecraft mc, MobEffectTextureManager tm,
                                    MobEffectInstance inst, int x, int y, boolean onRight) {
        Holder<MobEffect> eh = inst.getEffect();
        boolean harmful = eh.value().getCategory() == MobEffectCategory.HARMFUL;
        g.fill(x, y, x + 40, y + 18, harmful ? 0xAA8B0000 : 0xAA000000);
        g.blit(onRight ? x : x + 40 - 18 - 2, y + 1, 0, 18, 18, tm.get(eh));

        String dur = formatDuration(inst.getDuration());
        int amp = inst.getAmplifier() + 1;
        if (onRight) {
            if (amp > 1) g.drawString(mc.font, String.valueOf(amp), x + 40 - mc.font.width(String.valueOf(amp)) - 2, y + 1,  0xFFFFFF, false);
            g.drawString(mc.font, dur, x + 40 - mc.font.width(dur) - 2, amp > 1 ? y + 10 : y + 5, 0xAAAAAA, false);
        } else {
            if (amp > 1) g.drawString(mc.font, String.valueOf(amp), x + 2, y + 1, 0xFFFFFF, false);
            g.drawString(mc.font, dur, x + 2, amp > 1 ? y + 10 : y + 5, 0xAAAAAA, false);
        }
    }

    private String formatDuration(int ticks) {
        if (ticks <= 0 || ticks == Integer.MAX_VALUE) return "∞";
        int sec = ticks / 20;
        if (sec < 60) return sec + "s";
        int min = sec / 60;
        return min < 60 ? min + "m" : (min / 60) + "h";
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int btnY = getFooterBtnY(), eyeX = pX + pW - EYE_BTN_W - 4;
        int half = (pX + pW - EYE_BTN_W - 8 - (pX + 6) - 4) / 2;
        int bx1 = pX + 6, bx2 = bx1 + half + 4;

        if (hit(mx, my, eyeX, btnY, EYE_BTN_W, 16)) { panelHidden = !panelHidden; return true; }

        if (!panelHidden) {
            if (hit(mx, my, bx1, btnY, half, 16)) {
                durabilityXOffset = 0; durabilityYOffset = 0;
                effectsXOffset = 0; effectsYOffset = 0;
                armorFreeX = EDGE_PAD; armorFreeY = EDGE_PAD;
                clampOffsets(); saveAll(); return true;
            }
            if (hit(mx, my, bx2, btnY, half, 16)) { onClose(); return true; }
        }

        if (armorEnabled && !armorLocked) {
            int[] pos = getArmorHudPos(), dim = getArmorHudDimensions();
            if (hit(mx, my, pos[0]-2, pos[1]-2, dim[0]+4, dim[1]+4)) {
                drag = Drag.ARMOR; dragGrabX = mx - pos[0]; dragGrabY = my - pos[1]; return true;
            }
        }
        if (durabilityEnabled) {
            int dx = getDurabilityX(), dy = getDurabilityY();
            if (hit(mx, my, dx-4, dy-2, getDurabilityWidth()+8, 13)) {
                drag = Drag.DURABILITY; dragGrabX = mx - dx; dragGrabY = my - dy; return true;
            }
        }
        if (effectsEnabled) {
            int ex = getEffectsX(), ey = getEffectsY();
            if (hit(mx, my, ex, ey, getEffectsWidth(), getEffectsHeight())) {
                drag = Drag.EFFECTS; dragGrabX = mx - ex; dragGrabY = my - ey; return true;
            }
        }

        int vy = pY + hdrH + 1, vh = pH - hdrH - 1 - ftrH - 1;
        if (maxScroll > 0 && mx >= pX + pW - 8 && mx <= pX + pW && my >= vy && my <= vy + vh) {
            draggingScrollbar = true;
            scroll = (float) Math.clamp(((my - vy) / (float) vh) * maxScroll, 0.0, maxScroll);
            return true;
        }

        int vx = pX + 6, vw = pW - 10;
        if (mx >= vx && mx <= vx + vw && my >= vy && my <= vy + vh) {
            int curY = vy + 2;
            for (ConfigItem item : configItems) {
                int scrolledY = (int)(my + scroll);
                if (scrolledY >= curY && scrolledY < curY + ITEM_H) {
                    if (item.click(mx, scrolledY, vx, curY, vw)) { saveAll(); return true; }
                }
                curY += ITEM_H;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (draggingScrollbar) {
            int vy = pY + hdrH + 1, vh = pH - hdrH - 1 - ftrH - 1;
            scroll = (float) Math.clamp(((my - vy) / (float) vh) * maxScroll, 0.0, maxScroll);
            return true;
        }
        switch (drag) {
            case ARMOR -> { armorFreeX = mx - dragGrabX; armorFreeY = my - dragGrabY; clampOffsets(); saveAll(); return true; }
            case DURABILITY -> {
                durabilityXOffset = mx - dragGrabX - (width - getDurabilityWidth()) / 2;
                durabilityYOffset = my - dragGrabY - (height - 72);
                clampOffsets(); saveAll(); return true;
            }
            case EFFECTS -> {
                if (mouseX < width / 2.0) {
                    effectsLocation = OpmConfig.HudLocation.LEFT;
                    effectsXOffset  = mx - dragGrabX - EDGE_PAD;
                } else {
                    effectsLocation = OpmConfig.HudLocation.RIGHT;
                    effectsXOffset  = mx - dragGrabX - (width - EDGE_PAD - getEffectsWidth());
                }
                effectsYOffset = my - dragGrabY - EDGE_PAD;
                clampOffsets(); saveAll(); return true;
            }
            default -> {}
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
            if (drag != Drag.NONE) { drag = Drag.NONE; saveAll(); return true; }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int vy = pY + hdrH + 1, vh = pH - hdrH - 1 - ftrH - 1;
        if (!panelHidden && mouseX >= pX && mouseX <= pX + pW && mouseY >= vy && mouseY <= vy + vh) {
            scroll = Math.clamp(scroll - (float) scrollY * 12, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────
    private void drawOutline(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x, y, x+w, y+1, col); g.fill(x, y+h-1, x+w, y+h, col);
        g.fill(x, y+1, x+1, y+h-1, col); g.fill(x+w-1, y+1, x+w, y+h-1, col);
    }

    private void drawBtn(GuiGraphics g, String lbl, int bx, int by, int bw, boolean hov, int bg, int hbg) {
        g.fill(bx, by, bx+bw, by+16, hov ? hbg : bg);
        g.fill(bx, by, bx+bw, by+1, 0x44FFFFFF);
        g.drawCenteredString(font, lbl, bx+bw/2, by+4, 0xFFEEEEEE);
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x+w && my >= y && my <= y+h;
    }

    // ── Config item types ─────────────────────────────────────────────────────
    private abstract class ConfigItem {
        final String label;
        ConfigItem(String lbl) { this.label = lbl; }
        abstract void render(GuiGraphics g, int x, int y, int w, int mx, int my);
        abstract boolean click(int mx, int my, int x, int y, int w);
    }

    private class CategoryItem extends ConfigItem {
        CategoryItem(String lbl) { super(lbl); }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.fill(x, y+3, x+w, y+ITEM_H-1, 0xFF1A1A1A);
            g.fill(x, y+ITEM_H-1, x+w, y+ITEM_H, DIVIDER);
            g.fill(x, y+5, x+2, y+ITEM_H-3, CAT_COL);
            g.drawString(font, label.toUpperCase(), x+7, y+7, CAT_COL, false);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) { return false; }
    }

    private class BooleanItem extends ConfigItem {
        interface Getter { boolean get(); } interface Setter { void set(boolean v); }
        private final Getter getter; private final Setter setter;
        BooleanItem(String lbl, Getter g, Setter s) { super(lbl); getter = g; setter = s; }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            boolean val = getter.get();
            int bx = x+w-40, by = y+(ITEM_H-14)/2;
            boolean hov = hit(mx, my, bx, by, 36, 14);
            g.drawString(font, label, x+4, y+7, TEXT, false);
            g.fill(bx, by, bx+36, by+14, val ? (hov ? BTN_ON_H : BTN_ON) : (hov ? BTN_OFF_H : BTN_OFF));
            drawOutline(g, bx, by, 36, 14, BORDER);
            g.drawCenteredString(font, val ? "ON" : "OFF", bx+18, by+3, val ? 0xFF88FF88 : 0xFFAAAAAA);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) {
            int bx = x+w-40, by = y+(ITEM_H-14)/2;
            if (hit(mx, my, bx, by, 36, 14)) { setter.set(!getter.get()); return true; }
            return false;
        }
    }

    private class EnumItem<T extends Enum<T>> extends ConfigItem {
        interface Getter<T> { T get(); } interface Setter<T> { void set(T v); }
        private final Getter<T> getter; private final Setter<T> setter; private final T[] values;
        EnumItem(String lbl, Getter<T> g, T[] vals, Setter<T> s) { super(lbl); getter=g; values=vals; setter=s; }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            int bx = x+w-68, by = y+(ITEM_H-14)/2;
            boolean hov = hit(mx, my, bx, by, 64, 14);
            g.drawString(font, label, x+4, y+7, TEXT, false);
            g.fill(bx, by, bx+64, by+14, hov ? BTN_ENUM_H : BTN_ENUM);
            drawOutline(g, bx, by, 64, 14, BORDER);
            g.drawCenteredString(font, getter.get().name(), bx+32, by+3, 0xFFCCCCFF);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) {
            int bx = x+w-68, by = y+(ITEM_H-14)/2;
            if (hit(mx, my, bx, by, 64, 14)) { setter.set(values[(getter.get().ordinal()+1) % values.length]); return true; }
            return false;
        }
    }

    private class WideEnumItem<T extends Enum<T>> extends ConfigItem {
        interface Getter<T> { T get(); } interface Setter<T> { void set(T v); }
        private final Getter<T> getter; private final Setter<T> setter; private final T[] values;
        WideEnumItem(String lbl, Getter<T> g, T[] vals, Setter<T> s) { super(lbl); getter=g; values=vals; setter=s; }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            int bx = x+w-90, by = y+(ITEM_H-14)/2;
            boolean hov = hit(mx, my, bx, by, 86, 14);
            g.drawString(font, label, x+4, y+7, TEXT, false);
            g.fill(bx, by, bx+86, by+14, hov ? BTN_ENUM_H : BTN_ENUM);
            drawOutline(g, bx, by, 86, 14, BORDER);
            g.drawCenteredString(font, getter.get().name(), bx+43, by+3, 0xFFCCCCFF);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) {
            int bx = x+w-90, by = y+(ITEM_H-14)/2;
            if (hit(mx, my, bx, by, 86, 14)) { setter.set(values[(getter.get().ordinal()+1) % values.length]); return true; }
            return false;
        }
    }

    private class CycleArrowItem extends ConfigItem {
        interface Getter { int get(); } interface Setter { void set(int v); }
        private final Getter getter; private final Setter setter; private final int min, max;
        CycleArrowItem(String lbl, Getter g, int min, int max, Setter s) { super(lbl); getter=g; this.min=min; this.max=max; setter=s; }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            int bx = x+w-60, by = y+(ITEM_H-14)/2;
            boolean hL = hit(mx,my,bx,by,14,14), hR = hit(mx,my,bx+42,by,14,14);
            g.drawString(font, label, x+4, y+7, TEXT, false);
            g.fill(bx, by, bx+14, by+14, hL ? BTN_OFF_H : BTN_OFF);
            g.fill(bx+42, by, bx+56, by+14, hR ? BTN_OFF_H : BTN_OFF);
            drawOutline(g, bx, by, 14, 14, BORDER);
            drawOutline(g, bx+42, by, 14, 14, BORDER);
            g.drawCenteredString(font, "◀", bx+7, by+3, 0xFFEEEEEE);
            g.drawCenteredString(font, "▶", bx+49, by+3, 0xFFEEEEEE);
            g.drawCenteredString(font, String.valueOf(getter.get()), bx+28, by+3, TEXT);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) {
            int bx = x+w-60, by = y+(ITEM_H-14)/2;
            if (hit(mx,my,bx,by,14,14))    { int v = getter.get()-1; setter.set(v < min ? max : v); return true; }
            if (hit(mx,my,bx+42,by,14,14)) { int v = getter.get()+1; setter.set(v > max ? min : v); return true; }
            return false;
        }
    }

    private class IntItem extends ConfigItem {
        interface Getter { int get(); } interface Setter { void set(int v); }
        private final Getter getter; private final Setter setter; private final int min, max, step;
        IntItem(String lbl, Getter g, int min, int max, int step, Setter s) { super(lbl); getter=g; this.min=min; this.max=max; this.step=step; setter=s; }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            int bx = x+w-60, by = y+(ITEM_H-14)/2;
            boolean hM = hit(mx,my,bx,by,12,14), hP = hit(mx,my,bx+44,by,12,14);
            g.drawString(font, label, x+4, y+7, TEXT, false);
            g.fill(bx, by, bx+12, by+14, hM ? BTN_OFF_H : BTN_OFF);
            g.fill(bx+44, by, bx+56, by+14, hP ? BTN_OFF_H : BTN_OFF);
            drawOutline(g, bx, by, 12, 14, BORDER);
            drawOutline(g, bx+44, by, 12, 14, BORDER);
            g.drawCenteredString(font, "−", bx+6, by+3, 0xFFEEEEEE);
            g.drawCenteredString(font, "+", bx+50, by+3, 0xFFEEEEEE);
            g.drawCenteredString(font, String.valueOf(getter.get()), bx+28, by+3, TEXT);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) {
            int bx = x+w-60, by = y+(ITEM_H-14)/2;
            if (hit(mx,my,bx,by,12,14))    { setter.set(Math.clamp(getter.get()-step, min, max)); return true; }
            if (hit(mx,my,bx+44,by,12,14)) { setter.set(Math.clamp(getter.get()+step, min, max)); return true; }
            return false;
        }
    }

    private class DoubleItem extends ConfigItem {
        interface Getter { double get(); } interface Setter { void set(double v); }
        private final Getter getter; private final Setter setter; private final double min, max, step;
        DoubleItem(String lbl, Getter g, double min, double max, double step, Setter s) { super(lbl); getter=g; this.min=min; this.max=max; this.step=step; setter=s; }
        @Override void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            int bx = x+w-60, by = y+(ITEM_H-14)/2;
            boolean hM = hit(mx,my,bx,by,12,14), hP = hit(mx,my,bx+44,by,12,14);
            g.drawString(font, label, x+4, y+7, TEXT, false);
            g.fill(bx, by, bx+12, by+14, hM ? BTN_OFF_H : BTN_OFF);
            g.fill(bx+44, by, bx+56, by+14, hP ? BTN_OFF_H : BTN_OFF);
            drawOutline(g, bx, by, 12, 14, BORDER);
            drawOutline(g, bx+44, by, 12, 14, BORDER);
            g.drawCenteredString(font, "−", bx+6, by+3, 0xFFEEEEEE);
            g.drawCenteredString(font, "+", bx+50, by+3, 0xFFEEEEEE);
            g.drawCenteredString(font, String.format("%.2f", getter.get()), bx+28, by+3, TEXT);
        }
        @Override boolean click(int mx, int my, int x, int y, int w) {
            int bx = x+w-60, by = y+(ITEM_H-14)/2;
            if (hit(mx,my,bx,by,12,14))    { setter.set(Math.round(Math.clamp(getter.get()-step,min,max)*100.0)/100.0); return true; }
            if (hit(mx,my,bx+44,by,12,14)) { setter.set(Math.round(Math.clamp(getter.get()+step,min,max)*100.0)/100.0); return true; }
            return false;
        }
    }
}