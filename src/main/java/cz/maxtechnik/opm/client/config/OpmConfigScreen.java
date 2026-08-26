package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.ui.UiKit;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Moderní minimalistická konfigurační obrazovka a Interactive HUD Canvas.
 * Zcela bez ztmavování a bluru – svět i HUD zůstávají 100% čisté a živé.
 * Umožňuje přímou manipulaci s HUD prvky (přesouvání, změna měřítka kolečkem)
 * a kontextové nastavení přes dynamický postranní panel (ConfigSidebar).
 */
public final class OpmConfigScreen extends Screen {

	private final Screen parent;
	private final List<HudElement> elements = new ArrayList<>();
	private final ConfigSidebar sidebar = new ConfigSidebar();

	private HudElement selectedElement;
	private HudElement draggingElement;
	private int dragGrabX, dragGrabY;

	public OpmConfigScreen(Screen parent) {
		super(Component.literal("OPM Config"));
		this.parent = parent;

		elements.add(new ArmorHudElement());
		elements.add(new DurabilityHudElement());
		elements.add(new EffectsHudElement());
		elements.add(new ScoreboardHudElement());
		elements.add(new SimpleTextHudElement.Title());
		elements.add(new SimpleTextHudElement.Actionbar());

		// Výchozí výběr: Armor HUD
		this.selectedElement = elements.getFirst();
	}

	@Override
	protected void init() {
		super.init();
		for (HudElement el : elements) {
			el.clamp(width, height);
		}
	}

	@Override
	public void tick() {
		super.tick();
		for (HudElement el : elements) {
			el.clamp(width, height);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
		// Žádné ztmavování ani blur pozadí – svět a HUD zůstávají krystalicky čisté!
	}

	@Override
	public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
		// Horní decentní nápověda
		String tip = "Drag elements • Scroll to scale • Tab: Hide/Show Menu • F8: GUI Scale • Esc: Save";
		int tipW = font.width(tip);
		int tipX = (width - tipW) / 2 - 6;
		g.fill(tipX, 4, tipX + tipW + 12, 17, UiKit.C_POPUP_BG);
		UiKit.drawOutline(g, tipX, 4, tipW + 12, 13, UiKit.C_BORDER);
		g.drawString(font, tip, (width - tipW) / 2, 7, UiKit.C_LABEL, false);

		// Render HUD prvků na canvasu
		for (HudElement el : elements) {
			if (!el.isEnabled()) continue;
			int ex = el.getX(width, height);
			int ey = el.getY(width, height);
			int ew = el.getW();
			int eh = el.getH();

			boolean hovered = UiKit.hit(mx, my, ex - 2, ey - 2, ew + 4, eh + 4);
			boolean selected = (el == selectedElement && !sidebar.isShowGeneral());
			boolean dragging = (el == draggingElement);

			el.renderPreview(g, font, width, height, hovered, selected, dragging);
		}

		// Render postranního panelu na pravé straně s dynamickou výškou
		sidebar.render(g, font, width, height, mx, my, elements, selectedElement);

		super.render(g, mx, my, pt);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int mx = (int) mouseX, my = (int) mouseY;

		// 1. Zpracování kliknutí v postranním panelu
		HudElement[] selRef = new HudElement[]{selectedElement};
		if (sidebar.mouseClicked(mouseX, mouseY, button, width, height, elements, selRef, this::resetAllPositions, this::onClose)) {
			selectedElement = selRef[0];
			return true;
		}

		// 2. Kliknutí na canvas – test HUD prvků pro výběr / drag
		if (button == 0) {
			for (int i = elements.size() - 1; i >= 0; i--) {
				HudElement el = elements.get(i);
				if (!el.isEnabled()) continue;

				int ex = el.getX(width, height);
				int ey = el.getY(width, height);
				int ew = el.getW();
				int eh = el.getH();

				if (UiKit.hit(mx, my, ex - 2, ey - 2, ew + 4, eh + 4)) {
					selectedElement = el;
					sidebar.setShowGeneral(false);
					draggingElement = el;
					dragGrabX = mx - ex;
					dragGrabY = my - ey;
					return true;
				}
			}

			// Kliknutí do prázdného prostoru canvasu přepne na obecné možnosti
			sidebar.setShowGeneral(true);
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (sidebar.mouseDragged(mouseY)) return true;

		if (draggingElement != null) {
			draggingElement.onDrag((int) mouseX, (int) mouseY, dragGrabX, dragGrabY, width, height);
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		sidebar.mouseReleased();

		if (draggingElement != null) {
			draggingElement.save();
			OpmConfig.SPEC.save();
			draggingElement = null;
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (sidebar.mouseScrolled(mouseX, mouseY, scrollY, width, height, selectedElement)) {
			return true;
		}

		// Změna měřítka (scale) kolečkem myši přímo nad prvkem na canvasu
		for (HudElement el : elements) {
			if (!el.isEnabled()) continue;
			int ex = el.getX(width, height);
			int ey = el.getY(width, height);
			int ew = el.getW();
			int eh = el.getH();

			if (UiKit.hit((int) mouseX, (int) mouseY, ex - 2, ey - 2, ew + 4, eh + 4)) {
				el.adjustScale(scrollY > 0 ? 0.05 : -0.05);
				el.save();
				OpmConfig.SPEC.save();
				return true;
			}
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (cz.maxtechnik.opm.client.ui.UiScale.handleKeyPressed(minecraft, key)) return true;
		if (key == 258 || key == 72) { // Tab or H -> Toggle Sidebar Hide / Show
			sidebar.toggleCollapsed();
			return true;
		}
		if (key == 256) { // Esc
			onClose();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	private void resetAllPositions() {
		for (HudElement el : elements) {
			el.reset();
			el.save();
		}
		OpmConfig.SPEC.save();
	}

	private void saveAll() {
		sidebar.saveGeneralConfig();
		for (HudElement el : elements) {
			el.save();
		}
		OpmConfig.SPEC.save();
	}

	@Override
	public void onClose() {
		saveAll();
		if (minecraft != null) minecraft.setScreen(parent);
	}
}