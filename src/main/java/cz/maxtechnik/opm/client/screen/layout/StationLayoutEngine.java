package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
import cz.maxtechnik.opm.client.util.ScaleHelper;
import cz.maxtechnik.opm.client.widget.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Modul pro vykreslování stanic a určování jejich rozměrů a pozic na obrazovce.
 */
public final class StationLayoutEngine {

	public interface EditCallback extends StationInteractionHandler.EditCallback {}

	private StationLayoutEngine() {}

	// ─── SHODA A DELEGACE ───────────────────────────────────────────────

	public static StationLayout getLayout(StationType type, RecipeEditorData d) {
		return StationLayoutFactory.getLayout(type, d);
	}

	public static List<ItemStack> getItemListForGroup(RecipeEditorData d, StationType type, boolean isInput) {
		return StationItemBridge.getItemListForGroup(d, type, isInput);
	}

	public static List<CrushingOutput> getCrushingOutputsForGroup(RecipeEditorData d, StationType type) {
		return StationItemBridge.getCrushingOutputsForGroup(d, type);
	}

	public static ItemStack getResultItem(RecipeEditorData d, StationType type) {
		return StationItemBridge.getResultItem(d, type);
	}

	public static List<FluidEntry> getFluidInputs(RecipeEditorData d, StationType type) {
		return StationItemBridge.getFluidInputs(d, type);
	}

	public static List<FluidEntry> getFluidOutputs(RecipeEditorData d, StationType type) {
		return StationItemBridge.getFluidOutputs(d, type);
	}

	public static int getResultCount(RecipeEditorData d, StationType type) {
		return StationItemBridge.getResultCount(d, type);
	}

	public static void setResultCount(RecipeEditorData d, StationType type, int count) {
		StationItemBridge.setResultCount(d, type, count);
	}

	public static void setInputItem(RecipeEditorData d, StationType type, int idx, ItemStack s) {
		StationItemBridge.setInputItem(d, type, idx, s);
	}

	public static void setOutputItem(RecipeEditorData d, StationType type, ItemStack s) {
		StationItemBridge.setOutputItem(d, type, s);
	}


	// ─── OBSLUHA INTERAKCÍ (DELEGACE) ───────────────────────────────────

	public static boolean handleHeaderClicks(StationType type, RecipeEditorData d, int cx, int editorY, int mx, int my) {
		return StationInteractionHandler.handleHeaderClicks(type, d, cx, editorY, mx, my);
	}

	public static boolean handleSpinnerClicks(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		return StationInteractionHandler.handleSpinnerClicks(type, d, cx, leftWidth, editorY, mx, my);
	}

	public static boolean handleFluidSpins(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		return StationInteractionHandler.handleFluidSpins(type, d, cx, leftWidth, editorY, mx, my);
	}

	public static boolean handleScrollSpinners(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, double delta) {
		return StationInteractionHandler.handleScrollSpinners(type, d, cx, leftWidth, editorY, mx, my, delta);
	}

	public static boolean handleDoubleClick(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, StationInteractionHandler.EditCallback callback) {
		return StationInteractionHandler.handleDoubleClick(type, d, cx, leftWidth, editorY, mx, my, callback);
	}

	// ─── VÝPOČTY ROZMĚRŮ & ŠKÁLOVÁNÍ ──────────────────────────────────

	public static float getScale(StationType type, StationLayout layout, int leftWidth) {
		return getScale(type, null, layout, leftWidth);
	}

	public static float getScale(StationType type, RecipeEditorData d, StationLayout layout, int leftWidth) {
		int totalWidth = getLayoutTotalWidth(layout);
		return ScaleHelper.getStationScale(totalWidth, leftWidth - 24);
	}

	public static int getContentY(int editorTop) {
		return editorTop + 24;
	}

	public static int getContentY(StationLayout layout, int editorTop) {
		return getContentY(null, layout, editorTop);
	}

	public static int getContentY(StationType type, StationLayout layout, int editorTop) {
		int y = editorTop + 24;
		if (layout.getHeaderToggle() != null) y += 20;
		if (layout.getSubToggle() != null) y += 20;
		return y;
	}


	public static int getStartX(StationType type, StationLayout layout, int cx) {
		return getStartX(type, null, layout, cx);
	}

	public static int getStartX(StationType type, RecipeEditorData d, StationLayout layout, int cx) {
		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		int totalW = getLayoutTotalWidth(layout);
		if (type == StationType.FILLING) return cx - 80;
		if (inG != null && outG != null) return cx - totalW / 2;
		return cx - (inG != null ? inG.getWidth() : 0) / 2;
	}

	public static int getLayoutTotalWidth(StationLayout layout) {
		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		if (inG == null && outG == null) return 0;
		if (inG != null && outG == null) return inG.getWidth();
		if (inG == null) return outG.getWidth();

		int extraW = inG.getSpec().hasCount() ? 24 : 0;
		return inG.getWidth() + extraW + 40 + outG.getWidth();
	}

	// ─── VYKRESLOVÁNÍ STANICE ──────────────────────────────────────────

	public static int renderStation(GuiGraphics g, net.minecraft.client.gui.Font font, StationType type, RecipeEditorData d, int panelX, int leftWidth, int editorY, int mx, int my, boolean isDragging) {
		StationLayout layout = getLayout(type, d);
		int cx = panelX + leftWidth / 2;
		int hY = editorY + 4;

		ToggleGroup hToggle = layout.getHeaderToggle();
		if (hToggle != null) {
			int hW = hToggle.calcWidth(font);
			hToggle.render(g, font, cx - hW / 2, hY, mx, my);
		}
		ToggleGroup subToggle = layout.getSubToggle();
		if (subToggle != null) {
			int sW = subToggle.calcWidth(font);
			subToggle.render(g, font, cx - sW / 2, hY + 16, mx, my);
		}

		float scale = getScale(type, d, layout, leftWidth);
		int contentY = getContentY(type, layout, editorY);

		boolean scaled = ScaleHelper.pushPoseScale(g, scale, cx, contentY);
		if (scaled) {
			mx = ScaleHelper.transformMouseX(mx, cx, scale);
			my = ScaleHelper.transformMouseY(my, contentY, scale);
		}

		SlotGroup inputGroup  = layout.getInputSlots();
		SlotGroup outputGroup = layout.getOutputSlots();
		List<ItemStack> inputItems = getItemListForGroup(d, type, true);

		if (type == StationType.FILLING) {
			int startX = getStartX(type, d, layout, cx);
			UiKit.slot(g, font, mx, my, inputItems.isEmpty() ? ItemStack.EMPTY : inputItems.get(0), startX, contentY, UiKit.C_SLOT, isDragging);
			g.drawString(font, "+", startX + 22, contentY + 4, UiKit.C_LABEL, false);
			UiKit.slotFluid(g, font, mx, my, d.fillFluid, startX + 34, contentY, isDragging);
			g.drawString(font, "->", startX + 130, contentY + 4, UiKit.C_LABEL, false);
			UiKit.slot(g, font, mx, my, d.fillResult, startX + 147, contentY, d.fillResult.isEmpty() ? UiKit.C_SLOT : UiKit.C_SLOT_RES, isDragging);

			ScaleHelper.popPoseScale(g, scaled);
			return contentY;
		}


		if (outputGroup != null) {
			int startX = getStartX(type, d, layout, cx);
			inputGroup.setAnchor(startX, contentY);

			int extraW = inputGroup.getSpec().hasCount() ? 24 : 0;
			int arrowX = startX + inputGroup.getWidth() + extraW + 15;
			int outputX = arrowX + 25;
			int outputY = (inputGroup.getHeight() > outputGroup.getHeight()) ? (contentY + inputGroup.getHeight() / 2 - outputGroup.getHeight() / 2) : contentY;
			outputGroup.setAnchor(outputX, outputY);

			g.drawString(font, "->", arrowX, contentY + Math.max(inputGroup.getHeight(), outputGroup.getHeight()) / 2 - 4, UiKit.C_LABEL, false);

			renderSlotGroupItems(g, font, inputGroup, inputItems, mx, my, isDragging);

			List<CrushingOutput> crushOutputs = getCrushingOutputsForGroup(d, type);
			if (crushOutputs != null) {
				renderSlotGroupOutputs(g, font, outputGroup, crushOutputs, mx, my, isDragging);
			} else {
				ItemStack resItem = getResultItem(d, type);
				UiKit.slot(g, font, mx, my, resItem, outputX, outputY, resItem.isEmpty() ? UiKit.C_SLOT : UiKit.C_SLOT_RES, isDragging);
				if (outputGroup.getSpec().hasCount()) {
					int resCount = StationItemBridge.getResultCount(d, type);
					UiKit.spinner(g, font, mx, my, outputX + UiKit.SS + 4, outputY + 2, resCount);
				}
				if (outputGroup.getLabel() != null) {
					g.drawString(font, outputGroup.getLabel(), outputX, outputY - 10, UiKit.C_LABEL, false);
				}
			}
		} else if (inputGroup != null) {
			inputGroup.setAnchor(cx - inputGroup.getWidth() / 2, contentY);
			renderSlotGroupItems(g, font, inputGroup, inputItems, mx, my, isDragging);
		}

		SlotGroup inputFluids  = layout.getInputFluids();
		SlotGroup outputFluids = layout.getOutputFluids();
		if (inputFluids != null || outputFluids != null) {
			int itemAreaH = inputGroup != null ? (outputGroup != null ? Math.max(inputGroup.getHeight(), outputGroup.getHeight()) : inputGroup.getHeight()) : 0;
			int fluidY = contentY + itemAreaH + 15 + (type == StationType.MIXING ? 2 : 0);

			int sx = cx - 150;
			int rx = cx + 10;
			if (inputFluids != null) {
				inputFluids.setAnchor(sx, fluidY);
				if (inputFluids.getLabel() != null) g.drawString(font, inputFluids.getLabel(), sx, fluidY - 10, UiKit.C_LABEL, false);
				var fInputs = getFluidInputs(d, type);
				for (int i = 0; i < inputFluids.getTotalSlots() && i < fInputs.size(); i++) {
					UiKit.slotFluid(g, font, mx, my, fInputs.get(i), inputFluids.getSlotX(i), inputFluids.getSlotY(i), isDragging);
				}
			}
			if (outputFluids != null) {
				outputFluids.setAnchor(rx, fluidY);
				if (outputFluids.getLabel() != null) g.drawString(font, outputFluids.getLabel(), rx, fluidY - 10, UiKit.C_LABEL, false);
				var fOutputs = getFluidOutputs(d, type);
				for (int i = 0; i < outputFluids.getTotalSlots() && i < fOutputs.size(); i++) {
					UiKit.slotFluid(g, font, mx, my, fOutputs.get(i), outputFluids.getSlotX(i), outputFluids.getSlotY(i), isDragging);
				}
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tValX = cx - 15, spinX = cx + 45;
			g.drawString(font, "Time:", cx - 50, contentY + 4, UiKit.C_LABEL, false);
			g.drawString(font, pTime.getValue() + " t", tValX, contentY + 4, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, spinX, contentY + 2);
		}

		if (type == StationType.FURNACE) {
			int xpX = cx - 45, spinX = cx - 20;
			g.drawString(font, "XP:", cx - 70, contentY + 4, UiKit.C_LABEL, false);
			g.drawString(font, String.format(java.util.Locale.ROOT, "%.1f", d.furnXp), xpX, contentY + 4, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, spinX, contentY + 2);
		}

		ScaleHelper.popPoseScale(g, scaled);
		return contentY;
	}


	private static void renderSlotGroupItems(GuiGraphics g, net.minecraft.client.gui.Font font, SlotGroup group, List<ItemStack> items, int mx, int my, boolean isDragging) {
		for (int i = 0; i < group.getTotalSlots(); i++) {
			int sx = group.getSlotX(i);
			int sy = group.getSlotY(i);
			ItemStack stack = (items != null && i < items.size()) ? items.get(i) : ItemStack.EMPTY;
			UiKit.slot(g, font, mx, my, stack, sx, sy, UiKit.C_SLOT, isDragging);

			if (group.getSpec().hasCount()) {
				int cpx = sx + UiKit.SS + 4, cpy = sy + 2;
				int displayCount = stack.isEmpty() ? 1 : stack.getCount();
				UiKit.spinner(g, font, mx, my, cpx, cpy, displayCount);
			}

			if (group.getSeparatorSymbol() != null && i < group.getTotalSlots() - 1) {
				int nextSx = group.getSlotX(i + 1);
				int sepX = (sx + UiKit.SS + nextSx) / 2 - font.width(group.getSeparatorSymbol()) / 2;
				g.drawString(font, group.getSeparatorSymbol(), sepX, sy + 4, UiKit.C_LABEL, false);
			}
		}
	}

	private static void renderSlotGroupOutputs(GuiGraphics g, net.minecraft.client.gui.Font font, SlotGroup group, List<CrushingOutput> outputs, int mx, int my, boolean isDragging) {
		for (int i = 0; i < group.getTotalSlots() && i < outputs.size(); i++) {
			int ox = group.getSlotX(i);
			int oy = group.getSlotY(i);
			CrushingOutput co = outputs.get(i);
			UiKit.slot(g, font, mx, my, co.stack, ox, oy, co.isEmpty() ? UiKit.C_SLOT : UiKit.C_SLOT_RES, isDragging);

			int cpx = ox + UiKit.SS + 4, cpy = oy + 2;
			UiKit.spinner(g, font, mx, my, cpx, cpy, co.count);

			if (group.getSpec().hasChance()) {
				int chX = cpx + 28;
				String chStr = co.chance >= 1f ? "100%" : Math.round(co.chance * 100) + "%";
				UiKit.drawMiniSpinner(g, font, mx, my, chX, cpy - 2);
				g.drawString(font, chStr, chX + 14, cpy + 3, co.isEmpty() ? UiKit.C_LABEL : 0xFFAAFF88, false);
			}
		}
	}
}