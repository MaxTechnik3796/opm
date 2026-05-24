package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.widget.CodeViewerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
public class InspectorScreen extends Screen{

	//Persistentní stav ────────────────────────────────────────────────────────

	private static final String PREFS_FILE="opm_inspector.properties";
	private static final String KEY_SIMPLE="simpleMode";
	private static boolean globalSimpleMode=loadSimpleMode();
	private static boolean loadSimpleMode(){
		try{
			File f=new File(Minecraft.getInstance().gameDirectory,PREFS_FILE);
			if(!f.exists()) return false;
			Properties p=new Properties();
			try(FileReader r=new FileReader(f)){
				p.load(r);
			}
			return "true".equalsIgnoreCase(p.getProperty(KEY_SIMPLE,"false"));
		}catch(Exception e){
			return false;
		}
	}
	private static void saveSimpleMode(boolean value){
		try{
			File f=new File(Minecraft.getInstance().gameDirectory,PREFS_FILE);
			Properties p=new Properties();
			if(f.exists()){
				try(FileReader r=new FileReader(f)){
					p.load(r);
				}
			}
			p.setProperty(KEY_SIMPLE,Boolean.toString(value));
			try(FileWriter w=new FileWriter(f)){
				p.store(w,"OPM Inspector preferences");
			}
		}catch(Exception ignored){
		}
	}

	//Barvy ────────────────────────────────────────────────────────

	private static final int BG=0xF0222222;
	private static final int HEADER_BG=0xFF1A1A1A;
	private static final int BORDER=0xFF000000;
	private static final int TEXT=0xFFDDDDDD;
	private static final int LABEL=0xFF888888;
	private static final int ICON_SZ=32;

	//Pole ────────────────────────────────────────────────────────

	private final ItemStack stack;
	private final Screen parentScreen;
	private final String itemId, modName;
	private final ItemDataBuilder builder;
	private CodeViewerWidget codeViewer;
	private boolean simpleMode=globalSimpleMode;
	private int pX, pY, pW, pH, hdrH;
	private boolean hName, hMod, hId;

	//Konstruktor ────────────────────────────────────────────────────────

	public InspectorScreen(ItemStack stack,Screen parentScreen){
		super(Component.literal("Item Inspector"));
		this.stack=stack;
		this.parentScreen=parentScreen;
		this.builder=new ItemDataBuilder(stack);
		ResourceLocation loc=BuiltInRegistries.ITEM.getKey(stack.getItem());
		this.itemId=loc.toString();
		String ns=loc.getNamespace(), mn=ns;
		try{
			var mc=net.neoforged.fml.ModList.get().getModContainerById(ns);
			if(mc.isPresent()) mn=mc.get().getModInfo().getDisplayName();
		}catch(Exception ignored){
		}
		this.modName=mn;
	}

	//SCREEN INIT ────────────────────────────────────────────────────────

	@Override
	protected void init(){
		super.init();
		pW=Math.min(500,width-40);
		pH=height-60;
		pX=(width-pW)/2;
		pY=20;
		hdrH=ICON_SZ+16;
		rebuildCodeViewer();
	}

	//Vždy vytvoří NOVOU instanci CodeViewerWidget – žádné duplikáty tlačítek.
	private void rebuildCodeViewer(){
		String displayText=simpleMode?builder.buildSimpleText():builder.buildFullText();
		CodeViewerWidget w=new CodeViewerWidget(font,displayText);

		//Copy Give
		w.addButton("Copy Give",58,(mx,my)->{
			Minecraft mc=Minecraft.getInstance();
			String name=mc.player!=null?mc.player.getName().getString():"@s";
			w.clip(builder.buildGiveCommand(name,simpleMode),mx,my);
		});

		//Toggle ◈ Full / ◉ Simple
		w.addButton(simpleMode?"◉ Simple":"◈ Full",58,(mx,my)->{
			simpleMode=!simpleMode;
			globalSimpleMode=simpleMode;
			saveSimpleMode(simpleMode);
			rebuildCodeViewer();
		});
		w.setBounds(pX,pY+hdrH+1,pW,pH-hdrH-1);
		this.codeViewer=w;
	}

	//RENDER ────────────────────────────────────────────────────────

	@Override
	public void render(@NotNull GuiGraphics g,int mx,int my,float pt){
		if(codeViewer==null) return;
		renderBackground(g,mx,my,pt);
		g.fill(pX-1,pY-1,pX+pW+1,pY+pH+1,BORDER);
		g.fill(pX,pY,pX+pW,pY+pH,BG);
		g.fill(pX,pY,pX+pW,pY+hdrH,HEADER_BG);
		g.fill(pX,pY+hdrH,pX+pW,pY+hdrH+1,BORDER);
		int ix=pX+8, iy=pY+(hdrH-ICON_SZ)/2;
		g.pose().pushPose();
		g.pose().translate(ix,iy,0);
		g.pose().scale(2f,2f,1f);
		g.renderItem(stack,0,0);
		g.renderItemDecorations(font,stack,0,0);
		g.pose().popPose();
		int tx=ix+ICON_SZ+10, tw=pX+pW-tx-8, ty=pY+10;
		hName=drawHeaderText(g,stack.getHoverName().getString(),tx,ty,tw,mx,my,0xFFFFFFFF,TEXT,0xFFAAAAAA);
		ty+=14;
		hMod=drawHeaderText(g,modName,tx,ty,tw,mx,my,0xFFCCCCCC,LABEL,0xFF666666);
		ty+=14;
		hId=drawHeaderText(g,itemId,tx,ty,tw,mx,my,0xFF88FF88,0xFF55AA55,0xFF55AA55);
		codeViewer.render(g,mx,my);
		super.render(g,mx,my,pt);
	}
	private boolean drawHeaderText(GuiGraphics g,String text,int x,int y,int maxW,
	                               int mx,int my,int hoverColor,int normalColor,int underlineColor){
		String t=truncate(text,maxW);
		boolean hover=hit(mx,my,x,y,font.width(t));
		g.drawString(font,t,x,y,hover?hoverColor:normalColor,false);
		if(hover) g.fill(x,y+9,x+font.width(t),y+10,underlineColor);
		return hover;
	}

	//INPUT ────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(double mouseX,double mouseY,int button){
		int mx=(int)mouseX, my=(int)mouseY;
		if(button==0){
			if(hName){
				clip(stack.getHoverName().getString());
				return true;
			}
			if(hMod){
				clip(modName);
				return true;
			}
			if(hId){
				clip(itemId);
				return true;
			}
		}
		if(codeViewer.mouseClicked(mx,my,button)) return true;
		return super.mouseClicked(mouseX,mouseY,button);
	}
	@Override
	public boolean mouseDragged(double mx,double my,int btn,double dx,double dy){
		if(codeViewer.mouseDragged((int)my)) return true;
		return super.mouseDragged(mx,my,btn,dx,dy);
	}
	@Override
	public boolean mouseReleased(double mx,double my,int btn){
		if(btn==0) codeViewer.mouseReleased();
		return super.mouseReleased(mx,my,btn);
	}
	@Override
	public boolean mouseScrolled(double mx,double my,double sx,double sy){
		return codeViewer.mouseScrolled(sy,(int)mx,(int)my);
	}
	@Override
	public boolean keyPressed(int key,int scan,int mods){
		if(codeViewer.keyPressed(key,mods)) return true;
		if(key==256){
			onClose();
			return true;
		}
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public boolean charTyped(char chr,int mods){
		if(codeViewer.charTyped(chr)) return true;
		return super.charTyped(chr,mods);
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics,int mouseX,int mouseY,float partialTick){
	}
	@Override
	public void onClose(){
		assert minecraft!=null;
		minecraft.setScreen(parentScreen);
	}

	//HELPERS ────────────────────────────────────────────────────────

	private void clip(String text){
		Minecraft.getInstance().keyboardHandler.setClipboard(text);
	}
	private boolean hit(int mx,int my,int x,int y,int w){
		return mx>=x&&mx<=x+w&&my>=y&&my<=y+9;
	}
	private String truncate(String text,int maxW){
		if(font.width(text)<=maxW) return text;
		while(font.width(text+"...")>maxW&&!text.isEmpty()) text=text.substring(0,text.length()-1);
		return text+"...";
	}
}