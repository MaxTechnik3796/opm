package cz.maxtechnik.opm.client.screen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder;
import cz.maxtechnik.opm.client.recipe.StationType;
import cz.maxtechnik.opm.client.recipe.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.StationType.FluidEntry;
import cz.maxtechnik.opm.client.recipe.StationType.RecipeFileWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
public class RecipeEditorData{
	// ── Crafting ─────────────────────────────────────────────────────────────
	public boolean shapeless=false;
	public final List<ItemStack> craftGrid=initList(9);
	public final List<ItemStack> mechGrid=initList(81);
	public boolean mechMirrored=true;
	public ItemStack craftResult=ItemStack.EMPTY;
	public int craftCount=1;
	// ── Furnace ──────────────────────────────────────────────────────────────
	public int furnSubIdx=0;
	public final String[] furnSubs={"smelting","blasting","smoking","campfire_cooking"};
	public final String[] furnLabels={"Furnace","Blast Furnace","Smoker","Campfire"};
	public ItemStack furnIn=ItemStack.EMPTY, furnOut=ItemStack.EMPTY;
	public int furnCount=1, furnTime=200;
	public float furnXp=0.1f;
	// ── Stonecutter ──────────────────────────────────────────────────────────
	public ItemStack stoneIn=ItemStack.EMPTY, stoneOut=ItemStack.EMPTY;
	public int stoneCount=1;
	// ── Smithing ─────────────────────────────────────────────────────────────
	public ItemStack smTemplate=ItemStack.EMPTY, smBase=ItemStack.EMPTY;
	public ItemStack smAddition=ItemStack.EMPTY, smResult=ItemStack.EMPTY;
	public int smCount=1;
	// ── Mixing ───────────────────────────────────────────────────────────────
	public final List<ItemStack> mixIng=initList(9);
	public final List<FluidEntry> mixFluidIng=initFluidList(2);
	public final List<CrushingOutput> mixOuts=new ArrayList<>();
	public final List<FluidEntry> mixFluidOuts=initFluidList(2);
	public int mixTime=60, mixHeat=0;
	public boolean mixBasinPress=false;
	public final String[] heatLabels={"None","Heated","Superheated"};
	// ── Pressing ─────────────────────────────────────────────────────────────
	public final List<ItemStack> pressIng=initList(1);
	public final List<CrushingOutput> pressOuts=new ArrayList<>();
	public int pressTime=150;
	// ── Crushing / Milling ───────────────────────────────────────────────────
	public boolean isMilling=false;
	public ItemStack crushIn=ItemStack.EMPTY;
	public final List<CrushingOutput> crushOuts=new ArrayList<>();
	public int crushTime=150;
	// ── Fan ──────────────────────────────────────────────────────────────────
	public boolean fanHaunting=false;
	public ItemStack fanIn=ItemStack.EMPTY;
	public final List<CrushingOutput> fanOuts=new ArrayList<>();
	public int fanTime=200;
	// ── Bottom panel data ────────────────────────────────────────────────────
	public final List<ItemStack> availableFluids=new ArrayList<>();
	public final List<ItemStack> allItems=new ArrayList<>();
	public final List<ItemStack> cachedFilteredItems=new ArrayList<>();
	public final List<ItemStack> cachedTags=new ArrayList<>();
	public final List<ItemStack> favorites=new ArrayList<>();
	public final List<File> savedRecipeFiles=new ArrayList<>();
	public File selectedRecipeFile=null;
	public final java.util.Set<File> selectedRecipeFiles=new java.util.LinkedHashSet<>();
	public String statusMsg="";
	public long statusUntil;
	public boolean statusOk;
	public String popupError=null;
	// ─────────────────────────────────────────────────────────────────────────
	public RecipeEditorData(){
		for(int i=0;i<8;i++) crushOuts.add(new CrushingOutput());
		for(int i=0;i<4;i++) fanOuts.add(new CrushingOutput());
		for(int i=0;i<4;i++) mixOuts.add(new CrushingOutput());
		pressOuts.add(new CrushingOutput());
	}
	// ── JSON builder ─────────────────────────────────────────────────────────
	public String buildJson(List<StationType> tabs,int tabIdx){
		try{
			return switch(tabs.get(tabIdx)){
				case CRAFTING ->
						shapeless?RecipeJsonBuilder.buildShapeless(craftGrid,craftResult,craftCount):RecipeJsonBuilder.buildShaped(craftGrid,3,3,craftResult,craftCount);
				case FURNACE ->
						RecipeJsonBuilder.buildFurnace(furnSubs[furnSubIdx],furnIn,furnOut,furnCount,furnTime,furnXp);
				case STONECUTTER -> RecipeJsonBuilder.buildStonecutter(stoneIn,stoneOut,stoneCount);
				case SMITHING -> RecipeJsonBuilder.buildSmithing(smTemplate,smBase,smAddition,smResult,smCount);
				case MECH_CRAFTING ->
						RecipeJsonBuilder.buildMechCrafting(mechGrid,9,9,craftResult,craftCount,mechMirrored);
				case MIXING ->
						RecipeJsonBuilder.buildMixing(mixBasinPress?"create:compacting":"create:mixing",mixIng,mixFluidIng,mixOuts,mixFluidOuts,heatLabels[mixHeat].toLowerCase(Locale.ROOT));
				case PRESSING -> RecipeJsonBuilder.buildPressing(pressIng.getFirst(),pressOuts.getFirst());
				case FAN ->
						RecipeJsonBuilder.buildCrushing(fanHaunting?"create:haunting":"create:splashing",fanIn,fanOuts,fanTime);
				case CRUSHING ->
						RecipeJsonBuilder.buildCrushing(isMilling?"create:milling":"create:crushing",crushIn,crushOuts,crushTime);
			};
		}catch(Exception e){
			return "// Error: "+e.getMessage();
		}
	}
	// ── Clear ────────────────────────────────────────────────────────────────
	public void clear(){
		Collections.fill(craftGrid,ItemStack.EMPTY);
		Collections.fill(mechGrid,ItemStack.EMPTY);
		Collections.fill(mixIng,ItemStack.EMPTY);
		Collections.fill(pressIng,ItemStack.EMPTY);
		mixFluidIng.forEach(f->f.proxy=ItemStack.EMPTY);
		mixFluidOuts.forEach(f->f.proxy=ItemStack.EMPTY);
		resetOutputs(mixOuts);
		resetOutputs(pressOuts);
		resetOutputs(crushOuts);
		resetOutputs(fanOuts);
		craftResult=furnIn=furnOut=stoneIn=stoneOut=smTemplate=smBase=smAddition=smResult=crushIn=fanIn=ItemStack.EMPTY;
		craftCount=furnCount=stoneCount=smCount=1;
		mixHeat=0;
		status("Cleared.",true);
	}
	private static void resetOutputs(List<CrushingOutput> list){
		list.forEach(o->{
			o.stack=ItemStack.EMPTY;
			o.chance=1f;
			o.count=1;
		});
	}
	public void status(String m,boolean ok){
		statusMsg=m;
		statusOk=ok;
		statusUntil=System.currentTimeMillis()+3000;
	}
	// ── Data loading ─────────────────────────────────────────────────────────
	public void loadFluids(){
		availableFluids.clear();
		availableFluids.add(new ItemStack(Items.WATER_BUCKET));
		availableFluids.add(new ItemStack(Items.LAVA_BUCKET));
		availableFluids.add(new ItemStack(Items.MILK_BUCKET));
		if(net.neoforged.fml.ModList.get().isLoaded("create")){
			tryAddBucket("create:honey_bucket");
			tryAddBucket("create:chocolate_bucket");
		}
		for(Item item: BuiltInRegistries.ITEM){
			ItemStack s=new ItemStack(item);
			if(!s.isEmpty()){
				String id=BuiltInRegistries.ITEM.getKey(item).toString();
				if(id.endsWith("_bucket")&&!id.equals("minecraft:bucket")&&availableFluids.stream().noneMatch(f->ItemStack.isSameItem(f,s)))
					availableFluids.add(s);
			}
		}
	}
	private void tryAddBucket(String id){
		try{
			Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
			if(item!=Items.AIR) availableFluids.add(new ItemStack(item));
		}catch(Exception ignored){
		}
	}
	public void loadAllItems(){
		allItems.clear();
		for(Item item: BuiltInRegistries.ITEM) allItems.add(new ItemStack(item));
	}
	public void loadTags(){
		cachedTags.clear();
		BuiltInRegistries.ITEM.getTags().map(com.mojang.datafixers.util.Pair::getFirst).forEach(tagKey->{
			ItemStack stack=new ItemStack(Items.NAME_TAG);
			stack.set(DataComponents.CUSTOM_NAME,Component.literal("#"+tagKey.location()));
			cachedTags.add(stack);
		});
	}
	public void loadFavorites(net.minecraft.client.Minecraft mc){
		favorites.clear();
		if(mc==null) return;
		File f=new File(mc.gameDirectory,"config/opm_favorites.txt");
		if(!f.exists()) return;
		try{
			for(String s: Files.readAllLines(f.toPath())){
				ResourceLocation loc=ResourceLocation.tryParse(s);
				if(loc!=null)
					BuiltInRegistries.ITEM.getOptional(loc).ifPresent(item->favorites.add(new ItemStack(item)));
			}
		}catch(Exception ignored){
		}
	}
	public void saveFavorites(net.minecraft.client.Minecraft mc){
		if(mc==null) return;
		File f=new File(mc.gameDirectory,"config/opm_favorites.txt");
		try{
			Files.createDirectories(f.getParentFile().toPath());
			List<String> lines=new ArrayList<>();
			for(ItemStack s: favorites)
				if(!s.isEmpty()) lines.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
			Files.write(f.toPath(),lines);
		}catch(Exception ignored){
		}
	}
	public void scanSavedRecipes(){
		savedRecipeFiles.clear();
		try{
			java.nio.file.Path dir=RecipeFileWriter.getRecipeDir();
			if(!Files.exists(dir)) return;
			try(var stream=Files.walk(dir)){
				stream.filter(p->Files.isRegularFile(p)&&p.toString().endsWith(".json")).forEach(p->savedRecipeFiles.add(p.toFile()));
			}
			savedRecipeFiles.sort(RecipeEditorData::compareSavedRecipes);
		}catch(Exception ignored){
		}
	}
	public void loadConfig(Minecraft mc,IntConsumer setter){
		if(mc==null) return;
		File f=new File(mc.gameDirectory,"config/opm_editor.txt");
		if(!f.exists()) return;
		try{
			List<String> lines=Files.readAllLines(f.toPath());
			if(!lines.isEmpty()) setter.accept(Integer.parseInt(lines.getFirst()));
		}catch(Exception ignored){
		}
	}
	public void saveConfig(Minecraft mc,int invPanelHeight){
		if(mc==null) return;
		File f=new File(mc.gameDirectory,"config/opm_editor.txt");
		try{
			Files.createDirectories(f.getParentFile().toPath());
			Files.writeString(f.toPath(),String.valueOf(invPanelHeight));
		}catch(Exception ignored){
		}
	}
	// ── Recipe file loading ──────────────────────────────────────────────────
	public String loadRecipeFile(File file){
		try{
			String json=Files.readString(file.toPath());
			com.google.gson.JsonObject obj=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
			String type=obj.get("type").getAsString();
			StationType targetType=detectType(type);
			if(targetType==null) return "Unknown recipe type";
			clear();
			parseIntoData(obj,type,targetType);
			return null;
		}catch(Exception e){
			return "Invalid file";
		}
	}
	private StationType detectType(String type){
		return switch(type){
			case "minecraft:crafting_shaped","minecraft:crafting_shapeless" -> StationType.CRAFTING;
			case "minecraft:smelting","minecraft:blasting","minecraft:smoking","minecraft:campfire_cooking" ->
					StationType.FURNACE;
			case "minecraft:stonecutting" -> StationType.STONECUTTER;
			case "minecraft:smithing_transform" -> StationType.SMITHING;
			case "create:mechanical_crafting" -> StationType.MECH_CRAFTING;
			case "create:mixing" -> StationType.MIXING;
			case "create:pressing","create:compacting" -> StationType.PRESSING;
			case "create:crushing","create:milling" -> StationType.CRUSHING;
			case "create:splashing","create:haunting" -> StationType.FAN;
			default -> null;
		};
	}
	private void parseIntoData(JsonObject obj,String type,StationType t){
		switch(t){
			case CRAFTING -> parseCrafting(obj,type);
			case MECH_CRAFTING -> parseMechCrafting(obj);
			case FURNACE -> parseFurnace(obj,type);
			case STONECUTTER -> parseStonecutter(obj);
			case SMITHING -> parseSmithing(obj);
			case MIXING -> parseMixing(obj,type);
			case PRESSING -> parsePressing(obj);
			case CRUSHING -> parseCrushing(obj,type);
			case FAN -> parseFan(obj,type);
		}
	}
	private void parseCrafting(JsonObject obj,String type){
		shapeless=type.equals("minecraft:crafting_shapeless");
		if(!shapeless) parsePattern(obj,craftGrid,3,3);
		else{
			var ingArr=obj.getAsJsonArray("ingredients");
			for(int i=0;i<ingArr.size()&&i<9;i++) craftGrid.set(i,parseIngredient(ingArr.get(i)));
		}
		var res=obj.getAsJsonObject("result");
		craftResult=parseIngredient(res);
		craftCount=res.has("count")?res.get("count").getAsInt():1;
	}
	private void parseMechCrafting(JsonObject obj){
		mechMirrored=obj.has("accept_mirrored")&&obj.get("accept_mirrored").getAsBoolean();
		parsePattern(obj,mechGrid,9,9);
		var res=obj.getAsJsonObject("result");
		craftResult=parseIngredient(res);
		craftCount=res.has("count")?res.get("count").getAsInt():1;
	}
	private void parsePattern(JsonObject obj,List<ItemStack> grid,int maxCols,int maxRows){
		var patternArr=obj.getAsJsonArray("pattern");
		var keyObj=obj.getAsJsonObject("key");
		Map<Character,ItemStack> keyMap=new HashMap<>();
		for(var entry: keyObj.entrySet()) keyMap.put(entry.getKey().charAt(0),parseIngredient(entry.getValue()));
		for(int r=0;r<patternArr.size()&&r<maxRows;r++){
			String row=patternArr.get(r).getAsString();
			for(int c=0;c<row.length()&&c<maxCols;c++){
				char ch=row.charAt(c);
				if(ch!=' '&&keyMap.containsKey(ch)) grid.set(r*maxCols+c,keyMap.get(ch).copy());
			}
		}
	}
	private void parseFurnace(JsonObject obj,String type){
		for(int i=0;i<furnSubs.length;i++)
			if(type.equals("minecraft:"+furnSubs[i])){
				furnSubIdx=i;
				break;
			}
		furnIn=parseIngredient(obj.get("ingredient"));
		var res=obj.getAsJsonObject("result");
		furnOut=parseIngredient(res);
		furnCount=res.has("count")?res.get("count").getAsInt():1;
		furnTime=obj.has("cookingtime")?obj.get("cookingtime").getAsInt():200;
		furnXp=obj.has("experience")?obj.get("experience").getAsFloat():0.1f;
	}
	private void parseStonecutter(JsonObject obj){
		stoneIn=parseIngredient(obj.get("ingredient"));
		var res=obj.getAsJsonObject("result");
		stoneOut=parseIngredient(res);
		stoneCount=res.has("count")?res.get("count").getAsInt():1;
	}
	private void parseSmithing(JsonObject obj){
		smTemplate=parseIngredient(obj.get("template"));
		smBase=parseIngredient(obj.get("base"));
		smAddition=parseIngredient(obj.get("addition"));
		var res=obj.getAsJsonObject("result");
		smResult=parseIngredient(res);
		smCount=res.has("count")?res.get("count").getAsInt():1;
	}
	private void parseMixing(JsonObject obj,String type){
		mixBasinPress=type.equals("create:compacting");
		var ingArr=obj.getAsJsonArray("ingredients");
		int itemIdx=0, fluidIdx=0;
		for(var el: ingArr){
			if(el.isJsonObject()&&el.getAsJsonObject().has("fluid")){
				if(fluidIdx<2){
					var fObj=el.getAsJsonObject();
					FluidEntry fe=mixFluidIng.get(fluidIdx++);
					fe.proxy=parseIngredient(fObj);
					fe.amount=Math.clamp(fObj.has("amount")?fObj.get("amount").getAsInt():1000,1,1000);
				}
			}else if(itemIdx<9) mixIng.set(itemIdx++,parseIngredient(el));
		}
		var resArr=obj.getAsJsonArray("results");
		int outItemIdx=0, outFluidIdx=0;
		for(var el: resArr){
			var rObj=el.getAsJsonObject();
			if(rObj.has("fluid")||(rObj.has("amount")&&!rObj.has("count"))){
				if(outFluidIdx<2){
					FluidEntry fe=mixFluidOuts.get(outFluidIdx++);
					fe.proxy=parseIngredient(rObj);
					fe.amount=Math.clamp(rObj.has("amount")?rObj.get("amount").getAsInt():1000,1,1000);
				}
			}else if(outItemIdx<4){
				CrushingOutput co=mixOuts.get(outItemIdx++);
				applyOutput(co,rObj);
			}
		}
		String heat=obj.has("heat_requirement")?obj.get("heat_requirement").getAsString():(obj.has("heatRequirement")?obj.get("heatRequirement").getAsString():"none");
		mixHeat=heat.equalsIgnoreCase("superheated")?2:heat.equalsIgnoreCase("heated")?1:0;
		mixTime=obj.has("processingTime")?obj.get("processingTime").getAsInt():60;
	}
	private void parsePressing(JsonObject obj){
		var ingArr=obj.getAsJsonArray("ingredients");
		if(ingArr!=null&&!ingArr.isEmpty()) pressIng.set(0,parseIngredient(ingArr.get(0)));
		var resArr=obj.getAsJsonArray("results");
		if(resArr!=null&&!resArr.isEmpty()) applyOutput(pressOuts.getFirst(),resArr.get(0).getAsJsonObject());
		pressTime=obj.has("processingTime")?obj.get("processingTime").getAsInt():150;
	}
	private void parseCrushing(JsonObject obj,String type){
		isMilling=type.equals("create:milling");
		parseInOuts(obj,true);
		crushTime=obj.has("processingTime")?obj.get("processingTime").getAsInt():150;
	}
	private void parseFan(JsonObject obj,String type){
		fanHaunting=type.equals("create:haunting");
		parseInOuts(obj,false);
		fanTime=obj.has("processingTime")?obj.get("processingTime").getAsInt():200;
	}
	/**
	 * Shared parsing for crushing/fan: in slot + N outputs.
	 */
	private void parseInOuts(JsonObject obj,boolean crushing){
		var ingArr=obj.getAsJsonArray("ingredients");
		if(ingArr!=null&&!ingArr.isEmpty()){
			ItemStack input=parseIngredient(ingArr.get(0));
			if(crushing) crushIn=input;
			else fanIn=input;
		}
		var resArr=obj.getAsJsonArray("results");
		if(resArr==null) return;
		List<CrushingOutput> dst=crushing?crushOuts:fanOuts;
		int limit=crushing?8:4;
		for(int i=0;i<resArr.size()&&i<limit;i++) applyOutput(dst.get(i),resArr.get(i).getAsJsonObject());
	}
	private void applyOutput(CrushingOutput co,JsonObject rObj){
		co.stack=parseIngredient(rObj);
		co.count=rObj.has("count")?rObj.get("count").getAsInt():1;
		co.chance=rObj.has("chance")?rObj.get("chance").getAsFloat():1f;
	}
	// ── Ingredient parser ────────────────────────────────────────────────────
	public ItemStack parseIngredient(JsonElement el){
		if(el==null||el.isJsonNull()) return ItemStack.EMPTY;
		com.google.gson.JsonObject obj=null;
		if(el.isJsonObject()) obj=el.getAsJsonObject();
		else if(el.isJsonArray()){
			var arr=el.getAsJsonArray();
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
			String bucketId=fluidId+"_bucket";
			var opt=BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(bucketId));
			return opt.map(ItemStack::new).orElse(ItemStack.EMPTY);
		}
		String id=obj.has("item")?obj.get("item").getAsString():obj.has("id")?obj.get("id").getAsString():null;
		if(id==null) return ItemStack.EMPTY;
		return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id)).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}
	// ── Static helpers ───────────────────────────────────────────────────────
	public static List<ItemStack> initList(int n){
		List<ItemStack> l=new ArrayList<>(n);
		for(int i=0;i<n;i++) l.add(ItemStack.EMPTY);
		return l;
	}
	public static List<FluidEntry> initFluidList(int n){
		List<FluidEntry> l=new ArrayList<>(n);
		for(int i=0;i<n;i++) l.add(new FluidEntry());
		return l;
	}
	public static int compareSavedRecipes(File f1,File f2){
		try{
			Path base=RecipeFileWriter.getRecipeDir();
			return comparePaths(base,f1.toPath(),f2.toPath());
		}catch(Exception e){
			return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
		}
	}
	public static int comparePaths(Path base,Path p1,Path p2){
		Path r1=base.relativize(p1);
		Path r2=base.relativize(p2);
		String s1=r1.toString().replace('\\','/');
		String s2=r2.toString().replace('\\','/');
		String[] parts1=s1.split("/");
		String[] parts2=s2.split("/");
		int len=Math.min(parts1.length,parts2.length);
		for(int i=0;i<len;i++){
			boolean isFolder1=i<parts1.length-1;
			boolean isFolder2=i<parts2.length-1;
			if(isFolder1&&!isFolder2) return -1; // Folder 1 comes before File 2
			if(!isFolder1&&isFolder2) return 1;  // File 1 comes after Folder 2
			// Both are folders or both are files
			int cmp=compareNatural(parts1[i],parts2[i]);
			if(cmp!=0) return cmp;
		}
		return Integer.compare(parts1.length,parts2.length);
	}
	public static int compareNatural(String s1,String s2){
		int len1=s1.length(), len2=s2.length();
		int i1=0, i2=0;
		while(i1<len1&&i2<len2){
			char c1=s1.charAt(i1);
			char c2=s2.charAt(i2);
			if(Character.isDigit(c1)&&Character.isDigit(c2)){
				int start1=i1;
				while(i1<len1&&Character.isDigit(s1.charAt(i1))) i1++;
				int start2=i2;
				while(i2<len2&&Character.isDigit(s2.charAt(i2))) i2++;
				String numStr1=s1.substring(start1,i1);
				String numStr2=s2.substring(start2,i2);
				java.math.BigInteger n1=new java.math.BigInteger(numStr1);
				java.math.BigInteger n2=new java.math.BigInteger(numStr2);
				int cmp=n1.compareTo(n2);
				if(cmp!=0) return cmp;
			}else{
				int cmp=Character.compare(Character.toLowerCase(c1),Character.toLowerCase(c2));
				if(cmp!=0) return cmp;
				i1++;
				i2++;
			}
		}
		return Integer.compare(len1,len2);
	}
}