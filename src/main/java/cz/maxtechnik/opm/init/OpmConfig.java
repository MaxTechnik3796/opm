package cz.maxtechnik.opm.init;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
	public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;
	public static final ModConfigSpec.BooleanValue CUSTOM_DEBUG_SCREEN;
	static{
		BUILDER.push("ui");
		NO_RECIPE_BOOK=BUILDER
				.comment(
						"Hides Recipe Book button in inventory and crafting."
				)
				.define("noRecipeBook",true);
		NO_REALMS_BUTTON=BUILDER
				.comment(
						"Hides Realms button in main menu."
				)
				.define("noRealmsButton",true);
		CUSTOM_DEBUG_SCREEN=BUILDER
				.comment(
						"Replaces vanilla F3 debug screen with a custom one and enables F3+4 shortcut to toggle full tags."
				)
				.define("customDebugScreen",true);
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}