package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.StationType;
import cz.maxtechnik.opm.client.recipe.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.StationType.FluidEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

import static cz.maxtechnik.opm.client.screen.EditorColors.*;

public class EditorRenderer {

    Font font;
    private final RecipeEditorData d;

    // Layout (sync z RecipeEditorScreen)
    public int pX, pY, pW, pH, leftW, rightX, rightW;
    public int editorY, editorH, invY;
    public int btnSaveX, btnSaveY, btnClearX, btnCopyX;
    public boolean isDragging;

    public EditorRenderer(Font font, RecipeEditorData data) {
        this.font = font;
        this.d = data;
    }

    public void font_set(Font f) { this.font = f; }

    // ─────────────────────────────────────────────────────────────────────────
    // SCROLLBAR — sdílená nested klasa pro všechny scroll bary
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Stavový scrollbar. Renderování, hit-testing, dragování — vše na jednom místě.
     * Použití:
     *   sb.update(viewportH, contentH);
     *   sb.render(g, x, y);
     *   sb.startDragIfHit(mx, my);
     *   sb.dragTo(my);
     *   sb.handleScroll(deltaY, stepPx);
     */
    public static final class Scrollbar {
        public float scroll;
        public int x, y, h;       // posledně vykreslené umístění (pro hit-test)
        public int viewportH;
        public int max;           // maxScroll = content - viewport (≥ 0)
        public boolean dragging;

        public void update(int viewportH, int contentH) {
            this.viewportH = viewportH;
            this.max = Math.max(0, contentH - viewportH);
            if (scroll > max) scroll = max;
            if (scroll < 0)   scroll = 0;
        }

        public void render(GuiGraphics g, int sbX, int sbY) {
            this.x = sbX; this.y = sbY; this.h = viewportH;
            if (max <= 0) return;
            g.fill(sbX, sbY, sbX + SB_W, sbY + viewportH, C_SB_BG);
            int th = Math.max(20, viewportH * viewportH / (viewportH + max));
            int ty = sbY + (int) ((viewportH - th) * (scroll / (float) max));
            g.fill(sbX, ty, sbX + SB_W, ty + th, C_SB_THUMB);
        }

        public boolean hitTrack(int mx, int my) {
            return max > 0 && mx >= x && mx <= x + SB_W && my >= y && my <= y + h;
        }

        public boolean startDragIfHit(int mx, int my) {
            if (hitTrack(mx, my)) { dragging = true; return true; }
            return false;
        }

        /** Při draggingu nastaví scroll podle pozice kursoru (lineárně přes track). */
        public void dragTo(int my) {
            if (!dragging || max <= 0 || h <= 0) return;
            float t = (my - y) / (float) h;
            scroll = Math.clamp(t * max, 0, max);
        }

        public void stopDrag() { dragging = false; }

        /** Wheel scroll. deltaY je sy z mouseScrolled (kladné = nahoru). */
        public void handleScroll(double deltaY, int stepPx) {
            scroll = (float) Math.clamp(scroll - deltaY * stepPx, 0, max);
        }

        public void reset() { scroll = 0; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SLOT REGION — popisuje jeden item slot na obrazovce (geometrie + setter)
    // ─────────────────────────────────────────────────────────────────────────
    public interface SlotSink { void accept(ItemStack s); }
    public interface SlotSource { ItemStack get(); }

    /** Reprezentace jedné slot pozice — render, hit, drop, clear pomocí jednoho objektu. */
    public static final class SlotRegion {
        public final int x, y, size;
        public final SlotSource src;
        public final SlotSink  sink;
        public final int bgColor;
        public final boolean isFluid;

        public SlotRegion(int x, int y, int size, SlotSource src, SlotSink sink, int bg, boolean isFluid) {
            this.x = x; this.y = y; this.size = size;
            this.src = src; this.sink = sink;
            this.bgColor = bg; this.isFluid = isFluid;
        }

        public boolean hit(int mx, int my) {
            return mx >= x && mx <= x + size && my >= y && my <= y + size;
        }
    }

    // ── Záložky ──────────────────────────────────────────────────────────────
    public void renderTabs(GuiGraphics g, int mx, int my, List<StationType> tabs, int tabIdx) {
        int tabW = leftW / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            StationType t = tabs.get(i);
            int tx = pX + i * tabW;
            int tw = (i == tabs.size() - 1) ? (pX + leftW - tx) : tabW;
            boolean sel = i == tabIdx, hov = hit(mx, my, tx, pY, tw, TAB_H), cr = t.isCreate();
            int bg = sel ? (cr ? C_TAB_CRS : C_TAB_SEL) : hov ? (cr ? C_TAB_CR : 0xFF353535) : (cr ? C_TAB_CR : C_TAB);
            g.fill(tx, pY, tx + tw, pY + TAB_H, bg);
            if (sel) g.fill(tx, pY + TAB_H - 2, tx + tw, pY + TAB_H, 0xFF8888FF);
            if (i < tabs.size() - 1) g.fill(tx + tw - 1, pY + 2, tx + tw, pY + TAB_H - 2, 0xFF444444);
            int iconSz = 16;
            int icx = tx + (tw - iconSz) / 2;
            try {
                ResourceLocation loc = ResourceLocation.tryParse(t.stationItemId);
                if (loc != null) {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    opt.ifPresent(item -> g.renderItem(new ItemStack(item), icx, pY + (TAB_H - iconSz) / 2));
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Editor panely ────────────────────────────────────────────────────────
    public int renderCrafting(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 20;
        drawToggle2(g, mx, my, cx - 70, cy, "Shaped", "Shapeless", !d.shapeless);
        cy += 30;
        renderGridN(g, mx, my, d.craftGrid, 3, 3, cx - 70, cy, SS, SP, SP);
        int ax = cx - 70 + 3 * (SS + SP) + 15, ay = cy + SS + SP;
        g.drawString(font, "→", ax, ay - 4, C_LABEL, false);
        int rx = ax + 20;
        g.drawString(font, "Result", rx, cy - 12, C_LABEL, false);
        slot(g, mx, my, d.craftResult, rx, ay - 9, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, ay - 7, d.craftCount);
        return cy + 3 * (SS + SP) - editorY;
    }

    public int renderMechCrafting(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 20;
        drawToggle2(g, mx, my, cx - 60, cy, "Mirrored", "Exact", d.mechMirrored);
        cy += 30;
        int sz = 16, pad = 1, gridW = 9 * (sz + pad);
        int sx = cx - gridW / 2 - 40;
        renderGridN(g, mx, my, d.mechGrid, 9, 9, sx, cy, sz, pad, pad);
        int ax = sx + gridW + 15, ay = cy + (9 * (sz + pad)) / 2 - 4;
        g.drawString(font, "→", ax, ay, C_LABEL, false);
        int rx = ax + 20;
        g.drawString(font, "Result", rx, ay - 14, C_LABEL, false);
        slot(g, mx, my, d.craftResult, rx, ay - 4, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, ay - 2, d.craftCount);
        return cy + 9 * (sz + pad) - editorY;
    }

    public int renderFurnace(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 20;
        // Subtype přepínač
        renderHorizPicker(g, mx, my, cx, cy, d.furnLabels, d.furnSubIdx, C_TAB_SEL);
        cy += 40;
        renderIOPair(g, mx, my, cx, cy, d.furnIn, d.furnOut, "Input", "Result", d.furnCount);
        cy += 40;
        // XP + Time
        g.drawString(font, "XP:", cx - 70, cy + 4, C_LABEL, false);
        g.drawString(font, String.format(Locale.ROOT, "%.1f", d.furnXp), cx - 45, cy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx - 20, cy + 2);
        g.drawString(font, "Time:", cx + 10, cy + 4, C_LABEL, false);
        g.drawString(font, d.furnTime + " t", cx + 45, cy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 80, cy + 2);
        return cy + 20 - editorY;
    }

    public int renderStonecutter(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 40;
        renderIOPair(g, mx, my, cx, cy, d.stoneIn, d.stoneOut, "Input", "Result", d.stoneCount);
        return cy + 40 - editorY;
    }

    public int renderSmithing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 40;
        int step = SS + 36;
        int totalW = 3 * step + 20 + SS;
        int sx = cx - totalW / 2;
        String[] lbl = {"Template", "Base", "Addition"};
        ItemStack[] sl = {d.smTemplate, d.smBase, d.smAddition};
        for (int i = 0; i < 3; i++) {
            int x = sx + i * step;
            g.drawCenteredString(font, lbl[i], x + SS / 2, cy - 12, C_LABEL);
            slot(g, mx, my, sl[i], x, cy, C_SLOT);
            if (i < 2) g.drawCenteredString(font, "+", x + SS + 18, cy + 5, C_LABEL);
        }
        int arrowX = sx + 3 * step;
        g.drawString(font, "→", arrowX, cy + 5, C_LABEL, false);
        int rx = arrowX + 16;
        g.drawCenteredString(font, "Result", rx + SS / 2, cy - 12, C_LABEL);
        slot(g, mx, my, d.smResult, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, d.smCount);
        return cy + 40 - editorY;
    }

    public int renderMixing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        drawToggle2(g, mx, my, cx - 60, editorY + 15, "Mixer", "Press", !d.mixBasinPress);

        // Heat přepínač s heat-specific barvami
        int[] heatCols = {C_BTN, 0xFF4A2000, 0xFF6A0000};
        int heatY = editorY + 40;
        renderHorizPickerColored(g, mx, my, cx, heatY, d.heatLabels, d.mixHeat, heatCols, 0xFF4A4A7A, 0xFFFFCC88);

        int cy = editorY + 70;
        int sx = cx - 150;
        g.drawString(font, "Ingredients:", sx, cy - 12, C_LABEL, false);
        // Grid ingrediencí (3x3)
        renderGridN(g, mx, my, d.mixIng, 3, 3, sx, cy, SS, 32, 10);

        int rx = cx + 10;
        g.drawString(font, "Result Items:", rx, cy - 12, C_LABEL, false);
        // Výstupní itemy (2x2)
        for (int i = 0; i < 4; i++) {
            int col = i % 2, row = i / 2;
            int ox = rx + col * 90, oy = cy + row * 30;
            renderOutputWithChance(g, mx, my, d.mixOuts.get(i), ox, oy);
        }

        int fluidY = cy + 95;
        g.drawString(font, "Input Fluids:", sx, fluidY - 12, C_LABEL, false);
        // Vstupní fluidy
        for (int i = 0; i < 2; i++) slotFluid(g, mx, my, d.mixFluidIng.get(i), sx + i * 65, fluidY);

        g.drawString(font, "Result Fluids:", rx, fluidY - 12, C_LABEL, false);
        // Výstupní fluidy
        for (int i = 0; i < 2; i++) slotFluid(g, mx, my, d.mixFluidOuts.get(i), rx + i * 65, fluidY);

        return fluidY + 35 - editorY;
    }

    public int renderPressing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int gridY = editorY + 45;
        int sx = cx - 70;
        g.drawCenteredString(font, "Input", sx + SS / 2, gridY - 12, C_LABEL);
        slot(g, mx, my, d.pressIng.get(0), sx, gridY, C_SLOT);
        g.drawString(font, "→", sx + SS + 25, gridY + 5, C_LABEL, false);
        int rx = sx + SS + 50;
        g.drawCenteredString(font, "Result Item", rx + SS / 2, gridY - 12, C_LABEL);
        renderOutputWithChance(g, mx, my, d.pressOuts.get(0), rx, gridY);
        return gridY + SS + 15 - editorY;
    }

    public int renderCrushing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 15;
        drawToggle2(g, mx, my, cx - 55, cy, "Crushing", "Milling", d.isMilling);
        cy += 35;
        return renderProcessingPanel(g, mx, my, cx, cy, d.crushIn, d.crushOuts, 8, 4, d.crushTime) - editorY;
    }

    public int renderFan(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2, cy = editorY + 15;
        drawToggle2(g, mx, my, cx - 65, cy, "Washing", "Haunting", d.fanHaunting);
        cy += 35;
        return renderProcessingPanel(g, mx, my, cx, cy, d.fanIn, d.fanOuts, 4, 2, d.fanTime) - editorY;
    }

    // ── Sdílené pomocné rendery ──────────────────────────────────────────────

    /** I/O dvojice: input slot → šipka → result slot + spinner pro count. */
    private void renderIOPair(GuiGraphics g, int mx, int my, int cx, int cy,
                              ItemStack input, ItemStack output,
                              String inputLabel, String resultLabel, int count) {
        int sx = cx - IO_INPUT_OFFSET;
        g.drawString(font, inputLabel, sx, cy - 12, C_LABEL, false);
        slot(g, mx, my, input, sx, cy, C_SLOT);
        g.drawString(font, "→", sx + SS + 15, cy + 5, C_LABEL, false);
        int rx = sx + SS + IO_GAP;
        g.drawString(font, resultLabel, rx, cy - 12, C_LABEL, false);
        slot(g, mx, my, output, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, count);
    }

    /** Crushing/Fan panel: input vlevo + sloupce výstupů + time spinner. */
    private int renderProcessingPanel(GuiGraphics g, int mx, int my, int cx, int cy,
                                      ItemStack input, List<CrushingOutput> outs,
                                      int count, int rowsPerCol, int time) {
        int sx = cx - 120;
        g.drawCenteredString(font, "Input", sx + SS / 2, cy - 12, C_LABEL);
        slot(g, mx, my, input, sx, cy, C_SLOT);
        g.drawString(font, "→", sx + SS + 10, cy + 5, C_LABEL, false);
        int outX = sx + SS + 30, colW = 110;
        g.drawString(font, count == 8 ? "Outputs (chance via +/-):" : "Outputs:", outX, cy - 12, C_LABEL, false);
        for (int i = 0; i < count; i++) {
            int ox = outX + (i / rowsPerCol) * colW;
            int oy = cy + (i % rowsPerCol) * (SS + 12);
            renderOutputWithChance(g, mx, my, outs.get(i), ox, oy);
        }
        int oy = cy + rowsPerCol * (SS + 12) + 10;
        g.drawString(font, "Time:", cx - 20, oy + 4, C_LABEL, false);
        g.drawString(font, time + " t", cx + 15, oy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 55, oy + 2);
        return oy + 30;
    }

    /** Slot + count spinner + chance spinner + label "100%". */
    // Pro posunutí prvků (šipek a % šance) u mixeru, presu, crushing a fan upravte hodnoty zde:
    private void renderOutputWithChance(GuiGraphics g, int mx, int my, CrushingOutput co, int ox, int oy) {
        slot(g, mx, my, co.stack, ox, oy, co.isEmpty() ? C_SLOT : C_SLOT_RES);
        int cpx = ox + SS + 4, cpy = oy + 2;
        // Text čísla množství (např. "1")
        g.drawString(font, String.valueOf(co.count), cpx, cpy + 2, C_TEXT, false);
        // První mini-spinner (šipky +/- pro množství) - posunout změnou "cpx + 16"
        drawMiniSpinner(g, mx, my, cpx + 16, cpy - 2);
        int chX = cpx + 28; // Základní X souřadnice pro šance (šipky + text)
        String chStr = co.chance >= 1f ? "100%" : Math.round(co.chance * 100) + "%";
        // Druhý mini-spinner (šipky +/- pro procento šance) - posunout změnou "chX"
        drawMiniSpinner(g, mx, my, chX, cpy - 2);
        // Text procenta šance (např. "100%") - posunout změnou "chX + 12"
        g.drawString(font, chStr, chX + 14, cpy + 3, co.isEmpty() ? C_LABEL : 0xFFAAFF88, false);
    }

    /** Vodorovný picker s vlastními barvami pozadí pro každou položku (heat). */
    private void renderHorizPickerColored(GuiGraphics g, int mx, int my, int cx, int cy,
                                          String[] labels, int selIdx, int[] colors,
                                          int selBg, int selFg) {
        int tw = 0;
        for (String l : labels) tw += font.width(l) + 16;
        int bx = cx - tw / 2;
        for (int i = 0; i < labels.length; i++) {
            int bw = font.width(labels[i]) + 10;
            boolean sel = selIdx == i, hov = hit(mx, my, bx, cy, bw, 16);
            int bg = sel ? selBg : (hov ? colors[i] + 0x111100 : colors[i]);
            g.fill(bx, cy, bx + bw, cy + 16, bg);
            g.drawCenteredString(font, labels[i], bx + bw / 2, cy + 4, sel ? selFg : C_TEXT);
            bx += bw + 6;
        }
    }

    /** Vodorovný picker s jednotnou barvou (tabs). */
    private void renderHorizPicker(GuiGraphics g, int mx, int my, int cx, int cy,
                                   String[] labels, int selIdx, int selBg) {
        int tw = 0;
        for (String l : labels) tw += font.width(l) + 16;
        int bx = cx - tw / 2;
        for (int i = 0; i < labels.length; i++) {
            int bw = font.width(labels[i]) + 10;
            boolean sel = selIdx == i, hov = hit(mx, my, bx, cy, bw, 16);
            g.fill(bx, cy, bx + bw, cy + 16, sel ? selBg : (hov ? C_BTN_H : C_BTN));
            g.drawCenteredString(font, labels[i], bx + bw / 2, cy + 4, sel ? 0xFFCCCCFF : C_TEXT);
            bx += bw + 6;
        }
    }

    // ── Slot helpers ────────────────────────────────────────────────────────

    public void renderGridN(GuiGraphics g, int mx, int my, List<ItemStack> list,
                            int cols, int rows, int sx, int sy, int sz, int padX, int padY) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int bx = sx + c * (sz + padX);
            int by = sy + r * (sz + padY);
            int idx = r * cols + c;
            boolean hov = hit(mx, my, bx, by, sz, sz), drop = isDragging && hov;
            g.fill(bx - 1, by - 1, bx + sz + 1, by + sz + 1, C_BORDER);
            g.fill(bx, by, bx + sz, by + sz, drop ? C_SLOT_DR : (hov ? C_SLOT_HOV : C_SLOT));
            ItemStack s = idx < list.size() ? list.get(idx) : ItemStack.EMPTY;
            if (!s.isEmpty()) itemScaled(g, s, bx, by, sz);
            // Pokud je velký padding (mixing), kresli count spinner vedle slotu
            if (padX >= 24) {
                // cpxText určuje X pozici čísla množství ingredience, cpxClick je základ pro hitbox, cpy je výška
                int cpxText = bx + sz + 3, cpxClick = bx + sz + 1, cpy = by + 2;
                int count = !s.isEmpty() ? s.getCount() : 1;
                // Text množství ingredience
                g.drawString(font, String.valueOf(count), cpxText, cpy + 2, C_TEXT, false);
                // Mini-spinner (šipky +/-) pro ingredience - posunout změnou "cpxClick + 20"
                drawMiniSpinner(g, mx, my, cpxClick + 20, cpy - 2);
            }
        }
    }

    public void slot(GuiGraphics g, int mx, int my, ItemStack s, int sx, int sy, int bg) {
        boolean hov = hit(mx, my, sx, sy, SS, SS), drop = isDragging && hov;
        g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, C_BORDER);
        g.fill(sx, sy, sx + SS, sy + SS, drop ? C_SLOT_DR : (hov ? C_SLOT_HOV : bg));
        if (s != null && !s.isEmpty()) {
            ItemStack rs = s.copy(); rs.setCount(1);
            g.renderItem(rs, sx + 1, sy + 1);
            g.renderItemDecorations(font, rs, sx + 1, sy + 1);
        }
    }

    public void slotFluid(GuiGraphics g, int mx, int my, FluidEntry f, int sx, int sy) {
        boolean hov = hit(mx, my, sx, sy, SS, SS), drop = isDragging && hov;
        g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, 0xFF2255AA);
        g.fill(sx, sy, sx + SS, sy + SS, drop ? 0xFF2A5A6A : (hov ? 0xFF2A3A6A : 0xFF1A2A4A));
        if (!f.isEmpty()) g.renderItem(f.proxy, sx + 1, sy + 1);
        else g.drawCenteredString(font, "~", sx + SS / 2, sy + (SS - 8) / 2, 0xFF4488CC);
        // amtX a amtY určují pozici textu s množstvím fluidu v mB (např. "1000 mB")
        int amtX = sx + SS + 4, amtY = sy + 4;
        g.drawString(font, f.amount + " mB", amtX, amtY, 0xFF66AAFF, false);
        // hP a hM určují klikací zónu tlačítek + a - (musí přesně odpovídat souřadnicím draw níže)
        boolean hP = hit(mx, my, amtX - 2, amtY + 12, SPIN_W, SPIN_H);
        boolean hM = hit(mx, my, amtX + 10, amtY + 12, SPIN_W, SPIN_H);
        // Vykreslení pozadí tlačítek "+" a "-"
        g.fill(amtX - 2, amtY + 12, amtX + 8, amtY + 20, hP ? C_BTN_H : C_BTN);
        g.fill(amtX + 10, amtY + 12, amtX + 20, amtY + 20, hM ? C_BTN_H : C_BTN);
        // Vykreslení znaků "+" a "-"
        g.drawCenteredString(font, "+", amtX + 3, amtY + 12, C_TEXT);
        g.drawCenteredString(font, "-", amtX + 15, amtY + 12, C_TEXT);
    }

    public void invSlotRender(GuiGraphics g, int mx, int my, ItemStack s, int sx, int sy) {
        boolean hov = hit(mx, my, sx, sy, SS, SS);
        g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, C_BORDER);
        g.fill(sx, sy, sx + SS, sy + SS, hov ? C_SLOT_HOV : C_SLOT);
        if (s != null && !s.isEmpty()) {
            g.renderItem(s, sx + 1, sy + 1);
            g.renderItemDecorations(font, s, sx + 1, sy + 1);
        }
    }

    public void spinner(GuiGraphics g, int mx, int my, int cx, int cy, int count) {
        g.drawString(font, String.valueOf(count), cx, cy + 2, C_TEXT, false);
        boolean hP = hit(mx, my, cx + 18, cy, SPIN_W, SPIN_H), hM = hit(mx, my, cx + 18, cy + 8, SPIN_W, SPIN_H);
        g.fill(cx + 18, cy, cx + 28, cy + 8, hP ? C_BTN_H : C_BTN);
        g.fill(cx + 18, cy + 8, cx + 28, cy + 16, hM ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, "+", cx + 23, cy, C_TEXT);
        g.drawCenteredString(font, "-", cx + 23, cy + 8, C_TEXT);
    }

    public void valSpinner(GuiGraphics g, int mx, int my, int cx, int cy) {
        boolean hP = hit(mx, my, cx, cy, SPIN_W, SPIN_H), hM = hit(mx, my, cx, cy + 8, SPIN_W, SPIN_H);
        g.fill(cx, cy, cx + 10, cy + 8, hP ? C_BTN_H : C_BTN);
        g.fill(cx, cy + 8, cx + 10, cy + 16, hM ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, "+", cx + 5, cy, C_TEXT);
        g.drawCenteredString(font, "-", cx + 5, cy + 8, C_TEXT);
    }

    public void drawMiniSpinner(GuiGraphics g, int mx, int my, int cx, int cy) {
        boolean hP = hit(mx, my, cx, cy, MINI_SPIN, MINI_SPIN), hM = hit(mx, my, cx, cy + 9, MINI_SPIN, MINI_SPIN);
        g.fill(cx, cy, cx + 9, cy + 9, hP ? C_BTN_H : C_BTN);
        g.fill(cx, cy + 9, cx + 9, cy + 18, hM ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, "+", cx + 4, cy, C_TEXT);
        g.drawCenteredString(font, "-", cx + 4, cy + 9, C_TEXT);
    }

    public void drawToggle2(GuiGraphics g, int mx, int my, int x, int y, String a, String b, boolean aOn) {
        int wa = font.width(a) + 12, wb = font.width(b) + 12;
        boolean ha = hit(mx, my, x, y, wa, 16), hb = hit(mx, my, x + wa + 2, y, wb, 16);
        g.fill(x, y, x + wa, y + 16, aOn ? C_TAB_SEL : (ha ? C_BTN_H : C_BTN));
        g.fill(x + wa + 2, y, x + wa + 2 + wb, y + 16, !aOn ? C_TAB_SEL : (hb ? C_BTN_H : C_BTN));
        g.drawCenteredString(font, a, x + wa / 2, y + 4, aOn ? 0xFFCCCCFF : C_TEXT);
        g.drawCenteredString(font, b, x + wa + 2 + wb / 2, y + 4, !aOn ? 0xFFCCCCFF : C_TEXT);
    }

    public void drawBtn(GuiGraphics g, String lbl, int bx, int by, int bw, boolean hov, int bg, int hbg) {
        g.fill(bx, by, bx + bw, by + 16, hov ? hbg : bg);
        g.fill(bx, by, bx + bw, by + 1, 0x44FFFFFF);
        g.drawCenteredString(font, lbl, bx + bw / 2, by + 4, C_TEXT);
    }

    public void renderBtnBar(GuiGraphics g, int mx, int my, String fileName, boolean fnFocused, int fnCursor) {
        boolean hS = hit(mx, my, btnSaveX, btnSaveY, 92, 16);
        boolean hC = hit(mx, my, btnClearX, btnSaveY, 40, 16);
        boolean hP = hit(mx, my, btnCopyX, btnSaveY, 60, 16);
        drawBtn(g, "Generate", btnSaveX, btnSaveY, 92, hS, C_BTN_G, C_BTN_GH);
        drawBtn(g, "Clear", btnClearX, btnSaveY, 40, hC, C_BTN, C_BTN_H);
        drawBtn(g, "Copy", btnCopyX, btnSaveY, 60, hP, C_BTN, C_BTN_H);
        int fx = btnCopyX + 65, fy = btnSaveY;
        int fw = leftW - fx - 10;
        if (fw > 20) {
            g.drawString(font, "File:", fx, fy + 4, C_LABEL, false);
            int ffx = fx + font.width("File:") + 5;
            int ffw = leftW - ffx - 10;
            g.fill(ffx - 1, fy - 1, ffx + ffw + 1, fy + 17, C_BORDER);
            g.fill(ffx, fy, ffx + ffw, fy + 16, fnFocused ? 0xFF3D3D3D : 0xFF303030);
            String dn = truncate(fileName, ffw - 6);
            g.drawString(font, dn, ffx + 4, fy + 4, C_TEXT, false);
            if (fnFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = ffx + 4 + font.width(dn.substring(0, Math.min(fnCursor, dn.length())));
                g.fill(cx, fy + 3, cx + 1, fy + 13, C_TEXT);
            }
        }
        if (!d.statusMsg.isEmpty() && System.currentTimeMillis() < d.statusUntil)
            g.drawCenteredString(font, d.statusMsg, leftW / 2, btnSaveY - 14, d.statusOk ? 0xFF88FF88 : 0xFFFF6666);
    }

    public void renderErrorPopup(GuiGraphics g, int mx, int my, String error, int width, int height) {
        g.fill(0, 0, width, height, 0xAA000000);
        int pw = 260, ph = 100, px2 = (width - pw) / 2, py2 = (height - ph) / 2;
        g.fill(px2, py2, px2 + pw, py2 + ph, 0xFF222222);
        g.fill(px2, py2, px2 + pw, py2 + 2, 0xFFFF3333);
        g.fill(px2, py2 + ph - 2, px2 + pw, py2 + ph, 0xFFFF3333);
        g.fill(px2, py2, px2 + 2, py2 + ph, 0xFFFF3333);
        g.fill(px2 + pw - 2, py2, px2 + pw, py2 + ph, 0xFFFF3333);
        g.drawString(font, "Error", px2 + (pw - font.width("Error")) / 2, py2 + 12, 0xFFFF3333, false);
        g.drawString(font, error, px2 + (pw - font.width(error)) / 2, py2 + 36, C_TEXT, false);
        int bx = px2 + (pw - 60) / 2, by = py2 + 65, bw = 60, bh = 18;
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        g.fill(bx, by, bx + bw, by + bh, hov ? 0xFF666666 : 0xFF444444);
        g.fill(bx, by, bx + bw, by + 1, 0xFF888888);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF888888);
        g.drawString(font, "OK", bx + (bw - font.width("OK")) / 2, by + 5, C_TEXT, false);
    }

    public void showTip(GuiGraphics g, ItemStack s, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
            g.renderComponentTooltip(font, s.getTooltipLines(
                    Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL), mx, my);
    }

    private void itemScaled(GuiGraphics g, ItemStack s, int sx, int sy, int sz) {
        ItemStack rs = s.copy(); rs.setCount(1);
        if (sz >= 16) {
            g.renderItem(rs, sx + 1, sy + 1);
            if (sz >= 18) g.renderItemDecorations(font, rs, sx + 1, sy + 1);
        } else {
            float sc = sz / 16f;
            var p = g.pose(); p.pushPose();
            p.translate(sx + 1, sy + 1, 0); p.scale(sc, sc, 1f);
            g.renderItem(rs, 0, 0); p.popPose();
        }
    }

    public boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
        return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
    }

    private String truncate(String t, int maxW) {
        if (font.width(t) <= maxW) return t;
        while (font.width(t + "…") > maxW && !t.isEmpty()) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}