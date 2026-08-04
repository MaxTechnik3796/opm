package cz.maxtechnik.opm.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Pokročilý modul pro manipulaci s předměty v editoru (pick, place, sticky cursor, Ctrl duplikace a Drag-Paint).
 */
public final class ItemDragHandler {
	private ItemStack dragStack = ItemStack.EMPTY;
	private int lastPaintedSlotIndex = -1;

	public boolean hasStack() {
		return !dragStack.isEmpty();
	}

	public ItemStack getStack() {
		return dragStack;
	}


	public void pick(ItemStack stack) {
		if (stack != null && !stack.isEmpty()) {
			this.dragStack = stack.copy();
			this.lastPaintedSlotIndex = -1;
		}
	}

	public void pickFromSlot(ItemStack stack, int slotIndex) {
		if (stack != null && !stack.isEmpty()) {
			this.dragStack = stack.copy();
			this.lastPaintedSlotIndex = slotIndex;
		}
	}


	public void clear() {
		this.dragStack = ItemStack.EMPTY;
		this.lastPaintedSlotIndex = -1;
	}

	/**
	 * Obsluhuje kliknutí na slot editoru podle nastavených kláves (Ctrl / RightClick).
	 */
	public void handleSlotClick(ItemStack slotCurrentItem, Consumer<ItemStack> slotSetter, boolean hasCtrl, boolean isRightClick) {
		if (hasStack()) {
			if (isRightClick) {
				slotSetter.accept(ItemStack.EMPTY);
				return;
			}
			slotSetter.accept(dragStack.copy());
			if (!hasCtrl) {
				clear();
			}
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
			}
		}
	}

	/**
	 * Obsluhuje malování předmětu přes sloty při tažení myší (Drag-Paint).
	 */
	public void paintSlot(int slotIndex, Consumer<ItemStack> slotSetter) {
		if (!hasStack()) return;
		if (slotIndex != lastPaintedSlotIndex) {
			lastPaintedSlotIndex = slotIndex;
			slotSetter.accept(dragStack.copy());
		}
	}

	public void resetPaintIndex() {
		this.lastPaintedSlotIndex = -1;
	}

	/**
	 * Obsluhuje mazání předmětu přes sloty při tažení myší s Ctrl/RMB (Drag-Erase).
	 */
	public void eraseSlot(int slotIndex, Consumer<ItemStack> slotSetter) {
		if (slotIndex != lastPaintedSlotIndex) {
			lastPaintedSlotIndex = slotIndex;
			slotSetter.accept(ItemStack.EMPTY);
		}
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
