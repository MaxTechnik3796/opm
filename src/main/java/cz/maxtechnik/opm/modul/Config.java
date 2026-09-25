package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.util.MovableWidget;
import cz.maxtechnik.opm.util.OpmButton;
import cz.maxtechnik.opm.util.OpmColors;
import cz.maxtechnik.opm.util.SidelistWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
public class Config extends Screen{
	public static class ConfigWidgetHolder{
		public final MovableWidget widget;
		public MovableWidget.WidgetBounds bounds;
		public OpmButton button;
		public ConfigWidgetHolder(MovableWidget widget){
			this.widget=widget;
		}
		public void update(Font font,int screenWidth,int screenHeight){
			this.bounds=widget.getBounds(font,screenWidth,screenHeight);
			if(button==null){
				button=new OpmButton(font,Component.empty(),bounds.x(),bounds.y(),bounds.width(),bounds.height(),b->{
				});
				button.setBackGroundColors(OpmColors.TRANSPARENT,OpmColors.BLUE,OpmColors.TRANSPARENT_WHITE);
			}else{
				button.setX(bounds.x());
				button.setY(bounds.y());
				button.setWidth(bounds.width());
				button.setHeight(bounds.height());
			}
		}
	}
	private final List<ConfigWidgetHolder> widgets=new ArrayList<>();
	private ConfigWidgetHolder focusedWidget=null;
	private int grabOffsetX=0;
	private int grabOffsetY=0;
	public Config(){
		super(Component.translatable("screen.opm.config"));
	}
	@Override
	public void init(){
		super.init();
		widgets.clear();
		registerWidget(new SidelistWidget());
		//add here
		for(ConfigWidgetHolder holder: widgets){
			holder.update(font,width,height);
			addRenderableWidget(holder.button);
		}
	}
	private void registerWidget(MovableWidget widget){
		widgets.add(new ConfigWidgetHolder(widget));
	}
	@Override
	public void renderBackground(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTick){
	}
	@Override
	public void render(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		if(focusedWidget!=null){
			renderSectorGrid(gui);
			int boxX=mouseX-grabOffsetX;
			int boxY=mouseY-grabOffsetY;
			int centerX=boxX+focusedWidget.bounds.width()/2;
			int centerY=boxY+focusedWidget.bounds.height()/2;
			AnchorX newAnchorX;
			if(centerX<width/3) newAnchorX=AnchorX.LEFT;
			else if(centerX>(width*2)/3) newAnchorX=AnchorX.RIGHT;
			else newAnchorX=AnchorX.MIDDLE;
			AnchorY newAnchorY;
			if(centerY<height/3) newAnchorY=AnchorY.TOP;
			else if(centerY>(height*2)/3) newAnchorY=AnchorY.BOTTOM;
			else newAnchorY=AnchorY.MIDDLE;

			focusedWidget.widget.setAnchorX(newAnchorX);
			focusedWidget.widget.setAnchorY(newAnchorY);
			int anchorOffsetX=switch(newAnchorX){
				case RIGHT -> focusedWidget.bounds.width();
				case MIDDLE -> focusedWidget.bounds.width()/2;
				default -> 0;
			};
			int anchorOffsetY=switch(newAnchorY){
				case BOTTOM -> focusedWidget.bounds.height();
				case MIDDLE -> focusedWidget.bounds.height()/2;
				default -> 0;
			};
			focusedWidget.widget.setX(boxX+anchorOffsetX);
			focusedWidget.widget.setY(boxY+anchorOffsetY);
			focusedWidget.update(font,width,height);
		}
		if(focusedWidget!=null) renderAnchor(gui,focusedWidget.widget.getAnchorX(),focusedWidget.widget.getAnchorY());
		for(ConfigWidgetHolder holder: widgets) holder.button.render(gui,mouseX,mouseY,partialTicks);
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(button==0){
			if(focusedWidget!=null){
				focusedWidget.widget.save();
				focusedWidget=null;
				return true;
			}
			for(ConfigWidgetHolder holder: widgets){
				if(holder.button.isMouseOver(mouseX,mouseY)){
					focusedWidget=holder;
					grabOffsetX=(int)mouseX-holder.bounds.x();
					grabOffsetY=(int)mouseY-holder.bounds.y();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean keyPressed(int key,int scan,int mods){
		if(key==GLFW.GLFW_KEY_ESCAPE||key==OpmModKeys.CONFIG.getKey().getValue()){
			OpmModConfig.SPEC.save();
			onClose();
			return true;
		}
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		if(focusedWidget!=null){
			focusedWidget.widget.save();
			focusedWidget=null;
		}
		super.onClose();
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	private void renderSectorGrid(GuiGraphics gui){
		int gridColor=0x25FFFFFF;
		gui.fill(width/3,0,width/3+1,height,gridColor);
		gui.fill((width*2)/3,0,(width*2)/3+1,height,gridColor);
		gui.fill(0,height/3,width,height/3+1,gridColor);
		gui.fill(0,(height*2)/3,width,(height*2)/3+1,gridColor);
	}
	private void renderAnchor(GuiGraphics gui,AnchorX ax,AnchorY ay){
		int anchorX=1;
		int anchorY=0;
		switch(ax){
			case RIGHT -> anchorX=width-font.width("⚓")-4;
			case MIDDLE -> anchorX=width/2-font.width("⚓")/2-2;
			default -> {
			}
		}
		switch(ay){
			case BOTTOM -> anchorY=height-12;
			case MIDDLE -> anchorY=height/2-5;
			default -> {
			}
		}
		gui.drawString(font,"⚓",anchorX+2,anchorY+2,OpmColors.BLUE);
		gui.renderOutline(anchorX,anchorY,font.width("⚓")+4,12,OpmColors.BLUE);
	}
}