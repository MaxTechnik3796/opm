package cz.maxtechnik.opm.util.widget;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.config.Anchor;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.mixin.sidelist.SidelistCreator;
import cz.maxtechnik.opm.modul.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
public class OpmSidelistWidget implements OpmMovableWidget{
	@Override
	public WidgetBounds getBounds(Font font,int screenWidth,int screenHeight){
		return getDummyBounds(font,screenWidth,screenHeight);
	}
	@Override
	public int getX(){
		return OpmModConfig.SIDELIST_X.get();
	}
	@Override
	public void setX(int x){
		OpmModConfig.SIDELIST_X.set(x);
	}
	@Override
	public int getY(){
		return OpmModConfig.SIDELIST_Y.get();
	}
	@Override
	public void setY(int y){
		OpmModConfig.SIDELIST_Y.set(y);
	}
	@Override
	public Anchor.X getAnchorX(){
		return OpmModConfig.SIDELIST_ANCHOR_X.get();
	}
	@Override
	public void setAnchorX(Anchor.X anchor){
		OpmModConfig.SIDELIST_ANCHOR_X.set(anchor);
	}
	@Override
	public Anchor.Y getAnchorY(){
		return OpmModConfig.SIDELIST_ANCHOR_Y.get();
	}
	@Override
	public void setAnchorY(Anchor.Y anchor){
		OpmModConfig.SIDELIST_ANCHOR_Y.set(anchor);
	}
	@Override
	public void save(){
		OpmModConfig.SPEC.save();
	}
	private static boolean matrixPushed=false;
	public static final Scoreboard DUMMY_BOARD=new Scoreboard();
	public static final Objective DUMMY_OBJECTIVE;
	static{
		DUMMY_OBJECTIVE=new Objective(
				DUMMY_BOARD,"opm_config",ObjectiveCriteria.DUMMY,
				Component.literal("§e§lOPM TEST SERVER"),
				ObjectiveCriteria.RenderType.INTEGER,true,StyledFormat.SIDEBAR_DEFAULT
		);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" §724/05/2026"),DUMMY_OBJECTIVE).set(7);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" "),DUMMY_OBJECTIVE).set(6);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" Player: §aSuriken222"),DUMMY_OBJECTIVE).set(5);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" Player: §bMaxTechnik"),DUMMY_OBJECTIVE).set(4);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" Rank: §4Owner"),DUMMY_OBJECTIVE).set(3);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly("  "),DUMMY_OBJECTIVE).set(2);
		DUMMY_BOARD.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" §9§k_na_mátové_lože_"),DUMMY_OBJECTIVE).set(1);
		DUMMY_BOARD.setDisplayObjective(DisplaySlot.SIDEBAR,DUMMY_OBJECTIVE);
	}
	@SubscribeEvent
	public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event){
		if(!event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR)) return;
		Minecraft mc=Minecraft.getInstance();
		if(mc.level==null) return;
		if(mc.screen instanceof Config){
			event.setCanceled(true);
			event.getGuiGraphics().pose().pushPose();
			applyOffset(event.getGuiGraphics(),mc.font,DUMMY_OBJECTIVE);
			((SidelistCreator)mc.gui).opm$displaySidebar(event.getGuiGraphics(),DUMMY_OBJECTIVE);
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
	private static void applyOffset(net.minecraft.client.gui.GuiGraphics gui,Font font,Objective objective){
		getBounds(font,objective,gui.guiWidth(),gui.guiHeight()).apply(gui);
	}
	public static WidgetBounds getDummyBounds(Font font,int screenWidth,int screenHeight){
		return getBounds(font,DUMMY_OBJECTIVE,screenWidth,screenHeight);
	}
	public static WidgetBounds getBounds(Font font,Objective objective,int screenWidth,int screenHeight){
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
		int boxLeft=screenWidth-maxTextWidth-5;
		int boxTop=(screenHeight/2)+((scoreCount*9)/3)-(scoreCount*9)-10;
		int targetX=OpmModConfig.SIDELIST_X.get();
		int targetY=OpmModConfig.SIDELIST_Y.get();
		switch(OpmModConfig.SIDELIST_ANCHOR_X.get()){
			case RIGHT -> targetX-=boxWidth;
			case MIDDLE -> targetX-=boxWidth/2;
			default -> {
			}
		}
		switch(OpmModConfig.SIDELIST_ANCHOR_Y.get()){
			case BOTTOM -> targetY-=boxHeight;
			case MIDDLE -> targetY-=boxHeight/2;
			default -> {
			}
		}
		return new WidgetBounds(targetX,targetY,boxWidth,boxHeight,boxLeft,boxTop);
	}
}