package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.screen.InspectorScreen;
import cz.maxtechnik.opm.init.OpmKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin{
	@Inject(method="keyPressed", at=@At("HEAD"), cancellable=true)
	private void onKeyPressed(int keyCode,int scanCode,int modifiers,CallbackInfoReturnable<Boolean> cir){
		AbstractContainerScreen<?> self=(AbstractContainerScreen<?>)(Object)this;

		//Zkontroluj jestli je stisknutá klávesa inspector keybind
		if(OpmKeys.OPEN_INSPECTOR.matches(keyCode,scanCode)){
			Minecraft mc=Minecraft.getInstance();
			ItemStack hoveredStack=self.getMenu().getCarried();
			if(hoveredStack.isEmpty()){
				var slot=self.getSlotUnderMouse();
				if(slot!=null&&!slot.getItem().isEmpty()) hoveredStack=slot.getItem();
			}
			if(!hoveredStack.isEmpty()){
				mc.setScreen(new InspectorScreen(hoveredStack,self));
				cir.setReturnValue(true);
			}
		}
	}
}