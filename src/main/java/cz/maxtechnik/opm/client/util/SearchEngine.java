package cz.maxtechnik.opm.client.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Samostatný SearchEngine modul pro vyhledávání položek, fluidů, tagů a receptů.
 * Podporuje:
 *  - {@code @mod [item]} : vyhledávání podle namespace modu (např. {@code @minecraft iron} nebo {@code @create})
 *  - {@code #tag [item]} : vyhledávání podle registrovaných tagů (např. {@code #logs} nebo {@code #c:ingots})
 *  - {@code text}        : běžné vyhledávání podle názvu, ID předmětu nebo tagu
 */
public final class SearchEngine {
	private SearchEngine() {}

	/**
	 * Zjistí, zda předmět odpovídá zadanému dotazu.
	 */
	public static boolean matches(ItemStack stack, String query) {
		if (query == null || query.isBlank()) return true;
		if (stack == null || stack.isEmpty()) return false;

		String q = query.trim().toLowerCase(Locale.ROOT);

		if (q.startsWith("@")) {
			String body = q.substring(1).trim();
			int spaceIdx = body.indexOf(' ');
			String modPart = spaceIdx >= 0 ? body.substring(0, spaceIdx).trim() : body;
			String itemPart = spaceIdx >= 0 ? body.substring(spaceIdx + 1).trim() : "";

			ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
			String ns = loc.getNamespace().toLowerCase(Locale.ROOT);
			if (!ns.contains(modPart)) return false;

			if (!itemPart.isEmpty()) {
				String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
				String path = loc.getPath().toLowerCase(Locale.ROOT);
				String id = loc.toString().toLowerCase(Locale.ROOT);
				return name.contains(itemPart) || path.contains(itemPart) || id.contains(itemPart);
			}
			return true;
		}

		if (q.startsWith("#")) {
			String body = q.substring(1).trim();
			int spaceIdx = body.indexOf(' ');
			String tagPart = spaceIdx >= 0 ? body.substring(0, spaceIdx).trim() : body;
			String itemPart = spaceIdx >= 0 ? body.substring(spaceIdx + 1).trim() : "";

			boolean hasTag;
			String hoverName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
			if (hoverName.contains(tagPart)) {
				hasTag = true;
			} else {
				try {
					var holder = stack.getItem().builtInRegistryHolder();
					hasTag = holder.tags().anyMatch(t -> t.location().toString().toLowerCase(Locale.ROOT).contains(tagPart)
							|| t.location().getPath().toLowerCase(Locale.ROOT).contains(tagPart));
				} catch (Exception ignored) {
					hasTag = false;
				}
			}

			if (!hasTag) return false;

			if (!itemPart.isEmpty()) {
				ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
				String name = hoverName;
				String path = loc.getPath().toLowerCase(Locale.ROOT);
				String id = loc.toString().toLowerCase(Locale.ROOT);
				return name.contains(itemPart) || path.contains(itemPart) || id.contains(itemPart);
			}
			return true;
		}

		String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
		ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String id = loc.toString().toLowerCase(Locale.ROOT);
		String path = loc.getPath().toLowerCase(Locale.ROOT);

		if (name.contains(q) || id.contains(q) || path.contains(q)) return true;

		try {
			var holder = stack.getItem().builtInRegistryHolder();
			return holder.tags().anyMatch(t -> t.location().toString().toLowerCase(Locale.ROOT).contains(q));
		} catch (Exception ignored) {
			return false;
		}
	}



	/**
	 * Zjistí, zda soubor receptu odpovídá vyhledávacímu dotazu.
	 */
	public static boolean matchesFile(File file, Path baseDir, String query) {
		if (query == null || query.isBlank()) return true;
		if (file == null || !file.exists()) return false;

		String q = query.trim().toLowerCase(Locale.ROOT);
		try {
			if (baseDir != null) {
				String rel = baseDir.relativize(file.toPath()).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
				if (rel.contains(q)) return true;
			}
		} catch (Exception ignored) {}

		return file.getName().toLowerCase(Locale.ROOT).contains(q);
	}
}
