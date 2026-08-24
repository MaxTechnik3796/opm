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
public final class RecipeJsonParser{
	private RecipeJsonParser(){
	}
	public static StationType loadRecipeFile(File file,RecipeEditorData data){
		try{
			String json=Files.readString(file.toPath());
			JsonObject root=JsonParser.parseString(json).getAsJsonObject();
			String type=root.get("type").getAsString();
			StationType station=detectType(type);
			if(station==null) return null;
			data.clear();
			parseIntoData(root,type,station,data);
			return station;
		}catch(Exception e){
			return null;
		}
	}
	public static StationType detectType(String type){
		return switch(type){
			case "minecraft:crafting_shaped","minecraft:crafting_shapeless" -> StationType.CRAFTING;
			case "minecraft:smelting","minecraft:blasting","minecraft:smoking","minecraft:campfire_cooking" -> StationType.FURNACE;
			case "minecraft:stonecutting" -> StationType.STONECUTTER;
			case "minecraft:smithing_transform" -> StationType.SMITHING;
			case "create:mechanical_crafting" -> StationType.MECH_CRAFTING;
			case "create:mixing" -> StationType.MIXING;
			case "create:pressing","create:compacting" -> StationType.PRESSING;
			case "create:cutting" -> StationType.CUTTING;
			case "create:crushing","create:milling" -> StationType.CRUSHING;
			case "create:splashing","create:haunting" -> StationType.FAN;
			case "create:item_application","create:deploying" -> StationType.DEPLOYING;
			case "create:filling" -> StationType.FILLING;
			default -> null;
		};
	}
	private static void parseIntoData(JsonObject root,String type,StationType station,RecipeEditorData data){
		switch(station){
			case CRAFTING -> parseCrafting(root,type,data);
			case MECH_CRAFTING -> parseMechCrafting(root,data);
			case FURNACE -> parseFurnace(root,type,data);
			case STONECUTTER -> parseStonecutter(root,data);
			case SMITHING -> parseSmithing(root,data);
			case MIXING -> parseMixing(root,type,data);
			case PRESSING -> parsePressing(root,data);
			case CUTTING -> parseCutting(root,data);
			case CRUSHING -> parseCrushing(root,type,data);
			case FAN -> parseFan(root,type,data);
			case DEPLOYING -> parseDeploying(root,type,data);
			case FILLING -> parseFilling(root,data);
		}
	}
	private static void parseCrafting(JsonObject root,String type,RecipeEditorData data){
		data.shapeless=type.equals("minecraft:crafting_shapeless");
		if(!data.shapeless){
			parsePattern(root,data.craftGrid,3,3);
		}else{
			var ingredients=root.getAsJsonArray("ingredients");
			for(int i=0;i<ingredients.size()&&i<9;i++) data.craftGrid.set(i,parseIngredient(ingredients.get(i)));
		}
		var result=root.getAsJsonObject("result");
		data.craftResult=parseIngredient(result);
		data.craftCount=result.has("count")?result.get("count").getAsInt():1;
	}
	private static void parseMechCrafting(JsonObject root,RecipeEditorData data){
		data.mechMirrored=root.has("accept_mirrored")&&root.get("accept_mirrored").getAsBoolean();
		parsePattern(root,data.mechGrid,9,9);
		var result=root.getAsJsonObject("result");
		data.mechResult=parseIngredient(result);
		data.mechCount=result.has("count")?result.get("count").getAsInt():1;
	}
	private static void parsePattern(JsonObject root,List<ItemStack> grid,int maxCols,int maxRows){
		var patternArr=root.getAsJsonArray("pattern");
		var keyObj=root.getAsJsonObject("key");
		Map<Character,ItemStack> keyMap=new HashMap<>();
		for(var entry: keyObj.entrySet()) keyMap.put(entry.getKey().charAt(0),parseIngredient(entry.getValue()));
		for(int row=0;row<patternArr.size()&&row<maxRows;row++){
			String patternRow=patternArr.get(row).getAsString();
			for(int col=0;col<patternRow.length()&&col<maxCols;col++){
				char ch=patternRow.charAt(col);
				if(ch!=' '&&keyMap.containsKey(ch)) grid.set(row*maxCols+col,keyMap.get(ch).copy());
			}
		}
	}
	private static void parseFurnace(JsonObject root,String type,RecipeEditorData data){
		for(int i=0;i<data.furnSubs.length;i++){
			if(type.equals("minecraft:"+data.furnSubs[i])){
				data.furnSubIdx=i;
				break;
			}
		}
		data.furnIn=parseIngredient(root.get("ingredient"));
		var result=root.getAsJsonObject("result");
		data.furnOut=parseIngredient(result);
		data.furnCount=result.has("count")?result.get("count").getAsInt():1;
		data.furnTime=root.has("cookingtime")?root.get("cookingtime").getAsInt():200;
		data.furnXp=root.has("experience")?root.get("experience").getAsFloat():0.1f;
	}
	private static void parseStonecutter(JsonObject root,RecipeEditorData data){
		data.stoneIn=parseIngredient(root.get("ingredient"));
		var result=root.getAsJsonObject("result");
		data.stoneOut=parseIngredient(result);
		data.stoneCount=result.has("count")?result.get("count").getAsInt():1;
	}
	private static void parseSmithing(JsonObject root,RecipeEditorData data){
		data.smTemplate=parseIngredient(root.get("template"));
		data.smBase=parseIngredient(root.get("base"));
		data.smAddition=parseIngredient(root.get("addition"));
		var result=root.getAsJsonObject("result");
		data.smResult=parseIngredient(result);
		data.smCount=result.has("count")?result.get("count").getAsInt():1;
	}
	private static void parseMixing(JsonObject root,String type,RecipeEditorData data){
		data.mixBasinPress=type.equals("create:compacting");
		var ingredients=root.getAsJsonArray("ingredients");
		int itemIdx=0, fluidIdx=0;
		for(var element: ingredients){
			if(element.isJsonObject()&&isFluidJson(element.getAsJsonObject())){
				if(fluidIdx<2){
					var fluidObj=element.getAsJsonObject();
					FluidEntry entry=data.mixFluidIng.get(fluidIdx++);
					entry.proxy=parseIngredient(fluidObj);
					entry.amount=Math.clamp(fluidObj.has("amount")?fluidObj.get("amount").getAsInt():1000,1,1000);
				}
			}else if(itemIdx<9){
				data.mixIng.set(itemIdx++,parseIngredient(element));
			}
		}
		var results=root.getAsJsonArray("results");
		int outItemIdx=0, outFluidIdx=0;
		for(var element: results){
			var resultObj=element.getAsJsonObject();
			if(resultObj.has("fluid")||(resultObj.has("amount")&&!resultObj.has("count"))){
				if(outFluidIdx<2){
					FluidEntry entry=data.mixFluidOuts.get(outFluidIdx++);
					entry.proxy=parseIngredient(resultObj);
					entry.amount=Math.clamp(resultObj.has("amount")?resultObj.get("amount").getAsInt():1000,1,1000);
				}
			}else if(outItemIdx<4){
				applyOutput(data.mixOuts.get(outItemIdx++),resultObj);
			}
		}
		String heat=root.has("heat_requirement")?root.get("heat_requirement").getAsString()
				:(root.has("heatRequirement")?root.get("heatRequirement").getAsString():"none");
		data.mixHeat=heat.equalsIgnoreCase("superheated")?2:heat.equalsIgnoreCase("heated")?1:0;
		data.mixTime=root.has("processingTime")?root.get("processingTime").getAsInt():60;
	}
	private static void parsePressing(JsonObject root,RecipeEditorData data){
		var ingredients=root.getAsJsonArray("ingredients");
		if(ingredients!=null&&!ingredients.isEmpty()) data.pressIng.set(0,parseIngredient(ingredients.get(0)));
		var results=root.getAsJsonArray("results");
		if(results!=null){
			for(int i=0;i<results.size()&&i<4;i++) applyOutput(data.pressOuts.get(i),results.get(i).getAsJsonObject());
		}
		data.pressTime=root.has("processingTime")?root.get("processingTime").getAsInt():150;
	}
	private static void parseCutting(JsonObject root,RecipeEditorData data){
		var ingredients=root.getAsJsonArray("ingredients");
		if(ingredients!=null&&!ingredients.isEmpty()) data.cutIn=parseIngredient(ingredients.get(0));
		var results=root.getAsJsonArray("results");
		if(results!=null){
			for(int i=0;i<results.size()&&i<4;i++) applyOutput(data.cutOuts.get(i),results.get(i).getAsJsonObject());
		}
		data.cutTime=root.has("processing_time")?root.get("processing_time").getAsInt()
				:(root.has("processingTime")?root.get("processingTime").getAsInt():200);
	}
	private static void parseCrushing(JsonObject root,String type,RecipeEditorData data){
		data.isMilling=type.equals("create:milling");
		parseInOuts(root,true,data);
		data.crushTime=root.has("processingTime")?root.get("processingTime").getAsInt():150;
	}
	private static void parseFan(JsonObject root,String type,RecipeEditorData data){
		data.fanHaunting=type.equals("create:haunting");
		parseInOuts(root,false,data);
		data.fanTime=root.has("processingTime")?root.get("processingTime").getAsInt():200;
	}
	private static void parseDeploying(JsonObject root,String type,RecipeEditorData data){
		data.deployApplication="create:item_application".equals(type);
		data.deployKeepHeldItem=root.has("keep_held_item")&&root.get("keep_held_item").getAsBoolean();
		var ingredients=root.getAsJsonArray("ingredients");
		if(ingredients!=null){
			if(!ingredients.isEmpty()) data.deployTarget=parseIngredient(ingredients.get(0));
			if(ingredients.size()>1) data.deployTool=parseIngredient(ingredients.get(1));
		}
		var results=root.getAsJsonArray("results");
		if(results!=null&&!results.isEmpty()) data.deployResult=parseIngredient(results.get(0));
	}
	private static void parseFilling(JsonObject root,RecipeEditorData data){
		var ingredients=root.getAsJsonArray("ingredients");
		if(ingredients!=null){
			for(var element: ingredients){
				if(element.isJsonObject()&&isFluidJson(element.getAsJsonObject())){
					var fluidObj=element.getAsJsonObject();
					data.fillFluid.proxy=parseIngredient(fluidObj);
					data.fillFluid.amount=Math.clamp(fluidObj.has("amount")?fluidObj.get("amount").getAsInt():1000,1,1000);
				}else{
					data.fillIn=parseIngredient(element);
				}
			}
		}
		var results=root.getAsJsonArray("results");
		if(results!=null&&!results.isEmpty()) data.fillResult=parseIngredient(results.get(0));
	}
	private static boolean isFluidJson(JsonObject obj){
		if(obj.has("fluid")) return true;
		if(obj.has("type")){
			String t=obj.get("type").getAsString();
			if(t.equals("neoforge:tag")||t.equals("neoforge:single")||t.equals("neoforge:fluid")||t.startsWith("neoforge:")) return true;
		}
		return obj.has("amount")&&(obj.has("tag")||obj.has("id"));
	}
	private static void parseInOuts(JsonObject root,boolean isCrushing,RecipeEditorData data){
		var ingredients=root.getAsJsonArray("ingredients");
		if(ingredients!=null&&!ingredients.isEmpty()){
			ItemStack input=parseIngredient(ingredients.get(0));
			if(isCrushing) data.crushIn=input;
			else data.fanIn=input;
		}
		var results=root.getAsJsonArray("results");
		if(results==null) return;
		List<CrushingOutput> outputs=isCrushing?data.crushOuts:data.fanOuts;
		int limit=isCrushing?8:4;
		for(int i=0;i<results.size()&&i<limit;i++) applyOutput(outputs.get(i),results.get(i).getAsJsonObject());
	}
	private static void applyOutput(CrushingOutput output,JsonObject resultObj){
		output.stack=parseIngredient(resultObj);
		output.count=resultObj.has("count")?resultObj.get("count").getAsInt():1;
		output.chance=resultObj.has("chance")?resultObj.get("chance").getAsFloat():1f;
	}
	public static ItemStack parseIngredient(JsonElement element){
		if(element==null||element.isJsonNull()) return ItemStack.EMPTY;
		JsonObject obj=null;
		if(element.isJsonObject()){
			obj=element.getAsJsonObject();
		}else if(element.isJsonArray()){
			var arr=element.getAsJsonArray();
			if(!arr.isEmpty()&&arr.get(0).isJsonObject()) obj=arr.get(0).getAsJsonObject();
		}
		if(obj==null) return ItemStack.EMPTY;
		if(obj.has("tag")){
			String tag=obj.get("tag").getAsString();
			ItemStack proxy=new ItemStack(Items.NAME_TAG);
			proxy.set(DataComponents.CUSTOM_NAME,Component.literal("#"+tag));
			return proxy;
		}
		if(obj.has("fluid")||(obj.has("amount")&&obj.has("id"))){
			String fluidId=obj.has("fluid")?obj.get("fluid").getAsString():obj.get("id").getAsString();
			var opt=BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(fluidId+"_bucket"));
			return opt.map(ItemStack::new).orElse(ItemStack.EMPTY);
		}
		String id=obj.has("item")?obj.get("item").getAsString():obj.has("id")?obj.get("id").getAsString():null;
		if(id==null) return ItemStack.EMPTY;
		return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}
}
