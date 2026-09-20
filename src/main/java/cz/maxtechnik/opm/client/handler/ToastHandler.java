package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ToastAddEvent;

import java.util.Locale;

@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class ToastHandler{
	@SubscribeEvent
	public static void onToastAdd(ToastAddEvent event){
		if(!OpmConfig.SPEC.isLoaded()) return;
		if(isBlockedToast(event.getToast())){
			event.setCanceled(true);
		}
	}

	public static boolean isBlockedToast(Toast toast){
		if(toast==null||!OpmConfig.SPEC.isLoaded()) return false;

		if(OpmConfig.HIDE_TUTORIAL_TOAST.get()){
			if(toast instanceof TutorialToast) return true;
			String name=toast.getClass().getName().toLowerCase(Locale.ROOT);
			if(name.contains("tutorial")) return true;
		}

		if(OpmConfig.NO_RECIPE_BOOK.get()){
			if(toast instanceof RecipeToast) return true;
			String name=toast.getClass().getName().toLowerCase(Locale.ROOT);
			if(name.contains("recipetoast")||name.contains("recipe_toast")){
				return true;
			}
		}

		return false;
	}
}
