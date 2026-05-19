package cz.maxtechnik.opm.init;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OpmConfig {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	// UI
	public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
	public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;
	public static final ModConfigSpec.BooleanValue CUSTOM_DEBUG_SCREEN;
	public static final ModConfigSpec.ConfigValue<String> CUSTOM_RECIPE_PATH;
	public static final ModConfigSpec.ConfigValue<String> WORLD_NAME;
	public static final ModConfigSpec.ConfigValue<String> DATAPACK_NAME;
	public static final ModConfigSpec.ConfigValue<String> RECIPE_FOLDER;

	// Armor HUD
	public static final ModConfigSpec.BooleanValue ARMOR_HUD_ENABLED;
	public static final ModConfigSpec.BooleanValue ARMOR_HUD_INVERTED;
	public static final ModConfigSpec.EnumValue<HudLocation> ARMOR_HUD_LOCATION;

	// Effects HUD
	public static final ModConfigSpec.BooleanValue EFFECTS_HUD_ENABLED;
	public static final ModConfigSpec.EnumValue<HudLocation> EFFECTS_HUD_LOCATION;
	public static final ModConfigSpec.IntValue EFFECTS_HUD_TOP_OFFSET;

	// Pumpkin overlay
	public static final ModConfigSpec.EnumValue<PumpkinMode> PUMPKIN_OVERLAY;

	// Tutorial / join hints
	public static final ModConfigSpec.BooleanValue HIDE_TUTORIAL_TOAST;

	// Item durability in name
	public static final ModConfigSpec.BooleanValue ITEM_DURABILITY_IN_NAME;

	public enum HudLocation {
		LEFT, RIGHT
	}

	public enum PumpkinMode {
		NORMAL,       // vanilla chování
		TRANSPARENT,  // zprůhlední overlay
		HIDDEN        // úplně odstraní
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

		HIDE_TUTORIAL_TOAST = BUILDER
				.comment(
						"Hides the tutorial toast that appears when joining a world",
						"(e.g. 'Open your inventory'). Client only."
				)
				.define("hideTutorialToast", true);

		PUMPKIN_OVERLAY = BUILDER
				.comment(
						"Controls the pumpkin overlay when wearing a carved pumpkin.",
						"NORMAL = vanilla, TRANSPARENT = semi-transparent, HIDDEN = removed."
				)
				.defineEnum("pumpkinOverlay", PumpkinMode.HIDDEN);

		ITEM_DURABILITY_IN_NAME = BUILDER
				.comment(
						"Shows item durability as [current/max] after the item name",
						"when holding a damageable item. Client only."
				)
				.define("itemDurabilityInName", true);

		CUSTOM_RECIPE_PATH = BUILDER
				.comment(
						"Custom absolute path to a folder where recipes should be saved/loaded",
						"(e.g. absolute path to a datapack's recipe folder). Leave empty for default."
				)
				.define("customRecipePath", "");

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

		BUILDER.push("datapack");

		WORLD_NAME = BUILDER
				.comment("Name of the active Minecraft world saves folder (e.g. 'New World').")
				.define("1_worldName", "");

		DATAPACK_NAME = BUILDER
				.comment("Name of the datapack folder (e.g. 'dif_data').")
				.define("2_datapackName", "");

		RECIPE_FOLDER = BUILDER
				.comment("Folder name inside data/ (namespace, e.g. 'dif'). Leave empty to automatically detect.")
				.define("3_recipeFolder", "");

		BUILDER.pop();

		SPEC = BUILDER.build();
	}
}