package cz.maxtechnik.opm.mixin.overlayer_control;

import com.mojang.blaze3d.systems.RenderSystem;
import cz.maxtechnik.opm.config.OpmModConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public class SpyglassOverlayMixin{
	@Unique
	private static final ResourceLocation SPYGLASS_SCOPE_LOCATION=ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png");
	@Inject(method="renderSpyglassOverlay", at=@At("HEAD"), cancellable=true)
	private void onRenderSpyglassOverlay(GuiGraphics graphics,float scale,CallbackInfo ci){
		switch(OpmModConfig.SPYGLASS_OVERLAY.get()){
			case HIDDEN -> ci.cancel();
			case TRANSPARENT -> {
				ci.cancel();
				opm$renderTransparentSpyglass(graphics,scale);
			}
			default -> {
			}
		}
	}
	@Unique
	private void opm$renderTransparentSpyglass(GuiGraphics graphics,float scale){
		float f=(float)Math.min(graphics.guiWidth(),graphics.guiHeight());
		float f1=f;
		float f2=Math.min((float)graphics.guiWidth()/f,(float)graphics.guiHeight()/f1)*scale;
		int i=Mth.floor(f*f2);
		int j=Mth.floor(f1*f2);
		int k=(graphics.guiWidth()-i)/2;
		int l=(graphics.guiHeight()-j)/2;
		int i1=k+i;
		int j1=l+j;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1F,1F,1F,0.5F);
		graphics.blit(SPYGLASS_SCOPE_LOCATION,k,l,-90,0F,0F,i,j,i,j);
		RenderSystem.setShaderColor(1F,1F,1F,1F);
		RenderSystem.disableBlend();
		graphics.fill(RenderType.guiOverlay(),0,j1,graphics.guiWidth(),graphics.guiHeight(),-90,0x80000000);
		graphics.fill(RenderType.guiOverlay(),0,0,graphics.guiWidth(),l,-90,0x80000000);
		graphics.fill(RenderType.guiOverlay(),0,l,k,j1,-90,0x80000000);
		graphics.fill(RenderType.guiOverlay(),i1,l,graphics.guiWidth(),j1,-90,0x80000000);
	}
}
