package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (HeadlessModeHandler.isHeadlessMode()) {
            // Vyčistíme hlavní okno na čistou černou (16640 = GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
            com.mojang.blaze3d.systems.RenderSystem.clear(16640, Minecraft.ON_OSX);
            
            // Uřízneme kompletně celý zbytek renderu (nevykreslí se svět, UI, stíny, nic)
            ci.cancel();
        }
    }
}