package cz.maxtechnik.opm.client.editor.layout;

import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;
import java.util.function.Supplier;
public class ToggleGroup{
	private final String[] labels;
	private final Supplier<Integer> selectedSupplier;
	private final Consumer<Integer> onSelect;
	private int anchorX;
	private int anchorY;
	public ToggleGroup(String[] labels,Supplier<Integer> selectedSupplier,Consumer<Integer> onSelect){
		this.labels=labels;
		this.selectedSupplier=selectedSupplier;
		this.onSelect=onSelect;
	}
	public static ToggleGroup of(String[] labels,Supplier<Integer> selectedSupplier,Consumer<Integer> onSelect){
		return new ToggleGroup(labels,selectedSupplier,onSelect);
	}
	public void setAnchor(int x,int y){
		this.anchorX=x;
		this.anchorY=y;
	}
	public int getSelectedIndex(){
		return selectedSupplier!=null?selectedSupplier.get():0;
	}
	public void select(int index){
		if(onSelect!=null&&index>=0&&index<labels.length){
			onSelect.accept(index);
		}
	}
	public int getTotalWidth(Font font){
		int tw=0;
		for(String l: labels) tw+=font.width(l)+16;
		return Math.max(60,tw);
	}
	public void render(GuiGraphics g,Font font,int mx,int my){
		int tw=getTotalWidth(font);
		int bx=anchorX-tw/2;
		UiKit.drawTabs(g,font,bx,anchorY,tw,16,labels,getSelectedIndex(),mx,my);
	}
	public boolean handleClick(int mx,int my,Font font){
		int tw=getTotalWidth(font);
		int bx=anchorX-tw/2;
		int idx=UiKit.getClickedTab(bx,anchorY,tw,16,labels.length,mx,my);
		if(idx>=0){
			select(idx);
			return true;
		}
		return false;
	}
}