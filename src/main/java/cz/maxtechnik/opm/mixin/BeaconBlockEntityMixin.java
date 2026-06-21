package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.handler.BeaconVisualizerHandler;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {

	// Tato trojice potlačení varování spolehlivě utiší editor i Mixin kompilátor:
	// "unused" - skryje hlášku, že metodu nikdo v projektu nevolá
	// "Unique" - řekne Mixinu, že záměrně nechceme @Unique, protože přepisujeme metodu z NeoForge
	// "MixinNamePattern" - vypne kontrolu formátu názvu (nechceme předponu opm$)
	@Unique
	@SuppressWarnings({"unused", "Unique", "MixinNamePattern"})
	public AABB opm$getRenderBoundingBox() {
		BlockEntity self = (BlockEntity) (Object) this;

		if (BeaconVisualizerHandler.isActive()) {
			// Pokud je vizualizér aktivní, vrátíme obří box (65 bloků do stran, 320 na výšku)
			// Sodium i vanila engine zónu vykreslí ze všech úhlů bez mizení
			return new AABB(self.getBlockPos()).inflate(65.0, 320.0, 65.0);
		}

		// V opačném případě vracíme standardní malý box majáku
		return new AABB(self.getBlockPos());
	}
}