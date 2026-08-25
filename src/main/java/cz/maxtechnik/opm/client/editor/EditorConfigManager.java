package cz.maxtechnik.opm.client.editor;

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
		File configFile = new File(mc.gameDirectory, "config/opm_editor.txt");
		if (!configFile.exists()) return;
		try {
			List<String> lines = Files.readAllLines(configFile.toPath());
			if (!lines.isEmpty()) setter.accept(Integer.parseInt(lines.getFirst().trim()));
		} catch (Exception ignored) {}
	}

	public static void saveConfig(Minecraft mc, int invPanelHeight) {
		if (mc == null) return;
		File configFile = new File(mc.gameDirectory, "config/opm_editor.txt");
		try {
			Files.createDirectories(configFile.getParentFile().toPath());
			Files.writeString(configFile.toPath(), String.valueOf(invPanelHeight));
		} catch (Exception ignored) {}
	}
}
