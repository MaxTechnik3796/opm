package cz.maxtechnik.opm.client.overlay;

import cz.maxtechnik.opm.client.handler.HudTransformUtils;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
public class ItemDurabilityHudOverlay implements LayeredDraw.Layer{
	@Override
	public void render(@NotNull GuiGraphics graphics,@NotNull DeltaTracker deltaTracker){
		if(!HudTransformUtils.shouldRender()) return;
		if(!OpmConfig.ITEM_DURABILITY_IN_NAME.get()) return;
		Minecraft mc=Minecraft.getInstance();
		Player player=mc.player;
		assert player!=null;
		ItemStack stack=player.getMainHandItem();
		//Zobraz pouze pro damageable itemy které jsou poškozené
		if(stack.isEmpty()||!stack.isDamageableItem()) return;
		int current=stack.getMaxDamage()-stack.getDamageValue();
		int max=stack.getMaxDamage();
		//Formát: [current/max]
		String durText="["+current+"/"+max+"]";
		//Barva podle procenta durability
		float fraction=(float)current/max;
		int color;
		if(fraction>0.6F) color=0xFFAAFFAA; //zelená
		else if(fraction>0.3f) color=0xFFFFFF55; //žlutá
		else color=0xFFFF5555; // červená
		//Pozice — pod názvem itemu v ruce
		int screenW=graphics.guiWidth();
		int screenH=graphics.guiHeight();
		double scale=OpmConfig.ITEM_DURABILITY_SCALE.get();
		int textW=mc.font.width(durText);
		int scaledW=(int)(textW*scale);
		int x=(screenW-scaledW)/2+OpmConfig.ITEM_DURABILITY_X_OFFSET.get();
		int y=screenH-72+OpmConfig.ITEM_DURABILITY_Y_OFFSET.get();
		x=Math.clamp(x,2,screenW-scaledW-2);
		y=Math.clamp(y,2,screenH-(int)(9*scale)-2);
		HudTransformUtils.pushTransform(graphics.pose(),0,0,scale,x,y);
		graphics.fill(-2,-1,textW+2,9,0x55000000);
		graphics.drawString(mc.font,durText,0,0,color,true);
		HudTransformUtils.popTransform(graphics.pose(),scale,x,y);
	}
}