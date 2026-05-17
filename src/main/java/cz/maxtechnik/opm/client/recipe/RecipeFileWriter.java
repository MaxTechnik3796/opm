package cz.maxtechnik.opm.client.recipe;

import net.minecraft.client.Minecraft;
import java.nio.file.Path;

//Zapisuje vygenerované JSON recepty:
public final class RecipeFileWriter {

    private RecipeFileWriter() {}

    //Vrátí adresář config/opm/recipes/
    public static Path getRecipeDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("opm").resolve("recipes");
    }
}