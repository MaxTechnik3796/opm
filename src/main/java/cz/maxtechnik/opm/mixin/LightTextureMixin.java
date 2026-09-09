package cz.maxtechnik.opm.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import cz.maxtechnik.opm.client.handler.FullbrightHandler;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@SuppressWarnings("all")
@Mixin(LightTexture.class)
public class LightTextureMixin{
	@Final
	@Shadow
	private DynamicTexture lightTexture;
	@Unique
	private boolean opm$wasFullbright=false;
	@Inject(method="updateLightTexture", at=@At("HEAD"), cancellable=true)
	private void opm$injectFullbright(float _partialTicks,CallbackInfo ci){
		boolean isActive=FullbrightHandler.isActive();
		boolean dirty=FullbrightHandler.consumeDirty();
		if(isActive){
			if(!opm$wasFullbright||dirty){
				if(this.lightTexture!=null&&this.lightTexture.getPixels()!=null){
					NativeImage pixels=this.lightTexture.getPixels();
					for(int sky=0;sky<16;sky++){
						for(int block=0;block<16;block++){
							pixels.setPixelRGBA(block,sky,0xFFFFFFFF);
						}
					}
					this.lightTexture.upload();
				}
				opm$wasFullbright=true;
			}
			ci.cancel();
		}else if(opm$wasFullbright){
			opm$wasFullbright=false;
		}
	}
}
