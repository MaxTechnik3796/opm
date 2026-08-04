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
 * Hlavní modul pro stavbu rozvržení (layoutu) receptů jednotlivých stanic,
 * výpočet dynamického vycentrování, škálování a obsluhu interakcí uživatele.
 */
public class StationLayoutEngine {

	private StationLayoutEngine() {}

	public interface EditCallback {
		void edit(String fieldName, int bx, int by, int bw, String val, int idx);
	}

	// ─── GENERÁTORY VYMEZENÍ LAYOUTŮ STANIC ─────────────────────────────

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
					.input(SlotGroup.row(3, SlotSpec.item()).withSeparator("+"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
			case MIXING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Mixer", "Press"}, () -> d.mixBasinPress ? 1 : 0, i -> d.mixBasinPress = (i == 1)))
					.subToggle(ToggleGroup.colored(d.heatLabels, () -> d.mixHeat, i -> d.mixHeat = i, new int[]{UiKit.C_BTN, 0xFF4A2000, 0xFF6A0000}))
					.input(SlotGroup.grid(3, 3, SlotSpec.item()))
					.output(SlotGroup.col(4, SlotSpec.result().withCount().withChance(), "Outputs"))
					.inputFluids(SlotGroup.col(2, SlotSpec.fluid(), "Input Fluids"))
					.outputFluids(SlotGroup.col(2, SlotSpec.fluid(), "Result Fluids"))
					.processingTime(ProcessingTime.standard(() -> d.mixTime, v -> d.mixTime = v))
					.build();
			case PRESSING -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(new String[]{"Press", "Mixer"}, () -> d.mixBasinPress ? 0 : 1, i -> d.mixBasinPress = (i == 0)))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.col(4, SlotSpec.result().withCount().withChance(), "Outputs"))
					.processingTime(ProcessingTime.standard(() -> d.pressTime, v -> d.pressTime = v))
					.build();
			case CUTTING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.col(4, SlotSpec.result().withCount().withChance(), "Outputs"))
					.processingTime(ProcessingTime.standard(() -> d.cutTime, v -> d.cutTime = v))
					.build();
			case CRUSHING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.col(4, SlotSpec.result().withCount().withChance(), "Outputs"))
					.processingTime(ProcessingTime.standard(() -> d.crushTime, v -> d.crushTime = v))
					.build();
			case FAN -> StationLayout.builder()
					.headerToggle(ToggleGroup.of(d.fanLabels, () -> d.fanSubIdx, i -> d.fanSubIdx = i))
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.output(SlotGroup.col(4, SlotSpec.result().withCount().withChance(), "Outputs"))
					.processingTime(ProcessingTime.standard(() -> d.fanTime, v -> d.fanTime = v))
					.build();
			case DEPLOYING -> StationLayout.builder()
					.input(SlotGroup.row(2, SlotSpec.item()).withSeparator("+"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
			case FILLING -> StationLayout.builder()
					.input(SlotGroup.single(SlotSpec.item(), "Input"))
					.inputFluids(SlotGroup.single(SlotSpec.fluid(), "Fluid"))
					.output(SlotGroup.single(SlotSpec.result().withCount(), "Result"))
					.build();
		};
	}

	// ─── VÝPOČET ROZMĚRŮ A ŠKÁLOVÁNÍ ──────────────────────────────────

	public static int getContentY(StationType type, StationLayout layout, int editorY) {
		return editorY + 35;
	}

	public static int getLayoutTotalWidth(StationType type, RecipeEditorData d, StationLayout layout) {
		if (type == StationType.FILLING) {
			return UiKit.SS + 14 + (UiKit.SS + 60) + 25 + UiKit.SS + 24;
		}
		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		if (inG != null && outG != null) {
			int extraInW = inG.getSpec().hasCount() ? 24 : 0;
			int extraOutW = outG.getSpec().hasCount() ? (outG.getSpec().hasChance() ? 80 : 24) : 0;
			return inG.getWidth() + extraInW + 15 + 25 + outG.getWidth() + extraOutW;
		}
		return 300;
	}

	public static int getStartX(StationType type, RecipeEditorData d, StationLayout layout, int cx) {
		int totalW = getLayoutTotalWidth(type, d, layout);
		return cx - totalW / 2;
	}

	public static float getScale(StationType type, RecipeEditorData d, StationLayout layout, int leftWidth) {
		int totalWidth = getLayoutTotalWidth(type, d, layout);
		return ScaleHelper.getStationScale(totalWidth, leftWidth - 24);
	}

	// ─── VYKRESLOVÁNÍ STANICE ──────────────────────────────────────────

	public static int renderStation(GuiGraphics g, Font font, StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, boolean isDragging) {
		StationLayout layout = getLayout(type, d);

		// Horní tlačítka (Toggles)
		int hY = editorY + 4;
		ToggleGroup hToggle = layout.getHeaderToggle();
		if (hToggle != null) {
			int hW = hToggle.calcWidth(font);
			int hX = cx - hW / 2;
			hToggle.render(g, font, hX, hY, mx, my);
		}

		ToggleGroup subToggle = layout.getSubToggle();
		if (subToggle != null) {
			int sW = subToggle.calcWidth(font);
			int sX = cx - sW / 2;
			subToggle.render(g, font, sX, hY + 16, mx, my);
		}

		float scale = getScale(type, d, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);

		boolean scaled = ScaleHelper.pushPoseScale(g, scale, cx, cy);
		if (scaled) {
			mx = ScaleHelper.transformMouseX(mx, cx, scale);
			my = ScaleHelper.transformMouseY(my, cy, scale);
		}

		// Rychlé přizpůsobení pro FILLING
		if (type == StationType.FILLING) {
			int sx = getStartX(type, d, layout, cx);
			int itemInX = sx;
			int fluidX = sx + 34;
			int arrowX = fluidX + UiKit.SS + 60 + 5;
			int rx = arrowX + 20;

			g.drawString(font, "Input", itemInX, cy - 12, UiKit.C_LABEL, false);
			renderSlotGroupItems(g, font, layout.getInputSlots(), List.of(d.fillIn), mx, my, isDragging);

			g.drawString(font, "+", itemInX + UiKit.SS + 3, cy + 4, UiKit.C_LABEL, false);

			g.drawString(font, "Fluid", fluidX, cy - 12, UiKit.C_LABEL, false);
			UiKit.slotFluid(g, font, mx, my, d.fillFluid, fluidX, cy, isDragging);
			int amtX = fluidX + UiKit.SS + 4, amtY = cy + 4;
			g.drawString(font, d.fillFluid.amount + " mB", amtX, amtY, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, amtX - 2, amtY + 12);

			g.drawString(font, "➔", arrowX, cy + 4, UiKit.C_LABEL, false);

			g.drawString(font, "Result", rx, cy - 12, UiKit.C_LABEL, false);
			UiKit.slot(g, font, mx, my, d.fillResult, rx, cy, UiKit.C_SLOT_RES, isDragging);
			int cpx = rx + UiKit.SS + 4, cpy = cy + 2;
			g.drawString(font, String.valueOf(d.fillResult.isEmpty() ? 1 : d.fillResult.getCount()), cpx, cpy + 2, UiKit.C_TEXT, false);
			UiKit.drawMiniSpinner(g, font, mx, my, cpx + 14, cpy - 2);

			cy += UiKit.SS + 35;
			int totalH = cy - editorY;
			ScaleHelper.popPoseScale(g, scaled);
			return totalH;
		}

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();

		if (inG != null && outG != null) {
			int sx = getStartX(type, d, layout, cx);
			inG.setAnchor(sx, cy);

			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = sx + inG.getWidth() + extraW + 15;
			int arrowY = cy + inG.getHeight() / 2 - 4;
			int rx = arrowX + 25;
			int outY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(rx, outY);

			g.drawString(font, inG.getLabel() != null ? inG.getLabel() : "Inputs:", sx, cy - 12, UiKit.C_LABEL, false);
			List<ItemStack> inList = getItemListForGroup(d, type, true);
			renderSlotGroupItems(g, font, inG, inList, mx, my, isDragging);

			g.drawString(font, "➔", arrowX, arrowY, UiKit.C_LABEL, false);

			g.drawString(font, outG.getLabel() != null ? outG.getLabel() : "Result:", rx, outY - 12, UiKit.C_LABEL, false);

			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				renderSlotGroupOutputs(g, font, outG, crushOuts, mx, my, isDragging);
			} else {
				ItemStack res = getResultItem(d, type);
				UiKit.slot(g, font, mx, my, res, rx, outY, UiKit.C_SLOT_RES, isDragging);

				int cpx = rx + UiKit.SS + 4, cpy = outY + 2;
				int displayCount = res.isEmpty() ? getResultCount(d, type) : res.getCount();
				g.drawString(font, String.valueOf(displayCount), cpx, cpy + 2, UiKit.C_TEXT, false);
				UiKit.drawMiniSpinner(g, font, mx, my, cpx + 14, cpy - 2);
			}

			cy += Math.max(inG.getHeight(), outG.getHeight()) + 15;
		} else {
			if (inG != null) {
				int sx = cx - inG.getWidth() / 2;
				inG.setAnchor(sx, cy);
				List<ItemStack> inList = getItemListForGroup(d, type, true);
				renderSlotGroupItems(g, font, inG, inList, mx, my, isDragging);
				cy += inG.getHeight() + 15;
			}
		}

		if (inF != null || outF != null) {
			int sx = cx - 150;
			int rx = cx + 10;
			int fluidY = cy + (type == StationType.MIXING ? 2 : 0);
			if (inF != null) {
				inF.setAnchor(sx, fluidY);
				g.drawString(font, inF.getLabel() != null ? inF.getLabel() : "Input Fluids:", sx, fluidY - 12, UiKit.C_LABEL, false);
				List<FluidEntry> fList = getFluidInputs(d, type);
				for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
					UiKit.slotFluid(g, font, mx, my, fList.get(i), inF.getSlotX(i), inF.getSlotY(i), isDragging);
				}
			}
			if (outF != null) {
				outF.setAnchor(rx, fluidY);
				g.drawString(font, outF.getLabel() != null ? outF.getLabel() : "Result Fluids:", rx, fluidY - 12, UiKit.C_LABEL, false);
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
			g.drawString(font, String.format(java.util.Locale.ROOT, "%.1f", d.furnXp), cx - 45, cy + 4, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, cx - 20, cy + 2);
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tX = (type == StationType.FURNACE) ? (isCampfire ? cx - 35 : cx + 10) : cx - 35;
			int tValX = (type == StationType.FURNACE) ? (isCampfire ? cx : cx + 45) : cx;
			int spinX = (type == StationType.FURNACE) ? (isCampfire ? cx + 35 : cx + 80) : cx + 55;
			g.drawString(font, "Time:", tX, cy + 4, UiKit.C_LABEL, false);
			g.drawString(font, pTime.getValue() + " t", tValX, cy + 4, UiKit.C_TEXT, false);
			UiKit.valSpinner(g, font, mx, my, spinX, cy + 2);
			cy += 25;
		}

		int totalH = cy - editorY;
		ScaleHelper.popPoseScale(g, scaled);
		return totalH;
	}

	// ─── OBSLUHA INTERAKCÍ & KLIKNUTÍ ──────────────────────────────────

	public static boolean handleHeaderClicks(StationType type, RecipeEditorData d, int cx, int editorY, int mx, int my) {
		StationLayout layout = getLayout(type, d);
		Font font = net.minecraft.client.Minecraft.getInstance().font;
		int hY = editorY + 4;
		ToggleGroup hToggle = layout.getHeaderToggle();
		if (hToggle != null) {
			int hW = hToggle.calcWidth(font);
			int hX = cx - hW / 2;
			if (hToggle.handleClick(hX, hY, mx, my)) return true;
		}
		ToggleGroup subToggle = layout.getSubToggle();
		if (subToggle != null) {
			int sW = subToggle.calcWidth(font);
			int sX = cx - sW / 2;
			if (subToggle.handleClick(sX, hY + 16, mx, my)) return true;
		}
		return false;
	}


	public static boolean handleSpinnerClicks(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		if (inG != null && outG != null) {
			int sx = getStartX(type, d, layout, cx);
			inG.setAnchor(sx, cy);

			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = sx + inG.getWidth() + extraW + 15;
			int rx = arrowX + 25;
			int outY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(rx, outY);

			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					CrushingOutput co = crushOuts.get(i);
					if (co.isEmpty()) continue;
					int ox = outG.getSlotX(i), oy = outG.getSlotY(i);
					int cpx = ox + UiKit.SS + 4, cpy = oy + 2, chX = cpx + (outG.getSpec().hasChance() ? 28 : 0);
					if (miniCountChance(mx, my, cpx + 16, chX, cpy, co, outG.getSpec().hasChance())) return true;
				}
			} else {
				int spinX = rx + UiKit.SS + 6, spinY = outG.getAnchorY() + 2;
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
			int spinX = (type == StationType.FURNACE) ? (isCampfire ? cx + 35 : cx + 80) : cx + 55;
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

	public static boolean handleFluidSpins(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		if (inF == null && outF == null) return false;

		int cy = getContentY(type, layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		if (type == StationType.FILLING) {
			int sx = getStartX(type, d, layout, cx);
			int fluidX = sx + 34;
			if (inF != null) inF.setAnchor(fluidX, cy);
			FluidEntry f = d.fillFluid;
			if (f.isEmpty()) return false;
			int amtX = fluidX + UiKit.SS + 4, amtY = cy + 4;
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

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		if (inG != null || outG != null) {
			int inH = inG != null ? inG.getHeight() : 0;
			int outH = outG != null ? outG.getHeight() : 0;
			cy += Math.max(inH, outH) + 15 + (type == StationType.MIXING ? 2 : 0);
		}

		if (inF != null) inF.setAnchor(cx - 150, cy);
		if (outF != null) outF.setAnchor(cx + 10, cy);

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

	public static boolean handleScrollSpinners(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, double sy) {
		StationLayout layout = getLayout(type, d);
		float scale = getScale(type, d, layout, leftWidth);
		int cy = getContentY(type, layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		if (type == StationType.FILLING) {
			int sx = getStartX(type, d, layout, cx);
			int fluidX = sx + 34;
			int rx = sx + 147;
			if (UiKit.hit(mx, my, fluidX, cy, UiKit.SS + 60, UiKit.SS + 12)) {
				if (!d.fillFluid.isEmpty()) {
					d.fillFluid.amount = Math.clamp(d.fillFluid.amount + (sy > 0 ? 250 : -250), 1, 1000);
					return true;
				}
			}
			if (UiKit.hit(mx, my, rx, cy, UiKit.SS, UiKit.SS)) {
				if (!getResultItem(d, type).isEmpty()) {
					setResultCount(d, type, Math.clamp(getResultCount(d, type) + (int) sy, 1, 64));
					return true;
				}
			}
			return false;
		}

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();

		if (inG != null && outG != null) {
			int sx = getStartX(type, d, layout, cx);
			inG.setAnchor(sx, cy);

			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = sx + inG.getWidth() + extraW + 15;
			int rx = arrowX + 25;
			int outY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(rx, outY);

			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					if (UiKit.hit(mx, my, outG.getSlotX(i), outG.getSlotY(i), UiKit.SS, UiKit.SS)) {
						CrushingOutput co = crushOuts.get(i);
						if (!co.isEmpty()) {
							co.count = Math.clamp(co.count + (int) sy, 1, 64);
							return true;
						}
					}
				}
			} else {
				if (UiKit.hit(mx, my, rx, outG.getAnchorY(), UiKit.SS, UiKit.SS)) {
					if (!getResultItem(d, type).isEmpty()) {
						setResultCount(d, type, Math.clamp(getResultCount(d, type) + (int) sy, 1, 64));
						return true;
					}
				}
			}
		}

		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		if (inF != null) {
			List<FluidEntry> fList = getFluidInputs(d, type);
			for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (f.isEmpty()) continue;
				int sx = inF.getSlotX(i), sy2 = inF.getSlotY(i);
				if (UiKit.hit(mx, my, sx, sy2, UiKit.SS + 60, UiKit.SS + 12)) {
					f.amount = Math.clamp(f.amount + (sy > 0 ? 250 : -250), 1, 1000);
					return true;
				}
			}
		}
		if (outF != null) {
			List<FluidEntry> fList = getFluidOutputs(d, type);
			for (int i = 0; i < outF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (f.isEmpty()) continue;
				int sx = outF.getSlotX(i), sy2 = outF.getSlotY(i);
				if (UiKit.hit(mx, my, sx, sy2, UiKit.SS + 60, UiKit.SS + 12)) {
					f.amount = Math.clamp(f.amount + (sy > 0 ? 250 : -250), 1, 1000);
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

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		if (type == StationType.FILLING) {
			int sx = getStartX(type, d, layout, cx);
			int fluidX = sx + 34;
			int rx = sx + 147;
			int amtX = fluidX + UiKit.SS + 4, amtY = cy + 4;
			if (!d.fillFluid.isEmpty() && UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 14)) {
				callback.edit("fluid_fill_in", amtX - 2, amtY - 2, 45, String.valueOf(d.fillFluid.amount), -1);
				return true;
			}
			int cpx = rx + UiKit.SS + 6, cpy = cy + 2;
			if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) {
				callback.edit("filling_count", cpx - 4, cpy, 20, String.valueOf(getResultCount(d, type)), -1);
				return true;
			}
			return false;
		}

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		if (inG != null && outG != null) {
			int sx = getStartX(type, d, layout, cx);
			inG.setAnchor(sx, cy);

			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = sx + inG.getWidth() + extraW + 15;
			int rx = arrowX + 25;
			int outY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(rx, outY);

			List<CrushingOutput> crushOuts = getCrushingOutputsForGroup(d, type);
			if (crushOuts != null) {
				String prefix = type.name().toLowerCase() + "_out_";
				for (int i = 0; i < outG.getTotalSlots() && i < crushOuts.size(); i++) {
					CrushingOutput co = crushOuts.get(i);
					if (co.isEmpty()) continue;
					int ox = outG.getSlotX(i), oy = outG.getSlotY(i);
					int cpx = ox + UiKit.SS + 4, cpy = oy + 2, chX = cpx + 28;
					if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) {
						callback.edit(prefix + "count", cpx - 4, cpy, 20, String.valueOf(co.count), i);
						return true;
					}
					if (outG.getSpec().hasChance() && UiKit.hit(mx, my, chX + 11, cpy + 1, 26, 12)) {
						callback.edit(prefix + "chance", chX + 8, cpy + 1, 26, String.valueOf((int) (co.chance * 100)), i);
						return true;
					}
				}
			} else {
				int cpx = rx + UiKit.SS + 6, cpy = outG.getAnchorY() + 2;
				if (UiKit.hit(mx, my, cpx, cpy + 2, 14, 12)) {
					callback.edit(type.name().toLowerCase() + "_count", cpx - 4, cpy, 20, String.valueOf(getResultCount(d, type)), -1);
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
						callback.edit("grid_count", cpx - 2, cpy, 20, String.valueOf(stack.getCount()), i);
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
				callback.edit("furnXp", cx - 45, timeY + 4, 30, String.format(java.util.Locale.ROOT, "%.1f", d.furnXp), -1);
				return true;
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tValX = (type == StationType.FURNACE) ? (isCampfire ? cx : cx + 45) : cx;
			if (UiKit.hit(mx, my, tValX, timeY + 4, 35, 12)) {
				callback.edit(type.name().toLowerCase() + "Time", tValX, timeY + 4, 35, String.valueOf(pTime.getValue()), -1);
				return true;
			}
		}

		if (inF != null || outF != null) {
			int fluidCy = cy + Math.max(inG != null ? inG.getHeight() : 0, outG != null ? outG.getHeight() : 0) + 15 + (type == StationType.MIXING ? 2 : 0);
			if (inF != null) inF.setAnchor(cx - 150, fluidCy);
			if (outF != null) outF.setAnchor(cx + 10, fluidCy);

			if (inF != null) {
				List<FluidEntry> fList = getFluidInputs(d, type);
				for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
					FluidEntry f = fList.get(i);
					if (f.isEmpty()) continue;
					int amtX = inF.getSlotX(i) + UiKit.SS + 4, amtY = inF.getSlotY(i) + 4;
					if (UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 14)) {
						String field = (type == StationType.FILLING) ? "fluid_fill_in" : "fluid_mix_in";
						callback.edit(field, amtX - 2, amtY - 2, 45, String.valueOf(f.amount), i);
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
						callback.edit("fluid_mix_out", amtX - 2, amtY - 2, 45, String.valueOf(f.amount), i);
						return true;
					}
				}
			}
		}
		return false;
	}

	// ─── POMOCNÉ METODY ───────────────────────────────────────────────

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

	private static int getResultCount(RecipeEditorData d, StationType type) {
		return switch (type) {
			case CRAFTING, MECH_CRAFTING -> d.craftCount;
			case FURNACE -> d.furnCount;
			case STONECUTTER -> d.stoneCount;
			case SMITHING -> d.smCount;
			default -> 1;
		};
	}

	public static void setResultCount(RecipeEditorData d, StationType type, int count) {
		switch (type) {
			case CRAFTING, MECH_CRAFTING -> d.craftCount = count;
			case FURNACE -> d.furnCount = count;
			case STONECUTTER -> d.stoneCount = count;
			case SMITHING -> d.smCount = count;
		}
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
			g.drawString(font, String.valueOf(co.count), cpx, cpy + 2, UiKit.C_TEXT, false);
			UiKit.drawMiniSpinner(g, font, mx, my, cpx + 16, cpy - 2);

			if (group.getSpec().hasChance()) {
				int chX = cpx + 28;
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
			case CRAFTING, MECH_CRAFTING -> d.craftResult;
			case FURNACE -> d.furnOut;
			case STONECUTTER -> d.stoneOut;
			case SMITHING -> d.smResult;
			case DEPLOYING -> d.deployResult;
			case FILLING -> d.fillResult;
			default -> ItemStack.EMPTY;
		};
	}

	public static List<FluidEntry> getFluidInputs(RecipeEditorData d, StationType type) {
		return switch (type) {
			case MIXING -> d.mixFluidIng;
			case FILLING -> List.of(d.fillFluid);
			default -> List.of();
		};
	}

	public static List<FluidEntry> getFluidOutputs(RecipeEditorData d, StationType type) {
		return switch (type) {
			case MIXING -> d.mixFluidOuts;
			default -> List.of();
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
			case SMITHING -> {
				if (idx == 0) d.smTemplate = s;
				else if (idx == 1) d.smBase = s;
				else if (idx == 2) d.smAddition = s;
			}
			case DEPLOYING -> {
				if (idx == 0) d.deployTarget = s;
				else if (idx == 1) d.deployTool = s;
			}
			case FILLING -> d.fillIn = s;
		}
	}

	public static void setOutputItem(RecipeEditorData d, StationType type, ItemStack s) {
		switch (type) {
			case CRAFTING, MECH_CRAFTING -> d.craftResult = s;
			case FURNACE -> d.furnOut = s;
			case STONECUTTER -> d.stoneOut = s;
			case SMITHING -> d.smResult = s;
			case DEPLOYING -> d.deployResult = s;
			case FILLING -> d.fillResult = s;
			default -> {}
		}
	}
}