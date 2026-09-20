package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class RecipeBookHandler{
	@SubscribeEvent
	public static void onScreenInit(ScreenEvent.Init.Post event){
		if(!OpmConfig.SPEC.isLoaded()||!OpmConfig.NO_RECIPE_BOOK.get()) return;
		if(!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
		List<GuiEventListener> toRemove=new ArrayList<>();
		for(GuiEventListener widget: event.getListenersList()){
			if(widget instanceof RecipeBookComponent||(widget instanceof AbstractWidget aw&&aw.getWidth()==20&&aw.getHeight()==18)){
				toRemove.add(widget);
			}
		}
		for(GuiEventListener widget: toRemove){
			event.removeListener(widget);
			if(widget instanceof AbstractWidget aw){
				aw.visible=false;
				aw.active=false;
			}
		}
	}
}