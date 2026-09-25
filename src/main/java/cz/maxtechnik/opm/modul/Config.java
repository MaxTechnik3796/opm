package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.util.OpmButton;
import cz.maxtechnik.opm.util.OpmColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
public class Config extends Screen{
	Sidelist.ScoreboardBounds sidelistBounds;
	OpmButton sidelistButton;
	Button focusedButton=null;
	boolean focusMode=false;
	MousePos mousePos=new MousePos(0,0);
	MousePos mouseOffset=new MousePos(0,0);
	private record MousePos(int x,int y){
	}
	public Config(){
		super(Component.translatable("screen.opm.config"));
	}
	@Override
	public void init(){
		super.init();
		if(focusMode){
			OpmModConfig.SCOREBOARD_X.set(mousePos.x()-mouseOffset.x());
			OpmModConfig.SCOREBOARD_Y.set(mousePos.y()-mouseOffset.y());
			OpmModConfig.SPEC.save();
		}
		sidelistBounds=Sidelist.getDummyBounds(font,width,height);
		sidelistButton=new OpmButton(font,Component.empty(),sidelistBounds.x(),sidelistBounds.y(),sidelistBounds.width(),sidelistBounds.height(),button->setFocusedButton(button,new MousePos(mousePos.x()-button.getX(),mousePos.y()-button.getY())));
		sidelistButton.setBackGroundColors(OpmColors.TRANSPARENT,OpmColors.BLUE,OpmColors.TRANSPARENT_WHITE);
		addRenderableWidget(sidelistButton);
	}
	@Override
	public void tick(){
		if(focusMode) init();
		super.tick();
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	@Override
	public void render(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		renderSidelist(gui);
		sidelistButton.render(gui,mouseX,mouseY,partialTicks);
		mousePos=new MousePos(mouseX,mouseY);
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseDragged(double mouseX,double mouseY,int button,double dx,double dy){
		return super.mouseDragged(mouseX,mouseY,button,dx,dy);
	}
	@Override
	public boolean mouseReleased(double mouseX,double mouseY,int button){
		return super.mouseReleased(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
		return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
	}
	@Override
	public boolean keyPressed(int key,int scan,int mods){
		if(key==GLFW.GLFW_KEY_ESCAPE||key==OpmModKeys.CONFIG.getKey().getValue()){
			onClose();
			return true;
		}
		switch(key){
			case GLFW.GLFW_KEY_UP -> OpmModConfig.SCOREBOARD_Y.set(OpmModConfig.SCOREBOARD_Y.get()-1);
			case GLFW.GLFW_KEY_DOWN -> OpmModConfig.SCOREBOARD_Y.set(OpmModConfig.SCOREBOARD_Y.get()+1);
			case GLFW.GLFW_KEY_LEFT -> OpmModConfig.SCOREBOARD_X.set(OpmModConfig.SCOREBOARD_X.get()-1);
			case GLFW.GLFW_KEY_RIGHT -> OpmModConfig.SCOREBOARD_X.set(OpmModConfig.SCOREBOARD_X.get()+1);
			case GLFW.GLFW_KEY_KP_7 -> OpmModConfig.SCOREBOARD_ANCHOR_X.set(AnchorX.LEFT);
			case GLFW.GLFW_KEY_KP_8 -> OpmModConfig.SCOREBOARD_ANCHOR_X.set(AnchorX.MIDDLE);
			case GLFW.GLFW_KEY_KP_9 -> OpmModConfig.SCOREBOARD_ANCHOR_X.set(AnchorX.RIGHT);
			case GLFW.GLFW_KEY_KP_4 -> OpmModConfig.SCOREBOARD_ANCHOR_Y.set(AnchorY.TOP);
			case GLFW.GLFW_KEY_KP_1 -> OpmModConfig.SCOREBOARD_ANCHOR_Y.set(AnchorY.MIDDLE);
			case GLFW.GLFW_KEY_KP_0 -> OpmModConfig.SCOREBOARD_ANCHOR_Y.set(AnchorY.BOTTOM);
			case GLFW.GLFW_KEY_SPACE -> OpmModConfig.SPEC.save();
			default -> {
			}
		}
		init();
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		super.onClose();
	}
	private void setFocusedButton(Button button,MousePos offset){
		if(focusMode) focusedButton=null;
		else{
			mouseOffset=offset;
			focusedButton=button;
		}
		focusMode=!focusMode;
	}
	private void renderSidelist(GuiGraphics gui){
		sidelistBounds=Sidelist.getDummyBounds(font,width,height);
		int anchorX=1;
		int anchorY=0;
		switch(OpmModConfig.SCOREBOARD_ANCHOR_X.get()){
			case RIGHT->anchorX=width-font.width("⚓");
			case MIDDLE->anchorX=width/2-font.width("⚓")/2;
			default -> {}
		}
		switch(OpmModConfig.SCOREBOARD_ANCHOR_Y.get()){
			case BOTTOM->anchorY=height-9;
			case MIDDLE->anchorY=height/2-4;
			default -> {}
		}
		gui.drawString(font,"⚓",anchorX+2,anchorY+2,OpmColors.BLUE);
		gui.renderOutline(anchorX,anchorY,font.width("⚓")+4,12,OpmColors.BLUE);
	}
}
