package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

public class InspectorScreen extends Screen {
	// Trvalý stav (pamatuje si zvolenou mód mezi otevřeními)
	private static final String PREFS_FILE = "opm_inspector.properties";
	private static final String KEY_SIMPLE  = "simpleMode";
	private static boolean globalSimpleMode = loadSimpleMode();

	private static boolean loadSimpleMode() {
		try {
			File file = new File(Minecraft.getInstance().gameDirectory, PREFS_FILE);
			if (!file.exists()) return false;
			Properties props = new Properties();
			try (FileReader reader = new FileReader(file)) { props.load(reader); }
			return "true".equalsIgnoreCase(props.getProperty(KEY_SIMPLE, "false"));
		} catch (Exception e) {
			return false;
		}
	}

	private static void saveSimpleMode(boolean value) {
		try {
			File file = new File(Minecraft.getInstance().gameDirectory, PREFS_FILE);
			Properties props = new Properties();
			if (file.exists()) {
				try (FileReader reader = new FileReader(file)) { props.load(reader); }
			}
			props.setProperty(KEY_SIMPLE, Boolean.toString(value));
			try (FileWriter writer = new FileWriter(file)) { props.store(writer, "OPM Inspector preferences"); }
		} catch (Exception ignored) {}
	}

	// Barvy
	private static final int BG        = 0xF0222222;
	private static final int HEADER_BG = 0xFF1A1A1A;
	private static final int BORDER    = 0xFF000000;
	private static final int TEXT      = 0xFFDDDDDD;
	private static final int LABEL     = 0xFF888888;
	private static final int ICON_SIZE = 32;

	// Pole
	private final ItemStack stack;
	private final Screen parentScreen;
	private final String itemId, modName;
	private final ItemDataBuilder builder;
	private CodeViewerWidget codeViewer;
	private boolean simpleMode = globalSimpleMode;

	private int panelX, panelY, panelW, panelH, headerH;
	private boolean hoverName, hoverMod, hoverId;

	public InspectorScreen(ItemStack stack, Screen parentScreen) {
		super(Component.literal("Item Inspector"));
		this.stack = stack;
		this.parentScreen = parentScreen;
		this.builder = new ItemDataBuilder(stack);
		ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
		this.itemId = loc.toString();
		String namespace = loc.getNamespace(), displayName = namespace;
		try {
			var container = net.neoforged.fml.ModList.get().getModContainerById(namespace);
			if (container.isPresent()) displayName = container.get().getModInfo().getDisplayName();
		} catch (Exception ignored) {}
		this.modName = displayName;
	}

	@Override
	protected void init() {
		super.init();
		panelW = Math.min(500, width - 40);
		panelH = height - 60;
		panelX = (width - panelW) / 2;
		panelY = 20;
		headerH = ICON_SIZE + 16;
		rebuildCodeViewer();
	}

	/** Vždy vytvoří novou instanci CodeViewerWidget – žádné duplikáty tlačítek. */
	private void rebuildCodeViewer() {
		String displayText = simpleMode ? builder.buildSimpleText() : builder.buildFullText();
		CodeViewerWidget viewer = new CodeViewerWidget(font, displayText);
		viewer.addButton("Copy Give", 58, (mx, my) -> {
			Minecraft mc = Minecraft.getInstance();
			String playerName = mc.player != null ? mc.player.getName().getString() : "@s";
			viewer.clip(builder.buildGiveCommand(playerName, simpleMode), mx, my);
		});
		viewer.addButton(simpleMode ? "◉ Simple" : "◈ Full", 58, (mx, my) -> {
			simpleMode = !simpleMode;
			globalSimpleMode = simpleMode;
			saveSimpleMode(simpleMode);
			rebuildCodeViewer();
		});
		viewer.setBounds(panelX, panelY + headerH + 1, panelW, panelH - headerH - 1);
		this.codeViewer = viewer;
	}

	@Override
	public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
		if (codeViewer == null) return;
		renderBackground(g, mx, my, pt);
		g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER);
		g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG);
		g.fill(panelX, panelY, panelX + panelW, panelY + headerH, HEADER_BG);
		g.fill(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, BORDER);

		// Ikona předmětu (2x zvětšená)
		int iconX = panelX + 8, iconY = panelY + (headerH - ICON_SIZE) / 2;
		g.pose().pushPose();
		g.pose().translate(iconX, iconY, 0);
		g.pose().scale(2f, 2f, 1f);
		g.renderItem(stack, 0, 0);
		g.renderItemDecorations(font, stack, 0, 0);
		g.pose().popPose();

		// Textové hlavičky (klikací)
		int textX = iconX + ICON_SIZE + 10, textW = panelX + panelW - textX - 8, textY = panelY + 10;
		hoverName = drawClickableText(g, stack.getHoverName().getString(), textX, textY, textW, mx, my, 0xFFFFFFFF, TEXT, 0xFFAAAAAA);
		textY += 14;
		hoverMod  = drawClickableText(g, modName, textX, textY, textW, mx, my, 0xFFCCCCCC, LABEL, 0xFF666666);
		textY += 14;
		hoverId   = drawClickableText(g, itemId, textX, textY, textW, mx, my, 0xFF88FF88, 0xFF55AA55, 0xFF55AA55);

		codeViewer.render(g, mx, my);
		super.render(g, mx, my, pt);
	}

	/** Nakreslí text, který se zvýrazní při hoveru. Vrátí true pokud je myš nad textem. */
	private boolean drawClickableText(GuiGraphics g, String text, int x, int y, int maxW, int mx, int my, int hoverColor, int normalColor, int underlineColor) {
		String truncated = truncate(text, maxW);
		boolean hover = mx >= x && mx <= x + font.width(truncated) && my >= y && my <= y + 9;
		g.drawString(font, truncated, x, y, hover ? hoverColor : normalColor, false);
		if (hover) g.fill(x, y + 9, x + font.width(truncated), y + 10, underlineColor);
		return hover;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int mx = (int) mouseX, my = (int) mouseY;
		if (button == 0) {
			if (hoverName) { copyToClipboard(stack.getHoverName().getString()); return true; }
			if (hoverMod)  { copyToClipboard(modName); return true; }
			if (hoverId)   { copyToClipboard(itemId); return true; }
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
		if (key == 256) { onClose(); return true; } // ESC
		return super.keyPressed(key, scan, mods);
	}

	@Override
	public boolean charTyped(char chr, int mods) {
		if (codeViewer.charTyped(chr)) return true;
		return super.charTyped(chr, mods);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {}

	@Override
	public void onClose() {
		assert minecraft != null;
		minecraft.setScreen(parentScreen);
	}

	private void copyToClipboard(String text) {
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
	}

	private String truncate(String text, int maxW) {
		if (font.width(text) <= maxW) return text;
		while (font.width(text + "...") > maxW && !text.isEmpty()) text = text.substring(0, text.length() - 1);
		return text + "...";
	}
}