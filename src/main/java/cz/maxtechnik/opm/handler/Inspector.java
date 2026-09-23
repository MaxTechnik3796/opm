package cz.maxtechnik.opm.handler;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.OpmModUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
public class Inspector extends Screen{
	private final Screen parent;
	private final ItemStack itemStack;
	private boolean copyMode=false,itemText,modText,regNameText,copyText,copyGiveText,copyModeText;
	private int[] copyFeedbackPos={0,0,0};
	String item,mod;
	ResourceLocation regName;
	EditBox searchBox;
	Button copyButton;
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
		searchBox=new EditBox(font,width/2+20,71,118,18,Component.translatable("info.opm.search"));
		searchBox.setMaxLength(512);
		searchBox.setCanLoseFocus(true);
		addRenderableWidget(searchBox);
		copyButton=new PlainTextButton(width/2-130,71,25,15,Component.translatable("info.opm.copy"),e->{
			copyFeedback(e.getX(),e.getY(),"itemText");
		},font);
		addRenderableWidget(copyButton);
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
		OpmModUtil.drawWindowWithSize(gui,width/2-140,20,280,height-40,1,OpmModUtil.Color.GRAY);
		OpmModUtil.drawWindowWithSize(gui,width/2-140,20,280,46,1,OpmModUtil.Color.GRAY2);
		OpmModUtil.drawWindowWithSize(gui,width/2-140,65,280,20,1,OpmModUtil.Color.GRAY2);
		List<Integer> offsetY=new ArrayList<>();
		int offsetX=-94;
		for(int i=0;i<=2;i++) offsetY.add(27+i*12);
		itemText=OpmModUtil.drawClickableText(gui,font,item,width/2+offsetX,offsetY.getFirst(),mouseX,mouseY,OpmModUtil.Color.WHITE2);
		modText=OpmModUtil.drawClickableText(gui,font,mod,width/2+offsetX,offsetY.get(1),mouseX,mouseY,OpmModUtil.Color.BLUE);
		regNameText=OpmModUtil.drawClickableText(gui,font,regName.toString(),width/2+offsetX,offsetY.getLast(),mouseX,mouseY,OpmModUtil.Color.GREEN);
		gui.pose().pushPose();
		gui.pose().translate((float)width/2-133,27,0);
		gui.pose().scale(2F,2F,1F);
		gui.renderItem(itemStack,0,0);
		gui.pose().popPose();


		/*copyText=OpmModUtil.drawClickableText(gui,font,Component.translatable("info.opm.copy").getString(),width/2-130,71,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK);
		copyGiveText=OpmModUtil.drawClickableText(gui,font,Component.translatable("info.opm.copy_give").getString(),width/2-92,71,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK);
		copyModeText=OpmModUtil.drawClickableText(gui,font,copyMode?Component.translatable("info.opm.copy_mode1").getString():Component.translatable("info.opm.copy_mode0").getString(),width/2-30,71,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK,true,25);*/
		//OpmModUtil.drawBoxWithSize(gui,width/2+20,71,118,18,OpmModUtil.Color.BLACK);
		copyButton.render(gui,mouseX,mouseY,partialTicks);
		searchBox.render(gui,mouseX,mouseY,partialTicks);
		if(copyFeedbackPos[2]>0){
			gui.drawString(font,Component.translatable("info.opm.copy_feedback"),copyFeedbackPos[0],copyFeedbackPos[1],OpmModUtil.Color.GREEN);
			copyFeedbackPos[2]-=1;
		}
		//System.out.println(guiScale);
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		boolean done=false;
		if(itemText){
			copyFeedback(mouseX,mouseY,item);
			done=true;
		}else if(modText){
			copyFeedback(mouseX,mouseY,mod);
			done=true;
		}else if(regNameText){
			copyFeedback(mouseX,mouseY,regName.toString());
			done=true;
		}else if(copyText){
			copyFeedback(mouseX,mouseY,"itemText");
			done=true;
		}else if(copyGiveText){
			copyFeedback(mouseX,mouseY,"modText");
			done=true;
		}else if(copyModeText){
			copyMode=!copyMode;
			done=true;
		}
		if(searchBox.isFocused()&&!searchBox.isMouseOver(mouseX,mouseY)) searchBox.setFocused(false);
		if(done) return true;
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
		OpmModUtil.Mover.update(key);
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		if(minecraft!=null) minecraft.setScreen(parent);
	}
	private void copyFeedback(double mouseX,double mouseY,String text){
		copyFeedbackPos=new int[]{(int)mouseX+5,(int)mouseY-12,100};
		OpmModUtil.copyToClipboard(text);
	}
}
