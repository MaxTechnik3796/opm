package cz.maxtechnik.opm.client.recipe;

import net.minecraft.world.item.ItemStack;

/**
 * Jeden výstupní slot pro Crushing / Milling recept.
 * chance == 1.0 → vždy; chance < 1.0 → pravděpodobnost (např. 0.5 = 50 %)
 */
public class CrushingOutput {
    public ItemStack stack;
    public float chance;   // 0.0 – 1.0; 1.0 = guaranteed
    public int   count;

    public CrushingOutput() {
        this.stack  = ItemStack.EMPTY;
        this.chance = 1.0f;
        this.count  = 1;
    }

    public CrushingOutput(ItemStack stack, float chance, int count) {
        this.stack  = stack;
        this.chance = chance;
        this.count  = count;
    }

    public boolean isEmpty() { return stack == null || stack.isEmpty(); }
}