package cz.maxtechnik.opm.client.screen;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDataBuilder {

    private final ItemStack stack;

    public ItemDataBuilder(ItemStack stack) {
        this.stack = stack;
    }

    // ─── PUBLIC API ─────────────────────────────────────────────────────────────

    /** Full mód – všechny komponenty, správné SNBT. */
    public String buildFullText() {
        DataComponentMap comps = stack.getComponents();
        if (comps.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        comps.forEach(c -> sb.append("  ")
                .append(registryName(c.type()))
                .append(" = ")
                .append(formatSnbt(encodeComponent(c)))
                .append(",\n"));
        if (sb.length() > 2) { sb.setLength(sb.length() - 2); sb.append("\n"); }
        return sb.append("]").toString();
    }

    /** Simple mód – pouze diff oproti výchozímu stacku. */
    public String buildSimpleText() {
        List<String> parts = buildDiffParts(stack, true);
        if (parts.isEmpty()) return "[]";
        return formatSnbt("[" + String.join(",", parts) + "]");
    }

    /**
     * /give příkaz.
     * @param playerName cíl
     * @param simpleMode true = pouze diff komponenty
     */
    public String buildGiveCommand(String playerName, boolean simpleMode) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StringBuilder sb = new StringBuilder("/give ")
                .append(playerName).append(" ").append(loc);

        List<String> parts = simpleMode
                ? buildDiffParts(stack, false)
                : buildAllParts(stack, false);

        if (!parts.isEmpty()) {
            sb.append("[").append(String.join(",", parts)).append("]");
        }
        int count = stack.getCount();
        if (count > 1) sb.append(" ").append(count);
        return sb.toString();
    }

    // ─── PART BUILDERS ──────────────────────────────────────────────────────────

    /** Vrátí list "type=snbt" pro VŠECHNY komponenty. */
    private List<String> buildAllParts(ItemStack item, boolean prettyTypes) {
        List<String> parts = new ArrayList<>();
        item.getComponents().forEach(c ->
                parts.add(componentEntry(c, prettyTypes)));
        return parts;
    }

    /** Vrátí list "type=snbt" pouze pro komponenty odlišné od výchozích. */
    private List<String> buildDiffParts(ItemStack item, boolean prettyTypes) {
        ItemStack def = new ItemStack(item.getItem());
        DataComponentMap defComps = def.getComponents();
        List<String> parts = new ArrayList<>();
        item.getComponents().forEach(c -> {
            @SuppressWarnings("unchecked")
            DataComponentType<Object> type = (DataComponentType<Object>) c.type();
            Object defaultVal = defComps.get(type);
            if (defaultVal != null && defaultVal.equals(c.value())) return;
            parts.add(componentEntry(c, prettyTypes));
        });
        return parts;
    }

    /** Sestaví jeden záznam "minecraft:foo=<snbt>". */
    private String componentEntry(TypedDataComponent<?> c, boolean pretty) {
        String name = registryName(c.type());
        String snbt = encodeComponent(c);
        return name + "=" + snbt;
    }

    // ─── COMPONENT ENCODING ─────────────────────────────────────────────────────

    /**
     * Zakóduje hodnotu komponenty do platného SNBT pomocí jejího codec.
     * Pro CONTAINER použije speciální ruční serializer.
     */
    public String encodeComponent(TypedDataComponent<?> c) {
        if (c.type() == DataComponents.CONTAINER) {
            return encodeContainer((TypedDataComponent<ItemContainerContents>) c);
        }
        Optional<Tag> tag = encodeWithCodec(c.type(), c.value());
        if (tag.isPresent()) {
            return tag.get().toString();
        }
        return "\"" + c.value().toString().replace("\"", "\\\"") + "\"";
    }

    /** Rozbalí ItemContainerContents na správné SNBT pole slotů. */
    private String encodeContainer(TypedDataComponent<ItemContainerContents> c) {
        try {
            ItemContainerContents contents = c.value();
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

                List<String> subParts = buildDiffParts(item, false);
                if (!subParts.isEmpty()) {
                    sb.append(",components:{")
                            .append(String.join(",", subParts))
                            .append("}");
                }
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return encodeWithCodec(c.type(), c.value())
                    .map(Tag::toString)
                    .orElse("[]");
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────

    /** Vrátí registry name komponenty (např. "minecraft:enchantments"). */
    private String registryName(DataComponentType<?> type) {
        ResourceLocation key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return key != null ? key.toString() : type.toString();
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
    @SuppressWarnings("unchecked")
    private <T> Optional<Tag> encodeWithCodec(DataComponentType<T> type, Object value) {
        var codec = type.codec();
        if (codec == null) return Optional.empty();
        return codec.encodeStart(NbtOps.INSTANCE, (T) value).result();
    }
}