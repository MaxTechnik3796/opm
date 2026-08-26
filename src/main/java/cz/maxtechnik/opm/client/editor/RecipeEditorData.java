package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.recipe.RecipeFileManager;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class RecipeEditorData {
	// Crafting ─────────────────────────────────────────────────────────────
	public boolean shapeless = false;
	public final List<ItemStack> craftGrid = initList(9);
	public final List<ItemStack> mechGrid = initList(81);
	public boolean mechMirrored = true;
	public ItemStack craftResult = ItemStack.EMPTY;
	public int craftCount = 1;
	public ItemStack mechResult = ItemStack.EMPTY;
	public int mechCount = 1;

	// Furnace ──────────────────────────────────────────────────────────────
	public int furnSubIdx = 0;
	public final String[] furnSubs = {"smelting", "blasting", "smoking", "campfire_cooking"};
	public final String[] furnLabels = {"Furnace", "Blast", "Smoker", "Campfire"};
	public ItemStack furnIn = ItemStack.EMPTY, furnOut = ItemStack.EMPTY;
	public int furnCount = 1, furnTime = 200;
	public float furnXp = 0.1f;

	// Stonecutter ──────────────────────────────────────────────────────────
	public ItemStack stoneIn = ItemStack.EMPTY, stoneOut = ItemStack.EMPTY;
	public int stoneCount = 1;

	// Smithing ─────────────────────────────────────────────────────────────
	public ItemStack smTemplate = ItemStack.EMPTY, smBase = ItemStack.EMPTY;
	public ItemStack smAddition = ItemStack.EMPTY, smResult = ItemStack.EMPTY;
	public int smCount = 1;

	// Mixing ───────────────────────────────────────────────────────────────
	public final List<ItemStack> mixIng = initList(9);
	public final List<FluidEntry> mixFluidIng = initFluidList(2);
	public final List<CrushingOutput> mixOuts = new ArrayList<>();
	public final List<FluidEntry> mixFluidOuts = initFluidList(2);
	public int mixTime = 60, mixHeat = 0;
	public boolean mixBasinPress = false;
	public final String[] heatLabels = {"None", "Heated", "Super"};

	// Pressing ─────────────────────────────────────────────────────────────
	public final List<ItemStack> pressIng = initList(1);
	public final List<CrushingOutput> pressOuts = new ArrayList<>();
	public int pressTime = 150;

	// Cutting (Mechanical Saw) ───────────────────────────────────────────────
	public ItemStack cutIn = ItemStack.EMPTY;
	public final List<CrushingOutput> cutOuts = new ArrayList<>();
	public int cutTime = 200;

	// Crushing / Milling ───────────────────────────────────────────────────
	public boolean isMilling = false;
	public ItemStack crushIn = ItemStack.EMPTY;
	public final List<CrushingOutput> crushOuts = new ArrayList<>();
	public int crushTime = 150;

	// Fan ──────────────────────────────────────────────────────────────────
	public boolean fanHaunting = false;
	public ItemStack fanIn = ItemStack.EMPTY;
	public final List<CrushingOutput> fanOuts = new ArrayList<>();
	public int fanTime = 200;

	// Deploying (Item Application) ──────────────────────────────────────────
	public boolean deployApplication = false;
	public boolean deployKeepHeldItem = false;
	public ItemStack deployTarget = ItemStack.EMPTY, deployTool = ItemStack.EMPTY, deployResult = ItemStack.EMPTY;

	// Filling (Spouting) ────────────────────────────────────────────────────
	public ItemStack fillIn = ItemStack.EMPTY;
	public final FluidEntry fillFluid = new FluidEntry();
	public ItemStack fillResult = ItemStack.EMPTY;

	// Bottom panel data ────────────────────────────────────────────────────
	public final List<ItemStack> availableFluids = new ArrayList<>();
	public final List<ItemStack> allItems = new ArrayList<>();
	public final List<ItemStack> cachedFilteredItems = new ArrayList<>();
	public final List<ItemStack> cachedTags = new ArrayList<>();
	public final List<ItemStack> favorites = new ArrayList<>();
	public final List<File> savedRecipeFiles = new ArrayList<>();
	public File selectedRecipeFile = null;
	public final java.util.Set<File> selectedRecipeFiles = new java.util.LinkedHashSet<>();

	public String statusMsg = "";
	public long statusUntil;
	public boolean statusOk;
	public String popupError = null;

	public RecipeEditorData() {
		for (int i = 0; i < 8; i++) crushOuts.add(new CrushingOutput());
		for (int i = 0; i < 4; i++) fanOuts.add(new CrushingOutput());
		for (int i = 0; i < 4; i++) mixOuts.add(new CrushingOutput());
		for (int i = 0; i < 4; i++) pressOuts.add(new CrushingOutput());
		for (int i = 0; i < 4; i++) cutOuts.add(new CrushingOutput());
	}

	// JSON builder ─────────────────────────────────────────────────────────
	public String buildJson(List<StationType> tabs, int tabIdx) {
		return RecipeJsonBuilder.buildJson(tabs.get(tabIdx), this);
	}

	// Clear ────────────────────────────────────────────────────────────────
	public void clear() {
		Collections.fill(craftGrid, ItemStack.EMPTY);
		Collections.fill(mechGrid, ItemStack.EMPTY);
		Collections.fill(mixIng, ItemStack.EMPTY);
		Collections.fill(pressIng, ItemStack.EMPTY);
		mixFluidIng.forEach(f -> f.proxy = ItemStack.EMPTY);
		mixFluidOuts.forEach(f -> f.proxy = ItemStack.EMPTY);
		fillFluid.proxy = ItemStack.EMPTY;
		fillFluid.amount = 1000;
		resetOutputs(mixOuts);
		resetOutputs(pressOuts);
		resetOutputs(cutOuts);
		resetOutputs(crushOuts);
		resetOutputs(fanOuts);
		craftResult = mechResult = furnIn = furnOut = stoneIn = stoneOut = smTemplate = smBase = smAddition = smResult = cutIn = crushIn = fanIn = deployTarget = deployTool = deployResult = fillIn = fillResult = ItemStack.EMPTY;
		craftCount = mechCount = furnCount = stoneCount = smCount = 1;
		cutTime = 200; furnTime = 200; crushTime = 150; pressTime = 150; fanTime = 200;
		mixHeat = 0; mechMirrored = true;
		deployApplication = false; deployKeepHeldItem = false;
		status("Cleared.", true);
	}

	private static void resetOutputs(List<CrushingOutput> list) {
		list.forEach(o -> {
			o.stack = ItemStack.EMPTY;
			o.chance = 1f;
			o.count = 1;
		});
	}

	public void status(String m, boolean ok) {
		statusMsg = m;
		statusOk = ok;
		statusUntil = System.currentTimeMillis() + 3000;
	}

	// Data loading ─────────────────────────────────────────────────────────
	public void loadFluids() {
		availableFluids.clear();
		availableFluids.add(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
		availableFluids.add(new ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
		availableFluids.add(new ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));

		if (net.neoforged.fml.ModList.get().isLoaded("create")) {
			tryAddBucket("create:honey_bucket", availableFluids);
			tryAddBucket("create:chocolate_bucket", availableFluids);
		}

		for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			ItemStack s = new ItemStack(item);
			if (!s.isEmpty()) {
				String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
				boolean isBucket = item instanceof net.minecraft.world.item.BucketItem || id.endsWith("_bucket") || id.contains("bucket");
				if (isBucket && !id.equals("minecraft:bucket") && availableFluids.stream().noneMatch(f -> ItemStack.isSameItem(f, s))) {
					availableFluids.add(s);
				}
			}
		}
	}

	private static void tryAddBucket(String id, List<ItemStack> list) {
		try {
			net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
			if (item != net.minecraft.world.item.Items.AIR) list.add(new ItemStack(item));
		} catch (Exception ignored) {}
	}

	public static boolean isTagItem(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		return stack.getItem() == net.minecraft.world.item.Items.NAME_TAG
				&& stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
				&& stack.getHoverName().getString().startsWith("#");
	}

	public static boolean isFluidOrTag(ItemStack stack) {
		return isTagItem(stack) || isFluidItem(stack);
	}

	public static boolean isFluidItem(ItemStack stack) {
		if (stack == null || stack.isEmpty() || isTagItem(stack)) return false;
		net.minecraft.world.item.Item item = stack.getItem();
		if (item instanceof net.minecraft.world.item.BucketItem) return true;
		String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
		if (id.endsWith("_bucket") || id.contains("bucket") || id.contains("_fluid") || id.endsWith("_fluid")) return true;
		try {
			return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null;
		} catch (Exception ignored) {
			return false;
		}
	}

	public void loadAllItems() {
		allItems.clear();
		for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			allItems.add(new ItemStack(item));
		}
	}

	public void loadTags() {
		cachedTags.clear();
		net.minecraft.core.registries.BuiltInRegistries.ITEM.getTags().map(com.mojang.datafixers.util.Pair::getFirst).forEach(tagKey -> {
			ItemStack stack = new ItemStack(net.minecraft.world.item.Items.NAME_TAG);
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("#" + tagKey.location()));
			cachedTags.add(stack);
		});
	}

	public void loadFavorites(Minecraft mc) {
		favorites.clear();
		if (mc == null) return;
		File configFile = new File(mc.gameDirectory, "config/opm_favorites.txt");
		if (!configFile.exists()) return;
		try {
			for (String line : Files.readAllLines(configFile.toPath())) {
				net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.tryParse(line.trim());
				if (loc != null) {
					net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(loc).ifPresent(item -> favorites.add(new ItemStack(item)));
				}
			}
		} catch (Exception ignored) {}
	}

	public void saveFavorites(Minecraft mc) {
		if (mc == null) return;
		File configFile = new File(mc.gameDirectory, "config/opm_favorites.txt");
		try {
			Files.createDirectories(configFile.getParentFile().toPath());
			List<String> lines = new ArrayList<>();
			for (ItemStack stack : favorites) {
				if (stack != null && !stack.isEmpty()) {
					lines.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
				}
			}
			Files.write(configFile.toPath(), lines);
		} catch (Exception ignored) {}
	}

	public void scanSavedRecipes() {
		savedRecipeFiles.clear();
		try {
			Path dir = RecipeFileManager.getRecipeDir();
			if (!Files.exists(dir)) return;
			try (var stream = Files.walk(dir)) {
				stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).forEach(p -> savedRecipeFiles.add(p.toFile()));
			}
			savedRecipeFiles.sort(RecipeFileManager::compareFiles);
		} catch (Exception ignored) {}
	}

	public void loadConfig(Minecraft mc, IntConsumer setter) {
		if (mc == null) return;
		File configFile = new File(mc.gameDirectory, "config/opm_editor.txt");
		if (!configFile.exists()) return;
		try {
			List<String> lines = Files.readAllLines(configFile.toPath());
			if (!lines.isEmpty()) setter.accept(Integer.parseInt(lines.getFirst().trim()));
		} catch (Exception ignored) {}
	}

	public void saveConfig(Minecraft mc, int invPanelHeight) {
		if (mc == null) return;
		File configFile = new File(mc.gameDirectory, "config/opm_editor.txt");
		try {
			Files.createDirectories(configFile.getParentFile().toPath());
			Files.writeString(configFile.toPath(), String.valueOf(invPanelHeight));
		} catch (Exception ignored) {}
	}

	// Recipe file loading ──────────────────────────────────────────────────
	public StationType loadRecipeFile(File file) {
		return cz.maxtechnik.opm.client.recipe.RecipeJsonParser.loadRecipeFile(file, this);
	}

	// Static helpers ───────────────────────────────────────────────────────
	public static List<ItemStack> initList(int n) {
		List<ItemStack> l = new ArrayList<>(n);
		for (int i = 0; i < n; i++) l.add(ItemStack.EMPTY);
		return l;
	}

	public static List<FluidEntry> initFluidList(int n) {
		List<FluidEntry> l = new ArrayList<>(n);
		for (int i = 0; i < n; i++) l.add(new FluidEntry());
		return l;
	}
}