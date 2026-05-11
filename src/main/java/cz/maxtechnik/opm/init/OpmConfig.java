package cz.maxtechnik.opm.init;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OpmConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Vyžadují restart - Mixin se aplikuje při načítání
    public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
    public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;


    static {
        BUILDER.push("ui");

        NO_RECIPE_BOOK = BUILDER
                .comment(
                        "Schová tlačítko Recipe Book v inventáři a craftingu.",
                        "Vyžaduje restart hry."
                )
                .define("noRecipeBook", true);

        NO_REALMS_BUTTON = BUILDER
                .comment(
                        "Schová tlačítko Realms v hlavním menu.",
                        "Vyžaduje restart hry."
                )
                .define("noRealmsButton", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}