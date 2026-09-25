package cz.maxtechnik.opm.util.widget;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.config.Anchor;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.util.OpmColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class OpmExampleWidget implements OpmMovableWidget{
	// Pevné rozměry obdélníku
	public static final int WIDTH=60;
	public static final int HEIGHT=30;

	public OpmExampleWidget(){
	}

	public static WidgetBounds getBounds(int screenWidth,int screenHeight){
		// Procentuální přepočet pozice (0-10000) podle aktuální velikosti obrazovky
		int availableWidth=Math.max(1,screenWidth-WIDTH);
		int availableHeight=Math.max(1,screenHeight-HEIGHT);
		int relX=Math.clamp(OpmModConfig.EXAMPLE_X.get(),0,10000);
		int relY=Math.clamp(OpmModConfig.EXAMPLE_Y.get(),0,10000);
		int targetX=(int)Math.round(((double)relX/10000.0)*availableWidth);
		int targetY=(int)Math.round(((double)relY/10000.0)*availableHeight);
		return new WidgetBounds(targetX,targetY,WIDTH,HEIGHT,0,0);
	}

	@Override
	public WidgetBounds getBounds(Font font,int screenWidth,int screenHeight){
		return getBounds(screenWidth,screenHeight);
	}

	@Override
	public int getX(){
		return OpmModConfig.EXAMPLE_X.get();
	}

	@Override
	public void setX(int x){
		OpmModConfig.EXAMPLE_X.set(x);
	}

	@Override
	public int getY(){
		return OpmModConfig.EXAMPLE_Y.get();
	}

	@Override
	public void setY(int y){
		OpmModConfig.EXAMPLE_Y.set(y);
	}

	@Override
	public Anchor.X getAnchorX(){
		return OpmModConfig.EXAMPLE_ANCHOR_X.get();
	}

	@Override
	public void setAnchorX(Anchor.X anchor){
		OpmModConfig.EXAMPLE_ANCHOR_X.set(anchor);
	}

	@Override
	public Anchor.Y getAnchorY(){
		return OpmModConfig.EXAMPLE_ANCHOR_Y.get();
	}

	@Override
	public void setAnchorY(Anchor.Y anchor){
		OpmModConfig.EXAMPLE_ANCHOR_Y.set(anchor);
	}

	@Override
	public void save(){
		OpmModConfig.SPEC.save();
	}

	// Vykreslení šedého obdélníku ve hře (zobrazuje se i po zavření configu)
	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event){
		Minecraft mc=Minecraft.getInstance();
		if(mc.level==null) return;
		GuiGraphics gui=event.getGuiGraphics();
		WidgetBounds bounds=getBounds(gui.guiWidth(),gui.guiHeight());
		gui.fill(bounds.x(),bounds.y(),bounds.x()+bounds.width(),bounds.y()+bounds.height(),OpmColors.GRAY);
	}
}
