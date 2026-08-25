package cz.maxtechnik.opm.client.editor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Modul pro skenování a inicializaci registrů předmětů a tagů v Minecraftu.
 */
public final class ItemScanner {
	private ItemScanner() {}

	public static void loadAllItems(List<ItemStack> allItems) {
		allItems.clear();
		for (Item item : BuiltInRegistries.ITEM) {
			allItems.add(new ItemStack(item));
		}
	}

	public static void loadTags(List<ItemStack> cachedTags) {
		cachedTags.clear();
		BuiltInRegistries.ITEM.getTags().map(com.mojang.datafixers.util.Pair::getFirst).forEach(tagKey -> {
			ItemStack stack = new ItemStack(Items.NAME_TAG);
			stack.set(DataComponents.CUSTOM_NAME, Component.literal("#" + tagKey.location()));
			cachedTags.add(stack);
		});
	}
}
