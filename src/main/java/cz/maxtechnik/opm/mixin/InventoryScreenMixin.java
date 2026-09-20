package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.handler.RecipeBookHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin{
	@Inject(method="init", at=@At("TAIL"))
	private void removeRecipeBookButton(CallbackInfo ci){
		InventoryScreen self=(InventoryScreen)(Object)this;
		RecipeBookHandler.cleanScreen(self,null);
	}
}