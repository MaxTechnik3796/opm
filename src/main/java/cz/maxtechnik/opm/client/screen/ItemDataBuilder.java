package cz.maxtechnik.opm.client.screen;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemDataBuilder {

    private final ItemStack stack;

    public ItemDataBuilder(ItemStack stack) {
        this.stack = stack;
    }

    // ─── PUBLIC API ─────────────────────────────────────────────────────────────

    /** Vrátí naformátovaný text pro full mód. */
    public String buildFullText() {
        DataComponentMap comps = stack.getComponents();
        if (comps.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        comps.forEach(c -> sb.append("  ").append(c.type()).append(" = ")
                .append(formatSnbt(rawValue(c))).append(",\n"));
        if (sb.length() > 2) { sb.setLength(sb.length() - 2); sb.append("\n"); }
        return sb.append("]").toString();
    }

    /**
     * Vrátí naformátovaný text pro simple mód (styl F3+I).
     * Zobrazuje POUZE komponenty odlišné od výchozího (čistého) stacku.
     */
    public String buildSimpleText() {
        ItemStack        def      = new ItemStack(stack.getItem());
        DataComponentMap defComps = def.getComponents();
        DataComponentMap comps    = stack.getComponents();

        List<String> parts = new ArrayList<>();
        comps.forEach(c -> {
            @SuppressWarnings("unchecked")
            DataComponentType<Object> type = (DataComponentType<Object>) c.type();
            Object actualVal  = c.value();
            Object defaultVal = defComps.get(type);
            if (defaultVal != null && actualVal.equals(defaultVal)) return;
            parts.add(type + "=" + encodeForCommand(c));
        });

        if (parts.isEmpty()) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.size(); i++) {
            sb.append(parts.get(i));
            if (i < parts.size() - 1) sb.append(",\n");
        }
        sb.append("]");
        return formatSnbt(sb.toString());
    }

    /**
     * Sestaví /give příkaz pro aktuální stack.
     *
     * @param playerName jméno hráče (nahradí "@s")
     * @param simpleMode pokud true, zahrne pouze diff komponenty
     */
    public String buildGiveCommand(String playerName, boolean simpleMode) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StringBuilder sb = new StringBuilder("/give ")
                .append(playerName).append(" ").append(loc);

        List<String> parts = new ArrayList<>();
        if (simpleMode) {
            ItemStack        def      = new ItemStack(stack.getItem());
            DataComponentMap defComps = def.getComponents();
            stack.getComponents().forEach(entry -> {
                @SuppressWarnings("unchecked")
                DataComponentType<Object> type = (DataComponentType<Object>) entry.type();
                Object actualVal  = entry.value();
                Object defaultVal = defComps.get(type);
                if (defaultVal != null && actualVal.equals(defaultVal)) return;
                parts.add(entry.type() + "=" + encodeForCommand(entry));
            });
        } else {
            stack.getComponents().forEach(entry ->
                    parts.add(entry.type() + "=" + encodeForCommand(entry)));
        }

        if (!parts.isEmpty()) {
            sb.append("[").append(String.join(",", parts)).append("]");
        }
        int count = stack.getCount();
        if (count > 1) sb.append(" ").append(count);
        return sb.toString();
    }

    // ─── COMPONENT ENCODING ─────────────────────────────────────────────────────

    public String encodeForCommand(TypedDataComponent<?> c) {
        if (c.type() == net.minecraft.core.component.DataComponents.CONTAINER) {
            return encodeContainer(c);
        }
        var codec = c.type().codec();
        if (codec != null) {
            @SuppressWarnings("unchecked")
            var result = ((com.mojang.serialization.Codec<Object>) codec)
                    .encodeStart(NbtOps.INSTANCE, c.value());
            var opt = result.result();
            if (opt.isPresent()) return opt.get().toString();
        }
        return c.value().toString();
    }

    /** Rozbalí ItemContainerContents na čitelný seznam slotů (jako F3+I). */
    @SuppressWarnings("unchecked")
    private String encodeContainer(TypedDataComponent<?> c) {
        try {
            var contents = (net.minecraft.world.item.component.ItemContainerContents) c.value();
            NonNullList<ItemStack> items =
                    NonNullList.withSize(contents.getSlots(), ItemStack.EMPTY);
            contents.copyInto(items);

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack item = items.get(slot);
                if (item.isEmpty()) continue;
                if (!first) sb.append(",");
                first = false;

                ResourceLocation itemLoc = BuiltInRegistries.ITEM.getKey(item.getItem());
                sb.append("{Slot:").append(slot).append("b")
                        .append(",id:\"").append(itemLoc).append("\"")
                        .append(",count:").append(item.getCount());

                String subComps = buildSubComponents(item);
                if (!subComps.isEmpty()) {
                    sb.append(",components:{").append(subComps).append("}");
                }
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return c.value().toString();
        }
    }

    /** Diff komponent vnořeného itemu – pouze to co se liší od výchozího. */
    private String buildSubComponents(ItemStack item) {
        ItemStack        def      = new ItemStack(item.getItem());
        DataComponentMap defComps = def.getComponents();
        DataComponentMap comps    = item.getComponents();
        List<String> parts = new ArrayList<>();
        comps.forEach(c -> {
            @SuppressWarnings("unchecked")
            DataComponentType<Object> type = (DataComponentType<Object>) c.type();
            Object val = c.value();
            Object dv  = defComps.get(type);
            if (dv != null && val.equals(dv)) return;
            parts.add("\"" + type + "\":" + encodeForCommand(c));
        });
        return String.join(",", parts);
    }

    /** rawValue pro Full mód – container rozbalí, ostatní toString. */
    private String rawValue(TypedDataComponent<?> c) {
        if (c.type() == net.minecraft.core.component.DataComponents.CONTAINER) {
            return encodeContainer(c);
        }
        return c.value().toString();
    }

    // ─── SNBT FORMATTER ─────────────────────────────────────────────────────────

    public String formatSnbt(String raw) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
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
                    indent++;
                    sb.append(c).append("\n").append("  ".repeat(indent));
                }
            } else if (c == '}' || c == ']') {
                depth = Math.max(0, depth - 1);
                char open = c == '}' ? '{' : '[';
                if (i > 0 && raw.charAt(i - 1) == open) {
                    sb.append(c);
                } else {
                    indent = Math.max(0, indent - 1);
                    sb.append("\n").append("  ".repeat(indent)).append(c);
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
            } else if (c == '\n') {
                sb.append('\n');
                while (i + 1 < raw.length() && (raw.charAt(i + 1) == ' ' || raw.charAt(i + 1) == '\t')) i++;
                sb.append("  ".repeat(indent));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}