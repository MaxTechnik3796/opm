package cz.maxtechnik.opm;

import com.mojang.blaze3d.platform.InputConstants;
import cz.maxtechnik.opm.handler.*;
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
	static final String CATEGORY="key.categories.opm";
	static InputConstants.Type INPUT=InputConstants.Type.KEYSYM;
	public static final KeyMapping CONFIG=new KeyMapping(
			"key.opm.config",INPUT,
			GLFW.GLFW_KEY_F12,
			CATEGORY
	);
	public static final KeyMapping INSPECTOR=new KeyMapping(
			"key.opm.inspector",INPUT,
			GLFW.GLFW_KEY_I,
			CATEGORY
	);
	public static final KeyMapping RECIPE_EDITOR=new KeyMapping(
			"key.opm.recipe_editor",INPUT,
			GLFW.GLFW_KEY_UNKNOWN,
			CATEGORY
	);
	public static final KeyMapping REGION_GRID=new KeyMapping(
			"key.opm.region_grid",INPUT,
			GLFW.GLFW_KEY_F8,
			CATEGORY
	);
	public static final KeyMapping BEACON_VISUALIZER=new KeyMapping(
			"key.opm.beacon_visualizer",INPUT,
			GLFW.GLFW_KEY_MENU,
			CATEGORY
	);
	public static final KeyMapping HEADLESS_MODE=new KeyMapping(
			"key.opm.headless_mode",INPUT,
			GLFW.GLFW_KEY_K,
			CATEGORY
	);
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event){
		KeyMapping[] keys={CONFIG,INSPECTOR,RECIPE_EDITOR,REGION_GRID,BEACON_VISUALIZER,HEADLESS_MODE};
		for(KeyMapping key: keys) event.register(key);
	}
	@EventBusSubscriber(modid=OpmMod.MODID, bus=EventBusSubscriber.Bus.GAME, value=Dist.CLIENT)
	public static class ClientTickHandler{
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event){
			Minecraft mc=Minecraft.getInstance();
			if(mc.player==null) return;
			while(CONFIG.consumeClick()) mc.setScreen(new Config(mc.screen));
			while(INSPECTOR.consumeClick()) Inspector.openFromPlayerHand(mc);
			while(RECIPE_EDITOR.consumeClick()) OpmMod.LOGGER.info("Recipe Editor Key pressed!");
			while(REGION_GRID.consumeClick()) RegionGrid.toggle();
			while(BEACON_VISUALIZER.consumeClick()) BeaconVisualizer.toggle();
			while(HEADLESS_MODE.consumeClick()) HeadlessMode.start();
		}
	}
}
