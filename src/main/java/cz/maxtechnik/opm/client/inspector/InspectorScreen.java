package cz.maxtechnik.opm.client.inspector;

import cz.maxtechnik.opm.client.ui.CodeViewerWidget;
import cz.maxtechnik.opm.client.ui.UiKit;
import cz.maxtechnik.opm.client.ui.UiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
public final class InspectorScreen extends Screen{
	// Trvalý stav (pamatuje si zvolený mód mezi otevřeními)
	private static final String PREFS_FILE="opm_inspector.txt";
	private static boolean globalSimpleMode=loadSimpleMode();
	private static boolean loadSimpleMode(){
		try{
			File file=new File(Minecraft.getInstance().gameDirectory,PREFS_FILE);
			return file.exists()&&"true".equalsIgnoreCase(Files.readString(file.toPath()).trim());
		}catch(Exception e){
			return false;
		}
	}
	private static void saveSimpleMode(boolean value){
		try{
			File file=new File(Minecraft.getInstance().gameDirectory,PREFS_FILE);
			Files.writeString(file.toPath(),Boolean.toString(value));
		}catch(Exception ignored){
		}
	}
	private static final int ICON_SIZE=32;
	// Pole
	private final ItemStack stack;
	private final Screen parentScreen;
	private final String itemId, modName;
	private final ItemDataBuilder builder;
	private CodeViewerWidget codeViewer;
	private boolean simpleMode=globalSimpleMode;
	private int panelX, panelY, panelW, panelH, headerH;
	private boolean hoverName, hoverMod, hoverId;
	public InspectorScreen(ItemStack stack,Screen parentScreen){
		super(Component.literal("Item Inspector"));
		this.stack=stack;
		this.parentScreen=parentScreen;
		this.builder=new ItemDataBuilder(stack);
		ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
		this.itemId=loc.toString();
		String namespace=loc.getNamespace(), displayName=namespace;
		try{
			var container=net.neoforged.fml.ModList.get().getModContainerById(namespace);
			if(container.isPresent()) displayName=container.get().getModInfo().getDisplayName();
		}catch(Exception ignored){
		}
		this.modName=displayName;
	}
	@Override
	protected void init(){
		super.init();
		panelW=Math.min(500,width-40);
		panelH=height-60;
		panelX=(width-panelW)/2;
		panelY=20;
		headerH=ICON_SIZE+16;
		rebuildCodeViewer();
	}
	/** Vytvoří novou instanci CodeViewerWidget */
	private void rebuildCodeViewer(){
		String displayText=simpleMode?builder.buildSimpleText():builder.buildFullText();
		CodeViewerWidget viewer=new CodeViewerWidget(font,displayText);
		viewer.addButton("Copy Give",58,(mx,my)->{
			Minecraft mc=Minecraft.getInstance();
			String playerName=mc.player!=null?mc.player.getName().getString():"@s";
			viewer.clip(builder.buildGiveCommand(playerName,simpleMode),mx,my);
		});
		viewer.addButton(simpleMode?"Simple":"Full",46,(mx,my)->{
			simpleMode=!simpleMode;
			globalSimpleMode=simpleMode;
			saveSimpleMode(simpleMode);
			rebuildCodeViewer();
		});
		viewer.setBounds(panelX+1,panelY+headerH+1,panelW-2,panelH-headerH-2);
		this.codeViewer=viewer;
	}
	@Override
	public void renderBackground(@NotNull GuiGraphics g,int mx,int my,float pt){
	}
	@Override
	public void render(@NotNull GuiGraphics g,int mx,int my,float pt){
		if(codeViewer==null) return;
		renderBackground(g,mx,my,pt);
		// Jednolitý panel
		g.fill(panelX,panelY,panelX+panelW,panelY+panelH,UiKit.C_BG);
		UiKit.drawOutline(g,panelX,panelY,panelW,panelH,UiKit.C_BORDER);
		// Hlavička s oddělovací linkou
		g.fill(panelX+1,panelY+1,panelX+panelW-1,panelY+headerH,UiKit.C_HEADER);
		g.fill(panelX+1,panelY+headerH,panelX+panelW-1,panelY+headerH+1,UiKit.C_BORDER);
		// Ikona předmětu (2x zvětšená)
		int iconX=panelX+8, iconY=panelY+(headerH-ICON_SIZE)/2;
		g.pose().pushPose();
		g.pose().translate(iconX,iconY,0);
		g.pose().scale(2f,2f,1f);
		g.renderItem(stack,0,0);
		g.renderItemDecorations(font,stack,0,0);
		g.pose().popPose();
		// 3 textové řádky pod sebou s hoverem
		int textX=iconX+ICON_SIZE+10, textW=panelX+panelW-textX-8;
		int textY=panelY+7;
		hoverName=drawClickableText(g,stack.getHoverName().getString(),textX,textY,textW,mx,my,0xFFFFFFFF,UiKit.C_TEXT,UiKit.C_LABEL);
		textY+=13;
		hoverMod=drawClickableText(g,modName,textX,textY,textW,mx,my,UiKit.C_ACCENT_HOV,UiKit.C_ACCENT,UiKit.C_ACCENT_HOV);
		textY+=13;
		hoverId=drawClickableText(g,itemId,textX,textY,textW,mx,my,0xFF88FF88,0xFF55FF55,0xFF88FF88);
		codeViewer.render(g,mx,my);
		super.render(g,mx,my,pt);
	}
	private boolean drawClickableText(GuiGraphics g,String text,int x,int y,int maxW,int mx,int my,int hoverColor,int normalColor,int underlineColor){
		String truncated=UiKit.truncate(font,text,maxW);
		boolean hover=mx>=x&&mx<=x+font.width(truncated)&&my>=y&&my<=y+9;
		g.drawString(font,truncated,x,y,hover?hoverColor:normalColor,false);
		if(hover) g.fill(x,y+9,x+font.width(truncated),y+10,underlineColor);
		return hover;
	}
	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		int mx=(int)mouseX, my=(int)mouseY;
		if(button==0){
			if(hoverName){
				codeViewer.clip(stack.getHoverName().getString(),mx,my);
				return true;
			}
			if(hoverMod){
				codeViewer.clip(modName,mx,my);
				return true;
			}
			if(hoverId){
				codeViewer.clip(itemId,mx,my);
				return true;
			}
		}
		if(codeViewer!=null&&codeViewer.mouseClicked(mx,my,button)) return true;
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseDragged(double mouseX,double mouseY,int button,double dx,double dy){
		if(codeViewer!=null&&codeViewer.mouseDragged((int)mouseY)) return true;
		return super.mouseDragged(mouseX,mouseY,button,dx,dy);
	}
	@Override
	public boolean mouseReleased(double mouseX,double mouseY,int button){
		if(codeViewer!=null) codeViewer.mouseReleased();
		return super.mouseReleased(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
		if(codeViewer!=null&&codeViewer.mouseScrolled(scrollY,(int)mouseX,(int)mouseY)) return true;
		return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
	}
	@Override
	public boolean keyPressed(int keyCode,int scanCode,int modifiers){
		if(UiScale.handleKeyPressed(minecraft,keyCode)) return true;
		if(codeViewer!=null&&codeViewer.keyPressed(keyCode,modifiers)) return true;
		if(keyCode==256){ // ESC
			onClose();
			return true;
		}
		return super.keyPressed(keyCode,scanCode,modifiers);
	}
	@Override
	public boolean charTyped(char codePoint,int modifiers){
		if(codeViewer!=null&&codeViewer.charTyped(codePoint)) return true;
		return super.charTyped(codePoint,modifiers);
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	@Override
	public void onClose(){
		if(minecraft!=null) minecraft.setScreen(parentScreen);
	}
}