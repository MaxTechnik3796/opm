package cz.maxtechnik.opm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmModConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	//General:
	public static final ModConfigSpec.BooleanValue NO_TOASTS;
	public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
	//Button Remover:
	public static final ModConfigSpec.BooleanValue REMOVE_REALMS_BUTTON;
	public static final ModConfigSpec.BooleanValue REMOVE_TELEMETRY_BUTTON;
	//Overlayer Control:
	public static final ModConfigSpec.ConfigValue<PumpkinMode> PUMPKIN_OVERLAY;
	//Scoreboard:
	public static final ModConfigSpec.IntValue SCOREBOARD_X;
	public static final ModConfigSpec.IntValue SCOREBOARD_Y;
	public static final ModConfigSpec.ConfigValue<AnchorX> SCOREBOARD_ANCHOR_X;
	public static final ModConfigSpec.ConfigValue<AnchorY> SCOREBOARD_ANCHOR_Y;
	static{
		BUILDER.push("general");
		NO_TOASTS=BUILDER.comment("Hides all toast popups (tutorial, advancement, recipe, etc.)").define("noToasts",true);
		NO_RECIPE_BOOK=BUILDER.comment("Hides Recipe Book button in inventory and crafting.").define("noRecipeBook",true);
		BUILDER.pop();
		BUILDER.push("buttonRemover");
		REMOVE_REALMS_BUTTON=BUILDER.comment("Hides Realms button in main menu.").define("removeRealmsButton",true);
		REMOVE_TELEMETRY_BUTTON=BUILDER.comment("Hides Telemetry button in options.").define("removeTelemetryButton",true);
		BUILDER.pop();
		BUILDER.push("overlayerControl");
		PUMPKIN_OVERLAY=BUILDER.comment("Controls the pumpkin overlay when wearing a carved pumpkin.","NORMAL = vanilla","TRANSPARENT = semi-transparent","HIDDEN = removed").defineEnum("removePumpkinOverlay",PumpkinMode.NORMAL);
		BUILDER.pop();
		BUILDER.push("scoreboard");
		SCOREBOARD_X=BUILDER.defineInRange("scoreboardX",0,Integer.MIN_VALUE,Integer.MAX_VALUE);
		SCOREBOARD_Y=BUILDER.defineInRange("scoreboardY",0,Integer.MIN_VALUE,Integer.MAX_VALUE);
		SCOREBOARD_ANCHOR_X=BUILDER.defineEnum("scoreboardAnchorX",AnchorX.LEFT);
		SCOREBOARD_ANCHOR_Y=BUILDER.defineEnum("scoreboardAnchorY",AnchorY.TOP);
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}