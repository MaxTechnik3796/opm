package cz.maxtechnik.opm.client.recipe;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
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
	;
	public final String displayName;
	public final String stationItemId;
	StationType(String displayName,String stationItemId){
		this.displayName=displayName;
		this.stationItemId=stationItemId;
	}
	public boolean isCreate(){
		return switch(this){
			case MECH_CRAFTING,MIXING,PRESSING,CRUSHING,FAN -> true;
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
		// Vrátí fluid ResourceLocation z bucket itemu (odstraní _bucket suffix)
		public String fluidId(){
			if(isEmpty()) return "minecraft:empty";
			String id=BuiltInRegistries.ITEM.getKey(proxy.getItem()).toString();
			return id.endsWith("_bucket")?id.substring(0,id.length()-"_bucket".length()):id;
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