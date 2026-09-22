package cz.maxtechnik.opm;

import net.neoforged.neoforge.common.ModConfigSpec;
public class OpmConfig{
	public static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue DEBUG;
	static{
		DEBUG=BUILDER.define("debug", false);
		SPEC=BUILDER.build();
	}
}