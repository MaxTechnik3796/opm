package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
public class ArmorHudElement extends BaseHudElement{
	private static final int SLOT_SIZE=16;
	private static final int GAP=4;
	private static final int EDGE_PAD=2;
	private static final EquipmentSlot[] CANONICAL={
			EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET
	};
	private static final ItemStack[] MOCK_ARMOR={
			new ItemStack(Items.IRON_HELMET),new ItemStack(Items.IRON_CHESTPLATE),
			new ItemStack(Items.IRON_LEGGINGS),new ItemStack(Items.IRON_BOOTS)
	};
	private OpmConfig.HudLocation location;
	private int rotate;
	private boolean locked;
	private int freeX, freeY;
	public ArmorHudElement(){
		super("armor","Armor HUD","🛡",OpmConfig.ARMOR_HUD_ENABLED,OpmConfig.ARMOR_HUD_SCALE);
		this.location=OpmConfig.ARMOR_HUD_LOCATION.get();
		this.rotate=OpmConfig.ARMOR_HUD_ROTATE.get();
		this.locked=OpmConfig.ARMOR_HUD_LOCKED.get();
		this.freeX=OpmConfig.ARMOR_HUD_FREE_X.get();
		this.freeY=OpmConfig.ARMOR_HUD_FREE_Y.get();
	}
	@Override
	public int getW(){
		int span=4*SLOT_SIZE+3*GAP;
		int rawW=(rotate==0||rotate==2)?span:SLOT_SIZE;
		return Math.max(16,(int)(rawW*scale));
	}
	@Override
	public int getH(){
		int span=4*SLOT_SIZE+3*GAP;
		int rawH=(rotate==0||rotate==2)?SLOT_SIZE:span;
		return Math.max(16,(int)(rawH*scale));
	}
	@Override
	public int getX(int screenW,int screenH){
		int w=getW();
		if(!locked) return Math.clamp(freeX,EDGE_PAD,screenW-w-EDGE_PAD);
		int hotbarX=(screenW-182)/2;
		boolean horiz=(rotate==0||rotate==2);
		int startX=(location==OpmConfig.HudLocation.LEFT)
				?hotbarX-GAP-(horiz?w:(int)(SLOT_SIZE*scale))
				:hotbarX+182+GAP;
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null&&!mc.player.getOffhandItem().isEmpty()){
			boolean offhandLeft=(mc.player.getMainArm()==HumanoidArm.RIGHT);
			if(location==OpmConfig.HudLocation.LEFT&&offhandLeft) startX-=29;
			else if(location==OpmConfig.HudLocation.RIGHT&&!offhandLeft) startX+=29;
		}
		return Math.clamp(startX,EDGE_PAD,screenW-w-EDGE_PAD);
	}
	@Override
	public int getY(int screenW,int screenH){
		int h=getH();
		if(!locked) return Math.clamp(freeY,EDGE_PAD,screenH-h-EDGE_PAD);
		int itemY=screenH-22;
		boolean horiz=(rotate==0||rotate==2);
		int startY=horiz?itemY:(rotate==3?itemY:itemY-h+(int)(SLOT_SIZE*scale));
		return Math.clamp(startY,EDGE_PAD,screenH-h-EDGE_PAD);
	}
	@Override
	public void onDrag(int mx,int my,int grabX,int grabY,int screenW,int screenH){
		if(locked){
			if(mx<screenW/2){
				location=OpmConfig.HudLocation.LEFT;
			}else{
				location=OpmConfig.HudLocation.RIGHT;
			}
		}else{
			freeX=mx-grabX;
			freeY=my-grabY;
			clamp(screenW,screenH);
		}
	}
	@Override
	public void clamp(int screenW,int screenH){
		int w=getW(), h=getH();
		freeX=Math.clamp(freeX,EDGE_PAD,screenW-w-EDGE_PAD);
		freeY=Math.clamp(freeY,EDGE_PAD,screenH-h-EDGE_PAD);
	}
	@Override
	public void reset(){
		freeX=EDGE_PAD;
		freeY=EDGE_PAD;
		scale=1.0;
		locked=true;
		location=OpmConfig.HudLocation.RIGHT;
		rotate=1;
	}
	@Override
	public void save(){
		super.save();
		OpmConfig.ARMOR_HUD_LOCATION.set(location);
		OpmConfig.ARMOR_HUD_ROTATE.set(rotate);
		OpmConfig.ARMOR_HUD_LOCKED.set(locked);
		OpmConfig.ARMOR_HUD_FREE_X.set(freeX);
		OpmConfig.ARMOR_HUD_FREE_Y.set(freeY);
	}
	@Override
	protected String getBadgeText(){
		return title+(locked?" [LOCKED]":"");
	}
	@Override
	protected void renderContent(GuiGraphics g,Font font,int x,int y,int screenW,int screenH){
		boolean horiz=(rotate==0||rotate==2);
		EquipmentSlot[] slots=CANONICAL.clone();
		if(rotate==2||rotate==3){
			for(int i=0, j=slots.length-1;i<j;i++,j--){
				EquipmentSlot t=slots[i];
				slots[i]=slots[j];
				slots[j]=t;
			}
		}
		Minecraft mc=Minecraft.getInstance();
		Player player=mc.player;
		int curX=0, curY=0;
		for(EquipmentSlot slot: slots){
			int slotIdx=slot==EquipmentSlot.HEAD?0:(slot==EquipmentSlot.CHEST?1:(slot==EquipmentSlot.LEGS?2:3));
			ItemStack stack=(player!=null&&!player.getItemBySlot(slot).isEmpty())?player.getItemBySlot(slot):MOCK_ARMOR[slotIdx];
			g.renderItem(stack,curX,curY);
			if(stack.isDamageableItem()&&stack.isDamaged()){
				float f=1F-(float)stack.getDamageValue()/stack.getMaxDamage();
				int bx=curX+2, barY=curY+SLOT_SIZE+1;
				g.fill(bx-1,barY-1,bx+14,barY+2,0xFF000000);
				int durCol=0xFF000000|(Math.round(255*(1-f))<<16)|(Math.round(255*f)<<8);
				g.fill(bx,barY,bx+Math.round(f*13),barY+1,durCol);
			}
			if(horiz) curX+=SLOT_SIZE+GAP;
			else curY+=SLOT_SIZE+GAP;
		}
	}
	@Override
	protected int renderCustomInspectorOptions(GuiGraphics g,Font font,int x,int y,int w,int mx,int my){
		int curY=y;
		cz.maxtechnik.opm.client.ui.UiKit.drawToggle(g,font,"Locked to Hotbar",locked,x,curY,w,mx,my);
		curY+=cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
		if(locked){
			cz.maxtechnik.opm.client.ui.UiKit.drawEnumCycler(g,font,"Side",location.name(),x,curY,w,mx,my);
			curY+=cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
		}
		String rotLabel=switch(rotate){
			case 0 -> "Horiz →";
			case 1 -> "Vert ↓";
			case 2 -> "Horiz ←";
			default -> "Vert ↑";
		};
		cz.maxtechnik.opm.client.ui.UiKit.drawEnumCycler(g,font,"Rotation",rotLabel,x,curY,w,mx,my);
		curY+=cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
		return curY;
	}
	@Override
	protected int getCustomInspectorHeight(int startY){
		return startY+(locked?3:2)*cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
	}
	@Override
	protected boolean handleCustomInspectorClick(int mx,int my,int x,int startY,int w){
		int curY=startY;
		// Locked toggle
		if(cz.maxtechnik.opm.client.ui.UiKit.isToggleHit(mx,my,x,curY,w)){
			locked=!locked;
			return true;
		}
		curY+=cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
		// Side enum (if locked)
		if(locked){
			if(cz.maxtechnik.opm.client.ui.UiKit.isEnumHit(mx,my,x,curY,w)){
				location=(location==OpmConfig.HudLocation.LEFT)?OpmConfig.HudLocation.RIGHT:OpmConfig.HudLocation.LEFT;
				return true;
			}
			curY+=cz.maxtechnik.opm.client.ui.UiKit.ITEM_H;
		}
		// Rotation enum
		if(cz.maxtechnik.opm.client.ui.UiKit.isEnumHit(mx,my,x,curY,w)){
			rotate=(rotate+1)%4;
			return true;
		}
		return false;
	}
}
