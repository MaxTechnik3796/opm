package cz.maxtechnik.opm.client.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
public final class UiScale{
	private UiScale(){
	}
	public static int getMaxScale(Minecraft mc){
		if(mc==null) return 4;
		return mc.getWindow().calculateScale(0,mc.isEnforceUnicode());
	}
	public static int getScale(Minecraft mc){
		if(mc==null) return 0;
		return mc.options.guiScale().get();
	}
	public static String getLabel(Minecraft mc){
		int scale=getScale(mc);
		return scale==0?"Auto":scale+"x";
	}
	public static void setScale(Minecraft mc,int scale){
		if(mc==null) return;
		int max=getMaxScale(mc);
		int targetScale=(scale<0||scale>max)?0:scale;
		mc.options.guiScale().set(targetScale);
		mc.options.save();
		mc.resizeDisplay();
	}
	public static void adjustScale(Minecraft mc,int delta){
		if(mc==null) return;
		int max=getMaxScale(mc);
		int current=getScale(mc);
		int next;
		if(delta>0){
			next=(current>=max)?0:current+1;
		}else{
			next=(current<=0)?max:current-1;
		}
		setScale(mc,next);
	}
	public static void cycleScale(Minecraft mc){
		adjustScale(mc,1);
	}
	public static boolean handleKeyPressed(Minecraft mc,int keyCode){
		if(keyCode==GLFW.GLFW_KEY_F8){
			cycleScale(mc);
			return true;
		}
		return false;
	}
}
