package cz.maxtechnik.opm.client.util;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Správce konfigurace nastavení editoru (rozvržení, výška spodního panelu v config/opm_editor.txt).
 */
public final class EditorConfigManager {
	private EditorConfigManager() {}

	public static void loadConfig(Minecraft mc, IntConsumer setter) {
		if (mc == null) return;
		File f = new File(mc.gameDirectory, "config/opm_editor.txt");
		if (!f.exists()) return;
		try {
			List<String> lines = Files.readAllLines(f.toPath());
			if (!lines.isEmpty()) setter.accept(Integer.parseInt(lines.getFirst().trim()));
		} catch (Exception ignored) {}
	}

	public static void saveConfig(Minecraft mc, int invPanelHeight) {
		if (mc == null) return;
		File f = new File(mc.gameDirectory, "config/opm_editor.txt");
		try {
			Files.createDirectories(f.getParentFile().toPath());
			Files.writeString(f.toPath(), String.valueOf(invPanelHeight));
		} catch (Exception ignored) {}
	}
}
