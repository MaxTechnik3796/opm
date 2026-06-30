package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    // Blokace klikání
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void onMousePress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (HeadlessModeHandler.isHeadlessMode()) {
            ci.cancel();
        }
    }

    // Blokace pohybu myši (kamera se ani nehne)
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        if (HeadlessModeHandler.isHeadlessMode()) {
            ci.cancel();
        }
    }
    
    // Blokace kolečka myši
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (HeadlessModeHandler.isHeadlessMode()) {
            ci.cancel();
        }
    }
}