package cz.maxtechnik.opm.client.editor;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.editor.layout.SlotGroup;
import cz.maxtechnik.opm.client.editor.layout.StationLayoutEngine;
import cz.maxtechnik.opm.client.editor.BottomInventoryPanel;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RecipeSlotManager {

	public record SlotPos(int x, int y, int size, Supplier<ItemStack> get, Consumer<ItemStack> set) {
		public boolean contains(int mx, int my, int cx, int cy, float scale) {
			int smx = (scale < 0.99f && scale > 0) ? (int) (cx + (mx - cx) / scale) : mx;
			int smy = (scale < 0.99f && scale > 0) ? (int) (cy + (my - cy) / scale) : my;
			return smx >= x && smx <= x + size && smy >= y && smy <= y + size;
		}
	}

	/** Vrátí seznam všech interaktivních slotů pro danou stanici a layout. */
	public static List<SlotPos> getItemSlots(StationType station, RecipeEditorData data, int panelX, int leftWidth, int editorTop) {
		List<SlotPos> slots = new ArrayList<>();
		int centerX = panelX + leftWidth / 2;
		var layout = StationLayoutEngine.getLayout(station, data);
		int contentY = StationLayoutEngine.getContentY(station, layout, editorTop);

		StationLayoutEngine.setupLayoutAnchors(layout, station, data, centerX, contentY);

		SlotGroup inputGroup  = layout.getInputSlots();
		SlotGroup outputGroup = layout.getOutputSlots();
		List<ItemStack> inputItems = StationLayoutEngine.getItemListForGroup(data, station, true);

		if (station == StationType.FILLING) {
			slots.add(new SlotPos(inputGroup.getAnchorX(), contentY, UiKit.SS,
					() -> data.fillIn,
					s -> StationLayoutEngine.setInputItem(data, station, 0, s)));

			slots.add(new SlotPos(layout.getInputFluids().getAnchorX(), contentY, UiKit.SS,
					() -> data.fillFluid.proxy,
					s -> {
						if (s.isEmpty() || RecipeEditorData.isFluidOrTag(s)) {
							data.fillFluid.proxy = s.isEmpty() ? ItemStack.EMPTY : s.copy();
							if (!data.fillFluid.proxy.isEmpty()) data.fillFluid.proxy.setCount(1);
						}
					}));

			slots.add(new SlotPos(outputGroup.getAnchorX(), contentY, UiKit.SS,
					() -> StationLayoutEngine.getResultItem(data, station),
					s -> StationLayoutEngine.setOutputItem(data, station, s)));

			return slots;
		}

		if (inputGroup != null) {
			addInputSlots(slots, inputGroup, inputItems, station, data);

			if (outputGroup != null) {
				List<StationType.CrushingOutput> crushOutputs = StationLayoutEngine.getCrushingOutputsForGroup(data, station);
				if (crushOutputs != null) {
					for (int i = 0; i < outputGroup.getTotalSlots() && i < crushOutputs.size(); i++) {
						int idx = i;
						slots.add(new SlotPos(outputGroup.getSlotX(i), outputGroup.getSlotY(i), UiKit.SS,
								() -> crushOutputs.get(idx).stack,
								s -> crushOutputs.get(idx).stack = s));
					}
				} else {
					slots.add(new SlotPos(outputGroup.getAnchorX(), outputGroup.getAnchorY(), UiKit.SS,
							() -> StationLayoutEngine.getResultItem(data, station),
							s -> StationLayoutEngine.setOutputItem(data, station, s)));
				}
			}
		}

		SlotGroup inputFluids  = layout.getInputFluids();
		SlotGroup outputFluids = layout.getOutputFluids();

		if (inputFluids != null) {
			var fInputs = StationLayoutEngine.getFluidInputs(data, station);
			for (int i = 0; i < inputFluids.getTotalSlots() && i < fInputs.size(); i++) {
				int idx = i;
				slots.add(new SlotPos(inputFluids.getSlotX(i), inputFluids.getSlotY(i), UiKit.SS,
						() -> fInputs.get(idx).proxy,
						s -> {
							if (s.isEmpty() || RecipeEditorData.isFluidOrTag(s)) {
								fInputs.get(idx).proxy = s.isEmpty() ? ItemStack.EMPTY : s.copy();
								if (!fInputs.get(idx).proxy.isEmpty()) fInputs.get(idx).proxy.setCount(1);
							}
						}));
			}
		}

		if (outputFluids != null) {
			var fOutputs = StationLayoutEngine.getFluidOutputs(data, station);
			for (int i = 0; i < outputFluids.getTotalSlots() && i < fOutputs.size(); i++) {
				int idx = i;
				slots.add(new SlotPos(outputFluids.getSlotX(i), outputFluids.getSlotY(i), UiKit.SS,
						() -> fOutputs.get(idx).proxy,
						s -> {
							if (s.isEmpty() || RecipeEditorData.isFluidItem(s)) {
								fOutputs.get(idx).proxy = s.isEmpty() ? ItemStack.EMPTY : s.copy();
								if (!fOutputs.get(idx).proxy.isEmpty()) fOutputs.get(idx).proxy.setCount(1);
							}
						}));
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
			var layout = StationLayoutEngine.getLayout(station, data);
			int cx = panelX + leftWidth / 2;
			int cy = StationLayoutEngine.getContentY(station, layout, editorTop);
			float scale = StationLayoutEngine.getScale(station, data, layout, leftWidth);
			for (SlotPos slot : getItemSlots(station, data, panelX, leftWidth, editorTop)) {
				if (slot.contains(mx, scrolledY, cx, cy, scale)) {
					return slot.get().get();
				}
			}
		}
		return bottomPanel != null ? bottomPanel.itemAt(panelX, panelH, leftWidth, inventoryTop, mx, my) : ItemStack.EMPTY;
	}
}
