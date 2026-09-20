package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
