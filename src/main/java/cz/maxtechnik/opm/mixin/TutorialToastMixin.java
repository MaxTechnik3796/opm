package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TutorialToast.class)
public class TutorialToastMixin{
	@Inject(method="render", at=@At("HEAD"), cancellable=true)
	private void onRender(GuiGraphics graphics,ToastComponent toastComponent,long time,CallbackInfoReturnable<Toast.Visibility> cir){
		if(OpmConfig.HIDE_TUTORIAL_TOAST.get()) cir.setReturnValue(Toast.Visibility.HIDE);
	}
}