package cz.maxtechnik.opm.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor{
	@Accessor("leftPos")
	int opm$getLeftPos();

	@Accessor("leftPos")
	void opm$setLeftPos(int leftPos);

	@Accessor("imageWidth")
	int opm$getImageWidth();
}
