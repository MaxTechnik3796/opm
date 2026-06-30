package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void onGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        // Pokud jsme v AFK módu, vnutíne enginu limit pouhých 10 snímků za vteřinu
        if (HeadlessModeHandler.isHeadlessMode()) {
            cir.setReturnValue(10);
        }
    }
}