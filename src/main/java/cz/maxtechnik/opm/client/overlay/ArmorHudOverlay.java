package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.LayeredDraw;
import org.jetbrains.annotations.NotNull;
public class ArmorHudOverlay implements LayeredDraw.Layer{
	private static final int SLOT_SIZE=16;
	private static final int GAP=4;
	private static final int EDGE_PAD=2;
	private static final int OFFHAND_W=29;
	private static final int DUR_BAR_H=1;
	private static final int DUR_BAR_PAD=1;
	private static final EquipmentSlot[] CANONICAL={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
	@Override
	public void render(@NotNull GuiGraphics graphics,@NotNull DeltaTracker deltaTracker){
		Minecraft mc=Minecraft.getInstance();
		if(mc.player==null||mc.options.hideGui) return;
		if(mc.screen instanceof cz.maxtechnik.opm.client.screen.OpmConfigScreen) return;
		if(!OpmConfig.ARMOR_HUD_ENABLED.get()) return;
		Player player=mc.player;
		int screenWidth=graphics.guiWidth();
		int screenHeight=graphics.guiHeight();
		int rotate=OpmConfig.ARMOR_HUD_ROTATE.get();
		boolean locked=OpmConfig.ARMOR_HUD_LOCKED.get();
		EquipmentSlot[] slots=CANONICAL.clone();
		if(rotate==2||rotate==3){
			for(int i=0, j=slots.length-1;i<j;i++,j--){
				EquipmentSlot tmp=slots[i];
				slots[i]=slots[j];
				slots[j]=tmp;
			}
		}
		int count=0;
		for(EquipmentSlot s: slots) if(!player.getItemBySlot(s).isEmpty()) count++;
		if(count==0) return;
		boolean horizontal=(rotate==0||rotate==2);
		int totalSpan=count*SLOT_SIZE+(count-1)*GAP;
		int startX, startY;
		if(locked){
			int hotbarWidth=182;
			int hotbarX=(screenWidth-hotbarWidth)/2;
			int itemY=screenHeight-22;
			boolean hasOffhand=!player.getOffhandItem().isEmpty();
			boolean offhandLeft=(player.getMainArm()==HumanoidArm.RIGHT);
			boolean offhandRight=!offhandLeft;
			if(horizontal){
				if(OpmConfig.ARMOR_HUD_LOCATION.get()==OpmConfig.HudLocation.LEFT){
					startX=hotbarX-GAP-totalSpan;
					if(hasOffhand&&offhandLeft) startX-=OFFHAND_W;
				}else{
					startX=hotbarX+hotbarWidth+GAP;
					if(hasOffhand&&offhandRight) startX+=OFFHAND_W;
				}
				startY=itemY;
			}else{
				int vertTotal=count*SLOT_SIZE+(count-1)*GAP;
				if(OpmConfig.ARMOR_HUD_LOCATION.get()==OpmConfig.HudLocation.LEFT){
					startX=hotbarX-GAP-SLOT_SIZE;
					if(hasOffhand&&offhandLeft) startX-=OFFHAND_W;
				}else{
					startX=hotbarX+hotbarWidth+GAP;
					if(hasOffhand&&offhandRight) startX+=OFFHAND_W;
				}
				if(rotate==3) startY=itemY;
				else startY=itemY-vertTotal+SLOT_SIZE;
			}
		}else{
			startX=OpmConfig.ARMOR_HUD_FREE_X.get();
			startY=OpmConfig.ARMOR_HUD_FREE_Y.get();
		}
		double scale=OpmConfig.ARMOR_HUD_SCALE.get();
		int hudW=(int)((horizontal?totalSpan:SLOT_SIZE)*scale);
		int hudH=(int)((horizontal?(SLOT_SIZE+DUR_BAR_PAD+DUR_BAR_H+2):totalSpan)*scale);
		startX=Math.clamp(startX,EDGE_PAD,screenWidth-hudW-EDGE_PAD);
		startY=Math.clamp(startY,EDGE_PAD,screenHeight-hudH-EDGE_PAD);
		
		var pose=graphics.pose();
		pose.pushPose();
		pose.translate(startX,startY,0);
		if(scale!=1.0) pose.scale((float)scale,(float)scale,1F);
		int curX=0, curY=0;
		for(EquipmentSlot slot: slots){
			ItemStack stack=player.getItemBySlot(slot);
			if(stack.isEmpty()) continue;
			graphics.renderItem(stack,curX,curY);
			if(stack.isDamageableItem()&&stack.isDamaged()){
				float fraction=1F-(float)stack.getDamageValue()/stack.getMaxDamage();
				int barWidth=Math.round(fraction*13);
				int barX=curX+2;
				int barY=curY+SLOT_SIZE+DUR_BAR_PAD;
				graphics.fill(barX-1,barY-1,barX+14,barY+DUR_BAR_H+1,0xFF000000);
				graphics.fill(barX,barY,barX+barWidth,barY+DUR_BAR_H,getDurabilityColor(fraction));
			}
			if(horizontal) curX+=SLOT_SIZE+GAP;
			else curY+=SLOT_SIZE+GAP;
		}
		pose.popPose();
	}
	private int getDurabilityColor(float fraction){
		int r=Math.round(255*(1F-fraction));
		int g=Math.round(255*fraction);
		return 0xFF000000|(r<<16)|(g<<8);
	}
}