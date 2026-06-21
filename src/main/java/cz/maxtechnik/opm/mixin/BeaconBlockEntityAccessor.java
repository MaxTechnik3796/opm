package cz.maxtechnik.opm.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor{
	@Accessor("levels")
	int getLevels();
	@Accessor("primaryPower")
	Holder<MobEffect> getPrimaryPower();
}