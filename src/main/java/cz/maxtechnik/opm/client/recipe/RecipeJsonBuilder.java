package cz.maxtechnik.opm.client.recipe;

import static cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.*;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RecipeJsonBuilder{
	private RecipeJsonBuilder(){
	}

	//StationType a pomocné datové struktury ─────────────────────────────
	public enum StationType{
		CRAFTING("Crafting","minecraft:crafting_table"),
		FURNACE("Furnace","minecraft:furnace"),
		STONECUTTER("Stonecutter","minecraft:stonecutter"),
		SMITHING("Smithing","minecraft:smithing_table"),
		MECH_CRAFTING("Mech. Crafter","create:mechanical_crafter"),
		MIXING("Basin","create:basin"),
		PRESSING("Pressing","create:mechanical_press"),
		FAN("Fan","create:encased_fan"),
		CRUSHING("Crushing","create:crushing_wheel"),
		DEPLOYING("Deploying","create:deployer"),
		FILLING("Spouting","create:spout"),
		;
		public final String displayName;
		public final String stationItemId;
		StationType(String displayName,String stationItemId){
			this.displayName=displayName;
			this.stationItemId=stationItemId;
		}
		public boolean isCreate(){
			return switch(this){
				case MECH_CRAFTING,MIXING,PRESSING,CRUSHING,FAN,DEPLOYING,FILLING -> true;
				default -> false;
			};
		}
		//výstup s šancí a počtem (crushing, fan, mixing, pressing)
		public static final class CrushingOutput{
			public ItemStack stack;
			public float chance;
			public int count;
			public CrushingOutput(){
				this.stack=ItemStack.EMPTY;
				this.chance=1F;
				this.count=1;
			}
			public boolean isEmpty(){
				return stack==null||stack.isEmpty();
			}
		}
		//fluid záznam
		public static final class FluidEntry{
			public ItemStack proxy=ItemStack.EMPTY;
			public int amount=1000;
			public FluidEntry(){
			}
			public boolean isEmpty(){
				return proxy==null||proxy.isEmpty();
			}
			// Vrátí fluid ResourceLocation z bucketu nebo fluid containeru
			public String fluidId(){
				if(isEmpty()) return "minecraft:empty";
				try{
					var opt=net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(proxy);
					if(opt.isPresent()&&!opt.get().isEmpty()){
						net.minecraft.world.level.material.Fluid f=opt.get().getFluid();
						net.minecraft.resources.ResourceLocation loc=BuiltInRegistries.FLUID.getKey(f);
						if(loc!=null&&!loc.getPath().equals("empty")){
							return loc.toString();
						}
					}
				}catch(Throwable ignored){
				}
				if(proxy.getItem() instanceof net.minecraft.world.item.BucketItem bucketItem){
					try{
						net.minecraft.world.level.material.Fluid f=bucketItem.content;
						if(f!=null){
							net.minecraft.resources.ResourceLocation loc=BuiltInRegistries.FLUID.getKey(f);
							if(loc!=null&&!loc.getPath().equals("empty")){
								return loc.toString();
							}
						}
					}catch(Throwable ignored){
					}
				}
				String id=BuiltInRegistries.ITEM.getKey(proxy.getItem()).toString();
				if(id.endsWith("_bucket")){
					return id.substring(0,id.length()-"_bucket".length());
				}
				return id;
			}
		}
		//cesta k adresáři s recepty
		public static final class RecipeFileWriter{
			private RecipeFileWriter(){
			}
			public static Path getRecipeDir(){
				try{
					String world=OpmConfig.WORLD_NAME.get().trim();
					String dpName=OpmConfig.DATAPACK_NAME.get().trim();
					if(!world.isEmpty()&&!dpName.isEmpty()){
						Path gameDir=Minecraft.getInstance().gameDirectory.toPath();
						Path datapackDir=gameDir.resolve("saves").resolve(world).resolve("datapacks").resolve(dpName);
						if(Files.exists(datapackDir)){
							String rf=OpmConfig.RECIPE_FOLDER.get().trim();
							if(!rf.isEmpty()){
								return datapackDir.resolve("data").resolve(rf).resolve("recipe");
							}
							Path dataDir=datapackDir.resolve("data");
							if(Files.exists(dataDir)){
								try(var stream=Files.list(dataDir)){
									for(Path nsDir: stream.toList()){
										if(Files.isDirectory(nsDir)){
											Path rDir=nsDir.resolve("recipe");
											if(Files.exists(rDir)) return rDir;
										}
									}
								}
							}
							return datapackDir.resolve("data").resolve(dpName).resolve("recipe");
						}
					}
				}catch(Exception ignored){
				}
				return Minecraft.getInstance().gameDirectory.toPath()
						.resolve("config").resolve("opm").resolve("recipes");
			}
		}
	}
	//Crafting ────────────────────────────────────────────────────────
	public static String buildShaped(List<ItemStack> grid,int gridW,int gridH,ItemStack result,int count){
		return buildPatternBased("minecraft:crafting_shaped",grid,gridW,gridH,
				result,count,"ABCDEFGHIJKLMNOPQRSTUVWXYZ",false,false);
	}
	public static String buildMechCrafting(List<ItemStack> grid,int gridW,int gridH,ItemStack result,int count,boolean acceptMirrored){
		String sym="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"+"#@$%&*+-/:;<=>?^_{}|~!.,()[]";
		return buildPatternBased("create:mechanical_crafting",grid,gridW,gridH,result,count,sym,true,acceptMirrored);
	}
	//Sdílený builder pro shaped a mechanical_crafting
	private static String buildPatternBased(String type,List<ItemStack> grid,int gridW,int gridH,ItemStack result,int count,String symbols,boolean keyBeforePattern,boolean acceptMirrored){
		Map<String,Character> idToChar=new LinkedHashMap<>();
		char[][] pattern=new char[gridH][gridW];
		for(int r=0;r<gridH;r++)
			for(int c=0;c<gridW;c++){
				int idx=r*gridW+c;
				ItemStack s=safeGet(grid,idx);
				if(s.isEmpty()){
					pattern[r][c]=' ';
					continue;
				}
				String id=id(s);
				if(!idToChar.containsKey(id)){
					int ci=idToChar.size();
					idToChar.put(id,ci<symbols.length()?symbols.charAt(ci):'?');
				}
				pattern[r][c]=idToChar.get(id);
			}
		//Trim okrajových prázdných řádků/sloupců
		int minR=gridH, maxR=-1, minC=gridW, maxC=-1;
		for(int r=0;r<gridH;r++)
			for(int c=0;c<gridW;c++)
				if(pattern[r][c]!=' '){
					minR=Math.min(minR,r);
					maxR=Math.max(maxR,r);
					minC=Math.min(minC,c);
					maxC=Math.max(maxC,c);
				}
		if(maxR<0){
			minR=0;
			maxR=0;
			minC=0;
			maxC=0;
		}
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"").append(type).append("\",\n");
		if(keyBeforePattern){
			sb.append("  \"accept_mirrored\": ").append(acceptMirrored).append(",\n");
			appendKey(sb,idToChar);
			appendPattern(sb,pattern,minR,maxR,minC,maxC);
		}else{
			appendPattern(sb,pattern,minR,maxR,minC,maxC);
			appendKey(sb,idToChar);
		}
		appendResult(sb,result,count);
		sb.append("\n}");
		return sb.toString();
	}
	private static void appendPattern(StringBuilder sb,char[][] pattern,int minR,int maxR,int minC,int maxC){
		sb.append("  \"pattern\": [\n");
		for(int r=minR;r<=maxR;r++){
			sb.append("    \"");
			for(int c=minC;c<=maxC;c++) sb.append(pattern[r][c]);
			sb.append("\"");
			if(r<maxR) sb.append(",");
			sb.append("\n");
		}
		sb.append("  ],\n");
	}
	private static void appendKey(StringBuilder sb,Map<String,Character> idToChar){
		sb.append("  \"key\": {\n");
		var entries=idToChar.entrySet().stream().toList();
		for(int i=0;i<entries.size();i++){
			var e=entries.get(i);
			sb.append("    \"").append(e.getValue()).append("\": ").append(formatIngredient(e.getKey()));
			if(i<entries.size()-1) sb.append(",");
			sb.append("\n");
		}
		sb.append("  },\n");
	}
	private static void appendResult(StringBuilder sb,ItemStack result,int count){
		sb.append("  \"result\": { \"id\": \"").append(id(result)).append("\"");
		if(count>1) sb.append(", \"count\": ").append(count);
		sb.append(" }");
	}
	public static String buildShapeless(List<ItemStack> ingredients,ItemStack result,int count){
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"minecraft:crafting_shapeless\",\n");
		sb.append("  \"ingredients\": [\n");
		boolean first=true;
		for(ItemStack s: ingredients){
			if(s==null||s.isEmpty()) continue;
			if(!first) sb.append(",\n");
			sb.append("    ").append(formatIngredient(id(s)));
			first=false;
		}
		sb.append("\n  ],\n");
		appendResult(sb,result,count);
		sb.append("\n}");
		return sb.toString();
	}
	//Furnace family ────────────────────────────────────────────────────────
	public static String buildFurnace(String subType,ItemStack input,ItemStack result,int count,int cookTime,float xp){
		return "{\n"+
				"  \"type\": \"minecraft:"+subType+"\",\n"+
				"  \"ingredient\": "+formatIngredient(id(input))+",\n"+
				"  \"result\": { \"id\": \""+id(result)+"\""+
				(count>1?", \"count\": "+count:"")+" },\n"+
				"  \"experience\": "+String.format(Locale.ROOT,"%.1f",xp)+",\n"+
				"  \"cookingtime\": "+cookTime+"\n}";
	}
	public static String buildStonecutter(ItemStack input,ItemStack result,int count){
		return "{\n"+
				"  \"type\": \"minecraft:stonecutting\",\n"+
				"  \"ingredient\": "+formatIngredient(id(input))+",\n"+
				"  \"result\": { \"id\": \""+id(result)+"\""+
				(count>1?", \"count\": "+count:"")+" }\n}";
	}
	public static String buildSmithing(ItemStack template,ItemStack base,ItemStack addition,ItemStack result,int count){
		return "{\n"+
				"  \"type\": \"minecraft:smithing_transform\",\n"+
				"  \"template\": "+formatIngredient(id(template))+",\n"+
				"  \"base\":     "+formatIngredient(id(base))+",\n"+
				"  \"addition\": "+formatIngredient(id(addition))+",\n"+
				"  \"result\": { \"id\": \""+id(result)+"\""+
				(count>1?", \"count\": "+count:"")+" }\n}";
	}
	//Create: Mixing (mixer / compacting)
	public static String buildMixing(String type,List<ItemStack> ingredients,List<FluidEntry> fluidIngredients,List<CrushingOutput> results,List<FluidEntry> fluidResults,String heat){
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"").append(type).append("\",\n");
		sb.append("  \"ingredients\": [\n");
		boolean[] first={true};
		appendItemIngredients(sb,ingredients,first);
		appendFluidIngredients(sb,fluidIngredients,first);
		sb.append("\n  ],\n");
		sb.append("  \"results\": [\n");
		first[0]=true;
		appendCrushingOutputs(sb,results,first);
		appendFluidOutputs(sb,fluidResults,first);
		sb.append("\n  ]");
		if(!heat.equals("none")) sb.append(",\n  \"heat_requirement\": \"").append(heat).append("\"\n}");
		else sb.append("\n}");
		return sb.toString();
	}
	//Create: Pressing
	public static String buildPressing(ItemStack input,List<CrushingOutput> results){
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"create:pressing\",\n");
		appendSimpleIngredients(sb,List.of(input));
		sb.append("  \"results\": [\n");
		boolean[] first={true};
		appendCrushingOutputs(sb,results,first);
		sb.append("\n  ]\n}");
		return sb.toString();
	}
	//Create: Crushing / Milling / Splashing / Haunting
	public static String buildCrushing(String createType,ItemStack input,List<CrushingOutput> outputs,int processingTime){
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"").append(createType).append("\",\n");
		appendSimpleIngredients(sb,List.of(input));
		sb.append("  \"results\": [\n");
		boolean[] first={true};
		appendCrushingOutputs(sb,outputs,first);
		sb.append("\n  ],\n");
		sb.append("  \"processingTime\": ").append(processingTime).append("\n}");
		return sb.toString();
	}
	//Create: Item Application (Deploying)
	public static String buildItemApplication(ItemStack target,ItemStack tool,ItemStack result){
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"create:item_application\",\n");
		appendSimpleIngredients(sb,List.of(target,tool));
		sb.append("  \"results\": [\n    { \"id\": \"").append(id(result)).append("\" }\n  ]\n}");
		return sb.toString();
	}
	//Create: Filling (Spouting)
	public static String buildFilling(ItemStack input,FluidEntry fluid,ItemStack result){
		var sb=new StringBuilder();
		sb.append("{\n  \"type\": \"create:filling\",\n");
		sb.append("  \"ingredients\": [\n");
		boolean[] first={true};
		appendItemIngredients(sb,List.of(input),first);
		appendFluidIngredients(sb,List.of(fluid),first);
		sb.append("\n  ],\n");
		sb.append("  \"results\": [\n    { \"id\": \"").append(id(result)).append("\" }\n  ]\n}");
		return sb.toString();
	}
	//Helpers ────────────────────────────────────────────────────────
	private static void appendSimpleIngredients(StringBuilder sb,List<ItemStack> ingredients){
		sb.append("  \"ingredients\": [\n");
		boolean[] first={true};
		appendItemIngredients(sb,ingredients,first);
		sb.append("\n  ],\n");
	}
	private static void appendItemIngredients(StringBuilder sb,List<ItemStack> ingredients,boolean[] first){
		for(ItemStack s: ingredients){
			if(s==null||s.isEmpty()) continue;
			int c=s.getCount();
			for(int i=0;i<c;i++){
				if(!first[0]) sb.append(",\n");
				sb.append("    ").append(formatIngredient(id(s)));
				first[0]=false;
			}
		}
	}
	private static void appendFluidIngredients(StringBuilder sb,List<FluidEntry> fluids,boolean[] first){
		if(fluids==null) return;
		for(FluidEntry f: fluids){
			if(f==null||f.isEmpty()) continue;
			if(!first[0]) sb.append(",\n");
			sb.append("    { \"type\": \"neoforge:single\", \"fluid\": \"").append(fluidId(f))
					.append("\", \"amount\": ").append(f.amount).append(" }");
			first[0]=false;
		}
	}
	private static void appendCrushingOutputs(StringBuilder sb,List<CrushingOutput> outputs,boolean[] first){
		for(CrushingOutput o: outputs){
			if(o==null||o.isEmpty()) continue;
			if(!first[0]) sb.append(",\n");
			sb.append("    ");
			appendCrushingOutput(sb,o);
			first[0]=false;
		}
	}
	private static void appendCrushingOutput(StringBuilder sb,CrushingOutput o){
		sb.append("{ \"id\": \"").append(id(o.stack)).append("\"");
		if(o.count>1) sb.append(", \"count\": ").append(o.count);
		if(o.chance<1.0f) sb.append(", \"chance\": ").append(String.format(Locale.ROOT,"%.2f",o.chance));
		sb.append(" }");
	}
	private static void appendFluidOutputs(StringBuilder sb,List<FluidEntry> fluids,boolean[] first){
		if(fluids==null) return;
		for(FluidEntry f: fluids){
			if(f==null||f.isEmpty()) continue;
			if(!first[0]) sb.append(",\n");
			sb.append("    { \"id\": \"").append(fluidId(f)).append("\", \"amount\": ").append(f.amount).append(" }");
			first[0]=false;
		}
	}
	public static String formatIngredient(String id){
		if(id.startsWith("#")) return "{ \"tag\": \""+id.substring(1)+"\" }";
		return "{ \"item\": \""+id+"\" }";
	}
	public static String id(ItemStack stack){
		if(stack==null||stack.isEmpty()) return "minecraft:air";
		if(stack.getItem()==net.minecraft.world.item.Items.NAME_TAG&&stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)){
			String name=stack.getHoverName().getString();
			if(name.startsWith("#")) return name;
		}
		ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
		return loc.toString();
	}
	public static String fluidId(FluidEntry entry){
		return entry==null||entry.isEmpty()?"minecraft:empty":entry.fluidId();
	}
	private static ItemStack safeGet(List<ItemStack> list,int idx){
		return (idx>=0&&idx<list.size()&&list.get(idx)!=null)?list.get(idx):ItemStack.EMPTY;
	}
}