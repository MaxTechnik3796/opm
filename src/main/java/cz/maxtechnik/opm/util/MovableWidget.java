package cz.maxtechnik.opm.util;

import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
@SuppressWarnings("unused")
public interface MovableWidget{
	record WidgetBounds(int x,int y,int width,int height,int boxLeft,int boxTop){
		public void apply(GuiGraphics gui){
			gui.pose().translate((float)(this.x-this.boxLeft),(float)(this.y-this.boxTop),0.0F);
		}
	}
	WidgetBounds getBounds(Font font,int screenWidth,int screenHeight);
	int getX();
	void setX(int x);
	int getY();
	void setY(int y);
	AnchorX getAnchorX();
	void setAnchorX(AnchorX anchor);
	AnchorY getAnchorY();
	void setAnchorY(AnchorY anchor);
	void save();
}