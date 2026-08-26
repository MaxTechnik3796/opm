package cz.maxtechnik.opm.client.editor.layout;

import cz.maxtechnik.opm.client.editor.RecipeEditorData;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.ui.UiKit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class StationLayoutEngine {
	private StationLayoutEngine() {}

	public interface EditCallback {
		void edit(String fieldName, int bx, int by, int bw, String val, int idx);
	}

	public static StationLayout getLayout(StationType type, RecipeEditorData d) {
		return switch (type) {
			case CRAFTING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Shaped", "Shapeless"}, () -> d.shapeless ? 1 : 0, i -> d.shapeless = (i == 1)))
					.input(SlotGroup.grid(3, 3, SlotSpec.item()))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result")).build();
			case MECH_CRAFTING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Mirrored", "Exact"}, () -> d.mechMirrored ? 0 : 1, i -> d.mechMirrored = (i == 0)))
					.input(SlotGroup.grid(9, 9, SlotSpec.item()))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result")).build();
			case FURNACE -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(d.furnLabels, () -> d.furnSubIdx, i -> d.furnSubIdx = i))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.processingTime(ProcessingTime.standard(() -> d.furnTime, v -> d.furnTime = v)).build();
			case STONECUTTER -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result")).build();
			case SMITHING -> StationLayout.builder()
					.input(SlotGroup.row(3, SlotSpec.item(), "Input").withSeparator("+"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result")).build();
			case MIXING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Mixer", "Press"}, () -> d.mixBasinPress ? 1 : 0, i -> d.mixBasinPress = (i == 1)))
					.subToggle(ToggleGroup.of(d.heatLabels, () -> d.mixHeat, i -> d.mixHeat = i))
					.input(SlotGroup.grid(3, 3, 18, 28, 10, SlotSpec.item().withCount(), "Ingredients:"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Result Items:"))
					.inputFluids(SlotGroup.grid(2, 1, 18, 65, 0, SlotSpec.fluid(), "Input Fluids:"))
					.outputFluids(SlotGroup.grid(2, 1, 18, 65, 0, SlotSpec.fluid(), "Result Fluids:")).build();
			case PRESSING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item().withCount(), "Input"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Results")).build();
			case CUTTING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Outputs:"))
					.processingTime(ProcessingTime.standard(() -> d.cutTime, v -> d.cutTime = v)).build();
			case FAN -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Washing", "Haunting"}, () -> d.fanHaunting ? 1 : 0, i -> d.fanHaunting = (i == 1)))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.grid(2, 2, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Outputs:"))
					.processingTime(ProcessingTime.standard(() -> d.fanTime, v -> d.fanTime = v)).build();
			case CRUSHING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Crushing", "Milling"}, () -> d.isMilling ? 1 : 0, i -> d.isMilling = (i == 1)))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.grid(2, 4, 18, 75, 22, SlotSpec.item().withCount().withChance(), "Outputs:"))
					.processingTime(ProcessingTime.standard(() -> d.crushTime, v -> d.crushTime = v)).build();
			case DEPLOYING -> {
				var b = StationLayout.builder()
						.headerToggle(ToggleGroup.of(new String[]{"Deploying", "Application"}, () -> d.deployApplication ? 1 : 0, i -> d.deployApplication = (i == 1)))
						.input(SlotGroup.row(2, SlotSpec.item(), "Input").withSeparator("+"))
						.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"));
				if (!d.deployApplication) b.subToggle(ToggleGroup.of(new String[]{"Consume Item", "Keep Held Item"}, () -> d.deployKeepHeldItem ? 1 : 0, i -> d.deployKeepHeldItem = (i == 1)));
				yield b.build();
			}
			case FILLING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.inputFluids(SlotGroup.single(SlotSpec.fluid(), "Fluid"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result")).build();
		};
	}

	public static int getLayoutTotalWidth(StationType type, StationLayout layout) {
		SlotGroup inG = layout.getInputSlots(), outG = layout.getOutputSlots(), inF = layout.getInputFluids(), outF = layout.getOutputFluids();
		if (inG == null && inF == null) return 0;
		int inW = inG != null ? inG.getVisualWidth() : 0, outW = outG != null ? outG.getVisualWidth() : 0;
		if (type == StationType.MECH_CRAFTING) return inW + 70;
		if (type == StationType.FILLING) return inW + 50 + (inF != null ? inF.getVisualWidth() : 58) + outW;
		int itemsW = inW + (outG != null ? 35 + outW : 0);
		int fluidsW = (inF != null || outF != null) ? (inF != null ? inF.getVisualWidth() : 0) + (outF != null && inF != null ? 20 : 0) + (outF != null ? outF.getVisualWidth() : 0) : 0;
		return Math.max(itemsW, fluidsW);
	}

	public static float getScale(StationType type, StationLayout layout, int leftWidth) {
		int tw = getLayoutTotalWidth(type, layout), maxW = leftWidth - 24;
		return (tw > maxW && tw > 0 && maxW > 0) ? Math.max(0.45f, (float) maxW / tw) : 1.0f;
	}

	public static int getContentY(StationType type, StationLayout layout, int editorY) {
		return editorY + 50 + (layout.getSubToggle() != null ? 30 : 0) + (type == StationType.MIXING ? 2 : 0);
	}

	public static void setupLayoutAnchors(StationLayout layout, StationType type, int cx, int cy) {
		SlotGroup inG = layout.getInputSlots(), outG = layout.getOutputSlots(), inF = layout.getInputFluids(), outF = layout.getOutputFluids();
		if (type == StationType.FILLING) {
			int inW = inG != null ? inG.getVisualWidth() : 18, inFW = inF != null ? inF.getVisualWidth() : 58, outW = outG != null ? outG.getVisualWidth() : 44;
			int sx = cx - (inW + 50 + inFW + outW) / 2;
			if (inG != null) inG.setAnchor(sx, cy);
			if (inF != null) inF.setAnchor(sx + inW + 25, cy);
			if (outG != null) outG.setAnchor(sx + inW + 50 + inFW, cy);
			return;
		}
		if (inG != null) {
			if (outG != null) {
				int sx = cx - (inG.getVisualWidth() + 35 + outG.getVisualWidth()) / 2;
				inG.setAnchor(sx, cy);
				outG.setAnchor(sx + inG.getVisualWidth() + 35, (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy);
			} else inG.setAnchor(cx - inG.getVisualWidth() / 2, cy);
		}
		if (inF != null || outF != null) {
			int inFW = inF != null ? inF.getVisualWidth() : 0, outFW = outF != null ? outF.getVisualWidth() : 0;
			int fluidY = cy + Math.max(inG != null ? inG.getHeight() : 0, outG != null ? outG.getHeight() : 0) + 15 + (type == StationType.MIXING ? 2 : 0);
			int fluidStartX = cx - (inFW + (outF != null && inF != null ? 20 : 0) + outFW) / 2;
			if (inF != null) inF.setAnchor(fluidStartX, fluidY);
			if (outF != null) outF.setAnchor(fluidStartX + inFW + (inF != null ? 20 : 0), fluidY);
		}
	}

	public static int render(GuiGraphics g, Font font, StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, layout, leftWidth);
		int cy = editorY + 15;

		if (layout.getHeaderToggle() != null) { layout.getHeaderToggle().setAnchor(cx, cy); layout.getHeaderToggle().render(g, font, mx, my); }
		if (layout.getSubToggle() != null) { layout.getSubToggle().setAnchor(cx, cy + 25); layout.getSubToggle().render(g, font, mx, my); }

		cy = getContentY(type, layout, editorY);
		boolean scaled = scale < 0.99f;
		if (scaled) {
			g.pose().pushPose();
			g.pose().translate(cx, cy, 0);
			g.pose().scale(scale, scale, 1f);
			g.pose().translate(-cx, -cy, 0);
			mx = (int) (cx + (mx - cx) / scale);
			my = (int) (cy + (my - cy) / scale);
		}

		setupLayoutAnchors(layout, type, cx, cy);
		SlotGroup inG = layout.getInputSlots(), outG = layout.getOutputSlots(), inF = layout.getInputFluids(), outF = layout.getOutputFluids();

		if (type == StationType.FILLING) {
			g.drawString(font, "Input", inG.getAnchorX(), cy - 12, UiKit.C_LABEL, false);
			renderSlots(g, font, inG, getItemListForGroup(d, type, true), mx, my);
			g.drawString(font, "+", inG.getAnchorX() + inG.getVisualWidth() + 8, cy + 4, UiKit.C_LABEL, false);
			g.drawString(font, "Fluid", inF.getAnchorX(), cy - 12, UiKit.C_LABEL, false);
			UiKit.slotFluid(g, font, mx, my, d.fillFluid, inF.getAnchorX(), cy, false);
			g.drawString(font, "→", inF.getAnchorX() + inF.getVisualWidth() + 8, cy + 4, UiKit.C_LABEL, false);
			g.drawString(font, "Result", outG.getAnchorX(), cy - 12, UiKit.C_LABEL, false);
			UiKit.slot(g, font, mx, my, getResultItem(d, type), outG.getAnchorX(), cy, UiKit.C_SLOT_RES, false);
			UiKit.spinner(g, font, mx, my, outG.getAnchorX() + UiKit.SS + 6, cy + 2, getResultCount(d, type));
			if (scaled) g.pose().popPose();
			return cy + UiKit.SS + 25 - editorY;
		}

		if (inG != null) {
			if (inG.getLabel() != null) g.drawString(font, inG.getLabel(), inG.getAnchorX(), cy - 12, UiKit.C_LABEL, false);
			renderSlots(g, font, inG, getItemListForGroup(d, type, true), mx, my);
			if (outG != null) {
				g.drawString(font, "→", inG.getAnchorX() + inG.getVisualWidth() + 10, cy + inG.getHeight() / 2 - 4, UiKit.C_LABEL, false);
				if (outG.getLabel() != null) g.drawString(font, outG.getLabel(), outG.getAnchorX(), outG.getAnchorY() - 12, UiKit.C_LABEL, false);
				List<CrushingOutput> outList = getCrushingOutputsForGroup(d, type);
				if (outList != null) renderOutputs(g, font, outG, outList, mx, my);
				else {
					UiKit.slot(g, font, mx, my, getResultItem(d, type), outG.getAnchorX(), outG.getAnchorY(), UiKit.C_SLOT_RES, false);
					UiKit.spinner(g, font, mx, my, outG.getAnchorX() + UiKit.SS + 6, outG.getAnchorY() + 2, getResultCount(d, type));
				}
				cy += Math.max(inG.getHeight(), outG.getHeight()) + 15;
			} else cy += inG.getHeight() + 15;
		}

		if (inF != null || outF != null) {
			int fluidY = inF != null ? inF.getAnchorY() : outF.getAnchorY();
			if (inF != null) {
				g.drawString(font, inF.getLabel() != null ? inF.getLabel() : "Input Fluids:", inF.getAnchorX(), fluidY - 12, UiKit.C_LABEL, false);
				renderFluids(g, font, inF, getFluidInputs(d, type), mx, my);
			}
			if (outF != null) {
				g.drawString(font, outF.getLabel() != null ? outF.getLabel() : "Result Fluids:", outF.getAnchorX(), fluidY - 12, UiKit.C_LABEL, false);
				renderFluids(g, font, outF, getFluidOutputs(d, type), mx, my);
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

		if (scaled) g.pose().popPose();
		return cy - editorY;
	}

	public static boolean handleClicks(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, boolean fluidsOnly) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);
		if (scale < 0.99f && scale > 0) { mx = (int) (cx + (mx - cx) / scale); my = (int) (cy + (my - cy) / scale); }
		setupLayoutAnchors(layout, type, cx, cy);

		if (fluidsOnly) {
			if (type == StationType.FILLING && !d.fillFluid.isEmpty()) return checkFluidSpin(mx, my, layout.getInputFluids().getAnchorX() + UiKit.SS + 4, cy + 4, d.fillFluid);
			if (layout.getInputFluids() != null) {
				var list = getFluidInputs(d, type);
				for (int i = 0; i < layout.getInputFluids().getTotalSlots() && i < list.size(); i++) {
					if (!list.get(i).isEmpty() && checkFluidSpin(mx, my, layout.getInputFluids().getSlotX(i) + UiKit.SS + 4, layout.getInputFluids().getSlotY(i) + 4, list.get(i))) return true;
				}
			}
			if (layout.getOutputFluids() != null) {
				var list = getFluidOutputs(d, type);
				for (int i = 0; i < layout.getOutputFluids().getTotalSlots() && i < list.size(); i++) {
					if (!list.get(i).isEmpty() && checkFluidSpin(mx, my, layout.getOutputFluids().getSlotX(i) + UiKit.SS + 4, layout.getOutputFluids().getSlotY(i) + 4, list.get(i))) return true;
				}
			}
			return false;
		}

		SlotGroup inG = layout.getInputSlots(), outG = layout.getOutputSlots();
		if (inG != null && outG != null) {
			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					CrushingOutput co = crushOuts.get(i);
					if (co.isEmpty()) continue;
					int cpx = outG.getSlotX(i) + UiKit.SS + 4, cpy = outG.getSlotY(i) + 2;
					if (UiKit.hit(mx, my, cpx + 16, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) { co.count = Math.min(64, co.count + 1); return true; }
					if (UiKit.hit(mx, my, cpx + 16, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) { co.count = Math.max(1, co.count - 1); return true; }
					if (outG.getSpec().hasChance()) {
						int chX = cpx + 28;
						if (UiKit.hit(mx, my, chX, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) { co.chance = Math.min(1f, co.chance + 0.05f); return true; }
						if (UiKit.hit(mx, my, chX, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) { co.chance = Math.max(0.05f, co.chance - 0.05f); return true; }
					}
				}
			} else {
				int spinX = outG.getAnchorX() + UiKit.SS + 6, spinY = outG.getAnchorY() + 2;
				if (UiKit.hit(mx, my, spinX + 18, spinY, UiKit.SPIN_W, UiKit.SPIN_H)) { setResultCount(d, type, Math.min(64, getResultCount(d, type) + 1)); return true; }
				if (UiKit.hit(mx, my, spinX + 18, spinY + 8, UiKit.SPIN_W, UiKit.SPIN_H)) { setResultCount(d, type, Math.max(1, getResultCount(d, type) - 1)); return true; }
			}
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<ItemStack> inList = getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < inList.size(); i++) {
				ItemStack s = inList.get(i);
				int spinX = inG.getSlotX(i) + UiKit.SS + 18, cpy = inG.getSlotY(i) + 2;
				if (UiKit.hit(mx, my, spinX, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) { if (!s.isEmpty()) s.setCount(Math.min(64, s.getCount() + 1)); return true; }
				if (UiKit.hit(mx, my, spinX, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) { if (!s.isEmpty()) s.setCount(Math.max(1, s.getCount() - 1)); return true; }
			}
		}

		int timeY = cy + Math.max(inG != null ? inG.getHeight() : 0, outG != null ? outG.getHeight() : 0) + 15;
		if (layout.getInputFluids() != null || layout.getOutputFluids() != null) timeY += Math.max(layout.getInputFluids() != null ? layout.getInputFluids().getHeight() : 0, layout.getOutputFluids() != null ? layout.getOutputFluids().getHeight() : 0) + 25;

		boolean isCampfire = type == StationType.FURNACE && d.furnSubs[d.furnSubIdx].equals("campfire_cooking");
		if (type == StationType.FURNACE && !isCampfire) {
			if (UiKit.hit(mx, my, cx - 20, timeY + 2, UiKit.SPIN_W, UiKit.SPIN_H)) { d.furnXp = Math.clamp(d.furnXp + 0.1f, 0f, 100f); return true; }
			if (UiKit.hit(mx, my, cx - 20, timeY + 10, UiKit.SPIN_W, UiKit.SPIN_H)) { d.furnXp = Math.clamp(d.furnXp - 0.1f, 0f, 100f); return true; }
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int spinX = (type == StationType.FURNACE) ? (isCampfire ? cx + 40 : cx + 85) : cx + 45;
			if (UiKit.hit(mx, my, spinX, timeY + 2, UiKit.SPIN_W, UiKit.SPIN_H)) { pTime.increment(); return true; }
			if (UiKit.hit(mx, my, spinX, timeY + 10, UiKit.SPIN_W, UiKit.SPIN_H)) { pTime.decrement(); return true; }
		}
		return false;
	}

	private static boolean checkFluidSpin(int mx, int my, int amtX, int amtY, FluidEntry f) {
		if (UiKit.hit(mx, my, amtX - 2, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) { f.amount = Math.clamp(f.amount + 250, 1, 1000); return true; }
		if (UiKit.hit(mx, my, amtX + 10, amtY + 12, UiKit.SPIN_W, UiKit.SPIN_H)) { f.amount = Math.clamp(f.amount - 250, 1, 1000); return true; }
		return false;
	}

	public static boolean handleDoubleClick(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, EditCallback callback) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);
		if (scale < 0.99f && scale > 0) { mx = (int) (cx + (mx - cx) / scale); my = (int) (cy + (my - cy) / scale); }
		setupLayoutAnchors(layout, type, cx, cy);

		if (type == StationType.FILLING) {
			if (!d.fillFluid.isEmpty() && UiKit.hit(mx, my, layout.getInputFluids().getAnchorX() + UiKit.SS + 2, cy + 2, 50, 14)) {
				callback.edit("fluid_fill_in", layout.getInputFluids().getAnchorX() + UiKit.SS + 4, cy + 4, 45, String.valueOf(d.fillFluid.amount), -1);
				return true;
			}
			if (UiKit.hit(mx, my, layout.getOutputSlots().getAnchorX() + UiKit.SS + 6, cy + 4, 14, 12)) {
				callback.edit("filling_count", layout.getOutputSlots().getAnchorX() + UiKit.SS + 6, cy + 4, 20, String.valueOf(getResultCount(d, type)), -1);
				return true;
			}
			return false;
		}

		SlotGroup inG = layout.getInputSlots(), outG = layout.getOutputSlots();
		if (inG != null && outG != null) {
			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				String prefix = type.name().toLowerCase(Locale.ROOT) + "_out_";
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					CrushingOutput co = crushOuts.get(i);
					if (co.isEmpty()) continue;
					int cpx = outG.getSlotX(i) + UiKit.SS + 4, cpy = outG.getSlotY(i) + 2;
					if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) { callback.edit(prefix + "count", cpx, cpy + 2, 20, String.valueOf(co.count), i); return true; }
					if (outG.getSpec().hasChance() && UiKit.hit(mx, my, cpx + 38, cpy + 1, 30, 12)) { callback.edit(prefix + "chance", cpx + 42, cpy + 3, 26, String.valueOf((int) (co.chance * 100)), i); return true; }
				}
			} else {
				int cpx = outG.getAnchorX() + UiKit.SS + 6, cpy = outG.getAnchorY() + 2;
				if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) { callback.edit(type.name().toLowerCase(Locale.ROOT) + "_count", cpx, cpy + 2, 20, String.valueOf(getResultCount(d, type)), -1); return true; }
			}
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<ItemStack> inList = getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < inList.size(); i++) {
				if (!inList.get(i).isEmpty() && UiKit.hit(mx, my, inG.getSlotX(i) + UiKit.SS + 4, inG.getSlotY(i) + 2, 14, 12)) {
					callback.edit("grid_count", inG.getSlotX(i) + UiKit.SS + 4, inG.getSlotY(i) + 4, 20, String.valueOf(inList.get(i).getCount()), i);
					return true;
				}
			}
		}

		int timeY = cy + Math.max(inG != null ? inG.getHeight() : 0, outG != null ? outG.getHeight() : 0) + 15;
		if (layout.getInputFluids() != null || layout.getOutputFluids() != null) timeY += Math.max(layout.getInputFluids() != null ? layout.getInputFluids().getHeight() : 0, layout.getOutputFluids() != null ? layout.getOutputFluids().getHeight() : 0) + 25;

		boolean isCampfire = type == StationType.FURNACE && d.furnSubs[d.furnSubIdx].equals("campfire_cooking");
		if (type == StationType.FURNACE && !isCampfire && UiKit.hit(mx, my, cx - 45, timeY + 4, 30, 12)) {
			callback.edit("furnXp", cx - 45, timeY + 3, 30, String.format(Locale.ROOT, "%.1f", d.furnXp), -1);
			return true;
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tValX = (type == StationType.FURNACE) ? (isCampfire ? cx : cx + 45) : cx;
			if (UiKit.hit(mx, my, tValX, timeY + 4, 35, 12)) {
				callback.edit(type.name().toLowerCase(Locale.ROOT) + "Time", tValX, timeY + 3, 35, String.valueOf(pTime.getValue()), -1);
				return true;
			}
		}

		if (layout.getInputFluids() != null) {
			var list = getFluidInputs(d, type);
			for (int i = 0; i < layout.getInputFluids().getTotalSlots() && i < list.size(); i++) {
				if (!list.get(i).isEmpty() && UiKit.hit(mx, my, layout.getInputFluids().getSlotX(i) + UiKit.SS + 2, layout.getInputFluids().getSlotY(i) + 2, 50, 14)) {
					callback.edit("fluid_mix_in", layout.getInputFluids().getSlotX(i) + UiKit.SS + 4, layout.getInputFluids().getSlotY(i) + 4, 45, String.valueOf(list.get(i).amount), i);
					return true;
				}
			}
		}
		if (layout.getOutputFluids() != null) {
			var list = getFluidOutputs(d, type);
			for (int i = 0; i < layout.getOutputFluids().getTotalSlots() && i < list.size(); i++) {
				if (!list.get(i).isEmpty() && UiKit.hit(mx, my, layout.getOutputFluids().getSlotX(i) + UiKit.SS + 2, layout.getOutputFluids().getSlotY(i) + 2, 50, 14)) {
					callback.edit("fluid_mix_out", layout.getOutputFluids().getSlotX(i) + UiKit.SS + 4, layout.getOutputFluids().getSlotY(i) + 4, 45, String.valueOf(list.get(i).amount), i);
					return true;
				}
			}
		}
		return false;
	}

	public static List<FluidEntry> getFluidInputs(RecipeEditorData d, StationType type) {
		return type == StationType.MIXING ? d.mixFluidIng : (type == StationType.FILLING ? List.of(d.fillFluid) : List.of());
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

	private static void renderSlots(GuiGraphics g, Font font, SlotGroup group, List<ItemStack> items, int mx, int my) {
		for (int i = 0; i < group.getTotalSlots(); i++) {
			int sx = group.getSlotX(i), sy = group.getSlotY(i);
			ItemStack stack = (items != null && i < items.size()) ? items.get(i) : ItemStack.EMPTY;
			UiKit.slot(g, font, mx, my, stack, sx, sy, UiKit.C_SLOT, false);
			if (group.getSpec().hasCount()) {
				int cpx = sx + UiKit.SS + 4, cpy = sy + 2;
				g.drawString(font, String.valueOf(stack.isEmpty() ? 1 : stack.getCount()), cpx, cpy + 2, UiKit.C_TEXT, false);
				UiKit.drawMiniSpinner(g, font, mx, my, cpx + 14, cpy - 2);
			}
			if (group.getSeparatorSymbol() != null && i < group.getTotalSlots() - 1) {
				g.drawString(font, group.getSeparatorSymbol(), (sx + UiKit.SS + group.getSlotX(i + 1)) / 2 - font.width(group.getSeparatorSymbol()) / 2, sy + 4, UiKit.C_LABEL, false);
			}
		}
	}

	private static void renderOutputs(GuiGraphics g, Font font, SlotGroup group, List<CrushingOutput> outputs, int mx, int my) {
		for (int i = 0; i < group.getTotalSlots() && i < outputs.size(); i++) {
			int ox = group.getSlotX(i), oy = group.getSlotY(i);
			CrushingOutput co = outputs.get(i);
			UiKit.slot(g, font, mx, my, co.stack, ox, oy, co.isEmpty() ? UiKit.C_SLOT : UiKit.C_SLOT_RES, false);
			int cpx = ox + UiKit.SS + 4, cpy = oy + 2;
			if (group.getSpec().hasCount()) {
				g.drawString(font, String.valueOf(co.count), cpx, cpy + 2, UiKit.C_TEXT, false);
				UiKit.drawMiniSpinner(g, font, mx, my, cpx + 16, cpy - 2);
			}
			if (group.getSpec().hasChance()) {
				int chX = cpx + (group.getSpec().hasCount() ? 28 : 0);
				UiKit.drawMiniSpinner(g, font, mx, my, chX, cpy - 2);
				g.drawString(font, co.chance >= 1f ? "100%" : Math.round(co.chance * 100) + "%", chX + 14, cpy + 3, co.isEmpty() ? UiKit.C_LABEL : 0xFFAAFF88, false);
			}
		}
	}

	private static void renderFluids(GuiGraphics g, Font font, SlotGroup group, List<FluidEntry> fluids, int mx, int my) {
		for (int i = 0; i < group.getTotalSlots() && i < fluids.size(); i++) {
			UiKit.slotFluid(g, font, mx, my, fluids.get(i), group.getSlotX(i), group.getSlotY(i), false);
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

	public static void setInputItem(RecipeEditorData d, StationType type, int idx, ItemStack s) {
		switch (type) {
			case CRAFTING -> d.craftGrid.set(idx, s);
			case MECH_CRAFTING -> d.mechGrid.set(idx, s);
			case MIXING -> d.mixIng.set(idx, s);
			case PRESSING -> d.pressIng.set(0, s);
			case CUTTING -> d.cutIn = s;
			case CRUSHING -> d.crushIn = s;
			case FAN -> d.fanIn = s;
			case FURNACE -> d.furnIn = s;
			case STONECUTTER -> d.stoneIn = s;
			case SMITHING -> { if (idx == 0) d.smTemplate = s; else if (idx == 1) d.smBase = s; else if (idx == 2) d.smAddition = s; }
			case DEPLOYING -> { if (idx == 0) d.deployTarget = s; else if (idx == 1) d.deployTool = s; }
			case FILLING -> d.fillIn = s;
		}
	}

	public static void setOutputItem(RecipeEditorData d, StationType type, ItemStack s) {
		if (RecipeEditorData.isTagItem(s)) return;
		switch (type) {
			case CRAFTING -> d.craftResult = s;
			case MECH_CRAFTING -> d.mechResult = s;
			case FURNACE -> d.furnOut = s;
			case STONECUTTER -> d.stoneOut = s;
			case SMITHING -> d.smResult = s;
			case DEPLOYING -> d.deployResult = s;
			case FILLING -> d.fillResult = s;
			default -> {}
		}
	}

	public static void shiftGrid(List<ItemStack> grid, int cols, int rows, int dx, int dy) {
		if (grid == null || grid.isEmpty() || cols <= 0 || rows <= 0) return;
		int minR = rows, maxR = -1, minC = cols, maxC = -1;
		boolean hasAny = false;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int idx = r * cols + c;
				if (idx < grid.size() && !grid.get(idx).isEmpty()) {
					hasAny = true;
					if (r < minR) minR = r; if (r > maxR) maxR = r;
					if (c < minC) minC = c; if (c > maxC) maxC = c;
				}
			}
		}
		if (!hasAny || (minR + dy < 0 || maxR + dy >= rows) || (minC + dx < 0 || maxC + dx >= cols) || (dx == 0 && dy == 0)) return;
		List<ItemStack> old = new java.util.ArrayList<>(grid);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int nr = r - dy, nc = c - dx, toIdx = r * cols + c;
				grid.set(toIdx, (nr >= 0 && nr < rows && nc >= 0 && nc < cols) ? old.get(nr * cols + nc) : ItemStack.EMPTY);
			}
		}
	}
}