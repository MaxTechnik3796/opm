package cz.maxtechnik.opm.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InspectorScreen extends Screen {

    private static final int COLOR_BG        = 0xF0222222;
    private static final int COLOR_HEADER_BG = 0xFF1A1A1A;
    private static final int COLOR_BOX_BG    = 0xFF2D2D2D;
    private static final int COLOR_BORDER    = 0xFF000000; // černý 1px border
    private static final int COLOR_TEXT      = 0xFFDDDDDD;
    private static final int COLOR_LABEL     = 0xFF888888;

    private final ItemStack stack;
    private final String itemId;
    private final String modId;
    private final String modName;
    private final String componentText;
    private final List<String> componentLines;

    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 10;

    private boolean hoverItemName = false;
    private boolean hoverModName  = false;
    private boolean hoverGiveId   = false;

    // Ikona 32px
    private static final int ICON_SIZE = 32;

    public InspectorScreen(ItemStack stack) {
        super(Component.literal("Item Inspector"));
        this.stack = stack;

        ResourceLocation itemLoc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        this.itemId = itemLoc.toString();
        this.modId  = itemLoc.getNamespace();

        String modNameTemp = modId;
        try {
            var modInfo = net.neoforged.fml.ModList.get().getModContainerById(modId);
            if (modInfo.isPresent()) {
                modNameTemp = modInfo.get().getModInfo().getDisplayName();
            }
        } catch (Exception ignored) {}
        this.modName = modNameTemp;

        this.componentText  = buildComponentText(stack);
        this.componentLines = buildLines(componentText);
    }

    private String buildComponentText(ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        DataComponentMap components = stack.getComponents();
        if (components.isEmpty()) return "(no components)";
        sb.append("[\n");
        components.forEach(c -> sb.append("  ")
                .append(c.type().toString())
                .append(" = ")
                .append(c.value().toString())
                .append(",\n"));
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> buildLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            lines.add(line);
        }
        return lines;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = this.width / 2 - 100;
        int btnY = this.height - 28;
        this.addRenderableWidget(Button.builder(
                Component.literal("Copy Components"),
                btn -> this.minecraft.keyboardHandler.setClipboard(componentText)
        ).pos(btnX, btnY).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelW = Math.min(500, this.width - 40);
        int panelH = this.height - 60;
        int panelX = (this.width - panelW) / 2;
        int panelY = 20;

        // Border 1px černý
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COLOR_BORDER);
        // Hlavní panel
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_BG);

        // === HEADER ===
        int headerH = ICON_SIZE + 16;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + headerH, COLOR_HEADER_BG);
        // Spodní border headeru
        graphics.fill(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, COLOR_BORDER);

        // Ikona 32px — pomocí PoseStack scale
        int iconX = panelX + 8;
        int iconY = panelY + 8;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(iconX, iconY, 0);
        pose.scale(2.0f, 2.0f, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(this.font, stack, 0, 0);
        pose.popPose();

        // Texty v headeru
        int textStartX = iconX + ICON_SIZE + 6;
        int maxTextW = panelX + panelW - textStartX - 8; // max šířka textu
        int textY = panelY + 8;

        // Název itemu
        String itemDisplayName = stack.getHoverName().getString();
        String truncatedName = truncate(itemDisplayName, maxTextW);
        hoverItemName = mouseX >= textStartX && mouseX <= textStartX + font.width(truncatedName)
                && mouseY >= textY && mouseY <= textY + 9;
        int nameColor = hoverItemName ? 0xFFFFFFFF : COLOR_TEXT;
        graphics.drawString(font, truncatedName, textStartX, textY, nameColor, false);
        if (hoverItemName) {
            graphics.fill(textStartX, textY + 9, textStartX + font.width(truncatedName), textY + 10, 0xFFAAAAAA);
        }

        // Název modu
        textY += 12;
        String truncatedMod = truncate(modName, maxTextW);
        hoverModName = mouseX >= textStartX && mouseX <= textStartX + font.width(truncatedMod)
                && mouseY >= textY && mouseY <= textY + 9;
        int modColor = hoverModName ? 0xFFCCCCCC : COLOR_LABEL;
        graphics.drawString(font, truncatedMod, textStartX, textY, modColor, false);
        if (hoverModName) {
            graphics.fill(textStartX, textY + 9, textStartX + font.width(truncatedMod), textY + 10, 0xFF666666);
        }

        // Give ID
        textY += 12;
        String truncatedId = truncate(itemId, maxTextW);
        hoverGiveId = mouseX >= textStartX && mouseX <= textStartX + font.width(truncatedId)
                && mouseY >= textY && mouseY <= textY + 9;
        int giveColor = hoverGiveId ? 0xFF88FF88 : 0xFF55AA55;
        graphics.drawString(font, truncatedId, textStartX, textY, giveColor, false);
        if (hoverGiveId) {
            graphics.fill(textStartX, textY + 9, textStartX + font.width(truncatedId), textY + 10, 0xFF55AA55);
        }

        // === COMPONENT BOX ===
        int boxX = panelX + 6;
        int boxY = panelY + headerH + 6;
        int boxW = panelW - 12;
        int boxH = panelH - headerH - 46;

        // Border 1px černý
        graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, COLOR_BORDER);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_BOX_BG);

        int visibleLines = boxH / LINE_HEIGHT;
        int maxScroll = Math.max(0, componentLines.size() - visibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(boxX + 2, boxY + 2, boxX + boxW - 6, boxY + boxH - 2);
        int lineY = boxY + 3;
        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleLines + 1, componentLines.size()); i++) {
            graphics.drawString(font, componentLines.get(i), boxX + 4, lineY, COLOR_TEXT, false);
            lineY += LINE_HEIGHT;
        }
        graphics.disableScissor();

        // Scrollbar
        if (componentLines.size() > visibleLines) {
            int thumbH = Math.max(20, boxH * visibleLines / componentLines.size());
            int thumbY = boxY + (boxH - thumbH) * scrollOffset / Math.max(1, maxScroll);
            graphics.fill(boxX + boxW - 4, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
            graphics.fill(boxX + boxW - 4, thumbY, boxX + boxW, thumbY + thumbH, 0xFF666666);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Ořízne text pokud je delší než maxWidth, přidá "..." */
    private String truncate(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        while (font.width(text + ellipsis) > maxWidth && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (hoverItemName) { mc.keyboardHandler.setClipboard(stack.getHoverName().getString()); return true; }
            if (hoverModName)  { mc.keyboardHandler.setClipboard(modName); return true; }
            if (hoverGiveId)   { mc.keyboardHandler.setClipboard(itemId); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) scrollY;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}