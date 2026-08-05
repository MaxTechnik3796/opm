package cz.maxtechnik.opm.client.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Modul pro skenování a načítání dostupných fluidů a bucketů z registrů a zavedených modů.
 */
public final class FluidLoader {
	private FluidLoader() {}

	public static void loadFluids(List<ItemStack> availableFluids) {
		availableFluids.clear();
		availableFluids.add(new ItemStack(Items.WATER_BUCKET));
		availableFluids.add(new ItemStack(Items.LAVA_BUCKET));
		availableFluids.add(new ItemStack(Items.MILK_BUCKET));

		if (ModList.get().isLoaded("create")) {
			tryAddBucket("create:honey_bucket", availableFluids);
			tryAddBucket("create:chocolate_bucket", availableFluids);
		}

		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack s = new ItemStack(item);
			if (!s.isEmpty()) {
				String id = BuiltInRegistries.ITEM.getKey(item).toString();
				boolean isBucket = item instanceof net.minecraft.world.item.BucketItem || id.endsWith("_bucket") || id.contains("bucket");
				if (isBucket && !id.equals("minecraft:bucket") && availableFluids.stream().noneMatch(f -> ItemStack.isSameItem(f, s))) {
					availableFluids.add(s);
				}
			}
		}
	}

	public static boolean isFluidItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		Item item = stack.getItem();
		if (item instanceof net.minecraft.world.item.BucketItem) return true;
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		if (id.endsWith("_bucket") || id.contains("bucket") || id.contains("_fluid") || id.endsWith("_fluid")) return true;
		try {
			return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static void tryAddBucket(String id, List<ItemStack> availableFluids) {
		try {
			Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
			if (item != Items.AIR) availableFluids.add(new ItemStack(item));
		} catch (Exception ignored) {}
	}
}
