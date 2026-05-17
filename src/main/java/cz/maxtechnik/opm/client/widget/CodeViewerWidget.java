package cz.maxtechnik.opm.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Znovupoužitelný VS Code-like kódový prohlížeč.
 * Podporuje: syntax highlighting, číslování řádků, scrollbar (drag),
 * vyhledávání, výběr řádků, kopírování.
 */
public class CodeViewerWidget {

    // Barvy
    private static final int BOX_BG = 0xFF2D2D2D, BORDER = 0xFF000000, SEL = 0x553399FF;
    private static final int TEXT = 0xFFDDDDDD, BTN = 0xFF3A3A3A, BTN_H = 0xFF4A4A4A;
    private static final int SEARCH_BG = 0xFF333333, TOOLBAR_BG = 0xFF1E1E1E;
    private static final int LH = 10, TOOLBAR_H = 22, SEARCH_H = 16, ARROW_W = 12;

    // Syntax barvy
    private static final int SYN_STRING = 0xFFCE9178, SYN_NUM = 0xFFB5CEA8;
    private static final int SYN_BOOL = 0xFF569CD6, SYN_KEY = 0xFF9CDCFE;
    private static final int SYN_TYPE = 0xFF4EC9B0, SYN_CONST = 0xFF4FC1FF;
    private static final int SYN_BRACE = 0xFFFFD700, SYN_BRACKET = 0xFFDA70D6, SYN_PUNCT = 0xFF808080;

    public record LineEntry(String text, int lineNum) {}

    /** Custom tlačítko v toolbaru */
    public record ToolbarButton(String label, int width, BiConsumer<Integer, Integer> onClick) {}
    private record ButtonState(ToolbarButton btn, int x, int y, boolean hover) {}

    private final Font font;
    private final String rawText;
    private List<LineEntry> lines;
    private final List<ToolbarButton> extraButtons = new ArrayList<>();
    private final List<ButtonState> buttonStates = new ArrayList<>();

    // Geometrie
    private int x, y, w, h;
    private int toolbarY, boxX, boxY, boxW, boxH, lineNumW;
    private int sX, sY, sW; // search
    private int copyBtnX, copyBtnY;
    private static final int COPY_W = 40;

    // Stav
    private int scrollOffset, selStart = -1, selEnd = -1;
    private boolean draggingScroll, searchFocused;
    private String searchQuery = "";
    private final List<Integer> searchHits = new ArrayList<>();
    private int searchIdx, searchCursor;
    private boolean hCopy, hPrev, hNext;
    private long lastClickTime;
    private int lastClickLine = -1;

    // Copy feedback
    private String feedback;
    private long feedbackUntil;
    private int feedbackX, feedbackY;

    public CodeViewerWidget(Font font, String rawText) {
        this.font = font;
        this.rawText = rawText;
    }

    /** Přidá custom tlačítko do toolbaru (např. "Copy Give"). Volej před setBounds(). */
    public void addButton(String label, int width, BiConsumer<Integer, Integer> onClick) {
        extraButtons.add(new ToolbarButton(label, width, onClick));
    }

    /** Nastaví pozici a rozměry celého widgetu (toolbar + kód) */
    public void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        recalc();
        lineNumW = 30;
        lines = buildLines(rawText);
        int maxNum = lines.stream().mapToInt(LineEntry::lineNum).max().orElse(1);
        lineNumW = font.width(String.valueOf(maxNum)) + 8;
        lines = buildLines(rawText);
    }

    private void recalc() {
        toolbarY = y;
        int tcy = toolbarY + (TOOLBAR_H - 16) / 2;
        copyBtnX = x + 4; copyBtnY = tcy;

        // Rozložení custom tlačítek za Copy
        buttonStates.clear();
        int btnX = copyBtnX + COPY_W + 4;
        for (ToolbarButton btn : extraButtons) {
            buttonStates.add(new ButtonState(btn, btnX, tcy, false));
            btnX += btn.width() + 4;
        }

        sX = btnX + 2;
        sW = x + w - sX - 36 - ARROW_W * 2 - 4 - 8;
        sY = tcy;
        boxX = x + 6; boxY = toolbarY + TOOLBAR_H + 4;
        boxW = w - 12; boxH = y + h - boxY - 6;
    }

    // ==================== RENDER ====================

    public void render(GuiGraphics g, int mx, int my) {
        if (lines == null) return;

        // Toolbar
        g.fill(x, toolbarY, x + w, toolbarY + TOOLBAR_H, TOOLBAR_BG);
        g.fill(x, toolbarY + TOOLBAR_H, x + w, toolbarY + TOOLBAR_H + 1, BORDER);
        hCopy = drawBtn(g, "Copy", copyBtnX, copyBtnY, COPY_W, mx, my);
        for (int i = 0; i < buttonStates.size(); i++) {
            ButtonState bs = buttonStates.get(i);
            boolean hover = drawBtn(g, bs.btn().label(), bs.x(), bs.y(), bs.btn().width(), mx, my);
            buttonStates.set(i, new ButtonState(bs.btn(), bs.x(), bs.y(), hover));
        }

        // Search
        renderSearch(g, mx, my);

        // Code box
        g.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, BORDER);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, BOX_BG);
        renderCode(g);

        // Feedback
        if (feedback != null && System.currentTimeMillis() < feedbackUntil)
            g.drawString(font, feedback, feedbackX, feedbackY, 0xFFFFFF00, true);
        else feedback = null;
    }

    private boolean drawBtn(GuiGraphics g, String label, int bx, int by, int bw, int mx, int my) {
        boolean hover = hit(mx, my, bx, by, bw, 16);
        g.fill(bx, by, bx + bw, by + 16, hover ? BTN_H : BTN);
        g.drawCenteredString(font, label, bx + bw / 2, by + 4, TEXT);
        return hover;
    }

    private void renderSearch(GuiGraphics g, int mx, int my) {
        g.fill(sX - 1, sY - 1, sX + sW + 1, sY + SEARCH_H + 1, BORDER);
        g.fill(sX, sY, sX + sW, sY + SEARCH_H, searchFocused ? 0xFF3D3D3D : SEARCH_BG);

        if (searchQuery.isEmpty() && !searchFocused) {
            g.drawString(font, "Search...", sX + 3, sY + 4, 0xFF666666, false);
        } else {
            g.enableScissor(sX + 1, sY, sX + sW - 1, sY + SEARCH_H);
            g.drawString(font, searchQuery, sX + 3, sY + 4, TEXT, false);
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = sX + 3 + font.width(searchQuery.substring(0, Math.min(searchCursor, searchQuery.length())));
                g.fill(cx, sY + 3, cx + 1, sY + 13, TEXT);
            }
            g.disableScissor();
        }

        int countX = sX + sW + 2, prevX = countX + 36, nextX = prevX + ARROW_W + 2;
        if (!searchHits.isEmpty())
            g.drawString(font, (searchIdx + 1) + "/" + searchHits.size(), countX, sY + 4, 0xFF888888, false);
        if (searchHits.size() > 1) {
            hPrev = hit(mx, my, prevX, sY, ARROW_W, SEARCH_H);
            hNext = hit(mx, my, nextX, sY, ARROW_W, SEARCH_H);
            g.fill(prevX, sY, prevX + ARROW_W, sY + SEARCH_H, hPrev ? BTN_H : BTN);
            g.fill(nextX, sY, nextX + ARROW_W, sY + SEARCH_H, hNext ? BTN_H : BTN);
            g.drawCenteredString(font, "<", prevX + ARROW_W / 2, sY + 4, TEXT);
            g.drawCenteredString(font, ">", nextX + ARROW_W / 2, sY + 4, TEXT);
        }
    }

    private void renderCode(GuiGraphics g) {
        int vis = boxH / LH;
        int maxSc = Math.max(0, lines.size() - vis);
        scrollOffset = Math.clamp(scrollOffset, 0, maxSc);

        int sf = (selStart >= 0 && selEnd >= 0) ? Math.min(selStart, selEnd) : -1;
        int st = (selStart >= 0 && selEnd >= 0) ? Math.max(selStart, selEnd) : -1;
        int hl = searchHits.isEmpty() ? -1 : searchHits.get(Math.min(searchIdx, searchHits.size() - 1));

        g.enableScissor(boxX + 2, boxY + 2, boxX + boxW - 6, boxY + boxH - 2);
        g.fill(boxX + lineNumW, boxY, boxX + lineNumW + 1, boxY + boxH, 0xFF444444);

        int ly = boxY + 3;
        for (int i = scrollOffset; i < Math.min(scrollOffset + vis + 1, lines.size()); i++) {
            LineEntry e = lines.get(i);
            if (sf >= 0 && i >= sf && i <= st)
                g.fill(boxX + 2, ly - 1, boxX + boxW - 6, ly + LH - 1, SEL);
            if (i == hl)
                g.fill(boxX + 2, ly - 1, boxX + boxW - 6, ly + LH - 1, 0x553A3A1A);
            if (e.lineNum() >= 0) {
                String ns = String.valueOf(e.lineNum());
                g.drawString(font, ns, boxX + lineNumW - 4 - font.width(ns), ly, 0xFF858585, false);
            }
            drawSyntaxLine(g, e.text(), boxX + lineNumW + 4, ly, searchQuery);
            ly += LH;
        }
        g.disableScissor();

        if (lines.size() > vis) {
            int th = Math.max(20, boxH * vis / lines.size());
            int tY = boxY + (boxH - th) * scrollOffset / Math.max(1, maxSc);
            g.fill(boxX + boxW - 4, boxY, boxX + boxW, boxY + boxH, 0xFF1A1A1A);
            g.fill(boxX + boxW - 4, tY, boxX + boxW, tY + th, 0xFF666666);
        }
    }

    // ==================== INPUT ====================

    public boolean mouseClicked(int mx, int my, int button) {
        if (button != 0) return false;
        // Ignoruj kliky úplně mimo widget (toolbar + celý box) — fix pro použití ve více screenech
        if (!hit(mx, my, x, y, w, h)) return false;

        if (hCopy) {
            if (selStart >= 0 && selEnd >= 0) copySelection(mx, my);
            else clip(rawText, mx, my);
            return true;
        }
        for (ButtonState bs : buttonStates) {
            if (bs.hover()) { bs.btn().onClick().accept(mx, my); return true; }
        }
        if (hPrev && searchHits.size() > 1) { scrollToMatch(searchIdx - 1); return true; }
        if (hNext && searchHits.size() > 1) { scrollToMatch(searchIdx + 1); return true; }
        if (hit(mx, my, sX, sY, sW, SEARCH_H)) {
            searchFocused = true; searchCursor = searchQuery.length(); return true;
        }
        searchFocused = false;

        // Scrollbar
        if (lines.size() > boxH / LH && mx >= boxX + boxW - 8 && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH) {
            draggingScroll = true; updateScrollMouse(my); return true;
        }

        // Line selection + double-click deselect
        int li = lineAt(my);
        if (li >= 0) {
            long now = System.currentTimeMillis();
            if (li == lastClickLine && now - lastClickTime < 400) {
                // Double-click → odznačit
                selStart = -1; selEnd = -1;
                lastClickLine = -1;
            } else if (hasShift()) {
                if (selStart >= 0) selEnd = li;
            } else if (hasCtrl()) {
                if (selStart == li && selEnd == li) { selStart = -1; selEnd = -1; }
                else if (selStart < 0) { selStart = li; selEnd = li; }
                else { selStart = Math.min(selStart, li); selEnd = Math.max(selEnd, li); }
            } else {
                selStart = li; selEnd = li;
            }
            lastClickTime = now;
            lastClickLine = li;
            return true;
        } else if (!hit(mx, my, boxX, boxY, boxW, boxH)) {
            selStart = -1; selEnd = -1;
        }
        return false;
    }

    public boolean mouseDragged(int my) {
        if (draggingScroll) { updateScrollMouse(my); return true; }
        return false;
    }

    public void mouseReleased() {
        if (draggingScroll) { draggingScroll = false;
        }
    }

    public boolean mouseScrolled(double sy, int mx, int my) {
        if (!hit(mx, my, x, y, w, h)) return false;
        scrollOffset -= (int) (sy * 2); return true;
    }

    public boolean keyPressed(int key, int mods) {
        // Ctrl+C
        if (key == 67 && (mods & 2) != 0 && selStart >= 0 && selEnd >= 0) {
            Minecraft mc = Minecraft.getInstance();
            copySelection(mc.getWindow().getGuiScaledWidth() / 2, mc.getWindow().getGuiScaledHeight() / 2);
            return true;
        }
        if (searchFocused) {
            if (key == 259 && !searchQuery.isEmpty() && searchCursor > 0) {
                searchQuery = searchQuery.substring(0, searchCursor - 1) + searchQuery.substring(searchCursor);
                searchCursor--; updateSearch();
            } else if (key == 261 && searchCursor < searchQuery.length()) {
                searchQuery = searchQuery.substring(0, searchCursor) + searchQuery.substring(searchCursor + 1);
                updateSearch();
            } else if (key == 263) searchCursor = Math.max(0, searchCursor - 1);
            else if (key == 262) searchCursor = Math.min(searchQuery.length(), searchCursor + 1);
            else if (key == 256) searchFocused = false;
            else if ((key == 257 || key == 335) && !searchHits.isEmpty()) scrollToMatch(searchIdx + 1);
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (searchFocused) {
            searchQuery = searchQuery.substring(0, searchCursor) + chr + searchQuery.substring(searchCursor);
            searchCursor++; updateSearch(); return true;
        }
        return false;
    }

    // ==================== DATA ====================

    private List<LineEntry> buildLines(String text) {
        List<LineEntry> result = new ArrayList<>();
        int maxW = boxW - 16 - lineNumW;
        int num = 1;
        for (String line : text.split("\n")) {
            if (font.width(line) <= maxW) {
                result.add(new LineEntry(line, num++)); continue;
            }
            int ic = 0;
            while (ic < line.length() && line.charAt(ic) == ' ') ic++;
            String pad = "  " + " ".repeat(ic);
            String rem = line;
            boolean first = true;
            while (!rem.isEmpty()) {
                int lmw = first ? maxW : maxW - font.width(pad);
                String fit = font.plainSubstrByWidth(rem, lmw);
                int chars = fit.length();
                if (chars < rem.length()) {
                    for (int j = chars - 1; j > 0; j--)
                        if (" ,:{}[]".indexOf(fit.charAt(j)) >= 0) { chars = j + 1; break; }
                }
                if (chars == 0) chars = 1;
                result.add(new LineEntry(first ? rem.substring(0, chars) : pad + rem.substring(0, chars),
                        first ? num++ : -1));
                rem = rem.substring(chars);
                first = false;
            }
        }
        return result;
    }

    // ==================== SYNTAX ====================

    private void drawSyntaxLine(GuiGraphics g, String line, int lx, int ly, String query) {
        int cx = lx;
        boolean inStr = false;
        char strCh = 0;
        StringBuilder tok = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inStr) {
                tok.append(c);
                if (c == strCh && line.charAt(i - 1) != '\\') { inStr = false; cx = drawTok(g, tok, cx, ly, SYN_STRING); }
            } else if (c == '"' || c == '\'') {
                cx = flushTok(g, tok, cx, ly); inStr = true; strCh = c; tok.append(c);
            } else if (":{}[],=".indexOf(c) >= 0) {
                cx = flushTok(g, tok, cx, ly);
                int sc = (c == '{' || c == '}') ? SYN_BRACE : (c == '[' || c == ']') ? SYN_BRACKET : SYN_PUNCT;
                g.drawString(font, String.valueOf(c), cx, ly, sc, false);
                cx += font.width(String.valueOf(c));
            } else { tok.append(c); }
        }
        if (!tok.isEmpty()) drawTok(g, tok, cx, ly, inStr ? SYN_STRING : tokenColor(tok.toString().trim()));

        if (!query.isBlank()) {
            int idx = line.toLowerCase().indexOf(query.toLowerCase());
            if (idx >= 0) {
                int px = font.width(line.substring(0, idx));
                int pw = font.width(line.substring(idx, idx + query.length()));
                g.fill(lx + px - 1, ly - 1, lx + px + pw + 1, ly + LH - 1, 0x55FFFF00);
            }
        }
    }

    private int flushTok(GuiGraphics g, StringBuilder tok, int tx, int ty) {
        if (tok.isEmpty()) return tx;
        return drawTok(g, tok, tx, ty, tokenColor(tok.toString().trim()));
    }

    private int drawTok(GuiGraphics g, StringBuilder tok, int tx, int ty, int color) {
        String s = tok.toString(); g.drawString(font, s, tx, ty, color, false);
        tok.setLength(0); return tx + font.width(s);
    }

    static int tokenColor(String t) {
        if (t.isEmpty()) return 0xFFD4D4D4;
        if (t.matches("-?\\d+(\\.\\d+)?[bBsSlLfFdD]?") || t.matches("-?\\.\\d+[fFdD]?")
                || t.matches("0[xX][0-9a-fA-F]+") || t.matches("[IBLS];")) return SYN_NUM;
        if (t.equals("true") || t.equals("false")) return SYN_BOOL;
        if (t.matches("[A-Z][A-Z0-9_]+")) return SYN_CONST;
        if (t.length() > 1 && Character.isUpperCase(t.charAt(0)) && t.chars().anyMatch(Character::isLowerCase)) return SYN_TYPE;
        return SYN_KEY;
    }

    // ==================== HELPERS ====================

    private void updateSearch() {
        searchHits.clear(); searchIdx = 0;
        if (searchQuery.isBlank()) return;
        String q = searchQuery.toLowerCase();
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).text().toLowerCase().contains(q)) searchHits.add(i);
        if (!searchHits.isEmpty()) scrollToMatch(0);
    }

    private void scrollToMatch(int idx) {
        if (searchHits.isEmpty()) return;
        searchIdx = Math.clamp(idx, 0, searchHits.size() - 1);
        int vis = boxH > 0 ? boxH / LH : 20;
        scrollOffset = Math.max(0, searchHits.get(searchIdx) - vis / 2);
    }

    private void updateScrollMouse(int my) {
        int vis = boxH / LH, max = Math.max(0, lines.size() - vis);
        if (max > 0) {
            int th = Math.max(20, boxH * vis / lines.size());
            int track = boxH - th;
            if (track > 0) scrollOffset = Math.clamp((int) Math.round((my - boxY - th / 2.0) / track * max), 0, max);
        }
    }

    public void clip(String text, int mx, int my) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        feedback = "Copied!"; feedbackX = mx + 8; feedbackY = my - 10;
        feedbackUntil = System.currentTimeMillis() + 1200;
    }

    private void copySelection(int mx, int my) {
        if (selStart < 0 || selEnd < 0) return;
        int f = Math.min(selStart, selEnd), t = Math.max(selStart, selEnd);
        StringBuilder sb = new StringBuilder();
        for (int i = f; i <= t && i < lines.size(); i++) {
            if (i > f) sb.append("\n");
            sb.append(lines.get(i).text());
        }
        clip(sb.toString(), mx, my);
    }

    private int lineAt(int my) {
        if (my < boxY || my >= boxY + boxH) return -1;
        int li = scrollOffset + (my - boxY - 3) / LH;
        return (li >= 0 && li < lines.size()) ? li : -1;
    }

    private boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
        return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
    }

    private static boolean hasShift() { return net.minecraft.client.gui.screens.Screen.hasShiftDown(); }
    private static boolean hasCtrl() { return net.minecraft.client.gui.screens.Screen.hasControlDown(); }
}