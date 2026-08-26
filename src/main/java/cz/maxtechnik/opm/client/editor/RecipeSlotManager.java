package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.editor.layout.SlotGroup;
import cz.maxtechnik.opm.client.editor.layout.StationLayoutEngine;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
public class RecipeSlotManager{
	public record SlotPos(int x,int y,int size,Supplier<ItemStack> get,Consumer<ItemStack> set){
		public boolean contains(int mx,int my,int cx,int cy,float scale){
			int smx=(scale<0.99f&&scale>0)?(int)(cx+(mx-cx)/scale):mx;
			int smy=(scale<0.99f&&scale>0)?(int)(cy+(my-cy)/scale):my;
			return smx>=x&&smx<=x+size&&smy>=y&&smy<=y+size;
		}
	}
	public static List<SlotPos> getItemSlots(StationType station,RecipeEditorData data,int panelX,int leftWidth,int editorTop){
		List<SlotPos> slots=new ArrayList<>();
		int centerX=panelX+leftWidth/2;
		var layout=StationLayoutEngine.getLayout(station,data);
		int contentY=StationLayoutEngine.getContentY(station,layout,editorTop);
		StationLayoutEngine.setupLayoutAnchors(layout,station,centerX,contentY);
		SlotGroup inG=layout.getInputSlots(), outG=layout.getOutputSlots();
		SlotGroup inF=layout.getInputFluids(), outF=layout.getOutputFluids();
		if(station==StationType.FILLING){
			slots.add(new SlotPos(inG.getAnchorX(),contentY,UiKit.SS,()->data.fillIn,s->StationLayoutEngine.setInputItem(data,station,0,s)));
			slots.add(new SlotPos(inF.getAnchorX(),contentY,UiKit.SS,()->data.fillFluid.proxy,s->setProxy(data.fillFluid,s,true)));
			slots.add(new SlotPos(outG.getAnchorX(),contentY,UiKit.SS,()->StationLayoutEngine.getResultItem(data,station),s->StationLayoutEngine.setOutputItem(data,station,s)));
			return slots;
		}
		if(inG!=null){
			List<ItemStack> inItems=StationLayoutEngine.getItemListForGroup(data,station,true);
			for(int i=0;i<inG.getTotalSlots();i++){
				int idx=i;
				slots.add(new SlotPos(inG.getSlotX(i),inG.getSlotY(i),inG.getSlotSize(),
						()->idx<inItems.size()?inItems.get(idx):ItemStack.EMPTY,
						s->StationLayoutEngine.setInputItem(data,station,idx,s)));
			}
			if(outG!=null){
				var crushOuts=StationLayoutEngine.getCrushingOutputsForGroup(data,station);
				if(crushOuts!=null){
					for(int i=0;i<outG.getTotalSlots()&&i<crushOuts.size();i++){
						int idx=i;
						slots.add(new SlotPos(outG.getSlotX(i),outG.getSlotY(i),UiKit.SS,()->crushOuts.get(idx).stack,s->{
							if(!RecipeEditorData.isTagItem(s)) crushOuts.get(idx).stack=s;
						}));
					}
				}else{
					slots.add(new SlotPos(outG.getAnchorX(),outG.getAnchorY(),UiKit.SS,()->StationLayoutEngine.getResultItem(data,station),s->StationLayoutEngine.setOutputItem(data,station,s)));
				}
			}
		}
		if(inF!=null){
			var fInputs=StationLayoutEngine.getFluidInputs(data,station);
			for(int i=0;i<inF.getTotalSlots()&&i<fInputs.size();i++){
				int idx=i;
				slots.add(new SlotPos(inF.getSlotX(i),inF.getSlotY(i),UiKit.SS,()->fInputs.get(idx).proxy,s->setProxy(fInputs.get(idx),s,true)));
			}
		}
		if(outF!=null){
			var fOutputs=StationLayoutEngine.getFluidOutputs(data,station);
			for(int i=0;i<outF.getTotalSlots()&&i<fOutputs.size();i++){
				int idx=i;
				slots.add(new SlotPos(outF.getSlotX(i),outF.getSlotY(i),UiKit.SS,()->fOutputs.get(idx).proxy,s->setProxy(fOutputs.get(idx),s,false)));
			}
		}
		return slots;
	}
	private static void setProxy(StationType.FluidEntry entry,ItemStack s,boolean allowTag){
		if(s.isEmpty()||(allowTag?RecipeEditorData.isFluidOrTag(s):RecipeEditorData.isFluidItem(s))){
			entry.proxy=s.isEmpty()?ItemStack.EMPTY:s.copy();
			if(!entry.proxy.isEmpty()) entry.proxy.setCount(1);
		}
	}
	public static ItemStack getSlotItemAt(StationType station,RecipeEditorData data,int panelX,int leftWidth,int editorTop,int inventoryTop,float scroll,int mx,int my,BottomInventoryPanel bottomPanel,int panelH){
		if(my>=editorTop&&my<inventoryTop-20&&mx>=panelX&&mx<panelX+leftWidth){
			int scrolledY=(int)(my+scroll);
			var layout=StationLayoutEngine.getLayout(station,data);
			int cx=panelX+leftWidth/2, cy=StationLayoutEngine.getContentY(station,layout,editorTop);
			float scale=StationLayoutEngine.getScale(station,layout,leftWidth);
			for(SlotPos slot: getItemSlots(station,data,panelX,leftWidth,editorTop)){
				if(slot.contains(mx,scrolledY,cx,cy,scale)) return slot.get().get();
			}
		}
		return bottomPanel!=null?bottomPanel.itemAt(panelX,panelH,inventoryTop,mx,my):ItemStack.EMPTY;
	}
}
