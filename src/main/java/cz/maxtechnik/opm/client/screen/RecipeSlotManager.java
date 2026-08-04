package cz.maxtechnik.opm.client.screen;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.screen.layout.SlotGroup;
import cz.maxtechnik.opm.client.screen.layout.StationLayoutEngine;
import cz.maxtechnik.opm.client.widget.BottomInventoryPanel;
import cz.maxtechnik.opm.client.widget.UiKit;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RecipeSlotManager {

	/** Pozice jednoho interaktivního slotu receptu – jeho souřadnice, getter a setter itemu. */
	public record SlotPos(int x, int y, int size, Supplier<ItemStack> get, Consumer<ItemStack> set) {}

	/** Vrátí seznam všech interaktivních slotů pro danou stanici a layout. */
	public static List<SlotPos> getItemSlots(StationType station, RecipeEditorData data, int panelX, int leftWidth, int editorTop) {
		List<SlotPos> slots = new ArrayList<>();
		int centerX = panelX + leftWidth / 2;
		var layout = StationLayoutEngine.getLayout(station, data);
		int contentY = StationLayoutEngine.getContentY(station, layout, editorTop);

		SlotGroup inputGroup  = layout.getInputSlots();
		SlotGroup outputGroup = layout.getOutputSlots();

		List<ItemStack> inputItems = StationLayoutEngine.getItemListForGroup(data, station, true);

		if (station == StationType.FILLING) {
			int startX = StationLayoutEngine.getStartX(station, data, layout, centerX);
			slots.add(new SlotPos(startX, contentY, UiKit.SS, () -> inputItems.isEmpty() ? ItemStack.EMPTY : inputItems.get(0), s -> {
				if (inputItems.isEmpty()) inputItems.add(s);
				else inputItems.set(0, s);
			}));

			slots.add(new SlotPos(startX + 34, contentY, UiKit.SS, () -> data.fillFluid.proxy, s -> {
				data.fillFluid.proxy = s.isEmpty() ? ItemStack.EMPTY : s.copy();
				if (!data.fillFluid.proxy.isEmpty()) data.fillFluid.proxy.setCount(1);
			}));
			slots.add(new SlotPos(startX + 147, contentY, UiKit.SS, () -> StationLayoutEngine.getResultItem(data, station), s -> StationLayoutEngine.setOutputItem(data, station, s)));

			return slots;
		}

		if (outputGroup != null) {

			int startX = StationLayoutEngine.getStartX(station, data, layout, centerX);
			inputGroup.setAnchor(startX, contentY);

			int extraW = inputGroup.getSpec().hasCount() ? 24 : 0;
			int arrowX = startX + inputGroup.getWidth() + extraW + 15;
			int outputX = arrowX + 25;
			int outputY = (inputGroup.getHeight() > outputGroup.getHeight()) ? (contentY + inputGroup.getHeight() / 2 - outputGroup.getHeight() / 2) : contentY;
			outputGroup.setAnchor(outputX, outputY);



			addInputSlots(slots, inputGroup, inputItems, station, data);

			List<StationType.CrushingOutput> crushOutputs = StationLayoutEngine.getCrushingOutputsForGroup(data, station);
			if (crushOutputs != null) {
				for (int i = 0; i < outputGroup.getTotalSlots() && i < crushOutputs.size(); i++) {
					int idx = i;
					slots.add(new SlotPos(outputGroup.getSlotX(i), outputGroup.getSlotY(i), UiKit.SS, () -> crushOutputs.get(idx).stack, s -> crushOutputs.get(idx).stack = s));
				}
			} else {
				slots.add(new SlotPos(outputX, outputGroup.getAnchorY(), UiKit.SS, () -> StationLayoutEngine.getResultItem(data, station), s -> StationLayoutEngine.setOutputItem(data, station, s)));
			}
		} else {
			inputGroup.setAnchor(centerX - inputGroup.getWidth() / 2, contentY);
			addInputSlots(slots, inputGroup, inputItems, station, data);
		}

		SlotGroup inputFluids  = layout.getInputFluids();
		SlotGroup outputFluids = layout.getOutputFluids();
		if (inputFluids != null || outputFluids != null) {
			int itemAreaH = inputGroup != null ? (outputGroup != null ? Math.max(inputGroup.getHeight(), outputGroup.getHeight()) : inputGroup.getHeight()) : 0;
			int fluidY = contentY + itemAreaH + 15 + (station == StationType.MIXING ? 2 : 0);

			int sx = centerX - 150;
			int rx = centerX + 10;
			if (inputFluids != null) {
				inputFluids.setAnchor(sx, fluidY);
				var fInputs = StationLayoutEngine.getFluidInputs(data, station);
				for (int i = 0; i < inputFluids.getTotalSlots() && i < fInputs.size(); i++) {
					int idx = i;
					slots.add(new SlotPos(inputFluids.getSlotX(i), inputFluids.getSlotY(i), UiKit.SS,
							() -> fInputs.get(idx).proxy,
							s -> {
								fInputs.get(idx).proxy = s.isEmpty() ? ItemStack.EMPTY : s.copy();
								if (!fInputs.get(idx).proxy.isEmpty()) fInputs.get(idx).proxy.setCount(1);
							}));
				}
			}
			if (outputFluids != null) {
				outputFluids.setAnchor(rx, fluidY);
				var fOutputs = StationLayoutEngine.getFluidOutputs(data, station);
				for (int i = 0; i < outputFluids.getTotalSlots() && i < fOutputs.size(); i++) {
					int idx = i;
					slots.add(new SlotPos(outputFluids.getSlotX(i), outputFluids.getSlotY(i), UiKit.SS,
							() -> fOutputs.get(idx).proxy,
							s -> {
								fOutputs.get(idx).proxy = s.isEmpty() ? ItemStack.EMPTY : s.copy();
								if (!fOutputs.get(idx).proxy.isEmpty()) fOutputs.get(idx).proxy.setCount(1);
							}));
				}
			}
		}

		return slots;
	}


	private static void addInputSlots(List<SlotPos> slots, SlotGroup inputGroup, List<ItemStack> inputItems, StationType station, RecipeEditorData data) {
		for (int i = 0; i < inputGroup.getTotalSlots(); i++) {
			int idx = i;
			slots.add(new SlotPos(
					inputGroup.getSlotX(i), inputGroup.getSlotY(i), inputGroup.getSlotSize(),
					() -> idx < inputItems.size() ? inputItems.get(idx) : ItemStack.EMPTY,
					s -> StationLayoutEngine.setInputItem(data, station, idx, s)
			));
		}
	}

	/** Vrátí ItemStack na pozici myši – nejprve hledá ve slotech editoru, pak v inventáři. */
	public static ItemStack getSlotItemAt(StationType station, RecipeEditorData data, int panelX, int leftWidth, int editorTop, int inventoryTop, float scroll, int mx, int my, BottomInventoryPanel bottomPanel, int panelH) {
		if (my >= editorTop && my < inventoryTop - 20 && mx >= panelX && mx < panelX + leftWidth) {
			int scrolledY = (int) (my + scroll);
			for (SlotPos slot : getItemSlots(station, data, panelX, leftWidth, editorTop)) {
				if (mx >= slot.x() && mx <= slot.x() + slot.size() && scrolledY >= slot.y() && scrolledY <= slot.y() + slot.size()) {
					return slot.get().get();
				}
			}
		}
		return bottomPanel != null ? bottomPanel.itemAt(panelX, panelH, inventoryTop, mx, my) : ItemStack.EMPTY;
	}
}
