package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.widget.UiKit;

public class SlotGroup {
	private final int cols;
	private final int rows;
	private final int slotSize;
	private final int padX;
	private final int padY;
	private final SlotSpec spec;
	private final String label;
	private final String separatorSymbol;

	private int anchorX;
	private int anchorY;

	public SlotGroup(int cols, int rows, int slotSize, int padX, int padY, SlotSpec spec, String label) {
		this(cols, rows, slotSize, padX, padY, spec, label, null);
	}

	public SlotGroup(int cols, int rows, int slotSize, int padX, int padY, SlotSpec spec, String label, String separatorSymbol) {
		this.cols = cols;
		this.rows = rows;
		this.slotSize = slotSize;
		this.padX = padX;
		this.padY = padY;
		this.spec = spec;
		this.label = label;
		this.separatorSymbol = separatorSymbol;
	}

	public static SlotGroup single(SlotSpec spec) {
		return grid(1, 1, spec);
	}

	public static SlotGroup single(SlotSpec spec, String label) {
		return grid(1, 1, UiKit.SS, UiKit.SP, UiKit.SP, spec, label);
	}

	public static SlotGroup row(int count, SlotSpec spec) {
		return grid(count, 1, spec);
	}

	public static SlotGroup grid(int cols, int rows, SlotSpec spec) {
		int extraX = spec.hasChance() ? 65 : spec.hasCount() ? 32 : spec.isFluid() ? 60 : UiKit.SP;
		int extraY = spec.isFluid() ? 10 : UiKit.SP;
		return new SlotGroup(cols, rows, UiKit.SS, extraX, extraY, spec, null);
	}

	public static SlotGroup grid(int cols, int rows, int slotSize, int padX, int padY, SlotSpec spec, String label) {
		return new SlotGroup(cols, rows, slotSize, padX, padY, spec, label);
	}

	public SlotGroup withSeparator(String symbol) {
		int newPadX = (symbol != null) ? 18 : this.padX;
		return new SlotGroup(cols, rows, slotSize, newPadX, padY, spec, label, symbol);
	}

	public void setAnchor(int x, int y) {
		this.anchorX = x;
		this.anchorY = y;
	}

	public int getWidth() {
		return cols * (slotSize + padX) - padX;
	}

	public int getHeight() {
		return rows * (slotSize + padY) - padY;
	}

	public int getSlotX(int index) {
		int col = index % cols;
		return anchorX + col * (slotSize + padX);
	}

	public int getSlotY(int index) {
		int row = index / cols;
		return anchorY + row * (slotSize + padY);
	}

	public int getCols() { return cols; }
	public int getRows() { return rows; }
	public int getTotalSlots() { return cols * rows; }
	public int getSlotSize() { return slotSize; }
	public SlotSpec getSpec() { return spec; }
	public String getLabel() { return label; }
	public String getSeparatorSymbol() { return separatorSymbol; }
	public int getAnchorX() { return anchorX; }
	public int getAnchorY() { return anchorY; }
}
