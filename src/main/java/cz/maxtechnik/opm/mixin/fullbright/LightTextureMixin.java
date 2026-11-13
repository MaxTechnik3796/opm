package cz.maxtechnik.opm.mixin.fullbright;

import com.mojang.blaze3d.platform.NativeImage;
import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.util.OpmUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LightTexture.class)
public class LightTextureMixin{
	@Final
	@Shadow
	private DynamicTexture lightTexture;
	@Final
	@Shadow
	private Minecraft minecraft;
	@Shadow
	private boolean updateLightTexture;
	@Unique
	private boolean opm$wasFullbright=false;
	@Inject(method="updateLightTexture", at=@At("HEAD"), cancellable=true)
	private void opm$updateLightTexture(float partialTicks,CallbackInfo ci){
		if(OpmModKeys.fullbright){
			if(!this.opm$wasFullbright){
				if(this.lightTexture!=null){
					NativeImage pixels=this.lightTexture.getPixels();
					if(pixels!=null){
						for(int sky=0;sky<16;sky++){
							for(int block=0;block<16;block++){
								pixels.setPixelRGBA(block,sky,0xFFFFFFFF);
							}
						}
						this.lightTexture.upload();
					}
				}
				if(this.minecraft.player!=null) this.minecraft.player.displayClientMessage(OpmUtil.textOnOff("info.opm.fullbright",true),true);
				this.opm$wasFullbright=true;
			}
			ci.cancel();
		}else if(this.opm$wasFullbright){
			this.opm$wasFullbright=false;
			// Force vanilla recalculation even if toggled while paused or before next tick
			this.updateLightTexture=true;
			if(this.minecraft.player!=null) this.minecraft.player.displayClientMessage(OpmUtil.textOnOff("info.opm.fullbright",false),true);
		}
	}
}
