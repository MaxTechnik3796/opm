package cz.maxtechnik.opm.util.widget;

import cz.maxtechnik.opm.config.Anchor;
import net.minecraft.client.gui.Font;
@SuppressWarnings("unused")
public interface OpmMovableWidget{
	record WidgetBounds(int x,int y,int width,int height){
		public boolean contains(double mouseX,double mouseY){
			return mouseX>=this.x&&mouseX<this.x+this.width&&mouseY>=this.y&&mouseY<this.y+this.height;
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