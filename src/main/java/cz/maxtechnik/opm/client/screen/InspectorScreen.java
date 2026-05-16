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
import java.util.Arrays;
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
    private final List<String> componentLines;

    private int scrollOffset = 0;

    private boolean hoverItemName = false;
    private boolean hoverModName  = false;
    private boolean hoverGiveId   = false;

    // Toolbar
    private int btnCopyX;
    private int btnCopyY;
    private final int btnCopyW = 40;
    private int btnGiveX;
    private int btnGiveY;
    private final int btnGiveW = 60;
    private boolean hoverCopy = false;
    private boolean hoverGive = false;

    // Search
    private String searchQuery = "";
    private final List<Integer> searchMatches = new ArrayList<>();
    private int searchMatchIndex = 0;
    private int searchCursorPos = 0;
    private boolean searchFocused = false;
    private int searchX;
    private int searchY;
    private int searchW;
    private final int searchH = 16;
    private boolean hoverPrevArrow = false;
    private boolean hoverNextArrow = false;

    // Výběr řádků
    private int selectionStart = -1; // první označený řádek
    private int selectionEnd   = -1; // poslední označený řádek

    // Copy feedback
    private String copyFeedback = null;
    private long copyFeedbackUntil = 0;
    private int copyFeedbackX, copyFeedbackY;

    // Panel geometrie
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

        this.componentText  = buildComponentText(stack);
        this.componentLines = buildLines(componentText);
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

    private List<String> buildLines(String text) {
        return new ArrayList<>(Arrays.asList(text.split("\n")));
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

        // Search — kratší, nechá místo pro 999/999 + šipky
        int arrowW   = 12;
        int countW   = 36; // místo pro "999/999"
        int arrowsW  = arrowW * 2 + 4;
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
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Panel
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COLOR_BORDER);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_BG);

        // Header
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

        int btnCopyH = 16;
        hoverCopy = inBounds(mouseX, mouseY, btnCopyX, btnCopyY, btnCopyW, btnCopyH);
        graphics.fill(btnCopyX, btnCopyY, btnCopyX + btnCopyW, btnCopyY + btnCopyH,
                hoverCopy ? COLOR_BTN_HOVER : COLOR_BTN);
        graphics.drawCenteredString(font, "Copy", btnCopyX + btnCopyW / 2, btnCopyY + 4, COLOR_TEXT);

        int btnGiveH = 16;
        hoverGive = inBounds(mouseX, mouseY, btnGiveX, btnGiveY, btnGiveW, btnGiveH);
        graphics.fill(btnGiveX, btnGiveY, btnGiveX + btnGiveW, btnGiveY + btnGiveH,
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
                int cursorX = searchX + 3 + font.width(searchQuery.substring(0, Math.min(searchCursorPos, searchQuery.length())));
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
            String countStr = (searchMatchIndex + 1) + "/" + searchMatches.size();
            graphics.drawString(font, countStr, countX, searchY + 4, 0xFF888888, false);
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

        int selFrom = selectionStart >= 0 && selectionEnd >= 0
                ? Math.min(selectionStart, selectionEnd) : -1;
        int selTo   = selectionStart >= 0 && selectionEnd >= 0
                ? Math.max(selectionStart, selectionEnd) : -1;
        int highlightedLine = searchMatches.isEmpty() ? -1
                : searchMatches.get(Math.min(searchMatchIndex, searchMatches.size() - 1));

        graphics.enableScissor(boxX + 2, boxY + 2, boxX + boxW - 6, boxY + boxH - 2);
        int lineY = boxY + 3;
        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleLines + 1, componentLines.size()); i++) {
            String line = componentLines.get(i);

            // Výběr řádků (modrý highlight)
            if (selFrom >= 0 && i >= selFrom && i <= selTo) {
                graphics.fill(boxX + 2, lineY - 1, boxX + boxW - 6, lineY + LINE_HEIGHT - 1, COLOR_SELECTION);
            }
            // Search highlight (žlutý řádek)
            if (i == highlightedLine) {
                graphics.fill(boxX + 2, lineY - 1, boxX + boxW - 6, lineY + LINE_HEIGHT - 1, 0x553A3A1A);
            }

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

            // Header
            if (hoverItemName) { copyToClipboard(stack.getHoverName().getString(), mx, my); return true; }
            if (hoverModName)  { copyToClipboard(modName, mx, my); return true; }
            if (hoverGiveId)   { copyToClipboard(itemId, mx, my); return true; }

            // Toolbar
            if (hoverCopy) {
                // Pokud je výběr, zkopíruj výběr; jinak vše
                if (selectionStart >= 0 && selectionEnd >= 0) {
                    copySelection(mx, my);
                } else {
                    copyToClipboard(componentText, mx, my);
                }
                return true;
            }
            if (hoverGive) { copyToClipboard("/give @s " + itemId, mx, my); return true; }

            // Šipky search
            if (hoverPrevArrow && searchMatches.size() > 1) { scrollToMatch(searchMatchIndex - 1); return true; }
            if (hoverNextArrow && searchMatches.size() > 1) { scrollToMatch(searchMatchIndex + 1); return true; }

            // Search box focus
            if (inBounds(mx, my, searchX, searchY, searchW, searchH)) {
                searchFocused = true;
                searchCursorPos = searchQuery.length();
                return true;
            }
            searchFocused = false;

            // Klik na řádek v component boxu
            int lineIdx = getLineAtY(my);
            if (lineIdx >= 0) {
                boolean shift = hasShiftDown();
                boolean ctrl  = hasControlDown();

                if (shift && selectionStart >= 0) {
                    // Shift+klik — rozšíří výběr od posledního kliknutého řádku
                    selectionEnd = lineIdx;
                } else if (ctrl) {
                    // Ctrl+klik — přidá/odebere jednotlivý řádek
                    // Použijeme selectionStart/End jako "poslední ctrl klik"
                    // a druhý pár pro multi-selection
                    if (selectionStart == lineIdx && selectionEnd == lineIdx) {
                        // Klik na už označený → odznačí
                        selectionStart = -1;
                        selectionEnd   = -1;
                    } else if (selectionStart < 0) {
                        // Nic není označené → označ
                        selectionStart = lineIdx;
                        selectionEnd   = lineIdx;
                    } else {
                        // Něco je označené → přidej tento řádek
                        // Rozšíříme výběr aby zahrnoval oba
                        selectionStart = Math.min(selectionStart, lineIdx);
                        selectionEnd   = Math.max(selectionEnd, lineIdx);
                    }
                } else {
                    // Normální klik — vyber jeden řádek
                    selectionStart = lineIdx;
                    selectionEnd   = lineIdx;
                }
                return true;
            } else {
                // Klik mimo box — zruš výběr
                if (!inBounds(mx, my, boxX, boxY, boxW, boxH)) {
                    selectionStart = -1;
                    selectionEnd   = -1;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ctrl+C — zkopíruj výběr
        if (keyCode == 67 && (modifiers & 2) != 0) { // C + Ctrl
            if (selectionStart >= 0 && selectionEnd >= 0) {
                copySelection((int)(this.width / 2.0), (int)(this.height / 2.0));
                return true;
            }
        }

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
            if (keyCode == 263) { searchCursorPos = Math.max(0, searchCursorPos - 1); return true; }
            if (keyCode == 262) { searchCursorPos = Math.min(searchQuery.length(), searchCursorPos + 1); return true; }
            if (keyCode == 256) { searchFocused = false; return true; }
            if (keyCode == 257 || keyCode == 335) {
                if (!searchMatches.isEmpty()) scrollToMatch(searchMatchIndex + 1);
                return true;
            }
            return true;
        }

        if (keyCode == 256) { onClose(); return true; }
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
        assert this.minecraft != null;
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void copyToClipboard(String text, int mx, int my) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        copyFeedback      = "Copied!";
        copyFeedbackX     = mx + 8;
        copyFeedbackY     = my - 10;
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

    private int getLineAtY(int my) {
        if (my < boxY || my >= boxY + boxH) return -1;
        int lineIdx = scrollOffset + (my - boxY - 3) / LINE_HEIGHT;
        if (lineIdx < 0 || lineIdx >= componentLines.size()) return -1;
        return lineIdx;
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