package cz.maxtechnik.opm.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InspectorScreen extends Screen {

    private static final int COLOR_BG        = 0xF0222222;
    private static final int COLOR_HEADER_BG = 0xFF1A1A1A;
    private static final int COLOR_BOX_BG    = 0xFF2D2D2D;
    private static final int COLOR_TOOLBAR   = 0xFF1E1E1E;
    private static final int COLOR_BTN       = 0xFF3A3A3A;
    private static final int COLOR_BTN_HOVER = 0xFF4A4A4A;
    private static final int COLOR_SEARCH_BG = 0xFF333333;
    private static final int COLOR_BORDER    = 0xFF000000;
    private static final int COLOR_TEXT      = 0xFFDDDDDD;
    private static final int COLOR_LABEL     = 0xFF888888;
    private static final int COLOR_SELECTION = 0x553399FF;
    private static final int ICON_SIZE       = 32;
    private static final int LINE_HEIGHT     = 10;
    private static final int TOOLBAR_H       = 22;

    private final ItemStack stack;
    private final Screen parentScreen;
    private final String itemId;
    private final String modName;
    private final String componentText;
    private List<String> componentLines; // inicializuje se v init() kdy máme font

    private int scrollOffset = 0;

    private boolean hoverItemName = false;
    private boolean hoverModName  = false;
    private boolean hoverGiveId   = false;

    private int btnCopyX, btnCopyY;
    private final int btnCopyW = 40;
    private int btnGiveX, btnGiveY;
    private final int btnGiveW = 60;
    private boolean hoverCopy = false;
    private boolean hoverGive = false;

    private String searchQuery = "";
    private final List<Integer> searchMatches = new ArrayList<>();
    private int searchMatchIndex = 0;
    private int searchCursorPos = 0;
    private boolean searchFocused = false;
    private int searchX, searchY, searchW;
    private final int searchH = 16;
    private boolean hoverPrevArrow = false;
    private boolean hoverNextArrow = false;

    private int selectionStart = -1;
    private int selectionEnd   = -1;

    private boolean isDraggingScrollbar = false;

    private String copyFeedback = null;
    private long copyFeedbackUntil = 0;
    private int copyFeedbackX, copyFeedbackY;

    private int panelX, panelY, panelW, panelH, headerH, toolbarY;
    private int boxX, boxY, boxW, boxH;

    public InspectorScreen(ItemStack stack, Screen parentScreen) {
        super(Component.literal("Item Inspector"));
        this.stack        = stack;
        this.parentScreen = parentScreen;

        ResourceLocation itemLoc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        this.itemId = itemLoc.toString();
        String modId = itemLoc.getNamespace();

        String modNameTemp = modId;
        try {
            var modInfo = net.neoforged.fml.ModList.get().getModContainerById(modId);
            if (modInfo.isPresent()) modNameTemp = modInfo.get().getModInfo().getDisplayName();
        } catch (Exception ignored) {}
        this.modName = modNameTemp;

        this.componentText = buildComponentText(stack);
        // componentLines se inicializuje v init() kdy je dostupný font a boxW
    }

    private String buildComponentText(ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        DataComponentMap components = stack.getComponents();
        if (components.isEmpty()) return "(no components)";
        sb.append("[\n");
        components.forEach(c -> sb.append("  ")
                .append(c.type())
                .append(" = ")
                .append(c.value())
                .append(",\n"));
        if (sb.length() > 2) { sb.setLength(sb.length() - 2); sb.append("\n"); }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Rozdělí text na řádky a zalomí dlouhé řádky podle šířky boxu.
     * Vyžaduje font a boxW — volej až po recalcGeometry().
     */
    private List<String> buildLines(String text) {
        List<String> result = new ArrayList<>();
        int maxW = boxW - 16; // šířka boxu minus padding

        for (String line : text.split("\n")) {
            if (font.width(line) <= maxW) {
                result.add(line);
                continue;
            }
            // Zalom dlouhý řádek (rychlé O(N) zalamování s respektováním slov)
            int indentChars = 0;
            while (indentChars < line.length() && line.charAt(indentChars) == ' ') indentChars++;
            String indent = "  " + " ".repeat(indentChars);

            String remaining = line;
            boolean first = true;
            while (!remaining.isEmpty()) {
                int lineMaxW = first ? maxW : maxW - font.width(indent);
                
                // Rychlé zjištění, kolik znaků se vejde
                String fit = font.plainSubstrByWidth(remaining, lineMaxW);
                int chars = fit.length();
                
                // Pokud se řádek nevejde celý, zkusíme najít lepší místo k zalomení
                if (chars < remaining.length()) {
                    int breakPos = -1;
                    // Hledáme odzadu nějaký logický znak pro zalomení (mezera, čárka, dvojtečka, závorka)
                    for (int i = chars - 1; i > 0; i--) {
                        char c = fit.charAt(i);
                        if (c == ' ' || c == ',' || c == ':' || c == '{' || c == '[' || c == '}' || c == ']') {
                            breakPos = i;
                            break;
                        }
                    }
                    // Pokud jsme našli místo, zalomíme hned za ním
                    if (breakPos > 0) {
                        chars = breakPos + 1;
                    }
                }
                
                if (chars == 0) chars = 1; // pojistka
                
                result.add(first ? remaining.substring(0, chars) : indent + remaining.substring(0, chars));
                remaining = remaining.substring(chars);
                first = false;
            }
        }
        return result;
    }

    private void updateSearchMatches() {
        searchMatches.clear();
        searchMatchIndex = 0;
        if (searchQuery.isBlank()) return;
        String lower = searchQuery.toLowerCase();
        for (int i = 0; i < componentLines.size(); i++) {
            if (componentLines.get(i).toLowerCase().contains(lower)) searchMatches.add(i);
        }
        if (!searchMatches.isEmpty()) scrollToMatch(0);
    }

    private void scrollToMatch(int idx) {
        if (searchMatches.isEmpty()) return;
        searchMatchIndex = Math.clamp(idx, 0, searchMatches.size() - 1);
        int targetLine   = searchMatches.get(searchMatchIndex);
        int visibleLines = boxH > 0 ? boxH / LINE_HEIGHT : 20;
        scrollOffset = Math.max(0, targetLine - visibleLines / 2);
    }

    @Override
    protected void init() {
        super.init();
        recalcGeometry();
        // Inicializuj řádky až zde kdy máme font a boxW
        componentLines = buildLines(componentText);
        // Reset search při resize
        if (!searchQuery.isEmpty()) updateSearchMatches();
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

        int arrowW  = 12;
        int countW  = 36;
        int arrowsW = arrowW * 2 + 4;
        searchX = btnGiveX + btnGiveW + 6;
        searchW = panelX + panelW - searchX - countW - arrowsW - 8;
        searchY = toolbarContentY;

        boxX = panelX + 6;
        boxY = toolbarY + TOOLBAR_H + 4;
        boxW = panelW - 12;
        boxH = panelY + panelH - boxY - 6;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (componentLines == null) return; // ještě není inicializováno

        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Panel
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COLOR_BORDER);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_BG);

        // Header
        graphics.fill(panelX, panelY, panelX + panelW, panelY + headerH, COLOR_HEADER_BG);
        graphics.fill(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, COLOR_BORDER);

        // Ikona
        int iconX = panelX + 8;
        int iconY = panelY + (headerH - ICON_SIZE) / 2;
        graphics.renderItem(stack, iconX, iconY);
        graphics.renderItemDecorations(this.font, stack, iconX, iconY);

        // Texty header
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

        // Toolbar
        graphics.fill(panelX, toolbarY, panelX + panelW, toolbarY + TOOLBAR_H, COLOR_TOOLBAR);
        graphics.fill(panelX, toolbarY + TOOLBAR_H, panelX + panelW, toolbarY + TOOLBAR_H + 1, COLOR_BORDER);

        int btnH = 16;
        hoverCopy = inBounds(mouseX, mouseY, btnCopyX, btnCopyY, btnCopyW, btnH);
        graphics.fill(btnCopyX, btnCopyY, btnCopyX + btnCopyW, btnCopyY + btnH,
                hoverCopy ? COLOR_BTN_HOVER : COLOR_BTN);
        graphics.drawCenteredString(font, "Copy", btnCopyX + btnCopyW / 2, btnCopyY + 4, COLOR_TEXT);

        hoverGive = inBounds(mouseX, mouseY, btnGiveX, btnGiveY, btnGiveW, btnH);
        graphics.fill(btnGiveX, btnGiveY, btnGiveX + btnGiveW, btnGiveY + btnH,
                hoverGive ? COLOR_BTN_HOVER : COLOR_BTN);
        graphics.drawCenteredString(font, "Copy Give", btnGiveX + btnGiveW / 2, btnGiveY + 4, COLOR_TEXT);

        // Search box
        graphics.fill(searchX - 1, searchY - 1, searchX + searchW + 1, searchY + searchH + 1, COLOR_BORDER);
        graphics.fill(searchX, searchY, searchX + searchW, searchY + searchH,
                searchFocused ? 0xFF3D3D3D : COLOR_SEARCH_BG);
        if (searchQuery.isEmpty() && !searchFocused) {
            graphics.drawString(font, "Search...", searchX + 3, searchY + 4, 0xFF666666, false);
        } else {
            graphics.enableScissor(searchX + 1, searchY, searchX + searchW - 1, searchY + searchH);
            graphics.drawString(font, searchQuery, searchX + 3, searchY + 4, COLOR_TEXT, false);
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cursorX = searchX + 3 + font.width(
                        searchQuery.substring(0, Math.min(searchCursorPos, searchQuery.length())));
                graphics.fill(cursorX, searchY + 3, cursorX + 1, searchY + 13, COLOR_TEXT);
            }
            graphics.disableScissor();
        }

        // Počet shod + šipky
        int countX = searchX + searchW + 2;
        int arrowW  = 12;
        int prevX   = countX + 36;
        int nextX   = prevX + arrowW + 2;

        if (!searchMatches.isEmpty()) {
            graphics.drawString(font, (searchMatchIndex + 1) + "/" + searchMatches.size(),
                    countX, searchY + 4, 0xFF888888, false);
        }
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

        // Component box
        graphics.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, COLOR_BORDER);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, COLOR_BOX_BG);

        int visibleLines = boxH / LINE_HEIGHT;
        int maxScroll = Math.max(0, componentLines.size() - visibleLines);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

        int selFrom = (selectionStart >= 0 && selectionEnd >= 0)
                ? Math.min(selectionStart, selectionEnd) : -1;
        int selTo   = (selectionStart >= 0 && selectionEnd >= 0)
                ? Math.max(selectionStart, selectionEnd) : -1;
        int highlightedLine = searchMatches.isEmpty() ? -1
                : searchMatches.get(Math.min(searchMatchIndex, searchMatches.size() - 1));

        graphics.enableScissor(boxX + 2, boxY + 2, boxX + boxW - 6, boxY + boxH - 2);
        int lineY = boxY + 3;
        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleLines + 1, componentLines.size()); i++) {
            String line = componentLines.get(i);
            if (selFrom >= 0 && i >= selFrom && i <= selTo) {
                graphics.fill(boxX + 2, lineY - 1, boxX + boxW - 6, lineY + LINE_HEIGHT - 1, COLOR_SELECTION);
            }
            if (i == highlightedLine) {
                graphics.fill(boxX + 2, lineY - 1, boxX + boxW - 6, lineY + LINE_HEIGHT - 1, 0x553A3A1A);
            }
            drawSyntaxLine(graphics, line, boxX + 4, lineY, searchQuery);
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

        // Copy feedback
        if (copyFeedback != null && System.currentTimeMillis() < copyFeedbackUntil) {
            graphics.drawString(font, copyFeedback, copyFeedbackX, copyFeedbackY, 0xFFFFFF00, true);
        } else {
            copyFeedback = null;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (button == 0) {
            if (hoverItemName) { copyToClipboard(stack.getHoverName().getString(), mx, my); return true; }
            if (hoverModName)  { copyToClipboard(modName, mx, my); return true; }
            if (hoverGiveId)   { copyToClipboard(itemId, mx, my); return true; }

            if (hoverCopy) {
                if (selectionStart >= 0 && selectionEnd >= 0) copySelection(mx, my);
                else copyToClipboard(componentText, mx, my);
                return true;
            }
            if (hoverGive) { copyToClipboard("/give @s " + itemId, mx, my); return true; }

            if (hoverPrevArrow && searchMatches.size() > 1) { scrollToMatch(searchMatchIndex - 1); return true; }
            if (hoverNextArrow && searchMatches.size() > 1) { scrollToMatch(searchMatchIndex + 1); return true; }

            if (inBounds(mx, my, searchX, searchY, searchW, searchH)) {
                searchFocused = true;
                searchCursorPos = searchQuery.length();
                return true;
            }
            searchFocused = false;

            // Scrollbar track click
            int scrollbarX = boxX + boxW - 8;
            if (componentLines.size() > boxH / LINE_HEIGHT && mx >= scrollbarX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH) {
                isDraggingScrollbar = true;
                updateScrollFromMouse(my);
                return true;
            }

            int lineIdx = getLineAtY(my);
            if (lineIdx >= 0) {
                if (hasShiftDown() && selectionStart >= 0) {
                    selectionEnd = lineIdx;
                } else if (hasControlDown()) {
                    if (selectionStart == lineIdx && selectionEnd == lineIdx) {
                        selectionStart = -1; selectionEnd = -1;
                    } else if (selectionStart < 0) {
                        selectionStart = lineIdx; selectionEnd = lineIdx;
                    } else {
                        selectionStart = Math.min(selectionStart, lineIdx);
                        selectionEnd   = Math.max(selectionEnd, lineIdx);
                    }
                } else {
                    selectionStart = lineIdx; selectionEnd = lineIdx;
                }
                return true;
            } else if (!inBounds(mx, my, boxX, boxY, boxW, boxH)) {
                selectionStart = -1; selectionEnd = -1;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(int my) {
        int visibleLines = boxH / LINE_HEIGHT;
        int maxScroll = Math.max(0, componentLines.size() - visibleLines);
        if (maxScroll > 0) {
            int thumbH = Math.max(20, boxH * visibleLines / componentLines.size());
            int trackH = boxH - thumbH;
            if (trackH > 0) {
                double scrollFraction = (my - boxY - thumbH / 2.0) / trackH;
                scrollOffset = (int) Math.round(scrollFraction * maxScroll);
                scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse((int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 67 && (modifiers & 2) != 0) {
            if (selectionStart >= 0 && selectionEnd >= 0) {
                copySelection(this.width / 2, this.height / 2);
                return true;
            }
        }
        if (searchFocused) {
            if (keyCode == 259 && !searchQuery.isEmpty() && searchCursorPos > 0) {
                searchQuery = searchQuery.substring(0, searchCursorPos - 1) + searchQuery.substring(searchCursorPos);
                searchCursorPos--;
                updateSearchMatches();
            } else if (keyCode == 261 && searchCursorPos < searchQuery.length()) {
                searchQuery = searchQuery.substring(0, searchCursorPos) + searchQuery.substring(searchCursorPos + 1);
                updateSearchMatches();
            } else if (keyCode == 263) { searchCursorPos = Math.max(0, searchCursorPos - 1);
            } else if (keyCode == 262) { searchCursorPos = Math.min(searchQuery.length(), searchCursorPos + 1);
            } else if (keyCode == 256) { searchFocused = false;
            } else if ((keyCode == 257 || keyCode == 335) && !searchMatches.isEmpty()) {
                scrollToMatch(searchMatchIndex + 1);
            }
            return true;
        }
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            searchQuery = searchQuery.substring(0, searchCursorPos) + chr + searchQuery.substring(searchCursorPos);
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
        assert this.minecraft != null;
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void copyToClipboard(String text, int mx, int my) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        copyFeedback = "Copied!";
        copyFeedbackX = mx + 8;
        copyFeedbackY = my - 10;
        copyFeedbackUntil = System.currentTimeMillis() + 1200;
    }

    private void copySelection(int mx, int my) {
        if (selectionStart < 0 || selectionEnd < 0) return;
        int from = Math.min(selectionStart, selectionEnd);
        int to   = Math.max(selectionStart, selectionEnd);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to && i < componentLines.size(); i++) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(componentLines.get(i));
        }
        copyToClipboard(sb.toString(), mx, my);
    }

    private void drawSyntaxLine(GuiGraphics graphics, String line, int x, int y, String query) {
        int currentX = x;
        int len = line.length();
        boolean inString = false;
        char stringChar = 0;
        
        StringBuilder token = new StringBuilder();
        
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            
            if (inString) {
                token.append(c);
                if (c == stringChar && (i == 0 || line.charAt(i-1) != '\\')) {
                    inString = false;
                    graphics.drawString(font, token.toString(), currentX, y, 0xFFCE9178, false);
                    currentX += font.width(token.toString());
                    token.setLength(0);
                }
            } else {
                if (c == '"' || c == '\'') {
                    if (!token.isEmpty()) {
                        int cColor = getColorForToken(token.toString().trim(), line, i);
                        graphics.drawString(font, token.toString(), currentX, y, cColor, false);
                        currentX += font.width(token.toString());
                        token.setLength(0);
                    }
                    inString = true;
                    stringChar = c;
                    token.append(c);
                } else if (c == ':' || c == '{' || c == '}' || c == '[' || c == ']' || c == ',') {
                    if (!token.isEmpty()) {
                        int cColor = getColorForToken(token.toString().trim(), line, i);
                        graphics.drawString(font, token.toString(), currentX, y, cColor, false);
                        currentX += font.width(token.toString());
                        token.setLength(0);
                    }
                    int symColor = (c == '{' || c == '}') ? 0xFFFFD700 : ((c == '[' || c == ']') ? 0xFFDA70D6 : 0xFF808080);
                    graphics.drawString(font, String.valueOf(c), currentX, y, symColor, false);
                    currentX += font.width(String.valueOf(c));
                } else {
                    token.append(c);
                }
            }
        }
        
        if (!token.isEmpty()) {
            int cColor = inString ? 0xFFCE9178 : getColorForToken(token.toString().trim(), line, len);
            graphics.drawString(font, token.toString(), currentX, y, cColor, false);
        }
        
        if (!query.isBlank()) {
            String lower = line.toLowerCase();
            String lowerQ = query.toLowerCase();
            int idx = lower.indexOf(lowerQ);
            if (idx >= 0) {
                int pxBefore = font.width(line.substring(0, idx));
                int pxMatch = font.width(line.substring(idx, idx + query.length()));
                graphics.fill(x + pxBefore - 1, y - 1, x + pxBefore + pxMatch + 1, y + LINE_HEIGHT - 1, 0x55FFFF00);
            }
        }
    }

    private int getColorForToken(String t, String fullLine, int endIdx) {
        if (t.isEmpty()) return 0xFFD4D4D4;
        if (t.matches("-?\\d+(\\.\\d+)?[a-zA-Z]?")) return 0xFFB5CEA8; // Zelená (čísla)
        if (t.equals("true") || t.equals("false")) return 0xFF569CD6; // Modrá (booleans)
        return 0xFF9CDCFE; // Světle modrá (klíče/proměnné)
    }

    private int getLineAtY(int my) {
        if (my < boxY || my >= boxY + boxH) return -1;
        int lineIdx = scrollOffset + (my - boxY - 3) / LINE_HEIGHT;
        if (lineIdx < 0 || lineIdx >= componentLines.size()) return -1;
        return lineIdx;
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