package cz.maxtechnik.opm.client.recipe;

import net.minecraft.world.item.ItemStack;

public class FluidEntry {
    public ItemStack proxy = ItemStack.EMPTY;
    public int amount = 1000;

    public FluidEntry() {}

    public boolean isEmpty() {
        return proxy == null || proxy.isEmpty();
    }

    //Vrátí fluid ResourceLocation z bucket itemu (odstraní _bucket suffix)
    public String fluidId() {
        if (isEmpty()) return "minecraft:empty";
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(proxy.getItem()).toString();
        // water_bucket → minecraft:water, create:honey_bucket → create:honey
        return id.endsWith("_bucket") ? id.substring(0, id.length() - "_bucket".length()) : id;
    }
}