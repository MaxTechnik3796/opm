package cz.maxtechnik.opm.mixin.overlayer_control;

import com.mojang.blaze3d.systems.RenderSystem;
import cz.maxtechnik.opm.config.OpmModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public class PumpkinOverlayMixin{
	@Unique
	private static final ResourceLocation PUMPKIN_BLUR_LOCATION=ResourceLocation.withDefaultNamespace("textures/misc/pumpkinblur.png");
	@Inject(method="renderTextureOverlay", at=@At("HEAD"), cancellable=true)
	private void onRenderTextureOverlay(GuiGraphics graphics,ResourceLocation texture,float alpha,CallbackInfo ci){
		if(!texture.equals(PUMPKIN_BLUR_LOCATION)) return;
		switch(OpmModConfig.PUMPKIN_OVERLAY.get()){
			case HIDDEN -> ci.cancel();
			case TRANSPARENT -> {
				ci.cancel();
				opm$renderTransparentPumpkin(graphics);
			}
			default -> {
			}
		}
	}
	@Unique
	private void opm$renderTransparentPumpkin(GuiGraphics graphics){
		Minecraft mc=Minecraft.getInstance();
		int w=mc.getWindow().getGuiScaledWidth();
		int h=mc.getWindow().getGuiScaledHeight();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1F,1F,1F,0.5F);
		graphics.blit(PUMPKIN_BLUR_LOCATION,0,0,-90,0F,0F,w,h,w,h);
		RenderSystem.setShaderColor(1F,1F,1F,1F);
		RenderSystem.disableBlend();
	}
}