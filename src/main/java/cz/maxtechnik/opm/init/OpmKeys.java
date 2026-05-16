package cz.maxtechnik.opm.init;

import com.mojang.blaze3d.platform.InputConstants;
import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.handler.FullbrightHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class OpmKeys{
	public static final String CATEGORY="key.categories.opm";
	public static final KeyMapping TOGGLE_FULLBRIGHT=new KeyMapping(
			"key.opm.toggle_fullbright",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F7,
			CATEGORY
	);
	public static final KeyMapping OPEN_INSPECTOR=new KeyMapping(
			"key.opm.open_inspector",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_I,
			CATEGORY
	);
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event){
		event.register(TOGGLE_FULLBRIGHT);
		event.register(OPEN_INSPECTOR);
	}
	// Tick handler na klávesy - stejný styl jako DifModKeys
	@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
	public static class ClientTickHandler{
		@SubscribeEvent
		public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
			Minecraft mc = Minecraft.getInstance();
			if(mc.player==null) return;
			
			while(TOGGLE_FULLBRIGHT.consumeClick()){
				FullbrightHandler.toggle();
			}

		}
	}
}