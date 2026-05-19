package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder;
import cz.maxtechnik.opm.client.recipe.StationType;
import cz.maxtechnik.opm.client.recipe.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.StationType.FluidEntry;
import cz.maxtechnik.opm.client.recipe.StationType.RecipeFileWriter;
import cz.maxtechnik.opm.client.screen.EditorRenderer.Scrollbar;
import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static cz.maxtechnik.opm.client.screen.EditorColors.*;

public class RecipeEditorScreen extends Screen {

    // ── Závislosti ───────────────────────────────────────────────────────────
    private final Screen parent;
    final RecipeEditorData d;
    private final EditorRenderer r;

    // ── Záložky ──────────────────────────────────────────────────────────────
    final List<StationType> tabs = new ArrayList<>();
    private int tabIdx = 0;

    // ── UI stav ──────────────────────────────────────────────────────────────
    private int invPanelHeight = 150;
    private boolean isDraggingSplitter;

    private ItemStack dragStack = ItemStack.EMPTY;
    private boolean isDragging = false;
    private int dragX, dragY;

    private String fileName = "my_recipe";
    private boolean fnFocused = false;
    private int fnCursor = fileName.length();

    private enum BottomTab { INVENTORY, FLUIDS, ITEMS, TAGS }
    private BottomTab bottomTab = BottomTab.INVENTORY;
    private boolean showRecipesList = false;
    private long lastBtnClickTime = 0;

    // ── Scrollbary (sdílená nested klasa) ────────────────────────────────────
    private final Scrollbar editorSb  = new Scrollbar();
    private final Scrollbar bottomSb  = new Scrollbar();
    private final Scrollbar favSb     = new Scrollbar();
    private final Scrollbar recipeSb  = new Scrollbar();

    private EditBox searchBox;
    private String lastSearch = "";

    // ── Spinner edit ─────────────────────────────────────────────────────────
    private EditBox activeNumEditBox = null;
    private String activeFieldName = null;
    private int activeFieldIdx = -1;
    private long lastClickTime = 0;
    private int lastClickX = 0, lastClickY = 0;

    // ── JSON viewer ──────────────────────────────────────────────────────────
    private String curJson = "";
    private CodeViewerWidget codeViewer;

    // ── Geometrie ────────────────────────────────────────────────────────────
    private int pX, pY, pW, pH, leftW, rightX, rightW;
    private int editorY, editorH, invY;
    private int btnSaveX, btnSaveY, btnClearX, btnCopyX;

    // ─────────────────────────────────────────────────────────────────────────

    public RecipeEditorScreen(Screen parent) {
        super(Component.literal("Recipe Editor"));
        this.parent = parent;
        this.d = new RecipeEditorData();
        this.r = new EditorRenderer(null, d);
        boolean createLoaded = net.neoforged.fml.ModList.get().isLoaded("create");
        for (StationType t : StationType.values())
            if (!t.isCreate() || createLoaded) tabs.add(t);
    }

    @Override
    protected void init() {
        super.init();
        pX = 0; pY = 0; pW = width; pH = height;
        leftW = (int)(pW * 0.65);
        rightX = pX + leftW + 2;
        rightW = pW - leftW - 2;
        editorY = pY + TAB_H + 2;
        btnSaveX = pX + 10; btnClearX = btnSaveX + 96; btnCopyX = btnClearX + 44;

        r.font_set(font);
        d.loadConfig(minecraft, h -> invPanelHeight = h);
        updateLayout();

        codeViewer = new CodeViewerWidget(font, curJson);
        codeViewer.setBounds(rightX, pY, rightW, pH);
        searchBox = new EditBox(font, pX + 10, invY + 22, 176, 12, Component.empty());

        d.loadFluids();
        d.loadAllItems();
        d.loadTags();
        d.cachedFilteredItems.addAll(d.allItems);
        d.loadFavorites(minecraft);
        d.scanSavedRecipes();
    }

    private void updateLayout() {
        int min = 2 * (SS + SP) + 40, max = pH - (pY + TAB_H + 40);
        invPanelHeight = Math.clamp(invPanelHeight, min, max);
        invY = pY + pH - invPanelHeight;
        int btnBarY = invY - 20;
        btnSaveY = btnBarY;
        editorH = btnBarY - editorY - 4;
        if (searchBox != null) { searchBox.setX(pX + 10); searchBox.setY(invY + 22); }
        // Sync layout do rendereru
        r.pX = pX; r.pY = pY; r.pW = pW; r.pH = pH;
        r.leftW = leftW; r.rightX = rightX; r.rightW = rightW;
        r.editorY = editorY; r.editorH = editorH; r.invY = invY;
        r.btnSaveX = btnSaveX; r.btnSaveY = btnSaveY;
        r.btnClearX = btnClearX; r.btnCopyX = btnCopyX;
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (isDragging) { dragX = mx; dragY = my; }
        r.isDragging = isDragging;

        renderBackground(g, mx, my, pt);
        g.fill(pX, pY, pX + pW, pY + pH, C_BG);

        r.renderTabs(g, mx, my, tabs, tabIdx);

        g.fill(pX, editorY, pX + leftW, editorY + editorH, 0xFF222222);
        g.fill(pX + leftW, pY, rightX, pY + pH, 0xFF111111);

        g.enableScissor(pX, editorY, pX + leftW, editorY + editorH);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, -editorSb.scroll, 0);
        int mY = (int)(my + editorSb.scroll);

        int contentH = switch (tabs.get(tabIdx)) {
            case CRAFTING      -> r.renderCrafting(g, mx, mY);
            case FURNACE       -> r.renderFurnace(g, mx, mY);
            case STONECUTTER   -> r.renderStonecutter(g, mx, mY);
            case SMITHING      -> r.renderSmithing(g, mx, mY);
            case MECH_CRAFTING -> r.renderMechCrafting(g, mx, mY);
            case MIXING        -> r.renderMixing(g, mx, mY);
            case PRESSING      -> r.renderPressing(g, mx, mY);
            case FAN           -> r.renderFan(g, mx, mY);
            case CRUSHING      -> r.renderCrushing(g, mx, mY);
        };
        editorSb.update(editorH, contentH + 20);
        pose.popPose();
        g.disableScissor();

        editorSb.render(g, pX + leftW - 6, editorY);

        updateJson();
        codeViewer.render(g, mx, my);

        r.renderBtnBar(g, mx, my, fileName, fnFocused, fnCursor);
        renderBottomArea(g, mx, my);

        if (isDragging && !dragStack.isEmpty()) {
            g.renderItem(dragStack, dragX - 8, dragY - 8);
            g.renderItemDecorations(font, dragStack, dragX - 8, dragY - 8);
        }
        if (activeNumEditBox != null) activeNumEditBox.render(g, mx, my, pt);

        if (!isDragging) {
            ItemStack hs = slotAt(mx, my);
            if (hs != null && !hs.isEmpty()) r.showTip(g, hs, mx, my);
        }

        if (d.popupError != null) r.renderErrorPopup(g, mx, my, d.popupError, width, height);

        super.render(g, mx, my, pt);
    }

    private void renderBottomArea(GuiGraphics g, int mx, int my) {
        g.fill(pX, invY, pX + leftW, pY + pH, C_INV);
        g.fill(pX, invY, pX + leftW, invY + 2, C_BORDER);
        g.fill(pX + leftW / 2 - 20, invY, pX + leftW / 2 + 20, invY + 3, 0xFF666666);

        int startX = pX + 10;
        int favCols = 5;
        int favX = startX + 9 * (SS + SP) + 16;

        String[] bTabs = {"Inventory","Fluids","Items","Tags"};
        int txTabsEnd = startX;
        for (String s : bTabs) txTabsEnd += font.width(s) + 14;

        int recBtnW = font.width(showRecipesList ? "◀ Items" : "Recipes ▶") + 10;
        int recBtnX = txTabsEnd;

        // Řádek 1: záložky + Recipes tlačítko
        if (!showRecipesList) {
            int tx = startX;
            for (int i = 0; i < bTabs.length; i++) {
                int tw = font.width(bTabs[i]) + 10;
                boolean sel = bottomTab.ordinal() == i;
                boolean hov = r.hit(mx, my, tx, invY + 4, tw, 14);
                g.fill(tx, invY + 4, tx + tw, invY + 18, sel ? C_TAB_SEL : (hov ? C_BTN_H : C_BTN));
                g.drawCenteredString(font, bTabs[i], tx + tw / 2, invY + 7, sel ? 0xFFCCCCFF : C_TEXT);
                tx += tw + 4;
            }
        }
        if (!showRecipesList && bottomTab != BottomTab.INVENTORY) searchBox.render(g, mx, my, 0);

        boolean hRec = r.hit(mx, my, recBtnX, invY + 4, recBtnW, 14);
        g.fill(recBtnX, invY + 4, recBtnX + recBtnW, invY + 18, showRecipesList ? C_TAB_SEL : (hRec ? C_BTN_H : C_BTN));
        g.drawCenteredString(font, showRecipesList ? "◀ Items" : "Recipes ▶", recBtnX + recBtnW / 2, invY + 7, showRecipesList ? 0xFFCCCCFF : C_TEXT);

        int listY = bottomListY();
        int listH = (pY + pH) - listY - 5;

        if (!showRecipesList) {
            // Content list se scrollbarem
            g.enableScissor(startX, listY, startX + 9 * (SS + SP), listY + listH);
            var pose = g.pose();
            pose.pushPose(); pose.translate(0, -bottomSb.scroll, 0);
            int mY2 = (int)(my + bottomSb.scroll);
            int contentH = renderBottomContent(g, mx, mY2, startX, listY);
            pose.popPose();
            g.disableScissor();

            bottomSb.update(listH, contentH);
            bottomSb.render(g, startX + 9 * (SS + SP) + 2, listY);

            // Favorites label nahoře a seznam pod
            int favListY = listY + 12;
            int favListH = (pY + pH) - favListY - 5;
            g.drawString(font, "Favorite", favX, favListY - 11, 0xFFFFFFFF, false);
            renderFavorites(g, mx, my, favX, favCols, favListY, favListH);
        } else {
            boolean hDel = r.hit(mx, my, startX, invY + 4, 50, 14);
            boolean hUnl = r.hit(mx, my, startX + 54, invY + 4, 50, 14);
            r.drawBtn(g, "Delete", startX, invY + 4, 50, hDel, 0xFF4A1A1A, 0xFF6A2222);
            r.drawBtn(g, "Unload", startX + 54, invY + 4, 50, hUnl, C_BTN, C_BTN_H);
            renderRecipeList(g, mx, my, startX, listY, listH);
        }
    }

    private int bottomListY() {
        if (showRecipesList) return invY + 22;
        return bottomTab != BottomTab.INVENTORY ? invY + 38 : invY + 22;
    }

    private int renderBottomContent(GuiGraphics g, int mx, int mY, int startX, int listY) {
        if (bottomTab == BottomTab.INVENTORY && minecraft != null && minecraft.player != null) {
            Inventory inv = minecraft.player.getInventory();
            for (int row = 0; row < 3; row++) for (int col = 0; col < INV_COLS; col++)
                r.invSlotRender(g, mx, mY, inv.getItem(9 + row * INV_COLS + col), startX + col * (SS + SP), listY + row * (SS + SP));
            for (int col = 0; col < INV_COLS; col++)
                r.invSlotRender(g, mx, mY, inv.getItem(col), startX + col * (SS + SP), listY + 3 * (SS + SP) + 8);
            return 4 * (SS + SP) + 8;
        }
        String q = (searchBox != null) ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
        List<ItemStack> list = filteredList(q);
        for (int i = 0; i < list.size(); i++)
            r.invSlotRender(g, mx, mY, list.get(i), startX + (i % 9) * (SS + SP), listY + (i / 9) * (SS + SP));
        return ((list.size() + 8) / 9) * (SS + SP);
    }

    private List<ItemStack> filteredList(String q) {
        return switch (bottomTab) {
            case FLUIDS -> q.isEmpty() ? d.availableFluids :
                    d.availableFluids.stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
            case TAGS -> q.isEmpty() ? d.cachedTags :
                    d.cachedTags.stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
            case ITEMS -> {
                if (!q.equals(lastSearch)) {
                    lastSearch = q;
                    d.cachedFilteredItems.clear();
                    if (q.isEmpty()) d.cachedFilteredItems.addAll(d.allItems);
                    else if (q.startsWith("@")) {
                        String mod = q.substring(1);
                        d.cachedFilteredItems.addAll(d.allItems.stream().filter(s -> {
                            var loc = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
                            return loc.getNamespace().toLowerCase(Locale.ROOT).contains(mod);
                        }).toList());
                    } else {
                        d.cachedFilteredItems.addAll(d.allItems.stream()
                                .filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList());
                    }
                }
                yield d.cachedFilteredItems;
            }
            default -> List.of();
        };
    }

    private void renderFavorites(GuiGraphics g, int mx, int my, int favX, int favCols, int listY, int listH) {
        g.enableScissor(favX, listY, favX + favCols * (SS + SP), listY + listH);
        var pose = g.pose();
        pose.pushPose(); pose.translate(0, -favSb.scroll, 0);
        int mY = (int)(my + favSb.scroll);
        int favCount = Math.max(25, ((d.favorites.size() + favCols - 1) / favCols + 1) * favCols);
        for (int i = 0; i < favCount; i++) {
            int sx = favX + (i % favCols) * (SS + SP), sy = listY + (i / favCols) * (SS + SP);
            ItemStack s = i < d.favorites.size() ? d.favorites.get(i) : ItemStack.EMPTY;
            r.invSlotRender(g, mx, mY, s, sx, sy);
        }
        int favContentH = ((favCount + favCols - 1) / favCols) * (SS + SP);
        pose.popPose();
        g.disableScissor();
        favSb.update(listH, favContentH);
        favSb.render(g, favX + favCols * (SS + SP) + 2, listY);
    }

    private void renderRecipeList(GuiGraphics g, int mx, int my, int startX, int listY, int listH) {
        int recW = 9 * (SS + SP);
        int maxNameW = d.savedRecipeFiles.stream().mapToInt(f -> font.width(stripJson(f.getName()))).max().orElse(0);
        int rowW = Math.max(recW, maxNameW + 10);

        g.enableScissor(startX, listY, startX + recW, listY + listH);
        var pose = g.pose();
        pose.pushPose(); pose.translate(0, -recipeSb.scroll, 0);
        for (int i = 0; i < d.savedRecipeFiles.size(); i++) {
            File f = d.savedRecipeFiles.get(i);
            String name = stripJson(f.getName());
            int ry = listY + i * 14;
            boolean isSel = d.selectedRecipeFile != null && d.selectedRecipeFile.getAbsolutePath().equals(f.getAbsolutePath());
            boolean isHov = r.hit(mx, (int)(my + recipeSb.scroll), startX, ry, recW, 14);
            if (isSel)      g.fill(startX, ry, startX + rowW, ry + 14, 0xFF2255AA);
            else if (isHov) g.fill(startX, ry, startX + rowW, ry + 14, 0xFF333333);
            g.drawString(font, name, startX + 4, ry + 3, isSel || isHov ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        }
        pose.popPose();
        g.disableScissor();
        recipeSb.update(listH, d.savedRecipeFiles.size() * 14);
        recipeSb.render(g, startX + recW - 5, listY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CENTRALIZOVANÁ SLOT GEOMETRIE
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Vrátí seznam item-slotů (NE fluid-slotů, ty mají vlastní handling) pro aktuální tab.
     * Tatáž geometrie použitá pro render, hit-test, drop, clear a scroll.
     */
    private List<SlotPos> itemSlots(StationType t) {
        List<SlotPos> out = new ArrayList<>();
        int cx = pX + leftW / 2;
        switch (t) {
            case CRAFTING -> {
                int cy = editorY + 50;
                // 3x3 grid
                for (int i = 0; i < 9; i++) {
                    int col = i % 3, row = i / 3;
                    int bx = cx - 70 + col * (SS + SP);
                    int by = cy + row * (SS + SP);
                    int idx = i;
                    out.add(new SlotPos(bx, by, SS, () -> d.craftGrid.get(idx), s -> d.craftGrid.set(idx, s)));
                }
                int ax = cx - 70 + 3 * (SS + SP) + 15, ay = cy + SS + SP;
                out.add(new SlotPos(ax + 20, ay - 9, SS, () -> d.craftResult, s -> d.craftResult = s));
            }
            case MECH_CRAFTING -> {
                int cy = editorY + 50, sz = 16, pad = 1, gW = 9 * (sz + pad);
                int sx = cx - gW / 2 - 40;
                for (int i = 0; i < 81; i++) {
                    int col = i % 9, row = i / 9;
                    int bx = sx + col * (sz + pad), by = cy + row * (sz + pad);
                    int idx = i;
                    out.add(new SlotPos(bx, by, sz, () -> d.mechGrid.get(idx), s -> d.mechGrid.set(idx, s)));
                }
                int ay = cy + (9 * (sz + pad)) / 2 - 4;
                int rx = sx + gW + 15 + 20;
                out.add(new SlotPos(rx, ay - 4, SS, () -> d.craftResult, s -> d.craftResult = s));
            }
            case FURNACE -> {
                int cy = editorY + 60, sx = cx - IO_INPUT_OFFSET;
                out.add(new SlotPos(sx, cy, SS, () -> d.furnIn,  s -> d.furnIn  = s));
                out.add(new SlotPos(sx + SS + IO_GAP, cy, SS, () -> d.furnOut, s -> d.furnOut = s));
            }
            case STONECUTTER -> {
                int cy = editorY + 40, sx = cx - IO_INPUT_OFFSET;
                out.add(new SlotPos(sx, cy, SS, () -> d.stoneIn,  s -> d.stoneIn  = s));
                out.add(new SlotPos(sx + SS + IO_GAP, cy, SS, () -> d.stoneOut, s -> d.stoneOut = s));
            }
            case SMITHING -> {
                int cy = editorY + 40, step = SS + 36;
                int totalW = 3 * step + 20 + SS, sx = cx - totalW / 2;
                out.add(new SlotPos(sx,             cy, SS, () -> d.smTemplate, s -> d.smTemplate = s));
                out.add(new SlotPos(sx + step,      cy, SS, () -> d.smBase,     s -> d.smBase     = s));
                out.add(new SlotPos(sx + 2 * step,  cy, SS, () -> d.smAddition, s -> d.smAddition = s));
                out.add(new SlotPos(sx + 3 * step + 16, cy, SS, () -> d.smResult, s -> d.smResult = s));
            }
            case MIXING -> {
                // 3x3 grid (padding 24 → vedle slotu spinner)
                int cy = editorY + 70, sx = cx - 134;
                for (int i = 0; i < 9; i++) {
                    int col = i % 3, row = i / 3;
                    int bx = sx + col * (SS + 24);
                    int by = cy + row * (SS + 10);
                    int idx = i;
                    out.add(new SlotPos(bx, by, SS, () -> d.mixIng.get(idx), s -> d.mixIng.set(idx, s)));
                }
                int rx = cx + 10;
                for (int i = 0; i < 4; i++) {
                    int col = i % 2, row = i / 2;
                    int ox = rx + col * 90, oy = cy + row * 30;
                    int idx = i;
                    out.add(new SlotPos(ox, oy, SS,
                            () -> d.mixOuts.get(idx).stack,
                            s -> d.mixOuts.get(idx).stack = s));
                }
            }
            case PRESSING -> {
                int gridY = editorY + 45, sx = cx - 70;
                out.add(new SlotPos(sx,            gridY, SS, () -> d.pressIng.get(0), s -> d.pressIng.set(0, s)));
                out.add(new SlotPos(sx + SS + 50, gridY, SS,
                        () -> d.pressOuts.get(0).stack,
                        s -> d.pressOuts.get(0).stack = s));
            }
            case CRUSHING -> {
                int cy = editorY + 50, sx = cx - 120, outX = sx + SS + 30, colW = 110;
                out.add(new SlotPos(sx, cy, SS, () -> d.crushIn, s -> d.crushIn = s));
                for (int i = 0; i < 8; i++) {
                    int ox = outX + (i / 4) * colW, oy = cy + (i % 4) * (SS + 12);
                    int idx = i;
                    out.add(new SlotPos(ox, oy, SS,
                            () -> d.crushOuts.get(idx).stack,
                            s -> d.crushOuts.get(idx).stack = s));
                }
            }
            case FAN -> {
                int cy = editorY + 50, sx = cx - 120, outX = sx + SS + 30, colW = 110;
                out.add(new SlotPos(sx, cy, SS, () -> d.fanIn, s -> d.fanIn = s));
                for (int i = 0; i < 4; i++) {
                    int ox = outX + (i / 2) * colW, oy = cy + (i % 2) * (SS + 12);
                    int idx = i;
                    out.add(new SlotPos(ox, oy, SS,
                            () -> d.fanOuts.get(idx).stack,
                            s -> d.fanOuts.get(idx).stack = s));
                }
            }
        }
        return out;
    }

    /** Fluid sloty (samostatně - mají vlastní settery). */
    private List<FluidPos> fluidSlots(StationType t) {
        List<FluidPos> out = new ArrayList<>();
        if (t == StationType.MIXING) {
            int cx = pX + leftW / 2, cy = editorY + 70;
            int sx = cx - 134, fluidY = cy + 95, rx = cx + 10;
            for (int i = 0; i < 2; i++) {
                int idx = i;
                out.add(new FluidPos(sx + i * 70, fluidY, () -> d.mixFluidIng.get(idx)));
            }
            for (int i = 0; i < 2; i++) {
                int idx = i;
                out.add(new FluidPos(rx + i * 65, fluidY, () -> d.mixFluidOuts.get(idx)));
            }
        }
        return out;
    }

    /** Pomocná struktura — pozice slotu s gettrem a setterem. */
    private record SlotPos(int x, int y, int size, Supplier<ItemStack> get, Consumer<ItemStack> set) {
        boolean hit(int mx, int my) { return mx >= x && mx <= x + size && my >= y && my <= y + size; }
    }

    private record FluidPos(int x, int y, Supplier<FluidEntry> get) {
        boolean hit(int mx, int my) { return mx >= x && mx <= x + SS && my >= y && my <= y + SS; }
    }

    // ── Input ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int)mouseX, my = (int)mouseY;
        int mY = (int)(my + editorSb.scroll);

        if (d.popupError != null) {
            if (button == 0) {
                int pw=260, ph=100, px2=(width-pw)/2, py2=(height-ph)/2;
                int bx=px2+(pw-60)/2, by=py2+65;
                if (r.hit(mx, my, bx, by, 60, 18)) d.popupError = null;
            }
            return true;
        }

        if (isDragging) {
            if (button == 1) { dragStack = ItemStack.EMPTY; isDragging = false; return true; }
            if (button == 0) {
                if (r.hit(mx, my, pX, editorY, leftW, editorH)) drop(mx, mY, dragStack);
                else dropToFavorites(mx, my);
                if (!hasShiftDown()) { dragStack = ItemStack.EMPTY; isDragging = false; }
                return true;
            }
        }

        long now = System.currentTimeMillis();
        boolean isDbl = button == 0 && now - lastClickTime < 250
                && Math.abs(mx - lastClickX) < 5 && Math.abs(my - lastClickY) < 5;
        lastClickTime = now; lastClickX = mx; lastClickY = my;

        if (activeNumEditBox != null) {
            if (!activeNumEditBox.mouseClicked(mx, my, button)) applyActiveNumEdit();
            else return true;
        }

        if (isDbl && r.hit(mx, my, pX, editorY, leftW, editorH)) {
            if (handleDoubleClick(mx, mY)) return true;
        }

        if (r.hit(mx, my, pX, invY - 4, leftW, 8)) { isDraggingSplitter = true; return true; }

        if (bottomTab != BottomTab.INVENTORY && !showRecipesList && searchBox.mouseClicked(mx, my, button)) {
            searchBox.setFocused(true); return true;
        }

        fnFocused = false;
        int ffx = btnCopyX + 65 + font.width("File:") + 5;
        if (r.hit(mx, my, ffx, btnSaveY, leftW - ffx - 10, 16)) { fnFocused = true; return true; }

        // Tab klik
        int tabW2 = leftW / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            int tx = pX + i * tabW2, tw = (i == tabs.size()-1) ? (pX+leftW-tx) : tabW2;
            if (r.hit(mx, my, tx, pY, tw, TAB_H)) { tabIdx = i; editorSb.reset(); return true; }
        }

        if (System.currentTimeMillis() - lastBtnClickTime >= 250) {
            if (r.hit(mx, my, btnSaveX, btnSaveY, 92, 16))  { lastBtnClickTime = now; save(); return true; }
            if (r.hit(mx, my, btnClearX, btnSaveY, 40, 16)) { lastBtnClickTime = now; d.clear(); fileName = "my_recipe"; fnCursor = fileName.length(); return true; }
            if (r.hit(mx, my, btnCopyX, btnSaveY, 60, 16))  { lastBtnClickTime = now; if (minecraft!=null) minecraft.keyboardHandler.setClipboard(d.buildJson(tabs, tabIdx)); d.status("Copied!", true); return true; }
        }

        // Bottom tabs
        String[] bTabs = {"Inventory","Fluids","Items","Tags"};
        int tx2 = pX + 10;
        for (int i = 0; i < bTabs.length; i++) {
            int tw = font.width(bTabs[i]) + 10;
            if (!showRecipesList && r.hit(mx, my, tx2, invY + 4, tw, 14)) {
                bottomTab = BottomTab.values()[i]; bottomSb.reset(); showRecipesList = false; return true;
            }
            tx2 += tw + 4;
        }

        // Recipes button
        int startX = pX + 10, favCols = 5, favX = startX + 9*(SS+SP) + 16;
        int txTabsEnd = startX;
        for (String s : bTabs) txTabsEnd += font.width(s) + 14;
        int recBtnX = txTabsEnd;
        int bw = font.width(showRecipesList ? "◀ Items" : "Recipes ▶") + 10;
        if (r.hit(mx, my, recBtnX, invY + 4, bw, 14)) { showRecipesList = !showRecipesList; return true; }

        // Recipe list click
        if (showRecipesList) {
            int recW = 9*(SS+SP);
            int listY = invY + 22;
            if (r.hit(mx, my, startX, invY + 4, 50, 14))      { deleteRecipe(); return true; }
            if (r.hit(mx, my, startX + 54, invY + 4, 50, 14)) { unloadRecipe(); return true; }
            for (int i = 0; i < d.savedRecipeFiles.size(); i++) {
                int ry = (int)(listY + i * 14 - recipeSb.scroll);
                if (r.hit(mx, my, startX, ry, recW, 14)) {
                    File f = d.savedRecipeFiles.get(i);
                    if (d.selectedRecipeFile != null && d.selectedRecipeFile.equals(f)) {
                        loadRecipe(f);
                    } else {
                        d.selectedRecipeFile = f;
                        try {
                            String content = java.nio.file.Files.readString(f.toPath());
                            codeViewer = new CodeViewerWidget(font, content);
                            codeViewer.setBounds(rightX, pY, rightW, pH);
                        } catch (Exception ignored) {}
                    }
                    return true;
                }
            }
        }

        // Editor clicks (mode toggles, spinners, fluid spinners)
        if (r.hit(mx, my, pX, editorY, leftW, editorH)) {
            if (button == 1 && clearSlot(mx, mY)) return true;
            if (button == 0 && handleEditorClicks(mx, mY)) return true;
        }

        // Bottom area right-click (remove favorite)
        if (button == 1 && !showRecipesList) {
            int listY2 = bottomListY();
            int favListX = startX + 9*(SS+SP) + 16;
            int mY2 = (int)(my + favSb.scroll);
            for (int i = 0; i < d.favorites.size(); i++) {
                int sx = favListX + (i % favCols) * (SS+SP), sy = listY2 + 12 + (i / favCols) * (SS+SP);
                if (r.hit(mx, mY2, sx, sy, SS, SS)) { d.favorites.remove(i); d.saveFavorites(minecraft); return true; }
            }
        }

        // Drag start (item)
        if (button == 0) {
            ItemStack fi = slotAt(mx, my);
            if (fi != null && !fi.isEmpty()) {
                dragStack = fi.copy(); dragStack.setCount(1);
                isDragging = true; dragX = mx; dragY = my;
                return true;
            }
        }
        // Drag scrollbars
        if (button == 0) {
            int listY = bottomListY();
            int listH = pH - listY - 5;
            if (!showRecipesList) {
                if (bottomSb.startDragIfHit(mx, my)) return true;
                int favListY = listY + 12;
                if (favSb.startDragIfHit(mx, my)) return true;
            } else {
                if (recipeSb.startDragIfHit(mx, my)) return true;
            }
            // Editor scrollbar
            if (editorSb.startDragIfHit(mx, my)) return true;
        }

        codeViewer.mouseClicked(mx, my, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingSplitter) {
            int min = 2*(SS+SP)+40, max = pH-(pY+TAB_H+40);
            invPanelHeight = Math.clamp(pH - (int)my, min, max);
            updateLayout();
            if (codeViewer != null) codeViewer.setBounds(rightX, pY, rightW, pH);
            return true;
        }
        if (bottomSb.dragging) { bottomSb.dragTo((int)my); return true; }
        if (favSb.dragging)    { favSb.dragTo((int)my);    return true; }
        if (recipeSb.dragging) { recipeSb.dragTo((int)my); return true; }
        if (editorSb.dragging) { editorSb.dragTo((int)my); return true; }
        if (isDragging) { dragX = (int)mx; dragY = (int)my; return true; }
        if (codeViewer != null && codeViewer.mouseDragged((int)my)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (isDraggingSplitter) {
            isDraggingSplitter = false;
            d.saveConfig(minecraft, invPanelHeight);
            return true;
        }
        bottomSb.stopDrag(); favSb.stopDrag(); recipeSb.stopDrag(); editorSb.stopDrag();
        if (button == 0 && codeViewer != null) codeViewer.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        // Wheel uvnitř editoru: nejdřív otestuj jestli kursor je na item slotu (změna count)
        if (r.hit((int)mx, (int)my, pX, editorY, leftW, editorH)) {
            int mY = (int)(my + editorSb.scroll);
            StationType t = tabs.get(tabIdx);

            // Pokud je nad nějakým slotem v aktuálním tabu, pokus se změnit count
            if (handleScrollOverSlot(t, (int)mx, mY, sy)) return true;

            // Wheel přes různé custom spinnery (count slot výstupy)
            if (handleScrollSpinners(t, (int)mx, mY, sy)) return true;

            // Jinak skroluj editor
            editorSb.handleScroll(sy, 20);
            return true;
        }

        // Bottom area
        int listY = bottomListY();
        int listH = pH - listY - 5;
        int startX = pX + 10;
        if (!showRecipesList && r.hit((int)mx, (int)my, startX, listY, 9*(SS+SP), listH)) {
            bottomSb.handleScroll(sy, 20);
            return true;
        }
        int favCols = 5, favX = startX + 9*(SS+SP) + 16;
        int favListY = listY + 12;
        int favListH = pH - favListY - 5;
        if (r.hit((int)mx, (int)my, favX, favListY, favCols*(SS+SP), favListH)) {
            favSb.handleScroll(sy, 20);
            return true;
        }
        if (showRecipesList) {
            recipeSb.handleScroll(sy, 12);
            return true;
        }
        if (codeViewer != null) return codeViewer.mouseScrolled(sy, (int)mx, (int)my);
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /** Wheel nad item slotem v gridu → změň count u toho slotu. */
    private boolean handleScrollOverSlot(StationType t, int mx, int mY, double sy) {
        // Pouze gridy s počítatelnými itemy (crafting, mech crafting, mixing)
        if (t != StationType.CRAFTING && t != StationType.MECH_CRAFTING && t != StationType.MIXING) return false;
        int maxIdx = switch (t) {
            case CRAFTING -> 9;
            case MECH_CRAFTING -> 81;
            case MIXING -> 9;
            default -> 0;
        };
        var slots = itemSlots(t);
        for (int i = 0; i < maxIdx && i < slots.size(); i++) {
            SlotPos sp = slots.get(i);
            if (sp.hit(mx, mY)) {
                ItemStack s = sp.get.get();
                if (!s.isEmpty()) {
                    s.setCount(Math.clamp(s.getCount() + (int)sy, 1, 64));
                    return true;
                }
            }
        }
        return false;
    }

    /** Wheel nad spinnerem výstupu (count u CrushingOutput) — sjednocené přes všechna stanoviště. */
    private boolean handleScrollSpinners(StationType t, int mx, int mY, double sy) {
        int cx = pX + leftW / 2;
        switch (t) {
            case CRAFTING -> {
                int cy = editorY + 50, ax = cx - 70 + 3*(SS+SP) + 15, ay = cy + SS + SP;
                if (r.hit(mx, mY, ax + 20, ay - 9, SS, SS)) {
                    d.craftCount = Math.clamp(d.craftCount + (int)sy, 1, 64); return true;
                }
            }
            case MECH_CRAFTING -> {
                int cy = editorY + 50, sz = 16, pad = 1, gW = 9*(sz+pad);
                int sx = cx - gW/2 - 40, ay = cy + (9*(sz+pad))/2 - 4;
                if (r.hit(mx, mY, sx + gW + 15 + 20, ay - 4, SS, SS)) {
                    d.craftCount = Math.clamp(d.craftCount + (int)sy, 1, 64); return true;
                }
            }
            case FURNACE -> {
                int cy = editorY + 60;
                if (r.hit(mx, mY, cx - IO_INPUT_OFFSET + SS + IO_GAP, cy, SS, SS)) {
                    d.furnCount = Math.clamp(d.furnCount + (int)sy, 1, 64); return true;
                }
            }
            case STONECUTTER -> {
                int cy = editorY + 40;
                if (r.hit(mx, mY, cx - IO_INPUT_OFFSET + SS + IO_GAP, cy, SS, SS)) {
                    d.stoneCount = Math.clamp(d.stoneCount + (int)sy, 1, 64); return true;
                }
            }
            case SMITHING -> {
                int cy = editorY + 40, step = SS + 36, totalW = 3*step + 20 + SS, sx = cx - totalW/2;
                if (r.hit(mx, mY, sx + 3*step + 16, cy, SS, SS)) {
                    d.smCount = Math.clamp(d.smCount + (int)sy, 1, 64); return true;
                }
            }
            case PRESSING -> {
                int gridY = editorY + 45, sx = cx - 70;
                if (r.hit(mx, mY, sx + SS + 50, gridY, SS, SS)) {
                    CrushingOutput co = d.pressOuts.get(0);
                    if (!co.isEmpty()) { co.count = Math.clamp(co.count + (int)sy, 1, 64); return true; }
                }
            }
            case CRUSHING -> {
                int cy = editorY + 50, outX = cx - 120 + SS + 30, colW = 110;
                for (int i = 0; i < 8; i++) {
                    if (r.hit(mx, mY, outX + (i/4)*colW, cy + (i%4)*(SS+12), SS, SS)) {
                        CrushingOutput co = d.crushOuts.get(i);
                        if (!co.isEmpty()) { co.count = Math.clamp(co.count + (int)sy, 1, 64); return true; }
                    }
                }
            }
            case FAN -> {
                int cy = editorY + 50, outX = cx - 120 + SS + 30, colW = 110;
                for (int i = 0; i < 4; i++) {
                    if (r.hit(mx, mY, outX + (i/2)*colW, cy + (i%2)*(SS+12), SS, SS)) {
                        CrushingOutput co = d.fanOuts.get(i);
                        if (!co.isEmpty()) { co.count = Math.clamp(co.count + (int)sy, 1, 64); return true; }
                    }
                }
            }
            default -> {}
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (activeNumEditBox != null && activeNumEditBox.isFocused()) {
            if (key == 257 || key == 335) { applyActiveNumEdit(); return true; }
            if (key == 256) { activeNumEditBox = null; activeFieldName = null; activeFieldIdx = -1; return true; }
            activeNumEditBox.keyPressed(key, scan, mods); return true;
        }
        if (key == 256) { if (fnFocused) { fnFocused = false; return true; } onClose(); return true; }
        if (bottomTab != BottomTab.INVENTORY && !showRecipesList && searchBox.isFocused()) {
            searchBox.keyPressed(key, scan, mods); return true;
        }
        if (fnFocused) {
            if (key == 259 && !fileName.isEmpty() && fnCursor > 0) { fileName = fileName.substring(0, fnCursor-1) + fileName.substring(fnCursor); fnCursor--; }
            else if (key == 261 && fnCursor < fileName.length())  { fileName = fileName.substring(0, fnCursor) + fileName.substring(fnCursor+1); }
            else if (key == 263) fnCursor = Math.max(0, fnCursor - 1);
            else if (key == 262) fnCursor = Math.min(fileName.length(), fnCursor + 1);
            return true;
        }
        if (codeViewer != null && codeViewer.keyPressed(key, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (activeNumEditBox != null && activeNumEditBox.isFocused()) {
            if (Character.isDigit(chr) || (activeFieldName != null && activeFieldName.equals("furnXp") && chr == '.'))
                return activeNumEditBox.charTyped(chr, mods);
            return true;
        }
        if (bottomTab != BottomTab.INVENTORY && !showRecipesList && searchBox.isFocused()) {
            searchBox.charTyped(chr, mods); return true;
        }
        if (fnFocused) {
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '/') {
                fileName = fileName.substring(0, fnCursor) + chr + fileName.substring(fnCursor); fnCursor++;
            }
            return true;
        }
        if (codeViewer != null && codeViewer.charTyped(chr)) return true;
        return super.charTyped(chr, mods);
    }

    @Override public void onClose() { assert minecraft != null; minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    // ── Akce ─────────────────────────────────────────────────────────────────

    private void save() {
        String j = d.buildJson(tabs, tabIdx);
        try {
            var dir = RecipeFileWriter.getRecipeDir();
            java.nio.file.Files.createDirectories(dir);
            String safe = fileName.replaceAll("[^a-z0-9_/]", "_").toLowerCase();
            if (safe.isBlank()) safe = "recipe";
            var file = dir.resolve(safe + ".json");
            java.nio.file.Files.writeString(file, j, java.nio.charset.StandardCharsets.UTF_8);
            d.scanSavedRecipes();
            for (File f : d.savedRecipeFiles)
                if (f.getName().equalsIgnoreCase(safe + ".json")) { d.selectedRecipeFile = f; break; }
            d.status("Saved!", true);
        } catch (Exception e) { d.status("Save failed!", false); }
    }

    private void deleteRecipe() {
        if (d.selectedRecipeFile != null && d.selectedRecipeFile.exists()) {
            try { java.nio.file.Files.delete(d.selectedRecipeFile.toPath()); } catch (Exception ignored) {}
            d.selectedRecipeFile = null;
            fileName = ""; fnCursor = 0; d.clear(); d.scanSavedRecipes(); d.status("Deleted!", true);
        }
    }

    private void unloadRecipe() {
        d.selectedRecipeFile = null; fileName = ""; fnCursor = 0; d.clear(); d.status("Unloaded!", true);
    }

    private void loadRecipe(File f) {
        String err = d.loadRecipeFile(f, tabs);
        if (err != null) { d.popupError = err; return; }
        // Přepni na správnou záložku
        try {
            String json2 = java.nio.file.Files.readString(f.toPath());
            String type = com.google.gson.JsonParser.parseString(json2).getAsJsonObject().get("type").getAsString();
            StationType target = detectTabForType(type);
            for (int i = 0; i < tabs.size(); i++) if (tabs.get(i) == target) { tabIdx = i; break; }
        } catch (Exception ignored) {}
        d.selectedRecipeFile = f;
        fileName = stripJson(f.getName()); fnCursor = fileName.length();
        d.status("Recipe loaded!", true);
    }

    private StationType detectTabForType(String type) {
        return switch (type) {
            case "minecraft:crafting_shaped","minecraft:crafting_shapeless" -> StationType.CRAFTING;
            case "minecraft:smelting","minecraft:blasting","minecraft:smoking","minecraft:campfire_cooking" -> StationType.FURNACE;
            case "minecraft:stonecutting" -> StationType.STONECUTTER;
            case "minecraft:smithing_transform" -> StationType.SMITHING;
            case "create:mechanical_crafting" -> StationType.MECH_CRAFTING;
            case "create:mixing" -> StationType.MIXING;
            case "create:pressing","create:compacting" -> StationType.PRESSING;
            case "create:crushing","create:milling" -> StationType.CRUSHING;
            case "create:splashing","create:haunting" -> StationType.FAN;
            default -> tabs.get(tabIdx);
        };
    }

    private void dropToFavorites(int mx, int my) {
        int startX = pX + 10, favX = startX + 9*(SS+SP) + 16;
        if (r.hit(mx, my, favX, invY, leftW, pH - invY)) {
            boolean found = d.favorites.stream().anyMatch(s -> ItemStack.isSameItem(s, dragStack));
            if (!found && !dragStack.isEmpty()) { d.favorites.add(dragStack.copy()); d.saveFavorites(minecraft); }
        }
    }

    // ── JSON update ──────────────────────────────────────────────────────────

    private void updateJson() {
        String j = d.buildJson(tabs, tabIdx);
        if (!j.equals(curJson)) {
            curJson = j;
            codeViewer = new CodeViewerWidget(font, curJson);
            codeViewer.setBounds(rightX, pY, rightW, pH);
        }
    }

    // ── Slot drop / clear (centralizováno přes itemSlots/fluidSlots) ─────────

    private void drop(int mx, int mY, ItemStack s) {
        StationType t = tabs.get(tabIdx);
        // Item slot drop
        for (SlotPos sp : itemSlots(t)) {
            if (sp.hit(mx, mY)) {
                ItemStack dropped = s.copy();
                // Pokud je to mixing grid ingredient slot (3x3 na začátku), zachovej existing count
                if (t == StationType.MIXING) {
                    int idx = itemSlots(t).indexOf(sp);
                    if (idx < 9) { // ingredient slot
                        int existing = sp.get.get().getCount();
                        dropped.setCount(existing > 0 ? existing : 1);
                    }
                }
                sp.set.accept(dropped);
                return;
            }
        }
        // Fluid slot drop
        for (FluidPos fp : fluidSlots(t)) {
            if (fp.hit(mx, mY)) {
                fp.get.get().proxy = s.copy();
                return;
            }
        }
    }

    private boolean clearSlot(int mx, int mY) {
        StationType t = tabs.get(tabIdx);
        for (SlotPos sp : itemSlots(t)) {
            if (sp.hit(mx, mY)) { sp.set.accept(ItemStack.EMPTY); return true; }
        }
        for (FluidPos fp : fluidSlots(t)) {
            if (fp.hit(mx, mY)) { fp.get.get().proxy = ItemStack.EMPTY; return true; }
        }
        return false;
    }

    /** Hit-test slotu pod kursorem (pro tooltip a drag start). */
    private ItemStack slotAt(int mx, int my) {
        int mY = (int)(my + editorSb.scroll);
        if (r.hit(mx, my, pX, editorY, leftW, editorH)) {
            StationType t = tabs.get(tabIdx);
            for (SlotPos sp : itemSlots(t)) {
                if (sp.hit(mx, mY)) return sp.get.get();
            }
            for (FluidPos fp : fluidSlots(t)) {
                if (fp.hit(mx, mY)) return fp.get.get().proxy;
            }
        }
        // Bottom area
        int startX = pX + 10;
        int listY = bottomListY();
        int listH = pH - listY - 5;
        if (!showRecipesList && r.hit(mx, my, startX, listY, 9*(SS+SP), listH)) {
            int mY2 = (int)(my + bottomSb.scroll);
            if (bottomTab == BottomTab.INVENTORY && minecraft != null && minecraft.player != null) {
                Inventory inv = minecraft.player.getInventory();
                for (int row = 0; row < 3; row++) for (int col = 0; col < INV_COLS; col++)
                    if (r.hit(mx, mY2, startX + col*(SS+SP), listY + row*(SS+SP), SS, SS))
                        return inv.getItem(9 + row*INV_COLS + col);
                for (int col = 0; col < INV_COLS; col++)
                    if (r.hit(mx, mY2, startX + col*(SS+SP), listY + 3*(SS+SP) + 8, SS, SS))
                        return inv.getItem(col);
            }
            List<ItemStack> list = switch (bottomTab) {
                case FLUIDS -> d.availableFluids;
                case ITEMS  -> d.cachedFilteredItems;
                case TAGS   -> d.cachedTags;
                default     -> List.of();
            };
            for (int i = 0; i < list.size(); i++)
                if (r.hit(mx, mY2, startX + (i%9)*(SS+SP), listY + (i/9)*(SS+SP), SS, SS))
                    return list.get(i);
        }
        // Favorites
        int favCols2 = 5, favX2 = startX + 9*(SS+SP) + 16;
        int favListY = listY + 12;
        int favListH = pH - favListY - 5;
        if (!showRecipesList && r.hit(mx, my, favX2, favListY, favCols2*(SS+SP), favListH)) {
            int mY3 = (int)(my + favSb.scroll);
            for (int i = 0; i < d.favorites.size(); i++)
                if (r.hit(mx, mY3, favX2 + (i%favCols2)*(SS+SP), favListY + (i/favCols2)*(SS+SP), SS, SS))
                    return d.favorites.get(i);
        }
        return ItemStack.EMPTY;
    }

    // ── Mode toggles + spinners + fluid spins ────────────────────────────────

    private boolean handleEditorClicks(int mx, int mY) {
        StationType t = tabs.get(tabIdx); int cx = pX + leftW / 2;

        // Mode toggle podle typu
        if (t == StationType.MIXING) {
            int toggleX = cx - 60, toggleY = editorY + 15;
            int wa = font.width("Mixer") + 12, wb = font.width("Press") + 12;
            if (r.hit(mx, mY, toggleX, toggleY, wa, 16))         { d.mixBasinPress = false; return true; }
            if (r.hit(mx, mY, toggleX + wa + 2, toggleY, wb, 16)) { d.mixBasinPress = true; return true; }
            // Heat picker
            int heatY = editorY + 40, tw = 0;
            for (String l : d.heatLabels) tw += font.width(l) + 16;
            int bx = cx - tw / 2;
            for (int i = 0; i < d.heatLabels.length; i++) {
                int bw = font.width(d.heatLabels[i]) + 10;
                if (r.hit(mx, mY, bx, heatY, bw, 16)) { d.mixHeat = i; return true; }
                bx += bw + 6;
            }
        }
        if (t == StationType.FAN) {
            int cy = editorY + 15, wa = font.width("Washing") + 12, wb = font.width("Haunting") + 12;
            if (r.hit(mx, mY, cx - 65, cy, wa, 16))         { d.fanHaunting = false; return true; }
            if (r.hit(mx, mY, cx - 65 + wa + 2, cy, wb, 16)) { d.fanHaunting = true; return true; }
        }
        if (t == StationType.CRAFTING) {
            int cy = editorY + 20, wa = font.width("Shaped") + 12, wb = font.width("Shapeless") + 12;
            if (r.hit(mx, mY, cx - 70, cy, wa, 16))         { d.shapeless = false; return true; }
            if (r.hit(mx, mY, cx - 70 + wa + 2, cy, wb, 16)) { d.shapeless = true; return true; }
        }
        if (t == StationType.MECH_CRAFTING) {
            int cy = editorY + 20, wa = font.width("Mirrored") + 12, wb = font.width("Exact") + 12;
            if (r.hit(mx, mY, cx - 60, cy, wa, 16))         { d.mechMirrored = true; return true; }
            if (r.hit(mx, mY, cx - 60 + wa + 2, cy, wb, 16)) { d.mechMirrored = false; return true; }
        }
        if (t == StationType.FURNACE) {
            int cy = editorY + 20, tw = 0;
            for (String l : d.furnLabels) tw += font.width(l) + 16;
            int bx = cx - tw / 2;
            for (int i = 0; i < d.furnLabels.length; i++) {
                int bw = font.width(d.furnLabels[i]) + 10;
                if (r.hit(mx, mY, bx, cy, bw, 16)) { d.furnSubIdx = i; return true; }
                bx += bw + 6;
            }
        }
        if (t == StationType.CRUSHING) {
            int cy = editorY + 15, wa = font.width("Crushing") + 12, wb = font.width("Milling") + 12;
            if (r.hit(mx, mY, cx - 55, cy, wa, 16))         { d.isMilling = false; return true; }
            if (r.hit(mx, mY, cx - 55 + wa + 2, cy, wb, 16)) { d.isMilling = true; return true; }
        }

        return handleSpinnerClicks(mx, mY) || handleFluidSpins(mx, mY);
    }

    /**
     * Klik na spinner +/- tlačítka u count/chance/time.
     * Pomocí helper metod kompaktováno.
     */
    private boolean handleSpinnerClicks(int mx, int mY) {
        StationType t = tabs.get(tabIdx); int cx = pX + leftW / 2;

        if (t == StationType.CRAFTING) {
            int cy = editorY + 50, ax = cx - 70 + 3*(SS+SP) + 15, rx = ax + 20, cpx = rx + SS + 6, cpy = cy + SS + SP - 7;
            return countSpinner(mx, mY, cpx + 18, cpy, () -> d.craftCount, v -> d.craftCount = v);
        }
        if (t == StationType.MECH_CRAFTING) {
            int cy = editorY + 50, sz = 16, pad = 1, gW = 9*(sz+pad), sx = cx - gW/2 - 40;
            int ay = cy + (9*(sz+pad))/2 - 4, rx = sx + gW + 15 + 20, cpx = rx + SS + 6, cpy = ay - 2;
            return countSpinner(mx, mY, cpx + 18, cpy, () -> d.craftCount, v -> d.craftCount = v);
        }
        if (t == StationType.FURNACE) {
            int cy = editorY + 60, sx = cx - IO_INPUT_OFFSET, rx = sx + SS + IO_GAP, cpx = rx + SS + 6, cpy = cy + 2;
            if (countSpinner(mx, mY, cpx + 18, cpy, () -> d.furnCount, v -> d.furnCount = v)) return true;
            int xpX = cx - 20, xpY = cy + 42;
            if (r.hit(mx, mY, xpX, xpY, SPIN_W, SPIN_H)) { d.furnXp = Math.min(100f, d.furnXp + 0.1f); return true; }
            if (r.hit(mx, mY, xpX, xpY + 8, SPIN_W, SPIN_H)) { d.furnXp = Math.max(0f, d.furnXp - 0.1f); return true; }
            int tX = cx + 80, tY = cy + 42;
            if (r.hit(mx, mY, tX, tY, SPIN_W, SPIN_H)) { d.furnTime = Math.min(10000, d.furnTime + 50); return true; }
            if (r.hit(mx, mY, tX, tY + 8, SPIN_W, SPIN_H)) { d.furnTime = Math.max(10, d.furnTime - 50); return true; }
        }
        if (t == StationType.STONECUTTER) {
            int cy = editorY + 40, sx = cx - IO_INPUT_OFFSET, rx = sx + SS + IO_GAP, cpx = rx + SS + 6, cpy = cy + 2;
            return countSpinner(mx, mY, cpx + 18, cpy, () -> d.stoneCount, v -> d.stoneCount = v);
        }
        if (t == StationType.SMITHING) {
            int cy = editorY + 40, step = SS + 36, totalW = 3*step + 20 + SS;
            int sx = cx - totalW/2, rx = sx + 3*step + 16, cpx = rx + SS + 6, cpy = cy + 2;
            return countSpinner(mx, mY, cpx + 18, cpy, () -> d.smCount, v -> d.smCount = v);
        }
        if (t == StationType.PRESSING) {
            int gridY = editorY + 45, sx = cx - 70, rx = sx + SS + 50, cpx = rx + SS + 4, cpy = gridY + 2, chX = cpx + 28;
            CrushingOutput co = d.pressOuts.get(0);
            if (miniCountChance(mx, mY, cpx + 16, chX, cpy, co)) return true;
        }
        if (t == StationType.MIXING) {
            // Mixing grid spinner (count items v ingredient slotu)
            int cy = editorY + 70, sx = cx - 134;
            for (int i = 0; i < 9; i++) {
                int col = i % 3, row = i / 3;
                int bx = sx + col * (SS + 24), by = cy + row * (SS + 10);
                ItemStack s = d.mixIng.get(i);
                if (!s.isEmpty()) {
                    int spX = bx + SS + 1 + 20, spY = by + 2 - 2;
                    if (r.hit(mx, mY, spX, spY, MINI_SPIN, MINI_SPIN)) { s.setCount(Math.min(64, s.getCount()+1)); return true; }
                    if (r.hit(mx, mY, spX, spY + 9, MINI_SPIN, MINI_SPIN)) { s.setCount(Math.max(1, s.getCount()-1)); return true; }
                }
            }
            // Output spinnery (count+chance)
            int rx = cx + 10;
            for (int i = 0; i < 4; i++) {
                CrushingOutput co = d.mixOuts.get(i);
                if (co.isEmpty()) continue;
                int col = i % 2, row = i / 2;
                int ox = rx + col * 90, oy = cy + row * 30;
                int cpx = ox + SS + 4, cpy = oy + 2, chX = cpx + 28;
                if (miniCountChance(mx, mY, cpx + 16, chX, cpy, co)) return true;
            }
        }
        if (t == StationType.CRUSHING) {
            int cy = editorY + 50, outX = cx - 120 + SS + 30, colW = 110;
            for (int i = 0; i < 8; i++) {
                CrushingOutput co = d.crushOuts.get(i);
                int ox = outX + (i/4)*colW, oy = cy + (i%4)*(SS+12), cpx = ox + SS + 4, cpy = oy + 2, chX = cpx + 30;
                if (miniCountChance(mx, mY, cpx + 16, chX, cpy, co)) return true;
            }
            int tX = cx + 55, tY = cy + 4*(SS+12) + 12;
            if (r.hit(mx, mY, tX, tY, SPIN_W, SPIN_H)) { d.crushTime = Math.min(10000, d.crushTime + 10); return true; }
            if (r.hit(mx, mY, tX, tY + 8, SPIN_W, SPIN_H)) { d.crushTime = Math.max(10, d.crushTime - 10); return true; }
        }
        if (t == StationType.FAN) {
            int cy = editorY + 50, outX = cx - 120 + SS + 30, colW = 110;
            for (int i = 0; i < 4; i++) {
                CrushingOutput co = d.fanOuts.get(i);
                int ox = outX + (i/2)*colW, oy = cy + (i%2)*(SS+12), cpx = ox + SS + 4, cpy = oy + 2, chX = cpx + 30;
                if (miniCountChance(mx, mY, cpx + 16, chX, cpy, co)) return true;
            }
            int tX = cx + 55, tY = cy + 2*(SS+12) + 12;
            if (r.hit(mx, mY, tX, tY, SPIN_W, SPIN_H)) { d.fanTime = Math.min(10000, d.fanTime + 10); return true; }
            if (r.hit(mx, mY, tX, tY + 8, SPIN_W, SPIN_H)) { d.fanTime = Math.max(10, d.fanTime - 10); return true; }
        }
        return false;
    }

    /** +/- pro standardní count spinner (10×16 oblast). */
    private boolean countSpinner(int mx, int mY, int x, int y, Supplier<Integer> get, Consumer<Integer> set) {
        if (r.hit(mx, mY, x, y, SPIN_W, SPIN_H))     { set.accept(Math.min(64, get.get() + 1)); return true; }
        if (r.hit(mx, mY, x, y + 8, SPIN_W, SPIN_H)) { set.accept(Math.max(1,  get.get() - 1)); return true; }
        return false;
    }

    /** +/- pro mini spinner count + chance v jedné CrushingOutput. */
    private boolean miniCountChance(int mx, int mY, int countX, int chanceX, int cpy, CrushingOutput co) {
        if (r.hit(mx, mY, countX, cpy - 2, MINI_SPIN, MINI_SPIN))   { co.count = Math.min(64, co.count + 1); return true; }
        if (r.hit(mx, mY, countX, cpy + 7, MINI_SPIN, MINI_SPIN))   { co.count = Math.max(1,  co.count - 1); return true; }
        if (r.hit(mx, mY, chanceX, cpy - 2, MINI_SPIN, MINI_SPIN))  { co.chance = Math.min(1f,    co.chance + 0.05f); return true; }
        if (r.hit(mx, mY, chanceX, cpy + 7, MINI_SPIN, MINI_SPIN))  { co.chance = Math.max(0.05f, co.chance - 0.05f); return true; }
        return false;
    }

    private boolean handleFluidSpins(int mx, int mY) {
        if (tabs.get(tabIdx) != StationType.MIXING) return false;
        int cx = pX + leftW / 2, cy = editorY + 70, sx = cx - 130, fluidY = cy + 95, rx = cx + 10;
        for (int i = 0; i < 2; i++) {
            FluidEntry f = d.mixFluidIng.get(i);
            int amtX = sx + i*70 + SS + 4, amtY = fluidY + 4;
            if (r.hit(mx, mY, amtX - 2, amtY + 12, SPIN_W, SPIN_H)) { f.amount = Math.clamp(f.amount + 250, 1, 1000); return true; }
            if (r.hit(mx, mY, amtX + 10, amtY + 12, SPIN_W, SPIN_H)) { f.amount = Math.clamp(f.amount - 250, 1, 1000); return true; }
        }
        for (int i = 0; i < 2; i++) {
            FluidEntry f = d.mixFluidOuts.get(i);
            int amtX = rx + i*65 + SS + 4, amtY = fluidY + 4;
            if (r.hit(mx, mY, amtX - 2, amtY + 12, SPIN_W, SPIN_H)) { f.amount = Math.clamp(f.amount + 250, 1, 1000); return true; }
            if (r.hit(mx, mY, amtX + 10, amtY + 12, SPIN_W, SPIN_H)) { f.amount = Math.clamp(f.amount - 250, 1, 1000); return true; }
        }
        return false;
    }

    private boolean handleDoubleClick(int mx, int mY) {
        StationType t = tabs.get(tabIdx); int cx = pX + leftW / 2;
        if (t == StationType.CRAFTING) {
            int cy = editorY + 50, ax = cx - 70 + 3*(SS+SP) + 15, rx = ax + 20, cpx = rx + SS + 6, cpy = cy + SS + SP - 7;
            if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("craftCount", cpx, cpy + 2, 15, String.valueOf(d.craftCount)); return true; }
        }
        if (t == StationType.MECH_CRAFTING) {
            int cy = editorY + 50, sz = 16, pad = 1, gW = 9*(sz+pad), sx = cx - gW/2 - 40, ay = cy + (9*(sz+pad))/2 - 4;
            int rx = sx + gW + 15 + 20, cpx = rx + SS + 6, cpy = ay - 2;
            if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("craftCount", cpx, cpy + 2, 15, String.valueOf(d.craftCount)); return true; }
        }
        if (t == StationType.FURNACE) {
            int cy = editorY + 60, sx = cx - IO_INPUT_OFFSET, rx = sx + SS + IO_GAP, cpx = rx + SS + 6, cpy = cy + 2;
            if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("furnCount", cpx, cpy + 2, 15, String.valueOf(d.furnCount)); return true; }
            if (r.hit(mx, mY, cx - 48, cy + 42, 26, 12)) { startActiveNumEdit("furnXp", cx - 48, cy + 42, 26, String.format(Locale.ROOT, "%.1f", d.furnXp)); return true; }
            if (r.hit(mx, mY, cx + 42, cy + 42, 35, 12)) { startActiveNumEdit("furnTime", cx + 42, cy + 42, 35, String.valueOf(d.furnTime)); return true; }
        }
        if (t == StationType.STONECUTTER) {
            int cy = editorY + 40, sx = cx - IO_INPUT_OFFSET, rx = sx + SS + IO_GAP, cpx = rx + SS + 6, cpy = cy + 2;
            if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("stoneCount", cpx, cpy + 2, 15, String.valueOf(d.stoneCount)); return true; }
        }
        if (t == StationType.SMITHING) {
            int cy = editorY + 40, step = SS + 36, totalW = 3*step + 20 + SS;
            int sx = cx - totalW/2, rx = sx + 3*step + 16, cpx = rx + SS + 6, cpy = cy + 2;
            if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("smCount", cpx, cpy + 2, 15, String.valueOf(d.smCount)); return true; }
        }
        if (t == StationType.MIXING) {
            int cy = editorY + 70, fluidY = cy + 95, sx = cx - 130, rx = cx + 10;
            for (int i = 0; i < 2; i++) {
                int amtX = sx + i*70 + SS + 4, amtY = fluidY + 4;
                if (r.hit(mx, mY, amtX - 2, amtY - 2, 45, 12)) { startActiveNumEdit("fluid_mix_in", amtX - 2, amtY - 2, 45, String.valueOf(d.mixFluidIng.get(i).amount), i); return true; }
            }
            for (int i = 0; i < 4; i++) {
                CrushingOutput co = d.mixOuts.get(i);
                if (co.isEmpty()) continue;
                int col = i % 2, row = i / 2;
                int ox = rx + col * 90, oy = cy + row * 30;
                int cpx = ox + SS + 4, cpy = oy + 2, chX = cpx + 28;
                if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("mix_out_count", cpx, cpy + 2, 15, String.valueOf(co.count), i); return true; }
                if (r.hit(mx, mY, chX + 13, cpy + 1, 26, 12)) { startActiveNumEdit("mix_out_chance", chX + 11, cpy + 1, 26, String.valueOf((int)(co.chance * 100)), i); return true; }
            }
            for (int i = 0; i < 2; i++) {
                int amtX = rx + i*65 + SS + 4, amtY = fluidY + 4;
                if (r.hit(mx, mY, amtX - 2, amtY - 2, 45, 12)) { startActiveNumEdit("fluid_mix_out", amtX - 2, amtY - 2, 45, String.valueOf(d.mixFluidOuts.get(i).amount), i); return true; }
            }
            // Mixing grid count double-click
            int gsx = cx - 134;
            for (int i = 0; i < 9; i++) {
                int col = i % 3, row = i / 3;
                int bx = gsx + col * (SS + 24), by = cy + row * (SS + 10);
                ItemStack s = d.mixIng.get(i);
                if (!s.isEmpty()) {
                    int cpx = bx + SS + 1, cpy = by + 2;
                    if (r.hit(mx, mY, cpx, cpy + 2, 18, 12)) { startActiveNumEdit("grid_count", cpx, cpy + 2, 19, String.valueOf(s.getCount()), i); return true; }
                }
            }
        }
        if (t == StationType.PRESSING) {
            int cy = editorY + 45, sx = cx - 70, rx = sx + SS + 50, cpx = rx + SS + 4, cpy = cy + 2, chX = cpx + 28;
            CrushingOutput co = d.pressOuts.get(0);
            if (co.isEmpty()) return false;
            if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("press_out_count", cpx, cpy + 2, 15, String.valueOf(co.count), 0); return true; }
            if (r.hit(mx, mY, chX + 13, cpy + 1, 26, 12)) { startActiveNumEdit("press_out_chance", chX + 11, cpy + 1, 26, String.valueOf((int)(co.chance * 100)), 0); return true; }
        }
        if (t == StationType.CRUSHING) {
            int cy = editorY + 50, sx = cx - 120, outX = sx + SS + 30, colW = 110;
            for (int i = 0; i < 8; i++) {
                CrushingOutput co = d.crushOuts.get(i);
                if (co.isEmpty()) continue;
                int ox = outX + (i/4)*colW, oy = cy + (i%4)*(SS+12);
                int cpx = ox + SS + 4, cpy = oy + 2, chX = ox + SS + 34;
                if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("crush_out_count", cpx, cpy + 2, 15, String.valueOf(co.count), i); return true; }
                if (r.hit(mx, mY, chX + 11, cpy + 2, 26, 12)) { startActiveNumEdit("crush_out_chance", chX + 11, cpy + 2, 26, String.valueOf((int)(co.chance * 100)), i); return true; }
            }
            int oy = cy + 4*(SS+12) + 10;
            if (r.hit(mx, mY, cx + 12, oy + 2, 35, 12)) { startActiveNumEdit("crushTime", cx + 12, oy + 2, 35, String.valueOf(d.crushTime)); return true; }
        }
        if (t == StationType.FAN) {
            int cy = editorY + 50, sx = cx - 120, outX = sx + SS + 30, colW = 110;
            for (int i = 0; i < 4; i++) {
                CrushingOutput co = d.fanOuts.get(i);
                if (co.isEmpty()) continue;
                int ox = outX + (i/2)*colW, oy = cy + (i%2)*(SS+12);
                int cpx = ox + SS + 4, cpy = oy + 2, chX = ox + SS + 34;
                if (r.hit(mx, mY, cpx, cpy + 2, 14, 12)) { startActiveNumEdit("fan_out_count", cpx, cpy + 2, 15, String.valueOf(co.count), i); return true; }
                if (r.hit(mx, mY, chX + 11, cpy + 2, 26, 12)) { startActiveNumEdit("fan_out_chance", chX + 11, cpy + 2, 26, String.valueOf((int)(co.chance * 100)), i); return true; }
            }
            int oy = cy + 2*(SS+12) + 10;
            if (r.hit(mx, mY, cx + 12, oy + 2, 35, 12)) { startActiveNumEdit("fanTime", cx + 12, oy + 2, 35, String.valueOf(d.fanTime)); return true; }
        }
        return false;
    }

    private void startActiveNumEdit(String field, int bx, int by, int bw, String value) {
        startActiveNumEdit(field, bx, by, bw, value, -1);
    }

    private void startActiveNumEdit(String field, int bx, int by, int bw, String value, int idx) {
        activeFieldName = field; activeFieldIdx = idx;
        activeNumEditBox = new EditBox(font, bx, by - (int)editorSb.scroll, bw, 12, Component.empty());
        activeNumEditBox.setValue(value); activeNumEditBox.setFocused(true);
        activeNumEditBox.setMaxLength(8);
    }

    private void applyActiveNumEdit() {
        if (activeNumEditBox == null || activeFieldName == null) return;
        String v = activeNumEditBox.getValue().trim();
        try {
            switch (activeFieldName) {
                case "furnXp"      -> d.furnXp     = Float.parseFloat(v);
                case "furnTime"    -> d.furnTime   = Integer.parseInt(v);
                case "mixTime"     -> d.mixTime    = Integer.parseInt(v);
                case "pressTime"   -> d.pressTime  = Integer.parseInt(v);
                case "crushTime"   -> d.crushTime  = Integer.parseInt(v);
                case "fanTime"     -> d.fanTime    = Integer.parseInt(v);
                case "craftCount"  -> d.craftCount = Math.clamp(Integer.parseInt(v), 1, 64);
                case "furnCount"   -> d.furnCount  = Math.clamp(Integer.parseInt(v), 1, 64);
                case "stoneCount"  -> d.stoneCount = Math.clamp(Integer.parseInt(v), 1, 64);
                case "smCount"     -> d.smCount    = Math.clamp(Integer.parseInt(v), 1, 64);
                case "fluid_mix_in"   -> { if (activeFieldIdx >= 0) d.mixFluidIng.get(activeFieldIdx).amount = Math.clamp(Integer.parseInt(v), 1, 1000); }
                case "fluid_mix_out"  -> { if (activeFieldIdx >= 0) d.mixFluidOuts.get(activeFieldIdx).amount = Math.clamp(Integer.parseInt(v), 1, 1000); }
                case "mix_out_count"  -> applyOutCount(d.mixOuts, Integer.parseInt(v));
                case "mix_out_chance" -> applyOutChance(d.mixOuts, Integer.parseInt(v));
                case "press_out_count"  -> applyOutCount(d.pressOuts, Integer.parseInt(v));
                case "press_out_chance" -> applyOutChance(d.pressOuts, Integer.parseInt(v));
                case "crush_out_count"  -> applyOutCount(d.crushOuts, Integer.parseInt(v));
                case "crush_out_chance" -> applyOutChance(d.crushOuts, Integer.parseInt(v));
                case "fan_out_count"    -> applyOutCount(d.fanOuts, Integer.parseInt(v));
                case "fan_out_chance"   -> applyOutChance(d.fanOuts, Integer.parseInt(v));
                case "grid_count" -> {
                    if (activeFieldIdx >= 0) {
                        StationType cur = tabs.get(tabIdx);
                        List<ItemStack> gl = cur == StationType.MIXING ? d.mixIng
                                : cur == StationType.MECH_CRAFTING ? d.mechGrid
                                  : d.craftGrid;
                        if (activeFieldIdx < gl.size()) {
                            ItemStack s = gl.get(activeFieldIdx);
                            if (!s.isEmpty()) s.setCount(Math.clamp(Integer.parseInt(v), 1, 64));
                        }
                    }
                }
            }
        } catch (NumberFormatException ignored) {}
        activeNumEditBox = null; activeFieldName = null; activeFieldIdx = -1;
    }

    private void applyOutCount(List<CrushingOutput> list, int v) {
        if (activeFieldIdx >= 0 && activeFieldIdx < list.size())
            list.get(activeFieldIdx).count = Math.clamp(v, 1, 64);
    }

    private void applyOutChance(List<CrushingOutput> list, int pct) {
        if (activeFieldIdx >= 0 && activeFieldIdx < list.size())
            list.get(activeFieldIdx).chance = Math.clamp(pct, 1, 100) / 100f;
    }

    private static String stripJson(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length()-5) : name;
    }
}