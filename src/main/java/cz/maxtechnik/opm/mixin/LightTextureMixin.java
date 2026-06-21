package cz.maxtechnik.opm.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import cz.maxtechnik.opm.client.handler.FullbrightHandler;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LightTexture.class)
public class LightTextureMixin{
	// Získáme přístup k samotné dynamické textuře světla
	@Final
	@Shadow
	private DynamicTexture lightTexture;
	@Inject(method="updateLightTexture", at=@At("TAIL"))
	private void injectFullbright(float partialTicks,CallbackInfo ci){
		// Pokud je Fullbright aktivní, přemažeme vypočítanou texturu na bílou
		if(FullbrightHandler.isActive()){
			if(this.lightTexture!=null&&this.lightTexture.getPixels()!=null){
				NativeImage pixels=this.lightTexture.getPixels();
				// Světelná mapa v MC je čtverec 16x16 (osa X je block light, osa Y je sky light)
				for(int sky=0;sky<16;sky++){
					for(int block=0;block<16;block++){
						// 0xFFFFFFFF je hexadecimální zápis pro plnou bílou barvu (plný jas)
						pixels.setPixelRGBA(block,sky,0xFFFFFFFF);
					}
				}
				// Nahrajeme upravené pixely znovu do paměti grafické karty
				this.lightTexture.upload();
			}
		}
	}
}