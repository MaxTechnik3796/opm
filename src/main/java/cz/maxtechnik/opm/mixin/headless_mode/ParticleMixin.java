package cz.maxtechnik.opm.mixin.headless_mode;

import cz.maxtechnik.opm.handler.HeadlessMode;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public class ParticleMixin{
	@Inject(method="tick", at=@At("HEAD"), cancellable=true)
	private void onTick(CallbackInfo ci){
		if(HeadlessMode.isActive()) ci.cancel();
	}
}