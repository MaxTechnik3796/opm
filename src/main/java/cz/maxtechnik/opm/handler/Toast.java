package cz.maxtechnik.opm.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.config.OpmModConfig;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ToastAddEvent;

import java.util.Locale;
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class Toast{
	@SubscribeEvent
	public static void onToastAdd(ToastAddEvent event){
		if(isBlockedToast(event.getToast())) event.setCanceled(true);
	}
	public static boolean isBlockedToast(net.minecraft.client.gui.components.toasts.Toast toast){
		if(OpmModConfig.NO_TOASTS.get()) return true;
		if(OpmModConfig.NO_RECIPE_BOOK.get()){
			if(toast instanceof RecipeToast) return true;
			String name=toast.getClass().getName().toLowerCase(Locale.ROOT);
			return name.contains("recipetoast")||name.contains("recipe_toast");
		}
		return false;
	}
}