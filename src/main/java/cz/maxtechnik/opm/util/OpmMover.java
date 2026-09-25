package cz.maxtechnik.opm.util;

import cz.maxtechnik.opm.OpmMod;
import org.lwjgl.glfw.GLFW;
@SuppressWarnings("unused")
public class OpmMover{
	private static int[] mover={0,0};
	public static void update(int key){
		switch(key){
			case GLFW.GLFW_KEY_UP -> mover[1]=mover[1]-1;
			case GLFW.GLFW_KEY_DOWN -> mover[1]=mover[1]+1;
			case GLFW.GLFW_KEY_LEFT -> mover[0]=mover[0]-1;
			case GLFW.GLFW_KEY_RIGHT -> mover[0]=mover[0]+1;
			case GLFW.GLFW_KEY_KP_0 -> mover=new int[]{0,0};
		}
		OpmMod.LOGGER.info("Mover pos [{}, {}]; Key pressed: {}",mover[0],mover[1],key);
	}
	public static int getX(){
		return mover[0];
	}
	public static int getY(){
		return mover[1];
	}
}