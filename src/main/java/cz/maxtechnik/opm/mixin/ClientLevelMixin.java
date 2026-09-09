package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.afk.HeadlessModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public class ClientLevelMixin{
	@Inject(method="tickNonPassenger", at=@At("HEAD"), cancellable=true)
	private void onTickNonPassenger(Entity entity,CallbackInfo ci){
		if(!HeadlessModeHandler.isHeadlessMode()) return;
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null&&entity==mc.player) return;
		ci.cancel();
	}
	@Inject(method="doAnimateTick", at=@At("HEAD"), cancellable=true)
	private void onDoAnimateTick(CallbackInfo ci){
		if(HeadlessModeHandler.isHeadlessMode()){
			ci.cancel();
		}
	}
}
