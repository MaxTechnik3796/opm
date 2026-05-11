package cz.maxtechnik.opm.init;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue NO_RECIPE_BOOK;
	public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;
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
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}