package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderLevel(CallbackInfo ci) {
        // Pokud je aktivní náš Headless screen, okamžitě zrušíme render celého 3D světa
        if (HeadlessModeHandler.isHeadlessMode()) {
            ci.cancel();
        }
    }
}