package cz.maxtechnik.opm.client.screen.config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Rozhraní reprezentující konfigurovatelný HUD prvek na obrazovce.
 * Každý prvek obsluhuje vlastní pozici, měřítko, náhled a specifické vlastnosti v bočním panelu.
 */
public interface HudElement {

	String id();

	String title();

	String icon();

	boolean isEnabled();

	void setEnabled(boolean enabled);

	int getX(int screenW, int screenH);

	int getY(int screenW, int screenH);

	int getW();

	int getH();

	double getScale();

	void setScale(double scale);

	void adjustScale(double delta);

	void onDrag(int mx, int my, int grabX, int grabY, int screenW, int screenH);

	void clamp(int screenW, int screenH);

	void reset();

	void save();

	void renderPreview(GuiGraphics g, Font font, int screenW, int screenH, boolean hovered, boolean selected, boolean dragging);

	void renderInspector(GuiGraphics g, Font font, int x, int y, int w, int mx, int my);

	boolean handleInspectorClick(int mx, int my, int x, int y, int w);
}
