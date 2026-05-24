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
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
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
		LongSet forcedChunks=LongSets.EMPTY_SET;

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
		list.add(String.format(Locale.ROOT,"XYZ: %.3f / %.5f / %.3f",
				entity.getX(),entity.getY(),entity.getZ()));

		//Řádek 5 - pozice v blocích + relativně v chunku
		list.add(String.format(Locale.ROOT,"Block: %d %d %d [%d %d %d]",
				blockpos.getX(),blockpos.getY(),blockpos.getZ(),
				blockpos.getX()&15,blockpos.getY()&15,blockpos.getZ()&15));

		//Řádek 6 - chunk koordináty + region soubor
		list.add(String.format(Locale.ROOT,"Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
				chunkpos.x,SectionPos.blockToSectionCoord(blockpos.getY()),chunkpos.z,
				chunkpos.getRegionLocalX(),chunkpos.getRegionLocalZ(),
				chunkpos.getRegionX(),chunkpos.getRegionZ()));

		//Řádek 7 - směr pohledu
		String facingDescription=switch(entity.getDirection()){
			case NORTH -> "negative Z";
			case SOUTH -> "positive Z";
			case WEST -> "negative X";
			case EAST -> "positive X";
			default -> "Invalid";
		};
		list.add(String.format(Locale.ROOT,"Facing: %s (%s) (%.1f / %.1f)",
				entity.getDirection(),facingDescription,
				Mth.wrapDegrees(entity.getYRot()),
				Mth.wrapDegrees(entity.getXRot())));

		//Řádek 8 - entity
		list.add(mc.levelRenderer.getEntityStatistics());

		//Řádek 9 - světlost
		int totalLight=mc.level.getLightEngine().getRawBrightness(blockpos,0);
		int skyLight=mc.level.getBrightness(LightLayer.SKY,blockpos);
		int blockLight=mc.level.getBrightness(LightLayer.BLOCK,blockpos);
		list.add("Client Light: "+totalLight+" ("+skyLight+" sky, "+blockLight+" block)");

		//Řádek 10 - biom
		var biomeHolder=mc.level.getBiome(blockpos);
		String biomeName=biomeHolder.unwrap()
				.map(key->key.location().toString(),b->"unregistered");
		list.add("Biome: "+biomeName);

		//Řádek 11 - dimenze
		list.add("Dim: "+level.dimension().location());

		//Řádek 12 - herní den + force loaded chunky
		list.add("Day "+mc.level.getDayTime()/24000L+" | FC: "+forcedChunks.size());

		//Řádek 13 - shader
		PostChain postchain=mc.gameRenderer.currentEffect();
		list.add("Shader: "+(postchain!=null?postchain.getName():"none"));
		return list;
	}

	//collectGameInformationText volá getGameInformation() a pak přidává extra řádky
	@ModifyReturnValue(method="collectGameInformationText", at=@At("RETURN"))
	private List<String> modifyCollectedGameInfo(List<String> original){
		if(!OpmConfig.CUSTOM_DEBUG_SCREEN.get()) return original;

		//Odeber vanilla spodní řádky
		original.removeIf(line->
				line.startsWith("Debug charts:")||
						line.startsWith("For help:"));

		//AppleSkin přidává: "hunger: 20, sat: 2, exh: 0.85/4"
		String hungerLine=null;
		for(String line: original){
			if(line.startsWith("hunger:")){
				hungerLine=line;
				break;
			}
		}
		original.removeIf(line->line.startsWith("hunger:"));

		//Vlastní hint řádek
		original.add("[F3+1] Profiler [F3+2] FPS [F3+3] Ping");
		original.add("[F3+4] Tags [F3+Q] Help");

		//Hunger přepsaný na hezčí formát
		if(hungerLine!=null){
			try{

				//Hunger
				String[] parts=hungerLine.split(", ");
				String hunger=parts[0].replace("hunger: ","");
				String sat=parts[1].replace("sat: ","");
				original.add("Hunger: "+hunger+", Sat: "+sat);
			}catch(Exception e){
				original.add(hungerLine);
			}
		}
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
		list.add(String.format(Locale.ROOT,"Mem: %2d%% %03d/%03dMB",
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
		//F3+4 přepíná plné tagy vs jen počet
		boolean foundTargeted=false;
		List<String> currentTagLines=new ArrayList<>();
		for(String line: original){
			boolean b=line.contains("Targeted Block")||line.contains("Targeted Fluid")||line.contains("Targeted Entity");
			if(!foundTargeted){
				if(b){
					foundTargeted=true;
					list.add(line);
				}
				continue;
			}
			if(line.startsWith("#")){
				if(DebugScreenState.showFullTags) list.add(line);
				else currentTagLines.add(line);
			}else if(b){
				if(!DebugScreenState.showFullTags&&!currentTagLines.isEmpty()){
					list.add("Tags: "+currentTagLines.size());
					currentTagLines.clear();
				}
				list.add("");
				list.add(line);
			}else{
				if(!DebugScreenState.showFullTags&&!currentTagLines.isEmpty()){
					list.add("Tags: "+currentTagLines.size());
					currentTagLines.clear();
				}
				list.add(line);
			}
		}
		if(!DebugScreenState.showFullTags&&!currentTagLines.isEmpty()) list.add("Tags: "+currentTagLines.size());
		return list;
	}
}