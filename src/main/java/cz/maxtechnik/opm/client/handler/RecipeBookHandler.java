package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmConfig;
import cz.maxtechnik.opm.mixin.AbstractContainerScreenAccessor;
import cz.maxtechnik.opm.mixin.ImageButtonAccessor;
import cz.maxtechnik.opm.mixin.RecipeBookComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class RecipeBookHandler{
	@SubscribeEvent
	public static void onScreenInit(ScreenEvent.Init.Post event){
		if(!OpmConfig.SPEC.isLoaded()) return;
		if(!OpmConfig.NO_RECIPE_BOOK.get()) return;
		cleanScreen(event.getScreen(),event);
	}

	public static void cleanScreen(Screen screen,@Nullable ScreenEvent.Init.Post event){
		if(!OpmConfig.SPEC.isLoaded()) return;
		if(!OpmConfig.NO_RECIPE_BOOK.get()) return;

		List<GuiEventListener> currentWidgets=event!=null?event.getListenersList():new ArrayList<>(screen.children());
		List<GuiEventListener> toRemove=new ArrayList<>();

		for(GuiEventListener widget: currentWidgets){
			if(isRecipeWidget(widget,screen)){
				toRemove.add(widget);
			}
		}

		for(GuiEventListener widget: toRemove){
			if(event!=null){
				event.removeListener(widget);
			}
			if(widget instanceof AbstractWidget aw){
				aw.visible=false;
				aw.active=false;
			}
		}

		if(screen instanceof RecipeUpdateListener listener){
			RecipeBookComponent comp=listener.getRecipeBookComponent();
			if(comp!=null){
				try{
					((RecipeBookComponentAccessor)comp).opm$setVisible(false);
				}catch(Throwable ignored){
				}
				if(event!=null){
					event.removeListener(comp);
				}
			}
		}

		if(screen instanceof AbstractContainerScreen<?> container&&(screen instanceof RecipeUpdateListener||hasRecipeBookComponent(screen))){
			try{
				AbstractContainerScreenAccessor acc=(AbstractContainerScreenAccessor)container;
				int expectedLeft=(container.width-acc.opm$getImageWidth())/2;
				if(acc.opm$getLeftPos()!=expectedLeft){
					acc.opm$setLeftPos(expectedLeft);
				}
			}catch(Throwable ignored){
			}
		}
	}

	public static boolean isRecipeWidget(GuiEventListener widget,Screen screen){
		if(widget instanceof RecipeBookComponent){
			return true;
		}

		if(widget instanceof ImageButton btn){
			try{
				WidgetSprites sprites=((ImageButtonAccessor)btn).opm$getSprites();
				if(sprites!=null){
					if(sprites.equals(RecipeBookComponent.RECIPE_BUTTON_SPRITES)){
						return true;
					}
					if(sprites.enabled()!=null){
						String path=sprites.enabled().getPath().toLowerCase(Locale.ROOT);
						if(path.contains("recipe_book")||path.contains("recipe")){
							return true;
						}
					}
				}
			}catch(Throwable ignored){
			}

			if(btn.getWidth()==20&&btn.getHeight()==18){
				if(screen instanceof RecipeUpdateListener||screen instanceof AbstractContainerScreen<?>||hasRecipeBookComponent(screen)){
					return true;
				}
			}
		}

		if(widget instanceof AbstractWidget aw){
			String className=aw.getClass().getSimpleName().toLowerCase(Locale.ROOT);
			if(className.contains("recipebook")||className.contains("recipebutton")){
				return true;
			}

			if(aw.getTooltip()!=null){
				try{
					List<FormattedCharSequence> lines=aw.getTooltip().toCharSequence(Minecraft.getInstance());
					if(lines!=null){
						StringBuilder sb=new StringBuilder();
						for(FormattedCharSequence line: lines){
							line.accept((index,style,codePoint)->{
								sb.appendCodePoint(codePoint);
								return true;
							});
						}
						String tip=sb.toString().toLowerCase(Locale.ROOT);
						if(tip.contains("recipe")||tip.contains("recept")){
							return true;
						}
					}
				}catch(Throwable ignored){
				}
			}

			if(aw.getMessage()!=null){
				String msg=aw.getMessage().getString().toLowerCase(Locale.ROOT);
				if(msg.contains("recipe")||msg.contains("recept")){
					return true;
				}
			}

			if(aw.getWidth()==20&&aw.getHeight()==18){
				if(screen instanceof RecipeUpdateListener||hasRecipeBookComponent(screen)){
					return true;
				}
			}
		}

		return false;
	}

	public static boolean hasRecipeBookComponent(Screen screen){
		if(screen instanceof RecipeUpdateListener) return true;
		for(GuiEventListener child: screen.children()){
			if(child instanceof RecipeBookComponent) return true;
		}
		return false;
	}
}