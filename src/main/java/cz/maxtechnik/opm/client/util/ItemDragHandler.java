package cz.maxtechnik.opm.client.util;

import cz.maxtechnik.opm.client.screen.EditorRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Pokročilý modul pro manipulaci s předměty v editoru (pick, place, sticky cursor, Ctrl duplikace a Drag-Paint).
 */
public final class ItemDragHandler {
	private ItemStack dragStack = ItemStack.EMPTY;
	private boolean isDragging = false;
	private int lastPaintedSlotIndex = -1;

	public boolean hasStack() {
		return !dragStack.isEmpty();
	}

	public ItemStack getDragStack() {
		return dragStack;
	}

	public boolean isDragging() {
		return isDragging;
	}

	public void setDragging(boolean dragging) {
		this.isDragging = dragging;
	}

	public void pick(ItemStack stack) {
		if (stack != null && !stack.isEmpty()) {
			this.dragStack = stack.copy();
			this.isDragging = true;
			this.lastPaintedSlotIndex = -1;
		}
	}

	public void clear() {
		this.dragStack = ItemStack.EMPTY;
		this.isDragging = false;
		this.lastPaintedSlotIndex = -1;
	}

	/**
	 * Obsluhuje kliknutí na slot editoru podle nastavených kláves (Ctrl / RightClick).
	 */
	public boolean handleSlotClick(ItemStack slotCurrentItem, java.util.function.Consumer<ItemStack> slotSetter, boolean hasCtrl, boolean isRightClick) {
		if (hasStack()) {
			if (isRightClick) {
				slotSetter.accept(ItemStack.EMPTY);
				return true;
			}
			// Vloží předmět do slotu
			slotSetter.accept(dragStack.copy());
			if (!hasCtrl) {
				clear();
			}
			return true;
		} else {
			if (!slotCurrentItem.isEmpty()) {
				if (isRightClick) {
					slotSetter.accept(ItemStack.EMPTY);
				} else if (hasCtrl) {
					pick(slotCurrentItem);
				} else {
					pick(slotCurrentItem);
					slotSetter.accept(ItemStack.EMPTY);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Obsluhuje malování předmětu přes sloty při tažení myší (Drag-Paint).
	 */
	public boolean paintSlot(int slotIndex, java.util.function.Consumer<ItemStack> slotSetter) {
		if (!hasStack()) return false;
		if (slotIndex != lastPaintedSlotIndex) {
			lastPaintedSlotIndex = slotIndex;
			slotSetter.accept(dragStack.copy());
			return true;
		}
		return false;
	}

	public void resetPaintIndex() {
		this.lastPaintedSlotIndex = -1;
	}

	public void render(GuiGraphics g, Font font, int mx, int my) {
		if (hasStack()) {
			var pose = g.pose();
			pose.pushPose();
			pose.translate(0, 0, 300);
			g.renderItem(dragStack, mx - 8, my - 8);
			g.renderItemDecorations(font, dragStack, mx - 8, my - 8);
			pose.popPose();
		}
	}
}
