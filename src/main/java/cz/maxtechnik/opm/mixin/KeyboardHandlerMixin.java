package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.handler.DebugScreenState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void onDebugKey(int key, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();

        // F3+4 - toggle plné tagy vs jen počet
        // GLFW_KEY_4 = 52
        if (key == 52) {
            DebugScreenState.showFullTags = !DebugScreenState.showFullTags;
            mc.gui.getChat().addMessage(
                    Component.literal("[OPM] Full tags: "
                            + (DebugScreenState.showFullTags ? "ON" : "OFF"))
            );
            cir.setReturnValue(true);
        }
    }
}