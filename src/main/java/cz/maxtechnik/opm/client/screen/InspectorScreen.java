package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InspectorScreen extends Screen {

    private static final int BG = 0xF0222222, HEADER_BG = 0xFF1A1A1A, BORDER = 0xFF000000;
    private static final int TEXT = 0xFFDDDDDD, LABEL = 0xFF888888;
    private static final int ICON_SZ = 32;

    private final ItemStack stack;
    private final Screen parentScreen;
    private final String itemId, modName, componentText;
    private CodeViewerWidget codeViewer;

    // Geometrie
    private int pX, pY, pW, pH, hdrH;
    private boolean hName, hMod, hId;

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

    @Override
    protected void init() {
        super.init();
        pW = Math.min(500, width - 40); pH = height - 60;
        pX = (width - pW) / 2; pY = 20;
        hdrH = ICON_SZ + 16;

        codeViewer = new CodeViewerWidget(font, componentText);
        codeViewer.addButton("Copy Give", 60, (mx, my) -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            String playerName = mc.player != null ? mc.player.getName().getString() : "@s";
            codeViewer.clip(buildGiveCommand().replace("@s", playerName), mx, my);
        });
        codeViewer.setBounds(pX, pY + hdrH + 1, pW, pH - hdrH - 1);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (codeViewer == null) return;
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

        // Header texty
        int tx = ix + ICON_SZ + 10, tw = pX + pW - tx - 8, ty = pY + 10;
        hName = drawHeaderText(g, stack.getHoverName().getString(), tx, ty, tw, mx, my, 0xFFFFFFFF, TEXT, 0xFFAAAAAA);
        ty += 14;
        hMod = drawHeaderText(g, modName, tx, ty, tw, mx, my, 0xFFCCCCCC, LABEL, 0xFF666666);
        ty += 14;
        hId = drawHeaderText(g, itemId, tx, ty, tw, mx, my, 0xFF88FF88, 0xFF55AA55, 0xFF55AA55);

        // Code viewer widget
        codeViewer.render(g, mx, my);

        super.render(g, mx, my, pt);
    }

    private boolean drawHeaderText(GuiGraphics g, String text, int x, int y, int maxW,
                                   int mx, int my, int hoverColor, int normalColor, int underline) {
        String t = truncate(text, maxW);
        boolean hover = hit(mx, my, x, y, font.width(t));
        g.drawString(font, t, x, y, hover ? hoverColor : normalColor, false);
        if (hover) g.fill(x, y + 9, x + font.width(t), y + 10, underline);
        return hover;
    }

    //INPUT ─────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (button == 0) {
            if (hName) { clip(stack.getHoverName().getString()); return true; }
            if (hMod)  { clip(modName); return true; }
            if (hId)   { clip(itemId); return true; }
        }
        if (codeViewer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (codeViewer.mouseDragged((int) my)) return true;
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) codeViewer.mouseReleased();
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return codeViewer.mouseScrolled(sy, (int) mx, (int) my);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (codeViewer.keyPressed(key, mods)) return true;
        if (key == 256) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (codeViewer.charTyped(chr)) return true;
        return super.charTyped(chr, mods);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    //HELPERS ─────────────────────────────────────────────────────────

    private void clip(String text) {
        net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    private String buildGiveCommand() {
        StringBuilder sb = new StringBuilder("/give @s ").append(itemId);
        var components = stack.getComponents();
        if (!components.isEmpty()) {
            sb.append("[");
            boolean first = true;
            for (var entry : components) {
                if (!first) sb.append(",");
                sb.append(entry.type()).append("=").append(encodeComponent(entry));
                first = false;
            }
            sb.append("]");
        }
        int count = stack.getCount();
        if (count > 1) sb.append(" ").append(count);
        return sb.toString();
    }

    private <T> String encodeComponent(net.minecraft.core.component.TypedDataComponent<T> entry) {
        var codec = entry.type().codec();
        if (codec == null) return entry.value().toString();
        var result = codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, entry.value());
        return result.result().map(Object::toString).orElse(entry.value().toString());
    }

    private boolean hit(int mx, int my, int x, int y, int w) {
        return mx >= x && mx <= x + w && my >= y && my <= y + 9;
    }

    private String truncate(String text, int maxW) {
        if (font.width(text) <= maxW) return text;
        while (font.width(text + "...") > maxW && !text.isEmpty())
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}