package cz.maxtechnik.opm.client.recipe;

import net.minecraft.world.item.ItemStack;

public class CrushingOutput {
    public ItemStack stack;
    public float chance;
    public int   count;

    public CrushingOutput() {
        this.stack  = ItemStack.EMPTY;
        this.chance = 1.0f;
        this.count  = 1;
    }


    public boolean isEmpty() { return stack == null || stack.isEmpty(); }
}