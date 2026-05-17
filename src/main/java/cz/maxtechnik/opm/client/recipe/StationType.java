package cz.maxtechnik.opm.client.recipe;

/**
 * Typy stanic v editoru receptů.
 * Každý typ má vlastní UI uvnitř záložky.
 */
public enum StationType {
    CRAFTING          ("Crafting",     "minecraft:crafting_table"),
    FURNACE           ("Furnace",      "minecraft:furnace"),
    STONECUTTER       ("Stonecutter",  "minecraft:stonecutter"),
    SMITHING          ("Smithing",     "minecraft:smithing_table"),
    MECH_CRAFTING     ("Mech. Crafter","create:mechanical_crafter"),
    MIXING            ("Mixing",       "create:basin"),
    PRESSING          ("Pressing",     "create:mechanical_press"),
    FAN               ("Fan",          "create:encased_fan"),
    CRUSHING          ("Crushing",     "create:crushing_wheel"),
    ;

    public final String displayName;
    public final String stationItemId;

    StationType(String displayName, String stationItemId) {
        this.displayName   = displayName;
        this.stationItemId = stationItemId;
    }

    public boolean isCreate() {
        return switch (this) {
            case MECH_CRAFTING, MIXING, PRESSING, CRUSHING, FAN -> true;
            default -> false;
        };
    }
}