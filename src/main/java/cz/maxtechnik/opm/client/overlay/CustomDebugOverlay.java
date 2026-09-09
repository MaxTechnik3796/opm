package cz.maxtechnik.opm.client.overlay;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.GlUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public class CustomDebugOverlay{
	public static boolean showFullTags=false;
	public static List<String> getLeftLines(){
		Minecraft mc=Minecraft.getInstance();
		List<String> list=new ArrayList<>();
		if(mc.level==null||mc.player==null) return list;
		Entity entity=mc.getCameraEntity();
		if(entity==null) return list;
		BlockPos blockpos=entity.blockPosition();
		ChunkPos chunkpos=new ChunkPos(blockpos);
		Level level=mc.level;
		// Řádek 1 - verze Minecraftu, verze mod loaderu
		list.add("Minecraft "+SharedConstants.getCurrentVersion().getName()
				+" (NeoForge "+ModList.get().getModContainerById("neoforge")
				.map(c->c.getModInfo().getVersion().toString())
				.orElse("?")+")");
		// Řádek 2 - FPS
		list.add("FPS: "+mc.getFps());
		list.add("");
		// Řádek 3 - přesná XYZ pozice hráče
		list.add(String.format(Locale.ROOT,"XYZ: %.3f, %.5f, %.3f",
				entity.getX(),entity.getY(),entity.getZ()));
		// Řádek 4 - pozice v blocích + lokálně v chunku
		list.add(String.format(Locale.ROOT,"Block: %d, %d, %d [In Chunk: %d, %d, %d]",
				blockpos.getX(),blockpos.getY(),blockpos.getZ(),
				blockpos.getX()&15,blockpos.getY()&15,blockpos.getZ()&15));
		// Řádek 5 - chunk souřadnice
		list.add(String.format(Locale.ROOT,"Chunk: %d, %d, %d",
				chunkpos.x,SectionPos.blockToSectionCoord(blockpos.getY()),chunkpos.z));
		// Řádek 6 - region souřadnice a pozice v něm
		list.add(String.format(Locale.ROOT,"Region: %d, %d [%d, %d]",
				chunkpos.getRegionX(),chunkpos.getRegionZ(),
				chunkpos.getRegionLocalX(),chunkpos.getRegionLocalZ()));
		// Řádek 7 - směr pohledu
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
		list.add("");
		// Řádek 8 - světlost
		int totalLight=mc.level.getLightEngine().getRawBrightness(blockpos,0);
		int skyLight=mc.level.getBrightness(LightLayer.SKY,blockpos);
		int blockLight=mc.level.getBrightness(LightLayer.BLOCK,blockpos);
		list.add("Light: "+totalLight+" ("+skyLight+" sky, "+blockLight+" block)");
		// Řádek 9 - biom + dimenze
		var biomeHolder=mc.level.getBiome(blockpos);
		String biomeRaw=biomeHolder.unwrap()
				.map(key->key.location().toString(),b->"unregistered");
		String dimRaw=level.dimension().location().toString();
		String dimName=dimRaw.startsWith("minecraft:")?dimRaw.substring(10):dimRaw;
		list.add("Biome: "+biomeRaw+" ("+dimName+")");
		// Řádek 10 - entity
		String entityStats=mc.levelRenderer.getEntityStatistics();
		int commaIdx=entityStats.indexOf(',');
		String ePart=commaIdx!=-1?entityStats.substring(0,commaIdx):entityStats;
		String entityStr=ePart.replaceFirst("^E:","Entity:");
		list.add(entityStr);
		// Řádek 11 - herní den
		list.add("Day "+(mc.level.getDayTime()/24000L));
		// Řádek 12 - shader (pokud je aktivní)
		PostChain postchain=mc.gameRenderer.currentEffect();
		if(postchain!=null) list.add("Shader: "+postchain.getName());
		list.add("");
		// Vlastní hint řádky
		list.add("[F3+1] Profiler [F3+2] FPS [F3+3] Ping");
		list.add("[F3+4] Tags [F3+Q] Help");
		return list;
	}
	public static List<String> getRightLines(List<String> vanillaLines){
		Minecraft mc=Minecraft.getInstance();
		List<String> list=new ArrayList<>();
		// Řádek 1 - Java verze
		list.add("Java: "+System.getProperty("java.version"));
		// Řádek 2 - RAM
		long maxMem=Runtime.getRuntime().maxMemory();
		long totalMem=Runtime.getRuntime().totalMemory();
		long freeMem=Runtime.getRuntime().freeMemory();
		long usedMem=totalMem-freeMem;
		list.add(String.format(Locale.ROOT,"Mem: %d%% (%d / %d MB)",
				usedMem*100L/maxMem,
				usedMem/1024L/1024L,
				maxMem/1024L/1024L));
		list.add("");
		// Řádek 4 - CPU
		list.add("CPU: "+GlUtil.getCpuInfo());
		// Řádek 5 - GPU
		list.add("GPU: "+GlUtil.getRenderer());
		list.add("");
		// Řádek 7 - rozlišení okna
		list.add(String.format(Locale.ROOT,"Display: %dx%d",
				mc.getWindow().getWidth(),
				mc.getWindow().getHeight()));
		// Řádek 8 - OpenGL verze
		list.add("OpenGL: "+GlUtil.getOpenGLVersion());
		List<String> blockSection=new ArrayList<>();
		List<String> fluidSection=new ArrayList<>();
		List<String> entitySection=new ArrayList<>();
		List<String> currentSection=null;
		for(String line: vanillaLines){
			if(line.contains("Targeted Block")){
				currentSection=blockSection;
				currentSection.add(line);
			}else if(line.contains("Targeted Fluid")){
				currentSection=fluidSection;
				currentSection.add(line);
			}else if(line.contains("Targeted Entity")){
				currentSection=entitySection;
				currentSection.add(line);
			}else if(currentSection!=null){
				if(line.isEmpty()){
					currentSection=null;
				}else if(isValidTargetedLine(line)){
					currentSection.add(line);
				}else{
					currentSection=null;
				}
			}
		}
		boolean hasFluid=!fluidSection.isEmpty()&&fluidSection.stream().noneMatch(l->l.contains("minecraft:empty")||l.contains(":empty")||l.contains(":none"));
		if(hasFluid){
			addTargetedSection(list,fluidSection);
		}
		if(!blockSection.isEmpty()){
			addTargetedSection(list,blockSection);
		}
		if(!entitySection.isEmpty()){
			addTargetedSection(list,entitySection);
		}
		return list;
	}
	private static boolean isValidTargetedLine(String line){
		if(line.startsWith("#")) return true;
		if(line.contains(": ")||line.startsWith("[")) return true;
		return line.contains(":")&&!line.contains(" ");
	}
	private static void addTargetedSection(List<String> dest,List<String> sectionLines){
		if(sectionLines.isEmpty()) return;
		if(!dest.isEmpty()&&!dest.getLast().isEmpty()) dest.add("");
		List<String> tagLines=new ArrayList<>();
		for(String line: sectionLines){
			if(line.startsWith("#")){
				if(showFullTags) dest.add(line);
				else tagLines.add(line);
			}else{
				if(!showFullTags&&!tagLines.isEmpty()){
					dest.add("Tags: "+tagLines.size());
					tagLines.clear();
				}
				dest.add(line);
			}
		}
		if(!showFullTags&&!tagLines.isEmpty()) dest.add("Tags: "+tagLines.size());
	}
	public static void renderCustomLines(GuiGraphics guiGraphics,List<String> lines,boolean leftSide){
		Minecraft mc=Minecraft.getInstance();
		Font font=mc.font;
		int lineHeight=9;
		for(int i=0;i<lines.size();i++){
			String line=lines.get(i);
			if(!Strings.isNullOrEmpty(line)){
				int width=font.width(line);
				int x=leftSide?2:guiGraphics.guiWidth()-2-width;
				int y=2+lineHeight*i;
				guiGraphics.fill(x-1,y-1,x+width+1,y+lineHeight-1,0x90505050);
			}
		}
		for(int i=0;i<lines.size();i++){
			String line=lines.get(i);
			if(!Strings.isNullOrEmpty(line)){
				int width=font.width(line);
				int x=leftSide?2:guiGraphics.guiWidth()-2-width;
				int y=2+lineHeight*i;
				guiGraphics.drawString(font,line,x,y,0xE0E0E0,false);
			}
		}
	}
}
