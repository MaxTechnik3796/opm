package cz.maxtechnik.opm.util.widget;

import cz.maxtechnik.opm.config.Anchor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
@SuppressWarnings("unused")
public interface OpmMovableWidget{
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
	Anchor.X getAnchorX();
	void setAnchorX(Anchor.X anchor);
	Anchor.Y getAnchorY();
	void setAnchorY(Anchor.Y anchor);
	void save();
}