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
	public static final ModConfigSpec.IntValue PUMPKIN_OVERLAY;
	public static final ModConfigSpec.IntValue SPYGLASS_OVERLAY;
	//Sidelist:
	public static final ModConfigSpec.IntValue SIDELIST_X;
	public static final ModConfigSpec.IntValue SIDELIST_Y;
	public static final ModConfigSpec.ConfigValue<Anchor.X> SIDELIST_ANCHOR_X;
	public static final ModConfigSpec.ConfigValue<Anchor.Y> SIDELIST_ANCHOR_Y;
	//Example Widget:
	public static final ModConfigSpec.IntValue EXAMPLE_X;
	public static final ModConfigSpec.IntValue EXAMPLE_Y;
	public static final ModConfigSpec.ConfigValue<Anchor.X> EXAMPLE_ANCHOR_X;
	public static final ModConfigSpec.ConfigValue<Anchor.Y> EXAMPLE_ANCHOR_Y;
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
		PUMPKIN_OVERLAY=BUILDER.comment("Controls the opacity of the pumpkin overlay in percent (0 = hidden, 100 = vanilla).").defineInRange("pumpkinOverlayOpacity",100,0,100);
		SPYGLASS_OVERLAY=BUILDER.comment("Controls the opacity of the spyglass overlay in percent (0 = hidden, 100 = vanilla).").defineInRange("spyglassOverlayOpacity",100,0,100);
		BUILDER.pop();
		BUILDER.push("sidelist");
		SIDELIST_X=BUILDER.defineInRange("sidelistX",0,Integer.MIN_VALUE,Integer.MAX_VALUE);
		SIDELIST_Y=BUILDER.defineInRange("sidelistY",0,Integer.MIN_VALUE,Integer.MAX_VALUE);
		SIDELIST_ANCHOR_X=BUILDER.defineEnum("sidelistAnchorX",Anchor.X.LEFT);
		SIDELIST_ANCHOR_Y=BUILDER.defineEnum("sidelistAnchorY",Anchor.Y.TOP);
		BUILDER.pop();
		BUILDER.push("exampleWidget");
		EXAMPLE_X=BUILDER.defineInRange("exampleX",0,Integer.MIN_VALUE,Integer.MAX_VALUE);
		EXAMPLE_Y=BUILDER.defineInRange("exampleY",0,Integer.MIN_VALUE,Integer.MAX_VALUE);
		EXAMPLE_ANCHOR_X=BUILDER.defineEnum("exampleAnchorX",Anchor.X.LEFT);
		EXAMPLE_ANCHOR_Y=BUILDER.defineEnum("exampleAnchorY",Anchor.Y.TOP);
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}