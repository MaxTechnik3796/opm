package cz.maxtechnik.opm.client.recipe;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Zapisuje vygenerované JSON recepty do:
 *   .minecraft/config/opm/recipes/<name>.json
 *
 * Soubory pak můžeš zkopírovat do datapaku:
 *   data/<namespace>/recipes/<name>.json
 */
public final class RecipeFileWriter {

    private RecipeFileWriter() {}

    /**
     * Zapíše JSON do souboru. Pokud soubor existuje, přidá _2, _3, atd.
     * @return absolutní cesta k zapsanému souboru, nebo null při chybě
     */
    public static String write(String recipeName, String json) {
        try {
            Path dir = getRecipeDir();
            Files.createDirectories(dir);

            // Sanitize jméno souboru
            String safeName = recipeName.replaceAll("[^a-z0-9_/]", "_").toLowerCase();
            if (safeName.isBlank()) safeName = "recipe";

            Path file = uniquePath(dir, safeName);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            return null;
        }
    }

    /** Vrátí adresář config/opm/recipes/ relativně k game dir. */
    public static Path getRecipeDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("opm").resolve("recipes");
    }

    /** Pokud <name>.json existuje, zkusí <name>_2.json, <name>_3.json atd. */
    private static Path uniquePath(Path dir, String name) {
        Path candidate = dir.resolve(name + ".json");
        if (!Files.exists(candidate)) return candidate;
        int i = 2;
        while (true) {
            candidate = dir.resolve(name + "_" + i + ".json");
            if (!Files.exists(candidate)) return candidate;
            i++;
        }
    }
}