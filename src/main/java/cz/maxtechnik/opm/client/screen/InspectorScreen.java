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

    // --- Barvy (VS Code Dark theme) ---
    private static final int BG = 0xF0222222, HEADER_BG = 0xFF1A1A1A, BOX_BG = 0xFF2D2D2D;
    private static final int TOOLBAR_BG = 0xFF1E1E1E, BTN = 0xFF3A3A3A, BTN_H = 0xFF4A4A4A;
    private static final int SEARCH_BG = 0xFF333333, BORDER = 0xFF000000;
    private static final int TEXT = 0xFFDDDDDD, LABEL = 0xFF888888, SEL = 0x553399FF;
    private static final int ICON_SZ = 32, LH = 10, TOOLBAR_H = 22;

    // --- Barvy syntaxe ---
    private static final int SYN_STRING = 0xFFCE9178, SYN_NUM = 0xFFB5CEA8;
    private static final int SYN_BOOL = 0xFF569CD6, SYN_KEY = 0xFF9CDCFE;
    private static final int SYN_TYPE = 0xFF4EC9B0, SYN_CONST = 0xFF4FC1FF;
    private static final int SYN_BRACE = 0xFFFFD700, SYN_BRACKET = 0xFFDA70D6, SYN_PUNCT = 0xFF808080;

    private record LineEntry(String text, int lineNum) {}

    // --- Data ---
    private final ItemStack stack;
    private final Screen parentScreen;
    private final String itemId, modName, componentText;
    private List<LineEntry> lines;

    // --- Stav UI ---
    private int scrollOffset, selStart = -1, selEnd = -1;
    private boolean draggingScroll, searchFocused;
    private String searchQuery = "";
    private final List<Integer> searchHits = new ArrayList<>();
    private int searchIdx, searchCursor;
    private String feedback;
    private long feedbackUntil;
    private int feedbackX, feedbackY;

    // --- Geometrie (přepočítáno v recalc) ---
    private int pX, pY, pW, pH, hdrH, tbY;
    private int bX, bY, bW, bH, lineNumW;
    private int copyBtnX, copyBtnY, giveBtnX, giveBtnY, sX, sY, sW;
    private static final int COPY_W = 40, GIVE_W = 60, SEARCH_H = 16, ARROW_W = 12;

    // --- Hover stavy ---
    private boolean hName, hMod, hId, hCopy, hGive, hPrev, hNext;

    public InspectorScreen(ItemStack stack, Screen parentScreen) {
        super(Component.literal("Item Inspector"));
        this.stack = stack;
        this.parentScreen = parentScreen;

        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        this.itemId = loc.toString();
        String ns = loc.getNamespace();
        String mn = ns;
        try {
            var mc = net.neoforged.fml.ModList.get().getModContainerById(ns);
            if (mc.isPresent()) mn = mc.get().getModInfo().getDisplayName();
        } catch (Exception ignored) {}
        this.modName = mn;
        this.componentText = buildComponentText(stack);
    }

    // ==================== DATA ====================

    private String buildComponentText(ItemStack stack) {
        DataComponentMap comps = stack.getComponents();
        if (comps.isEmpty()) return "(no components)";
        StringBuilder sb = new StringBuilder("[\n");
        comps.forEach(c -> sb.append("  ").append(c.type()).append(" = ")
                .append(formatSnbt(c.value().toString())).append(",\n"));
        if (sb.length() > 2) { sb.setLength(sb.length() - 2); sb.append("\n"); }
        return sb.append("]").toString();
    }

    private String formatSnbt(String raw) {
        StringBuilder sb = new StringBuilder();
        int indent = 1;
        boolean inStr = false;
        char strCh = 0;
        char[] scope = new char[256];
        int depth = 0;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inStr) {
                sb.append(c);
                if (c == strCh && (i == 0 || raw.charAt(i - 1) != '\\')) inStr = false;
            } else if (c == '"' || c == '\'') {
                inStr = true; strCh = c; sb.append(c);
            } else if (c == '{' || c == '[') {
                if (depth < 255) scope[depth] = c;
                depth++;
                char close = c == '{' ? '}' : ']';
                if (i + 1 < raw.length() && raw.charAt(i + 1) == close) {
                    sb.append(c);
                } else {
                    sb.append(c); indent++;
                    sb.append("\n").repeat("  ", indent);
                }
            } else if (c == '}' || c == ']') {
                depth = Math.max(0, depth - 1);
                char open = c == '}' ? '{' : '[';
                if (i > 0 && raw.charAt(i - 1) == open) {
                    sb.append(c);
                } else {
                    indent = Math.max(1, indent - 1);
                    sb.append("\n").repeat("  ", indent).append(c);
                }
            } else if (c == ';') {
                sb.append(c).append(" ");
                while (i + 1 < raw.length() && raw.charAt(i + 1) == ' ') i++;
            } else if (c == ',') {
                sb.append(c);
                boolean inArray = depth > 0 && scope[Math.min(depth - 1, 255)] == '[';
                sb.append(inArray ? " " : "\n" + "  ".repeat(indent));
                while (i + 1 < raw.length() && raw.charAt(i + 1) == ' ') i++;
            } else if (c == ':') {
                sb.append(c);
                if (i + 1 < raw.length() && raw.charAt(i + 1) != ' ') sb.append(" ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private List<LineEntry> buildLines(String text) {
        List<LineEntry> result = new ArrayList<>();
        int maxW = bW - 16 - lineNumW;
        int num = 1;

        for (String line : text.split("\n")) {
            if (font.width(line) <= maxW) {
                result.add(new LineEntry(line, num++));
                continue;
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
                    for (int j = chars - 1; j > 0; j--) {
                        char ch = fit.charAt(j);
                        if (" ,:{}[]".indexOf(ch) >= 0) { chars = j + 1; break; }
                    }
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

    // ==================== INIT / LAYOUT ====================

    @Override
    protected void init() {
        super.init();
        recalc();
        // Prvně buildLines s výchozí šířkou, pak přepočítáme lineNumW a rebuild
        lineNumW = 30; // výchozí odhad
        lines = buildLines(componentText);
        int maxNum = lines.stream().mapToInt(LineEntry::lineNum).max().orElse(1);
        lineNumW = font.width(String.valueOf(maxNum)) + 8;
        lines = buildLines(componentText); // rebuild s přesnou šířkou
        if (!searchQuery.isEmpty()) updateSearch();
    }

    private void recalc() {
        pW = Math.min(500, width - 40); pH = height - 60;
        pX = (width - pW) / 2; pY = 20;
        hdrH = ICON_SZ + 16; tbY = pY + hdrH + 1;
        int tcy = tbY + (TOOLBAR_H - 16) / 2;
        copyBtnX = pX + 4; copyBtnY = tcy;
        giveBtnX = copyBtnX + COPY_W + 4; giveBtnY = tcy;
        sX = giveBtnX + GIVE_W + 6;
        sW = pX + pW - sX - 36 - ARROW_W * 2 - 4 - 8;
        sY = tcy;
        bX = pX + 6; bY = tbY + TOOLBAR_H + 4;
        bW = pW - 12; bH = pY + pH - bY - 6;
    }

    // ==================== RENDER ====================

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (lines == null) return;
        renderBackground(g, mx, my, pt);

        // Panel + header
        g.fill(pX - 1, pY - 1, pX + pW + 1, pY + pH + 1, BORDER);
        g.fill(pX, pY, pX + pW, pY + pH, BG);
        g.fill(pX, pY, pX + pW, pY + hdrH, HEADER_BG);
        g.fill(pX, pY + hdrH, pX + pW, pY + hdrH + 1, BORDER);

        // Ikona (2x scale)
        int ix = pX + 8, iy = pY + (hdrH - ICON_SZ) / 2;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(ix, iy, 0);
        pose.scale(2.0f, 2.0f, 1.0f);
        g.renderItem(stack, 0, 0);
        g.renderItemDecorations(font, stack, 0, 0);
        pose.popPose();

        // Header texty (název, mod, ID)
        int tx = ix + ICON_SZ + 10, tw = pX + pW - tx - 8, ty = pY + 10;
        hName = drawHeaderText(g, stack.getHoverName().getString(), tx, ty, tw, mx, my, 0xFFFFFFFF, TEXT, 0xFFAAAAAA);
        ty += 14;
        hMod = drawHeaderText(g, modName, tx, ty, tw, mx, my, 0xFFCCCCCC, LABEL, 0xFF666666);
        ty += 14;
        hId = drawHeaderText(g, itemId, tx, ty, tw, mx, my, 0xFF88FF88, 0xFF55AA55, 0xFF55AA55);

        // Toolbar
        g.fill(pX, tbY, pX + pW, tbY + TOOLBAR_H, TOOLBAR_BG);
        g.fill(pX, tbY + TOOLBAR_H, pX + pW, tbY + TOOLBAR_H + 1, BORDER);
        hCopy = drawBtn(g, "Copy", copyBtnX, copyBtnY, COPY_W, mx, my);
        hGive = drawBtn(g, "Copy Give", giveBtnX, giveBtnY, GIVE_W, mx, my);

        // Search box
        renderSearchBox(g, mx, my);

        // Component box
        g.fill(bX - 1, bY - 1, bX + bW + 1, bY + bH + 1, BORDER);
        g.fill(bX, bY, bX + bW, bY + bH, BOX_BG);
        renderCodeView(g);

        // Copy feedback
        if (feedback != null && System.currentTimeMillis() < feedbackUntil)
            g.drawString(font, feedback, feedbackX, feedbackY, 0xFFFFFF00, true);
        else feedback = null;

        super.render(g, mx, my, pt);
    }

    private boolean drawHeaderText(GuiGraphics g, String text, int x, int y, int maxW,
                                   int mx, int my, int hoverColor, int normalColor, int underline) {
        String t = truncate(text, maxW);
        boolean hover = hit(mx, my, x, y, font.width(t), 9);
        g.drawString(font, t, x, y, hover ? hoverColor : normalColor, false);
        if (hover) g.fill(x, y + 9, x + font.width(t), y + 10, underline);
        return hover;
    }

    private boolean drawBtn(GuiGraphics g, String label, int x, int y, int w, int mx, int my) {
        boolean hover = hit(mx, my, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, hover ? BTN_H : BTN);
        g.drawCenteredString(font, label, x + w / 2, y + 4, TEXT);
        return hover;
    }

    private void renderSearchBox(GuiGraphics g, int mx, int my) {
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

    private void renderCodeView(GuiGraphics g) {
        int vis = bH / LH;
        int maxSc = Math.max(0, lines.size() - vis);
        scrollOffset = Math.clamp(scrollOffset, 0, maxSc);

        int sf = (selStart >= 0 && selEnd >= 0) ? Math.min(selStart, selEnd) : -1;
        int st = (selStart >= 0 && selEnd >= 0) ? Math.max(selStart, selEnd) : -1;
        int hl = searchHits.isEmpty() ? -1 : searchHits.get(Math.min(searchIdx, searchHits.size() - 1));

        g.enableScissor(bX + 2, bY + 2, bX + bW - 6, bY + bH - 2);
        g.fill(bX + lineNumW, bY, bX + lineNumW + 1, bY + bH, 0xFF444444);

        int ly = bY + 3;
        for (int i = scrollOffset; i < Math.min(scrollOffset + vis + 1, lines.size()); i++) {
            LineEntry e = lines.get(i);
            if (sf >= 0 && i >= sf && i <= st)
                g.fill(bX + 2, ly - 1, bX + bW - 6, ly + LH - 1, SEL);
            if (i == hl)
                g.fill(bX + 2, ly - 1, bX + bW - 6, ly + LH - 1, 0x553A3A1A);
            if (e.lineNum() >= 0) {
                String ns = String.valueOf(e.lineNum());
                g.drawString(font, ns, bX + lineNumW - 4 - font.width(ns), ly, 0xFF858585, false);
            }
            drawSyntaxLine(g, e.text(), bX + lineNumW + 4, ly, searchQuery);
            ly += LH;
        }
        g.disableScissor();

        // Scrollbar
        if (lines.size() > vis) {
            int th = Math.max(20, bH * vis / lines.size());
            int tY = bY + (bH - th) * scrollOffset / Math.max(1, maxSc);
            g.fill(bX + bW - 4, bY, bX + bW, bY + bH, 0xFF1A1A1A);
            g.fill(bX + bW - 4, tY, bX + bW, tY + th, 0xFF666666);
        }
    }

    // ==================== SYNTAX ====================

    private void drawSyntaxLine(GuiGraphics g, String line, int x, int y, String query) {
        int cx = x;
        boolean inStr = false;
        char strCh = 0;
        StringBuilder tok = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inStr) {
                tok.append(c);
                if (c == strCh && line.charAt(i - 1) != '\\') {
                    inStr = false;
                    cx = drawTok(g, tok, cx, y, SYN_STRING);
                }
            } else if (c == '"' || c == '\'') {
                cx = flushTok(g, tok, cx, y);
                inStr = true; strCh = c; tok.append(c);
            } else if (":{}[],=".indexOf(c) >= 0) {
                cx = flushTok(g, tok, cx, y);
                int sc = (c == '{' || c == '}') ? SYN_BRACE : (c == '[' || c == ']') ? SYN_BRACKET : SYN_PUNCT;
                g.drawString(font, String.valueOf(c), cx, y, sc, false);
                cx += font.width(String.valueOf(c));
            } else {
                tok.append(c);
            }
        }
        if (!tok.isEmpty()) drawTok(g, tok, cx, y, inStr ? SYN_STRING : tokenColor(tok.toString().trim()));

        // Search highlight overlay
        if (!query.isBlank()) {
            int idx = line.toLowerCase().indexOf(query.toLowerCase());
            if (idx >= 0) {
                int px = font.width(line.substring(0, idx));
                int pw = font.width(line.substring(idx, idx + query.length()));
                g.fill(x + px - 1, y - 1, x + px + pw + 1, y + LH - 1, 0x55FFFF00);
            }
        }
    }

    private int flushTok(GuiGraphics g, StringBuilder tok, int x, int y) {
        if (tok.isEmpty()) return x;
        return drawTok(g, tok, x, y, tokenColor(tok.toString().trim()));
    }

    private int drawTok(GuiGraphics g, StringBuilder tok, int x, int y, int color) {
        String s = tok.toString();
        g.drawString(font, s, x, y, color, false);
        tok.setLength(0);
        return x + font.width(s);
    }

    private static int tokenColor(String t) {
        if (t.isEmpty()) return 0xFFD4D4D4;
        if (t.matches("-?\\d+(\\.\\d+)?[bBsSlLfFdD]?") || t.matches("-?\\.\\d+[fFdD]?")
                || t.matches("0[xX][0-9a-fA-F]+") || t.matches("[IBLS];")) return SYN_NUM;
        if (t.equals("true") || t.equals("false")) return SYN_BOOL;
        // ALL_CAPS = konstanty/enumy (COMMON, INSTANCE, MAINHAND, ADD_VALUE)
        if (t.matches("[A-Z][A-Z0-9_]+")) return SYN_CONST;
        // PascalCase = typy (ItemEnchantments, Reference, ResourceKey, ItemLore)
        if (t.length() > 1 && Character.isUpperCase(t.charAt(0)) && t.chars().anyMatch(Character::isLowerCase)) return SYN_TYPE;
        return SYN_KEY;
    }

    // ==================== INPUT ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (hName) { clip(stack.getHoverName().getString(), mx, my); return true; }
        if (hMod)  { clip(modName, mx, my); return true; }
        if (hId)   { clip(itemId, mx, my); return true; }
        if (hCopy) {
            if (selStart >= 0 && selEnd >= 0) copySelection(mx, my);
            else clip(componentText, mx, my);
            return true;
        }
        if (hGive) { clip("/give @s " + itemId, mx, my); return true; }
        if (hPrev && searchHits.size() > 1) { scrollToMatch(searchIdx - 1); return true; }
        if (hNext && searchHits.size() > 1) { scrollToMatch(searchIdx + 1); return true; }
        if (hit(mx, my, sX, sY, sW, SEARCH_H)) {
            searchFocused = true; searchCursor = searchQuery.length(); return true;
        }
        searchFocused = false;

        // Scrollbar
        if (lines.size() > bH / LH && mx >= bX + bW - 8 && mx <= bX + bW && my >= bY && my <= bY + bH) {
            draggingScroll = true; updateScrollMouse(my); return true;
        }

        // Line selection
        int li = lineAt(my);
        if (li >= 0) {
            if (hasShiftDown() && selStart >= 0) selEnd = li;
            else if (hasControlDown()) {
                if (selStart == li && selEnd == li) { selStart = -1; selEnd = -1; }
                else if (selStart < 0) { selStart = li; selEnd = li; }
                else { selStart = Math.min(selStart, li); selEnd = Math.max(selEnd, li); }
            } else { selStart = li; selEnd = li; }
            return true;
        } else if (!hit(mx, my, bX, bY, bW, bH)) { selStart = -1; selEnd = -1; }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingScroll) { updateScrollMouse((int) my); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) draggingScroll = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scrollOffset -= (int) (sy * 2); return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 67 && (mods & 2) != 0 && selStart >= 0 && selEnd >= 0) {
            copySelection(width / 2, height / 2); return true;
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
        if (key == 256) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (searchFocused) {
            searchQuery = searchQuery.substring(0, searchCursor) + chr + searchQuery.substring(searchCursor);
            searchCursor++; updateSearch(); return true;
        }
        return super.charTyped(chr, mods);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ==================== HELPERS ====================

    private void updateSearch() {
        searchHits.clear(); searchIdx = 0;
        if (searchQuery.isBlank()) return;
        String q = searchQuery.toLowerCase();
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).text.toLowerCase().contains(q)) searchHits.add(i);
        if (!searchHits.isEmpty()) scrollToMatch(0);
    }

    private void scrollToMatch(int idx) {
        if (searchHits.isEmpty()) return;
        searchIdx = Math.clamp(idx, 0, searchHits.size() - 1);
        int vis = bH > 0 ? bH / LH : 20;
        scrollOffset = Math.max(0, searchHits.get(searchIdx) - vis / 2);
    }

    private void updateScrollMouse(int my) {
        int vis = bH / LH, max = Math.max(0, lines.size() - vis);
        if (max > 0) {
            int th = Math.max(20, bH * vis / lines.size());
            int track = bH - th;
            if (track > 0) scrollOffset = Math.clamp((int) Math.round((my - bY - th / 2.0) / track * max), 0, max);
        }
    }

    private void clip(String text, int mx, int my) {
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
            sb.append(lines.get(i).text);
        }
        clip(sb.toString(), mx, my);
    }

    private int lineAt(int my) {
        if (my < bY || my >= bY + bH) return -1;
        int li = scrollOffset + (my - bY - 3) / LH;
        return (li >= 0 && li < lines.size()) ? li : -1;
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String truncate(String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        while (font.width(text + "...") > maxW && !text.isEmpty())
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}