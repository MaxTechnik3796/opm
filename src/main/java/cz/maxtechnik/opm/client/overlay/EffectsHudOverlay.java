package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
public class EffectsHudOverlay implements LayeredDraw.Layer{
	private static final int ICON_SIZE=18;
	private static final int GAP=2;
	private static final int EDGE_PADDING=4;
	private static final int WIDGET_WIDTH=40;
	private static final int BG_NEGATIVE=0xAA450000;
	private static final int BG_NEUTRAL=0xAA000000;
	@Override
	public void render(@NotNull GuiGraphics graphics,@NotNull DeltaTracker deltaTracker){
		Minecraft mc=Minecraft.getInstance();
		if(mc.player==null||mc.options.hideGui) return;
		if(cz.maxtechnik.opm.client.handler.F1Handler.shouldHideHUD()) return;
		if(mc.screen instanceof cz.maxtechnik.opm.client.screen.OpmConfigScreen) return;
		if(mc.getDebugOverlay().showDebugScreen()) return;
		if(!OpmConfig.EFFECTS_HUD_ENABLED.get()) return;
		Collection<MobEffectInstance> effects=mc.player.getActiveEffects();
		if(effects.isEmpty()) return;
		int screenWidth=graphics.guiWidth();
		int screenHeight=graphics.guiHeight();
		OpmConfig.HudLocation loc=OpmConfig.EFFECTS_HUD_LOCATION.get();
		boolean onRight=loc!=OpmConfig.HudLocation.LEFT;
		double scale=OpmConfig.EFFECTS_HUD_SCALE.get();
		int effW=(int)((WIDGET_WIDTH+8)*scale);
		int effWidgetH=(int)((ICON_SIZE+GAP)*scale);
		int startY=EDGE_PADDING+OpmConfig.EFFECTS_HUD_Y_OFFSET.get();
		int startX;
		if(loc==OpmConfig.HudLocation.RIGHT) startX=screenWidth-EDGE_PADDING-effW+OpmConfig.EFFECTS_HUD_X_OFFSET.get();
		else startX=EDGE_PADDING+OpmConfig.EFFECTS_HUD_X_OFFSET.get();
		startX=Math.clamp(startX, EDGE_PADDING, screenWidth-effW-EDGE_PADDING);
		startY=Math.clamp(startY, EDGE_PADDING, screenHeight-effWidgetH-EDGE_PADDING);
		int maxY=screenHeight-EDGE_PADDING;
		int availableHeight=maxY-startY;
		int maxVisible=availableHeight/effWidgetH;
		if(maxVisible<=0) return;
		List<MobEffectInstance> effectList=new ArrayList<>(effects);
		int hiddenCount=Math.max(0,effectList.size()-maxVisible);
		int actualVisible=hiddenCount>0?maxVisible-1:maxVisible;
		actualVisible=Math.min(actualVisible,effectList.size());
		MobEffectTextureManager textureManager=mc.getMobEffectTextures();
		int currentY=startY;
		var pose=graphics.pose();
		pose.pushPose();
		if(scale!=1) pose.scale((float)scale,(float)scale,1F);
		for(int i=0;i<actualVisible;i++){
			int rx=startX;
			int ry=currentY;
			if(scale!=1.0){
				rx=(int)(rx/scale);
				ry=(int)(ry/scale);
			}
			renderEffectWidget(graphics,mc,textureManager,effectList.get(i),rx,ry,onRight);
			currentY+=effWidgetH;
		}
		if(hiddenCount>0){
			boolean hasNegative=false;
			for(int i=actualVisible;i<effectList.size();i++){
				if(effectList.get(i).getEffect().value().getCategory()
						==MobEffectCategory.HARMFUL){
					hasNegative=true;
					break;
				}
			}
			int rx=startX;
			int ry=currentY;
			if(scale!=1){
				rx=(int)(rx/scale);
				ry=(int)(ry/scale);
			}
			renderPlusWidget(graphics,mc,"+"+hiddenCount,rx,ry,hasNegative);
		}
		pose.popPose();
	}
	private void renderEffectWidget(GuiGraphics graphics,Minecraft mc,MobEffectTextureManager textureManager,MobEffectInstance instance,int x,int y,boolean onRight){
		Holder<MobEffect> effectHolder=instance.getEffect();
		boolean isHarmful=effectHolder.value().getCategory()==MobEffectCategory.HARMFUL;
		int bgColor=isHarmful?BG_NEGATIVE:BG_NEUTRAL;
		int W=WIDGET_WIDTH+8;
		int accentColor=isHarmful?0xFFCC2222:0xFF2255CC;
		int textColor=isHarmful?0xFFFF8888:0xFF88AAFF;
		int ICON_OFFSET=1;
		int iconX=onRight?x+2+ICON_OFFSET:x+W-ICON_SIZE-2-ICON_OFFSET;
		//Pozadí
		graphics.fill(x,y,x+W,y+ICON_SIZE,bgColor);
		//Pruh
		if(onRight) graphics.fill(x,y,x+2,y+ICON_SIZE,accentColor);
		else graphics.fill(x+W-2,y,x+W,y+ICON_SIZE,accentColor);
		//Ikona
		TextureAtlasSprite sprite=textureManager.get(effectHolder);
		graphics.blit(iconX,y,0,ICON_SIZE,ICON_SIZE,sprite);
		int amplifier=instance.getAmplifier()+1;
		int textX=onRight?x+W-mc.font.width(formatDuration(instance.getDuration()))-3:x+3;
		//Amplifier
		if(amplifier>1){
			String ampText=String.valueOf(amplifier);
			int ampX=onRight?x+W-mc.font.width(ampText)-3:x+3;
			graphics.drawString(mc.font,ampText,ampX,y+1,textColor,false);
		}
		//Timer
		String durationText=formatDuration(instance.getDuration());
		graphics.drawString(mc.font,durationText,textX,y+ICON_SIZE-8,textColor,false);
	}
	private void renderPlusWidget(GuiGraphics graphics,Minecraft mc,String text,int x,int y,boolean negative){
		int halfHeight=ICON_SIZE/2;
		int bgColor=negative?BG_NEGATIVE:BG_NEUTRAL;
		int W=WIDGET_WIDTH+8;
		graphics.fill(x,y,x+W,y+halfHeight,bgColor);
		int textX=x+W/2-mc.font.width(text)/2;
		int textY=y+halfHeight/2-4;
		graphics.drawString(mc.font,text,textX,textY,0xFFFFFF,false);
	}
	private String formatDuration(int ticks){
		if(ticks<=0||ticks==Integer.MAX_VALUE) return "∞";
		int seconds=ticks/20;
		if(seconds<60) return seconds+"s";
		int minutes=seconds/60;
		if(minutes<60) return minutes+"m";
		int hours=minutes/60;
		if(hours<24) return hours+"h";
		return (hours/24)+"d";
	}
}