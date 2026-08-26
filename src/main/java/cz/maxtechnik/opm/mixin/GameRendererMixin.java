package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.handler.F1Handler;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Redirect(method = "renderItemInHand", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;hideGui:Z", opcode = Opcodes.GETFIELD))
	private boolean opm$redirectHideGuiInHand(Options options) {
		if (F1Handler.shouldShowHandInF1()) {
			return false;
		}
		return options.hideGui;
	}
}
