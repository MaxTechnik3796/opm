package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.afk.HeadlessModeHandler;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin{
	@Inject(method="tick", at=@At("HEAD"), cancellable=true)
	private void onTick(CallbackInfo ci){
		if(HeadlessModeHandler.isHeadlessMode()){
			ci.cancel();
		}
	}
}
