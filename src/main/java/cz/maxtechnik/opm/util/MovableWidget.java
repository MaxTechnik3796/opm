package cz.maxtechnik.opm.util;

import cz.maxtechnik.opm.config.AnchorX;
import cz.maxtechnik.opm.config.AnchorY;
import net.minecraft.client.gui.Font;
public interface MovableWidget{
	SidelistWidget.ScoreboardBounds getBounds(Font font,int screenWidth,int screenHeight);
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