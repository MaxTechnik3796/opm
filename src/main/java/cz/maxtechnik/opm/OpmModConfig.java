package cz.maxtechnik.opm;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmModConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue DEBUG;
	public static final ModConfigSpec.BooleanValue NO_REALMS_BUTTON;
	static{
		DEBUG=BUILDER.define("debug",false);
		BUILDER.push("general");
		NO_REALMS_BUTTON=BUILDER.comment("Hides Realms button in main menu.").define("noRealmsButton",true);
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}