package cz.maxtechnik.opm.init;

import com.mojang.blaze3d.platform.InputConstants;
import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.handler.FullbrightHandler;
import cz.maxtechnik.opm.client.handler.RegionGrid;
import cz.maxtechnik.opm.client.screen.InspectorScreen;
import cz.maxtechnik.opm.client.screen.RecipeEditorScreen;
import cz.maxtechnik.opm.client.screen.OpmConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
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
	public static final KeyMapping OPEN_RECIPE_EDITOR=new KeyMapping(
			"key.opm.open_recipe_editor",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_G,
			CATEGORY
	);
	public static final KeyMapping OPEN_CONFIG_SCREEN=new KeyMapping(
			"key.opm.open_config_screen",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			CATEGORY
	);
	public static final KeyMapping TOGGLE_REGION_GRID=new KeyMapping(
			"key.opm.toggle_region_grid",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			CATEGORY
	);
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event){
		event.register(TOGGLE_FULLBRIGHT);
		event.register(OPEN_INSPECTOR);
		event.register(OPEN_RECIPE_EDITOR);
		event.register(OPEN_CONFIG_SCREEN);
		event.register(TOGGLE_REGION_GRID);
	}
	@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
	public static class ClientTickHandler{
		@SubscribeEvent
		public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
			Minecraft mc=Minecraft.getInstance();
			if(mc.player==null) return;
			while(TOGGLE_FULLBRIGHT.consumeClick()) FullbrightHandler.toggle();
			while(OPEN_INSPECTOR.consumeClick()){

				//Otevře InspectorScreen — item v ruce, nebo první neprázdný slot v inventáři
				ItemStack stack=getRelevantStack(mc);if(!stack.isEmpty()){mc.setScreen(new InspectorScreen(stack,mc.screen));}}
			while(OPEN_RECIPE_EDITOR.consumeClick()) mc.setScreen(new RecipeEditorScreen(mc.screen));
			while(OPEN_CONFIG_SCREEN.consumeClick()) mc.setScreen(new OpmConfigScreen(mc.screen));
			while(TOGGLE_REGION_GRID.consumeClick()) RegionGrid.toggleGrid();
		}

		//Vrátí item v hlavní ruce, pak v offhandu, pak první neprázdný slot hotbaru.
		private static ItemStack getRelevantStack(Minecraft mc){
			if(mc.player==null) return ItemStack.EMPTY;
			ItemStack main=mc.player.getMainHandItem();
			if(!main.isEmpty()) return main;
			ItemStack off=mc.player.getOffhandItem();
			if(!off.isEmpty()) return off;
			for(int i=0;i<9;i++){
				ItemStack s=mc.player.getInventory().getItem(i);
				if(!s.isEmpty()) return s;
			}
			return ItemStack.EMPTY;
		}
	}
}