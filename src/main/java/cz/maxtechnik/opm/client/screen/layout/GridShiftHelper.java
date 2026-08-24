package cz.maxtechnik.opm.client.screen.layout;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
/**
 * Helper pro posunování mřížky 9x9 (Mechanical Crafter apod.) klávesami WASD / šipkami.
 * Automaticky hlídá hranice mřížky, aby nedošlo k vypadnutí předmětů mimo rozsah.
 */
public final class GridShiftHelper{
	private GridShiftHelper(){
	}
	/**
	 * Posune mřížku předmětů o {@code dx, dy} v rozsahu {@code cols x rows}.
	 * Pokud by posun vytlačil jakýkoliv umístěný předmět mimo mřížku, posun v dané ose zamezí (clamp).
	 */
	public static void shiftGrid(List<ItemStack> grid,int cols,int rows,int dx,int dy){
		if(grid==null||grid.isEmpty()||cols<=0||rows<=0) return;
		int minR=rows, maxR=-1, minC=cols, maxC=-1;
		boolean hasAny=false;
		for(int r=0;r<rows;r++){
			for(int c=0;c<cols;c++){
				int idx=r*cols+c;
				if(idx<grid.size()&&!grid.get(idx).isEmpty()){
					hasAny=true;
					if(r<minR) minR=r;
					if(r>maxR) maxR=r;
					if(c<minC) minC=c;
					if(c>maxC) maxC=c;
				}
			}
		}
		if(!hasAny) return;
		// Ořezávání hranic
		if(minR+dy<0||maxR+dy>=rows) dy=0;
		if(minC+dx<0||maxC+dx>=cols) dx=0;
		if(dx==0&&dy==0) return;
		List<ItemStack> old=new ArrayList<>(grid);
		for(int r=0;r<rows;r++){
			for(int c=0;c<cols;c++){
				int nr=r-dy, nc=c-dx;
				int targetIdx=r*cols+c;
				if(targetIdx<grid.size()){
					if(nr>=0&&nr<rows&&nc>=0&&nc<cols){
						int sourceIdx=nr*cols+nc;
						grid.set(targetIdx,sourceIdx<old.size()?old.get(sourceIdx):ItemStack.EMPTY);
					}else{
						grid.set(targetIdx,ItemStack.EMPTY);
					}
				}
			}
		}
	}
}
