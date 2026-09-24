package cz.maxtechnik.opm.handler;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
public class Inspector extends Screen{
	private final Screen parent;
	private final ItemStack itemStack;
	private int[] copyFeedbackPos={0,0,0};
	String item,mod;
	ResourceLocation regName;
	OpmEditBox searchBox;
	boolean copyMode=false;
	OpmButton itemButton,modButton,regNameButton,copyButton,copyGiveButton,copyModeButton;
	public Inspector(ItemStack itemStack,Screen parent){
		super(Component.translatable("screen.opm.inspector"));
		this.parent=parent;
		this.itemStack=itemStack;
		item=itemStack.getHoverName().getString();
		regName=BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		mod=regName.getNamespace();
	}
	@Override
	public void init(){
		super.init();
		List<Integer> offsetY=new ArrayList<>();
		int offsetX=width/2-94;
		for(int i=0;i<=2;i++) offsetY.add(27+i*12);
		itemButton=new OpmButton(font,Component.literal(item),offsetX,offsetY.getFirst(),font.width(item),9,button->copyFeedback(button.getX(),button.getY(),item));
		itemButton.setBackGroud(false);
		itemButton.setUnderline(true);
		itemButton.setTextColors(OpmColors.WHITE2);
		addRenderableWidget(itemButton);
		modButton=new OpmButton(font,Component.literal(mod),offsetX,offsetY.get(1),font.width(mod),9,button->copyFeedback(button.getX(),button.getY(),mod));
		modButton.setBackGroud(false);
		modButton.setUnderline(true);
		modButton.setTextColors(OpmColors.BLUE);
		addRenderableWidget(modButton);
		regNameButton=new OpmButton(font,Component.literal(regName.toString()),offsetX,offsetY.getLast(),font.width(regName.toString()),9,button->{copyFeedback(button.getX(),button.getY(),regName.toString());});
		regNameButton.setBackGroud(false);
		regNameButton.setUnderline(true);
		regNameButton.setTextColors(OpmColors.GREEN);
		addRenderableWidget(regNameButton);
		copyButton=new OpmButton(font,Component.translatable("info.opm.copy"),width/2-135,67,40,16,button->{copyFeedback(button.getX(),button.getY(),"copy");});
		addRenderableWidget(copyButton);
		copyGiveButton=new OpmButton(font,Component.translatable("info.opm.copy_give"),width/2-92,67,54,16,button->{copyFeedback(button.getX(),button.getY(),"copy_give");});
		addRenderableWidget(copyGiveButton);
		copyModeButton=new OpmButton(font,copyMode?Component.translatable("info.opm.copy_mode1"):Component.translatable("info.opm.copy_mode0"),width/2-35,67,40,16,button->{
			copyMode=!copyMode;
			copyModeButton.setText(copyMode?Component.translatable("info.opm.copy_mode1"):Component.translatable("info.opm.copy_mode0"));
		});
		addRenderableWidget(copyModeButton);
		searchBox=new OpmEditBox(font,width/2+20,67,118,16,Component.translatable("info.opm.search"));
		searchBox.setHint(Component.translatable("info.opm.search").withStyle(Style.EMPTY.withItalic(true).withColor(OpmColors.GRAY3)));
		searchBox.setMaxLength(512);
		searchBox.setCanLoseFocus(true);
		addRenderableWidget(searchBox);
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
		OpmModUtil.drawWindowWithSize(gui,width/2-140,20,280,height-40,1,OpmColors.GRAY);
		OpmModUtil.drawWindowWithSize(gui,width/2-140,20,280,46,1,OpmColors.GRAY2);
		OpmModUtil.drawWindowWithSize(gui,width/2-140,65,280,20,1,OpmColors.GRAY2);
		gui.pose().pushPose();
		gui.pose().translate((float)width/2-133,27,0);
		gui.pose().scale(2F,2F,1F);
		gui.renderItem(itemStack,0,0);
		gui.pose().popPose();


		/*copyText=OpmModUtil.drawClickableText(gui,font,Component.translatable("info.opm.copy").getString(),width/2-130,71,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK);
		copyGiveText=OpmModUtil.drawClickableText(gui,font,Component.translatable("info.opm.copy_give").getString(),width/2-92,71,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK);
		copyModeText=OpmModUtil.drawClickableText(gui,font,copyMode?Component.translatable("info.opm.copy_mode1").getString():Component.translatable("info.opm.copy_mode0").getString(),width/2-30,71,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK,true,25);*/
		//OpmModUtil.drawBoxWithSize(gui,width/2+20,71,118,18,OpmModUtil.Color.BLACK);
		itemButton.render(gui,mouseX,mouseY,partialTicks);
		modButton.render(gui,mouseX,mouseY,partialTicks);
		regNameButton.render(gui,mouseX,mouseY,partialTicks);
		copyButton.render(gui,mouseX,mouseY,partialTicks);
		copyGiveButton.render(gui,mouseX,mouseY,partialTicks);
		copyModeButton.render(gui,mouseX,mouseY,partialTicks);

		searchBox.render(gui,mouseX,mouseY,partialTicks);
		if(copyFeedbackPos[2]>0){
			gui.drawString(font,Component.translatable("info.opm.copy_feedback"),copyFeedbackPos[0],copyFeedbackPos[1],OpmColors.GREEN);
			copyFeedbackPos[2]-=1;
		}
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(searchBox.isFocused()&&!searchBox.isMouseOver(mouseX,mouseY)) searchBox.setFocused(false);
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
		if(searchBox.isFocused()){
			if(key==GLFW.GLFW_KEY_ESCAPE){
				searchBox.setFocused(false);
				return true;
			}
			return searchBox.keyPressed(key,scan,mods);
		}
		if(key==GLFW.GLFW_KEY_ESCAPE||key==OpmModKeys.INSPECTOR.getKey().getValue()){
			onClose();
			return true;
		}
		OpmMover.update(key);
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		if(minecraft!=null) minecraft.setScreen(parent);
	}
	private void copyFeedback(double x,double y,String text){
		copyFeedbackPos=new int[]{(int)x+5,(int)y-12,100};
		OpmModUtil.copyToClipboard(text);
	}
}
