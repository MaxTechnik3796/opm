package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RecipeEditorData {

    // ── Crafting ─────────────────────────────────────────────────────────────
    public boolean shapeless = false;
    public final List<ItemStack> craftGrid = initList(9);
    public final List<ItemStack> mechGrid  = initList(81);
    public ItemStack craftResult = ItemStack.EMPTY;
    public int craftCount = 1;

    // ── Furnace ───────────────────────────────────────────────────────────────
    public int furnSubIdx = 0;
    public final String[] furnSubs   = {"smelting","blasting","smoking","campfire_cooking"};
    public final String[] furnLabels = {"Furnace","Blast Furnace","Smoker","Campfire"};
    public ItemStack furnIn = ItemStack.EMPTY, furnOut = ItemStack.EMPTY;
    public int furnCount = 1, furnTime = 200;
    public float furnXp = 0.1f;

    // ── Stonecutter ───────────────────────────────────────────────────────────
    public ItemStack stoneIn = ItemStack.EMPTY, stoneOut = ItemStack.EMPTY;
    public int stoneCount = 1;

    // ── Smithing ──────────────────────────────────────────────────────────────
    public ItemStack smTemplate = ItemStack.EMPTY, smBase = ItemStack.EMPTY;
    public ItemStack smAddition = ItemStack.EMPTY, smResult = ItemStack.EMPTY;
    public int smCount = 1;

    // ── Mixing ────────────────────────────────────────────────────────────────
    public final List<ItemStack> mixIng = initList(9);
    public final List<FluidEntry> mixFluidIng = initFluidList(4);
    public ItemStack mixResult = ItemStack.EMPTY;
    public FluidEntry mixFluidResult = new FluidEntry();
    public int mixCount = 1, mixTime = 60, mixHeat = 0;
    public final String[] heatLabels = {"None","Heated","Superheated"};

    // ── Pressing ──────────────────────────────────────────────────────────────
    public boolean pressBasin = false;
    public ItemStack pressIn = ItemStack.EMPTY, pressOut = ItemStack.EMPTY;
    public FluidEntry pressFluidOut = new FluidEntry();
    public int pressCount = 1, pressTime = 150;

    // ── Crushing / Milling ────────────────────────────────────────────────────
    public boolean isMilling = false;
    public ItemStack crushIn = ItemStack.EMPTY;
    public final List<CrushingOutput> crushOuts = new ArrayList<>();
    public int crushTime = 150;

    // ── Fan ───────────────────────────────────────────────────────────────────
    public boolean fanHaunting = false;
    public ItemStack fanIn = ItemStack.EMPTY;
    public final List<CrushingOutput> fanOuts = new ArrayList<>();
    public int fanTime = 200;

    // ── Bottom panel data ─────────────────────────────────────────────────────
    public final List<ItemStack> availableFluids = new ArrayList<>();
    public final List<ItemStack> allItems = new ArrayList<>();
    public final List<ItemStack> cachedFilteredItems = new ArrayList<>();
    public final List<ItemStack> cachedTags = new ArrayList<>();
    public final List<ItemStack> favorites = new ArrayList<>();
    public final List<File> savedRecipeFiles = new ArrayList<>();
    public File selectedRecipeFile = null;

    public String statusMsg = "";
    public long statusUntil;
    public boolean statusOk;

    // ── Popup ─────────────────────────────────────────────────────────────────
    public String popupError = null;

    // ─────────────────────────────────────────────────────────────────────────

    public RecipeEditorData() {
        for (int i = 0; i < 8; i++) crushOuts.add(new CrushingOutput());
        for (int i = 0; i < 4; i++) fanOuts.add(new CrushingOutput());
    }

    // ── JSON builder ──────────────────────────────────────────────────────────

    public String buildJson(List<StationType> tabs, int tabIdx) {
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
                            ? RecipeJsonBuilder.buildMixingWithFluids(mixIng, mixFluidIng, mixResult, mixCount, mixFluidResult, heatLabels[mixHeat].toLowerCase(Locale.ROOT), mixTime)
                            : RecipeJsonBuilder.buildMixing(mixIng, mixResult, mixCount, heatLabels[mixHeat].toLowerCase(Locale.ROOT), mixTime);
                }
                case PRESSING -> pressBasin
                        ? RecipeJsonBuilder.buildPressingBasin(pressIn, pressOut, pressCount, pressFluidOut, pressTime)
                        : RecipeJsonBuilder.buildPressing(pressIn, pressOut, pressCount, pressTime);
                case FAN ->
                        RecipeJsonBuilder.buildCrushing(fanHaunting ? "create:haunting" : "create:splashing", fanIn, fanOuts, fanTime);
                case CRUSHING ->
                        RecipeJsonBuilder.buildCrushing(isMilling ? "create:milling" : "create:crushing", crushIn, crushOuts, crushTime);
            };
        } catch (Exception e) { return "// Error: " + e.getMessage(); }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    public void clear() {
        Collections.fill(craftGrid, ItemStack.EMPTY);
        Collections.fill(mechGrid, ItemStack.EMPTY);
        Collections.fill(mixIng, ItemStack.EMPTY);
        mixFluidIng.forEach(f -> f.proxy = ItemStack.EMPTY);
        crushOuts.forEach(o -> { o.stack = ItemStack.EMPTY; o.chance = 1f; o.count = 1; });
        fanOuts.forEach(o -> { o.stack = ItemStack.EMPTY; o.chance = 1f; o.count = 1; });
        craftResult = furnIn = furnOut = stoneIn = stoneOut =
                smTemplate = smBase = smAddition = smResult =
                mixResult = pressIn = pressOut = crushIn = fanIn = ItemStack.EMPTY;
        mixFluidResult.proxy = ItemStack.EMPTY;
        pressFluidOut.proxy  = ItemStack.EMPTY;
        craftCount = furnCount = stoneCount = smCount = mixCount = pressCount = 1;
        status("Cleared.", true);
    }

    public void status(String m, boolean ok) {
        statusMsg = m; statusOk = ok; statusUntil = System.currentTimeMillis() + 3000;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    public void loadFluids() {
        availableFluids.clear();
        availableFluids.add(new ItemStack(Items.WATER_BUCKET));
        availableFluids.add(new ItemStack(Items.LAVA_BUCKET));
        availableFluids.add(new ItemStack(Items.MILK_BUCKET));
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            tryAddBucket("create:honey_bucket");
            tryAddBucket("create:chocolate_bucket");
        }
        // Přidej všechny buckety z registru
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack s = new ItemStack(item);
            if (!s.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (id.endsWith("_bucket") && !id.equals("minecraft:bucket")
                        && availableFluids.stream().noneMatch(f -> ItemStack.isSameItem(f, s))) {
                    availableFluids.add(s);
                }
            }
        }
    }

    private void tryAddBucket(String id) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item != Items.AIR) availableFluids.add(new ItemStack(item));
        } catch (Exception ignored) {}
    }

    public void loadAllItems() {
        allItems.clear();
        for (Item item : BuiltInRegistries.ITEM) allItems.add(new ItemStack(item));
    }

    public void loadTags() {
        cachedTags.clear();
        BuiltInRegistries.ITEM.getTags()
                .map(com.mojang.datafixers.util.Pair::getFirst)
                .forEach(tagKey -> {
                    ItemStack stack = new ItemStack(Items.NAME_TAG);
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                            net.minecraft.network.chat.Component.literal("#" + tagKey.location()));
                    cachedTags.add(stack);
                });
    }

    public void loadFavorites(net.minecraft.client.Minecraft mc) {
        favorites.clear();
        if (mc == null) return;
        File f = new File(mc.gameDirectory, "config/opm_favorites.txt");
        if (!f.exists()) return;
        try {
            for (String s : Files.readAllLines(f.toPath())) {
                ResourceLocation loc = ResourceLocation.tryParse(s);
                if (loc != null) BuiltInRegistries.ITEM.getOptional(loc)
                        .ifPresent(item -> favorites.add(new ItemStack(item)));
            }
        } catch (Exception ignored) {}
    }

    public void saveFavorites(net.minecraft.client.Minecraft mc) {
        if (mc == null) return;
        File f = new File(mc.gameDirectory, "config/opm_favorites.txt");
        try {
            java.nio.file.Files.createDirectories(f.getParentFile().toPath());
            List<String> lines = new ArrayList<>();
            for (ItemStack s : favorites)
                if (!s.isEmpty()) lines.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            Files.write(f.toPath(), lines);
        } catch (Exception ignored) {}
    }

    public void scanSavedRecipes() {
        savedRecipeFiles.clear();
        try {
            java.nio.file.Path dir = RecipeFileWriter.getRecipeDir();
            if (!Files.exists(dir)) return;
            try (var stream = Files.walk(dir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> savedRecipeFiles.add(p.toFile()));
            }
            savedRecipeFiles.sort(java.util.Comparator.comparing(File::getName));
        } catch (Exception ignored) {}
    }

    public void loadConfig(net.minecraft.client.Minecraft mc, java.util.function.IntConsumer setter) {
        if (mc == null) return;
        File f = new File(mc.gameDirectory, "config/opm_editor.txt");
        if (!f.exists()) return;
        try {
            List<String> lines = Files.readAllLines(f.toPath());
            if (!lines.isEmpty()) setter.accept(Integer.parseInt(lines.getFirst()));
        } catch (Exception ignored) {}
    }

    public void saveConfig(net.minecraft.client.Minecraft mc, int invPanelHeight) {
        if (mc == null) return;
        File f = new File(mc.gameDirectory, "config/opm_editor.txt");
        try {
            java.nio.file.Files.createDirectories(f.getParentFile().toPath());
            Files.writeString(f.toPath(), String.valueOf(invPanelHeight));
        } catch (Exception ignored) {}
    }

    // ── Recipe file loading ───────────────────────────────────────────────────

    public String loadRecipeFile(File file, List<StationType> tabs) {
        try {
            String json = Files.readString(file.toPath());
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            String type = obj.get("type").getAsString();
            StationType targetType = detectType(type);
            if (targetType == null) return "Unknown recipe type";

            // Přepni na správnou záložku
            for (StationType tab : tabs) {
                if (tab == targetType) { /* caller nastaví tabIdx */
                    break;
                }
            }

            clear();
            parseIntoData(obj, type, targetType);
            return null; // success
        } catch (Exception e) {
            return "Invalid file";
        }
    }

    private StationType detectType(String type) {
        return switch (type) {
            case "minecraft:crafting_shaped", "minecraft:crafting_shapeless" -> StationType.CRAFTING;
            case "minecraft:smelting", "minecraft:blasting",
                 "minecraft:smoking", "minecraft:campfire_cooking" -> StationType.FURNACE;
            case "minecraft:stonecutting"       -> StationType.STONECUTTER;
            case "minecraft:smithing_transform" -> StationType.SMITHING;
            case "create:mechanical_crafting"   -> StationType.MECH_CRAFTING;
            case "create:mixing"                -> StationType.MIXING;
            case "create:pressing", "create:compacting" -> StationType.PRESSING;
            case "create:crushing", "create:milling"    -> StationType.CRUSHING;
            case "create:splashing", "create:haunting"  -> StationType.FAN;
            default -> null;
        };
    }

    private void parseIntoData(com.google.gson.JsonObject obj, String type, StationType t) {
        switch (t) {
            case CRAFTING -> {
                shapeless = type.equals("minecraft:crafting_shapeless");
                if (!shapeless) {
                    // Shaped
                    var patternArr = obj.getAsJsonArray("pattern");
                    var keyObj = obj.getAsJsonObject("key");
                    java.util.Map<Character, ItemStack> keyMap = new java.util.HashMap<>();
                    for (var entry : keyObj.entrySet())
                        keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue()));
                    for (int r = 0; r < patternArr.size() && r < 3; r++) {
                        String row = patternArr.get(r).getAsString();
                        for (int c = 0; c < row.length() && c < 3; c++) {
                            char ch = row.charAt(c);
                            if (ch != ' ' && keyMap.containsKey(ch)) craftGrid.set(r * 3 + c, keyMap.get(ch).copy());
                        }
                    }
                } else {
                    // Shapeless
                    var ingArr = obj.getAsJsonArray("ingredients");
                    for (int i = 0; i < ingArr.size() && i < 9; i++)
                        craftGrid.set(i, parseIngredient(ingArr.get(i)));
                }
                var res = obj.getAsJsonObject("result");
                craftResult = parseIngredient(res);
                craftCount = res.has("count") ? res.get("count").getAsInt() : 1;
            }
            case MECH_CRAFTING -> {
                var patternArr = obj.getAsJsonArray("pattern");
                var keyObj = obj.getAsJsonObject("key");
                java.util.Map<Character, ItemStack> keyMap = new java.util.HashMap<>();
                for (var entry : keyObj.entrySet())
                    keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue()));
                for (int r = 0; r < patternArr.size() && r < 9; r++) {
                    String row = patternArr.get(r).getAsString();
                    for (int c = 0; c < row.length() && c < 9; c++) {
                        char ch = row.charAt(c);
                        if (ch != ' ' && keyMap.containsKey(ch)) mechGrid.set(r * 9 + c, keyMap.get(ch).copy());
                    }
                }
                var res = obj.getAsJsonObject("result");
                craftResult = parseIngredient(res);
                craftCount = res.has("count") ? res.get("count").getAsInt() : 1;
            }
            case FURNACE -> {
                for (int i = 0; i < furnSubs.length; i++)
                    if (type.equals("minecraft:" + furnSubs[i])) { furnSubIdx = i; break; }
                furnIn = parseIngredient(obj.get("ingredient"));
                var res = obj.getAsJsonObject("result");
                furnOut = parseIngredient(res);
                furnCount = res.has("count") ? res.get("count").getAsInt() : 1;
                furnTime  = obj.has("cookingtime") ? obj.get("cookingtime").getAsInt() : 200;
                furnXp    = obj.has("experience")  ? obj.get("experience").getAsFloat()  : 0.1f;
            }
            case STONECUTTER -> {
                stoneIn = parseIngredient(obj.get("ingredient"));
                var res = obj.getAsJsonObject("result");
                stoneOut   = parseIngredient(res);
                stoneCount = res.has("count") ? res.get("count").getAsInt() : 1;
            }
            case SMITHING -> {
                smTemplate = parseIngredient(obj.get("template"));
                smBase     = parseIngredient(obj.get("base"));
                smAddition = parseIngredient(obj.get("addition"));
                var res = obj.getAsJsonObject("result");
                smResult = parseIngredient(res);
                smCount  = res.has("count") ? res.get("count").getAsInt() : 1;
            }
            case MIXING -> {
                var ingArr = obj.getAsJsonArray("ingredients");
                int itemIdx = 0, fluidIdx = 0;
                for (var el : ingArr) {
                    if (el.isJsonObject() && el.getAsJsonObject().has("fluid")) {
                        if (fluidIdx < 4) {
                            var fObj = el.getAsJsonObject();
                            FluidEntry fe = mixFluidIng.get(fluidIdx++);
                            fe.proxy  = parseIngredient(fObj);
                            fe.amount = fObj.has("amount") ? fObj.get("amount").getAsInt() : 1000;
                        }
                    } else if (itemIdx < 9) {
                        mixIng.set(itemIdx++, parseIngredient(el));
                    }
                }
                var resArr = obj.getAsJsonArray("results");
                for (var el : resArr) {
                    var rObj = el.getAsJsonObject();
                    if (rObj.has("fluid")) {
                        mixFluidResult.proxy  = parseIngredient(rObj);
                        mixFluidResult.amount = rObj.has("amount") ? rObj.get("amount").getAsInt() : 1000;
                    } else {
                        mixResult = parseIngredient(rObj);
                        mixCount  = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                    }
                }
                String heat = obj.has("heatRequirement") ? obj.get("heatRequirement").getAsString() : "none";
                mixHeat = heat.equalsIgnoreCase("superheated") ? 2 : heat.equalsIgnoreCase("heated") ? 1 : 0;
                mixTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 60;
            }
            case PRESSING -> {
                pressBasin = type.equals("create:compacting");
                var ingArr = obj.getAsJsonArray("ingredients");
                if (!ingArr.isEmpty()) pressIn = parseIngredient(ingArr.get(0));
                var resArr = obj.getAsJsonArray("results");
                for (var el : resArr) {
                    var rObj = el.getAsJsonObject();
                    if (rObj.has("fluid")) {
                        pressFluidOut.proxy  = parseIngredient(rObj);
                        pressFluidOut.amount = rObj.has("amount") ? rObj.get("amount").getAsInt() : 1000;
                    } else {
                        pressOut   = parseIngredient(rObj);
                        pressCount = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
                    }
                }
                pressTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 150;
            }
            case CRUSHING -> {
                isMilling = type.equals("create:milling");
                var ingArr = obj.getAsJsonArray("ingredients");
                if (!ingArr.isEmpty()) crushIn = parseIngredient(ingArr.get(0));
                var resArr = obj.getAsJsonArray("results");
                for (int i = 0; i < resArr.size() && i < 8; i++) {
                    var rObj = resArr.get(i).getAsJsonObject();
                    crushOuts.get(i).stack  = parseIngredient(rObj);
                    crushOuts.get(i).count  = rObj.has("count")  ? rObj.get("count").getAsInt()   : 1;
                    crushOuts.get(i).chance = rObj.has("chance") ? rObj.get("chance").getAsFloat() : 1f;
                }
                crushTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 150;
            }
            case FAN -> {
                fanHaunting = type.equals("create:haunting");
                var ingArr = obj.getAsJsonArray("ingredients");
                if (!ingArr.isEmpty()) fanIn = parseIngredient(ingArr.get(0));
                var resArr = obj.getAsJsonArray("results");
                for (int i = 0; i < resArr.size() && i < 4; i++) {
                    var rObj = resArr.get(i).getAsJsonObject();
                    fanOuts.get(i).stack  = parseIngredient(rObj);
                    fanOuts.get(i).count  = rObj.has("count")  ? rObj.get("count").getAsInt()   : 1;
                    fanOuts.get(i).chance = rObj.has("chance") ? rObj.get("chance").getAsFloat() : 1f;
                }
                fanTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 200;
            }
        }
    }

    // ── Ingredient parser ─────────────────────────────────────────────────────

    public ItemStack parseIngredient(com.google.gson.JsonElement el) {
        if (el == null || el.isJsonNull()) return ItemStack.EMPTY;
        com.google.gson.JsonObject obj = null;
        if (el.isJsonObject()) obj = el.getAsJsonObject();
        else if (el.isJsonArray()) {
            var arr = el.getAsJsonArray();
            if (!arr.isEmpty() && arr.get(0).isJsonObject()) obj = arr.get(0).getAsJsonObject();
        }
        if (obj == null) return ItemStack.EMPTY;

        // Tag ingredient → use NAME_TAG proxy with "#" prefix
        if (obj.has("tag")) {
            String tag = obj.get("tag").getAsString();
            ItemStack proxy = new ItemStack(Items.NAME_TAG);
            proxy.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal("#" + tag));
            return proxy;
        }
        // Fluid ingredient → find matching bucket
        if (obj.has("fluid")) {
            String fluidId = obj.get("fluid").getAsString();
            String bucketId = fluidId + "_bucket";
            var opt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(bucketId));
            return opt.map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        // Regular item
        String id = obj.has("item") ? obj.get("item").getAsString()
                  : obj.has("id")   ? obj.get("id").getAsString()
                  : null;
        if (id == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id))
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static List<ItemStack> initList(int n) {
        List<ItemStack> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(ItemStack.EMPTY);
        return l;
    }

    public static List<FluidEntry> initFluidList(int n) {
        List<FluidEntry> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(new FluidEntry());
        return l;
    }
}