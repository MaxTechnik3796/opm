package cz.maxtechnik.opm.mixin.headless_mode;

import cz.maxtechnik.opm.handler.HeadlessMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class ScreenFpsMixin{
	@Shadow
	public Screen screen;
	@Inject(method="getFramerateLimit", at=@At("HEAD"), cancellable=true)
	private void onGetFramerateLimit(CallbackInfoReturnable<Integer> cir){
		if(HeadlessMode.isActive()) cir.setReturnValue(1);
	}
	/*@Inject(method="setScreen", at=@At("HEAD"), cancellable=true)
	private void onSetScreen(Screen newScreen,CallbackInfo ci){
		if(this.screen instanceof HeadlessAfkScreen afkScreen&&HeadlessModeHandler.active){
			if(!afkScreen.forceClose){
				if(newScreen instanceof HeadlessAfkScreen) return;
				HeadlessModeHandler.savedScreen=newScreen;
				ci.cancel();
			}
		}
	}*/
}