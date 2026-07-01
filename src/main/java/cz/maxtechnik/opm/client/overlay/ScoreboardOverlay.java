package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.handler.HudTransformUtils;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Collection;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, bus=EventBusSubscriber.Bus.GAME, value=Dist.CLIENT)
public class ScoreboardOverlay{
	@SubscribeEvent
	public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event){
		if(event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)){
			if(!OpmConfig.SCOREBOARD_ENABLED.get()||!HudTransformUtils.shouldRender()){
				event.setCanceled(true);
				return;
			}
			applyScoreboardTransform(event);
		}
	}
	private static void applyScoreboardTransform(RenderGuiLayerEvent.Pre event){
		Minecraft mc=Minecraft.getInstance();
		int screenWidth=event.getGuiGraphics().guiWidth();
		int screenHeight=event.getGuiGraphics().guiHeight();
		double scale=OpmConfig.SCOREBOARD_SCALE.get();
		int rawXOffset=OpmConfig.SCOREBOARD_X_OFFSET.get();
		int rawYOffset=OpmConfig.SCOREBOARD_Y_OFFSET.get();
		OpmConfig.HudLocation side=OpmConfig.SCOREBOARD_SIDE.get();
		int sw=(int)(getScoreboardWidth(mc)*scale);
		int sh=(int)(getScoreboardHeight(mc)*scale);
		int baseSX=(side==OpmConfig.HudLocation.RIGHT)?(screenWidth-4-sw):4;
		int xOffset=Math.clamp(rawXOffset,4-baseSX,screenWidth-4-sw-baseSX);
		int yOffset=Math.clamp(rawYOffset,0,screenHeight-4-sh-4);
		int sx=(side==OpmConfig.HudLocation.RIGHT)?screenWidth-4-sw+xOffset:4+xOffset;
		int sy=4+yOffset;
		int vx=screenWidth-getScoreboardWidth(mc);
		int unscaledHeight=getScoreboardHeight(mc);
		int scoresCount=Math.max(0,(unscaledHeight/9)-1);
		int i1=scoresCount*9;
		int j1=screenHeight/2+i1/3;
		int vy=j1-i1-9;
		var pose=event.getGuiGraphics().pose();
		pose.pushPose();
		pose.translate(sx-scale*vx,sy-scale*vy,0);
		if(scale!=1.0)
			pose.scale((float)scale,(float)scale,1.0f);
	}
	@SubscribeEvent
	public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event){
		if(event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)){
			if(OpmConfig.SCOREBOARD_ENABLED.get()&&HudTransformUtils.shouldRender()){
				event.getGuiGraphics().pose().popPose();
			}
		}
	}
	public static final String[] MOCK_PLAYERS={
			"§724/05/2026",
			" ",
			"Player: §fSuriken222",
			"Player: §fMaxTechnik",
			"Rank: §4Owner",
			"  ",
			"§ephttps://discord.gg/mjVprFtWTk"
	};
	public static final String[] MOCK_SCORES={
			"10","9","8","7","6","5","4","3","2","1"
	};
	public static java.util.List<net.minecraft.world.scores.PlayerScoreEntry> getActiveScores(Scoreboard scoreboard,Objective objective){
		Collection<net.minecraft.world.scores.PlayerScoreEntry> allScores=scoreboard.listPlayerScores(objective);
		java.util.List<net.minecraft.world.scores.PlayerScoreEntry> list=new java.util.ArrayList<>();
		for(net.minecraft.world.scores.PlayerScoreEntry entry: allScores){
			if(!entry.isHidden()){
				list.add(entry);
			}
		}
		list.sort((a,b)->Integer.compare(b.value(),a.value()));
		return list.size()>15?list.subList(0,15):list;
	}
	public static int getMockScoreboardWidth(Minecraft mc){
		int maxW=mc.font.width("§e§lOPM TEST SERVER");
		for(int i=0;i<MOCK_PLAYERS.length;i++){
			int nameW=mc.font.width(MOCK_PLAYERS[i]);
			int scoreW=mc.font.width(MOCK_SCORES[i]);
			maxW=Math.max(maxW,nameW+scoreW+15);
		}
		return maxW+2;
	}
	public static int getMockScoreboardHeight(){
		return (MOCK_PLAYERS.length+1)*9;
	}
	public static int getScoreboardWidth(Minecraft mc){
		if(mc.level==null) return getMockScoreboardWidth(mc);
		Scoreboard scoreboard=mc.level.getScoreboard();
		Objective objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if(objective==null) return getMockScoreboardWidth(mc);
		int maxW=mc.font.width(objective.getDisplayName());
		try{
			java.util.List<net.minecraft.world.scores.PlayerScoreEntry> scores=getActiveScores(scoreboard,objective);
			for(net.minecraft.world.scores.PlayerScoreEntry entry: scores){
				net.minecraft.world.scores.PlayerTeam team=scoreboard.getPlayersTeam(entry.owner());
				net.minecraft.network.chat.Component nameComp=team!=null?team.getFormattedName(entry.ownerName()):entry.ownerName();
				int nameW=mc.font.width(nameComp);
				net.minecraft.network.chat.Component scoreComp=entry.formatValue(objective.numberFormatOrDefault(net.minecraft.network.chat.numbers.StyledFormat.NO_STYLE));
				int scoreW=mc.font.width(scoreComp);
				maxW=Math.max(maxW,nameW+scoreW+15);
			}
		}catch(Exception ignored){
		}
		return maxW+2;
	}
	public static int getScoreboardHeight(Minecraft mc){
		if(mc.level==null) return getMockScoreboardHeight();
		Scoreboard scoreboard=mc.level.getScoreboard();
		Objective objective=scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if(objective==null) return getMockScoreboardHeight();
		int lines=1; // title
		try{
			lines+=getActiveScores(scoreboard,objective).size();
		}catch(Exception e){
			lines+=3;
		}
		return lines*9;
	}
}
