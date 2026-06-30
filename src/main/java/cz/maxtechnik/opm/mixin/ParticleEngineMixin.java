package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    /**
     * Pokud je aktivní Headless AFK mód, přeskočíme veškeré tikání částic.
     * Ušetří to zbytečné CPU cykly – nikdo ty částice stejně nevidí.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (HeadlessModeHandler.isHeadlessMode()) {
            ci.cancel();
        }
    }
}
