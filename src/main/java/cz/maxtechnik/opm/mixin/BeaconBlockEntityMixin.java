package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.handler.BeaconTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin{
	@Inject(method="setLevel", at=@At("TAIL"))
	private void opm$onSetLevel(Level level,CallbackInfo ci){
		if(level!=null&&level.isClientSide()){
			BeaconTracker.BEACONS.add((BeaconBlockEntity)(Object)this);
		}
	}
	@Inject(method="setRemoved", at=@At("TAIL"))
	private void opm$onSetRemoved(CallbackInfo ci){
		BeaconTracker.BEACONS.remove((BeaconBlockEntity)(Object)this);
	}
}