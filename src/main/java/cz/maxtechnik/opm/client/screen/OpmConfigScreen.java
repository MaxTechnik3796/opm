package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OpmConfigScreen extends Screen {

    private final Screen parent;

    // Working copies for general config options
    private boolean noRecipeBook;
    private boolean noRealmsButton;
    private boolean customDebugScreen;
    private boolean hideTutorialToast;
    private OpmConfig.PumpkinMode pumpkinOverlay;

    // Working copies for HUD options
    private boolean durabilityEnabled;
    private int durabilityXOffset;
    private int durabilityYOffset;

    private boolean armorEnabled;
    private boolean armorInverted;
    private OpmConfig.HudLocation armorLocation;

    private boolean effectsEnabled;
    private OpmConfig.HudLocation effectsLocation;
    private double effectsScale;
    private int effectsTopOffset;
    private int effectsXOffset;
    private int effectsYOffset;

    // Dragging state
    private DraggedElement draggedElement = DraggedElement.NONE;
    private int dragGrabX = 0;
    private int dragGrabY = 0;

    // Scrollbar state for the options panel
    private float scroll = 0;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;

    // Config options list
    private final List<ConfigItem> configItems = new ArrayList<>();

    private int pX, pY, pW, pH, hdrH;
    private boolean hasChanges = false;

    private enum DraggedElement {
        NONE, DURABILITY, EFFECTS
    }

    public OpmConfigScreen(Screen parent) {
        super(Component.literal("OPM Config"));
        this.parent = parent;

        // Load working copies from config
        this.noRecipeBook = OpmConfig.NO_RECIPE_BOOK.get();
        this.noRealmsButton = OpmConfig.NO_REALMS_BUTTON.get();
        this.customDebugScreen = OpmConfig.CUSTOM_DEBUG_SCREEN.get();
        this.hideTutorialToast = OpmConfig.HIDE_TUTORIAL_TOAST.get();
        this.pumpkinOverlay = OpmConfig.PUMPKIN_OVERLAY.get();

        this.durabilityEnabled = OpmConfig.ITEM_DURABILITY_IN_NAME.get();
        this.durabilityXOffset = OpmConfig.ITEM_DURABILITY_X_OFFSET.get();
        this.durabilityYOffset = OpmConfig.ITEM_DURABILITY_Y_OFFSET.get();

        this.armorEnabled = OpmConfig.ARMOR_HUD_ENABLED.get();
        this.armorInverted = OpmConfig.ARMOR_HUD_INVERTED.get();
        this.armorLocation = OpmConfig.ARMOR_HUD_LOCATION.get();

        this.effectsEnabled = OpmConfig.EFFECTS_HUD_ENABLED.get();
        this.effectsLocation = OpmConfig.EFFECTS_HUD_LOCATION.get();
        this.effectsScale = OpmConfig.EFFECTS_HUD_SCALE.get();
        this.effectsTopOffset = OpmConfig.EFFECTS_HUD_TOP_OFFSET.get();
        this.effectsXOffset = OpmConfig.EFFECTS_HUD_X_OFFSET.get();
        this.effectsYOffset = OpmConfig.EFFECTS_HUD_Y_OFFSET.get();

        // Populate options list
        configItems.add(new CategoryItem("UI Options"));
        configItems.add(new BooleanItem("Hide Recipe Book", this.noRecipeBook, val -> this.noRecipeBook = val));
        configItems.add(new BooleanItem("Hide Realms Button", this.noRealmsButton, val -> this.noRealmsButton = val));
        configItems.add(new BooleanItem("Custom Debug Screen", this.customDebugScreen, val -> this.customDebugScreen = val));
        configItems.add(new BooleanItem("Hide Tutorial Toast", this.hideTutorialToast, val -> this.hideTutorialToast = val));
        configItems.add(new EnumItem<>("Pumpkin Overlay", this.pumpkinOverlay, OpmConfig.PumpkinMode.values(), val -> this.pumpkinOverlay = val));
        configItems.add(new BooleanItem("Durability HUD", this.durabilityEnabled, val -> this.durabilityEnabled = val));

        configItems.add(new CategoryItem("Armor HUD"));
        configItems.add(new BooleanItem("Enabled", this.armorEnabled, val -> this.armorEnabled = val));
        configItems.add(new BooleanItem("Inverted Order", this.armorInverted, val -> this.armorInverted = val));
        configItems.add(new EnumItem<>("Align Location", this.armorLocation, OpmConfig.HudLocation.values(), val -> this.armorLocation = val));

        configItems.add(new CategoryItem("Effects HUD"));
        configItems.add(new BooleanItem("Enabled", this.effectsEnabled, val -> this.effectsEnabled = val));
        configItems.add(new EnumItem<>("Align Location", this.effectsLocation, OpmConfig.HudLocation.values(), val -> this.effectsLocation = val));
        configItems.add(new IntItem("Top Offset", this.effectsTopOffset, 0, 1000, 5, val -> this.effectsTopOffset = val));
        configItems.add(new DoubleItem("Scale Factor", this.effectsScale, 1.0, 2.0, 0.05, val -> this.effectsScale = val));
    }

    @Override
    protected void init() {
        super.init();

        pW = 300;
        pH = Math.min(height - 20, 240);
        pX = (width - pW) / 2;
        pY = (height - pH) / 2;
        hdrH = 26;

        clampOffsets();
    }

    @Override
    public void tick() {
        super.tick();
        if (hasChanges) {
            saveConfigValues();
            hasChanges = false;
        }
    }

    private void clampOffsets() {
        // Durability clamp (keep at least 2px from edges)
        int dw = getDurabilityWidth();
        int dh = 9;
        int minDurX = 2 - (width - dw) / 2;
        int maxDurX = width - 2 - dw - (width - dw) / 2;
        int minDurY = 2 - (height - 72);
        int maxDurY = height - 2 - dh - (height - 72);
        durabilityXOffset = Math.clamp(durabilityXOffset, minDurX, maxDurX);
        durabilityYOffset = Math.clamp(durabilityYOffset, minDurY, maxDurY);

        // Effects clamp (keep at least 2px from edges)
        int ew = getEffectsWidth();
        int eh = getEffectsHeight();
        int baseX;
        if (effectsLocation == OpmConfig.HudLocation.RIGHT) {
            baseX = width - 4 - ew;
        } else if (effectsLocation == OpmConfig.HudLocation.CENTER) {
            baseX = (width - ew) / 2;
        } else {
            baseX = 4;
        }
        int baseY = 4 + effectsTopOffset;

        int minEffX = 2 - baseX;
        int maxEffX = width - 2 - ew - baseX;
        int minEffY = 2 - baseY;
        int maxEffY = height - 2 - eh - baseY;

        effectsXOffset = Math.clamp(effectsXOffset, minEffX, maxEffX);
        effectsYOffset = Math.clamp(effectsYOffset, minEffY, maxEffY);
    }

    private void saveConfigValues() {
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

        OpmConfig.EFFECTS_HUD_ENABLED.set(effectsEnabled);
        OpmConfig.EFFECTS_HUD_LOCATION.set(effectsLocation);
        OpmConfig.EFFECTS_HUD_SCALE.set(effectsScale);
        OpmConfig.EFFECTS_HUD_TOP_OFFSET.set(effectsTopOffset);
        OpmConfig.EFFECTS_HUD_X_OFFSET.set(effectsXOffset);
        OpmConfig.EFFECTS_HUD_Y_OFFSET.set(effectsYOffset);

        OpmConfig.SPEC.save();
    }

    @Override
    public void onClose() {
        saveConfigValues();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Coordinates and sizes of mock elements
    private int getDurabilityWidth() {
        return font.width("[380/1561]");
    }

    private int getDurabilityX() {
        return (width - getDurabilityWidth()) / 2 + durabilityXOffset;
    }

    private int getDurabilityY() {
        return height - 72 + durabilityYOffset;
    }

    private int getEffectsWidth() {
        return (int) (40 * effectsScale);
    }

    private int getEffectsHeight() {
        int singleHeight = (int) (20 * effectsScale);
        return singleHeight * 2; // 2 mock effects
    }

    private int getEffectsX() {
        int effW = getEffectsWidth();
        if (effectsLocation == OpmConfig.HudLocation.RIGHT) {
            return width - 4 - effW + effectsXOffset;
        } else if (effectsLocation == OpmConfig.HudLocation.CENTER) {
            return (width - effW) / 2 + effectsXOffset;
        } else {
            return 4 + effectsXOffset;
        }
    }

    private int getEffectsY() {
        return 4 + effectsTopOffset + effectsYOffset;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        // Draw semi-transparent background
        g.fill(0, 0, width, height, 0x77000000);

        // Draw HUD editor instructions on top
        String title = "HUD Editor - Drag elements to reposition (Esc to exit)";
        g.drawString(font, title, (width - font.width(title)) / 2, 6, 0xFFFFFF, true);

        // Draw settings panel background
        g.fill(pX - 1, pY - 1, pX + pW + 1, pY + pH + 1, 0xFF000000); // Border
        g.fill(pX, pY, pX + pW, pY + pH, 0xF0222222); // BG
        g.fill(pX, pY, pX + pW, pY + hdrH, 0xFF111111); // Header BG
        g.fill(pX, pY + hdrH, pX + pW, pY + hdrH + 1, 0xFF000000); // Divider

        // Draw panel header title
        g.drawCenteredString(font, "OPM CONFIG", pX + pW / 2, pY + 8, 0xFFEEEEEE);

        // Render scrollable options list
        renderOptions(g, mx, my, pt);

        // Draw bottom panel area divider & background
        g.fill(pX, pY + pH - 24, pX + pW, pY + pH - 23, 0xFF000000);
        g.fill(pX, pY + pH - 23, pX + pW, pY + pH, 0xFF111111);

        // Draw bottom buttons
        int btnW = (pW - 24) / 2;
        int btnY = pY + pH - 20;
        boolean hovReset = mx >= pX + 8 && mx <= pX + 8 + btnW && my >= btnY && my <= btnY + 16;
        boolean hovDone = mx >= pX + 8 + btnW + 8 && mx <= pX + 8 + btnW + 8 + btnW && my >= btnY && my <= btnY + 16;

        drawBtn(g, "Reset Position", pX + 8, btnY, btnW, hovReset, 0xFF383838, 0xFF585858);
        drawBtn(g, "Done", pX + 8 + btnW + 8, btnY, btnW, hovDone, 0xFF1E4A1E, 0xFF2A6A2A);

        // Render Durability HUD preview
        if (durabilityEnabled) {
            int dx = getDurabilityX();
            int dy = getDurabilityY();
            int dw = getDurabilityWidth();
            int dh = 9;

            // Draw bounding box
            boolean hover = mx >= dx - 4 && mx <= dx + dw + 4 && my >= dy - 2 && my <= dy + dh + 2;
            int boxColor = draggedElement == DraggedElement.DURABILITY ? 0xFFFFFF55 : (hover ? 0xFF55FFFF : 0x8855FFFF);
            g.fill(dx - 4, dy - 2, dx + dw + 4, dy + dh + 2, 0x3300FFFF);
            drawOutline(g, dx - 4, dy - 2, dw + 8, dh + 4, boxColor);

            // Draw text
            g.fill(dx - 2, dy - 1, dx + dw + 2, dy + 9, 0x55000000);
            g.drawString(font, "[380/1561]", dx, dy, 0xFFAAFFAA, true);
        }

        // Render Effects HUD preview
        if (effectsEnabled) {
            int ex = getEffectsX();
            int ey = getEffectsY();
            int ew = getEffectsWidth();
            int eh = getEffectsHeight();

            // Draw bounding box
            boolean hover = mx >= ex && mx <= ex + ew && my >= ey && my <= ey + eh;
            int boxColor = draggedElement == DraggedElement.EFFECTS ? 0xFFFFFF55 : (hover ? 0xFF55FFFF : 0x8855FFFF);
            g.fill(ex, ey, ex + ew, ey + eh, 0x3300FFFF);
            drawOutline(g, ex, ey, ew, eh, boxColor);

            // Draw mock effects
            renderMockEffects(g, ex, ey);
        }
    }

    private void renderOptions(GuiGraphics g, int mx, int my, float pt) {
        int vx = pX + 8;
        int vy = pY + hdrH + 4;
        int vw = pW - 16;
        int vh = pH - hdrH - 4 - 28;

        g.enableScissor(vx, vy, vx + vw, vy + vh);

        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, -scroll, 0);

        int curY = vy;
        int itemH = 20;

        for (ConfigItem item : configItems) {
            boolean rowHover = mx >= vx && mx <= vx + vw && my + scroll >= curY && my + scroll < curY + itemH;
            if (!(item instanceof CategoryItem) && rowHover && my >= vy && my < vy + vh) {
                g.fill(vx, curY, vx + vw, curY + itemH, 0x15FFFFFF);
            }
            item.render(g, vx, curY, vw, mx, (int) (my + scroll));
            curY += itemH;
        }

        pose.popPose();
        g.disableScissor();

        int totalHeight = configItems.size() * itemH;
        maxScroll = Math.max(0, totalHeight - vh);
        if (scroll > maxScroll) scroll = maxScroll;

        // Render scrollbar inside the panel on the right
        if (maxScroll > 0) {
            int sbX = pX + pW - 6;
            int sbY = vy;
            g.fill(sbX, sbY, sbX + 4, sbY + vh, 0xFF111111);
            int th = Math.max(15, vh * vh / totalHeight);
            int ty = sbY + (int) ((vh - th) * (scroll / (float) maxScroll));
            g.fill(sbX, ty, sbX + 4, ty + th, 0xFF666666);
        }
    }

    private void drawOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);           // top
        g.fill(x, y + h - 1, x + w, y + h, color);   // bottom
        g.fill(x, y + 1, x + 1, y + h - 1, color);   // left
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color); // right
    }

    private void drawBtn(GuiGraphics g, String lbl, int bx, int by, int bw, boolean hov, int bg, int hbg) {
        g.fill(bx, by, bx + bw, by + 16, hov ? hbg : bg);
        g.fill(bx, by, bx + bw, by + 1, 0x44FFFFFF);
        g.drawCenteredString(font, lbl, bx + bw / 2, by + 4, 0xFFEEEEEE);
    }

    private void renderMockEffects(GuiGraphics g, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        MobEffectTextureManager textureManager = mc.getMobEffectTextures();

        List<MobEffectInstance> mockList = new ArrayList<>();
        if (mc.level != null) {
            try {
                var registry = mc.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
                List<Holder.Reference<MobEffect>> holders = registry.holders().toList();
                if (!holders.isEmpty()) {
                    mockList.add(new MobEffectInstance(holders.get(0), 1800, 1));
                    if (holders.size() > 1) {
                        mockList.add(new MobEffectInstance(holders.get(1), 3000, 0));
                    }
                }
            } catch (Exception ignored) {}
        }

        int singleH = 20;

        var pose = g.pose();
        pose.pushPose();
        if (effectsScale != 1.0) {
            pose.scale((float) effectsScale, (float) effectsScale, 1.0f);
            x = (int) (x / effectsScale);
            y = (int) (y / effectsScale);
        }

        boolean onRight = effectsLocation != OpmConfig.HudLocation.LEFT;

        for (int i = 0; i < mockList.size(); i++) {
            renderEffectWidget(g, mc, textureManager, mockList.get(i), x, y + i * singleH, onRight);
        }

        pose.popPose();
    }

    private void renderEffectWidget(GuiGraphics graphics, Minecraft mc,
                                    MobEffectTextureManager textureManager,
                                    MobEffectInstance instance,
                                    int x, int y, boolean onRight) {
        Holder<MobEffect> effectHolder = instance.getEffect();
        boolean isHarmful = effectHolder.value().getCategory() == MobEffectCategory.HARMFUL;
        int bgColor = isHarmful ? 0xAA8B0000 : 0xAA000000;

        graphics.fill(x, y, x + 40, y + 18, bgColor);

        TextureAtlasSprite sprite = textureManager.get(effectHolder);
        int iconX = onRight ? x + 1 : x + 40 - 18 - 1;
        graphics.blit(iconX, y, 0, 18, 18, sprite);

        String durationText = formatDuration(instance.getDuration());
        int amplifier = instance.getAmplifier() + 1;

        if (onRight) {
            if (amplifier > 1) {
                String ampText = String.valueOf(amplifier);
                graphics.drawString(mc.font, ampText, x + 40 - mc.font.width(ampText) - 2, y + 1, 0xFFFFFF, false);
                graphics.drawString(mc.font, durationText, x + 40 - mc.font.width(durationText) - 2, y + 10, 0xAAAAAA, false);
            } else {
                graphics.drawString(mc.font, durationText, x + 40 - mc.font.width(durationText) - 2, y + 5, 0xAAAAAA, false);
            }
        } else {
            if (amplifier > 1) {
                graphics.drawString(mc.font, String.valueOf(amplifier), x + 2, y + 1, 0xFFFFFF, false);
                graphics.drawString(mc.font, durationText, x + 2, y + 10, 0xAAAAAA, false);
            } else {
                graphics.drawString(mc.font, durationText, x + 2, y + 5, 0xAAAAAA, false);
            }
        }
    }

    private String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        return minutes + ":" + String.format("%02d", seconds % 60);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Check HUD previews first
        if (button == 0) {
            // Durability HUD
            if (durabilityEnabled) {
                int dx = getDurabilityX();
                int dy = getDurabilityY();
                int dw = getDurabilityWidth();
                if (mx >= dx - 4 && mx <= dx + dw + 4 && my >= dy - 2 && my <= dy + 11) {
                    draggedElement = DraggedElement.DURABILITY;
                    dragGrabX = (int) (mouseX - dx);
                    dragGrabY = (int) (mouseY - dy);
                    return true;
                }
            }

            // Effects HUD
            if (effectsEnabled) {
                int ex = getEffectsX();
                int ey = getEffectsY();
                int ew = getEffectsWidth();
                int eh = getEffectsHeight();
                if (mx >= ex && mx <= ex + ew && my >= ey && my <= ey + eh) {
                    draggedElement = DraggedElement.EFFECTS;
                    dragGrabX = (int) (mouseX - ex);
                    dragGrabY = (int) (mouseY - ey);
                    return true;
                }
            }
        }

        // Check bottom buttons
        int btnW = (pW - 24) / 2;
        int btnY = pY + pH - 20;
        if (button == 0) {
            // Reset Position click
            if (mx >= pX + 8 && mx <= pX + 8 + btnW && my >= btnY && my <= btnY + 16) {
                durabilityXOffset = 0;
                durabilityYOffset = 0;
                effectsXOffset = 0;
                effectsYOffset = 0;
                clampOffsets();
                hasChanges = true;
                saveConfigValues();
                return true;
            }

            // Done click
            if (mx >= pX + 8 + btnW + 8 && mx <= pX + 8 + btnW + 8 + btnW && my >= btnY && my <= btnY + 16) {
                onClose();
                return true;
            }
        }

        // Check scrollbar
        int vx = pX + 8;
        int vy = pY + hdrH + 4;
        int vw = pW - 16;
        int vh = pH - hdrH - 4 - 28;
        if (button == 0 && maxScroll > 0 && mx >= pX + pW - 6 && mx <= pX + pW && my >= vy && my <= vy + vh) {
            draggingScrollbar = true;
            scroll = (float) Math.clamp(((my - vy) / (float) vh) * maxScroll, 0.0, maxScroll);
            return true;
        }

        // Check options list
        if (mx >= vx && mx <= vx + vw && my >= vy && my <= vy + vh) {
            int clickedY = (int) (my + scroll);
            int curY = vy;
            int itemH = 20;
            for (ConfigItem item : configItems) {
                if (clickedY >= curY && clickedY < curY + itemH) {
                    if (item.click(mx, (int) (my + scroll), vx, curY, vw)) {
                        hasChanges = true;
                        saveConfigValues();
                        return true;
                    }
                }
                curY += itemH;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            int vy = pY + hdrH + 4;
            int vh = pH - hdrH - 4 - 28;
            scroll = (float) Math.clamp(((mouseY - vy) / (float) vh) * maxScroll, 0.0, maxScroll);
            return true;
        }

        if (draggedElement == DraggedElement.DURABILITY) {
            durabilityXOffset = (int) (mouseX - dragGrabX - (width - getDurabilityWidth()) / 2);
            durabilityYOffset = (int) (mouseY - dragGrabY - (height - 72));
            clampOffsets();
            hasChanges = true;
            return true;
        } else if (draggedElement == DraggedElement.EFFECTS) {
            int ew = getEffectsWidth();
            int baseY = 4 + effectsTopOffset;

            // Snap behavior: Left or Right half of the screen
            if (mouseX < width / 2) {
                effectsLocation = OpmConfig.HudLocation.LEFT;
                effectsXOffset = (int) (mouseX - dragGrabX - 4);
            } else {
                effectsLocation = OpmConfig.HudLocation.RIGHT;
                effectsXOffset = (int) (mouseX - dragGrabX - (width - 4 - ew));
            }
            effectsYOffset = (int) (mouseY - dragGrabY - baseY);
            clampOffsets();
            hasChanges = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
            if (draggedElement != DraggedElement.NONE) {
                draggedElement = DraggedElement.NONE;
                saveConfigValues();
                hasChanges = false;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int vy = pY + hdrH + 4;
        int vh = pH - hdrH - 4 - 28;
        if (mouseX >= pX && mouseX <= pX + pW && mouseY >= vy && mouseY <= vy + vh) {
            scroll = Math.clamp(scroll - (float) scrollY * 12, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── CONFIG ITEMS HIERARCHY ──────────────────────────────────────────────────

    private abstract class ConfigItem {
        final String label;
        ConfigItem(String label) { this.label = label; }
        abstract void render(GuiGraphics g, int x, int y, int w, int mx, int my);
        abstract boolean click(int mx, int my, int x, int y, int w);
    }

    private class CategoryItem extends ConfigItem {
        CategoryItem(String label) { super(label); }
        @Override
        void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.fill(x, y + 2, x + w, y + 18, 0xFF111111);
            String txt = "--- " + label + " ---";
            g.drawCenteredString(font, txt.toUpperCase(), x + w / 2, y + 6, 0xFFFFAA00);
        }
        @Override
        boolean click(int mx, int my, int x, int y, int w) { return false; }
    }

    private class BooleanItem extends ConfigItem {
        interface BooleanSetter { void set(boolean val); }
        private final BooleanSetter setter;
        private boolean value;

        BooleanItem(String label, boolean initial, BooleanSetter setter) {
            super(label);
            this.value = initial;
            this.setter = setter;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.drawString(font, label, x + 4, y + 6, 0xFFEEEEEE, false);
            int valW = 40;
            int valX = x + w - valW - 4;
            boolean hov = mx >= valX && mx <= valX + valW && my >= y + 2 && my <= y + 18;
            int bg = value ? (hov ? 0xFF2A6A2A : 0xFF1E4A1E) : (hov ? 0xFF6A2222 : 0xFF4A1A1A);
            g.fill(valX, y + 2, valX + valW, y + 18, bg);
            drawOutline(g, valX, y + 2, valW, 16, 0xFF000000);
            g.drawCenteredString(font, value ? "ON" : "OFF", valX + valW / 2, y + 6, 0xFFFFFFFF);
        }

        @Override
        boolean click(int mx, int my, int x, int y, int w) {
            int valW = 40;
            int valX = x + w - valW - 4;
            if (mx >= valX && mx <= valX + valW && my >= y + 2 && my <= y + 18) {
                value = !value;
                setter.set(value);
                return true;
            }
            return false;
        }
    }

    private class EnumItem<T extends Enum<T>> extends ConfigItem {
        interface EnumSetter<T extends Enum<T>> { void set(T val); }
        private final EnumSetter<T> setter;
        private T value;
        private final T[] values;

        EnumItem(String label, T initial, T[] values, EnumSetter<T> setter) {
            super(label);
            this.value = initial;
            this.values = values;
            this.setter = setter;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.drawString(font, label, x + 4, y + 6, 0xFFEEEEEE, false);
            int valW = 74;
            int valX = x + w - valW - 4;
            boolean hov = mx >= valX && mx <= valX + valW && my >= y + 2 && my <= y + 18;
            int bg = hov ? 0xFF585858 : 0xFF383838;
            g.fill(valX, y + 2, valX + valW, y + 18, bg);
            drawOutline(g, valX, y + 2, valW, 16, 0xFF000000);
            g.drawCenteredString(font, value.name(), valX + valW / 2, y + 6, 0xFFFFFFFF);
        }

        @Override
        boolean click(int mx, int my, int x, int y, int w) {
            int valW = 74;
            int valX = x + w - valW - 4;
            if (mx >= valX && mx <= valX + valW && my >= y + 2 && my <= y + 18) {
                int nextIdx = (value.ordinal() + 1) % values.length;
                value = values[nextIdx];
                setter.set(value);
                return true;
            }
            return false;
        }
    }

    private class IntItem extends ConfigItem {
        interface IntSetter { void set(int val); }
        private final IntSetter setter;
        private int value;
        private final int min, max, step;

        IntItem(String label, int initial, int min, int max, int step, IntSetter setter) {
            super(label);
            this.value = initial;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.drawString(font, label, x + 4, y + 6, 0xFFEEEEEE, false);
            int valW = 60;
            int valX = x + w - valW - 4;
            int btnW = 12;

            boolean hovMinus = mx >= valX && mx <= valX + btnW && my >= y + 2 && my <= y + 18;
            boolean hovPlus = mx >= valX + valW - btnW && mx <= valX + valW && my >= y + 2 && my <= y + 18;

            // Minus Button
            g.fill(valX, y + 2, valX + btnW, y + 18, hovMinus ? 0xFF585858 : 0xFF383838);
            drawOutline(g, valX, y + 2, btnW, 16, 0xFF000000);
            g.drawCenteredString(font, "-", valX + btnW / 2, y + 6, 0xFFFFFFFF);

            // Value text
            g.drawCenteredString(font, String.valueOf(value), valX + valW / 2, y + 6, 0xFFFFFFFF);

            // Plus Button
            g.fill(valX + valW - btnW, y + 2, valX + valW, y + 18, hovPlus ? 0xFF585858 : 0xFF383838);
            drawOutline(g, valX + valW - btnW, y + 2, btnW, 16, 0xFF000000);
            g.drawCenteredString(font, "+", valX + valW - btnW / 2, y + 6, 0xFFFFFFFF);
        }

        @Override
        boolean click(int mx, int my, int x, int y, int w) {
            int valW = 60;
            int valX = x + w - valW - 4;
            int btnW = 12;
            if (mx >= valX && mx <= valX + btnW && my >= y + 2 && my <= y + 18) {
                value = Math.clamp(value - step, min, max);
                setter.set(value);
                return true;
            }
            if (mx >= valX + valW - btnW && mx <= valX + valW && my >= y + 2 && my <= y + 18) {
                value = Math.clamp(value + step, min, max);
                setter.set(value);
                return true;
            }
            return false;
        }
    }

    private class DoubleItem extends ConfigItem {
        interface DoubleSetter { void set(double val); }
        private final DoubleSetter setter;
        private double value;
        private final double min, max, step;

        DoubleItem(String label, double initial, double min, double max, double step, DoubleSetter setter) {
            super(label);
            this.value = initial;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
        }

        @Override
        void render(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.drawString(font, label, x + 4, y + 6, 0xFFEEEEEE, false);
            int valW = 60;
            int valX = x + w - valW - 4;
            int btnW = 12;

            boolean hovMinus = mx >= valX && mx <= valX + btnW && my >= y + 2 && my <= y + 18;
            boolean hovPlus = mx >= valX + valW - btnW && mx <= valX + valW && my >= y + 2 && my <= y + 18;

            // Minus Button
            g.fill(valX, y + 2, valX + btnW, y + 18, hovMinus ? 0xFF585858 : 0xFF383838);
            drawOutline(g, valX, y + 2, btnW, 16, 0xFF000000);
            g.drawCenteredString(font, "-", valX + btnW / 2, y + 6, 0xFFFFFFFF);

            // Value text formatted as %
            int pct = (int) Math.round(value * 100);
            g.drawCenteredString(font, pct + "%", valX + valW / 2, y + 6, 0xFFFFFFFF);

            // Plus Button
            g.fill(valX + valW - btnW, y + 2, valX + valW, y + 18, hovPlus ? 0xFF585858 : 0xFF383838);
            drawOutline(g, valX + valW - btnW, y + 2, btnW, 16, 0xFF000000);
            g.drawCenteredString(font, "+", valX + valW - btnW / 2, y + 6, 0xFFFFFFFF);
        }

        @Override
        boolean click(int mx, int my, int x, int y, int w) {
            int valW = 60;
            int valX = x + w - valW - 4;
            int btnW = 12;
            if (mx >= valX && mx <= valX + btnW && my >= y + 2 && my <= y + 18) {
                value = Math.clamp(value - step, min, max);
                value = Math.round(value * 100.0) / 100.0;
                setter.set(value);
                return true;
            }
            if (mx >= valX + valW - btnW && mx <= valX + valW && my >= y + 2 && my <= y + 18) {
                value = Math.clamp(value + step, min, max);
                value = Math.round(value * 100.0) / 100.0;
                setter.set(value);
                return true;
            }
            return false;
        }
    }
}
