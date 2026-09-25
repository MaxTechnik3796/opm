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
		// Pozice widgetu podle kotvy a pevného pixelového offsetu
		int offsetX=OpmModConfig.EXAMPLE_X.get();
		int offsetY=OpmModConfig.EXAMPLE_Y.get();
		int targetX=switch(OpmModConfig.EXAMPLE_ANCHOR_X.get()){
			case LEFT -> offsetX;
			case RIGHT -> screenWidth-WIDTH-offsetX;
			case MIDDLE -> (screenWidth/2)+offsetX-(WIDTH/2);
		};
		int targetY=switch(OpmModConfig.EXAMPLE_ANCHOR_Y.get()){
			case TOP -> offsetY;
			case BOTTOM -> screenHeight-HEIGHT-offsetY;
			case MIDDLE -> (screenHeight/2)+offsetY-(HEIGHT/2);
		};
		// Zajištění, aby byl widget vždy na obrazovce
		targetX=Math.clamp(targetX,0,Math.max(0,screenWidth-WIDTH));
		targetY=Math.clamp(targetY,0,Math.max(0,screenHeight-HEIGHT));
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
