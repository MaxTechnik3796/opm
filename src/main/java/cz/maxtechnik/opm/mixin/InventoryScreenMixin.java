package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin{
	@Inject(method="init", at=@At("TAIL"))
	private void removeRecipeBookButton(CallbackInfo ci){
		if(!OpmConfig.NO_RECIPE_BOOK.get()) return;
		InventoryScreen self=(InventoryScreen)(Object)this;
		self.children().forEach(widget->{
			if(widget instanceof ImageButton btn){
				btn.visible=false;
				btn.active=false;
			}
		});
	}
}