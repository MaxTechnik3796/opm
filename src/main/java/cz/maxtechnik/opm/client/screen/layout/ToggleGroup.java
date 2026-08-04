package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.widget.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ToggleGroup {
	private final String[] labels;
	private final Supplier<Integer> selectedSupplier;
	private final Consumer<Integer> onSelect;
	private final int[] customColors;

	private int anchorX;
	private int anchorY;

	public ToggleGroup(String[] labels, Supplier<Integer> selectedSupplier, Consumer<Integer> onSelect, int[] customColors) {
		this.labels = labels;
		this.selectedSupplier = selectedSupplier;
		this.onSelect = onSelect;
		this.customColors = customColors;
	}

	public static ToggleGroup of(String[] labels, Supplier<Integer> selectedSupplier, Consumer<Integer> onSelect) {
		return new ToggleGroup(labels, selectedSupplier, onSelect, null);
	}

	public static ToggleGroup colored(String[] labels, Supplier<Integer> selectedSupplier, Consumer<Integer> onSelect, int[] colors) {
		return new ToggleGroup(labels, selectedSupplier, onSelect, colors);
	}

	public void setAnchor(int x, int y) {
		this.anchorX = x;
		this.anchorY = y;
	}

	public int getSelectedIndex() {
		return selectedSupplier != null ? selectedSupplier.get() : 0;
	}

	public void select(int index) {
		if (onSelect != null && index >= 0 && index < labels.length) {
			onSelect.accept(index);
		}
	}

	public String[] getLabels() { return labels; }
	public int[] getCustomColors() { return customColors; }
	public int getAnchorX() { return anchorX; }
	public int getAnchorY() { return anchorY; }

	public int getTotalWidth(Font font) {
		int tw = 0;
		for (String l : labels) tw += font.width(l) + 16;
		return tw;
	}

	public void render(GuiGraphics g, Font font, int mx, int my) {
		int tw = getTotalWidth(font);
		int bx = anchorX - tw / 2;
		int selIdx = getSelectedIndex();

		for (int i = 0; i < labels.length; i++) {
			int bw = font.width(labels[i]) + 10;
			boolean sel = selIdx == i;
			boolean hov = UiKit.hit(mx, my, bx, anchorY, bw, 16);

			int bg;
			if (customColors != null && i < customColors.length) {
				bg = sel ? -11908486 : (hov ? customColors[i] + 0x111100 : customColors[i]);
			} else {
				bg = sel ? UiKit.C_TAB_SEL : (hov ? UiKit.C_BTN_H : UiKit.C_BTN);
			}

			g.fill(bx, anchorY, bx + bw, anchorY + 16, bg);
			g.drawCenteredString(font, labels[i], bx + bw / 2, anchorY + 4, sel ? (customColors != null ? -13176 : 0xFFCCCCFF) : UiKit.C_TEXT);
			bx += bw + 6;
		}
	}

	public boolean handleClick(int mx, int my, Font font) {
		int tw = getTotalWidth(font);
		int bx = anchorX - tw / 2;
		for (int i = 0; i < labels.length; i++) {
			int bw = font.width(labels[i]) + 10;
			if (UiKit.hit(mx, my, bx, anchorY, bw, 16)) {
				select(i);
				return true;
			}
			bx += bw + 6;
		}
		return false;
	}
}
