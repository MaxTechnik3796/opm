package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import cz.maxtechnik.opm.config.OpmModConfig;
import cz.maxtechnik.opm.util.OpmButton;
import cz.maxtechnik.opm.util.OpmColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
public class Config extends Screen{
	private Sidelist.ScoreboardBounds sidelistBounds;
	private OpmButton sidelistButton;
	private boolean focusMode=false;
	private int grabOffsetX=0;
	private int grabOffsetY=0;
	public Config(){
		super(Component.translatable("screen.opm.config"));
	}
	@Override
	public void init(){
		super.init();
		updateBoundsAndButton();
	}
	private void updateBoundsAndButton(){
		sidelistBounds=Sidelist.getDummyBounds(font,width,height);
		if(sidelistButton==null){
			sidelistButton=new OpmButton(font,Component.empty(),sidelistBounds.x(),sidelistBounds.y(),sidelistBounds.width(),sidelistBounds.height(),button->{});
			sidelistButton.setBackGroundColors(OpmColors.TRANSPARENT,OpmColors.BLUE,OpmColors.TRANSPARENT_WHITE);
			addRenderableWidget(sidelistButton);
		}else{
			sidelistButton.setX(sidelistBounds.x());
			sidelistButton.setY(sidelistBounds.y());
			sidelistButton.setWidth(sidelistBounds.width());
			sidelistButton.setHeight(sidelistBounds.height());
		}
	}
	@Override
	public void render(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		if(focusMode){
			// 1. Požadovaný levý horní roh rámečku na obrazovce
			int boxX=mouseX-grabOffsetX;
			int boxY=mouseY-grabOffsetY;
			// 2. Přepočet zpět na SCOREBOARD_X/Y s respektováním nastaveného Anchoru
			int anchorOffsetX=switch(OpmModConfig.SCOREBOARD_ANCHOR_X.get()){
				case RIGHT -> sidelistBounds.width();
				case MIDDLE -> sidelistBounds.width()/2;
				default -> 0;
			};
			int anchorOffsetY=switch(OpmModConfig.SCOREBOARD_ANCHOR_Y.get()){
				case BOTTOM -> sidelistBounds.height();
				case MIDDLE -> sidelistBounds.height()/2;
				default -> 0;
			};
			OpmModConfig.SCOREBOARD_X.set(boxX+anchorOffsetX);
			OpmModConfig.SCOREBOARD_Y.set(boxY+anchorOffsetY);
			// Aktualizujeme pozici tlačítka bez volání init()
			updateBoundsAndButton();
		}
		renderSidelist(gui);
		sidelistButton.render(gui,mouseX,mouseY,partialTicks);
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(button==0){
			// Kliknuto do plochy sidebaru
			if(sidelistButton.isMouseOver(mouseX,mouseY)){
				focusMode=!focusMode;
				if(focusMode){
					// Chytnutí: přesně spočítáme offset od levého horního rohu rámečku
					grabOffsetX=(int)mouseX-sidelistBounds.x();
					grabOffsetY=(int)mouseY-sidelistBounds.y();
				}else{
					// Puštění: teprve teď uložíme změny na disk
					OpmModConfig.SPEC.save();
				}
				return true;
			}else if(focusMode){
				focusMode=false;
				OpmModConfig.SPEC.save();
				return true;
			}
		}
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean keyPressed(int key,int scan,int mods){
		if(key==GLFW.GLFW_KEY_ESCAPE||key==OpmModKeys.CONFIG.getKey().getValue()){
			onClose();
			return true;
		}
		boolean changed=switch(key){
			case GLFW.GLFW_KEY_UP -> {
				OpmModConfig.SCOREBOARD_Y.set(OpmModConfig.SCOREBOARD_Y.get()-1);
				yield true;
			}
			case GLFW.GLFW_KEY_DOWN -> {
				OpmModConfig.SCOREBOARD_Y.set(OpmModConfig.SCOREBOARD_Y.get()+1);
				yield true;
			}
			case GLFW.GLFW_KEY_LEFT -> {
				OpmModConfig.SCOREBOARD_X.set(OpmModConfig.SCOREBOARD_X.get()-1);
				yield true;
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				OpmModConfig.SCOREBOARD_X.set(OpmModConfig.SCOREBOARD_X.get()+1);
				yield true;
			}
			case GLFW.GLFW_KEY_KP_7 -> {
				OpmModConfig.SCOREBOARD_ANCHOR_X.set(AnchorX.LEFT);
				yield true;
			}
			case GLFW.GLFW_KEY_KP_8 -> {
				OpmModConfig.SCOREBOARD_ANCHOR_X.set(AnchorX.MIDDLE);
				yield true;
			}
			case GLFW.GLFW_KEY_KP_9 -> {
				OpmModConfig.SCOREBOARD_ANCHOR_X.set(AnchorX.RIGHT);
				yield true;
			}
			case GLFW.GLFW_KEY_KP_4 -> {
				OpmModConfig.SCOREBOARD_ANCHOR_Y.set(AnchorY.TOP);
				yield true;
			}
			case GLFW.GLFW_KEY_KP_1 -> {
				OpmModConfig.SCOREBOARD_ANCHOR_Y.set(AnchorY.MIDDLE);
				yield true;
			}
			case GLFW.GLFW_KEY_KP_0 -> {
				OpmModConfig.SCOREBOARD_ANCHOR_Y.set(AnchorY.BOTTOM);
				yield true;
			}
			case GLFW.GLFW_KEY_SPACE -> {
				OpmModConfig.SPEC.save();
				yield false;
			}
			default -> false;
		};
		if(changed){
			updateBoundsAndButton();
		}
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		if(focusMode){
			focusMode=false;
			OpmModConfig.SPEC.save();
		}
		super.onClose();
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	private void renderSidelist(GuiGraphics gui){
		int anchorX=1;
		int anchorY=0;
		switch(OpmModConfig.SCOREBOARD_ANCHOR_X.get()){
			case RIGHT -> anchorX=width-font.width("⚓");
			case MIDDLE -> anchorX=width/2-font.width("⚓")/2;
			default -> {
			}
		}
		switch(OpmModConfig.SCOREBOARD_ANCHOR_Y.get()){
			case BOTTOM -> anchorY=height-9;
			case MIDDLE -> anchorY=height/2-4;
			default -> {
			}
		}
		gui.drawString(font,"⚓",anchorX+2,anchorY+2,OpmColors.BLUE);
		gui.renderOutline(anchorX,anchorY,font.width("⚓")+4,12,OpmColors.BLUE);
	}
}