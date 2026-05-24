package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
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
		OpmConfig.PumpkinMode mode=OpmConfig.PUMPKIN_OVERLAY.get();
		switch(mode){
			case HIDDEN -> ci.cancel();
			case TRANSPARENT -> {
				ci.cancel();
				opm$renderTransparentPumpkin(graphics);
			}
			case NORMAL -> {
			}
		}
	}
	@Unique
	private void opm$renderTransparentPumpkin(GuiGraphics graphics){
		Minecraft mc=Minecraft.getInstance();
		int w=mc.getWindow().getGuiScaledWidth();
		int h=mc.getWindow().getGuiScaledHeight();

		//Renderuj dýni s nižší opacitou (přibližně 30% průhledná)
		//setShaderColor nastaví alpha kanál pro následující render
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F,1F,1F,0.5F);
		graphics.blit(ResourceLocation.withDefaultNamespace("textures/misc/pumpkinblur.png"),0,0,-90,0F,0F,w,h,w,h);
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1F,1F,1F,1F);
		com.mojang.blaze3d.systems.RenderSystem.disableBlend();
	}
}