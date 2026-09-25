package cz.maxtechnik.opm.modul;

import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
public class Inspector extends Screen{
	private final Screen parent;
	private final ItemStack itemStack;
	private feedbackData copyFeedback=new feedbackData(0,0,0);
	String item, mod;
	ResourceLocation regName;
	OpmEditBox searchBox;
	private static boolean copyMode=false;
	OpmButton itemButton, modButton, regNameButton, copyButton, copyGiveButton, copyModeButton;
	private OpmCodeViewer codeViewer;
	public static void openFromPlayerHand(Minecraft mc){
		Player player=mc.player;
		if(player==null) return;
		ItemStack target=player.getMainHandItem();
		if(target.isEmpty()) target=player.getOffhandItem();
		if(target.isEmpty()){
			for(int i=0;i<9;i++){
				ItemStack slot=player.getInventory().getItem(i);
				if(!slot.isEmpty()){
					target=slot;
					break;
				}
			}
		}
		if(!target.isEmpty()) mc.setScreen(new Inspector(target,mc.screen));
	}
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
		itemButton.setBackGround(false);
		itemButton.setUnderline(true);
		itemButton.setTextColors(OpmColors.WHITE2);
		addRenderableWidget(itemButton);
		modButton=new OpmButton(font,Component.literal(mod),offsetX,offsetY.get(1),font.width(mod),9,button->copyFeedback(button.getX(),button.getY(),mod));
		modButton.setBackGround(false);
		modButton.setUnderline(true);
		modButton.setTextColors(OpmColors.BLUE);
		addRenderableWidget(modButton);
		regNameButton=new OpmButton(font,Component.literal(regName.toString()),offsetX,offsetY.getLast(),font.width(regName.toString()),9,button->copyFeedback(button.getX(),button.getY(),regName.toString()));
		regNameButton.setBackGround(false);
		regNameButton.setUnderline(true);
		regNameButton.setTextColors(OpmColors.GREEN);
		addRenderableWidget(regNameButton);
		copyButton=new OpmButton(font,Component.translatable("info.opm.copy"),width/2-135,67,40,16,button->{
			String copy=OpmComponentHandler.extractComponentsToString(itemStack,copyMode);
			copyFeedback(button.getX(),button.getY(),copy.isEmpty()?"[]":copy);
		});
		addRenderableWidget(copyButton);
		copyGiveButton=new OpmButton(font,Component.translatable("info.opm.copy_give"),width/2-92,67,54,16,button->copyFeedback(button.getX(),button.getY(),"/give @s "+regName.toString()+OpmComponentHandler.extractComponentsToString(itemStack,copyMode)));
		addRenderableWidget(copyGiveButton);
		copyModeButton=new OpmButton(font,copyMode?Component.translatable("info.opm.copy_mode1"):Component.translatable("info.opm.copy_mode0"),width/2-35,67,40,16,button->{
			copyMode=!copyMode;
			copyModeButton.setText(copyMode?Component.translatable("info.opm.copy_mode1"):Component.translatable("info.opm.copy_mode0"));
			if(codeViewer!=null) codeViewer.loadFromItemStack(itemStack,copyMode);
		});
		addRenderableWidget(copyModeButton);
		searchBox=new OpmEditBox(font,width/2+8,67,127,16,Component.translatable("info.opm.search"));
		searchBox.setHint(Component.translatable("info.opm.search").withStyle(Style.EMPTY.withItalic(true).withColor(OpmColors.LIGHT_GRAY)));
		searchBox.setMaxLength(512);
		searchBox.setCanLoseFocus(true);
		addRenderableWidget(searchBox);
		codeViewer=new OpmCodeViewer(font,width/2-138,87,276,height-111,this::copyFeedback);
		codeViewer.loadFromItemStack(itemStack,copyMode);
		addRenderableWidget(codeViewer);
		searchBox.setResponder(text->codeViewer.setFilter(text));
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
		OpmUtil.drawWindowWithSize(gui,width/2-140,20,280,height-40,1,OpmColors.GRAY);
		OpmUtil.drawWindowWithSize(gui,width/2-140,20,280,46,1,OpmColors.DARK_GRAY);
		OpmUtil.drawWindowWithSize(gui,width/2-140,65,280,20,1,OpmColors.DARK_GRAY);
		gui.pose().pushPose();
		gui.pose().translate((float)width/2-133,27,0);
		gui.pose().scale(2F,2F,1F);
		gui.renderItem(itemStack,0,0);
		gui.pose().popPose();
		itemButton.render(gui,mouseX,mouseY,partialTicks);
		modButton.render(gui,mouseX,mouseY,partialTicks);
		regNameButton.render(gui,mouseX,mouseY,partialTicks);
		copyButton.render(gui,mouseX,mouseY,partialTicks);
		copyGiveButton.render(gui,mouseX,mouseY,partialTicks);
		copyModeButton.render(gui,mouseX,mouseY,partialTicks);
		searchBox.render(gui,mouseX,mouseY,partialTicks);
		codeViewer.render(gui,mouseX,mouseY,partialTicks);
		if(copyFeedback.time>0){
			gui.pose().pushPose();
			gui.pose().translate(0,0,300);
			int alpha=Math.min(255,(int)(copyFeedback.time*(255.0F/40.0F)));
			int colorWithFade=(alpha<<24)|(OpmColors.GREEN&0x00FFFFFF);
			gui.drawString(font,Component.translatable("info.opm.copy_feedback"),copyFeedback.x,copyFeedback.y,colorWithFade);
			gui.pose().popPose();
			copyFeedback=new feedbackData(copyFeedback.x,copyFeedback.y,copyFeedback.time-1);
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
		if(codeViewer!=null&&codeViewer.mouseScrolled(mouseX,mouseY,scrollX,scrollY)) return true;
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
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		if(minecraft!=null) minecraft.setScreen(parent);
	}
	private record feedbackData(int x,int y,int time){
	}
	private void copyFeedback(int x,int y,String text){
		copyFeedback=new feedbackData(x+15,y-12,100);
		OpmUtil.copyToClipboard(text);
	}
}
