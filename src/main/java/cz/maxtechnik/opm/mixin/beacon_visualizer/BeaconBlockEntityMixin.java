package cz.maxtechnik.opm.mixin.beacon_visualizer;

import cz.maxtechnik.opm.handler.beacon.BeaconVisualizer;
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
		if(level!=null&&level.isClientSide()) BeaconVisualizer.BEACONS.add((BeaconBlockEntity)(Object)this);
	}
	@Inject(method="setRemoved", at=@At("TAIL"))
	private void opm$onSetRemoved(CallbackInfo ci){
		BeaconVisualizer.BEACONS.remove((BeaconBlockEntity)(Object)this);
	}
}