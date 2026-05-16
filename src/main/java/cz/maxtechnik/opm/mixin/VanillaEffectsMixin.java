package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class VanillaEffectsMixin {

    @Inject(
            method = "renderEffects",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hideVanillaEffects(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (OpmConfig.EFFECTS_HUD_ENABLED.get()) {
            ci.cancel();
        }
    }
}