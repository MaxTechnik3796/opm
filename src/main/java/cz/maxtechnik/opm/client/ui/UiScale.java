package cz.maxtechnik.opm.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Centrální správce a pomocná utilita pro změnu měřítka GUI (GUI Scale)
 * napříč všemi obrazovkami OPM módu.
 * 
 * Zajišťuje správný sled kroků v Minecraftu (nastavení -> uložení -> resizeDisplay),
 * aby se okno a veškeré widgety bezpečně přepočítaly bez rozpadu rozlišení a souřadnic.
 */
public final class UiScale {

	private UiScale() {}

	/**
	 * Zjistí maximální povolený GUI Scale pro aktuální rozlišení okna.
	 */
	public static int getMaxScale(Minecraft mc) {
		if (mc == null || mc.getWindow() == null) return 4;
		return mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
	}

	/**
	 * Vrátí aktuální GUI Scale (0 = Auto, 1 = 1x, 2 = 2x, atd.).
	 */
	public static int getScale(Minecraft mc) {
		if (mc == null || mc.options == null) return 0;
		return mc.options.guiScale().get();
	}

	/**
	 * Nastaví konkrétní GUI Scale a bezpečně aktualizuje celý display a aktivní screen.
	 */
	public static void setScale(Minecraft mc, int scale) {
		if (mc == null || mc.options == null) return;
		int max = getMaxScale(mc);
		int targetScale = (scale < 0 || scale > max) ? 0 : scale;

		mc.options.guiScale().set(targetScale);
		mc.options.save();
		mc.resizeDisplay();
	}

	/**
	 * Přepne na následující GUI scale v cyklu: Auto (0) -> 1x -> 2x -> ... -> Max -> Auto (0).
	 */
	public static void cycleScale(Minecraft mc) {
		if (mc == null) return;
		int max = getMaxScale(mc);
		int current = getScale(mc);
		int next = (current >= max) ? 0 : current + 1;
		setScale(mc, next);
	}

	/**
	 * Textový popisek pro UI tlačítko (např. "GUI: Auto", "GUI: 2x", "GUI: 3x").
	 */
	public static String getLabel(Minecraft mc) {
		int scale = getScale(mc);
		return scale == 0 ? "GUI: Auto" : "GUI: " + scale + "x";
	}

	/**
	 * Zpracuje společné klávesové zkratky pro změnu GUI Scale (např. klávesa F8).
	 * @return true pokud byla klávesa zpracována.
	 */
	public static boolean handleKeyPressed(Minecraft mc, int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_F8) {
			cycleScale(mc);
			return true;
		}
		return false;
	}

	/**
	 * Vykreslí malé elegantní tlačítko pro rychlé přepnutí GUI Scale.
	 */
	public static void renderScaleButton(GuiGraphics g, Font font, Minecraft mc, int x, int y, int w, int h, int mx, int my) {
		boolean hov = UiKit.hit(mx, my, x, y, w, h);
		g.fill(x, y, x + w, y + h, hov ? UiKit.C_BTN_H : UiKit.C_BTN);
		g.fill(x, y, x + w, y + 1, 0x44FFFFFF);
		UiKit.drawOutline(g, x, y, w, h, UiKit.C_BORDER);
		String label = getLabel(mc);
		g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, hov ? UiKit.C_ACCENT_HOV : UiKit.C_TEXT);
	}

	/**
	 * Zpracuje kliknutí na tlačítko GUI Scale.
	 */
	public static boolean handleScaleButtonClick(Minecraft mc, int x, int y, int w, int h, int mx, int my, int button) {
		if (button == 0 && UiKit.hit(mx, my, x, y, w, h)) {
			cycleScale(mc);
			return true;
		}
		return false;
	}
}
