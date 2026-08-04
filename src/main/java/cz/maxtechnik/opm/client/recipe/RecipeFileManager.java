package cz.maxtechnik.opm.client.recipe;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.RecipeFileWriter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class RecipeFileManager {
	private RecipeFileManager() {}

	public record SaveResult(boolean success, File savedFile, String message) {}

	public static SaveResult saveRecipe(String fileName, String json) {
		try {
			Path dir = RecipeFileWriter.getRecipeDir();
			String safe = fileName.replaceAll("[^a-z0-9_/]", "_").toLowerCase(java.util.Locale.ROOT);
			if (safe.isBlank()) safe = "recipe";
			Path file = dir.resolve(safe + ".json");
			Files.createDirectories(file.getParent());
			Files.writeString(file, json, StandardCharsets.UTF_8);
			return new SaveResult(true, file.toFile(), "Saved!");
		} catch (Exception e) {
			return new SaveResult(false, null, "Save failed: " + e.getMessage());
		}
	}

	public static boolean deleteRecipes(java.util.Collection<File> filesToDelete) {
		if (filesToDelete == null || filesToDelete.isEmpty()) return false;
		boolean anyDeleted = false;
		HashSet<Path> parentDirsToCheck = new HashSet<>();

		for (File f : new ArrayList<>(filesToDelete)) {
			if (f.exists()) {
				try {
					Path filePath = f.toPath();
					Path parentDir = filePath.getParent();
					Files.delete(filePath);
					anyDeleted = true;
					if (parentDir != null) parentDirsToCheck.add(parentDir);
				} catch (Exception ignored) {}
			}
		}

		// Cleanup empty subdirectories
		try {
			Path baseDir = RecipeFileWriter.getRecipeDir();
			for (Path p : parentDirsToCheck) {
				cleanEmptyParents(p, baseDir);
			}
		} catch (Exception ignored) {}

		return anyDeleted;
	}

	private static void cleanEmptyParents(Path p, Path baseDir) {
		try {
			if (p == null || !p.startsWith(baseDir) || p.equals(baseDir)) return;
			try (var s = Files.list(p)) {
				if (s.findAny().isEmpty()) {
					Files.delete(p);
					cleanEmptyParents(p.getParent(), baseDir);
				}
			}
		} catch (Exception ignored) {}
	}
}
