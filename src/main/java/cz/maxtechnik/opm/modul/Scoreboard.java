package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.config.OpmModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Comparator;
import java.util.List;
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class Scoreboard{
	private record CustomScoreLine(String label,String value){
	}
	private static boolean matrixPushed=false;
	private static void offset(RenderGuiLayerEvent.Pre event){
		if(event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)){
			Minecraft mc=Minecraft.getInstance();
			if(mc.level==null) return;
			net.minecraft.world.scores.Scoreboard scoreboard=mc.level.getScoreboard();
			Objective objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
			if(objective==null) return;
			Font font=mc.font;
			NumberFormat numberFormat=objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
			List<PlayerScoreEntry> scores=scoreboard.listPlayerScores(objective).stream().filter(s->!s.isHidden()).sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()).limit(15).toList();
			int maxTextWidth=font.width(objective.getDisplayName());
			for(PlayerScoreEntry entry: scores){
				Component name=PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()),Component.literal(entry.ownerName().getString()));
				Component value=entry.formatValue(numberFormat);
				maxTextWidth=Math.max(maxTextWidth,font.width(name)+font.width("   ")+font.width(value));
			}
			int scoreCount=scores.size();
			int totalWidth=maxTextWidth;
			int totalHeight=(scoreCount+1)*9;
			int screenWidth=event.getGuiGraphics().guiWidth();
			int screenHeight=event.getGuiGraphics().guiHeight();
			int vanillaLeft=screenWidth-totalWidth-3;
			int vanillaTop=(screenHeight/2)+(scoreCount*9/3)-totalHeight;
			int targetX=OpmModConfig.SCOREBOARD_X.get();
			int targetY=OpmModConfig.SCOREBOARD_Y.get();
			switch(OpmModConfig.SCOREBOARD_ANCHOR_X.get()){
				case RIGHT -> targetX=targetX-totalWidth;
				case MIDDLE -> targetX=targetX-totalWidth/2;
				default -> {
				}
			}
			switch(OpmModConfig.SCOREBOARD_ANCHOR_Y.get()){
				case BOTTOM -> targetY=targetY-totalHeight;
				case MIDDLE -> targetY=targetY-totalHeight/2;
				default -> {
				}
			}
			event.getGuiGraphics().pose().pushPose();
			event.getGuiGraphics().pose().translate(targetX-vanillaLeft-4,targetY-vanillaTop+1,0.0F);
			matrixPushed=true;
		}
	}
	private static void debugInConfig(RenderGuiLayerEvent.Pre event){
		Minecraft mc=Minecraft.getInstance();
		if(mc.screen instanceof Config){
			event.setCanceled(true);
			List<CustomScoreLine> customData=List.of(
					new CustomScoreLine("OPM Debug","v1.0"),
					new CustomScoreLine("Hand Item",mc.player!=null?mc.player.getMainHandItem().getHoverName().getString():"-"),
					new CustomScoreLine("Copy Mode","Active")
			);
			GuiGraphics gui=event.getGuiGraphics();
			int maxTextWidth=mc.font.width("INSPECTOR HUD");
			for(CustomScoreLine line: customData){
				int width=mc.font.width(line.label())+mc.font.width("   ")+mc.font.width(line.value());
				maxTextWidth=Math.max(maxTextWidth,width);
			}
			int totalWidth=maxTextWidth+4;
			int lineHeight=9;
			int totalHeight=(customData.size()+1)*lineHeight;
			gui.fill(10,10,10+totalWidth,10+totalHeight,0x60000000);
			gui.drawString(mc.font,"INSPECTOR HUD",10+(totalWidth-mc.font.width("INSPECTOR HUD"))/2,10+1,0xFFFFFFFF,false);
			for(int i=0;i<customData.size();i++){
				CustomScoreLine line=customData.get(i);
				int rowY=10+(i+1)*lineHeight;
				gui.drawString(mc.font,line.label(),10+2,rowY,0xFFE0E0E0,false);
				gui.drawString(mc.font,line.value(),10+totalWidth-mc.font.width(line.value())-2,rowY,0xFFFF5555,false);
			}
		}
	}
	@SubscribeEvent
	public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event){
		debugInConfig(event);
		offset(event);
	}
	@SubscribeEvent
	public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event){
		if(event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)&&matrixPushed){
			event.getGuiGraphics().pose().popPose();
			matrixPushed=false;
		}
	}
}