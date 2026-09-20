package cz.maxtechnik.opm.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor{
	@Invoker("setVisible")
	void opm$setVisible(boolean visible);
}
