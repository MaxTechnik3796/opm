package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.*;
import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeEditorScreen extends Screen {

    private static final int C_BG      = 0xFF181818, C_HDR      = 0xFF181818;
    private static final int C_BORDER  = 0xFF000000, C_TAB      = 0xFF282828;
    private static final int C_TAB_SEL = 0xFF4A4A6A, C_TAB_CR   = 0xFF352010;
    private static final int C_TAB_CRS = 0xFF603810;
    private static final int C_SLOT    = 0xFF3A3A3A, C_SLOT_HOV = 0xFF5A5A5A;
    private static final int C_SLOT_DR = 0xFF3A5A3A, C_SLOT_RES = 0xFF224422;
    private static final int C_INV     = 0xFF141414, C_TEXT     = 0xFFEEEEEE;
    private static final int C_LABEL   = 0xFFAAAAAA, C_BTN      = 0xFF383838;
    private static final int C_BTN_H   = 0xFF585858, C_BTN_G    = 0xFF1E4A1E;
    private static final int C_BTN_GH  = 0xFF2A6A2A;

    private static final int SS = 18, SP = 2, TAB_H = 22, INV_COLS = 9;
    
    private int leftW, rightX, rightW, invY;
    private final Screen parent;
    private final List<StationType> tabs = new ArrayList<>();
    private int tabIdx = 0;

    // Scrolling
    private float scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingBottomScroll = false;
    private boolean isDraggingFavScroll = false;

    // Resizing
    private int invPanelHeight = 150;
    private boolean isDraggingSplitter = false;

    // Numerical double-click editing
    private EditBox activeNumEditBox = null;
    private String activeFieldName = null;
    private int activeFieldIdx = -1;
    private long lastClickTime = 0;
    private int lastClickX = 0, lastClickY = 0;

    // Crafting
    private boolean shapeless = false;
    private final List<ItemStack> craftGrid = initList(9);
    private final List<ItemStack> mechGrid  = initList(81);
    private ItemStack craftResult = ItemStack.EMPTY;
    private int craftCount = 1;

    // Furnace
    private int furnSubIdx = 0;
    private final String[] furnSubs   = {"smelting","blasting","smoking","campfire_cooking"};
    private final String[] furnLabels = {"Furnace","Blast Furnace","Smoker","Campfire"};
    private ItemStack furnIn = ItemStack.EMPTY, furnOut = ItemStack.EMPTY;
    private int furnCount = 1, furnTime = 200;
    private float furnXp = 0.1f;

    // Stonecutter
    private ItemStack stoneIn = ItemStack.EMPTY, stoneOut = ItemStack.EMPTY;
    private int stoneCount = 1;

    // Smithing
    private ItemStack smTemplate = ItemStack.EMPTY, smBase = ItemStack.EMPTY;
    private ItemStack smAddition = ItemStack.EMPTY, smResult = ItemStack.EMPTY;
    private int smCount = 1;

    // Mixing
    private final List<ItemStack> mixIng = initList(9);
    private final List<FluidEntry> mixFluidIng = initFluidList(4);
    private ItemStack mixResult = ItemStack.EMPTY;
    private FluidEntry mixFluidResult = new FluidEntry();
    private int mixCount = 1, mixTime = 60, mixHeat = 0;
    private final String[] heatLabels = {"None","Heated","Superheated"};

    // Pressing
    private boolean pressBasin = false;
    private ItemStack pressIn = ItemStack.EMPTY, pressOut = ItemStack.EMPTY;
    private FluidEntry pressFluidOut = new FluidEntry();
    private int pressCount = 1, pressTime = 150;

    // Crushing
    private boolean isMilling = false;
    private ItemStack crushIn = ItemStack.EMPTY;
    private final List<CrushingOutput> crushOuts = new ArrayList<>();
    private int crushTime = 150;

    // Fan (Washing / Haunting)
    private boolean fanHaunting = false;
    private ItemStack fanIn = ItemStack.EMPTY;
    private final List<CrushingOutput> fanOuts = new ArrayList<>();
    private int fanTime = 200;

    // Drag
    private ItemStack dragStack = ItemStack.EMPTY;
    private boolean isDragging = false;
    private int dragX, dragY;

    // File name
    private String fileName = "my_recipe";
    private boolean fnFocused = false;
    private int fnCursor = fileName.length();

    // Bottom Area
    private enum BottomTab { INVENTORY, FLUIDS, ITEMS, TAGS }
    private BottomTab bottomTab = BottomTab.INVENTORY;
    private boolean showRecipesList = false;
    private long lastBtnClickTime = 0;
    private float bottomScroll = 0, favScroll = 0;
    private EditBox searchBox;
    private String lastSearch = "";
    private final List<ItemStack> availableFluids = new ArrayList<>();
    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> cachedFilteredItems = new ArrayList<>();
    private final List<ItemStack> cachedTags = new ArrayList<>();
    private final List<ItemStack> favorites = new ArrayList<>();
    private final List<java.io.File> savedRecipeFiles = new ArrayList<>();
    private java.io.File selectedRecipeFile = null;
    private float recipeListScroll = 0f;
    private float recipeListHorizScroll = 0f;
    private boolean isDraggingRecipeScroll = false;
    private boolean isDraggingRecipeHorizScroll = false;
    private String popupError = null;
    private final int rowH = 14;

    // JSON
    private String curJson = "";
    private CodeViewerWidget codeViewer;

    // Status
    private String statusMsg = ""; private long statusUntil; private boolean statusOk;

    // Geometry
    private int pX, pY, pW, pH, editorY, editorH;
    private int btnSaveX, btnSaveY, btnClearX, btnCopyX;

    public static class FluidEntry {
        public ItemStack proxy = ItemStack.EMPTY;
        public int amount = 1000;
        public boolean isEmpty() { return proxy.isEmpty(); }
    }

    public RecipeEditorScreen(Screen parent) {
        super(Component.literal("Recipe Editor"));
        this.parent = parent;
        for (int i = 0; i < 8; i++) crushOuts.add(new CrushingOutput());
        for (int i = 0; i < 4; i++) fanOuts.add(new CrushingOutput());
        
        boolean createLoaded = net.neoforged.fml.ModList.get().isLoaded("create");
        for (StationType t : StationType.values()) {
            if (!t.isCreate() || createLoaded) tabs.add(t);
        }
    }

    @Override
    protected void init() {
        super.init();
        pX = 0; pY = 0; pW = width; pH = height;

        leftW = (int) (pW * 0.65);
        rightX = pX + leftW + 2;
        rightW = pW - leftW - 2;

        editorY = pY + TAB_H + 2;
        
        btnSaveX = pX + 10;
        btnClearX = btnSaveX + 96;
        btnCopyX = btnClearX + 44;

        loadConfig();
        updateLayout();

        codeViewer = new CodeViewerWidget(font, curJson);
        codeViewer.setBounds(rightX, pY, rightW, pH);
        
        searchBox = new EditBox(font, pX + 10, invY + 22, 176, 12, Component.empty());

        loadFluids();
        loadAllItems();
        loadTags();
        cachedFilteredItems.addAll(allItems);
        loadFavorites();
        scanSavedRecipes();
    }

    private void updateLayout() {
        int minHeight = 2 * (SS + SP) + 40;
        int maxHeight = pH - (pY + TAB_H + 40);
        invPanelHeight = Math.max(minHeight, Math.min(maxHeight, invPanelHeight));
        
        invY = pY + pH - invPanelHeight;
        int btnBarY = invY - 20;
        btnSaveY = btnBarY;
        editorH = btnBarY - editorY - 4;
        if (searchBox != null) {
            searchBox.setX(pX + 10);
            searchBox.setY(invY + 22);
        }
    }

    private void loadConfig() {
        if (minecraft == null) return;
        File f = new File(minecraft.gameDirectory, "config/opm_editor.txt");
        if (!f.exists()) return;
        try {
            List<String> lines = Files.readAllLines(f.toPath());
            if (!lines.isEmpty()) invPanelHeight = Integer.parseInt(lines.get(0));
        } catch (Exception ignored) {}
    }

    private void saveConfig() {
        if (minecraft == null) return;
        File f = new File(minecraft.gameDirectory, "config/opm_editor.txt");
        try {
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), String.valueOf(invPanelHeight));
        } catch (Exception ignored) {}
    }

    private void loadFluids() {
        availableFluids.clear();
        availableFluids.add(new ItemStack(Items.WATER_BUCKET));
        availableFluids.add(new ItemStack(Items.LAVA_BUCKET));
        availableFluids.add(new ItemStack(Items.MILK_BUCKET));
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            try {
                Item honey = BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:honey_bucket"));
                if (honey != null && honey != Items.AIR) availableFluids.add(new ItemStack(honey));
                Item choc = BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:chocolate_bucket"));
                if (choc != null && choc != Items.AIR) availableFluids.add(new ItemStack(choc));
            } catch (Exception ignored) {}
        }
    }

    private void loadAllItems() {
        allItems.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            allItems.add(new ItemStack(item));
        }
    }

    private void loadTags() {
        cachedTags.clear();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getTags()
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .forEach(tagKey -> {
                    ItemStack stack = new ItemStack(net.minecraft.world.item.Items.NAME_TAG);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("#" + tagKey.location().toString()));
                    cachedTags.add(stack);
                });
    }

    private void loadFavorites() {
        favorites.clear();
        if (minecraft == null) return;
        File f = new File(minecraft.gameDirectory, "config/opm_favorites.txt");
        if (!f.exists()) return;
        try {
            List<String> lines = Files.readAllLines(f.toPath());
            for (String s : lines) {
                ResourceLocation loc = ResourceLocation.tryParse(s);
                if (loc != null) {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    opt.ifPresent(item -> favorites.add(new ItemStack(item)));
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveFavorites() {
        if (minecraft == null) return;
        File f = new File(minecraft.gameDirectory, "config/opm_favorites.txt");
        try {
            f.getParentFile().mkdirs();
            List<String> lines = new ArrayList<>();
            for (ItemStack s : favorites) {
                if (!s.isEmpty()) lines.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            }
            Files.write(f.toPath(), lines);
        } catch (Exception ignored) {}
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (isDragging) {
            dragX = mx;
            dragY = my;
        }
        renderBackground(g, mx, my, pt);
        g.fill(pX, pY, pX + pW, pY + pH, C_BG);
        
        renderTabs(g, mx, my);
        
        g.fill(pX, editorY, pX + leftW, editorY + editorH, 0xFF222222);
        g.fill(pX + leftW, pY, rightX, pY + pH, 0xFF111111);

        g.enableScissor(pX, editorY, pX + leftW, editorY + editorH);
        
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, -scrollOffset, 0);
        int mY = (int)(my + scrollOffset);
        
        int contentH = 0;
        switch (tabs.get(tabIdx)) {
            case CRAFTING      -> contentH = renderCrafting(g, mx, mY);
            case FURNACE       -> contentH = renderFurnace(g, mx, mY);
            case STONECUTTER   -> contentH = renderStonecutter(g, mx, mY);
            case SMITHING      -> contentH = renderSmithing(g, mx, mY);
            case MECH_CRAFTING -> contentH = renderMechCrafting(g, mx, mY);
            case MIXING        -> contentH = renderMixing(g, mx, mY);
            case PRESSING      -> contentH = renderPressing(g, mx, mY);
            case FAN           -> contentH = renderFan(g, mx, mY);
            case CRUSHING      -> contentH = renderCrushing(g, mx, mY);
        }
        maxScroll = Math.max(0, contentH - editorH + 20);
        
        pose.popPose();
        g.disableScissor();

        if (maxScroll > 0) {
            int sbX = pX + leftW - 6;
            int sbY = editorY;
            int sbH = editorH;
            g.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF111111);
            int thumbH = Math.max(20, sbH * sbH / (sbH + maxScroll));
            int thumbY = sbY + (int)((sbH - thumbH) * (scrollOffset / maxScroll));
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFF666666);
        }

        updateJson();
        codeViewer.render(g, mx, my);
        
        renderBtnBar(g, mx, my);
        renderBottomArea(g, mx, my);
        
        if (isDragging && !dragStack.isEmpty()) {
            g.renderItem(dragStack, dragX - 8, dragY - 8);
            g.renderItemDecorations(font, dragStack, dragX - 8, dragY - 8);
        }
        
        if (activeNumEditBox != null) {
            activeNumEditBox.render(g, mx, my, pt);
        }
        
        renderTooltips(g, mx, my);
        super.render(g, mx, my, pt);
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        int tabW = leftW / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            StationType t = tabs.get(i);
            int tx = pX + i * tabW;
            int tw = (i == tabs.size() - 1) ? (pX + leftW - tx) : tabW;
            boolean sel = i == tabIdx, hov = hit(mx, my, tx, pY, tw, TAB_H);
            boolean cr = t.isCreate();
            int bg = sel ? (cr ? C_TAB_CRS : C_TAB_SEL) : hov ? (cr ? C_TAB_CR : 0xFF353535) : (cr ? C_TAB_CR : C_TAB);
            g.fill(tx, pY, tx + tw, pY + TAB_H, bg);
            if (sel) g.fill(tx, pY + TAB_H - 2, tx + tw, pY + TAB_H, 0xFF8888FF);
            if (i < tabs.size() - 1) g.fill(tx + tw - 1, pY + 2, tx + tw, pY + TAB_H - 2, 0xFF444444);
            
            int iconSz = 16;
            int cx = tx + (tw - iconSz) / 2;
            try {
                ResourceLocation loc = ResourceLocation.tryParse(t.stationItemId);
                if (loc != null) {
                    var opt = BuiltInRegistries.ITEM.getOptional(loc);
                    opt.ifPresent(item -> g.renderItem(new ItemStack(item), cx, pY + (TAB_H - iconSz) / 2));
                }
            } catch (Exception ignored) {}
        }
    }

    private int renderCrafting(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 20;
        
        drawToggle2(g, mx, my, cx - 70, cy, "Shaped", "Shapeless", !shapeless);
        
        cy += 30;
        renderGrid3(g, mx, my, craftGrid, cx - 70, cy);
        
        int ax = cx - 70 + 3 * (SS + SP) + 15;
        int ay = cy + SS + SP;
        g.drawString(font, "→", ax, ay - 4, C_LABEL, false);
        
        int rx = ax + 20;
        g.drawString(font, "Result", rx, cy - 12, C_LABEL, false);
        slot(g, mx, my, craftResult, rx, ay - 9, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, ay - 7, craftCount, "cc");
        
        return cy + 3 * (SS + SP) - editorY;
    }

    private int renderMechCrafting(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 20;
        
        int sz = 16, pad = 1;
        int gridW = 9 * (sz + pad);
        int sx = cx - gridW / 2 - 40;
        
        renderGridN(g, mx, my, mechGrid, 9, 9, sx, cy, sz, pad);
        
        int ax = sx + gridW + 15;
        int ay = cy + (9 * (sz + pad)) / 2 - 4;
        g.drawString(font, "→", ax, ay, C_LABEL, false);
        
        int rx = ax + 20;
        g.drawString(font, "Result", rx, ay - 14, C_LABEL, false);
        slot(g, mx, my, craftResult, rx, ay - 4, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, ay - 2, craftCount, "cc");
        
        return cy + 9 * (sz + pad) - editorY;
    }

    private int renderFurnace(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 20;
        
        int tw = 0;
        for (String l : furnLabels) tw += font.width(l) + 16;
        int bx = cx - tw / 2;
        
        for (int i = 0; i < furnLabels.length; i++) {
            int bw = font.width(furnLabels[i]) + 10;
            boolean sel = furnSubIdx == i, hov = hit(mx, my, bx, cy, bw, 16);
            g.fill(bx, cy, bx + bw, cy + 16, sel ? C_TAB_SEL : (hov ? C_BTN_H : C_BTN));
            g.drawCenteredString(font, furnLabels[i], bx + bw / 2, cy + 4, sel ? 0xFFCCCCFF : C_TEXT);
            bx += bw + 6;
        }
        
        cy += 40;
        int sx = cx - 60;
        g.drawString(font, "Input", sx, cy - 12, C_LABEL, false);
        slot(g, mx, my, furnIn, sx, cy, C_SLOT);
        
        g.drawString(font, "→", sx + SS + 15, cy + 5, C_LABEL, false);
        
        int rx = sx + SS + 40;
        g.drawString(font, "Result", rx, cy - 12, C_LABEL, false);
        slot(g, mx, my, furnOut, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, furnCount, "fc");
        
        cy += 40;
        g.drawString(font, "XP:", cx - 70, cy + 4, C_LABEL, false);
        g.drawString(font, String.format(Locale.ROOT, "%.1f", furnXp), cx - 45, cy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx - 20, cy + 2);

        g.drawString(font, "Time:", cx + 10, cy + 4, C_LABEL, false);
        g.drawString(font, furnTime + " t", cx + 45, cy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 80, cy + 2);
        
        return cy + 20 - editorY;
    }

    private int renderStonecutter(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 40;
        
        int sx = cx - 50;
        g.drawString(font, "Input", sx, cy - 12, C_LABEL, false);
        slot(g, mx, my, stoneIn, sx, cy, C_SLOT);
        
        g.drawString(font, "→", sx + SS + 15, cy + 5, C_LABEL, false);
        
        int rx = sx + SS + 40;
        g.drawString(font, "Result", rx, cy - 12, C_LABEL, false);
        slot(g, mx, my, stoneOut, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, stoneCount, "sc");
        
        return cy + 40 - editorY;
    }

    private int renderSmithing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 40;
        
        int sx = cx - 120;
        String[] lbl = {"Template", "Base", "Addition"};
        ItemStack[] sl = {smTemplate, smBase, smAddition};
        
        for (int i = 0; i < 3; i++) {
            g.drawCenteredString(font, lbl[i], sx + 9, cy - 12, C_LABEL);
            slot(g, mx, my, sl[i], sx, cy, C_SLOT);
            if (i < 2) g.drawString(font, "+", sx + SS + 10, cy + 5, C_LABEL, false);
            sx += SS + 26;
        }
        
        g.drawString(font, "→", sx - 6, cy + 5, C_LABEL, false);
        
        int rx = sx + 14;
        g.drawCenteredString(font, "Result", rx + 9, cy - 12, C_LABEL);
        slot(g, mx, my, smResult, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, smCount, "smc");
        
        return cy + 40 - editorY;
    }

    private int renderMixing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 15;
        
        int tw = 0;
        for (String l : heatLabels) tw += font.width(l) + 16;
        int bx = cx - tw / 2;
        int[] heatCols = {C_BTN, 0xFF4A2000, 0xFF6A0000};
        
        for (int i = 0; i < heatLabels.length; i++) {
            int bw = font.width(heatLabels[i]) + 10;
            boolean sel = mixHeat == i, hov = hit(mx, my, bx, cy, bw, 16);
            g.fill(bx, cy, bx + bw, cy + 16, sel ? 0xFF4A4A7A : (hov ? heatCols[i] + 0x111100 : heatCols[i]));
            g.drawCenteredString(font, heatLabels[i], bx + bw / 2, cy + 4, sel ? 0xFFFFCC88 : C_TEXT);
            bx += bw + 6;
        }
        
        cy += 30;
        int sx = cx - 130;
        g.drawString(font, "Items:", sx, cy - 12, C_LABEL, false);
        renderGrid3(g, mx, my, mixIng, sx, cy);
        
        int fluidY = cy + 3 * (SS + SP) + 20;
        g.drawString(font, "Fluids:", sx, fluidY - 12, C_LABEL, false);
        for (int i = 0; i < 4; i++) {
            int r = i / 2, c = i % 2;
            slotFluid(g, mx, my, mixFluidIng.get(i), sx + c * 70, fluidY + r * 35);
        }
        
        int ax = cx - 40;
        int ay = cy + SS + SP;
        g.drawString(font, "→", ax, ay + 10, C_LABEL, false);
        
        int rx = cx + 10;
        g.drawString(font, "Result Item", rx, cy - 12, C_LABEL, false);
        slot(g, mx, my, mixResult, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, mixCount, "mc");
        
        int rfx = rx + 60;
        g.drawString(font, "Result Fluid", rfx, cy - 12, C_LABEL, false);
        slotFluid(g, mx, my, mixFluidResult, rfx, cy);
        
        int oy = fluidY + 2 * 35 + 10;
        g.drawString(font, "Time:", cx - 20, oy + 4, C_LABEL, false);
        g.drawString(font, mixTime + " t", cx + 15, oy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 55, oy + 2);
        
        return oy + 30 - editorY;
    }

    private int renderPressing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 20;
        
        drawToggle2(g, mx, my, cx - 60, cy, "Press", "Press + Basin", pressBasin);
        
        cy += 40;
        int sx = cx - 70;
        g.drawCenteredString(font, "Input", sx + 9, cy - 12, C_LABEL);
        slot(g, mx, my, pressIn, sx, cy, C_SLOT);
        
        g.drawString(font, "→", sx + SS + 25, cy + 5, C_LABEL, false);
        
        int rx = sx + SS + 50;
        g.drawCenteredString(font, "Result Item", rx + 9, cy - 12, C_LABEL);
        slot(g, mx, my, pressOut, rx, cy, C_SLOT_RES);
        spinner(g, mx, my, rx + SS + 6, cy + 2, pressCount, "pc");
        
        if (pressBasin) {
            int rfx = rx + 60;
            g.drawCenteredString(font, "Fluid Out", rfx + 9, cy - 12, C_LABEL);
            slotFluid(g, mx, my, pressFluidOut, rfx, cy);
        }
        
        int oy = cy + SS + 30;
        g.drawString(font, "Time:", cx - 20, oy + 4, C_LABEL, false);
        g.drawString(font, pressTime + " t", cx + 15, oy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 55, oy + 2);
        
        return oy + 30 - editorY;
    }

    private int renderCrushing(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 15;
        
        drawToggle2(g, mx, my, cx - 55, cy, "Crushing", "Milling", isMilling);
        
        cy += 35;
        int sx = cx - 120;
        g.drawCenteredString(font, "Input", sx + 9, cy - 12, C_LABEL);
        slot(g, mx, my, crushIn, sx, cy, C_SLOT);
        
        g.drawString(font, "→", sx + SS + 10, cy + 5, C_LABEL, false);
        
        int outX = sx + SS + 30;
        int colW = 110;
        g.drawString(font, "Outputs (chance via +/-):", outX, cy - 12, C_LABEL, false);
        
        for (int i = 0; i < 8; i++) {
            CrushingOutput co = crushOuts.get(i);
            int col = i / 4;
            int row = i % 4;
            int ox = outX + col * colW;
            int oy = cy + row * (SS + 12);
            
            slot(g, mx, my, co.stack, ox, oy, co.isEmpty() ? C_SLOT : C_SLOT_RES);
            
            int cpx = ox + SS + 4, cpy = oy + 2;
            g.drawString(font, "×" + co.count, cpx, cpy + 2, C_TEXT, false);
            boolean hP = hit(mx, my, cpx + 16, cpy - 2, 9, 9);
            boolean hM = hit(mx, my, cpx + 16, cpy + 7, 9, 9);
            g.fill(cpx + 16, cpy - 2, cpx + 25, cpy + 7, hP ? C_BTN_H : C_BTN);
            g.fill(cpx + 16, cpy + 7, cpx + 25, cpy + 16, hM ? C_BTN_H : C_BTN);
            g.drawCenteredString(font, "+", cpx + 20, cpy - 2, C_TEXT);
            g.drawCenteredString(font, "-", cpx + 20, cpy + 7, C_TEXT);
            
            int chX = cpx + 30;
            String chStr = co.chance >= 1f ? "100%" : Math.round(co.chance * 100) + "%";
            boolean hCP = hit(mx, my, chX, cpy - 2, 9, 9);
            boolean hCM = hit(mx, my, chX, cpy + 7, 9, 9);
            g.fill(chX, cpy - 2, chX + 9, cpy + 7, hCP ? C_BTN_H : C_BTN);
            g.fill(chX, cpy + 7, chX + 9, cpy + 16, hCM ? C_BTN_H : C_BTN);
            g.drawCenteredString(font, "+", chX + 4, cpy - 2, C_LABEL);
            g.drawCenteredString(font, "-", chX + 4, cpy + 7, C_LABEL);
            g.drawString(font, chStr, chX + 12, cpy + 3, co.isEmpty() ? C_LABEL : 0xFFAAFF88, false);
        }
        
        int oy = cy + 4 * (SS + 12) + 10;
        g.drawString(font, "Time:", cx - 20, oy + 4, C_LABEL, false);
        g.drawString(font, crushTime + " t", cx + 15, oy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 55, oy + 2);
        
        return oy + 30 - editorY;
    }

    private void renderGrid3(GuiGraphics g, int mx, int my, List<ItemStack> list, int sx, int sy) {
        renderGridN(g, mx, my, list, 3, 3, sx, sy, SS, SP);
    }

    private void renderGridN(GuiGraphics g, int mx, int my, List<ItemStack> list, int cols, int rows, int sx, int sy, int sz, int pad) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int idx = r * cols + c, bx = sx + c * (sz + pad), by = sy + r * (sz + pad);
            boolean hov = hit(mx, my, bx, by, sz, sz), drop = isDragging && hov;
            g.fill(bx - 1, by - 1, bx + sz + 1, by + sz + 1, C_BORDER);
            g.fill(bx, by, bx + sz, by + sz, drop ? C_SLOT_DR : (hov ? C_SLOT_HOV : C_SLOT));
            ItemStack s = idx < list.size() ? list.get(idx) : ItemStack.EMPTY;
            if (!s.isEmpty()) itemScaled(g, s, bx, by, sz);
        }
    }

    private void slot(GuiGraphics g, int mx, int my, ItemStack s, int sx, int sy, int bg) {
        boolean hov = hit(mx, my, sx, sy, SS, SS), drop = isDragging && hov;
        g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, C_BORDER);
        g.fill(sx, sy, sx + SS, sy + SS, drop ? C_SLOT_DR : (hov ? C_SLOT_HOV : bg));
        if (!s.isEmpty()) { g.renderItem(s, sx + 1, sy + 1); g.renderItemDecorations(font, s, sx + 1, sy + 1); }
    }

    private void slotFluid(GuiGraphics g, int mx, int my, FluidEntry f, int sx, int sy) {
        boolean hov = hit(mx, my, sx, sy, SS, SS), drop = isDragging && hov;
        int fluidBg = 0xFF1A2A4A;
        g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, 0xFF2255AA);
        g.fill(sx, sy, sx + SS, sy + SS, drop ? 0xFF2A5A6A : (hov ? 0xFF2A3A6A : fluidBg));
        if (!f.isEmpty()) {
            g.renderItem(f.proxy, sx + 1, sy + 1);
        } else {
            g.drawCenteredString(font, "~", sx + SS / 2, sy + (SS - 8) / 2, 0xFF4488CC);
        }
        
        int amtX = sx + SS + 4, amtY = sy + 4;
        g.drawString(font, f.amount + " mB", amtX, amtY, 0xFF66AAFF, false);
        
        boolean hP = hit(mx, my, amtX - 2, amtY + 12, 10, 8);
        boolean hM = hit(mx, my, amtX + 10, amtY + 12, 10, 8);
        g.fill(amtX - 2, amtY + 12, amtX + 8, amtY + 20, hP ? C_BTN_H : C_BTN);
        g.fill(amtX + 10, amtY + 12, amtX + 20, amtY + 20, hM ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, "+", amtX + 3, amtY + 12, C_TEXT);
        g.drawCenteredString(font, "-", amtX + 15, amtY + 12, C_TEXT);
    }

    private void spinner(GuiGraphics g, int mx, int my, int cx, int cy, int count, String id) {
        g.drawString(font, "×" + count, cx, cy + 2, C_TEXT, false);
        boolean hP = hit(mx, my, cx + 18, cy, 10, 8);
        boolean hM = hit(mx, my, cx + 18, cy + 8, 10, 8);
        g.fill(cx + 18, cy, cx + 28, cy + 8, hP ? C_BTN_H : C_BTN);
        g.fill(cx + 18, cy + 8, cx + 28, cy + 16, hM ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, "+", cx + 23, cy, C_TEXT);
        g.drawCenteredString(font, "-", cx + 23, cy + 8, C_TEXT);
    }
    
    private void valSpinner(GuiGraphics g, int mx, int my, int cx, int cy) {
        boolean hP = hit(mx, my, cx, cy, 10, 8);
        boolean hM = hit(mx, my, cx, cy + 8, 10, 8);
        g.fill(cx, cy, cx + 10, cy + 8, hP ? C_BTN_H : C_BTN);
        g.fill(cx, cy + 8, cx + 10, cy + 16, hM ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, "+", cx + 5, cy, C_TEXT);
        g.drawCenteredString(font, "-", cx + 5, cy + 8, C_TEXT);
    }

    private void drawToggle2(GuiGraphics g, int mx, int my, int x, int y, String a, String b, boolean aOn) {
        int wa = font.width(a) + 12, wb = font.width(b) + 12;
        boolean ha = hit(mx, my, x, y, wa, 16), hb = hit(mx, my, x + wa + 2, y, wb, 16);
        g.fill(x, y, x + wa, y + 16, aOn ? C_TAB_SEL : (ha ? C_BTN_H : C_BTN));
        g.fill(x + wa + 2, y, x + wa + 2 + wb, y + 16, !aOn ? C_TAB_SEL : (hb ? C_BTN_H : C_BTN));
        g.drawCenteredString(font, a, x + wa / 2, y + 4, aOn ? 0xFFCCCCFF : C_TEXT);
        g.drawCenteredString(font, b, x + wa + 2 + wb / 2, y + 4, !aOn ? 0xFFCCCCFF : C_TEXT);
    }

    private void itemScaled(GuiGraphics g, ItemStack s, int sx, int sy, int sz) {
        if (sz >= 16) { g.renderItem(s, sx + 1, sy + 1); if (sz >= 18) g.renderItemDecorations(font, s, sx + 1, sy + 1); }
        else {
            float sc = sz / 16f;
            var p = g.pose(); p.pushPose();
            p.translate(sx + 1, sy + 1, 0); p.scale(sc, sc, 1f);
            g.renderItem(s, 0, 0); p.popPose();
        }
    }

    private void renderBtnBar(GuiGraphics g, int mx, int my) {
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
        
        if (!statusMsg.isEmpty() && System.currentTimeMillis() < statusUntil) {
            g.drawCenteredString(font, statusMsg, leftW / 2, btnSaveY - 14, statusOk ? 0xFF88FF88 : 0xFFFF6666);
        }
    }

    private void drawBtn(GuiGraphics g, String lbl, int bx, int by, int bw, boolean hov, int bg, int hbg) {
        g.fill(bx, by, bx + bw, by + 16, hov ? hbg : bg);
        g.fill(bx, by, bx + bw, by + 1, 0x44FFFFFF);
        g.drawCenteredString(font, lbl, bx + bw / 2, by + 4, C_TEXT);
    }

    private void renderBottomArea(GuiGraphics g, int mx, int my) {
        var pose = g.pose();
        // Draw restricted to leftW
        g.fill(pX, invY, pX + leftW, pY + pH, C_INV);
        g.fill(pX, invY, pX + leftW, invY + 2, C_BORDER);
        
        // Drag handle for resizing
        int hw = 40;
        g.fill(pX + leftW / 2 - hw / 2, invY, pX + leftW / 2 + hw / 2, invY + 3, 0xFF666666);
        
        String[] bTabs = {"Inventory", "Fluids", "Items", "Tags"};
        int tx = pX + 10;
        if (!showRecipesList) {
            for (int i = 0; i < bTabs.length; i++) {
                int tw = font.width(bTabs[i]) + 10;
                boolean sel = bottomTab.ordinal() == i;
                boolean hov = hit(mx, my, tx, invY + 4, tw, 14);
                g.fill(tx, invY + 4, tx + tw, invY + 18, sel ? C_TAB_SEL : (hov ? C_BTN_H : C_BTN));
                g.drawCenteredString(font, bTabs[i], tx + tw / 2, invY + 7, sel ? 0xFFCCCCFF : C_TEXT);
                tx += tw + 4;
            }

            boolean hasSearch = bottomTab != BottomTab.INVENTORY;
            if (hasSearch) {
                searchBox.render(g, mx, my, 0);
            }
        }
        
        int startX = pX + 10;
        int listY = (bottomTab != BottomTab.INVENTORY && !showRecipesList) ? invY + 38 : invY + 22;
        int listH = (pY + pH) - listY - 5;
        
        if (!showRecipesList) {
            g.enableScissor(startX, listY, startX + 9 * (SS + SP), listY + listH);
            pose.pushPose(); pose.translate(0, -bottomScroll, 0);
            int mY = (int)(my + bottomScroll);
            
            int contentH = 0;
            if (bottomTab == BottomTab.INVENTORY && minecraft != null && minecraft.player != null) {
                Inventory inv = minecraft.player.getInventory();
                for (int row = 0; row < 3; row++) for (int col = 0; col < INV_COLS; col++)
                    invSlotRender(g, mx, mY, inv.getItem(9 + row * INV_COLS + col), startX + col * (SS + SP), listY + row * (SS + SP));
                for (int col = 0; col < INV_COLS; col++)
                    invSlotRender(g, mx, mY, inv.getItem(col), startX + col * (SS + SP), listY + 3 * (SS + SP) + 8);
                contentH = 4 * (SS + SP) + 8;
            } else if (bottomTab == BottomTab.FLUIDS) {
                String q = searchBox.getValue().toLowerCase(Locale.ROOT);
                List<ItemStack> filtered = new ArrayList<>();
                if (q.isEmpty()) filtered.addAll(availableFluids);
                else filtered.addAll(availableFluids.stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList());
                
                for (int i = 0; i < filtered.size(); i++) {
                    int c = i % 9, r = i / 9;
                    invSlotRender(g, mx, mY, filtered.get(i), startX + c * (SS + SP), listY + r * (SS + SP));
                }
                contentH = ((filtered.size() + 8) / 9) * (SS + SP);
            } else if (bottomTab == BottomTab.ITEMS) {
                String q = searchBox.getValue().toLowerCase(Locale.ROOT);
                if (!q.equals(lastSearch)) {
                    lastSearch = q;
                    cachedFilteredItems.clear();
                    if (q.isEmpty()) {
                        cachedFilteredItems.addAll(allItems);
                    } else if (q.startsWith("@")) {
                        String modQuery = q.substring(1);
                        cachedFilteredItems.addAll(allItems.stream().filter(s -> {
                            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(s.getItem());
                            return loc.getNamespace().toLowerCase(Locale.ROOT).contains(modQuery);
                        }).toList());
                    } else {
                        cachedFilteredItems.addAll(allItems.stream().filter(s -> 
                            s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                        ).toList());
                    }
                }
                for (int i = 0; i < cachedFilteredItems.size(); i++) {
                    int c = i % 9, r = i / 9;
                    invSlotRender(g, mx, mY, cachedFilteredItems.get(i), startX + c * (SS + SP), listY + r * (SS + SP));
                }
                contentH = ((cachedFilteredItems.size() + 8) / 9) * (SS + SP);
            } else if (bottomTab == BottomTab.TAGS) {
                String q = searchBox.getValue().toLowerCase(Locale.ROOT);
                List<ItemStack> filtered = new ArrayList<>();
                if (q.isEmpty()) filtered.addAll(cachedTags);
                else filtered.addAll(cachedTags.stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList());

                for (int i = 0; i < filtered.size(); i++) {
                    int c = i % 9, r = i / 9;
                    invSlotRender(g, mx, mY, filtered.get(i), startX + c * (SS + SP), listY + r * (SS + SP));
                }
                contentH = ((filtered.size() + 8) / 9) * (SS + SP);
            }
            pose.popPose(); g.disableScissor();
            
            int bMax = Math.max(0, contentH - listH);
            if (bMax > 0) {
                int sbX = startX + 9 * (SS + SP) + 2, sbY = listY, sbH = listH;
                g.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF111111);
                int thumbH = Math.max(20, sbH * sbH / (sbH + bMax));
                int thumbY = sbY + (int)((sbH - thumbH) * (bottomScroll / bMax));
                g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFF666666);
            }
        }
        
        int favX = startX + 9 * (SS + SP) + 16;
        String favBtnText = showRecipesList ? "Recipes" : "Favorite";
        int favBtnW = font.width(favBtnText) + 10;
        boolean favHov = hit(mx, my, favX, invY + 4, favBtnW, 14);
        g.fill(favX, invY + 4, favX + favBtnW, invY + 18, favHov ? C_BTN_H : C_BTN);
        g.drawCenteredString(font, favBtnText, favX + favBtnW / 2, invY + 7, 0xFFFFFFFF);


        int favCols = 5; // Fixed width of 5 slots
        int favListY = invY + 22;
        int favListH = (pY + pH) - favListY - 5;
        
        g.enableScissor(favX, favListY, favX + favCols * (SS + SP), favListY + favListH);
        pose.pushPose(); pose.translate(0, -favScroll, 0);
        int fY = (int)(my + favScroll);
        
        int minFavSlots = 25;
        int favCount = Math.max(minFavSlots, ((favorites.size() + favCols - 1) / favCols + 1) * favCols);
        for (int i = 0; i < favCount; i++) {
            int c = i % favCols, r = i / favCols;
            ItemStack s = i < favorites.size() ? favorites.get(i) : ItemStack.EMPTY;
            invSlotRender(g, mx, fY, s, favX + c * (SS + SP), favListY + r * (SS + SP));
        }
        int favContentH = ((favCount + favCols - 1) / favCols) * (SS + SP);
        pose.popPose(); g.disableScissor();
        
        int fMax = Math.max(0, favContentH - favListH);
        if (fMax > 0) {
            int sbX = favX + favCols * (SS + SP) + 2, sbY = favListY, sbH = favListH;
            g.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF111111);
            int thumbH = Math.max(20, sbH * sbH / (sbH + fMax));
            g.fill(sbX, sbY + (int)((sbH - thumbH) * (favScroll / fMax)), sbX + 4, sbY + (int)((sbH - thumbH) * (favScroll / fMax)) + thumbH, 0xFF666666);
        }

        // ── Saved Recipes Panel ───────────────────────────────────────────────
        if (showRecipesList) {
            int recX = startX;
            int recW = 9 * (SS + SP);
            int recListY = invY + 22;
            int recListH = pH - (recListY - pY) - 5;
            
            // Draw Unload and Remove buttons above the list
            boolean hasSelected = selectedRecipeFile != null;
            int btnW = (recW - 4) / 2;

            // Unload Button
            boolean hovUnload = hasSelected && hit(mx, my, recX, invY + 4, btnW, 14);
            int unloadBg = hasSelected ? C_BTN : 0xFF222222;
            int unloadHov = hasSelected ? C_BTN_H : 0xFF222222;
            int unloadText = hasSelected ? 0xFFFFFFFF : 0xFF666666;
            g.fill(recX, invY + 4, recX + btnW, invY + 18, hovUnload ? unloadHov : unloadBg);
            g.drawCenteredString(font, "Unload", recX + btnW / 2, invY + 7, unloadText);

            // Remove Button
            int remX = recX + btnW + 4;
            boolean hovRemove = hasSelected && hit(mx, my, remX, invY + 4, btnW, 14);
            int removeBg = hasSelected ? 0xFF5A1E1E : 0xFF222222;
            int removeHov = hasSelected ? 0xFF8A2E2E : 0xFF222222;
            int removeText = hasSelected ? 0xFFFFFFFF : 0xFF666666;
            g.fill(remX, invY + 4, remX + btnW, invY + 18, hovRemove ? removeHov : removeBg);
            g.drawCenteredString(font, "Remove", remX + btnW / 2, invY + 7, removeText);
            
            if (recW > 10 && recListH > 10) {
                g.fill(recX - 1, recListY - 1, recX + recW + 1, recListY + recListH + 1, C_BORDER);
                g.fill(recX, recListY, recX + recW, recListY + recListH, 0xFF151515);
                
                int maxNameW = 0;
                for (java.io.File f : savedRecipeFiles) {
                    String name = f.getName();
                    if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                    maxNameW = Math.max(maxNameW, font.width(name));
                }
                int rhMax = Math.max(0, (maxNameW + 10) - recW);
                int totalH = savedRecipeFiles.size() * rowH;
                int rMax = Math.max(0, totalH - recListH);
                
                g.enableScissor(recX, recListY, recX + recW, recListY + recListH);
                pose.pushPose();
                pose.translate(-recipeListHorizScroll, -recipeListScroll, 0);
                
                for (int i = 0; i < savedRecipeFiles.size(); i++) {
                    java.io.File f = savedRecipeFiles.get(i);
                    String name = f.getName();
                    if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                    
                    int ry = recListY + i * rowH;
                    boolean isSelected = selectedRecipeFile != null && selectedRecipeFile.getAbsolutePath().equals(f.getAbsolutePath());
                    boolean isHovered = hit((int)(mx + recipeListHorizScroll), (int)(my + recipeListScroll), recX, ry, recW, rowH);
                    
                    if (isSelected) {
                        g.fill(recX, ry, recX + Math.max(recW, maxNameW + 10), ry + rowH, 0xFF2255AA);
                        g.drawString(font, name, recX + 4, ry + 3, 0xFFFFFFFF, false);
                    } else {
                        if (isHovered) {
                            g.fill(recX, ry, recX + Math.max(recW, maxNameW + 10), ry + rowH, 0xFF333333);
                        }
                        g.drawString(font, name, recX + 4, ry + 3, isHovered ? 0xFFFFFFFF : 0xFFAAAAAA, false);
                    }
                }
                
                pose.popPose();
                g.disableScissor();
                
                if (rhMax > 0) {
                    int sbX = recX, sbY = recListY + recListH - 5, sbW = recW;
                    g.fill(sbX, sbY, sbX + sbW, sbY + 4, 0xFF111111);
                    int thumbW = Math.max(20, sbW * sbW / (sbW + rhMax));
                    int thumbX = sbX + (int)((sbW - thumbW) * (recipeListHorizScroll / rhMax));
                    g.fill(thumbX, sbY, thumbX + thumbW, sbY + 4, 0xFF666666);
                }
                if (rMax > 0) {
                    int sbX = recX + recW - 5, sbY = recListY, sbH = recListH;
                    g.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF111111);
                    int thumbH = Math.max(20, sbH * sbH / (sbH + rMax));
                    int thumbY = sbY + (int)((sbH - thumbH) * (recipeListScroll / rMax));
                    g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFF666666);
                }
            }
        }

        // ── Error Popup Modal ────────────────────────────────────────────────
        if (popupError != null) {
            g.fill(0, 0, width, height, 0xAA000000);
            
            int pw = 260, ph = 100;
            int px = (width - pw) / 2;
            int py = (height - ph) / 2;
            g.fill(px, py, px + pw, py + ph, 0xFF222222);
            g.fill(px, py, px + pw, py + 2, 0xFFFF3333);
            g.fill(px, py + ph - 2, px + pw, py + ph, 0xFFFF3333);
            g.fill(px, py, px + 2, py + ph, 0xFFFF3333);
            g.fill(px + pw - 2, py, px + pw, py + ph, 0xFFFF3333);
            
            String title = "Error";
            g.drawString(font, title, px + (pw - font.width(title)) / 2, py + 12, 0xFFFF3333, false);
            g.drawString(font, popupError, px + (pw - font.width(popupError)) / 2, py + 36, 0xFFFFFFFF, false);
            
            int bx = px + (pw - 60) / 2, by = py + 65, bw = 60, bh = 18;
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            g.fill(bx, by, bx + bw, by + bh, hov ? 0xFF666666 : 0xFF444444);
            g.fill(bx, by, bx + bw, by + 1, 0xFF888888);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF888888);
            g.fill(bx, by, bx + 1, by + bh, 0xFF888888);
            g.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF888888);
            
            String okText = "OK";
            g.drawString(font, okText, bx + (bw - font.width(okText)) / 2, by + 5, 0xFFFFFFFF, false);
        }
    }

    private void invSlotRender(GuiGraphics g, int mx, int my, ItemStack s, int sx, int sy) {
        boolean hov = hit(mx, my, sx, sy, SS, SS);
        g.fill(sx - 1, sy - 1, sx + SS + 1, sy + SS + 1, C_BORDER);
        g.fill(sx, sy, sx + SS, sy + SS, hov ? C_SLOT_HOV : C_SLOT);
        if (s != null && !s.isEmpty()) { g.renderItem(s, sx + 1, sy + 1); g.renderItemDecorations(font, s, sx + 1, sy + 1); }
    }

    private void renderTooltips(GuiGraphics g, int mx, int my) {
        if (isDragging) return;
        ItemStack hs = invAt(mx, my);
        if (hs != null && !hs.isEmpty()) { showTip(g, hs, mx, my); return; }
    }

    private void showTip(GuiGraphics g, ItemStack s, int mx, int my) {
        if (minecraft != null && minecraft.player != null) {
            g.renderComponentTooltip(font, s.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.Default.NORMAL), mx, my);
        }
    }

    private void updateJson() {
        String j = buildJson();
        if (!j.equals(curJson)) {
            curJson = j;
            codeViewer = new CodeViewerWidget(font, curJson);
            codeViewer.setBounds(rightX, pY, rightW, pH);
        }
    }

    private String buildJson() {
        try {
            return switch (tabs.get(tabIdx)) {
                case CRAFTING -> shapeless
                        ? RecipeJsonBuilder.buildShapeless(craftGrid, craftResult, craftCount)
                        : RecipeJsonBuilder.buildShaped(craftGrid, 3, 3, craftResult, craftCount);
                case FURNACE ->
                        RecipeJsonBuilder.buildFurnace(furnSubs[furnSubIdx], furnIn, furnOut, furnCount, furnTime, furnXp);
                case STONECUTTER ->
                        RecipeJsonBuilder.buildStonecutter(stoneIn, stoneOut, stoneCount);
                case SMITHING ->
                        RecipeJsonBuilder.buildSmithing(smTemplate, smBase, smAddition, smResult, smCount);
                case MECH_CRAFTING ->
                        RecipeJsonBuilder.buildShaped(mechGrid, 9, 9, craftResult, craftCount);
                case MIXING -> {
                    boolean hasFluid = mixFluidIng.stream().anyMatch(f -> !f.isEmpty()) || !mixFluidResult.isEmpty();
                    yield hasFluid
                            ? RecipeJsonBuilder.buildMixingWithFluids(mixIng, mixFluidIng, mixResult, mixCount, mixFluidResult, heatLabels[mixHeat].toLowerCase(), mixTime)
                            : RecipeJsonBuilder.buildMixing(mixIng, mixResult, mixCount, heatLabels[mixHeat].toLowerCase(), mixTime);
                }
                case PRESSING -> pressBasin
                        ? RecipeJsonBuilder.buildPressingBasin(pressIn, pressOut, pressCount, pressFluidOut, pressTime)
                        : RecipeJsonBuilder.buildPressing(pressIn, pressOut, pressCount, pressTime);
                case FAN ->
                        RecipeJsonBuilder.buildCrushing(
                                fanHaunting ? "create:haunting" : "create:splashing",
                                fanIn, fanOuts, fanTime);
                case CRUSHING ->
                        RecipeJsonBuilder.buildCrushing(
                                isMilling ? "create:milling" : "create:crushing",
                                crushIn, crushOuts, crushTime);
            };
        } catch (Exception e) { return "// Error: " + e.getMessage(); }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        int mY = (int)(my + scrollOffset);
        
        if (popupError != null) {
            int pw = 260, ph = 100;
            int px = (width - pw) / 2;
            int py = (height - ph) / 2;
            int bx = px + (pw - 60) / 2, by = py + 65, bw = 60, bh = 18;
            if (button == 0 && mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                popupError = null;
            }
            return true;
        }
        
        if (isDragging) {
            if (button == 1) {
                dragStack = ItemStack.EMPTY;
                isDragging = false;
                return true;
            }
            if (button == 0) {
                if (hit(mx, my, pX, editorY, leftW, editorH)) {
                    drop(mx, (int)(my + scrollOffset), dragStack); 
                } else if (hit(mx, my, pX + 10 + 9 * (SS + SP) + 16, invY, leftW, pH - invY)) {
                    boolean found = false;
                    for (ItemStack s : favorites) if (ItemStack.isSameItem(s, dragStack)) found = true;
                    if (!found && !dragStack.isEmpty()) { favorites.add(dragStack.copy()); saveFavorites(); }
                }
                
                if (!hasShiftDown()) {
                    dragStack = ItemStack.EMPTY;
                    isDragging = false;
                }
                return true;
            }
        }
        
        long now = System.currentTimeMillis();
        boolean isDoubleClick = (button == 0) && (now - lastClickTime < 250) && (Math.abs(mx - lastClickX) < 5) && (Math.abs(my - lastClickY) < 5);
        lastClickTime = now;
        lastClickX = mx;
        lastClickY = my;

        if (activeNumEditBox != null) {
            if (!activeNumEditBox.mouseClicked(mx, my, button)) {
                applyActiveNumEdit();
            } else {
                return true;
            }
        }

        if (isDoubleClick) {
            StationType t = tabs.get(tabIdx);
            int cx = pX + leftW / 2;
            if (hit(mx, my, pX, editorY, leftW, editorH)) {
                if (t == StationType.FURNACE) {
                    int cy = editorY + 20 + 40;
                    if (hit(mx, mY, cx - 48, cy + 42, 35, 12)) {
                        startActiveNumEdit("furnXp", cx - 48, cy + 42, 35, 12, String.format(Locale.ROOT, "%.1f", furnXp));
                        return true;
                    }
                    if (hit(mx, mY, cx + 42, cy + 42, 35, 12)) {
                        startActiveNumEdit("furnTime", cx + 42, cy + 42, 35, 12, String.valueOf(furnTime));
                        return true;
                    }
                }
                if (t == StationType.MIXING) {
                    int cy = editorY + 15 + 30;
                    int fluidY = cy + 3 * (SS + SP) + 20;
                    int sx = cx - 130;
                    for (int i = 0; i < 4; i++) {
                        int r = i / 2, c = i % 2;
                        int slotX = sx + c * 70;
                        int slotY = fluidY + r * 35;
                        int amtX = slotX + SS + 4, amtY = slotY + 4;
                        if (hit(mx, mY, amtX - 2, amtY - 2, 45, 12)) {
                            startActiveNumEdit("fluid_mix_in", amtX - 2, amtY - 2, 45, 12, String.valueOf(mixFluidIng.get(i).amount), i);
                            return true;
                        }
                    }
                    int rfx = cx + 10 + 60;
                    int amtX = rfx + SS + 4, amtY = cy + 4;
                    if (hit(mx, mY, amtX - 2, amtY - 2, 45, 12)) {
                        startActiveNumEdit("fluid_mix_out", amtX - 2, amtY - 2, 45, 12, String.valueOf(mixFluidResult.amount));
                        return true;
                    }
                    int oy = fluidY + 2 * 35 + 10;
                    if (hit(mx, mY, cx + 12, oy + 2, 35, 12)) {
                        startActiveNumEdit("mixTime", cx + 12, oy + 2, 35, 12, String.valueOf(mixTime));
                        return true;
                    }
                }
                if (t == StationType.PRESSING) {
                    int cy = editorY + 20 + 40;
                    if (pressBasin) {
                        int sx = cx - 70, rx = sx + SS + 50;
                        int rfx = rx + 60;
                        int amtX = rfx + SS + 4, amtY = cy + 4;
                        if (hit(mx, mY, amtX - 2, amtY - 2, 45, 12)) {
                            startActiveNumEdit("fluid_press_out", amtX - 2, amtY - 2, 45, 12, String.valueOf(pressFluidOut.amount));
                            return true;
                        }
                    }
                    int oy = cy + SS + 30;
                    if (hit(mx, mY, cx + 12, oy + 2, 35, 12)) {
                        startActiveNumEdit("pressTime", cx + 12, oy + 2, 35, 12, String.valueOf(pressTime));
                        return true;
                    }
                }
                if (t == StationType.FAN) {
                    int cy = editorY + 15 + 35;
                    int oy = cy + 2 * (SS + 12) + 10;
                    if (hit(mx, mY, cx + 12, oy + 2, 35, 12)) {
                        startActiveNumEdit("fanTime", cx + 12, oy + 2, 35, 12, String.valueOf(fanTime));
                        return true;
                    }
                }
                if (t == StationType.CRUSHING) {
                    int cy = editorY + 15 + 35;
                    int oy = cy + 4 * (SS + 12) + 10;
                    if (hit(mx, mY, cx + 12, oy + 2, 35, 12)) {
                        startActiveNumEdit("crushTime", cx + 12, oy + 2, 35, 12, String.valueOf(crushTime));
                        return true;
                    }
                }
            }
        }
        
        if (hit(mx, my, pX, invY - 4, leftW, 8)) {
            isDraggingSplitter = true;
            return true;
        }

        boolean hasSearch = bottomTab != BottomTab.INVENTORY;
        if (hasSearch && !showRecipesList) {
            boolean clickedBox = searchBox.mouseClicked(mx, my, button);
            searchBox.setFocused(clickedBox);
            if (clickedBox) return true;
        }
        
        fnFocused = false;
        int ffx = btnCopyX + 65 + font.width("File:") + 5;
        if (hit(mx, my, ffx, btnSaveY, leftW - ffx - 10, 16)) { fnFocused = true; return true; }
        
        int tabW = leftW / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            int tx = pX + i * tabW;
            int tw = (i == tabs.size() - 1) ? (pX + leftW - tx) : tabW;
            if (hit(mx, my, tx, pY, tw, TAB_H)) { tabIdx = i; scrollOffset = 0; return true; }
        }
        
        if (System.currentTimeMillis() - lastBtnClickTime >= 250) {
            if (hit(mx, my, btnSaveX, btnSaveY, 92, 16)) { lastBtnClickTime = System.currentTimeMillis(); save(); return true; }
            if (hit(mx, my, btnClearX, btnSaveY, 40, 16)) { lastBtnClickTime = System.currentTimeMillis(); clear(); return true; }
            if (hit(mx, my, btnCopyX, btnSaveY, 60, 16))  { lastBtnClickTime = System.currentTimeMillis(); copyJ(); return true; }
        }
        
        if (hit(mx, my, pX, editorY, leftW, editorH)) {
            if (tabs.get(tabIdx) == StationType.PRESSING) {
                int cx = pX + leftW / 2;
                int cy = editorY + 20;
                int wa = font.width("Press") + 12, wb = font.width("Press + Basin") + 12;
                if (hit(mx, mY, cx - 60, cy, wa, 16)) { pressBasin = false; return true; }
                if (hit(mx, mY, cx - 60 + wa + 2, cy, wb, 16)) { pressBasin = true; return true; }
            }
            if (tabs.get(tabIdx) == StationType.FAN) {
                int cx = pX + leftW / 2;
                int cy = editorY + 15;
                int wa = font.width("Washing") + 12, wb = font.width("Haunting") + 12;
                if (hit(mx, mY, cx - 65, cy, wa, 16)) { fanHaunting = false; return true; }
                if (hit(mx, mY, cx - 65 + wa + 2, cy, wb, 16)) { fanHaunting = true; return true; }
            }
            
            if (tabs.get(tabIdx) == StationType.FURNACE) {
                int cx = pX + leftW / 2, cy = editorY + 20;
                int tw = 0; for (String l : furnLabels) tw += font.width(l) + 16;
                int bx = cx - tw / 2;
                for (int i = 0; i < furnLabels.length; i++) {
                    int bw = font.width(furnLabels[i]) + 10;
                    if (hit(mx, mY, bx, cy, bw, 16)) { furnSubIdx = i; return true; }
                    bx += bw + 6;
                }
            }
            
            if (tabs.get(tabIdx) == StationType.MIXING) {
                int cx = pX + leftW / 2, cy = editorY + 15;
                int tw = 0; for (String l : heatLabels) tw += font.width(l) + 16;
                int bx = cx - tw / 2;
                for (int i = 0; i < heatLabels.length; i++) {
                    int bw = font.width(heatLabels[i]) + 10;
                    if (hit(mx, mY, bx, cy, bw, 16)) { mixHeat = i; return true; }
                    bx += bw + 6;
                }
            }
            
            if (tabs.get(tabIdx) == StationType.CRAFTING) {
                int cx = pX + leftW / 2, cy = editorY + 20;
                int wa = font.width("Shaped") + 12, wb = font.width("Shapeless") + 12;
                if (hit(mx, mY, cx - 70, cy, wa, 16)) { shapeless = false; return true; }
                if (hit(mx, mY, cx - 70 + wa + 2, cy, wb, 16)) { shapeless = true; return true; }
            }
            
            if (tabs.get(tabIdx) == StationType.CRUSHING) {
                int cx = pX + leftW / 2, cy = editorY + 15;
                int wa = font.width("Crushing") + 12, wb = font.width("Milling") + 12;
                if (hit(mx, mY, cx - 55, cy, wa, 16)) { isMilling = false; return true; }
                if (hit(mx, mY, cx - 55 + wa + 2, cy, wb, 16)) { isMilling = true; return true; }
            }
            
            if (handleFluidSpins(mx, mY)) return true;
            if (handleClicks(mx, mY)) return true;
            if ((button == 1 || hasControlDown()) && clearSlot(mx, mY)) return true;
        }
        
        if (hit(mx, my, pX, invY, leftW, pH - invY)) {
            String[] bTabs = {"Inventory", "Fluids", "Items", "Tags"};
            int tx = pX + 10;
            if (!showRecipesList) {
                for (int i = 0; i < bTabs.length; i++) {
                    int tw = font.width(bTabs[i]) + 10;
                    if (hit(mx, my, tx, invY + 4, tw, 14)) { bottomTab = BottomTab.values()[i]; bottomScroll = 0; return true; }
                    tx += tw + 4;
                }
            }
            
            int startX = pX + 10;
            int favX = startX + 9 * (SS + SP) + 16;
            int favBtnW = font.width(showRecipesList ? "Recipes" : "Favorite") + 10;
            if (hit(mx, my, favX, invY + 4, favBtnW, 14)) {
                showRecipesList = !showRecipesList;
                return true;
            }
            

            
            // Left Scrollbar click & drag start
            if (!showRecipesList) {
                int listY = (bottomTab == BottomTab.ITEMS) ? invY + 38 : invY + 22;
                int listH = pH - listY - 5;
                int sbX = startX + 9 * (SS + SP) + 2;
                int contentH = 0;
                if (bottomTab == BottomTab.INVENTORY && minecraft != null && minecraft.player != null) {
                    contentH = 4 * (SS + SP) + 8;
                } else if (bottomTab == BottomTab.FLUIDS) {
                    contentH = ((availableFluids.size() + 8) / 9) * (SS + SP);
                } else if (bottomTab == BottomTab.ITEMS) {
                    contentH = ((cachedFilteredItems.size() + 8) / 9) * (SS + SP);
                } else if (bottomTab == BottomTab.TAGS) {
                    contentH = ((cachedTags.size() + 8) / 9) * (SS + SP);
                }
                int bMax = Math.max(0, contentH - listH);
                if (bMax > 0 && hit(mx, my, sbX - 2, listY, 8, listH)) {
                    isDraggingBottomScroll = true;
                    float ratio = (float)(my - listY) / listH;
                    bottomScroll = Math.max(0, Math.min(bMax, ratio * bMax));
                    return true;
                }
            }

            // Right Scrollbar click & drag start
            int favCols = 5;
            int favSbX = favX + favCols * (SS + SP) + 2;
            int favListY = invY + 22;
            int favListH = pH - favListY - 5;
            int minFavSlots = 25;
            int favCount = Math.max(minFavSlots, ((favorites.size() + favCols - 1) / favCols + 1) * favCols);
            int favContentH = ((favCount + favCols - 1) / favCols) * (SS + SP);
            int fMax = Math.max(0, favContentH - favListH);
            if (fMax > 0 && hit(mx, my, favSbX - 2, favListY, 8, favListH)) {
                isDraggingFavScroll = true;
                float ratio = (float)(my - favListY) / favListH;
                favScroll = Math.max(0, Math.min(fMax, ratio * fMax));
                return true;
            }
            
            if (button == 1 && hasControlDown()) {
                int fY = (int)(my + favScroll);
                for (int i = 0; i < favorites.size(); i++) {
                    int c = i % favCols, r = i / favCols;
                    if (hit(mx, fY, favX + c * (SS + SP), favListY + r * (SS + SP), SS, SS)) {
                        favorites.remove(i); saveFavorites(); return true;
                    }
                }
            }
        }

        // ── Saved Recipes Click Handling ─────────────────────────────────────
        if (showRecipesList) {
            int recX = pX + 10;
            int recW = 9 * (SS + SP);
            int recListY = invY + 22;
            int recListH = pH - (recListY - pY) - 5;
            
            // Click handling for buttons above the list
            boolean hasSelected = selectedRecipeFile != null;
            int btnW = (recW - 4) / 2;
            
            if (hasSelected && System.currentTimeMillis() - lastBtnClickTime >= 250) {
                // Unload Button
                if (hit(mx, my, recX, invY + 4, btnW, 14)) {
                    lastBtnClickTime = System.currentTimeMillis();
                    unloadRecipe();
                    return true;
                }
                // Remove Button
                int remX = recX + btnW + 4;
                if (hit(mx, my, remX, invY + 4, btnW, 14)) {
                    lastBtnClickTime = System.currentTimeMillis();
                    deleteRecipe();
                    return true;
                }
            }
            
            if (hit(mx, my, recX, recListY, recW, recListH)) {
                int maxNameW = 0;
                for (java.io.File f : savedRecipeFiles) {
                    String name = f.getName();
                    if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                    maxNameW = Math.max(maxNameW, font.width(name));
                }
                int rhMax = Math.max(0, (maxNameW + 10) - recW);
                int totalH = savedRecipeFiles.size() * rowH;
                int rMax = Math.max(0, totalH - recListH);
                
                if (rMax > 0 && mx >= recX + recW - 8 && mx <= recX + recW) {
                    isDraggingRecipeScroll = true;
                    float ratio = (float)(my - recListY) / recListH;
                    recipeListScroll = Math.max(0, Math.min(rMax, ratio * rMax));
                    return true;
                }
                if (rhMax > 0 && my >= recListY + recListH - 8 && my <= recListY + recListH) {
                    isDraggingRecipeHorizScroll = true;
                    float ratio = (float)(mx - recX) / recW;
                    recipeListHorizScroll = Math.max(0, Math.min(rhMax, ratio * rhMax));
                    return true;
                }
                
                if (button == 0) {
                    int clickedIdx = (int) ((my - recListY + recipeListScroll) / rowH);
                    if (clickedIdx >= 0 && clickedIdx < savedRecipeFiles.size()) {
                        java.io.File f = savedRecipeFiles.get(clickedIdx);
                        selectedRecipeFile = f;
                        loadRecipeFromJson(f);
                        return true;
                    }
                }
            }
        }
        
        if (button == 0) {
            ItemStack fi = invAt(mx, my);
            if (fi != null && !fi.isEmpty()) { 
                dragStack = fi.copy(); dragStack.setCount(1); 
                isDragging = true; dragX = mx; dragY = my; 
                return true; 
            }
        }
        if (mx >= rightX && codeViewer != null && codeViewer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleFluidSpins(int mx, int mY) {
        if (tabs.get(tabIdx) == StationType.MIXING) {
            int cx = pX + leftW / 2;
            int cy = editorY + 15 + 30;
            int sx = cx - 130;
            int fluidY = cy + 3 * (SS + SP) + 20;
            
            for (int i = 0; i < 4; i++) {
                FluidEntry f = mixFluidIng.get(i);
                int r = i / 2, c = i % 2;
                int amtX = sx + c * 70 + SS + 4, amtY = fluidY + r * 35 + 4;
                if (hit(mx, mY, amtX - 2, amtY + 12, 10, 8)) { f.amount = Math.min(1000, f.amount + 250); return true; }
                if (hit(mx, mY, amtX + 10, amtY + 12, 10, 8)) { f.amount = Math.max(1, f.amount - 250); return true; }
            }
            
            int rfx = cx + 10 + 60;
            int amtX = rfx + SS + 4, amtY = cy + 4;
            if (hit(mx, mY, amtX - 2, amtY + 12, 10, 8)) { mixFluidResult.amount = Math.min(1000, mixFluidResult.amount + 250); return true; }
            if (hit(mx, mY, amtX + 10, amtY + 12, 10, 8)) { mixFluidResult.amount = Math.max(1, mixFluidResult.amount - 250); return true; }
        }
        if (tabs.get(tabIdx) == StationType.PRESSING && pressBasin) {
            int cx = pX + leftW / 2, cy = editorY + 20 + 40;
            int rfx = cx - 70 + SS + 50 + 60;
            int amtX = rfx + SS + 4, amtY = cy + 4;
            if (hit(mx, mY, amtX - 2, amtY + 12, 10, 8)) { pressFluidOut.amount = Math.min(1000, pressFluidOut.amount + 250); return true; }
            if (hit(mx, mY, amtX + 10, amtY + 12, 10, 8)) { pressFluidOut.amount = Math.max(1, pressFluidOut.amount - 250); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDraggingSplitter) {
            int newY = (int) my;
            int minHeight = 2 * (SS + SP) + 40;
            int maxHeight = pH - (pY + TAB_H + 40);
            invPanelHeight = Math.max(minHeight, Math.min(maxHeight, pH - newY));
            updateLayout();
            return true;
        }
        if (isDraggingBottomScroll && !showRecipesList) {
            int listY = (bottomTab == BottomTab.ITEMS) ? invY + 38 : invY + 22;
            int listH = pH - listY - 5;
            int contentH = 0;
            if (bottomTab == BottomTab.INVENTORY && minecraft != null && minecraft.player != null) {
                contentH = 4 * (SS + SP) + 8;
            } else if (bottomTab == BottomTab.FLUIDS) {
                contentH = ((availableFluids.size() + 8) / 9) * (SS + SP);
            } else if (bottomTab == BottomTab.ITEMS) {
                contentH = ((cachedFilteredItems.size() + 8) / 9) * (SS + SP);
            } else if (bottomTab == BottomTab.TAGS) {
                contentH = ((cachedTags.size() + 8) / 9) * (SS + SP);
            }
            int bMax = Math.max(0, contentH - listH);
            float ratio = (float)((int)my - listY) / listH;
            bottomScroll = Math.max(0, Math.min(bMax, ratio * bMax));
            return true;
        }
        if (isDraggingFavScroll) {
            int favCols = 5;
            int favListY = invY + 22;
            int favListH = pH - favListY - 5;
            int minFavSlots = 25;
            int favCount = Math.max(minFavSlots, ((favorites.size() + favCols - 1) / favCols + 1) * favCols);
            int favContentH = ((favCount + favCols - 1) / favCols) * (SS + SP);
            int fMax = Math.max(0, favContentH - favListH);
            float ratio = (float)((int)my - favListY) / favListH;
            favScroll = Math.max(0, Math.min(fMax, ratio * fMax));
            return true;
        }
        if (isDraggingRecipeScroll && showRecipesList) {
            int recListY = invY + 22;
            int recListH = pH - (recListY - pY) - 5;
            int totalH = savedRecipeFiles.size() * rowH;
            int rMax = Math.max(0, totalH - recListH);
            float ratio = (float)((int)my - recListY) / recListH;
            recipeListScroll = Math.max(0, Math.min(rMax, ratio * rMax));
            return true;
        }
        if (isDraggingRecipeHorizScroll && showRecipesList) {
            int recW = 9 * (SS + SP);
            int maxNameW = 0;
            for (java.io.File f : savedRecipeFiles) {
                String name = f.getName();
                if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                maxNameW = Math.max(maxNameW, font.width(name));
            }
            int rhMax = Math.max(0, (maxNameW + 10) - recW);
            float ratio = (float)((int)mx - (pX + 10)) / recW;
            recipeListHorizScroll = Math.max(0, Math.min(rhMax, ratio * rhMax));
            return true;
        }
        if (isDragging) { dragX = (int) mx; dragY = (int) my; return true; }
        if (codeViewer != null && codeViewer.mouseDragged((int) my)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingSplitter) {
            isDraggingSplitter = false;
            saveConfig();
            return true;
        }
        isDraggingBottomScroll = false;
        isDraggingFavScroll = false;
        isDraggingRecipeScroll = false;
        isDraggingRecipeHorizScroll = false;
        if (button == 0 && codeViewer != null) codeViewer.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (hit((int)mx, (int)my, pX, editorY, leftW, editorH)) {
            scrollOffset = (float) Math.max(0, Math.min(maxScroll, scrollOffset - sy * 20));
            return true;
        }
        if (!showRecipesList && hit((int)mx, (int)my, pX + 10, invY + 22, 9 * (SS + SP), pH - invY - 22)) {
            int listH = pH - (invY + 22) - 5;
            int contentH = 0;
            if (bottomTab == BottomTab.INVENTORY) contentH = 4 * (SS + SP) + 8;
            else if (bottomTab == BottomTab.FLUIDS) contentH = ((availableFluids.size() + 8) / 9) * (SS + SP);
            else if (bottomTab == BottomTab.ITEMS) contentH = ((cachedFilteredItems.size() + 8) / 9) * (SS + SP);
            else if (bottomTab == BottomTab.TAGS) contentH = ((cachedTags.size() + 8) / 9) * (SS + SP);
            float bMax = Math.max(0, contentH - listH);
            bottomScroll = (float) Math.max(0, Math.min(bMax, bottomScroll - sy * 20));
            return true;
        }
        int startX = pX + 10;
        int favCols = 5;
        int favX = startX + 9 * (SS + SP) + 16;
        if (hit((int)mx, (int)my, favX, invY + 22, favCols * (SS + SP), pH - invY - 22)) {
            int listH = pH - (invY + 22) - 5;
            int minFavSlots = 25;
            int favCount = Math.max(minFavSlots, ((favorites.size() + favCols - 1) / favCols + 1) * favCols);
            int favContentH = ((favCount + favCols - 1) / favCols) * (SS + SP);
            float fMax = Math.max(0, favContentH - listH);
            favScroll = (float) Math.max(0, Math.min(fMax, favScroll - sy * 20));
            return true;
        }
        if (showRecipesList) {
            int recX = startX;
            int recW = 9 * (SS + SP);
            int recListY = invY + 22;
            int recListH = pH - (recListY - pY) - 5;
            if (hit((int)mx, (int)my, recX, recListY, recW, recListH)) {
                int totalH = savedRecipeFiles.size() * rowH;
                float rMax = Math.max(0, totalH - recListH);
                recipeListScroll = (float) Math.max(0, Math.min(rMax, recipeListScroll - sy * 12));
                return true;
            }
        }
        if (codeViewer != null) return codeViewer.mouseScrolled(sy);
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (activeNumEditBox != null && activeNumEditBox.isFocused()) {
            if (key == 257 || key == 335) { // Enter or Keypad Enter
                applyActiveNumEdit();
                return true;
            }
            if (key == 256) { // Escape
                activeNumEditBox = null;
                activeFieldName = null;
                activeFieldIdx = -1;
                return true;
            }
            activeNumEditBox.keyPressed(key, scan, mods);
            return true;
        }
        if (key == 256) { if (fnFocused) { fnFocused = false; return true; } onClose(); return true; }
        boolean hasSearch = bottomTab != BottomTab.INVENTORY;
        if (hasSearch && !showRecipesList && searchBox.isFocused()) { searchBox.keyPressed(key, scan, mods); return true; }
        if (fnFocused) {
            if (key == 259 && !fileName.isEmpty() && fnCursor > 0) { fileName = fileName.substring(0, fnCursor - 1) + fileName.substring(fnCursor); fnCursor--; }
            else if (key == 261 && fnCursor < fileName.length()) { fileName = fileName.substring(0, fnCursor) + fileName.substring(fnCursor + 1); }
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
            if (Character.isDigit(chr) || (activeFieldName.equals("furnXp") && chr == '.')) {
                return activeNumEditBox.charTyped(chr, mods);
            }
            return true;
        }
        boolean hasSearch = bottomTab != BottomTab.INVENTORY;
        if (hasSearch && !showRecipesList && searchBox.isFocused()) { searchBox.charTyped(chr, mods); return true; }
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

    private void save() {
        String j = buildJson();
        try {
            java.nio.file.Path dir = cz.maxtechnik.opm.client.recipe.RecipeFileWriter.getRecipeDir();
            java.nio.file.Files.createDirectories(dir);
            String safeName = fileName.replaceAll("[^a-z0-9_/]", "_").toLowerCase();
            if (safeName.isBlank()) safeName = "recipe";
            java.nio.file.Path file = dir.resolve(safeName + ".json");
            java.nio.file.Files.writeString(file, j, java.nio.charset.StandardCharsets.UTF_8);
            scanSavedRecipes();
            for (java.io.File f : savedRecipeFiles) {
                if (f.getName().equalsIgnoreCase(safeName + ".json")) {
                    selectedRecipeFile = f;
                    break;
                }
            }
            status("Saved!", true);
        } catch (Exception e) {
            status("Save failed!", false);
        }
    }
    private void copyJ() { if (minecraft != null) minecraft.keyboardHandler.setClipboard(buildJson()); status("Copied!", true); }
    
    private void deleteRecipe() {
        if (selectedRecipeFile != null && selectedRecipeFile.exists()) {
            selectedRecipeFile.delete();
            selectedRecipeFile = null;
            fileName = "";
            fnCursor = 0;
            clear();
            scanSavedRecipes();
            status("Deleted!", true);
        }
    }
    
    private void unloadRecipe() {
        selectedRecipeFile = null;
        fileName = "";
        fnCursor = 0;
        clear();
        status("Unloaded!", true);
    }

    private void clear() {
        craftGrid.replaceAll(s -> ItemStack.EMPTY); mechGrid.replaceAll(s -> ItemStack.EMPTY);
        mixIng.replaceAll(s -> ItemStack.EMPTY); mixFluidIng.forEach(f -> f.proxy = ItemStack.EMPTY);
        crushOuts.forEach(o -> { o.stack = ItemStack.EMPTY; o.chance = 1f; o.count = 1; });
        fanOuts.forEach(o -> { o.stack = ItemStack.EMPTY; o.chance = 1f; o.count = 1; });
        craftResult = furnIn = furnOut = stoneIn = stoneOut = smTemplate = smBase = smAddition = smResult
                = mixResult = pressIn = pressOut = crushIn = fanIn = ItemStack.EMPTY;
        mixFluidResult.proxy = ItemStack.EMPTY; pressFluidOut.proxy = ItemStack.EMPTY;
        craftCount = furnCount = stoneCount = smCount = mixCount = pressCount = 1; 
        status("Cleared.", true);
    }
    private void status(String m, boolean ok) { statusMsg = m; statusOk = ok; statusUntil = System.currentTimeMillis() + 3000; }

    private void drop(int mx, int mY, ItemStack s) {
        StationType t = tabs.get(tabIdx);
        int[] gr = gridParams(t);
        if (gr != null) {
            List<ItemStack> gl = gridList(t);
            for (int r = 0; r < gr[1]; r++) for (int c = 0; c < gr[0]; c++) {
                int bx = gr[2] + c * (gr[4] + gr[5]), by = gr[3] + r * (gr[4] + gr[5]);
                if (hit(mx, mY, bx, by, gr[4], gr[4])) { gl.set(r * gr[0] + c, s.copy()); return; }
            }
        }
        int cx = pX + leftW / 2;
        switch (t) {
            case CRAFTING -> {
                int cy = editorY + 20 + 30;
                int ax = cx - 70 + 3 * (SS + SP) + 15;
                int ay = cy + SS + SP;
                if (hit(mx, mY, ax + 20, ay - 9, SS, SS)) craftResult = s.copy();
            }
            case FURNACE -> {
                int cy = editorY + 20 + 40;
                if (hit(mx, mY, cx - 60, cy, SS, SS)) furnIn = s.copy();
                if (hit(mx, mY, cx - 60 + SS + 40, cy, SS, SS)) furnOut = s.copy();
            }
            case STONECUTTER -> {
                int cy = editorY + 40;
                if (hit(mx, mY, cx - 50, cy, SS, SS)) stoneIn = s.copy();
                if (hit(mx, mY, cx - 50 + SS + 40, cy, SS, SS)) stoneOut = s.copy();
            }
            case SMITHING -> {
                int cy = editorY + 40;
                int sx = cx - 120;
                if (hit(mx, mY, sx, cy, SS, SS)) smTemplate = s.copy();
                if (hit(mx, mY, sx + SS + 26, cy, SS, SS)) smBase = s.copy();
                if (hit(mx, mY, sx + 2 * (SS + 26), cy, SS, SS)) smAddition = s.copy();
                if (hit(mx, mY, sx + 3 * (SS + 26) + 14 - SS - 26, cy, SS, SS)) smResult = s.copy();
            }
            case MECH_CRAFTING -> {
                int cy = editorY + 20;
                int sz = 16, pad = 1, gridW = 9 * (sz + pad);
                int sx = cx - gridW / 2 - 40;
                int ay = cy + (9 * (sz + pad)) / 2 - 4;
                if (hit(mx, mY, sx + gridW + 15 + 20, ay - 4, SS, SS)) craftResult = s.copy();
            }
            case MIXING -> {
                int cy = editorY + 15 + 30;
                int sx = cx - 130;
                int fluidY = cy + 3 * (SS + SP) + 20;
                
                for (int i = 0; i < 4; i++) {
                    int r = i / 2, c = i % 2;
                    if (hit(mx, mY, sx + c * 70, fluidY + r * 35, SS, SS)) { mixFluidIng.get(i).proxy = s.copy(); return; }
                }
                
                int rx = cx + 10;
                if (hit(mx, mY, rx, cy, SS, SS)) mixResult = s.copy();
                int rfx = rx + 60;
                if (hit(mx, mY, rfx, cy, SS, SS)) mixFluidResult.proxy = s.copy();
            }
            case PRESSING -> {
                int cy = editorY + 20 + 40;
                int sx = cx - 70;
                if (hit(mx, mY, sx, cy, SS, SS)) pressIn = s.copy();
                if (hit(mx, mY, sx + SS + 50, cy, SS, SS)) pressOut = s.copy();
                if (pressBasin && hit(mx, mY, sx + SS + 50 + 60, cy, SS, SS)) pressFluidOut.proxy = s.copy();
            }
            case CRUSHING -> {
                int cy = editorY + 15 + 35;
                int sx = cx - 120;
                if (hit(mx, mY, sx, cy, SS, SS)) { crushIn = s.copy(); return; }
                int outX = sx + SS + 30, colW = 110;
                for (int i = 0; i < 8; i++) {
                    if (hit(mx, mY, outX + (i / 4) * colW, cy + (i % 4) * (SS + 12), SS, SS)) { crushOuts.get(i).stack = s.copy(); return; }
                }
            }
            case FAN -> {
                int cy = editorY + 15 + 35;
                int sx = cx - 120;
                if (hit(mx, mY, sx, cy, SS, SS)) { fanIn = s.copy(); return; }
                int outX = sx + SS + 30, colW = 110;
                for (int i = 0; i < 4; i++) {
                    if (hit(mx, mY, outX + (i / 2) * colW, cy + (i % 2) * (SS + 12), SS, SS)) { fanOuts.get(i).stack = s.copy(); return; }
                }
            }
        }
    }

    private boolean clearSlot(int mx, int mY) {
        StationType t = tabs.get(tabIdx);
        int[] gr = gridParams(t);
        if (gr != null) {
            List<ItemStack> gl = gridList(t);
            for (int r = 0; r < gr[1]; r++) for (int c = 0; c < gr[0]; c++) {
                int bx = gr[2] + c * (gr[4] + gr[5]), by = gr[3] + r * (gr[4] + gr[5]);
                if (hit(mx, mY, bx, by, gr[4], gr[4])) { gl.set(r * gr[0] + c, ItemStack.EMPTY); return true; }
            }
        }
        int cx = pX + leftW / 2;
        if (t == StationType.CRAFTING) {
            int cy = editorY + 20 + 30;
            int ax = cx - 70 + 3 * (SS + SP) + 15;
            int ay = cy + SS + SP;
            if (hit(mx, mY, ax + 20, ay - 9, SS, SS)) { craftResult = ItemStack.EMPTY; return true; }
        }
        if (t == StationType.FURNACE) {
            int cy = editorY + 20 + 40;
            if (hit(mx, mY, cx - 60, cy, SS, SS)) { furnIn = ItemStack.EMPTY; return true; }
            if (hit(mx, mY, cx - 60 + SS + 40, cy, SS, SS)) { furnOut = ItemStack.EMPTY; return true; }
        }
        if (t == StationType.STONECUTTER) {
            int cy = editorY + 40;
            if (hit(mx, mY, cx - 50, cy, SS, SS)) { stoneIn = ItemStack.EMPTY; return true; }
            if (hit(mx, mY, cx - 50 + SS + 40, cy, SS, SS)) { stoneOut = ItemStack.EMPTY; return true; }
        }
        if (t == StationType.SMITHING) {
            int cy = editorY + 40;
            int sx = cx - 120;
            if (hit(mx, mY, sx, cy, SS, SS)) { smTemplate = ItemStack.EMPTY; return true; }
            if (hit(mx, mY, sx + SS + 26, cy, SS, SS)) { smBase = ItemStack.EMPTY; return true; }
            if (hit(mx, mY, sx + 2 * (SS + 26), cy, SS, SS)) { smAddition = ItemStack.EMPTY; return true; }
            if (hit(mx, mY, sx + 3 * (SS + 26) + 14 - SS - 26, cy, SS, SS)) { smResult = ItemStack.EMPTY; return true; }
        }
        if (t == StationType.MECH_CRAFTING) {
            int cy = editorY + 20, sz = 16, pad = 1, gridW = 9 * (sz + pad);
            int sx = cx - gridW / 2 - 40, ay = cy + (9 * (sz + pad)) / 2 - 4;
            if (hit(mx, mY, sx + gridW + 15 + 20, ay - 4, SS, SS)) { craftResult = ItemStack.EMPTY; return true; }
        }
        if (t == StationType.CRUSHING) {
            int cy = editorY + 15 + 35;
            int sx = cx - 120;
            if (hit(mx, mY, sx, cy, SS, SS)) { crushIn = ItemStack.EMPTY; return true; }
            int outX = sx + SS + 30, colW = 110;
            for (int i = 0; i < 8; i++) {
                if (hit(mx, mY, outX + (i / 4) * colW, cy + (i % 4) * (SS + 12), SS, SS)) { crushOuts.get(i).stack = ItemStack.EMPTY; return true; }
            }
        }
        if (t == StationType.FAN) {
            int cy = editorY + 15 + 35;
            int sx = cx - 120;
            if (hit(mx, mY, sx, cy, SS, SS)) { fanIn = ItemStack.EMPTY; return true; }
            int outX = sx + SS + 30, colW = 110;
            for (int i = 0; i < 4; i++) {
                if (hit(mx, mY, outX + (i / 2) * colW, cy + (i % 2) * (SS + 12), SS, SS)) { fanOuts.get(i).stack = ItemStack.EMPTY; return true; }
            }
        }
        if (t == StationType.MIXING) {
            int cy = editorY + 15 + 30;
            int sx = cx - 130;
            int fluidY = cy + 3 * (SS + SP) + 20;
            for (int i = 0; i < 4; i++) {
                int r = i / 2, c = i % 2;
                if (hit(mx, mY, sx + c * 70, fluidY + r * 35, SS, SS)) { mixFluidIng.get(i).proxy = ItemStack.EMPTY; return true; }
            }
            int rx = cx + 10;
            if (hit(mx, mY, rx, cy, SS, SS)) { mixResult = ItemStack.EMPTY; return true; }
            int rfx = rx + 60;
            if (hit(mx, mY, rfx, cy, SS, SS)) { mixFluidResult.proxy = ItemStack.EMPTY; return true; }
        }
        if (t == StationType.PRESSING) {
            int cy = editorY + 20 + 40;
            int sx = cx - 70;
            if (hit(mx, mY, sx, cy, SS, SS)) { pressIn = ItemStack.EMPTY; return true; }
            if (hit(mx, mY, sx + SS + 50, cy, SS, SS)) { pressOut = ItemStack.EMPTY; return true; }
            if (pressBasin && hit(mx, mY, sx + SS + 50 + 60, cy, SS, SS)) { pressFluidOut.proxy = ItemStack.EMPTY; return true; }
        }
        return false;
    }

    private int[] gridParams(StationType t) {
        int cx = pX + leftW / 2;
        return switch (t) {
            case CRAFTING -> new int[]{3, 3, cx - 70, editorY + 20 + 30, SS, SP};
            case MIXING -> new int[]{3, 3, cx - 130, editorY + 15 + 30, SS, SP};
            case MECH_CRAFTING -> new int[]{9, 9, cx - (9 * 17) / 2 - 40, editorY + 20, 16, 1};
            default -> null;
        };
    }

    private List<ItemStack> gridList(StationType t) {
        return switch (t) {
            case MECH_CRAFTING -> mechGrid;
            case MIXING -> mixIng;
            default -> craftGrid;
        };
    }

    private boolean handleClicks(int mx, int mY) {
        StationType t = tabs.get(tabIdx);
        int cx = pX + leftW / 2;
        
        if (t == StationType.CRAFTING) {
            int cy = editorY + 20 + 30;
            int ax = cx - 70 + 3 * (SS + SP) + 15;
            int rx = ax + 20;
            int cpx = rx + SS + 6, cpy = cy + SS + SP - 7;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { craftCount = Math.min(64, craftCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { craftCount = Math.max(1, craftCount - 1); return true; }
        }
        if (t == StationType.MECH_CRAFTING) {
            int cy = editorY + 20, sz = 16, pad = 1, gridW = 9 * (sz + pad);
            int sx = cx - gridW / 2 - 40, ay = cy + (9 * (sz + pad)) / 2 - 4;
            int rx = sx + gridW + 15 + 20;
            int cpx = rx + SS + 6, cpy = ay - 2;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { craftCount = Math.min(64, craftCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { craftCount = Math.max(1, craftCount - 1); return true; }
        }
        if (t == StationType.FURNACE) {
            int cy = editorY + 20 + 40;
            int sx = cx - 60, rx = sx + SS + 40;
            int cpx = rx + SS + 6, cpy = cy + 2;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { furnCount = Math.min(64, furnCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { furnCount = Math.max(1, furnCount - 1); return true; }
            
            int xpX = cx - 20, xpY = cy + 42;
            if (hit(mx, mY, xpX, xpY, 10, 8)) { furnXp = Math.min(100f, furnXp + 0.1f); return true; }
            if (hit(mx, mY, xpX, xpY + 8, 10, 8)) { furnXp = Math.max(0f, furnXp - 0.1f); return true; }

            int tX = cx + 80, tY = cy + 42;
            if (hit(mx, mY, tX, tY, 10, 8)) { furnTime = Math.min(10000, furnTime + 50); return true; }
            if (hit(mx, mY, tX, tY + 8, 10, 8)) { furnTime = Math.max(10, furnTime - 50); return true; }
        }
        if (t == StationType.STONECUTTER) {
            int cy = editorY + 40, sx = cx - 50, rx = sx + SS + 40;
            int cpx = rx + SS + 6, cpy = cy + 2;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { stoneCount = Math.min(64, stoneCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { stoneCount = Math.max(1, stoneCount - 1); return true; }
        }
        if (t == StationType.SMITHING) {
            int cy = editorY + 40, sx = cx - 120 + 3 * (SS + 26);
            int rx = sx - 12, cpx = rx + SS + 6, cpy = cy + 2;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { smCount = Math.min(64, smCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { smCount = Math.max(1, smCount - 1); return true; }
        }
        if (t == StationType.MIXING) {
            int cy = editorY + 15 + 30;
            int rx = cx + 10;
            int cpx = rx + SS + 6, cpy = cy + 2;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { mixCount = Math.min(64, mixCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { mixCount = Math.max(1, mixCount - 1); return true; }
            
            int fluidY = cy + 3 * (SS + SP) + 20;
            int tX = cx + 55, tY = fluidY + 2 * 35 + 10 + 2;
            if (hit(mx, mY, tX, tY, 10, 8)) { mixTime = Math.min(10000, mixTime + 10); return true; }
            if (hit(mx, mY, tX, tY + 8, 10, 8)) { mixTime = Math.max(10, mixTime - 10); return true; }
        }
        if (t == StationType.PRESSING) {
            int cy = editorY + 20 + 40;
            int sx = cx - 70, rx = sx + SS + 50;
            int cpx = rx + SS + 6, cpy = cy + 2;
            if (hit(mx, mY, cpx + 18, cpy, 10, 8)) { pressCount = Math.min(64, pressCount + 1); return true; }
            if (hit(mx, mY, cpx + 18, cpy + 8, 10, 8)) { pressCount = Math.max(1, pressCount - 1); return true; }
            
            int tX = cx + 55, tY = cy + SS + 32;
            if (hit(mx, mY, tX, tY, 10, 8)) { pressTime = Math.min(10000, pressTime + 10); return true; }
            if (hit(mx, mY, tX, tY + 8, 10, 8)) { pressTime = Math.max(10, pressTime - 10); return true; }
        }
        if (t == StationType.CRUSHING) {
            int cy = editorY + 15 + 35;
            int outX = cx - 120 + SS + 30, colW = 110;
            for (int i = 0; i < 8; i++) {
                CrushingOutput co = crushOuts.get(i);
                int ox = outX + (i / 4) * colW, oy = cy + (i % 4) * (SS + 12);
                int cpx = ox + SS + 4, cpy = oy + 2;
                if (hit(mx, mY, cpx + 16, cpy - 2, 9, 9)) { co.count = Math.min(64, co.count + 1); return true; }
                if (hit(mx, mY, cpx + 16, cpy + 7, 9, 9)) { co.count = Math.max(1, co.count - 1); return true; }
                int chX = cpx + 30;
                if (hit(mx, mY, chX, cpy - 2, 9, 9)) { co.chance = Math.min(1f, co.chance + 0.05f); return true; }
                if (hit(mx, mY, chX, cpy + 7, 9, 9)) { co.chance = Math.max(0.05f, co.chance - 0.05f); return true; }
            }
            
            int tX = cx + 55, tY = cy + 4 * (SS + 12) + 12;
            if (hit(mx, mY, tX, tY, 10, 8)) { crushTime = Math.min(10000, crushTime + 10); return true; }
            if (hit(mx, mY, tX, tY + 8, 10, 8)) { crushTime = Math.max(10, crushTime - 10); return true; }
        }
        if (t == StationType.FAN) {
            int cy = editorY + 15 + 35;
            int outX = cx - 120 + SS + 30, colW = 110;
            for (int i = 0; i < 4; i++) {
                CrushingOutput co = fanOuts.get(i);
                int ox = outX + (i / 2) * colW, oy = cy + (i % 2) * (SS + 12);
                int cpx = ox + SS + 4, cpy = oy + 2;
                if (hit(mx, mY, cpx + 16, cpy - 2, 9, 9)) { co.count = Math.min(64, co.count + 1); return true; }
                if (hit(mx, mY, cpx + 16, cpy + 7, 9, 9)) { co.count = Math.max(1, co.count - 1); return true; }
                int chX = cpx + 30;
                if (hit(mx, mY, chX, cpy - 2, 9, 9)) { co.chance = Math.min(1f, co.chance + 0.05f); return true; }
                if (hit(mx, mY, chX, cpy + 7, 9, 9)) { co.chance = Math.max(0.05f, co.chance - 0.05f); return true; }
            }
            
            int tX = cx + 55, tY = cy + 2 * (SS + 12) + 12;
            if (hit(mx, mY, tX, tY, 10, 8)) { fanTime = Math.min(10000, fanTime + 10); return true; }
            if (hit(mx, mY, tX, tY + 8, 10, 8)) { fanTime = Math.max(10, fanTime - 10); return true; }
        }
        return false;
    }

    private ItemStack invAt(int mx, int my) {
        int cx = pX + leftW / 2;
        int mY = (int)(my + scrollOffset);
        if (hit(mx, my, pX, editorY, leftW, editorH)) {
            StationType t = tabs.get(tabIdx);
            int[] gr = gridParams(t);
            if (gr != null) {
                List<ItemStack> gl = gridList(t);
                for (int r = 0; r < gr[1]; r++) for (int c = 0; c < gr[0]; c++) {
                    int bx = gr[2] + c * (gr[4] + gr[5]), by = gr[3] + r * (gr[4] + gr[5]);
                    if (hit(mx, mY, bx, by, gr[4], gr[4])) {
                        int idx = r * gr[0] + c;
                        if (idx < gl.size()) return gl.get(idx);
                    }
                }
            }
            if (t == StationType.CRAFTING) {
                int cy = editorY + 20 + 30;
                int ax = cx - 70 + 3 * (SS + SP) + 15;
                if (hit(mx, mY, ax + 20, cy + SS + SP - 9, SS, SS)) return craftResult;
            }
            if (t == StationType.MECH_CRAFTING) {
                int cy = editorY + 20, sz = 16, pad = 1, gridW = 9 * (sz + pad);
                int sx = cx - gridW / 2 - 40, ay = cy + (9 * (sz + pad)) / 2 - 4;
                if (hit(mx, mY, sx + gridW + 15 + 20, ay - 4, SS, SS)) return craftResult;
            }
            if (t == StationType.FURNACE) {
                int cy = editorY + 20 + 40;
                if (hit(mx, mY, cx - 60, cy, SS, SS)) return furnIn;
                if (hit(mx, mY, cx - 60 + SS + 40, cy, SS, SS)) return furnOut;
            }
            if (t == StationType.STONECUTTER) {
                int cy = editorY + 40;
                if (hit(mx, mY, cx - 50, cy, SS, SS)) return stoneIn;
                if (hit(mx, mY, cx - 50 + SS + 40, cy, SS, SS)) return stoneOut;
            }
            if (t == StationType.SMITHING) {
                int cy = editorY + 40;
                int sx = cx - 120;
                if (hit(mx, mY, sx, cy, SS, SS)) return smTemplate;
                if (hit(mx, mY, sx + SS + 26, cy, SS, SS)) return smBase;
                if (hit(mx, mY, sx + 2 * (SS + 26), cy, SS, SS)) return smAddition;
                if (hit(mx, mY, sx + 3 * (SS + 26) + 14 - SS - 26, cy, SS, SS)) return smResult;
            }
            if (t == StationType.MIXING) {
                int cy = editorY + 15 + 30;
                int rx = cx + 10;
                if (hit(mx, mY, rx, cy, SS, SS)) return mixResult;
            }
            if (t == StationType.PRESSING) {
                int cy = editorY + 20 + 40;
                int sx = cx - 70;
                if (hit(mx, mY, sx, cy, SS, SS)) return pressIn;
                if (hit(mx, mY, sx + SS + 50, cy, SS, SS)) return pressOut;
            }
            if (t == StationType.CRUSHING) {
                int cy = editorY + 15 + 35;
                int sx = cx - 120;
                if (hit(mx, mY, sx, cy, SS, SS)) return crushIn;
                int outX = sx + SS + 30, colW = 110;
                for (int i = 0; i < 8; i++) {
                    if (hit(mx, mY, outX + (i / 4) * colW, cy + (i % 4) * (SS + 12), SS, SS)) return crushOuts.get(i).stack;
                }
            }
            if (t == StationType.FAN) {
                int cy = editorY + 15 + 35;
                int sx = cx - 120;
                if (hit(mx, mY, sx, cy, SS, SS)) return fanIn;
                int outX = sx + SS + 30, colW = 110;
                for (int i = 0; i < 4; i++) {
                    if (hit(mx, mY, outX + (i / 2) * colW, cy + (i % 2) * (SS + 12), SS, SS)) return fanOuts.get(i).stack;
                }
            }
        }

        int startX = pX + 10;
        boolean hasSearch = bottomTab != BottomTab.INVENTORY;
        int listY = hasSearch ? invY + 38 : invY + 22;
        int listH = pH - listY - 5;
        
        if (!showRecipesList && hit(mx, my, startX, listY, 9 * (SS + SP), listH)) {
            mY = (int)(my + bottomScroll);
            if (bottomTab == BottomTab.INVENTORY && minecraft != null && minecraft.player != null) {
                Inventory inv = minecraft.player.getInventory();
                for (int r = 0; r < 3; r++) for (int c = 0; c < INV_COLS; c++) {
                    if (hit(mx, mY, startX + c * (SS + SP), listY + r * (SS + SP), SS, SS)) return inv.getItem(9 + r * INV_COLS + c);
                }
                for (int c = 0; c < INV_COLS; c++)
                    if (hit(mx, mY, startX + c * (SS + SP), listY + 3 * (SS + SP) + 8, SS, SS)) return inv.getItem(c);
            } else if (bottomTab == BottomTab.FLUIDS) {
                String q = searchBox.getValue().toLowerCase(Locale.ROOT);
                List<ItemStack> filtered = new ArrayList<>();
                if (q.isEmpty()) filtered.addAll(availableFluids);
                else filtered.addAll(availableFluids.stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList());
                for (int i = 0; i < filtered.size(); i++) {
                    int c = i % 9, r = i / 9;
                    if (hit(mx, mY, startX + c * (SS + SP), listY + r * (SS + SP), SS, SS)) return filtered.get(i);
                }
            } else if (bottomTab == BottomTab.ITEMS) {
                for (int i = 0; i < cachedFilteredItems.size(); i++) {
                    int c = i % 9, r = i / 9;
                    if (hit(mx, mY, startX + c * (SS + SP), listY + r * (SS + SP), SS, SS)) return cachedFilteredItems.get(i);
                }
            } else if (bottomTab == BottomTab.TAGS) {
                String q = searchBox.getValue().toLowerCase(Locale.ROOT);
                List<ItemStack> filtered = new ArrayList<>();
                if (q.isEmpty()) filtered.addAll(cachedTags);
                else filtered.addAll(cachedTags.stream().filter(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList());
                for (int i = 0; i < filtered.size(); i++) {
                    int c = i % 9, r = i / 9;
                    if (hit(mx, mY, startX + c * (SS + SP), listY + r * (SS + SP), SS, SS)) return filtered.get(i);
                }
            }
        }
        
        int favX = pX + 10 + 9 * (SS + SP) + 16;
        int favCols = 5;
        int favListY = invY + 22;
        int favListH = pH - favListY - 5;
        if (hit(mx, my, favX, favListY, favCols * (SS + SP), favListH)) {
            int fY = (int)(my + favScroll);
            for (int i = 0; i < favorites.size(); i++) {
                int c = i % favCols, r = i / favCols;
                if (hit(mx, fY, favX + c * (SS + SP), favListY + r * (SS + SP), SS, SS)) return favorites.get(i);
            }
        }
        return null;
    }

    private boolean hit(int mx, int my, int hx, int hy, int hw, int hh) {
        return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
    }

    private String truncate(String t, int maxW) {
        if (font.width(t) <= maxW) return t;
        while (font.width(t + "…") > maxW && !t.isEmpty()) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    private static List<ItemStack> initList(int n) {
        List<ItemStack> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(ItemStack.EMPTY);
        return l;
    }

    private static List<FluidEntry> initFluidList(int n) {
        List<FluidEntry> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(new FluidEntry());
        return l;
    }

    private void startActiveNumEdit(String fieldName, int x, int y, int w, int h, String initialVal) {
        startActiveNumEdit(fieldName, x, y, w, h, initialVal, -1);
    }
    
    private int renderFan(GuiGraphics g, int mx, int my) {
        int cx = pX + leftW / 2;
        int cy = editorY + 15;
        
        drawToggle2(g, mx, my, cx - 65, cy, "Washing", "Haunting", !fanHaunting);
        
        cy += 35;
        int sx = cx - 120;
        g.drawCenteredString(font, "Input", sx + 9, cy - 12, C_LABEL);
        slot(g, mx, my, fanIn, sx, cy, C_SLOT);
        
        g.drawString(font, "→", sx + SS + 10, cy + 5, C_LABEL, false);
        
        int outX = sx + SS + 30;
        int colW = 110;
        g.drawString(font, "Outputs (chance via +/-):", outX, cy - 12, C_LABEL, false);
        
        for (int i = 0; i < 4; i++) {
            CrushingOutput co = fanOuts.get(i);
            int col = i / 2;
            int row = i % 2;
            int ox = outX + col * colW;
            int oy = cy + row * (SS + 12);
            
            slot(g, mx, my, co.stack, ox, oy, co.isEmpty() ? C_SLOT : C_SLOT_RES);
            
            int cpx = ox + SS + 4, cpy = oy + 2;
            g.drawString(font, "×" + co.count, cpx, cpy + 2, C_TEXT, false);
            boolean hP = hit(mx, my, cpx + 16, cpy - 2, 9, 9);
            boolean hM = hit(mx, my, cpx + 16, cpy + 7, 9, 9);
            g.fill(cpx + 16, cpy - 2, cpx + 25, cpy + 7, hP ? C_BTN_H : C_BTN);
            g.fill(cpx + 16, cpy + 7, cpx + 25, cpy + 16, hM ? C_BTN_H : C_BTN);
            g.drawCenteredString(font, "+", cpx + 20, cpy - 2, C_TEXT);
            g.drawCenteredString(font, "-", cpx + 20, cpy + 7, C_TEXT);
            
            int chX = cpx + 30;
            String chStr = co.chance >= 1f ? "100%" : Math.round(co.chance * 100) + "%";
            boolean hCP = hit(mx, my, chX, cpy - 2, 9, 9);
            boolean hCM = hit(mx, my, chX, cpy + 7, 9, 9);
            g.fill(chX, cpy - 2, chX + 9, cpy + 7, hCP ? C_BTN_H : C_BTN);
            g.fill(chX, cpy + 7, chX + 9, cpy + 16, hCM ? C_BTN_H : C_BTN);
            g.drawCenteredString(font, "+", chX + 4, cpy - 2, C_LABEL);
            g.drawCenteredString(font, "-", chX + 4, cpy + 7, C_LABEL);
            g.drawString(font, chStr, chX + 12, cpy + 3, co.isEmpty() ? C_LABEL : 0xFFAAFF88, false);
        }
        
        int oy = cy + 2 * (SS + 12) + 10;
        g.drawString(font, "Time:", cx - 20, oy + 4, C_LABEL, false);
        g.drawString(font, fanTime + " t", cx + 15, oy + 4, C_TEXT, false);
        valSpinner(g, mx, my, cx + 55, oy + 2);
        
        return oy + 30 - editorY;
    }

    private void startActiveNumEdit(String fieldName, int x, int y, int w, int h, String initialVal, int idx) {
        activeFieldName = fieldName;
        activeFieldIdx = idx;
        activeNumEditBox = new EditBox(font, x, (int)(y - scrollOffset), w, h, Component.empty());
        activeNumEditBox.setValue(initialVal);
        activeNumEditBox.setMaxLength(5);
        activeNumEditBox.setFocused(true);
    }

    private void applyActiveNumEdit() {
        if (activeNumEditBox == null || activeFieldName == null) return;
        String val = activeNumEditBox.getValue().trim();
        try {
            if (activeFieldName.equals("furnXp")) {
                float f = Float.parseFloat(val);
                furnXp = Math.max(0.0f, Math.min(100.0f, f));
            } else if (activeFieldName.equals("furnTime")) {
                int i = Integer.parseInt(val);
                furnTime = Math.max(1, Math.min(99999, i));
            } else if (activeFieldName.equals("mixTime")) {
                int i = Integer.parseInt(val);
                mixTime = Math.max(1, Math.min(99999, i));
            } else if (activeFieldName.equals("pressTime")) {
                int i = Integer.parseInt(val);
                pressTime = Math.max(1, Math.min(99999, i));
            } else if (activeFieldName.equals("crushTime")) {
                int i = Integer.parseInt(val);
                crushTime = Math.max(1, Math.min(99999, i));
            } else if (activeFieldName.equals("fanTime")) {
                int i = Integer.parseInt(val);
                fanTime = Math.max(1, Math.min(99999, i));
            } else if (activeFieldName.equals("fluid_mix_in")) {
                int i = Integer.parseInt(val);
                if (activeFieldIdx >= 0 && activeFieldIdx < mixFluidIng.size()) {
                    mixFluidIng.get(activeFieldIdx).amount = Math.max(1, Math.min(1000, i));
                }
            } else if (activeFieldName.equals("fluid_mix_out")) {
                int i = Integer.parseInt(val);
                mixFluidResult.amount = Math.max(1, Math.min(1000, i));
            } else if (activeFieldName.equals("fluid_press_out")) {
                int i = Integer.parseInt(val);
                pressFluidOut.amount = Math.max(1, Math.min(1000, i));
            }
        } catch (NumberFormatException e) {
            // Ignore invalid format
        }
    }

    private void scanSavedRecipes() {
        savedRecipeFiles.clear();
        try {
            java.nio.file.Path dir = cz.maxtechnik.opm.client.recipe.RecipeFileWriter.getRecipeDir();
            if (java.nio.file.Files.exists(dir)) {
                try (var stream = java.nio.file.Files.list(dir)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                          .map(java.nio.file.Path::toFile)
                          .sorted(java.util.Comparator.comparing(java.io.File::getName))
                          .forEach(savedRecipeFiles::add);
                }
            }
        } catch (Exception ignored) {}
    }

    private ItemStack parseIngredient(com.google.gson.JsonElement el) {
        if (el == null || el.isJsonNull()) return ItemStack.EMPTY;
        if (el.isJsonArray()) {
            com.google.gson.JsonArray arr = el.getAsJsonArray();
            if (arr.size() > 0) return parseIngredient(arr.get(0));
        }
        if (el.isJsonObject()) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            if (obj.has("item")) {
                String itemId = obj.get("item").getAsString();
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                if (item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            } else if (obj.has("tag")) {
                String tagId = obj.get("tag").getAsString();
                ItemStack stack = new ItemStack(net.minecraft.world.item.Items.NAME_TAG);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("#" + tagId));
                return stack;
            } else if (obj.has("fluid")) {
                String fluidId = obj.get("fluid").getAsString();
                net.minecraft.world.item.Item bucket = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(fluidId + "_bucket"));
                if (bucket != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(bucket);
                }
                bucket = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(fluidId));
                if (bucket != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(bucket);
                }
            }
        } else if (el.isJsonPrimitive()) {
            String str = el.getAsString();
            if (str.startsWith("#")) {
                ItemStack stack = new ItemStack(net.minecraft.world.item.Items.NAME_TAG);
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(str));
                return stack;
            } else {
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(str));
                if (item != net.minecraft.world.item.Items.AIR) {
                    return new ItemStack(item);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void loadRecipeFromJson(java.io.File file) {
        try {
            String content = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            if (!obj.has("type")) {
                popupError = "Invalid file";
                return;
            }
            String type = obj.get("type").getAsString();
            StationType targetType = null;
            for (StationType t : tabs) {
                if (t == StationType.CRAFTING && (type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless"))) {
                    targetType = t;
                    break;
                }
                if (t == StationType.MECH_CRAFTING && type.equals("create:mechanical_crafting")) {
                    targetType = t;
                    break;
                }
                if (t == StationType.FURNACE && (type.equals("minecraft:smelting") || type.equals("minecraft:blasting") || type.equals("minecraft:smoking") || type.equals("minecraft:campfire_cooking"))) {
                    targetType = t;
                    break;
                }
                if (t == StationType.STONECUTTER && type.equals("minecraft:stonecutting")) {
                    targetType = t;
                    break;
                }
                if (t == StationType.SMITHING && type.equals("minecraft:smithing_transform")) {
                    targetType = t;
                    break;
                }
                if (t == StationType.MIXING && type.equals("create:mixing")) {
                    targetType = t;
                    break;
                }
                if (t == StationType.PRESSING && (type.equals("create:pressing") || type.equals("create:compacting"))) {
                    targetType = t;
                    break;
                }
                if (t == StationType.FAN && (type.equals("create:splashing") || type.equals("create:haunting"))) {
                    targetType = t;
                    break;
                }
                if (t == StationType.CRUSHING && (type.equals("create:crushing") || type.equals("create:milling"))) {
                    targetType = t;
                    break;
                }
            }

            if (targetType == null) {
                popupError = "Unsupported machine";
                return;
            }

            // Clear before loading
            clear();

            tabIdx = tabs.indexOf(targetType);
            fileName = file.getName();
            if (fileName.endsWith(".json")) fileName = fileName.substring(0, fileName.length() - 5);

            if (targetType == StationType.CRAFTING) {
                if (type.equals("minecraft:crafting_shapeless")) {
                    shapeless = true;
                    com.google.gson.JsonArray ingArr = obj.getAsJsonArray("ingredients");
                    if (ingArr.size() > 9) { popupError = "Invalid file"; clear(); return; }
                    for (int i = 0; i < ingArr.size(); i++) {
                        craftGrid.set(i, parseIngredient(ingArr.get(i)));
                    }
                } else {
                    shapeless = false;
                    com.google.gson.JsonObject keyObj = obj.getAsJsonObject("key");
                    java.util.Map<Character, ItemStack> keyMap = new java.util.HashMap<>();
                    for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : keyObj.entrySet()) {
                        if (!entry.getKey().isEmpty()) {
                            keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue()));
                        }
                    }
                    com.google.gson.JsonArray patternArr = obj.getAsJsonArray("pattern");
                    if (patternArr.size() > 3) { popupError = "Invalid file"; clear(); return; }
                    for (int r = 0; r < patternArr.size(); r++) {
                        String rowStr = patternArr.get(r).getAsString();
                        if (rowStr.length() > 3) { popupError = "Invalid file"; clear(); return; }
                        for (int c = 0; c < rowStr.length(); c++) {
                            char ch = rowStr.charAt(c);
                            if (ch != ' ') {
                                ItemStack item = keyMap.get(ch);
                                if (item != null) {
                                    craftGrid.set(r * 3 + c, item.copy());
                                }
                            }
                        }
                    }
                }
                com.google.gson.JsonObject resObj = obj.getAsJsonObject("result");
                craftResult = parseIngredient(resObj);
                craftCount = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
            }
            else if (targetType == StationType.MECH_CRAFTING) {
                com.google.gson.JsonObject keyObj = obj.getAsJsonObject("key");
                java.util.Map<Character, ItemStack> keyMap = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : keyObj.entrySet()) {
                    if (!entry.getKey().isEmpty()) {
                        keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue()));
                    }
                }
                com.google.gson.JsonArray patternArr = obj.getAsJsonArray("pattern");
                if (patternArr.size() > 9) { popupError = "Invalid file"; clear(); return; }
                for (int r = 0; r < patternArr.size(); r++) {
                    String rowStr = patternArr.get(r).getAsString();
                    if (rowStr.length() > 9) { popupError = "Invalid file"; clear(); return; }
                    for (int c = 0; c < rowStr.length(); c++) {
                        char ch = rowStr.charAt(c);
                        if (ch != ' ') {
                            ItemStack item = keyMap.get(ch);
                            if (item != null) {
                                mechGrid.set(r * 9 + c, item.copy());
                            }
                        }
                    }
                }
                com.google.gson.JsonObject resObj = obj.getAsJsonObject("result");
                craftResult = parseIngredient(resObj);
                craftCount = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
            }
            else if (targetType == StationType.FURNACE) {
                for (int i = 0; i < furnSubs.length; i++) {
                    if (type.equals("minecraft:" + furnSubs[i])) { furnSubIdx = i; break; }
                }
                furnIn = parseIngredient(obj.get("ingredient"));
                com.google.gson.JsonObject resObj = obj.getAsJsonObject("result");
                furnOut = parseIngredient(resObj);
                furnCount = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
                furnTime = obj.has("cookingtime") ? obj.get("cookingtime").getAsInt() : 200;
                furnXp = obj.has("experience") ? obj.get("experience").getAsFloat() : 0.1f;
            }
            else if (targetType == StationType.STONECUTTER) {
                stoneIn = parseIngredient(obj.get("ingredient"));
                com.google.gson.JsonObject resObj = obj.getAsJsonObject("result");
                stoneOut = parseIngredient(resObj);
                stoneCount = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
            }
            else if (targetType == StationType.SMITHING) {
                smTemplate = parseIngredient(obj.get("template"));
                smBase = parseIngredient(obj.get("base"));
                smAddition = parseIngredient(obj.get("addition"));
                com.google.gson.JsonObject resObj = obj.getAsJsonObject("result");
                smResult = parseIngredient(resObj);
                smCount = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
            }
            else if (targetType == StationType.MIXING) {
                com.google.gson.JsonArray ingArr = obj.getAsJsonArray("ingredients");
                int itemIdx = 0, fluidIdx = 0;
                for (com.google.gson.JsonElement el : ingArr) {
                    if (el.isJsonObject() && el.getAsJsonObject().has("fluid")) {
                        if (fluidIdx >= 4) { popupError = "Invalid file"; clear(); return; }
                        com.google.gson.JsonObject fObj = el.getAsJsonObject();
                        FluidEntry fe = mixFluidIng.get(fluidIdx++);
                        fe.proxy = parseIngredient(fObj);
                        fe.amount = fObj.get("amount").getAsInt();
                    } else {
                        if (itemIdx >= 9) { popupError = "Invalid file"; clear(); return; }
                        mixIng.set(itemIdx++, parseIngredient(el));
                    }
                }
                com.google.gson.JsonArray resArr = obj.getAsJsonArray("results");
                int itemResCount = 0, fluidResCount = 0;
                for (com.google.gson.JsonElement el : resArr) {
                    com.google.gson.JsonObject rObj = el.getAsJsonObject();
                    if (rObj.has("fluid")) {
                        if (fluidResCount >= 1) { popupError = "Invalid file"; clear(); return; }
                        mixFluidResult.proxy = parseIngredient(rObj);
                        mixFluidResult.amount = rObj.get("amount").getAsInt();
                        fluidResCount++;
                    } else {
                        if (itemResCount >= 1) { popupError = "Invalid file"; clear(); return; }
                        mixResult = parseIngredient(rObj);
                        mixCount = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                        itemResCount++;
                    }
                }
                String heat = obj.has("heatRequirement") ? obj.get("heatRequirement").getAsString() : "none";
                mixHeat = 0;
                if (heat.equalsIgnoreCase("heated")) mixHeat = 1;
                else if (heat.equalsIgnoreCase("superheated")) mixHeat = 2;
                mixTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 60;
            }
            else if (targetType == StationType.PRESSING) {
                pressBasin = type.equals("create:compacting");
                com.google.gson.JsonArray ingArr = obj.getAsJsonArray("ingredients");
                if (ingArr.size() > 0) {
                    pressIn = parseIngredient(ingArr.get(0));
                }
                com.google.gson.JsonArray resArr = obj.getAsJsonArray("results");
                if (pressBasin) {
                    int itemResCount = 0, fluidResCount = 0;
                    for (com.google.gson.JsonElement el : resArr) {
                        com.google.gson.JsonObject rObj = el.getAsJsonObject();
                        if (rObj.has("fluid")) {
                            if (fluidResCount >= 1) { popupError = "Invalid file"; clear(); return; }
                            pressFluidOut.proxy = parseIngredient(rObj);
                            pressFluidOut.amount = rObj.get("amount").getAsInt();
                            fluidResCount++;
                        } else {
                            if (itemResCount >= 1) { popupError = "Invalid file"; clear(); return; }
                            pressOut = parseIngredient(rObj);
                            pressCount = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                            itemResCount++;
                        }
                    }
                } else {
                    if (resArr.size() > 0) {
                        com.google.gson.JsonObject rObj = resArr.get(0).getAsJsonObject();
                        pressOut = parseIngredient(rObj);
                        pressCount = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                    }
                }
                pressTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 150;
            }
            else if (targetType == StationType.FAN) {
                fanHaunting = type.equals("create:haunting");
                com.google.gson.JsonArray ingArr = obj.getAsJsonArray("ingredients");
                if (ingArr.size() > 0) {
                    fanIn = parseIngredient(ingArr.get(0));
                }
                com.google.gson.JsonArray resArr = obj.getAsJsonArray("results");
                if (resArr.size() > 4) { popupError = "Invalid file"; clear(); return; }
                for (int i = 0; i < resArr.size(); i++) {
                    com.google.gson.JsonObject rObj = resArr.get(i).getAsJsonObject();
                    CrushingOutput out = fanOuts.get(i);
                    out.stack = parseIngredient(rObj);
                    out.count = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                    out.chance = rObj.has("chance") ? rObj.get("chance").getAsFloat() : 1.0f;
                }
                fanTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 200;
            }
            else if (targetType == StationType.CRUSHING) {
                isMilling = type.equals("create:milling");
                com.google.gson.JsonArray ingArr = obj.getAsJsonArray("ingredients");
                if (ingArr.size() > 0) {
                    crushIn = parseIngredient(ingArr.get(0));
                }
                com.google.gson.JsonArray resArr = obj.getAsJsonArray("results");
                if (resArr.size() > 8) { popupError = "Invalid file"; clear(); return; }
                for (int i = 0; i < resArr.size(); i++) {
                    com.google.gson.JsonObject rObj = resArr.get(i).getAsJsonObject();
                    CrushingOutput out = crushOuts.get(i);
                    out.stack = parseIngredient(rObj);
                    out.count = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                    out.chance = rObj.has("chance") ? rObj.get("chance").getAsFloat() : 1.0f;
                }
                crushTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 150;
            }

            status("Recipe loaded!", true);
        } catch (Exception e) {
            popupError = "Invalid file";
            clear();
        }
    }
}
