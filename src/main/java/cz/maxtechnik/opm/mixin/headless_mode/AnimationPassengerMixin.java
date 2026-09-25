package cz.maxtechnik.opm.mixin.headless_mode;

import cz.maxtechnik.opm.modul.HeadlessMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public class AnimationPassengerMixin{
	@Inject(method="tickNonPassenger", at=@At("HEAD"), cancellable=true)
	private void onTickNonPassenger(Entity entity,CallbackInfo ci){
		if(!HeadlessMode.isActive()) return;
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null&&entity==mc.player) return;
		ci.cancel();
	}
	@Inject(method="doAnimateTick", at=@At("HEAD"), cancellable=true)
	private void onDoAnimateTick(CallbackInfo ci){
		if(HeadlessMode.isActive()) ci.cancel();
	}
}