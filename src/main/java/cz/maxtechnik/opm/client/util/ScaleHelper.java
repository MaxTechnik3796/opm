package cz.maxtechnik.opm.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Pomocná třída pro výpočet škálování rozvržení stanic, tlačítek a přepočet souřadnic myši.
 */
public class ScaleHelper {

	/** Vypočítá měřítko (scale factor) pro stanici tak, aby se vešla do určené šířky. */
	public static float getStationScale(int totalWidth, int maxAllowedWidth) {
		if (totalWidth > maxAllowedWidth && maxAllowedWidth > 0) {
			return Math.max(0.5f, (float) maxAllowedWidth / totalWidth);
		}
		return 1.0f;
	}

	/** Vypočítá měřítko pro tlačítkovou lištu. */
	public static float getButtonScale(int leftW, int reqW) {
		int availW = leftW - 16;
		if (availW < reqW && availW > 0) {
			return Math.max(0.5f, (float) availW / reqW);
		}
		return 1.0f;
	}

	/** Přepočítá X souřadnici myši ze skalovaného prostoru zpět do původních souřadnic. */
	public static int transformMouseX(int mx, int pivotX, float scale) {
		if (scale < 0.99f && scale > 0) {
			return (int) (pivotX + (mx - pivotX) / scale);
		}
		return mx;
	}

	/** Přepočítá Y souřadnici myši ze skalovaného prostoru zpět do původních souřadnic. */
	public static int transformMouseY(int my, int pivotY, float scale) {
		if (scale < 0.99f && scale > 0) {
			return (int) (pivotY + (my - pivotY) / scale);
		}
		return my;
	}

	/** Aplikuje měřítko PoseStack okolo středu (pivotX, pivotY). Vrací true pokud bylo aplikováno. */
	public static boolean pushPoseScale(GuiGraphics g, float scale, int pivotX, int pivotY) {
		if (scale < 0.99f && scale > 0) {
			PoseStack pose = g.pose();
			pose.pushPose();
			pose.translate(pivotX, pivotY, 0);
			pose.scale(scale, scale, 1.0f);
			pose.translate(-pivotX, -pivotY, 0);
			return true;
		}
		return false;
	}

	/** Obnoví matici po použití pushPoseScale. */
	public static void popPoseScale(GuiGraphics g, boolean scaled) {
		if (scaled) {
			g.pose().popPose();
		}
	}
}
