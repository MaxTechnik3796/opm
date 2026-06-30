package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import cz.maxtechnik.opm.client.handler.DebugScreenState;
import cz.maxtechnik.opm.client.handler.F1Handler;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin{
	@Inject(method="handleDebugKeys", at=@At("HEAD"), cancellable=true)
	private void onDebugKey(int key,CallbackInfoReturnable<Boolean> cir){
		if(!OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return;
		//F3+4 - toggle plné tagy vs jen počet
		if(key==52){
			DebugScreenState.showFullTags=!DebugScreenState.showFullTags;
			cir.setReturnValue(true);
		}
	}
	@Inject(method="keyPress", at=@At("HEAD"), cancellable=true)
	private void onKeyPress(long windowPointer,int key,int scanCode,int action,int modifiers,CallbackInfo ci){
		if (HeadlessModeHandler.isHeadlessMode()) {
			// Pokud stisknutá klávesa NENÍ naše AFK klávesa, vstup kompletně zablokujeme
			if (key != HeadlessModeHandler.AFK_KEY.getKey().getValue()) {
				ci.cancel();
			}
		}else{
			Minecraft mc=Minecraft.getInstance();
			if(mc.player!=null&&mc.screen==null&&OpmConfig.CUSTOM_F1.get()){
				if(key==290&&action==1){ // F1 pressed
					F1Handler.handleF1Press();
					ci.cancel();
				}
			}
		}
	}
}