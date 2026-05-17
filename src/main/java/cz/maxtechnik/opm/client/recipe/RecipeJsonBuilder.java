package cz.maxtechnik.opm.client.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Generuje datapack JSON pro všechny typy receptů editoru.
 * Kompatibilní s Minecraft 1.21.1 + Create (latest).
 */
public final class RecipeJsonBuilder {

    private RecipeJsonBuilder() {}

    // ── Crafting ─────────────────────────────────────────────────────────────

    public static String buildShaped(List<ItemStack> grid, int gridW, int gridH,
                                     ItemStack result, int count) {
        char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        java.util.Map<String, Character> idToChar = new java.util.LinkedHashMap<>();
        char[][] pattern = new char[gridH][gridW];

        for (int r = 0; r < gridH; r++)
            for (int c = 0; c < gridW; c++) {
                int idx = r * gridW + c;
                ItemStack s = safeGet(grid, idx);
                if (s.isEmpty()) { pattern[r][c] = ' '; continue; }
                String id = id(s);
                idToChar.putIfAbsent(id, symbols[idToChar.size()]);
                pattern[r][c] = idToChar.get(id);
            }

        int minR = gridH, maxR = -1, minC = gridW, maxC = -1;
        for (int r = 0; r < gridH; r++)
            for (int c = 0; c < gridW; c++)
                if (pattern[r][c] != ' ') {
                    minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                    minC = Math.min(minC, c); maxC = Math.max(maxC, c);
                }
        if (maxR < 0) { minR = 0; maxR = 0; minC = 0; maxC = 0; }

        boolean isMechCrafter = (gridW > 3 || gridH > 3);
        String type = isMechCrafter ? "create:mechanical_crafting" : "minecraft:crafting_shaped";

        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"").append(type).append("\",\n");
        sb.append("  \"pattern\": [\n");
        for (int r = minR; r <= maxR; r++) {
            sb.append("    \"");
            for (int c = minC; c <= maxC; c++) sb.append(pattern[r][c]);
            sb.append("\"");
            if (r < maxR) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"key\": {\n");
        var entries = idToChar.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            sb.append("    \"").append(e.getValue()).append("\": ").append(formatIngredient(e.getKey()));
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");
        sb.append("  \"result\": { \"item\": \"").append(id(result)).append("\"");
        if (count > 1) sb.append(", \"count\": ").append(count);
        sb.append(" }\n}");
        return sb.toString();
    }

    public static String buildShapeless(List<ItemStack> ingredients, ItemStack result, int count) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"minecraft:crafting_shapeless\",\n");
        sb.append("  \"ingredients\": [\n");
        boolean first = true;
        for (ItemStack s : ingredients) {
            if (s == null || s.isEmpty()) continue;
            if (!first) sb.append(",\n");
            sb.append("    ").append(formatIngredient(id(s)));
            first = false;
        }
        sb.append("\n  ],\n");
        sb.append("  \"result\": { \"item\": \"").append(id(result)).append("\"");
        if (count > 1) sb.append(", \"count\": ").append(count);
        sb.append(" }\n}");
        return sb.toString();
    }

    // ── Furnace family ────────────────────────────────────────────────────────

    public static String buildFurnace(String subType, ItemStack input,
                                      ItemStack result, int count,
                                      int cookTime, float xp) {
        return "{\n" +
               "  \"type\": \"minecraft:" + subType + "\",\n" +
               "  \"ingredient\": " + formatIngredient(id(input)) + ",\n" +
               "  \"result\": { \"item\": \"" + id(result) + "\"" +
               (count > 1 ? ", \"count\": " + count : "") + " },\n" +
               "  \"experience\": " + String.format(java.util.Locale.ROOT, "%.1f", xp) + ",\n" +
               "  \"cookingtime\": " + cookTime + "\n}";
    }

    // ── Stonecutter ───────────────────────────────────────────────────────────

    public static String buildStonecutter(ItemStack input, ItemStack result, int count) {
        return "{\n" +
               "  \"type\": \"minecraft:stonecutting\",\n" +
               "  \"ingredient\": " + formatIngredient(id(input)) + ",\n" +
               "  \"result\": { \"item\": \"" + id(result) + "\"" +
               (count > 1 ? ", \"count\": " + count : "") + " }\n}";
    }

    // ── Smithing ──────────────────────────────────────────────────────────────

    public static String buildSmithing(ItemStack template, ItemStack base,
                                       ItemStack addition, ItemStack result, int count) {
        return "{\n" +
               "  \"type\": \"minecraft:smithing_transform\",\n" +
               "  \"template\": " + formatIngredient(id(template)) + ",\n" +
               "  \"base\":     " + formatIngredient(id(base)) + ",\n" +
               "  \"addition\": " + formatIngredient(id(addition)) + ",\n" +
               "  \"result\": { \"item\": \"" + id(result) + "\"" +
               (count > 1 ? ", \"count\": " + count : "") + " }\n}";
    }

    // ── Create Mixing ─────────────────────────────────────────────────────────

    public static String buildMixing(List<ItemStack> ingredients, ItemStack result,
                                     int count, String heat, int processingTime) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"create:mixing\",\n");
        appendIngredients(sb, ingredients);
        sb.append("  \"results\": [\n");
        sb.append("    { \"item\": \"").append(id(result)).append("\"");
        if (count > 1) sb.append(", \"count\": ").append(count);
        sb.append(" }\n  ],\n");
        if (!heat.equals("none"))
            sb.append("  \"heatRequirement\": \"").append(heat).append("\",\n");
        sb.append("  \"processingTime\": ").append(processingTime).append("\n}");
        return sb.toString();
    }

    // ── Create Pressing ───────────────────────────────────────────────────────

    public static String buildPressing(ItemStack input, ItemStack result, int count, int processingTime) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"create:pressing\",\n");
        appendIngredients(sb, List.of(input));
        sb.append("  \"results\": [\n");
        sb.append("    { \"item\": \"").append(id(result)).append("\"");
        if (count > 1) sb.append(", \"count\": ").append(count);
        sb.append(" }\n  ],\n");
        sb.append("  \"processingTime\": ").append(processingTime).append("\n}");
        return sb.toString();
    }

    public static String buildPressingBasin(ItemStack input, ItemStack result, int count, cz.maxtechnik.opm.client.screen.RecipeEditorScreen.FluidEntry fluidOut, int processingTime) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"create:compacting\",\n");
        sb.append("  \"ingredients\": [\n");
        sb.append("    ").append(formatIngredient(id(input))).append("\n");
        sb.append("  ],\n");
        sb.append("  \"results\": [\n");
        boolean hasItem = result != null && !result.isEmpty();
        if (hasItem) {
            sb.append("    { \"item\": \"").append(id(result)).append("\"");
            if (count > 1) sb.append(", \"count\": ").append(count);
            sb.append(" }");
        }
        if (fluidOut != null && !fluidOut.isEmpty()) {
            if (hasItem) sb.append(",\n");
            sb.append("    { \"fluid\": \"").append(fluidId(fluidOut.proxy)).append("\", \"amount\": ").append(fluidOut.amount).append(" }");
        }
        sb.append("\n  ],\n");
        sb.append("  \"processingTime\": ").append(processingTime).append("\n}");
        return sb.toString();
    }
    
    public static String buildMixingWithFluids(List<ItemStack> ingredients, List<cz.maxtechnik.opm.client.screen.RecipeEditorScreen.FluidEntry> fluidIngredients,
                                               ItemStack result, int count, cz.maxtechnik.opm.client.screen.RecipeEditorScreen.FluidEntry fluidResult,
                                               String heat, int processingTime) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"create:mixing\",\n");
        sb.append("  \"ingredients\": [\n");
        boolean first = true;
        for (ItemStack s : ingredients) {
            if (s == null || s.isEmpty()) continue;
            if (!first) sb.append(",\n");
            sb.append("    ").append(formatIngredient(id(s)));
            first = false;
        }
        for (cz.maxtechnik.opm.client.screen.RecipeEditorScreen.FluidEntry f : fluidIngredients) {
            if (f == null || f.isEmpty()) continue;
            if (!first) sb.append(",\n");
            sb.append("    { \"fluid\": \"").append(fluidId(f.proxy)).append("\", \"amount\": ").append(f.amount).append(" }");
            first = false;
        }
        sb.append("\n  ],\n");
        sb.append("  \"results\": [\n");
        first = true;
        if (result != null && !result.isEmpty()) {
            sb.append("    { \"item\": \"").append(id(result)).append("\"");
            if (count > 1) sb.append(", \"count\": ").append(count);
            sb.append(" }");
            first = false;
        }
        if (fluidResult != null && !fluidResult.isEmpty()) {
            if (!first) sb.append(",\n");
            sb.append("    { \"fluid\": \"").append(fluidId(fluidResult.proxy)).append("\", \"amount\": ").append(fluidResult.amount).append(" }");
        }
        sb.append("\n  ],\n");
        if (!heat.equals("none"))
            sb.append("  \"heatRequirement\": \"").append(heat).append("\",\n");
        sb.append("  \"processingTime\": ").append(processingTime).append("\n}");
        return sb.toString();
    }

    // ── Create Crushing / Milling ─────────────────────────────────────────────

    public static String buildCrushing(String createType, ItemStack input,
                                       List<CrushingOutput> outputs, int processingTime) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"").append(createType).append("\",\n");
        appendIngredients(sb, List.of(input));
        sb.append("  \"results\": [\n");
        boolean first = true;
        for (CrushingOutput o : outputs) {
            if (o.isEmpty()) continue;
            if (!first) sb.append(",\n");
            sb.append("    { \"item\": \"").append(id(o.stack)).append("\"");
            if (o.count > 1)     sb.append(", \"count\": ").append(o.count);
            if (o.chance < 1.0f) sb.append(", \"chance\": ").append(String.format(java.util.Locale.ROOT, "%.2f", o.chance));
            sb.append(" }");
            first = false;
        }
        sb.append("\n  ],\n");
        sb.append("  \"processingTime\": ").append(processingTime).append("\n}");
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void appendIngredients(StringBuilder sb, List<ItemStack> ingredients) {
        sb.append("  \"ingredients\": [\n");
        boolean first = true;
        for (ItemStack s : ingredients) {
            if (s == null || s.isEmpty()) continue;
            if (!first) sb.append(",\n");
            sb.append("    ").append(formatIngredient(id(s)));
            first = false;
        }
        sb.append("\n  ],\n");
    }

    public static String formatIngredient(String id) {
        if (id.startsWith("#")) {
            return "{ \"tag\": \"" + id.substring(1) + "\" }";
        }
        return "{ \"item\": \"" + id + "\" }";
    }

    public static String id(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "minecraft:air";
        if (stack.getItem() == net.minecraft.world.item.Items.NAME_TAG && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            String name = stack.getHoverName().getString();
            if (name.startsWith("#")) return name;
        }
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return loc != null ? loc.toString() : "minecraft:air";
    }

    public static String fluidId(ItemStack stack) {
        String baseId = id(stack);
        if (baseId.endsWith("_bucket")) {
            return baseId.substring(0, baseId.length() - "_bucket".length());
        }
        return baseId;
    }

    private static ItemStack safeGet(List<ItemStack> list, int idx) {
        return (idx >= 0 && idx < list.size() && list.get(idx) != null)
                ? list.get(idx) : ItemStack.EMPTY;
    }
}