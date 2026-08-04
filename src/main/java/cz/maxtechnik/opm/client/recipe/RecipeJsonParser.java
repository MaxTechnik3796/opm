package cz.maxtechnik.opm.client.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Samostatný modul pro parsování receptů z JSON souborů a jejich načítání do RecipeEditorData.
 */
public final class RecipeJsonParser {
	private RecipeJsonParser() {}

	public static StationType loadRecipeFile(File file, RecipeEditorData data) {
		try {
			String json = Files.readString(file.toPath());
			JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
			String type = obj.get("type").getAsString();
			StationType targetType = detectType(type);
			if (targetType == null) return null;
			data.clear();
			parseIntoData(obj, type, targetType, data);
			return targetType;
		} catch (Exception e) {
			return null;
		}
	}

	public static StationType detectType(String type) {
		return switch (type) {
			case "minecraft:crafting_shaped", "minecraft:crafting_shapeless" -> StationType.CRAFTING;
			case "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking" -> StationType.FURNACE;
			case "minecraft:stonecutting" -> StationType.STONECUTTER;
			case "minecraft:smithing_transform" -> StationType.SMITHING;
			case "create:mechanical_crafting" -> StationType.MECH_CRAFTING;
			case "create:mixing" -> StationType.MIXING;
			case "create:pressing", "create:compacting" -> StationType.PRESSING;
			case "create:cutting" -> StationType.CUTTING;
			case "create:crushing", "create:milling" -> StationType.CRUSHING;
			case "create:splashing", "create:haunting" -> StationType.FAN;
			case "create:item_application", "create:deploying" -> StationType.DEPLOYING;
			case "create:filling" -> StationType.FILLING;
			default -> null;
		};
	}

	private static void parseIntoData(JsonObject obj, String type, StationType t, RecipeEditorData data) {
		switch (t) {
			case CRAFTING -> parseCrafting(obj, type, data);
			case MECH_CRAFTING -> parseMechCrafting(obj, data);
			case FURNACE -> parseFurnace(obj, type, data);
			case STONECUTTER -> parseStonecutter(obj, data);
			case SMITHING -> parseSmithing(obj, data);
			case MIXING -> parseMixing(obj, type, data);
			case PRESSING -> parsePressing(obj, data);
			case CUTTING -> parseCutting(obj, data);
			case CRUSHING -> parseCrushing(obj, type, data);
			case FAN -> parseFan(obj, type, data);
			case DEPLOYING -> parseItemApplication(obj, data);
			case FILLING -> parseFilling(obj, data);
		}
	}

	private static void parseCrafting(JsonObject obj, String type, RecipeEditorData d) {
		d.shapeless = type.equals("minecraft:crafting_shapeless");
		if (!d.shapeless) parsePattern(obj, d.craftGrid, 3, 3);
		else {
			var ingArr = obj.getAsJsonArray("ingredients");
			for (int i = 0; i < ingArr.size() && i < 9; i++) d.craftGrid.set(i, parseIngredient(ingArr.get(i)));
		}
		var res = obj.getAsJsonObject("result");
		d.craftResult = parseIngredient(res);
		d.craftCount = res.has("count") ? res.get("count").getAsInt() : 1;
	}

	private static void parseMechCrafting(JsonObject obj, RecipeEditorData d) {
		d.mechMirrored = obj.has("accept_mirrored") && obj.get("accept_mirrored").getAsBoolean();
		parsePattern(obj, d.mechGrid, 9, 9);
		var res = obj.getAsJsonObject("result");
		d.craftResult = parseIngredient(res);
		d.craftCount = res.has("count") ? res.get("count").getAsInt() : 1;
	}

	private static void parsePattern(JsonObject obj, List<ItemStack> grid, int maxCols, int maxRows) {
		var patternArr = obj.getAsJsonArray("pattern");
		var keyObj = obj.getAsJsonObject("key");
		Map<Character, ItemStack> keyMap = new HashMap<>();
		for (var entry : keyObj.entrySet()) keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue()));
		for (int r = 0; r < patternArr.size() && r < maxRows; r++) {
			String row = patternArr.get(r).getAsString();
			for (int c = 0; c < row.length() && c < maxCols; c++) {
				char ch = row.charAt(c);
				if (ch != ' ' && keyMap.containsKey(ch)) grid.set(r * maxCols + c, keyMap.get(ch).copy());
			}
		}
	}

	private static void parseFurnace(JsonObject obj, String type, RecipeEditorData d) {
		for (int i = 0; i < d.furnSubs.length; i++) {
			if (type.equals("minecraft:" + d.furnSubs[i])) {
				d.furnSubIdx = i;
				break;
			}
		}
		d.furnIn = parseIngredient(obj.get("ingredient"));
		var res = obj.getAsJsonObject("result");
		d.furnOut = parseIngredient(res);
		d.furnCount = res.has("count") ? res.get("count").getAsInt() : 1;
		d.furnTime = obj.has("cookingtime") ? obj.get("cookingtime").getAsInt() : 200;
		d.furnXp = obj.has("experience") ? obj.get("experience").getAsFloat() : 0.1f;
	}

	private static void parseStonecutter(JsonObject obj, RecipeEditorData d) {
		d.stoneIn = parseIngredient(obj.get("ingredient"));
		var res = obj.getAsJsonObject("result");
		d.stoneOut = parseIngredient(res);
		d.stoneCount = res.has("count") ? res.get("count").getAsInt() : 1;
	}

	private static void parseSmithing(JsonObject obj, RecipeEditorData d) {
		d.smTemplate = parseIngredient(obj.get("template"));
		d.smBase = parseIngredient(obj.get("base"));
		d.smAddition = parseIngredient(obj.get("addition"));
		var res = obj.getAsJsonObject("result");
		d.smResult = parseIngredient(res);
		d.smCount = res.has("count") ? res.get("count").getAsInt() : 1;
	}

	private static void parseMixing(JsonObject obj, String type, RecipeEditorData d) {
		d.mixBasinPress = type.equals("create:compacting");
		var ingArr = obj.getAsJsonArray("ingredients");
		int itemIdx = 0, fluidIdx = 0;
		for (var el : ingArr) {
			if (el.isJsonObject() && el.getAsJsonObject().has("fluid")) {
				if (fluidIdx < 2) {
					var fObj = el.getAsJsonObject();
					FluidEntry fe = d.mixFluidIng.get(fluidIdx++);
					fe.proxy = parseIngredient(fObj);
					fe.amount = Math.clamp(fObj.has("amount") ? fObj.get("amount").getAsInt() : 1000, 1, 1000);
				}
			} else if (itemIdx < 9) d.mixIng.set(itemIdx++, parseIngredient(el));
		}
		var resArr = obj.getAsJsonArray("results");
		int outItemIdx = 0, outFluidIdx = 0;
		for (var el : resArr) {
			var rObj = el.getAsJsonObject();
			if (rObj.has("fluid") || (rObj.has("amount") && !rObj.has("count"))) {
				if (outFluidIdx < 2) {
					FluidEntry fe = d.mixFluidOuts.get(outFluidIdx++);
					fe.proxy = parseIngredient(rObj);
					fe.amount = Math.clamp(rObj.has("amount") ? rObj.get("amount").getAsInt() : 1000, 1, 1000);
				}
			} else if (outItemIdx < 4) {
				CrushingOutput co = d.mixOuts.get(outItemIdx++);
				applyOutput(co, rObj);
			}
		}
		String heat = obj.has("heat_requirement") ? obj.get("heat_requirement").getAsString() : (obj.has("heatRequirement") ? obj.get("heatRequirement").getAsString() : "none");
		d.mixHeat = heat.equalsIgnoreCase("superheated") ? 2 : heat.equalsIgnoreCase("heated") ? 1 : 0;
		d.mixTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 60;
	}

	private static void parsePressing(JsonObject obj, RecipeEditorData d) {
		var ingArr = obj.getAsJsonArray("ingredients");
		if (ingArr != null && !ingArr.isEmpty()) d.pressIng.set(0, parseIngredient(ingArr.get(0)));
		var resArr = obj.getAsJsonArray("results");
		if (resArr != null) {
			for (int i = 0; i < resArr.size() && i < 4; i++) {
				applyOutput(d.pressOuts.get(i), resArr.get(i).getAsJsonObject());
			}
		}
		d.pressTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 150;
	}

	private static void parseCutting(JsonObject obj, RecipeEditorData d) {
		var ingArr = obj.getAsJsonArray("ingredients");
		if (ingArr != null && !ingArr.isEmpty()) d.cutIn = parseIngredient(ingArr.get(0));
		var resArr = obj.getAsJsonArray("results");
		if (resArr != null) {
			for (int i = 0; i < resArr.size() && i < 4; i++) {
				applyOutput(d.cutOuts.get(i), resArr.get(i).getAsJsonObject());
			}
		}
		d.cutTime = obj.has("processing_time") ? obj.get("processing_time").getAsInt() : (obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 200);
	}

	private static void parseCrushing(JsonObject obj, String type, RecipeEditorData d) {
		d.isMilling = type.equals("create:milling");
		parseInOuts(obj, true, d);
		d.crushTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 150;
	}

	private static void parseFan(JsonObject obj, String type, RecipeEditorData d) {
		d.fanHaunting = type.equals("create:haunting");
		parseInOuts(obj, false, d);
		d.fanTime = obj.has("processingTime") ? obj.get("processingTime").getAsInt() : 200;
	}

	private static void parseItemApplication(JsonObject obj, RecipeEditorData d) {
		var ingArr = obj.getAsJsonArray("ingredients");
		if (ingArr != null) {
			if (ingArr.size() > 0) d.deployTarget = parseIngredient(ingArr.get(0));
			if (ingArr.size() > 1) d.deployTool = parseIngredient(ingArr.get(1));
		}
		var resArr = obj.getAsJsonArray("results");
		if (resArr != null && !resArr.isEmpty()) {
			d.deployResult = parseIngredient(resArr.get(0));
		}
	}

	private static void parseFilling(JsonObject obj, RecipeEditorData d) {
		var ingArr = obj.getAsJsonArray("ingredients");
		if (ingArr != null) {
			for (var el : ingArr) {
				if (el.isJsonObject() && el.getAsJsonObject().has("fluid")) {
					var fObj = el.getAsJsonObject();
					d.fillFluid.proxy = parseIngredient(fObj);
					d.fillFluid.amount = Math.clamp(fObj.has("amount") ? fObj.get("amount").getAsInt() : 1000, 1, 1000);
				} else {
					d.fillIn = parseIngredient(el);
				}
			}
		}
		var resArr = obj.getAsJsonArray("results");
		if (resArr != null && !resArr.isEmpty()) {
			d.fillResult = parseIngredient(resArr.get(0));
		}
	}

	private static void parseInOuts(JsonObject obj, boolean crushing, RecipeEditorData d) {
		var ingArr = obj.getAsJsonArray("ingredients");
		if (ingArr != null && !ingArr.isEmpty()) {
			ItemStack input = parseIngredient(ingArr.get(0));
			if (crushing) d.crushIn = input;
			else d.fanIn = input;
		}
		var resArr = obj.getAsJsonArray("results");
		if (resArr == null) return;
		List<CrushingOutput> dst = crushing ? d.crushOuts : d.fanOuts;
		int limit = crushing ? 8 : 4;
		for (int i = 0; i < resArr.size() && i < limit; i++) applyOutput(dst.get(i), resArr.get(i).getAsJsonObject());
	}

	private static void applyOutput(CrushingOutput co, JsonObject rObj) {
		co.stack = parseIngredient(rObj);
		co.count = rObj.has("count") ? rObj.get("count").getAsInt() : 1;
		co.chance = rObj.has("chance") ? rObj.get("chance").getAsFloat() : 1f;
	}

	public static ItemStack parseIngredient(JsonElement el) {
		if (el == null || el.isJsonNull()) return ItemStack.EMPTY;
		JsonObject obj = null;
		if (el.isJsonObject()) obj = el.getAsJsonObject();
		else if (el.isJsonArray()) {
			var arr = el.getAsJsonArray();
			if (!arr.isEmpty() && arr.get(0).isJsonObject()) obj = arr.get(0).getAsJsonObject();
		}
		if (obj == null) return ItemStack.EMPTY;
		if (obj.has("tag")) {
			String tag = obj.get("tag").getAsString();
			ItemStack proxy = new ItemStack(Items.NAME_TAG);
			proxy.set(DataComponents.CUSTOM_NAME, Component.literal("#" + tag));
			return proxy;
		}
		if (obj.has("fluid") || (obj.has("amount") && obj.has("id"))) {
			String fluidId = obj.has("fluid") ? obj.get("fluid").getAsString() : obj.get("id").getAsString();
			String bucketId = fluidId + "_bucket";
			var opt = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(bucketId));
			return opt.map(ItemStack::new).orElse(ItemStack.EMPTY);
		}
		String id = obj.has("item") ? obj.get("item").getAsString() : obj.has("id") ? obj.get("id").getAsString() : null;
		if (id == null) return ItemStack.EMPTY;
		return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}
}
