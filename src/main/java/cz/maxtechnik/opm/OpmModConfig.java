package cz.maxtechnik.opm;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmModConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue DEBUG;
	public static final ModConfigSpec.BooleanValue REMOVE_REALMS_BUTTON;
	public static final ModConfigSpec.BooleanValue REMOVE_TELEMETRY_BUTTON;
	static{
		DEBUG=BUILDER.define("debug",false);
		BUILDER.push("buttonRemover");
		REMOVE_REALMS_BUTTON=BUILDER.comment("Hides Realms button in main menu.").define("removeRealmsButton",true);
		REMOVE_TELEMETRY_BUTTON=BUILDER.comment("Hides Telemetry button in options.").define("removeTelemetryButton",true);
		BUILDER.pop();
		SPEC=BUILDER.build();
	}
}