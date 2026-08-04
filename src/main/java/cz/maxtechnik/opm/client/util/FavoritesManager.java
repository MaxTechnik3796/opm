package cz.maxtechnik.opm.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Správce oblíbených předmětů (nacítání a ukládání do config/opm_favorites.txt).
 */
public final class FavoritesManager {
	private FavoritesManager() {}

	public static void loadFavorites(Minecraft mc, List<ItemStack> favorites) {
		favorites.clear();
		if (mc == null) return;
		File f = new File(mc.gameDirectory, "config/opm_favorites.txt");
		if (!f.exists()) return;
		try {
			for (String s : Files.readAllLines(f.toPath())) {
				ResourceLocation loc = ResourceLocation.tryParse(s.trim());
				if (loc != null) {
					BuiltInRegistries.ITEM.getOptional(loc).ifPresent(item -> favorites.add(new ItemStack(item)));
				}
			}
		} catch (Exception ignored) {}
	}

	public static void saveFavorites(Minecraft mc, List<ItemStack> favorites) {
		if (mc == null) return;
		File f = new File(mc.gameDirectory, "config/opm_favorites.txt");
		try {
			Files.createDirectories(f.getParentFile().toPath());
			List<String> lines = new ArrayList<>();
			for (ItemStack s : favorites) {
				if (s != null && !s.isEmpty()) {
					lines.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
				}
			}
			Files.write(f.toPath(), lines);
		} catch (Exception ignored) {}
	}
}
