package cz.maxtechnik.opm.client.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Centrální správce a pomocná utilita pro změnu měřítka GUI (GUI Scale)
 * napříč všemi obrazovkami OPM módu.
 * Zajišťuje správný sled kroků v Minecraftu (nastavení -> uložení -> resizeDisplay),
 * aby se okno a veškeré widgety bezpečně přepočítaly bez rozpadu rozlišení a souřadnic.
 */
public final class UiScale {

	private UiScale() {}

	/**
	 * Zjistí maximální povolený GUI Scale pro aktuální rozlišení okna.
	 */
	public static int getMaxScale(Minecraft mc) {
		if (mc == null) return 4;
		return mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
	}

	/**
	 * Vrátí aktuální GUI Scale (0 = Auto, 1 = 1x, 2 = 2x, atd.).
	 */
	public static int getScale(Minecraft mc) {
		if (mc == null) return 0;
		return mc.options.guiScale().get();
	}

	/**
	 * Nastaví konkrétní GUI Scale a bezpečně aktualizuje celý display a aktivní screen.
	 */
	public static void setScale(Minecraft mc, int scale) {
		if (mc == null) return;
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

}
