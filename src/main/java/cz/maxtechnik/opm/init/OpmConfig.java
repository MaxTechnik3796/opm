package cz.maxtechnik.opm.init;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OpmConfig {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	// UI
	public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
	public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;
	public static final ModConfigSpec.BooleanValue CUSTOM_DEBUG_SCREEN;

	// Armor HUD
	public static final ModConfigSpec.BooleanValue ARMOR_HUD_ENABLED;
	public static final ModConfigSpec.BooleanValue ARMOR_HUD_INVERTED;
	public static final ModConfigSpec.EnumValue<HudLocation> ARMOR_HUD_LOCATION;

	// Effects HUD
	public static final ModConfigSpec.BooleanValue EFFECTS_HUD_ENABLED;
	public static final ModConfigSpec.EnumValue<HudLocation> EFFECTS_HUD_LOCATION;
	public static final ModConfigSpec.IntValue EFFECTS_HUD_TOP_OFFSET;

	public enum HudLocation {
		LEFT, RIGHT
	}

	static {
		BUILDER.push("ui");

		NO_RECIPE_BOOK = BUILDER
				.comment("Hides Recipe Book button in inventory and crafting.")
				.define("noRecipeBook", true);

		NO_REALMS_BUTTON = BUILDER
				.comment("Hides Realms button in main menu.")
				.define("noRealmsButton", true);

		CUSTOM_DEBUG_SCREEN = BUILDER
				.comment("Replaces vanilla F3 debug screen with a custom one and enables F3+4 shortcut to toggle full tags.")
				.define("customDebugScreen", true);

		BUILDER.pop();
		BUILDER.push("armorHud");

		ARMOR_HUD_ENABLED = BUILDER
				.comment("Show armor HUD next to the hotbar.")
				.define("enabled", true);

		ARMOR_HUD_INVERTED = BUILDER
				.comment("Invert the order of armor (boots first if true, helmet first if false).")
				.define("inverted", false);

		ARMOR_HUD_LOCATION = BUILDER
				.comment("Location of the armor HUD relative to the hotbar.")
				.defineEnum("location", HudLocation.RIGHT);

		BUILDER.pop();
		BUILDER.push("effectsHud");

		EFFECTS_HUD_ENABLED = BUILDER
				.comment("Show custom effects HUD.")
				.define("enabled", true);

		EFFECTS_HUD_LOCATION = BUILDER
				.comment("Location of the effects HUD. Default is LEFT.")
				.defineEnum("location", HudLocation.LEFT);

		EFFECTS_HUD_TOP_OFFSET = BUILDER
				.comment(
						"Additional offset from the top of the screen in pixels.",
						"Permanent 4px edge padding is always applied on top of this.",
						"Default: 0"
				)
				.defineInRange("topOffset", 0, 0, 10000);

		BUILDER.pop();
		SPEC = BUILDER.build();
	}
}