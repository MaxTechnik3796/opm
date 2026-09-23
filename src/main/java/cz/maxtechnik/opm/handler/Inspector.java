package cz.maxtechnik.opm.handler;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.OpmModUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
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
	public Inspector(ItemStack itemStack,Screen parent){
		super(Component.translatable("screen.opm.inspector"));
		this.parent=parent;
		this.itemStack=itemStack;
		item=itemStack.getHoverName().getString();
		regName=BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		mod=regName.getNamespace();
		try{
			var container=ModList.get().getModContainerById(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getNamespace());
			container.ifPresent(modContainer->mod=modContainer.getModInfo().getDisplayName());
		}catch(Exception ignored){
		}
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
		OpmModUtil.drawCentredWindow(gui,280,200,width,height,1);
		OpmModUtil.drawCentredWindow(gui,280,50,width,height-150,1,OpmModUtil.Color.GRAY2);
		OpmModUtil.drawCentredWindow(gui,280,20,width,height-82,1,OpmModUtil.Color.GRAY2);
		if(minecraft!=null)
			System.out.println(minecraft.options.guiScale().get());
		List<Integer> offsetY=new ArrayList<>();
		int offsetX=-94;
		for(int i=0;i<=2;i++) offsetY.add(-93+i*12);
		itemText=OpmModUtil.drawClickableText(gui,font,item,width/2+offsetX,height/2+offsetY.getFirst(),mouseX,mouseY,OpmModUtil.Color.WHITE2);
		modText=OpmModUtil.drawClickableText(gui,font,mod,width/2+offsetX,height/2+offsetY.get(1),mouseX,mouseY,OpmModUtil.Color.BLUE);
		regNameText=OpmModUtil.drawClickableText(gui,font,regName.toString(),width/2+offsetX,height/2+offsetY.getLast(),mouseX,mouseY,OpmModUtil.Color.GREEN);
		gui.pose().pushPose();
		gui.pose().translate((float)width/2-133,(float)height/2-93,0);
		gui.pose().scale(2F,2F,1F);
		gui.renderItem(itemStack,0,0);
		gui.pose().popPose();
		copyText=OpmModUtil.drawClickableText(gui,font,Component.translatable("info.opm.copy").getString(),width/2-130,height/2-45,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK,false);
		copyGiveText=OpmModUtil.drawClickableText(gui,font,Component.translatable("info.opm.copy_give").getString(),width/2-100,height/2-45,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK,false);
		copyModeText=OpmModUtil.drawClickableText(gui,font,copyMode?Component.translatable("info.opm.copy_mode1").getString():Component.translatable("info.opm.copy_mode0").getString(),width/2-40,height/2-45,mouseX,mouseY,OpmModUtil.Color.GRAY3,OpmModUtil.Color.WHITE2,-1,OpmModUtil.Color.GRAY,OpmModUtil.Color.BLACK,false);

		if(copyFeedbackPos[2]>0){
			gui.drawString(font,Component.translatable("info.opm.copy_feedback"),copyFeedbackPos[0],copyFeedbackPos[1],OpmModUtil.Color.GREEN);
			copyFeedbackPos[2]-=1;
		}
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		if(itemText){
			copyFeedback(mouseX,mouseY,item);
			return true;
		}
		if(modText){
			copyFeedback(mouseX,mouseY,mod);
			return true;
		}
		if(regNameText){
			copyFeedback(mouseX,mouseY,regName.toString());
			return true;
		}
		if(copyText){
			copyFeedback(mouseX,mouseY,"itemText");
			return true;
		}
		if(copyGiveText){
			copyFeedback(mouseX,mouseY,"modText");
			return true;
		}
		if(copyModeText){
			copyMode=!copyMode;
			return true;
		}
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
