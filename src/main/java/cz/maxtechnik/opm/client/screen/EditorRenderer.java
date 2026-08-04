package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.screen.layout.StationLayoutEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class EditorRenderer{
	//Barvy ────────────────────────────────────────────────────────
	public static final int C_BG=0xFF181818;
	public static final int C_BORDER=0xFF000000;
	public static final int C_TAB=0xFF282828;
	public static final int C_TAB_SEL=0xFF4A4A6A;
	public static final int C_TAB_CR=0xFF352010;
	public static final int C_TAB_CRS=0xFF603810;
	public static final int C_SLOT=0xFF3A3A3A;
	public static final int C_SLOT_HOV=0xFF5A5A5A;
	public static final int C_SLOT_DR=0xFF3A5A3A;
	public static final int C_SLOT_RES=0xFF224422;
	public static final int C_INV=0xFF141414;
	public static final int C_TEXT=0xFFEEEEEE;
	public static final int C_LABEL=0xFFAAAAAA;
	public static final int C_BTN=0xFF383838;
	public static final int C_BTN_H=0xFF585858;
	public static final int C_BTN_G=0xFF1E4A1E;
	public static final int C_BTN_GH=0xFF2A6A2A;
	//Layout konstanty ────────────────────────────────────────────────────────
	public static final int SS=18;
	public static final int SP=2;
	public static final int TAB_H=22;
	public static final int INV_COLS=9;
	public static final int SPIN_W=10, SPIN_H=8;
	public static final int MINI_SPIN=9;
	public static final int IO_GAP=40;
	public static final int IO_INPUT_OFFSET=60;
	//Scrollbar konstanty ────────────────────────────────────────────────────
	public static final int SB_W=4;
	public static final int C_SB_BG=0xFF111111;
	public static final int C_SB_THUMB=0xFF666666;

	Font font;
	private final RecipeEditorData d;
	//Layout (sync z RecipeEditorScreen)
	public int pX, pY, pW, pH, leftW, rightX, rightW;
	public int editorY, editorH, invY;
	public int btnSaveX, btnSaveY, btnClearX, btnCopyX;
	public boolean isDragging;
	public EditorRenderer(Font font,RecipeEditorData data){
		this.font=font;
		this.d=data;
	}
	public void font_set(Font f){
		this.font=f;
	}
	// SCROLLBAR — sdílená klasa ────────────────────────────────────────────────────────
	/**
	 * Stavový scrollbar. Renderování, hit-testing, dragování — vše na jednom místě.
	 * Použití:
	 * sb.update(viewportH, contentH);
	 * sb.render(g, x, y);
	 * sb.startDragIfHit(mx, my);
	 * sb.dragTo(my);
	 * sb.handleScroll(deltaY, stepPx);
	 */
	public static final class Scrollbar{
		public float scroll;
		public int x, y, h;       //posledně vykreslené umístění (pro hit-test)
		public int viewportH;
		public int max;           //maxScroll = content - viewport (≥ 0)
		public boolean dragging;
		public void update(int viewportH,int contentH){
			this.viewportH=viewportH;
			this.max=Math.max(0,contentH-viewportH);
			if(scroll>max) scroll=max;
			if(scroll<0) scroll=0;
		}
		public void render(GuiGraphics g,int sbX,int sbY){
			this.x=sbX;
			this.y=sbY;
			this.h=viewportH;
			if(max<=0) return;
			g.fill(sbX,sbY,sbX+SB_W,sbY+viewportH,C_SB_BG);
			int th=Math.max(20,viewportH*viewportH/(viewportH+max));
			int ty=sbY+(int)((viewportH-th)*(scroll/(float)max));
			g.fill(sbX,ty,sbX+SB_W,ty+th,C_SB_THUMB);
		}
		public boolean hitTrack(int mx,int my){
			return max>0&&mx>=x&&mx<=x+SB_W&&my>=y&&my<=y+h;
		}
		public boolean startDragIfHit(int mx,int my){
			if(hitTrack(mx,my)){
				dragging=true;
				int th=Math.max(20,viewportH*viewportH/(viewportH+max));
				int ty=y+(int)((viewportH-th)*(scroll/(float)max));
				if(my>=ty&&my<=ty+th){
					dragOffset=my-ty;
				}else{
					dragOffset=th/2f;
					dragTo(my);
				}
				return true;
			}
			return false;
		}
		//Při draggingu nastaví scroll podle pozice kursoru (lineárně přes track).
		private float dragOffset;
		public void dragTo(int my){
			if(!dragging||max<=0||h<=0) return;
			int th=Math.max(20,h*h/(h+max));
			int trackH=h-th;
			if(trackH<=0){
				scroll=0;
				return;
			}
			float targetTy=my-dragOffset;
			float t=(targetTy-y)/(float)trackH;
			scroll=Math.clamp(t*max,0,max);
		}
		public void stopDrag(){
			dragging=false;
		}
		//Wheel scroll. deltaY je sy z mouseScrolled (kladné = nahoru).
		public void handleScroll(double deltaY,int stepPx){
			scroll=(float)Math.clamp(scroll-deltaY*stepPx,0,max);
		}
		public void reset(){
			scroll=0;
		}
	}
	//Záložky ────────────────────────────────────────────────────────
	public void renderTabs(GuiGraphics g,int mx,int my,List<StationType> tabs,int tabIdx){
		int tabW=leftW/tabs.size();
		for(int i=0;i<tabs.size();i++){
			StationType t=tabs.get(i);
			int tx=pX+i*tabW;
			int tw=(i==tabs.size()-1)?(pX+leftW-tx):tabW;
			boolean sel=i==tabIdx, hov=hit(mx,my,tx,pY,tw,TAB_H), cr=t.isCreate();
			int bg=sel?(cr?C_TAB_CRS:C_TAB_SEL):hov?(cr?C_TAB_CR:0xFF353535):(cr?C_TAB_CR:C_TAB);
			g.fill(tx,pY,tx+tw,pY+TAB_H,bg);
			if(sel) g.fill(tx,pY+TAB_H-2,tx+tw,pY+TAB_H,0xFF8888FF);
			if(i<tabs.size()-1) g.fill(tx+tw-1,pY+2,tx+tw,pY+TAB_H-2,0xFF444444);
			int iconSz=16;
			int icx=tx+(tw-iconSz)/2;
			try{
				ResourceLocation loc=ResourceLocation.tryParse(t.stationItemId);
				if(loc!=null){
					var opt=BuiltInRegistries.ITEM.getOptional(loc);
					opt.ifPresent(item->g.renderItem(new ItemStack(item),icx,pY+(TAB_H-iconSz)/2));
				}
			}catch(Exception ignored){
			}
		}
	}
	//Editor panely ────────────────────────────────────────────────────────
	public int renderStation(GuiGraphics g, StationType type, int mx, int my) {
		int cx = pX + leftW / 2;
		return StationLayoutEngine.render(g, font, type, d, cx, editorY, mx, my, isDragging);
	}
	//Sdílené pomocné rendery ────────────────────────────────────────────────────────
	//input slot → šipka → result slot + spinner pro count.
	private void renderIOPair(GuiGraphics g,int mx,int my,int cx,int cy,ItemStack input,ItemStack output,int count){
		int sx=cx-IO_INPUT_OFFSET;
		g.drawString(font,"Input",sx,cy-12,C_LABEL,false);
		slot(g,mx,my,input,sx,cy,C_SLOT);
		g.drawString(font,"→",sx+SS+15,cy+5,C_LABEL,false);
		int rx=sx+SS+IO_GAP;
		g.drawString(font,"Result",rx,cy-12,C_LABEL,false);
		slot(g,mx,my,output,rx,cy,C_SLOT_RES);
		spinner(g,mx,my,rx+SS+6,cy+2,count);
	}
	//Crushing/Fan panel: input vlevo + sloupce výstupů + time spinner
	private int renderProcessingPanel(GuiGraphics g,int mx,int my,int cx,int cy,ItemStack input,List<CrushingOutput> outs,int count,int rowsPerCol,int time){
		int sx=cx-120;
		g.drawCenteredString(font,"Input",sx+SS/2,cy-12,C_LABEL);
		slot(g,mx,my,input,sx,cy,C_SLOT);
		g.drawString(font,"→",sx+SS+10,cy+5,C_LABEL,false);
		int outX=sx+SS+30, colW=110;
		g.drawString(font,count==8?"Outputs (chance via +/-):":"Outputs:",outX,cy-12,C_LABEL,false);
		for(int i=0;i<count;i++){
			int ox=outX+(i/rowsPerCol)*colW;
			int oy=cy+(i%rowsPerCol)*(SS+12);
			renderOutputWithChance(g,mx,my,outs.get(i),ox,oy);
		}
		int oy=cy+rowsPerCol*(SS+12)+10;
		g.drawString(font,"Time:",cx-20,oy+4,C_LABEL,false);
		g.drawString(font,time+" t",cx+15,oy+4,C_TEXT,false);
		valSpinner(g,mx,my,cx+55,oy+2);
		return oy+30;
	}
	//Slot + count spinner + chance spinner + label "100%".
	//Pro posunutí prvků (šipek a % šance) u mixeru, presu, crushing a fan upravte hodnoty
	private void renderOutputWithChance(GuiGraphics g,int mx,int my,CrushingOutput co,int ox,int oy){
		slot(g,mx,my,co.stack,ox,oy,co.isEmpty()?C_SLOT:C_SLOT_RES);
		int cpx=ox+SS+4, cpy=oy+2;
		//Text čísla množství (např. "1")
		g.drawString(font,String.valueOf(co.count),cpx,cpy+2,C_TEXT,false);
		//První mini-spinner (šipky +/- pro množství)
		drawMiniSpinner(g,mx,my,cpx+16,cpy-2);
		int chX=cpx+28; //Základní X souřadnice pro šance (šipky + text)
		String chStr=co.chance>=1f?"100%":Math.round(co.chance*100)+"%";
		//Druhý mini-spinner (šipky +/- pro procento šance) - posunout změnou "chX"
		drawMiniSpinner(g,mx,my,chX,cpy-2);
		//Text procenta šance
		g.drawString(font,chStr,chX+14,cpy+3,co.isEmpty()?C_LABEL:0xFFAAFF88,false);
	}
	//Vodorovný picker s vlastními barvami pozadí pro každou položku (heat)
	private void renderHorizPickerColored(GuiGraphics g,int mx,int my,int cx,int cy,String[] labels,int selIdx,int[] colors){
		int tw=0;
		for(String l: labels) tw+=font.width(l)+16;
		int bx=cx-tw/2;
		for(int i=0;i<labels.length;i++){
			int bw=font.width(labels[i])+10;
			boolean sel=selIdx==i, hov=hit(mx,my,bx,cy,bw,16);
			int bg=sel?-11908486:(hov?colors[i]+0x111100:colors[i]);
			g.fill(bx,cy,bx+bw,cy+16,bg);
			g.drawCenteredString(font,labels[i],bx+bw/2,cy+4,sel?-13176:C_TEXT);
			bx+=bw+6;
		}
	}
	//Vodorovný picker s jednotnou barvou (tabs).
	private void renderHorizPicker(GuiGraphics g,int mx,int my,int cx,int cy,String[] labels,int selIdx){
		int tw=0;
		for(String l: labels) tw+=font.width(l)+16;
		int bx=cx-tw/2;
		for(int i=0;i<labels.length;i++){
			int bw=font.width(labels[i])+10;
			boolean sel=selIdx==i, hov=hit(mx,my,bx,cy,bw,16);
			g.fill(bx,cy,bx+bw,cy+16,sel?C_TAB_SEL:(hov?C_BTN_H:C_BTN));
			g.drawCenteredString(font,labels[i],bx+bw/2,cy+4,sel?0xFFCCCCFF:C_TEXT);
			bx+=bw+6;
		}
	}
	//Slot helpers ────────────────────────────────────────────────────────
	public void renderGridN(GuiGraphics g,int mx,int my,List<ItemStack> list,int cols,int rows,int sx,int sy,int sz,int padX,int padY){
		for(int r=0;r<rows;r++)
			for(int c=0;c<cols;c++){
				int bx=sx+c*(sz+padX);
				int by=sy+r*(sz+padY);
				int idx=r*cols+c;
				boolean hov=hit(mx,my,bx,by,sz,sz), drop=isDragging&&hov;
				g.fill(bx-1,by-1,bx+sz+1,by+sz+1,C_BORDER);
				g.fill(bx,by,bx+sz,by+sz,drop?C_SLOT_DR:(hov?C_SLOT_HOV:C_SLOT));
				ItemStack s=idx<list.size()?list.get(idx):ItemStack.EMPTY;
				if(!s.isEmpty()) itemScaled(g,s,bx,by,sz);
				//Pokud je velký padding (mixing), kresli count spinner vedle slotu
				if(padX>=24){
					//cpxText určuje X pozici čísla množství ingredience, cpxClick je základ pro hitbox, cpy je výška
					int cpxText=bx+sz+3, cpxClick=bx+sz+1, cpy=by+2;
					int count=!s.isEmpty()?s.getCount():1;
					//Text množství ingredience
					g.drawString(font,String.valueOf(count),cpxText,cpy+2,C_TEXT,false);
					//Mini-spinner (šipky +/-) pro ingredience - posunout změnou "cpxClick + 20"
					drawMiniSpinner(g,mx,my,cpxClick+20,cpy-2);
				}
			}
	}
	public void slot(GuiGraphics g,int mx,int my,ItemStack s,int sx,int sy,int bg){
		boolean hov=hit(mx,my,sx,sy,SS,SS), drop=isDragging&&hov;
		g.fill(sx-1,sy-1,sx+SS+1,sy+SS+1,C_BORDER);
		g.fill(sx,sy,sx+SS,sy+SS,drop?C_SLOT_DR:(hov?C_SLOT_HOV:bg));
		if(s!=null&&!s.isEmpty()){
			ItemStack rs=s.copy();
			rs.setCount(1);
			g.renderItem(rs,sx+1,sy+1);
			g.renderItemDecorations(font,rs,sx+1,sy+1);
		}
	}
	public void slotFluid(GuiGraphics g,int mx,int my,FluidEntry f,int sx,int sy){
		boolean hov=hit(mx,my,sx,sy,SS,SS), drop=isDragging&&hov;
		g.fill(sx-1,sy-1,sx+SS+1,sy+SS+1,0xFF2255AA);
		g.fill(sx,sy,sx+SS,sy+SS,drop?0xFF2A5A6A:(hov?0xFF2A3A6A:0xFF1A2A4A));
		if(!f.isEmpty()) g.renderItem(f.proxy,sx+1,sy+1);
		else g.drawCenteredString(font,"~",sx+SS/2,sy+(SS-8)/2,0xFF4488CC);
		//amtX a amtY určují pozici textu s množstvím fluidu v mB
		int amtX=sx+SS+4, amtY=sy+4;
		g.drawString(font,f.amount+" mB",amtX,amtY,0xFF66AAFF,false);
		//hP a hM určují klikací zónu tlačítek + a -
		boolean hP=hit(mx,my,amtX-2,amtY+12,SPIN_W,SPIN_H);
		boolean hM=hit(mx,my,amtX+10,amtY+12,SPIN_W,SPIN_H);
		//Vykreslení pozadí tlačítek "+" a "-"
		g.fill(amtX-2,amtY+12,amtX+8,amtY+20,hP?C_BTN_H:C_BTN);
		g.fill(amtX+10,amtY+12,amtX+20,amtY+20,hM?C_BTN_H:C_BTN);
		//Vykreslení znaků "+" a "-"
		g.drawCenteredString(font,"+",amtX+3,amtY+12,C_TEXT);
		g.drawCenteredString(font,"-",amtX+15,amtY+12,C_TEXT);
	}
	public void invSlotRender(GuiGraphics g,int mx,int my,ItemStack s,int sx,int sy){
		boolean hov=hit(mx,my,sx,sy,SS,SS);
		g.fill(sx-1,sy-1,sx+SS+1,sy+SS+1,C_BORDER);
		g.fill(sx,sy,sx+SS,sy+SS,hov?C_SLOT_HOV:C_SLOT);
		if(s!=null&&!s.isEmpty()){
			g.renderItem(s,sx+1,sy+1);
			g.renderItemDecorations(font,s,sx+1,sy+1);
		}
	}
	public void spinner(GuiGraphics g,int mx,int my,int cx,int cy,int count){
		g.drawString(font,String.valueOf(count),cx,cy+2,C_TEXT,false);
		boolean hP=hit(mx,my,cx+18,cy,SPIN_W,SPIN_H), hM=hit(mx,my,cx+18,cy+8,SPIN_W,SPIN_H);
		g.fill(cx+18,cy,cx+28,cy+8,hP?C_BTN_H:C_BTN);
		g.fill(cx+18,cy+8,cx+28,cy+16,hM?C_BTN_H:C_BTN);
		g.drawCenteredString(font,"+",cx+23,cy,C_TEXT);
		g.drawCenteredString(font,"-",cx+23,cy+8,C_TEXT);
	}
	public void valSpinner(GuiGraphics g,int mx,int my,int cx,int cy){
		boolean hP=hit(mx,my,cx,cy,SPIN_W,SPIN_H), hM=hit(mx,my,cx,cy+8,SPIN_W,SPIN_H);
		g.fill(cx,cy,cx+10,cy+8,hP?C_BTN_H:C_BTN);
		g.fill(cx,cy+8,cx+10,cy+16,hM?C_BTN_H:C_BTN);
		g.drawCenteredString(font,"+",cx+5,cy,C_TEXT);
		g.drawCenteredString(font,"-",cx+5,cy+8,C_TEXT);
	}
	public void drawMiniSpinner(GuiGraphics g,int mx,int my,int cx,int cy){
		boolean hP=hit(mx,my,cx,cy,MINI_SPIN,MINI_SPIN), hM=hit(mx,my,cx,cy+9,MINI_SPIN,MINI_SPIN);
		g.fill(cx,cy,cx+9,cy+9,hP?C_BTN_H:C_BTN);
		g.fill(cx,cy+9,cx+9,cy+18,hM?C_BTN_H:C_BTN);
		g.drawCenteredString(font,"+",cx+4,cy,C_TEXT);
		g.drawCenteredString(font,"-",cx+4,cy+9,C_TEXT);
	}
	public void drawToggle2(GuiGraphics g,int mx,int my,int x,int y,String a,String b,boolean aOn){
		int wa=font.width(a)+12, wb=font.width(b)+12;
		boolean ha=hit(mx,my,x,y,wa,16), hb=hit(mx,my,x+wa+2,y,wb,16);
		g.fill(x,y,x+wa,y+16,aOn?C_TAB_SEL:(ha?C_BTN_H:C_BTN));
		g.fill(x+wa+2,y,x+wa+2+wb,y+16,!aOn?C_TAB_SEL:(hb?C_BTN_H:C_BTN));
		g.drawCenteredString(font,a,x+wa/2,y+4,aOn?0xFFCCCCFF:C_TEXT);
		g.drawCenteredString(font,b,x+wa+2+wb/2,y+4,!aOn?0xFFCCCCFF:C_TEXT);
	}
	public void drawBtn(GuiGraphics g,String lbl,int bx,int by,int bw,boolean hov,int bg,int hbg){
		g.fill(bx,by,bx+bw,by+16,hov?hbg:bg);
		g.fill(bx,by,bx+bw,by+1,0x44FFFFFF);
		g.drawCenteredString(font,lbl,bx+bw/2,by+4,C_TEXT);
	}
	public void drawMicroBtn(GuiGraphics g,String lbl,int bx,int by,int bw,int bh,boolean hov){
		g.fill(bx,by,bx+bw,by+bh,hov?C_BTN_H:C_BTN);
		g.fill(bx,by,bx+bw,by+1,0x44FFFFFF);
		g.drawCenteredString(font,lbl,bx+bw/2,by+(bh-8)/2,C_TEXT);
	}
	public void renderBtnBar(GuiGraphics g,int mx,int my,String fileName,boolean fnFocused,int fnCursor){
		boolean hS=hit(mx,my,btnSaveX,btnSaveY,92,16);
		boolean hC=hit(mx,my,btnClearX,btnSaveY,40,16);
		boolean hP=hit(mx,my,btnCopyX,btnSaveY,60,16);
		drawBtn(g,"Generate",btnSaveX,btnSaveY,92,hS,C_BTN_G,C_BTN_GH);
		drawBtn(g,"Clear",btnClearX,btnSaveY,40,hC,C_BTN,C_BTN_H);
		drawBtn(g,"Copy",btnCopyX,btnSaveY,60,hP,C_BTN,C_BTN_H);
		int fx=btnCopyX+65, fy=btnSaveY;
		int fw=leftW-fx-10;
		if(fw>20){
			g.drawString(font,"File:",fx,fy+4,C_LABEL,false);
			int ffx=fx+font.width("File:")+5;
			int ffw=leftW-ffx-10;
			g.fill(ffx-1,fy-1,ffx+ffw+1,fy+17,C_BORDER);
			g.fill(ffx,fy,ffx+ffw,fy+16,fnFocused?0xFF3D3D3D:0xFF303030);
			String dn=truncate(fileName,ffw-6);
			g.drawString(font,dn,ffx+4,fy+4,C_TEXT,false);
			if(fnFocused&&(System.currentTimeMillis()/500)%2==0){
				int cx=ffx+4+font.width(dn.substring(0,Math.min(fnCursor,dn.length())));
				g.fill(cx,fy+3,cx+1,fy+13,C_TEXT);
			}
		}
		if(!d.statusMsg.isEmpty()&&System.currentTimeMillis()<d.statusUntil)
			g.drawCenteredString(font,d.statusMsg,leftW/2,btnSaveY-14,d.statusOk?0xFF88FF88:0xFFFF6666);
	}
	public void renderErrorPopup(GuiGraphics g,int mx,int my,String error,int width,int height){
		g.fill(0,0,width,height,0xAA000000);
		int pw=260, ph=100, px2=(width-pw)/2, py2=(height-ph)/2;
		g.fill(px2,py2,px2+pw,py2+ph,0xFF222222);
		g.fill(px2,py2,px2+pw,py2+2,0xFFFF3333);
		g.fill(px2,py2+ph-2,px2+pw,py2+ph,0xFFFF3333);
		g.fill(px2,py2,px2+2,py2+ph,0xFFFF3333);
		g.fill(px2+pw-2,py2,px2+pw,py2+ph,0xFFFF3333);
		g.drawString(font,"Error",px2+(pw-font.width("Error"))/2,py2+12,0xFFFF3333,false);
		g.drawString(font,error,px2+(pw-font.width(error))/2,py2+36,C_TEXT,false);
		int bx=px2+(pw-60)/2, by=py2+65, bw=60, bh=18;
		boolean hov=mx>=bx&&mx<=bx+bw&&my>=by&&my<=by+bh;
		g.fill(bx,by,bx+bw,by+bh,hov?0xFF666666:0xFF444444);
		g.fill(bx,by,bx+bw,by+1,0xFF888888);
		g.fill(bx,by+bh-1,bx+bw,by+bh,0xFF888888);
		g.drawString(font,"OK",bx+(bw-font.width("OK"))/2,by+5,C_TEXT,false);
	}
	public void showTip(GuiGraphics g,ItemStack s,int mx,int my){
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null)
			g.renderComponentTooltip(font,s.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.Default.NORMAL),mx,my);
	}
	private void itemScaled(GuiGraphics g,ItemStack s,int sx,int sy,int sz){
		ItemStack rs=s.copy();
		rs.setCount(1);
		if(sz>=16){
			int off=(sz-16)/2;
			g.renderItem(rs,sx+off,sy+off);
			if(sz>=18) g.renderItemDecorations(font,rs,sx+off,sy+off);
		}else{
			float sc=sz/16f;
			var p=g.pose();
			p.pushPose();
			p.translate(sx,sy,0);
			p.scale(sc,sc,1f);
			g.renderItem(rs,0,0);
			p.popPose();
		}
	}
	public boolean hit(int mx,int my,int hx,int hy,int hw,int hh){
		return mx>=hx&&mx<=hx+hw&&my>=hy&&my<=hy+hh;
	}
	private String truncate(String t,int maxW){
		if(font.width(t)<=maxW) return t;
		while(font.width(t+"…")>maxW&&!t.isEmpty()) t=t.substring(0,t.length()-1);
		return t+"…";
	}
}