package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.config.Anchor;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.util.OpmColors;
import cz.maxtechnik.opm.util.OpmUtil;
import cz.maxtechnik.opm.util.widget.OpmExampleWidget;
import cz.maxtechnik.opm.util.widget.OpmMovableWidget;
import cz.maxtechnik.opm.util.widget.OpmSidelistWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
public class Config extends Screen{
	private final List<OpmMovableWidget> widgets=new ArrayList<>();
	private OpmMovableWidget draggedWidget=null;
	private int grabOffsetX=0;
	private int grabOffsetY=0;

	public Config(){
		super(Component.translatable("screen.opm.config"));
	}

	@Override
	public void init(){
		super.init();
		widgets.clear();
		widgets.add(new OpmSidelistWidget());
		widgets.add(new OpmExampleWidget());
		// Další widgety přidej sem
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTick){
	}

	@Override
	public void render(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		if(draggedWidget!=null){
			renderSectorGrid(gui);
			renderAnchor(gui,draggedWidget.getAnchorX(),draggedWidget.getAnchorY());
		}
		for(OpmMovableWidget widget: widgets){
			OpmMovableWidget.WidgetBounds b=widget.getBounds(font,width,height);
			gui.renderOutline(b.x(),b.y(),b.width(),b.height(),OpmColors.BLUE);
			boolean highlight=widget==draggedWidget||(draggedWidget==null&&b.contains(mouseX,mouseY));
			if(highlight&&b.width()>2&&b.height()>2){
				OpmUtil.drawBoxWithSize(gui,b.x()+1,b.y()+1,b.width()-2,b.height()-2,OpmColors.TRANSPARENT_WHITE);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(button==0){
			for(int i=widgets.size()-1;i>=0;i--){
				OpmMovableWidget widget=widgets.get(i);
				OpmMovableWidget.WidgetBounds b=widget.getBounds(font,width,height);
				if(b.contains(mouseX,mouseY)){
					draggedWidget=widget;
					grabOffsetX=(int)mouseX-b.x();
					grabOffsetY=(int)mouseY-b.y();
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX,mouseY,button);
	}

	@Override
	public boolean mouseDragged(double mouseX,double mouseY,int button,double dx,double dy){
		if(button==0&&draggedWidget!=null){
			OpmMovableWidget.WidgetBounds b=draggedWidget.getBounds(font,width,height);
			int boxX=Math.clamp((int)mouseX-grabOffsetX,0,Math.max(0,width-b.width()));
			int boxY=Math.clamp((int)mouseY-grabOffsetY,0,Math.max(0,height-b.height()));
			Anchor.X ax=Anchor.snapX(boxX+b.width()/2,width);
			Anchor.Y ay=Anchor.snapY(boxY+b.height()/2,height);
			draggedWidget.setAnchorX(ax);
			draggedWidget.setAnchorY(ay);
			draggedWidget.setX(Anchor.offsetX(ax,boxX,b.width(),width));
			draggedWidget.setY(Anchor.offsetY(ay,boxY,b.height(),height));
			return true;
		}
		return super.mouseDragged(mouseX,mouseY,button,dx,dy);
	}

	@Override
	public boolean mouseReleased(double mouseX,double mouseY,int button){
		if(button==0&&draggedWidget!=null){
			draggedWidget.save();
			draggedWidget=null;
			return true;
		}
		return super.mouseReleased(mouseX,mouseY,button);
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
		if(draggedWidget!=null){
			draggedWidget.save();
			draggedWidget=null;
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

	private void renderAnchor(GuiGraphics gui,Anchor.X ax,Anchor.Y ay){
		int iconW=font.width("⚓")+4;
		int iconH=12;
		int anchorX=switch(ax){
			case LEFT -> 0;
			case MIDDLE -> width/2-iconW/2;
			case RIGHT -> width-iconW;
		};
		int anchorY=switch(ay){
			case TOP -> 0;
			case MIDDLE -> height/2-iconH/2;
			case BOTTOM -> height-iconH;
		};
		gui.drawString(font,"⚓",anchorX+2,anchorY+2,OpmColors.BLUE);
		gui.renderOutline(anchorX,anchorY,iconW,iconH,OpmColors.BLUE);
	}
}
