package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.mixin.sidelist.SidelistCreator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Comparator;
import java.util.List;
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class Sidelist{
	private static boolean matrixPushed=false;
	@SubscribeEvent
	public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event){
		if(!event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)) return;
		Minecraft mc=Minecraft.getInstance();
		if(mc.level==null) return;
		if(mc.screen instanceof Config){
			event.setCanceled(true);
			Scoreboard dummyBoard=new Scoreboard();
			Objective dummyObjective=new Objective(dummyBoard,"opm_config",ObjectiveCriteria.DUMMY,Component.literal("§e§lOPM TEST SERVER"),ObjectiveCriteria.RenderType.INTEGER,true,StyledFormat.SIDEBAR_DEFAULT);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" §724/05/2026"),dummyObjective).set(7);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" "),dummyObjective).set(6);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" Player: §aSuriken222"),dummyObjective).set(5);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" Player: §bMaxTechnik"),dummyObjective).set(4);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" Rank: §4Owner"),dummyObjective).set(3);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("  "),dummyObjective).set(2);
			dummyBoard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" §9§k_na_mátové_lože_"),dummyObjective).set(1);
			dummyBoard.setDisplayObjective(DisplaySlot.SIDEBAR,dummyObjective);
			event.getGuiGraphics().pose().pushPose();
			applyOffset(event.getGuiGraphics(),mc.font,dummyObjective);
			((SidelistCreator)mc.gui).opm$displaySidebar(event.getGuiGraphics(),dummyObjective);
			event.getGuiGraphics().pose().popPose();
			return;
		}
		Scoreboard scoreboard=mc.level.getScoreboard();
		Objective objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if(objective==null) return;
		event.getGuiGraphics().pose().pushPose();
		applyOffset(event.getGuiGraphics(),mc.font,objective);
		matrixPushed=true;
	}
	@SubscribeEvent
	public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event){
		if(event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)&&matrixPushed){
			event.getGuiGraphics().pose().popPose();
			matrixPushed=false;
		}
	}
	private static void applyOffset(GuiGraphics gui,Font font,Objective objective){
		Scoreboard scoreboard=objective.getScoreboard();
		NumberFormat numberFormat=objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		List<PlayerScoreEntry> scores=scoreboard.listPlayerScores(objective).stream()
				.filter(s->!s.isHidden())
				.sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
				.limit(15)
				.toList();
		int maxTextWidth=font.width(objective.getDisplayName());
		int colonSpaceWidth=font.width(": ");
		for(PlayerScoreEntry entry: scores){
			Component name=PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()),entry.ownerName());
			Component value=entry.formatValue(numberFormat);
			int valueWidth=font.width(value);
			int entryWidth=font.width(name)+(valueWidth>0?colonSpaceWidth+valueWidth:0);
			maxTextWidth=Math.max(maxTextWidth,entryWidth);
		}
		int scoreCount=scores.size();
		int boxWidth=maxTextWidth+4;
		int boxHeight=(scoreCount*9)+10;
		int screenWidth=gui.guiWidth();
		int screenHeight=gui.guiHeight();
		int boxLeft=screenWidth-maxTextWidth-5;
		int boxTop=(screenHeight/2)+((scoreCount*9)/3)-(scoreCount*9)-10;
		int targetX=OpmModConfig.SCOREBOARD_X.get();
		int targetY=OpmModConfig.SCOREBOARD_Y.get();
		switch(OpmModConfig.SCOREBOARD_ANCHOR_X.get()){
			case RIGHT -> targetX-=boxWidth;
			case MIDDLE -> targetX-=boxWidth/2;
			default -> {
			}
		}
		switch(OpmModConfig.SCOREBOARD_ANCHOR_Y.get()){
			case BOTTOM -> targetY-=boxHeight;
			case MIDDLE -> targetY-=boxHeight/2;
			default -> {
			}
		}
		gui.pose().translate((float)(targetX-boxLeft),(float)(targetY-boxTop),0.0F);
	}
}