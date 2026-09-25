package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import cz.maxtechnik.opm.config.OpmModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
public class Config extends Screen{
	public Config(){
		super(Component.translatable("screen.opm.config"));
	}
	@Override
	public void init(){
		super.init();
	}
	@Override
	public void tick(){
		super.tick();
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	@Override
	public void render(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
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
			default -> {}
		}
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		super.onClose();
	}
}
