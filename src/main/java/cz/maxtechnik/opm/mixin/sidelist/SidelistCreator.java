package cz.maxtechnik.opm.mixin.sidelist;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface SidelistCreator{
    @Invoker("displayScoreboardSidebar")
    void opm$displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective);
}