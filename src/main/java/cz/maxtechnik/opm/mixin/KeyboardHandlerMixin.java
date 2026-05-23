package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.handler.DebugScreenState;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin{
	@Inject(method="handleDebugKeys",at=@At("HEAD"),cancellable=true)
	private void onDebugKey(int key,CallbackInfoReturnable<Boolean> cir){
		if(!OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return;
		// F3+4 - toggle plné tagy vs jen počet
		// GLFW_KEY_4 = 52
		if(key==52){
			DebugScreenState.showFullTags=!DebugScreenState.showFullTags;
			cir.setReturnValue(true);
		}
	}
}