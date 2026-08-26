package cz.maxtechnik.opm.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.GlUtil;
import cz.maxtechnik.opm.client.handler.DebugScreenState;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin{
	//LEVÝ SLOUPEC
	@ModifyReturnValue(method="getGameInformation", at=@At("RETURN"))
	private List<String> modifyGameInfo(List<String> original){
		if(!cz.maxtechnik.opm.init.OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return original;
		Minecraft mc=Minecraft.getInstance();
		if(mc.level==null||mc.player==null) return original;
		List<String> list=new ArrayList<>();
		Entity entity=mc.getCameraEntity();
		assert entity!=null;
		BlockPos blockpos=entity.blockPosition();
		ChunkPos chunkpos=new ChunkPos(blockpos);
		Level level=mc.level;
		//Řádek 1 - verze Minecraftu, verze launcheru, název mod loaderu
		list.add("Minecraft "+SharedConstants.getCurrentVersion().getName()
				+" (NeoForge "+ModList.get().getModContainerById("neoforge")
				.map(c->c.getModInfo().getVersion().toString())
				.orElse("?")+")");
		//Řádek 2 - FPS
		list.add("FPS: "+mc.getFps());
		//Řádek 3 - PRÁZDNÝ
		list.add("");
		//Řádek 4 - přesná XYZ pozice hráče
		list.add(String.format(Locale.ROOT,"XYZ: %.3f, %.5f, %.3f",
				entity.getX(),entity.getY(),entity.getZ()));
		//Řádek 5 - pozice v blocích + lokálně v chunku
		list.add(String.format(Locale.ROOT,"Block: %d, %d, %d [In Chunk: %d, %d, %d]",
				blockpos.getX(),blockpos.getY(),blockpos.getZ(),
				blockpos.getX()&15,blockpos.getY()&15,blockpos.getZ()&15));
		//Řádek 6 - chunk souřadnice
		list.add(String.format(Locale.ROOT,"Chunk: %d, %d, %d",
				chunkpos.x,SectionPos.blockToSectionCoord(blockpos.getY()),chunkpos.z));
		//Řádek 7 - region souřadnice a pozice v něm
		list.add(String.format(Locale.ROOT,"Region: %d, %d [%d, %d]",
				chunkpos.getRegionX(),chunkpos.getRegionZ(),
				chunkpos.getRegionLocalX(),chunkpos.getRegionLocalZ()));
		//Řádek 8 - směr pohledu
		String facingDescription=switch(entity.getDirection()){
			case NORTH -> "-Z";
			case SOUTH -> "+Z";
			case WEST -> "-X";
			case EAST -> "+X";
			default -> "Invalid";
		};
		String dirName=Character.toUpperCase(entity.getDirection().getName().charAt(0))
				+entity.getDirection().getName().substring(1);
		list.add(String.format(Locale.ROOT,"Facing: %s (%s) [%.1f° / %.1f°]",
				dirName,facingDescription,
				Mth.wrapDegrees(entity.getYRot()),
				Mth.wrapDegrees(entity.getXRot())));
		//Oddělovač
		list.add("");
		//Řádek 9 - světlost
		int totalLight=mc.level.getLightEngine().getRawBrightness(blockpos,0);
		int skyLight=mc.level.getBrightness(LightLayer.SKY,blockpos);
		int blockLight=mc.level.getBrightness(LightLayer.BLOCK,blockpos);
		list.add("Light: "+totalLight+" ("+skyLight+" sky, "+blockLight+" block)");
		//Řádek 10 - biom + dimenze
		var biomeHolder=mc.level.getBiome(blockpos);
		String biomeRaw=biomeHolder.unwrap()
				.map(key->key.location().toString(),b->"unregistered");
		String dimRaw=level.dimension().location().toString();
		String dimName=dimRaw.startsWith("minecraft:")?dimRaw.substring(10):dimRaw;
		list.add("Biome: "+biomeRaw+" ("+dimName+")");
		//Řádek 11 - entity
		String entityStats=mc.levelRenderer.getEntityStatistics();
		int commaIdx=entityStats.indexOf(',');
		String ePart=commaIdx!=-1?entityStats.substring(0,commaIdx):entityStats;
		String entityStr=ePart.replaceFirst("^E:","Entity:");
		list.add(entityStr);
		//Řádek 12 - herní den
		list.add("Day "+(mc.level.getDayTime()/24000L));
		//Řádek 13 - shader (pouze pokud je aktivní)
		PostChain postchain=mc.gameRenderer.currentEffect();
		if(postchain!=null) list.add("Shader: "+postchain.getName());
		return list;
	}
	//collectGameInformationText volá getGameInformation() a pak přidává extra řádky
	@ModifyReturnValue(method="collectGameInformationText", at=@At("RETURN"))
	private List<String> modifyCollectedGameInfo(List<String> original){
		if(!OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return original;
		//Odeber vanilla spodní řádky a případný hunger
		original.removeIf(line->
				line.startsWith("Debug charts:")||
						line.startsWith("For help:")||
						line.startsWith("hunger:"));
		//Vlastní hint řádek
		original.add("[F3+1] Profiler [F3+2] FPS [F3+3] Ping");
		original.add("[F3+4] Tags [F3+Q] Help");
		DebugScreenState.lastLeftList.clear();
		DebugScreenState.lastLeftList.addAll(original);
		return original;
	}
	//PRAVÝ SLOUPEC
	@ModifyReturnValue(method="getSystemInformation", at=@At("RETURN"))
	private List<String> modifySystemInfo(List<String> original){
		if(!cz.maxtechnik.opm.init.OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return original;
		Minecraft mc=Minecraft.getInstance();
		List<String> list=new ArrayList<>();
		//Řádek 1 - Java verze
		list.add("Java: "+System.getProperty("java.version"));
		//Řádek 2 - RAM
		long maxMem=Runtime.getRuntime().maxMemory();
		long totalMem=Runtime.getRuntime().totalMemory();
		long freeMem=Runtime.getRuntime().freeMemory();
		long usedMem=totalMem-freeMem;
		list.add(String.format(Locale.ROOT,"Mem: %d%% (%d / %d MB)",
				usedMem*100L/maxMem,
				usedMem/1024L/1024L,
				maxMem/1024L/1024L));
		//Řádek 3 - PRÁZDNÝ
		list.add("");
		//Řádek 4 - CPU
		list.add("CPU: "+GlUtil.getCpuInfo());
		//Řádek 5 - GPU
		list.add("GPU: "+GlUtil.getRenderer());
		// Řádek 6 - PRÁZDNÝ
		list.add("");
		//Řádek 7 - rozlišení okna
		list.add(String.format(Locale.ROOT,"Display: %dx%d",
				mc.getWindow().getWidth(),
				mc.getWindow().getHeight()));
		//Řádek 8 - OpenGL verze
		list.add("OpenGL: "+GlUtil.getOpenGLVersion());
		//Řádek 9 - PRÁZDNÝ
		list.add("");
		//TARGETED BLOCK / FLUID / ENTITY
		List<String> blockSection=new ArrayList<>();
		List<String> fluidSection=new ArrayList<>();
		List<String> entitySection=new ArrayList<>();
		List<String> currentSection=null;
		for(String line: original){
			if(line.contains("Targeted Block")){
				currentSection=blockSection;
			}else if(line.contains("Targeted Fluid")){
				currentSection=fluidSection;
			}else if(line.contains("Targeted Entity")){
				currentSection=entitySection;
			}
			if(currentSection!=null){
				currentSection.add(line);
			}
		}
		// Pořadí: nejdříve tekutina (pokud existuje a není prázdná), pak blok pod ní, pak entita
		boolean hasFluid=!fluidSection.isEmpty()&&fluidSection.stream().noneMatch(l->l.contains("minecraft:empty")||l.contains(":empty")||l.contains(":none"));
		if(hasFluid){
			opm$addTargetedSection(list,fluidSection);
		}
		if(!blockSection.isEmpty()){
			opm$addTargetedSection(list,blockSection);
		}
		if(!entitySection.isEmpty()){
			opm$addTargetedSection(list,entitySection);
		}
		DebugScreenState.lastRightList.clear();
		DebugScreenState.lastRightList.addAll(list);
		return list;
	}
	@Unique
	private void opm$addTargetedSection(List<String> dest,List<String> sectionLines){
		if(sectionLines.isEmpty()) return;
		if(!dest.isEmpty()&&!dest.getLast().isEmpty()) dest.add("");
		List<String> tagLines=new ArrayList<>();
		for(String line: sectionLines){
			if(line.startsWith("#")){
				if(DebugScreenState.showFullTags) dest.add(line);
				else tagLines.add(line);
			}else{
				if(!DebugScreenState.showFullTags&&!tagLines.isEmpty()){
					dest.add("Tags: "+tagLines.size());
					tagLines.clear();
				}
				dest.add(line);
			}
		}
		if(!DebugScreenState.showFullTags&&!tagLines.isEmpty()) dest.add("Tags: "+tagLines.size());
	}
}