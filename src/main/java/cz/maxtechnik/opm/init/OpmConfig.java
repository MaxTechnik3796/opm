package cz.maxtechnik.opm.init;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;

	//UI
	public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
	public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;
	public static final ModConfigSpec.BooleanValue CUSTOM_DEBUG_SCREEN;
	public static final ModConfigSpec.ConfigValue<String> WORLD_NAME;
	public static final ModConfigSpec.ConfigValue<String> DATAPACK_NAME;
	public static final ModConfigSpec.ConfigValue<String> RECIPE_FOLDER;

	//Armor HUD
	public static final ModConfigSpec.BooleanValue ARMOR_HUD_ENABLED;
	public static final ModConfigSpec.EnumValue<HudLocation> ARMOR_HUD_LOCATION;
	public static final ModConfigSpec.IntValue ARMOR_HUD_ROTATE;
	public static final ModConfigSpec.BooleanValue ARMOR_HUD_LOCKED;
	public static final ModConfigSpec.IntValue ARMOR_HUD_FREE_X;
	public static final ModConfigSpec.IntValue ARMOR_HUD_FREE_Y;
	public static final ModConfigSpec.DoubleValue ARMOR_HUD_SCALE;

	//Effects HUD
	public static final ModConfigSpec.BooleanValue EFFECTS_HUD_ENABLED;
	public static final ModConfigSpec.EnumValue<HudLocation> EFFECTS_HUD_LOCATION;
	public static final ModConfigSpec.IntValue EFFECTS_HUD_X_OFFSET;
	public static final ModConfigSpec.IntValue EFFECTS_HUD_Y_OFFSET;
	public static final ModConfigSpec.DoubleValue EFFECTS_HUD_SCALE;

	//Pumpkin overlay
	public static final ModConfigSpec.EnumValue<PumpkinMode> PUMPKIN_OVERLAY;

	//Tutorial / join hints
	public static final ModConfigSpec.BooleanValue HIDE_TUTORIAL_TOAST;

	//Item durability in name
	public static final ModConfigSpec.BooleanValue ITEM_DURABILITY_IN_NAME;
	public static final ModConfigSpec.IntValue ITEM_DURABILITY_X_OFFSET;
	public static final ModConfigSpec.IntValue ITEM_DURABILITY_Y_OFFSET;
	public static final ModConfigSpec.DoubleValue ITEM_DURABILITY_SCALE;

	//Scoreboard
	public static final ModConfigSpec.BooleanValue SCOREBOARD_ENABLED;
	public static final ModConfigSpec.EnumValue<HudLocation> SCOREBOARD_SIDE;
	public static final ModConfigSpec.IntValue SCOREBOARD_X_OFFSET;
	public static final ModConfigSpec.IntValue SCOREBOARD_Y_OFFSET;
	public static final ModConfigSpec.DoubleValue SCOREBOARD_SCALE;
	public enum HudLocation{
		LEFT,RIGHT
	}
	public enum PumpkinMode{
		NORMAL,TRANSPARENT,HIDDEN
	}
	static{
		BUILDER.push("ui");
		NO_RECIPE_BOOK=BUILDER.comment("Hides Recipe Book button in inventory and crafting.").define("noRecipeBook",true);
		NO_REALMS_BUTTON=BUILDER.comment("Hides Realms button in main menu.").define("noRealmsButton",true);
		CUSTOM_DEBUG_SCREEN=BUILDER.comment("Replaces vanilla F3 debug screen with a custom one and enables F3+4 shortcut to toggle full tags.").define("customDebugScreen",true);
		HIDE_TUTORIAL_TOAST=BUILDER.comment("Hides the tutorial toast that appears when joining a world (e.g. 'Open your inventory'). Client only.").define("hideTutorialToast",true);
		PUMPKIN_OVERLAY=BUILDER.comment("Controls the pumpkin overlay when wearing a carved pumpkin. NORMAL = vanilla, TRANSPARENT = semi-transparent, HIDDEN = removed.").defineEnum("pumpkinOverlay",PumpkinMode.HIDDEN);
		BUILDER.pop();
		BUILDER.push("armorHud");
		ARMOR_HUD_ENABLED=BUILDER.comment("Show armor HUD next to the hotbar.").define("enabled",true);
		ARMOR_HUD_LOCATION=BUILDER.comment("Location of the armor HUD relative to the hotbar (used when locked).").defineEnum("location",HudLocation.RIGHT);
		ARMOR_HUD_ROTATE=BUILDER.comment("Rotation/direction of armor slot layout. 0 = helmet LEFT, boots RIGHT (horizontal), 1 = helmet TOP, boots BOTTOM (vertical), 2 = helmet RIGHT, boots LEFT (horizontal, mirrored), 3 = helmet BOTTOM, boots TOP (vertical, mirrored)").defineInRange("rotate",1,0,3);
		ARMOR_HUD_LOCKED=BUILDER.comment("If true, armor HUD is anchored to the hotbar. If false, it can be freely positioned.").define("locked",true);
		ARMOR_HUD_FREE_X=BUILDER.comment("Absolute X position of the free (unlocked) armor HUD.").defineInRange("freeX",2,2,10000);
		ARMOR_HUD_FREE_Y=BUILDER.comment("Absolute Y position of the free (unlocked) armor HUD.").defineInRange("freeY",2,2,10000);
		ARMOR_HUD_SCALE=BUILDER.comment("Scale of the armor HUD (from 0.5 to 2.0).").defineInRange("scale",1.0,0.5,2.0);
		BUILDER.pop();
		BUILDER.push("effectsHud");
		EFFECTS_HUD_ENABLED=BUILDER.comment("Show custom effects HUD.").define("enabled",true);
		EFFECTS_HUD_LOCATION=BUILDER.comment("Location of the effects HUD. Default is LEFT.").defineEnum("location",HudLocation.LEFT);
		EFFECTS_HUD_X_OFFSET=BUILDER.comment("Horizontal offset for the effects HUD display.").defineInRange("xOffset",0,-10000,10000);
		EFFECTS_HUD_Y_OFFSET=BUILDER.comment("Vertical offset for the effects HUD display.").defineInRange("yOffset",0,-10000,10000);
		EFFECTS_HUD_SCALE=BUILDER.comment("Scale of the effects HUD (from 0.5 to 2.0).").defineInRange("scale",1.0,0.5,2.0);
		BUILDER.pop();
		BUILDER.push("datapack");
		WORLD_NAME=BUILDER.comment("Name of the active Minecraft world saves folder (e.g. 'New World').").define("1_worldName","");
		DATAPACK_NAME=BUILDER.comment("Name of the datapack folder (e.g. 'dif_data').").define("2_datapackName","");
		RECIPE_FOLDER=BUILDER.comment("Folder name inside data/ (namespace, e.g. 'dif'). Leave empty to automatically detect.").define("3_recipeFolder","");
		BUILDER.pop();
		BUILDER.push("itemDurability");
		ITEM_DURABILITY_IN_NAME=BUILDER.comment("Shows item durability as [current/max] after the item name when holding a damageable item. Client only.").define("itemDurabilityInName",true);
		ITEM_DURABILITY_X_OFFSET=BUILDER.comment("Horizontal offset for the item durability HUD display.").defineInRange("itemDurabilityXOffset",0,-10000,10000);
		ITEM_DURABILITY_Y_OFFSET=BUILDER.comment("Vertical offset for the item durability HUD display.").defineInRange("itemDurabilityYOffset",0,-10000,10000);
		ITEM_DURABILITY_SCALE=BUILDER.comment("Scale of the item durability HUD display (from 0.5 to 2.0).").defineInRange("scale",1.0,0.5,2.0);
		BUILDER.pop();
		BUILDER.push("scoreboard");
		SCOREBOARD_ENABLED=BUILDER.comment("Show scoreboard.").define("enabled",true);
		SCOREBOARD_SIDE=BUILDER.comment("Location of the scoreboard (LEFT or RIGHT).").defineEnum("side",HudLocation.RIGHT);
		SCOREBOARD_X_OFFSET=BUILDER.comment("Horizontal offset for the scoreboard display.").defineInRange("xOffset",0,-10000,10000);
		SCOREBOARD_Y_OFFSET=BUILDER.comment("Vertical offset for the scoreboard display.").defineInRange("yOffset",0,-10000,10000);
		SCOREBOARD_SCALE=BUILDER.comment("Scale of the scoreboard (from 0.5 to 2.0).").defineInRange("scale",1.0,0.5,2.0);
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}