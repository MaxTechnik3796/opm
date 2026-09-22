package cz.maxtechnik.opm;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class OpmModKeys{
	public static final String CATEGORY="key.categories.opm";
	public static final KeyMapping DEBUG=new KeyMapping(
			"key.opm.debug",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			CATEGORY
	);
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event){
		event.register(DEBUG);
	}
	@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
	public static class ClientTickHandler{
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event){
			Minecraft mc=Minecraft.getInstance();
			if(mc.player==null) return;
			if(DEBUG.consumeClick()) OpmMod.LOGGER.info("Debug Key pressed!");
		}
	}
}
