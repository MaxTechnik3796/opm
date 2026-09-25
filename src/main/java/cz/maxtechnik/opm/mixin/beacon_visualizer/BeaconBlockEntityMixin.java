package cz.maxtechnik.opm.mixin.beacon_visualizer;

import cz.maxtechnik.opm.modul.BeaconVisualizer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
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
	@Inject(method="setRemoved", at=@At("HEAD"))
	private void opm$onSetRemoved(CallbackInfo ci){
		BlockEntity be=(BlockEntity)(Object)this;
		if(be.getLevel()!=null&&be.getLevel().isClientSide()){
			BeaconVisualizer.BEACONS.remove((BeaconBlockEntity)be);
			BeaconVisualizer.clearCache(be.getBlockPos());
		}
	}
}