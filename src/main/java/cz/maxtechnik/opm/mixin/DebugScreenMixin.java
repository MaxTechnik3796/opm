package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.overlay.CustomDebugOverlay;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin{
	@Inject(method="renderLines", at=@At("HEAD"), cancellable=true)
	private void opm$onRenderLines(GuiGraphics guiGraphics,List<String> lines,boolean leftSide,CallbackInfo ci){
		if(!OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return;
		if(leftSide){
			List<String> customLeft=CustomDebugOverlay.getLeftLines();
			CustomDebugOverlay.renderCustomLines(guiGraphics,customLeft,true);
		}else{
			List<String> customRight=CustomDebugOverlay.getRightLines(lines);
			CustomDebugOverlay.renderCustomLines(guiGraphics,customRight,false);
		}
		ci.cancel();
	}
}