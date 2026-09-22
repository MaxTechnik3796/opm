package cz.maxtechnik.opm.mixin.headless_mode;

import cz.maxtechnik.opm.handler.HeadlessMode;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public class LevelRendererMixin{
	@Inject(method="renderLevel", at=@At("HEAD"), cancellable=true)
	private void onRenderLevel(CallbackInfo ci){
		if(HeadlessMode.isActive()) ci.cancel();
	}
}