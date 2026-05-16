package cz.maxtechnik.opm.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final int COLOR_TOOLBAR   = 0xFF1E1E1E;
    private static final int COLOR_BTN       = 0xFF3A3A3A; // světlejší než pozadí
    private static final int COLOR_BTN_HOVER = 0xFF4A4A4A;
    private static final int COLOR_SEARCH_BG = 0xFF333333;
    private static final int COLOR_BORDER    = 0xFF000000;
    private static final int COLOR_TEXT      = 0xFFDDDDDD;
    private static final int COLOR_LABEL     = 0xFF888888;
    private static final int ICON_SIZE       = 32;
    private static final int LINE_HEIGHT     = 10;
    private static final int TOOLBAR_H       = 22;

    private final ItemStack stack;
    private final Screen parentScreen; // předchozí screen (inventář)
    private final String itemId;
    private final String modId;
    private final String modName;
    private final String componentText;
    private final List<String> componentLines;

    private int scrollOffset = 0;

    private boolean hoverItemName = false;
    private boolean hoverModName  = false;
    private boolean hoverGiveId   = false;

    // Toolbar tlačítka (custom — bez vanilla textury)
    private int btnCopyX, btnCopyY, btnCopyW = 40, btnCopyH = 16;
    private int btnGiveX, btnGiveY, btnGiveW = 60, btnGiveH = 16;
    private boolean hoverCopy = false;
    private boolean hoverGive = false;

    // Search
    private String searchQuery = "";
    private List<Integer> searchMatches = new ArrayList<>(); // indexy řádků kde je shoda
    private int searchMatchIndex = 0; // aktuálně vybraná shoda
    private int searchCursorPos = 0;
    private boolean searchFocused = false;
    private int searchX, searchY, searchW, searchH = 16;
    private boolean hoverPrevArrow = false;
    private boolean hoverNextArrow = false;

    // Copy feedback
    private String copyFeedback = null;
    private long copyFeedbackUntil = 0;
    private int copyFeedbackX, copyFeedbackY;

    // Panel geometrie
    private int panelX, panelY, panelW, panelH, headerH, toolbarY, boxX, boxY, boxW, boxH;

    public InspectorScreen(ItemStack stack, Screen parentScreen) {
        super(Component.literal("Item Inspector"));
        this.stack        = stack;
        this.parentScreen = parentScreen;

        ResourceLocation itemLoc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        this.itemId = itemLoc.toString();
        this.modId  = itemLoc.getNamespace();

        String modNameTemp = modId;
        try {
            var modInfo = net.neoforged.fml.ModList.get().getModContainerById(modId);
            if (modInfo.isPresent()) modNameTemp = modInfo.get().getModInfo().getDisplayName();
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
        if (sb.length() > 2) { sb.setLength(sb.length() - 2); sb.append("\n"); }
        sb.append("]");
        return sb.toString();
    }

    private List<String> buildLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) lines.add(line);
        return lines;
    }

    /** Přepočítá shody pro aktuální searchQuery */
    private void updateSearchMatches() {
        searchMatches.clear();
        searchMatchIndex = 0;
        if (searchQuery.isBlank()) return;
        String lower = searchQuery.toLowerCase();
        for (int i = 0; i < componentLines.size(); i++) {
            if (componentLines.get(i).toLowerCase().contains(lower)) {
                searchMatches.add(i);
            }
        }
        // Naskroluj na první shodu
        if (!searchMatches.isEmpty()) {
            scrollToMatch(0);
        }
    }

    private void scrollToMatch(int idx) {
        if (searchMatches.isEmpty()) return;
        searchMatchIndex = Math.max(0, Math.min(idx, searchMatches.size() - 1));
        int targetLine = searchMatches.get(searchMatchIndex);
        int visibleLines = boxH > 0 ? boxH / LINE_HEIGHT : 20;
        // Vycentruj pokud možno
        scrollOffset = Math.max(0, targetLine - visibleLines / 2);
    }

    @Override
    protected void init() {
        super.init();
        recalcGeometry();
    }

    private void recalcGeometry() {
        panelW   = Math.min(500, this.width - 40);
        panelH   = this.height - 60;
        panelX   = (this.width - panelW) / 2;
        panelY   = 20;
        headerH  = ICON_SIZE + 16;
        toolbarY = panelY + headerH + 1;

        int toolbarContentY = toolbarY + (TOOLBAR_H - 16) / 2;

        btnCopyX = panelX + 4;
        btnCopyY = toolbarContentY;

        btnGiveX = btnCopyX + btnCopyW + 4;
        btnGiveY = toolbarContentY;

        // Search začíná za Give tlačítkem, šipky na konci
        int arrowW  = 12;
        int arrowGap = 2;
        searchX = btnGiveX + btnGiveW + 6;
        searchW = panelX + panelW - searchX - arrowW * 2 - arrowGap * 2 - 4;
        searchY = toolbarContentY;

        boxX = panelX + 6;
        boxY = toolbarY + TOOLBAR_H + 4;
        boxW = panelW - 12;
        boxH = panelY + panelH - boxY - 6;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // === HLAVNÍ PANEL ===
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COLOR_BORDER);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_BG);

        // === HEADER ===
        graphics.fill(panelX, panelY, panelX + panelW, panelY + headerH, COLOR_HEADER_BG);
        graphics.fill(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, COLOR_BORDER);

        // Ikona 32px
        int iconX = panelX + 8;
        int iconY = panelY + (headerH - ICON_SIZE) / 2;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(iconX, iconY, 0);
        pose.scale(2.0f, 2.0f, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(this.font, stack, 0, 0);
        pose.popPose();

        // Texty
        int textStartX = iconX + ICON_SIZE + 10;
        int maxTextW   = panelX + panelW - textStartX - 8;
        int textY      = panelY + 10;

        String truncName = truncate(stack.getHoverName().getString(), maxTextW);
        hoverItemName = inBounds(mouseX, mouseY, textStartX, textY, font.width(truncName), 9);
        graphics.drawString(font, truncName, textStartX, textY,
                hoverItemName ? 0xFFFFFFFF : COLOR_TEXT, false);
        if (hoverItemName) graphics.fill(textStartX, textY + 9, textStartX + font.width(truncName), textY + 10, 0xFFAAAAAA);

        textY += 14;
        String truncMod = truncate(modName, maxTextW);
        hoverModName = inBounds(mouseX, mouseY, textStartX, textY, font.width(truncMod), 9);
        graphics.drawString(font, truncMod, textStartX, textY,
                hoverModName ? 0xFFCCCCCC : COLOR_LABEL, false);
        if (hoverModName) graphics.fill(textStartX, textY + 9, textStartX + font.width(truncMod), textY + 10, 0xFF666666);

        textY += 14;
        String truncId = truncate(itemId, maxTextW);
        hoverGiveId = inBounds(mouseX, mouseY, textStartX, textY, font.width(truncId), 9);
        graphics.drawString(font, truncId, textStartX, textY,
                hoverGiveId ? 0xFF88FF88 : 0xFF55AA55, false);
        if (hoverGiveId) graphics.fill(textStartX, textY + 9, textStartX + font.width(truncId), textY + 10, 0xFF55AA55);

        // === TOOLBAR ===
        graphics.fill(panelX, toolbarY, panelX + panelW, toolbarY + TOOLBAR_H, COLOR_TOOLBAR);
        graphics.fill(panelX, toolbarY + TOOLBAR_H, panelX + panelW, toolbarY + TOOLBAR_H + 1, COLOR_BORDER);

        // Copy button
        hoverCopy = inBounds(mouseX, mouseY, btnCopyX, btnCopyY, btnCopyW, btnCopyH);
        graphics.fill(btnCopyX, btnCopyY, btnCopyX + btnCopyW, btnCopyY + btnCopyH,
                hoverCopy ? COLOR_BTN_HOVER : COLOR_BTN);
        graphics.drawCenteredString(font, "Copy",
                btnCopyX + btnCopyW / 2, btnCopyY + 4, COLOR_TEXT);

        // Copy Give button
        hoverGive = inBounds(mouseX, mouseY, btnGiveX, btnGiveY, btnGiveW, btnGiveH);
        graphics.fill(btnGiveX, btnGiveY, btnGiveX + btnGiveW, btnGiveY + btnGiveH,
                hoverGive ? COLOR_BTN_HOVER : COLOR_BTN);
        graphics.drawCenteredString(font, "Copy Give",
                btnGiveX + btnGiveW / 2, btnGiveY + 4, COLOR_TEXT);

        // Search box
        int searchBgColor = searchFocused ? 0xFF3D3D3D : COLOR_SEARCH_BG;
        graphics.fill(searchX - 1, searchY - 1, searchX + searchW + 1, searchY + searchH + 1, COLOR_BORDER);
        graphics.fill(searchX, searchY, searchX + searchW, searchY + searchH, searchBgColor);
        // Text v search boxu
        String displaySearch = searchQuery.isEmpty() ? "" : searchQuery;
        if (searchQuery.isEmpty() && !searchFocused) {
            graphics.drawString(font, "Search...", searchX + 3, searchY + 4, 0xFF666666, false);
        } else {
            graphics.drawString(font, displaySearch, searchX + 3, searchY + 4, COLOR_TEXT, false);
            // Kurzor
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = searchX + 3 + font.width(searchQuery.substring(0, Math.min(searchCursorPos, searchQuery.length())));
                graphics.fill(cursorX, searchY + 3, cursorX + 1, searchY + 13, COLOR_TEXT);
            }
        }

        // Počet shod
        int arrowBaseX = searchX + searchW + 2;
        if (!searchMatches.isEmpty()) {
            String countStr = (searchMatchIndex + 1) + "/" + searchMatches.size();
            graphics.drawString(font, countStr, arrowBaseX, searchY + 4, 0xFF888888, false);
        }

        // Šipky (pouze pokud je více shod)
        int arrowW = 12;
        int prevX = arrowBaseX + 22;
        int nextX = prevX + arrowW + 2;
        if (searchMatches.size() > 1) {
            hoverPrevArrow = inBounds(mouseX, mouseY, prevX, searchY, arrowW, searchH);
            hoverNextArrow = inBounds(mouseX, mouseY, nextX, searchY, arrowW, searchH);
            graphics.fill(prevX, searchY, prevX + arrowW, searchY + searchH,
                    hoverPrevArrow ? COLOR_BTN_HOVER : COLOR_BTN);
            graphics.fill(nextX, searchY, nextX + arrowW, searchY + searchH,
                    hoverNextArrow ? COLOR_BTN_HOVER : COLOR_BTN);
            graphics.drawCenteredString(font, "<", prevX + arrowW / 2, searchY + 4, COLOR_TEXT);
            graphics.drawCenteredString(font, ">", nextX + arrowW / 2, searchY + 4, COLOR_TEXT);
        }

        // === COMPONENT BOX ===
        graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, COLOR_BORDER);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_BOX_BG);

        int visibleLines = boxH / LINE_HEIGHT;
        int maxScroll = Math.max(0, componentLines.size() - visibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(boxX + 2, boxY + 2, boxX + boxW - 6, boxY + boxH - 2);
        int lineY = boxY + 3;
        // Aktuálně vybraná shoda
        int highlightedLine = searchMatches.isEmpty() ? -1 : searchMatches.get(
                Math.min(searchMatchIndex, searchMatches.size() - 1));

        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleLines + 1, componentLines.size()); i++) {
            String line = componentLines.get(i);
            // Zvýrazni řádek aktuální shody
            if (i == highlightedLine) {
                graphics.fill(boxX + 2, lineY - 1, boxX + boxW - 6, lineY + LINE_HEIGHT - 1, 0xFF3A3A1A);
            }
            // Zvýrazni hledaný text žlutě
            if (!searchQuery.isBlank()) {
                graphics.drawString(font, highlightQuery(line, searchQuery), boxX + 4, lineY, COLOR_TEXT, false);
            } else {
                graphics.drawString(font, line, boxX + 4, lineY, COLOR_TEXT, false);
            }
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

        // Copy feedback (text u kurzoru)
        if (copyFeedback != null && System.currentTimeMillis() < copyFeedbackUntil) {
            graphics.drawString(font, copyFeedback, copyFeedbackX, copyFeedbackY, 0xFFFFFF00, true);
        } else {
            copyFeedback = null;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void showCopyFeedback(int mx, int my) {
        copyFeedback     = "Copied!";
        copyFeedbackX    = mx + 8;
        copyFeedbackY    = my - 10;
        copyFeedbackUntil = System.currentTimeMillis() + 1200;
    }

    private String highlightQuery(String line, String query) {
        String lower  = line.toLowerCase();
        String lowerQ = query.toLowerCase();
        int idx = lower.indexOf(lowerQ);
        if (idx < 0) return line;
        return line.substring(0, idx)
                + "§e" + line.substring(idx, idx + query.length())
                + "§r" + line.substring(idx + query.length());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (button == 0) {
            Minecraft mc = Minecraft.getInstance();

            // Header klikatelné texty
            if (hoverItemName) { copyToClipboard(stack.getHoverName().getString(), mx, my); return true; }
            if (hoverModName)  { copyToClipboard(modName, mx, my); return true; }
            if (hoverGiveId)   { copyToClipboard(itemId, mx, my); return true; }

            // Toolbar tlačítka
            if (hoverCopy) { copyToClipboard(componentText, mx, my); return true; }
            if (hoverGive) { copyToClipboard("/give @s " + itemId, mx, my); return true; }

            // Šipky
            if (hoverPrevArrow && searchMatches.size() > 1) {
                scrollToMatch(searchMatchIndex - 1);
                return true;
            }
            if (hoverNextArrow && searchMatches.size() > 1) {
                scrollToMatch(searchMatchIndex + 1);
                return true;
            }

            // Search box focus
            searchFocused = inBounds(mx, my, searchX, searchY, searchW, searchH);
            if (searchFocused) {
                searchCursorPos = searchQuery.length();
                return true;
            } else {
                searchFocused = false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void copyToClipboard(String text, int mx, int my) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        showCopyFeedback(mx, my);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == 259) { // Backspace
                if (!searchQuery.isEmpty() && searchCursorPos > 0) {
                    searchQuery = searchQuery.substring(0, searchCursorPos - 1)
                            + searchQuery.substring(searchCursorPos);
                    searchCursorPos--;
                    updateSearchMatches();
                }
                return true;
            }
            if (keyCode == 261) { // Delete
                if (searchCursorPos < searchQuery.length()) {
                    searchQuery = searchQuery.substring(0, searchCursorPos)
                            + searchQuery.substring(searchCursorPos + 1);
                    updateSearchMatches();
                }
                return true;
            }
            if (keyCode == 263) { searchCursorPos = Math.max(0, searchCursorPos - 1); return true; } // Left
            if (keyCode == 262) { searchCursorPos = Math.min(searchQuery.length(), searchCursorPos + 1); return true; } // Right
            if (keyCode == 256) { searchFocused = false; return true; } // Escape
            if (keyCode == 257 || keyCode == 335) { // Enter — přejdi na další shodu
                if (!searchMatches.isEmpty()) scrollToMatch(searchMatchIndex + 1);
                return true;
            }
            return true;
        }
        if (keyCode == 256) { // Escape — zavři a vrať se
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            searchQuery = searchQuery.substring(0, searchCursorPos) + chr
                    + searchQuery.substring(searchCursorPos);
            searchCursorPos++;
            updateSearchMatches();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) scrollY;
        return true;
    }

    @Override
    public void onClose() {
        // Vrátí se na předchozí screen (inventář)
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean inBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (font.width(text + "...") > maxWidth && !text.isEmpty())
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}