package cz.maxtechnik.opm.client.editor.layout;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.editor.RecipeEditorData;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public class StationLayoutEngine {
	private StationLayoutEngine() {}

	public interface EditCallback {
		void edit(String fieldName, int bx, int by, int bw, String val, int idx);
	}

	public static StationLayout getLayout(StationType type, RecipeEditorData d) {
		return switch (type) {
			case CRAFTING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Shaped", "Shapeless"}, () -> d.shapeless ? 1 : 0, i -> d.shapeless = (i == 1)))
					.input(SlotGroup.grid(3, 3, SlotSpec.item()))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
			case MECH_CRAFTING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Mirrored", "Exact"}, () -> d.mechMirrored ? 0 : 1, i -> d.mechMirrored = (i == 0)))
					.input(SlotGroup.grid(9, 9, SlotSpec.item()))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
			case FURNACE -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(d.furnLabels, () -> d.furnSubIdx, i -> d.furnSubIdx = i))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.processingTime(ProcessingTime.standard(() -> d.furnTime, v -> d.furnTime = v))
					.build();
			case STONECUTTER -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
			case SMITHING -> StationLayout.builder()
					.input(SlotGroup.row(3, SlotSpec.item(), "Input").withSeparator("+"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
			case MIXING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Mixer", "Press"}, () -> d.mixBasinPress ? 1 : 0, i -> d.mixBasinPress = (i == 1)))
					.subToggle(ToggleGroup.of(d.heatLabels, () -> d.mixHeat, i -> d.mixHeat = i))
					.input(SlotGroup.grid(3, 3, 18, 28, 10, SlotSpec.item().withCount(), "Ingredients:"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Result Items:"))
					.inputFluids(SlotGroup.grid(2, 1, 18, 65, 0, SlotSpec.fluid(), "Input Fluids:"))
					.outputFluids(SlotGroup.grid(2, 1, 18, 65, 0, SlotSpec.fluid(), "Result Fluids:"))
					.build();
			case PRESSING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item().withCount(), "Input"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Results"))
					.build();
			case CUTTING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Outputs:"))
					.processingTime(ProcessingTime.standard(() -> d.cutTime, v -> d.cutTime = v))
					.build();
			case FAN -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Washing", "Haunting"}, () -> d.fanHaunting ? 1 : 0, i -> d.fanHaunting = (i == 1)))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Outputs:"))
					.processingTime(ProcessingTime.standard(() -> d.fanTime, v -> d.fanTime = v))
					.build();
			case CRUSHING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Crushing", "Milling"}, () -> d.isMilling ? 1 : 0, i -> d.isMilling = (i == 1)))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.grid(2, 4, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Outputs:"))
					.processingTime(ProcessingTime.standard(() -> d.crushTime, v -> d.crushTime = v))
					.build();
			case DEPLOYING -> {
				var builder = StationLayout.builder()
						.headerToggle(ToggleGroup.of(new String[]{"Deploying", "Application"}, () -> d.deployApplication ? 1 : 0, i -> d.deployApplication = (i == 1)))
						.input(SlotGroup.row(2, SlotSpec.item(), "Input").withSeparator("+"))
						.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"));
				if (!d.deployApplication) {
					builder.subToggle(ToggleGroup.of(new String[]{"Consume Item", "Keep Held Item"}, () -> d.deployKeepHeldItem ? 1 : 0, i -> d.deployKeepHeldItem = (i == 1)));
				}
				yield builder.build();
			}
			case FILLING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.inputFluids(SlotGroup.single(SlotSpec.fluid(), "Fluid"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
		};
	}

	public static int getLayoutTotalWidth(StationType type, RecipeEditorData d, StationLayout layout) {
		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		if (inG == null && inF == null) return 0;

		int inVisualW = inG != null ? inG.getVisualWidth() : 0;
		int outVisualW = outG != null ? outG.getVisualWidth() : 0;

		if (type == StationType.MECH_CRAFTING) return inVisualW + 70;
		if (type == StationType.FILLING) {
			int inFW = inF != null ? inF.getVisualWidth() : 58;
			return inVisualW + 25 + inFW + 25 + outVisualW;
		}

		int itemsW = inVisualW + (outG != null ? 35 + outVisualW : 0);

		int fluidsW = 0;
		if (inF != null || outF != null) {
			int inFW = inF != null ? inF.getVisualWidth() : 0;
			int outFW = outF != null ? outF.getVisualWidth() : 0;
			fluidsW = inFW + (outF != null && inF != null ? 20 : 0) + outFW;
		}

		return Math.max(itemsW, fluidsW);
	}

	public static int getStartX(StationType type, RecipeEditorData d, StationLayout layout, int cx) {
		SlotGroup inG = layout.getInputSlots();
		if (inG == null) return cx;
		if (type == StationType.MECH_CRAFTING) return cx - inG.getVisualWidth() / 2 - 35;
		return cx - getLayoutTotalWidth(type, d, layout) / 2;
	}

	public static float getScale(StationType type, RecipeEditorData d, StationLayout layout, int leftWidth) {
		int totalWidth = getLayoutTotalWidth(type, d, layout);
		int maxW = leftWidth - 24;
		if (totalWidth > maxW && totalWidth > 0 && maxW > 0) {
			return Math.max(0.45f, (float) maxW / totalWidth);
		}
		return 1.0f;
	}

	public static int getContentY(StationType type, StationLayout layout, int editorY) {
		int cy = editorY + 15;
		int headerH = 35;
		int subH = (layout.getSubToggle() != null) ? 30 : 0;
		if (type == StationType.MIXING) cy += 2;
		return cy + headerH + subH;
	}

	/** Sjednocený výpočet ukotvení (anchorX, anchorY) pro všechny skupiny slotů v rozvržení. */
	public static void setupLayoutAnchors(StationLayout layout, StationType type, RecipeEditorData d, int cx, int cy) {
		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();

		if (type == StationType.FILLING) {
			int inW = inG != null ? inG.getVisualWidth() : 18;
			int inFW = inF != null ? inF.getVisualWidth() : 58;
			int outW = outG != null ? outG.getVisualWidth() : 44;
			int totalW = inW + 25 + inFW + 25 + outW;
			int sx = cx - totalW / 2;
			if (inG != null) inG.setAnchor(sx, cy);
			if (inF != null) inF.setAnchor(sx + inW + 25, cy);
			if (outG != null) outG.setAnchor(sx + inW + 25 + inFW + 25, cy);
			return;
		}

		if (inG != null) {
			if (outG != null) {
				int inW = inG.getVisualWidth();
				int outW = outG.getVisualWidth();
				int totalW = inW + 35 + outW;
				int sx = cx - totalW / 2;
				inG.setAnchor(sx, cy);
				int arrowX = sx + inW + 10;
				int rx = arrowX + 25;
				int outY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
				outG.setAnchor(rx, outY);
			} else {
				inG.setAnchor(cx - inG.getVisualWidth() / 2, cy);
			}
		}

		if (inF != null || outF != null) {
			int inH = inG != null ? inG.getHeight() : 0;
			int outH = outG != null ? outG.getHeight() : 0;
			int itemH = Math.max(inH, outH);
			int fluidY = cy + itemH + 15 + (type == StationType.MIXING ? 2 : 0);

			int inFW = inF != null ? inF.getVisualWidth() : 0;
			int outFW = outF != null ? outF.getVisualWidth() : 0;
			int totalFW = inFW + (outF != null && inF != null ? 20 : 0) + outFW;
			int fluidStartX = cx - totalFW / 2;

			if (inF != null) inF.setAnchor(fluidStartX, fluidY);
			if (outF != null) outF.setAnchor(fluidStartX + inFW + (inF != null ? 20 : 0), fluidY);
		}
	}

	public static int render(GuiGraphics g, Font font, StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, boolean isDragging) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		int cy = editorY + 15;

		if (layout.getHeaderToggle() != null) {
			layout.getHeaderToggle().setAnchor(cx, cy);
			layout.getHeaderToggle().render(g, font, mx, my);
		}
		if (layout.getSubToggle() != null) {
			layout.getSubToggle().setAnchor(cx, cy + 25);
			layout.getSubToggle().render(g, font, mx, my);
		}

		cy = getContentY(type, layout, editorY);
		boolean scaled = scale < 0.99f;
		if (scaled) {
			g.pose().pushPose();
			g.pose().translate(cx, cy, 0);
			g.pose().scale(scale, scale, 1.0f);
			g.pose().translate(-cx, -cy, 0);
			mx = (int) (cx + (mx - cx) / scale);
			my = (int) (cy + (my - cy) / scale);
		}

		setupLayoutAnchors(layout, type, d, cx, cy);

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();

		if (type == StationType.FILLING) {
			int sx = inG.getAnchorX();
			g.drawString(font, "Input", sx, cy - 12, UiKit.C_LABEL, false);
			renderSlotGroupItems(g, font, inG, getItemListForGroup(d, type, true), mx, my, isDragging);

			g.drawString(font, "+", sx + inG.getVisualWidth() + 8, cy + 4, UiKit.C_LABEL, false);

			int fluidX = inF.getAnchorX();
			g.drawString(font, "Fluid", fluidX, cy - 12, UiKit.C_LABEL, false);
			UiKit.slotFluid(g, font, mx, my, d.fillFluid, fluidX, cy, isDragging);

			g.drawString(font, "→", fluidX + inF.getVisualWidth() + 8, cy + 4, UiKit.C_LABEL, false);

			int rx = outG.getAnchorX();
			g.drawString(font, "Result", rx, cy - 12, UiKit.C_LABEL, false);
			UiKit.slot(g, font, mx, my, getResultItem(d, type), rx, cy, UiKit.C_SLOT_RES, isDragging);
			UiKit.spinner(g, font, mx, my, rx + UiKit.SS + 6, cy + 2, getResultCount(d, type));

			if (scaled) g.pose().popPose();
			return cy + UiKit.SS + 25 - editorY;
		}

		if (inG != null) {
			if (outG != null) {
				int sx = inG.getAnchorX();
				int rx = outG.getAnchorX();
				int outY = outG.getAnchorY();

				if (inG.getLabel() != null) g.drawString(font, inG.getLabel(), sx, cy - 12, UiKit.C_LABEL, false);

				int arrowX = sx + inG.getVisualWidth() + 10;
				int arrowY = cy + inG.getHeight() / 2 - 4;
				g.drawString(font, "→", arrowX, arrowY, UiKit.C_LABEL, false);

				if (outG.getLabel() != null) g.drawString(font, outG.getLabel(), rx, outY - 12, UiKit.C_LABEL, false);

				renderSlotGroupItems(g, font, inG, getItemListForGroup(d, type, true), mx, my, isDragging);
				List<CrushingOutput> outList = getCrushingOutputsForGroup(d, type);
				if (outList != null) {
					renderSlotGroupOutputs(g, font, outG, outList, mx, my, isDragging);
				} else {
					UiKit.slot(g, font, mx, my, getResultItem(d, type), rx, outY, UiKit.C_SLOT_RES, isDragging);
					UiKit.spinner(g, font, mx, my, rx + UiKit.SS + 6, outY + 2, getResultCount(d, type));
				}
				cy += Math.max(inG.getHeight(), outG.getHeight()) + 15;
			} else {
				renderSlotGroupItems(g, font, inG, getItemListForGroup(d, type, true), mx, my, isDragging);
				cy += inG.getHeight() + 15;
			}
		}

		if (inF != null || outF != null) {
			int fluidY = inF != null ? inF.getAnchorY() : outF.getAnchorY();
			if (inF != null) {
				g.drawString(font, inF.getLabel() != null ? inF.getLabel() : "Input Fluids:", inF.getAnchorX(), fluidY - 12, UiKit.C_LABEL, false);
				List<FluidEntry> fList = getFluidInputs(d, type);
				for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
					UiKit.slotFluid(g, font, mx, my, fList.get(i), inF.getSlotX(i), inF.getSlotY(i), isDragging);
				}
			}
			if (outF != null) {
				g.drawString(font, outF.getLabel() != null ? outF.getLabel() : "Result Fluids:", outF.getAnchorX(), fluidY - 12, UiKit.C_LABEL, false);
				List<FluidEntry> fList = getFluidOutputs(d, type);
				for (int i = 0; i < outF.getTotalSlots() && i < fList.size(); i++) {
					UiKit.slotFluid(g, font, mx, my, fList.get(i), outF.getSlotX(i), outF.getSlotY(i), isDragging);
				}
			}
			cy = fluidY + Math.max(inF != null ? inF.getHeight() : 0, outF != null ? outF.getHeight() : 0) + 25;
		}

		boolean isCampfire = type == StationType.FURNACE && d.furnSubs[d.furnSubIdx].equals("campfire_cooking");
		if (type == StationType.FURNACE && !isCampfire) {
			g.drawString(font, "XP:", cx - 70, cy + 4, UiKit.C_LABEL, false);
			g.drawString(font, String.format(Locale.ROOT, "%.1f", d.furnXp), cx - 45, cy + 4, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, cx - 20, cy + 2);
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tX = (type == StationType.FURNACE) ? (isCampfire ? cx - 35 : cx + 10) : cx - 35;
			int tValX = (type == StationType.FURNACE) ? (isCampfire ? cx : cx + 45) : cx;
			int spinX = (type == StationType.FURNACE) ? (isCampfire ? cx + 40 : cx + 85) : cx + 45;
			g.drawString(font, "Time:", tX, cy + 4, UiKit.C_LABEL, false);
			g.drawString(font, pTime.getValue() + " t", tValX, cy + 4, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, spinX, cy + 2);
			cy += 25;
		}

		int totalH = cy - editorY;
		if (scaled) g.pose().popPose();
		return totalH;
	}

	public static boolean handleSpinnerClicks(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);

		if (scale < 0.99f && scale > 0) {
			mx = (int) (cx + (mx - cx) / scale);
			my = (int) (cy + (my - cy) / scale);
		}

		setupLayoutAnchors(layout, type, d, cx, cy);
		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();

		if (inG != null && outG != null) {
			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					CrushingOutput co = crushOuts.get(i);
					if (co.isEmpty()) continue;
					int ox = outG.getSlotX(i), oy = outG.getSlotY(i);
					int cpx = ox + UiKit.SS + 4, cpy = oy + 2, chX = cpx + 28;
					if (miniCountChance(mx, my, cpx + 16, chX, cpy, co, outG.getSpec().hasChance())) return true;
				}
			} else {
				int spinX = outG.getAnchorX() + UiKit.SS + 6, spinY = outG.getAnchorY() + 2;
				if (UiKit.hit(mx, my, spinX + 18, spinY, UiKit.SPIN_W, UiKit.SPIN_H)) {
					setResultCount(d, type, Math.min(64, getResultCount(d, type) + 1));
					return true;
				}
				if (UiKit.hit(mx, my, spinX + 18, spinY + 8, UiKit.SPIN_W, UiKit.SPIN_H)) {
					setResultCount(d, type, Math.max(1, getResultCount(d, type) - 1));
					return true;
				}
			}
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<ItemStack> inList = getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < inList.size(); i++) {
				ItemStack stack = inList.get(i);
				int sx = inG.getSlotX(i), sy = inG.getSlotY(i);
				int cpx = sx + UiKit.SS + 4, cpy = sy + 2;
				int spinX = cpx + 14;
				if (UiKit.hit(mx, my, spinX, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
					if (!stack.isEmpty()) stack.setCount(Math.min(64, stack.getCount() + 1));
					return true;
				}
				if (UiKit.hit(mx, my, spinX, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
					if (!stack.isEmpty()) stack.setCount(Math.max(1, stack.getCount() - 1));
					return true;
				}
			}
		}

		int timeY = cy + Math.max(inG != null ? inG.getHeight() : 0, outG != null ? outG.getHeight() : 0) + 15;
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		if (inF != null || outF != null) {
			timeY += Math.max(inF != null ? inF.getHeight() : 0, outF != null ? outF.getHeight() : 0) + 25;
		}

		boolean isCampfire = type == StationType.FURNACE && d.furnSubs[d.furnSubIdx].equals("campfire_cooking");
		if (type == StationType.FURNACE && !isCampfire) {
			if (UiKit.hit(mx, my, cx - 20, timeY + 2, UiKit.SPIN_W, UiKit.SPIN_H)) {
				d.furnXp = Math.clamp(d.furnXp + 0.1f, 0f, 100f);
				return true;
			}
			if (UiKit.hit(mx, my, cx - 20, timeY + 10, UiKit.SPIN_W, UiKit.SPIN_H)) {
				d.furnXp = Math.clamp(d.furnXp - 0.1f, 0f, 100f);
				return true;
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int spinX = (type == StationType.FURNACE) ? (isCampfire ? cx + 40 : cx + 85) : cx + 45;
			if (UiKit.hit(mx, my, spinX, timeY + 2, UiKit.SPIN_W, UiKit.SPIN_H)) {
				pTime.increment();
				return true;
			}
			if (UiKit.hit(mx, my, spinX, timeY + 10, UiKit.SPIN_W, UiKit.SPIN_H)) {
				pTime.decrement();
				return true;
			}
		}

		return false;
	}

	private static boolean miniCountChance(int mx, int my, int countX, int chanceX, int cpy, CrushingOutput co, boolean hasChance) {
		if (UiKit.hit(mx, my, countX, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
			co.count = Math.min(64, co.count + 1);
			return true;
		}
		if (UiKit.hit(mx, my, countX, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
			co.count = Math.max(1, co.count - 1);
			return true;
		}
		if (hasChance) {
			if (UiKit.hit(mx, my, chanceX, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
				co.chance = Math.min(1f, co.chance + 0.05f);
				return true;
			}
			if (UiKit.hit(mx, my, chanceX, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
				co.chance = Math.max(0.05f, co.chance - 0.05f);
				return true;
			}
		}
		return false;
	}

	public static boolean handleFluidSpins(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		if (inF == null && outF == null) return false;

		int cy = getContentY(type, layout, editorY);

		if (scale < 0.99f && scale > 0) {
			mx = (int) (cx + (mx - cx) / scale);
			my = (int) (cy + (my - cy) / scale);
		}

		setupLayoutAnchors(layout, type, d, cx, cy);

		if (type == StationType.FILLING) {
			FluidEntry f = d.fillFluid;
			if (f.isEmpty()) return false;
            assert inF != null;
            int amtX = inF.getAnchorX() + UiKit.SS + 4, amtY = cy + 4;
			if (UiKit.hit(mx, my, amtX - 2, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) {
				f.amount = Math.clamp(f.amount + 250, 1, 1000);
				return true;
			}
			if (UiKit.hit(mx, my, amtX + 10, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) {
				f.amount = Math.clamp(f.amount - 250, 1, 1000);
				return true;
			}
			return false;
		}

		if (inF != null) {
			List<FluidEntry> fList = getFluidInputs(d, type);
			for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (f.isEmpty()) continue;
				int amtX = inF.getSlotX(i) + UiKit.SS + 4, amtY = inF.getSlotY(i) + 4;
				if (UiKit.hit(mx, my, amtX - 2, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) {
					f.amount = Math.clamp(f.amount + 250, 1, 1000);
					return true;
				}
				if (UiKit.hit(mx, my, amtX + 10, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) {
					f.amount = Math.clamp(f.amount - 250, 1, 1000);
					return true;
				}
			}
		}

		if (outF != null) {
			List<FluidEntry> fList = getFluidOutputs(d, type);
			for (int i = 0; i < outF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (f.isEmpty()) continue;
				int amtX = outF.getSlotX(i) + UiKit.SS + 4, amtY = outF.getSlotY(i) + 4;
				if (UiKit.hit(mx, my, amtX - 2, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) {
					f.amount = Math.clamp(f.amount + 250, 1, 1000);
					return true;
				}
				if (UiKit.hit(mx, my, amtX + 10, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) {
					f.amount = Math.clamp(f.amount - 250, 1, 1000);
					return true;
				}
			}
		}
		return false;
	}

	public static boolean handleDoubleClick(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, EditCallback callback) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);

		if (scale < 0.99f && scale > 0) {
			mx = (int) (cx + (mx - cx) / scale);
			my = (int) (cy + (my - cy) / scale);
		}

		setupLayoutAnchors(layout, type, d, cx, cy);

		if (type == StationType.FILLING) {
			SlotGroup inF = layout.getInputFluids();
			SlotGroup outG = layout.getOutputSlots();
			int amtX = inF.getAnchorX() + UiKit.SS + 4, amtY = cy + 4;
			if (!d.fillFluid.isEmpty() && UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 14)) {
				callback.edit("fluid_fill_in", amtX, amtY, 45, String.valueOf(d.fillFluid.amount), -1);
				return true;
			}
			int cpx = outG.getAnchorX() + UiKit.SS + 6, cpy = cy + 2;
			if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) {
				callback.edit("filling_count", cpx, cpy + 2, 20, String.valueOf(getResultCount(d, type)), -1);
				return true;
			}
			return false;
		}

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		if (inG != null && outG != null) {
			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				String prefix = type.name().toLowerCase() + "_out_";
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					CrushingOutput co = crushOuts.get(i);
					if (co.isEmpty()) continue;
					int ox = outG.getSlotX(i), oy = outG.getSlotY(i);
					int cpx = ox + UiKit.SS + 4, cpy = oy + 2, chX = cpx + 28;
					if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) {
						callback.edit(prefix + "count", cpx, cpy + 2, 20, String.valueOf(co.count), i);
						return true;
					}
					if (outG.getSpec().hasChance() && UiKit.hit(mx, my, chX + 10, cpy + 1, 30, 12)) {
						callback.edit(prefix + "chance", chX + 14, cpy + 3, 26, String.valueOf((int) (co.chance * 100)), i);
						return true;
					}
				}
			} else {
				int cpx = outG.getAnchorX() + UiKit.SS + 6, cpy = outG.getAnchorY() + 2;
				if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) {
					callback.edit(type.name().toLowerCase() + "_count", cpx, cpy + 2, 20, String.valueOf(getResultCount(d, type)), -1);
					return true;
				}
			}
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<ItemStack> inList = getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < inList.size(); i++) {
				ItemStack stack = inList.get(i);
				if (!stack.isEmpty()) {
					int sx = inG.getSlotX(i), sy = inG.getSlotY(i);
					int cpx = sx + UiKit.SS + 4, cpy = sy + 2;
					if (UiKit.hit(mx, my, cpx, cpy, 14, 12)) {
						callback.edit("grid_count", cpx, cpy + 2, 20, String.valueOf(stack.getCount()), i);
						return true;
					}
				}
			}
		}

		int timeY = cy + Math.max(inG != null ? inG.getHeight() : 0, outG != null ? outG.getHeight() : 0) + 15;
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		if (inF != null || outF != null) {
			timeY += Math.max(inF != null ? inF.getHeight() : 0, outF != null ? outF.getHeight() : 0) + 25;
		}

		boolean isCampfire = type == StationType.FURNACE && d.furnSubs[d.furnSubIdx].equals("campfire_cooking");
		if (type == StationType.FURNACE && !isCampfire) {
			if (UiKit.hit(mx, my, cx - 45, timeY + 4, 30, 12)) {
				callback.edit("furnXp", cx - 45, timeY + 3, 30, String.format(Locale.ROOT, "%.1f", d.furnXp), -1);
				return true;
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tValX = (type == StationType.FURNACE) ? (isCampfire ? cx : cx + 45) : cx;
			if (UiKit.hit(mx, my, tValX, timeY + 4, 35, 12)) {
				callback.edit(type.name().toLowerCase() + "Time", tValX, timeY + 3, 35, String.valueOf(pTime.getValue()), -1);
				return true;
			}
		}

		if (inF != null || outF != null) {
			if (inF != null) {
				List<FluidEntry> fList = getFluidInputs(d, type);
				for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
					FluidEntry f = fList.get(i);
					if (f.isEmpty()) continue;
					int amtX = inF.getSlotX(i) + UiKit.SS + 4, amtY = inF.getSlotY(i) + 4;
					if (UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 14)) {
						String field = "fluid_mix_in";
						callback.edit(field, amtX, amtY, 45, String.valueOf(f.amount), i);
						return true;
					}
				}
			}

			if (outF != null) {
				List<FluidEntry> fList = getFluidOutputs(d, type);
				for (int i = 0; i < outF.getTotalSlots() && i < fList.size(); i++) {
					FluidEntry f = fList.get(i);
					if (f.isEmpty()) continue;
					int amtX = outF.getSlotX(i) + UiKit.SS + 4, amtY = outF.getSlotY(i) + 4;
					if (UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 14)) {
						callback.edit("fluid_mix_out", amtX, amtY, 45, String.valueOf(f.amount), i);
						return true;
					}
				}
			}
		}

		return false;
	}

	public static List<FluidEntry> getFluidInputs(RecipeEditorData d, StationType type) {
		return switch (type) {
			case MIXING -> d.mixFluidIng;
			case FILLING -> List.of(d.fillFluid);
			default -> List.of();
		};
	}

	public static List<FluidEntry> getFluidOutputs(RecipeEditorData d, StationType type) {
		return type == StationType.MIXING ? d.mixFluidOuts : List.of();
	}

	public static void setResultCount(RecipeEditorData d, StationType type, int count) {
		switch (type) {
			case CRAFTING -> d.craftCount = count;
			case MECH_CRAFTING -> d.mechCount = count;
			case FURNACE -> d.furnCount = count;
			case STONECUTTER -> d.stoneCount = count;
			case SMITHING -> d.smCount = count;
		}
	}

	private static int getResultCount(RecipeEditorData d, StationType type) {
		return switch (type) {
			case CRAFTING -> d.craftCount;
			case MECH_CRAFTING -> d.mechCount;
			case FURNACE -> d.furnCount;
			case STONECUTTER -> d.stoneCount;
			case SMITHING -> d.smCount;
			default -> 1;
		};
	}

	private static void renderSlotGroupItems(GuiGraphics g, Font font, SlotGroup group, List<ItemStack> items, int mx, int my, boolean isDragging) {
		for (int i = 0; i < group.getTotalSlots(); i++) {
			int sx = group.getSlotX(i);
			int sy = group.getSlotY(i);
			ItemStack stack = (items != null && i < items.size()) ? items.get(i) : ItemStack.EMPTY;
			UiKit.slot(g, font, mx, my, stack, sx, sy, UiKit.C_SLOT, isDragging);

			if (group.getSpec().hasCount()) {
				int cpx = sx + UiKit.SS + 4, cpy = sy + 2;
				int displayCount = stack.isEmpty() ? 1 : stack.getCount();
				g.drawString(font, String.valueOf(displayCount), cpx, cpy + 2, UiKit.C_TEXT, false);
				UiKit.drawMiniSpinner(g, font, mx, my, cpx + 14, cpy - 2);
			}

			if (group.getSeparatorSymbol() != null && i < group.getTotalSlots() - 1) {
				int nextSx = group.getSlotX(i + 1);
				int sepX = (sx + UiKit.SS + nextSx) / 2 - font.width(group.getSeparatorSymbol()) / 2;
				g.drawString(font, group.getSeparatorSymbol(), sepX, sy + 4, UiKit.C_LABEL, false);
			}
		}
	}

	private static void renderSlotGroupOutputs(GuiGraphics g, Font font, SlotGroup group, List<CrushingOutput> outputs, int mx, int my, boolean isDragging) {
		for (int i = 0; i < group.getTotalSlots() && i < outputs.size(); i++) {
			int ox = group.getSlotX(i);
			int oy = group.getSlotY(i);
			CrushingOutput co = outputs.get(i);
			UiKit.slot(g, font, mx, my, co.stack, ox, oy, co.isEmpty() ? UiKit.C_SLOT : UiKit.C_SLOT_RES, isDragging);

			int cpx = ox + UiKit.SS + 4, cpy = oy + 2;
			if (group.getSpec().hasCount()) {
				g.drawString(font, String.valueOf(co.count), cpx, cpy + 2, UiKit.C_TEXT, false);
				UiKit.drawMiniSpinner(g, font, mx, my, cpx + 16, cpy - 2);
			}

			if (group.getSpec().hasChance()) {
				int chX = cpx + (group.getSpec().hasCount() ? 28 : 0);
				String chStr = co.chance >= 1f ? "100%" : Math.round(co.chance * 100) + "%";
				UiKit.drawMiniSpinner(g, font, mx, my, chX, cpy - 2);
				g.drawString(font, chStr, chX + 14, cpy + 3, co.isEmpty() ? UiKit.C_LABEL : 0xFFAAFF88, false);
			}
		}
	}

	public static List<ItemStack> getItemListForGroup(RecipeEditorData d, StationType type, boolean isInput) {
		return switch (type) {
			case CRAFTING -> d.craftGrid;
			case MECH_CRAFTING -> d.mechGrid;
			case MIXING -> d.mixIng;
			case PRESSING -> d.pressIng;
			case CUTTING -> List.of(d.cutIn);
			case CRUSHING -> List.of(d.crushIn);
			case FAN -> List.of(d.fanIn);
			case FURNACE -> List.of(d.furnIn);
			case STONECUTTER -> List.of(d.stoneIn);
			case SMITHING -> List.of(d.smTemplate, d.smBase, d.smAddition);
			case DEPLOYING -> isInput ? List.of(d.deployTarget, d.deployTool) : List.of(d.deployResult);
			case FILLING -> List.of(d.fillIn);
		};
	}

	public static List<CrushingOutput> getCrushingOutputsForGroup(RecipeEditorData d, StationType type) {
		return switch (type) {
			case MIXING -> d.mixOuts;
			case PRESSING -> d.pressOuts;
			case CUTTING -> d.cutOuts;
			case CRUSHING -> d.crushOuts;
			case FAN -> d.fanOuts;
			default -> null;
		};
	}

	public static ItemStack getResultItem(RecipeEditorData d, StationType type) {
		return switch (type) {
			case CRAFTING -> d.craftResult;
			case MECH_CRAFTING -> d.mechResult;
			case FURNACE -> d.furnOut;
			case STONECUTTER -> d.stoneOut;
			case SMITHING -> d.smResult;
			case DEPLOYING -> d.deployResult;
			case FILLING -> d.fillResult;
			default -> ItemStack.EMPTY;
		};
	}

	/** Nastaví vstupní item na pozici {@code idx} pro danou stanici. */
	public static void setInputItem(RecipeEditorData d, StationType type, int idx, ItemStack s) {
		switch (type) {
			case CRAFTING    -> d.craftGrid.set(idx, s);
			case MECH_CRAFTING -> d.mechGrid.set(idx, s);
			case MIXING      -> d.mixIng.set(idx, s);
			case PRESSING    -> d.pressIng.set(0, s);
			case CUTTING     -> d.cutIn = s;
			case CRUSHING    -> d.crushIn = s;
			case FAN         -> d.fanIn = s;
			case FURNACE     -> d.furnIn = s;
			case STONECUTTER -> d.stoneIn = s;
			case SMITHING    -> {
				if (idx == 0) d.smTemplate = s;
				else if (idx == 1) d.smBase = s;
				else if (idx == 2) d.smAddition = s;
			}
			case DEPLOYING   -> {
				if (idx == 0) d.deployTarget = s;
				else if (idx == 1) d.deployTool = s;
			}
			case FILLING     -> d.fillIn = s;
		}
	}

	/** Nastaví výstupní (result) item pro danou stanici. */
	public static void setOutputItem(RecipeEditorData d, StationType type, ItemStack s) {
		switch (type) {
			case CRAFTING    -> d.craftResult = s;
			case MECH_CRAFTING -> d.mechResult = s;
			case FURNACE     -> d.furnOut = s;
			case STONECUTTER -> d.stoneOut = s;
			case SMITHING    -> d.smResult = s;
			case DEPLOYING   -> d.deployResult = s;
			case FILLING     -> d.fillResult = s;
			default -> {}
		}
	}
}