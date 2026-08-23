package cz.maxtechnik.opm.client.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * SearchEngine pro vyhledávání položek, fluidů, tagů a receptů.
 * {@code @mod slovo ...} – filtr podle namespace modu, {@code #tag slovo ...} – filtr podle tagu,
 * {@code text} – fulltext v názvu/ID/tagu, {@code |} – logické NEBO mezi dotazy.
 */
public final class SearchEngine {
	private SearchEngine() {}

	public static boolean matches(ItemStack stack, String query) {
		if (query == null || query.isBlank()) return true;
		if (stack == null || stack.isEmpty()) return false;
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.contains("|")) {
			for (String b : q.split("\\|")) { if (!b.isBlank() && matchesSingle(stack, b.trim())) return true; }
			return false;
		}
		return matchesSingle(stack, q);
	}

	private static boolean matchesSingle(ItemStack stack, String q) {
		if (q.isEmpty()) return true;
		ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
		String path = loc.getPath().toLowerCase(Locale.ROOT);
		String id = loc.toString().toLowerCase(Locale.ROOT);
		boolean isTagItem = stack.getItem() == Items.NAME_TAG && name.startsWith("#");

		if (q.startsWith("@")) {
			String body = q.substring(1).trim();
			if (body.isEmpty()) return true;
			String[] parts = body.split("\\s+");
			String ns = loc.getNamespace().toLowerCase(Locale.ROOT);
			boolean modOk = ns.contains(parts[0]);
			if (!modOk && stack.getItem() instanceof BucketItem b && b.content != null)
				modOk = BuiltInRegistries.FLUID.getKey(b.content).getNamespace().toLowerCase(Locale.ROOT).contains(parts[0]);
			if (!modOk) return false;
			for (int i = 1; i < parts.length; i++)
				if (!parts[i].isEmpty() && !textHit(stack, name, path, id, isTagItem, parts[i])) return false;
			return true;
		}

		if (q.startsWith("#")) {
			String body = q.substring(1).trim();
			if (body.isEmpty()) return true;
			String[] parts = body.split("\\s+");
			if (!tagHit(stack, name, parts[0], isTagItem)) return false;
			for (int i = 1; i < parts.length; i++)
				if (!parts[i].isEmpty() && !textHit(stack, name, path, id, isTagItem, parts[i])) return false;
			return true;
		}

		for (String w : q.split("\\s+"))
			if (!w.isEmpty() && !textHit(stack, name, path, id, isTagItem, w) && !tagHit(stack, name, w, isTagItem)) return false;
		return true;
	}

	/** Kontrola shody tagu – item tagy, fluid tagy, nebo název NameTag položky. */
	private static boolean tagHit(ItemStack stack, String name, String tq, boolean isTagItem) {
		if (isTagItem && name.contains(tq)) return true;
		try {
			if (stack.getItemHolder().tags().anyMatch(t -> locContains(t.location(), tq))) return true;
		} catch (Exception ignored) {}
		if (stack.getItem() instanceof BucketItem b && b.content != null) {
			try {
				if (locContains(BuiltInRegistries.FLUID.getKey(b.content), tq)) return true;
				if (BuiltInRegistries.FLUID.wrapAsHolder(b.content).tags().anyMatch(t -> locContains(t.location(), tq))) return true;
			} catch (Exception ignored) {}
		}
		return false;
	}

	/** Kontrola shody textu – název, ID, path, fluid obsah bucketu, nebo obsah tagu v záložce Tags. */
	private static boolean textHit(ItemStack stack, String name, String path, String id, boolean isTagItem, String w) {
		if (name.contains(w) || path.contains(w) || id.contains(w)) return true;
		if (stack.getItem() instanceof BucketItem b && b.content != null) {
			try { if (locContains(BuiltInRegistries.FLUID.getKey(b.content), w)) return true; } catch (Exception ignored) {}
		}
		if (isTagItem) {
			ResourceLocation tagLoc = ResourceLocation.tryParse(name.substring(1).trim());
			if (tagLoc != null) {
				try {
					var tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tagLoc));
					if (tag.isPresent() && tag.get().stream().anyMatch(h -> {
						ResourceLocation il = BuiltInRegistries.ITEM.getKey(h.value());
						return h.value().getName(new ItemStack(h.value())).getString().toLowerCase(Locale.ROOT).contains(w)
								|| il.getPath().toLowerCase(Locale.ROOT).contains(w) || il.toString().toLowerCase(Locale.ROOT).contains(w);
					})) return true;
				} catch (Exception ignored) {}
			}
		}
		return false;
	}

	private static boolean locContains(ResourceLocation loc, String q) {
		return loc.toString().toLowerCase(Locale.ROOT).contains(q) || loc.getPath().toLowerCase(Locale.ROOT).contains(q);
	}

	// --- Vyhledávání souborů receptů ---

	public static boolean matchesFile(File file, Path baseDir, String query) {
		if (query == null || query.isBlank()) return true;
		if (file == null || !file.exists()) return false;
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.contains("|")) {
			for (String b : q.split("\\|")) { if (!b.isBlank() && matchesSingleFile(file, baseDir, b.trim())) return true; }
			return false;
		}
		return matchesSingleFile(file, baseDir, q);
	}

	private static boolean matchesSingleFile(File file, Path baseDir, String q) {
		if (q.isEmpty()) return true;
		String fileName = file.getName().toLowerCase(Locale.ROOT);
		String rel = "";
		try { if (baseDir != null) rel = baseDir.relativize(file.toPath()).toString().replace('\\', '/').toLowerCase(Locale.ROOT); } catch (Exception ignored) {}
		for (String w : q.split("\\s+"))
			if (!w.isEmpty() && !fileName.contains(w) && (rel.isEmpty() || !rel.contains(w))) return false;
		return true;
	}
}
