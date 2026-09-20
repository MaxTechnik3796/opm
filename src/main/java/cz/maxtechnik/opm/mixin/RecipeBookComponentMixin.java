package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin{
	@Inject(method="isVisible", at=@At("HEAD"), cancellable=true)
	private void opm$neverVisible(CallbackInfoReturnable<Boolean> cir){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.NO_RECIPE_BOOK.get()){
			cir.setReturnValue(false);
		}
	}

	@Inject(method="isVisibleAccordingToBookData", at=@At("HEAD"), cancellable=true)
	private void opm$neverVisibleBookData(CallbackInfoReturnable<Boolean> cir){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.NO_RECIPE_BOOK.get()){
			cir.setReturnValue(false);
		}
	}

	@Inject(method="updateScreenPosition", at=@At("HEAD"), cancellable=true)
	private void opm$alwaysCentered(int width,int imageWidth,CallbackInfoReturnable<Integer> cir){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.NO_RECIPE_BOOK.get()){
			cir.setReturnValue((width-imageWidth)/2);
		}
	}

	@Inject(method="render", at=@At("HEAD"), cancellable=true)
	private void opm$noRender(GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.NO_RECIPE_BOOK.get()){
			ci.cancel();
		}
	}

	@Inject(method="renderGhostRecipe", at=@At("HEAD"), cancellable=true)
	private void opm$noGhostRecipe(GuiGraphics guiGraphics,int leftPos,int topPos,boolean isNarrow,float partialTick,CallbackInfo ci){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.NO_RECIPE_BOOK.get()){
			ci.cancel();
		}
	}

	@Inject(method="toggleVisibility", at=@At("HEAD"), cancellable=true)
	private void opm$noToggle(CallbackInfo ci){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.NO_RECIPE_BOOK.get()){
			ci.cancel();
		}
	}
}
