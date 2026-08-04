package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
import cz.maxtechnik.opm.client.util.ScaleHelper;
import cz.maxtechnik.opm.client.widget.UiKit;
import net.minecraft.client.gui.Font;

import java.util.List;


/**
 * Modul pro obsluhu interakcí (kliknutí na tlačítka, spinnery, skrolování a dvojklik).
 */
public class StationInteractionHandler {

	public interface EditCallback {
		void edit(String field, int x, int y, int w, String val, int idx);
	}

	private StationInteractionHandler() {}

	public static boolean handleHeaderClicks(StationType type, RecipeEditorData d, int cx, int editorY, int mx, int my) {
		StationLayout layout = StationLayoutFactory.getLayout(type, d);
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
            return subToggle.handleClick(sX, hY + 16, mx, my);
		}
		return false;
	}

	public static boolean handleSpinnerClicks(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = StationLayoutFactory.getLayout(type, d);
		float scale = StationLayoutEngine.getScale(layout, leftWidth);

		int cy = StationLayoutEngine.getContentY(layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		int startX = StationLayoutEngine.getStartX(type, layout, cx);


		if (inG != null) inG.setAnchor(startX, cy);
		if (outG != null && inG != null) {
			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = startX + inG.getWidth() + extraW + 15;
			int outputX = arrowX + 25;
			int outputY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(outputX, outputY);
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<net.minecraft.world.item.ItemStack> items = StationItemBridge.getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < items.size(); i++) {
				net.minecraft.world.item.ItemStack s = items.get(i);
				if (s.isEmpty()) continue;
				int cpx = inG.getSlotX(i) + UiKit.SS + 4, cpy = inG.getSlotY(i) + 2;
				if (UiKit.hit(mx, my, cpx + 14, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
					s.setCount(Math.min(64, s.getCount() + 1));
					return true;
				}
				if (UiKit.hit(mx, my, cpx + 14, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
					s.setCount(Math.max(1, s.getCount() - 1));
					return true;
				}
			}
		}

		if (outG != null) {
			List<CrushingOutput> outputs = StationItemBridge.getCrushingOutputsForGroup(d, type);
			if (outputs != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < outputs.size(); i++) {
					int cpx = outG.getSlotX(i) + UiKit.SS + 4, cpy = outG.getSlotY(i) + 2;
					if (miniCountChance(mx, my, cpx + 16, cpx + 28, cpy, outputs.get(i), outG.getSpec().hasChance())) return true;
				}
			} else if (outG.getSpec().hasCount()) {
				int resCount = StationItemBridge.getResultCount(d, type);
				int cpx = outG.getSlotX(0) + UiKit.SS + 4, cpy = outG.getSlotY(0) + 2;
				if (UiKit.hit(mx, my, cpx + 16, cpy - 2, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
					StationItemBridge.setResultCount(d, type, Math.min(64, resCount + 1));
					return true;
				}
				if (UiKit.hit(mx, my, cpx + 16, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
					StationItemBridge.setResultCount(d, type, Math.max(1, resCount - 1));
					return true;
				}
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int spinX = cx + 45;
			if (UiKit.hit(mx, my, spinX, cy + 2, UiKit.SPIN_W, UiKit.SPIN_H)) {
				pTime.increment();
				return true;
			}
			if (UiKit.hit(mx, my, spinX, cy + 10, UiKit.SPIN_W, UiKit.SPIN_H)) {
				pTime.decrement();
				return true;
			}
		}

		if (type == StationType.FURNACE) {
			int spinX = cx - 20;
			if (UiKit.hit(mx, my, spinX, cy + 2, UiKit.SPIN_W, UiKit.SPIN_H)) {
				d.furnXp = Math.clamp(d.furnXp + 0.1f, 0.0f, 100.0f);
				return true;
			}
			if (UiKit.hit(mx, my, spinX, cy + 10, UiKit.SPIN_W, UiKit.SPIN_H)) {
				d.furnXp = Math.clamp(d.furnXp - 0.1f, 0.0f, 100.0f);
				return true;
			}
		}
		return false;
	}

	public static boolean handleFluidSpins(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my) {
		StationLayout layout = StationLayoutFactory.getLayout(type, d);
		float scale = StationLayoutEngine.getScale(layout, leftWidth);

		int cy = StationLayoutEngine.getContentY(layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();

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
			List<FluidEntry> fList = StationItemBridge.getFluidInputs(d, type);
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
			List<FluidEntry> fList = StationItemBridge.getFluidOutputs(d, type);
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

	public static boolean handleScrollSpinners(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, double delta) {
		StationLayout layout = StationLayoutFactory.getLayout(type, d);
		float scale = StationLayoutEngine.getScale(layout, leftWidth);

		int cy = StationLayoutEngine.getContentY(layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		int step = delta > 0 ? 1 : -1;
		int fluidStep = delta > 0 ? 250 : -250;

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		int startX = StationLayoutEngine.getStartX(type, layout, cx);


		if (inG != null) inG.setAnchor(startX, cy);
		if (outG != null && inG != null) {
			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = startX + inG.getWidth() + extraW + 15;
			int outputX = arrowX + 25;
			int outputY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(outputX, outputY);
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<net.minecraft.world.item.ItemStack> items = StationItemBridge.getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < items.size(); i++) {
				net.minecraft.world.item.ItemStack s = items.get(i);
				if (s.isEmpty()) continue;
				int cpx = inG.getSlotX(i) + UiKit.SS + 4, cpy = inG.getSlotY(i) + 2;
				if (UiKit.hit(mx, my, cpx, cpy - 2, 45, 18)) {
					s.setCount(Math.clamp(s.getCount() + step, 1, 64));
					return true;
				}
			}
		}

		if (outG != null) {
			List<CrushingOutput> outputs = StationItemBridge.getCrushingOutputsForGroup(d, type);
			if (outputs != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < outputs.size(); i++) {
					int cpx = outG.getSlotX(i) + UiKit.SS + 4, cpy = outG.getSlotY(i) + 2;
					CrushingOutput co = outputs.get(i);
					if (UiKit.hit(mx, my, cpx, cpy - 2, 26, 18)) {
						co.count = Math.clamp(co.count + step, 1, 64);
						return true;
					}
					if (outG.getSpec().hasChance() && UiKit.hit(mx, my, cpx + 28, cpy - 2, 45, 18)) {
						float newChance = (float) Math.round((co.chance + step * 0.05f) * 100) / 100f;
						co.chance = Math.clamp(newChance, 0.01f, 1.0f);
						return true;
					}
				}
			} else if (outG.getSpec().hasCount()) {
				int resCount = StationItemBridge.getResultCount(d, type);
				int cpx = outG.getSlotX(0) + UiKit.SS + 4, cpy = outG.getSlotY(0) + 2;
				if (UiKit.hit(mx, my, cpx, cpy - 2, 45, 18)) {
					StationItemBridge.setResultCount(d, type, Math.clamp(resCount + step, 1, 64));
					return true;
				}
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tValX = cx - 15, spinX = cx + 45;
			if (UiKit.hit(mx, my, tValX - 2, cy + 2, spinX + UiKit.SPIN_W - tValX + 4, 18)) {
				pTime.setValue(pTime.getValue() + step * 10);
				return true;
			}
		}

		if (type == StationType.FURNACE) {
			int xpX = cx - 45, spinX = cx - 20;
			if (UiKit.hit(mx, my, xpX - 2, cy + 2, spinX + UiKit.SPIN_W - xpX + 4, 18)) {
				d.furnXp = (float) Math.round(Math.clamp(d.furnXp + step * 0.1f, 0.0f, 100.0f) * 10f) / 10f;
				return true;
			}
		}

		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		int itemAreaH = inG != null ? (outG != null ? Math.max(inG.getHeight(), outG.getHeight()) : inG.getHeight()) : 0;
		int fluidY = cy + itemAreaH + 15 + (type == StationType.MIXING ? 2 : 0);

		if (inF != null) {
			inF.setAnchor(cx - 150, fluidY);
			List<FluidEntry> fList = StationItemBridge.getFluidInputs(d, type);
			for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (f.isEmpty()) continue;
				int amtX = inF.getSlotX(i) + UiKit.SS + 4, amtY = inF.getSlotY(i) + 4;
				if (UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 22)) {
					f.amount = Math.clamp(f.amount + fluidStep, 1, 1000);
					return true;
				}
			}
		}

		if (outF != null) {
			outF.setAnchor(cx + 10, fluidY);
			List<FluidEntry> fList = StationItemBridge.getFluidOutputs(d, type);
			for (int i = 0; i < outF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (f.isEmpty()) continue;
				int amtX = outF.getSlotX(i) + UiKit.SS + 4, amtY = outF.getSlotY(i) + 4;
				if (UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 22)) {
					f.amount = Math.clamp(f.amount + fluidStep, 1, 1000);
					return true;
				}
			}
		}
		return false;
	}

	public static boolean handleDoubleClick(StationType type, RecipeEditorData d, int cx, int leftWidth, int editorY, int mx, int my, EditCallback callback) {
		StationLayout layout = StationLayoutFactory.getLayout(type, d);
		float scale = StationLayoutEngine.getScale(layout, leftWidth);

		int cy = StationLayoutEngine.getContentY(layout, editorY);

		mx = ScaleHelper.transformMouseX(mx, cx, scale);
		my = ScaleHelper.transformMouseY(my, cy, scale);

		SlotGroup inG = layout.getInputSlots();
		SlotGroup outG = layout.getOutputSlots();
		int startX = StationLayoutEngine.getStartX(type, layout, cx);


		if (inG != null) inG.setAnchor(startX, cy);
		if (outG != null && inG != null) {
			int extraW = inG.getSpec().hasCount() ? 24 : 0;
			int arrowX = startX + inG.getWidth() + extraW + 15;
			int outputX = arrowX + 25;
			int outputY = (inG.getHeight() > outG.getHeight()) ? (cy + inG.getHeight() / 2 - outG.getHeight() / 2) : cy;
			outG.setAnchor(outputX, outputY);
		}

		if (inG != null && inG.getSpec().hasCount()) {
			List<net.minecraft.world.item.ItemStack> items = StationItemBridge.getItemListForGroup(d, type, true);
			for (int i = 0; i < inG.getTotalSlots() && i < items.size(); i++) {
				int cpx = inG.getSlotX(i) + UiKit.SS + 4, cpy = inG.getSlotY(i) + 2;
				if (UiKit.hit(mx, my, cpx - 2, cpy - 2, 18, 14)) {
					int displayCount = items.get(i).isEmpty() ? 1 : items.get(i).getCount();
					callback.edit("in_count", cpx - 2, cpy - 2, 18, String.valueOf(displayCount), i);
					return true;
				}
			}
		}

		if (outG != null) {
			List<CrushingOutput> outputs = StationItemBridge.getCrushingOutputsForGroup(d, type);
			if (outputs != null) {
				for (int i = 0; i < outG.getTotalSlots() && i < outputs.size(); i++) {
					int cpx = outG.getSlotX(i) + UiKit.SS + 4, cpy = outG.getSlotY(i) + 2;
					CrushingOutput co = outputs.get(i);
					if (UiKit.hit(mx, my, cpx - 2, cpy - 2, 18, 14)) {
						callback.edit("crush_out_count", cpx - 2, cpy - 2, 18, String.valueOf(co.count), i);
						return true;
					}
					if (outG.getSpec().hasChance() && UiKit.hit(mx, my, cpx + 26, cpy - 2, 35, 14)) {
						int pct = Math.round(co.chance * 100);
						callback.edit("crush_out_chance", cpx + 26, cpy - 2, 32, String.valueOf(pct), i);
						return true;
					}
				}
			} else if (outG.getSpec().hasCount()) {
				int resCount = StationItemBridge.getResultCount(d, type);
				int cpx = outG.getSlotX(0) + UiKit.SS + 4, cpy = outG.getSlotY(0) + 2;
				if (UiKit.hit(mx, my, cpx - 2, cpy - 2, 18, 14)) {
					callback.edit("res_count", cpx - 2, cpy - 2, 18, String.valueOf(resCount), 0);
					return true;
				}
			}
		}

		ProcessingTime pTime = layout.getProcessingTime();
		if (pTime != null) {
			int tValX = cx - 15;
			if (UiKit.hit(mx, my, tValX - 2, cy + 2, 45, 14)) {
				callback.edit(type.name().toLowerCase() + "Time", tValX - 2, cy + 2, 45, String.valueOf(pTime.getValue()), 0);
				return true;
			}
		}

		if (type == StationType.FURNACE) {
			int xpX = cx - 45;
			if (UiKit.hit(mx, my, xpX - 2, cy + 2, 24, 14)) {
				callback.edit("furnXp", xpX - 2, cy + 2, 24, String.format(java.util.Locale.ROOT, "%.1f", d.furnXp), 0);
				return true;
			}
		}

		SlotGroup inF = layout.getInputFluids();
		SlotGroup outF = layout.getOutputFluids();
		int itemAreaH = inG != null ? (outG != null ? Math.max(inG.getHeight(), outG.getHeight()) : inG.getHeight()) : 0;
		int fluidY = cy + itemAreaH + 15 + (type == StationType.MIXING ? 2 : 0);

		if (inF != null) {
			inF.setAnchor(cx - 150, fluidY);
			List<FluidEntry> fList = StationItemBridge.getFluidInputs(d, type);
			for (int i = 0; i < inF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (!f.isEmpty()) {
					int amtX = inF.getSlotX(i) + UiKit.SS + 4, amtY = inF.getSlotY(i) + 4;
					if (UiKit.hit(mx, my, amtX - 2, amtY - 2, 50, 14)) {
						callback.edit("fluid_mix_in", amtX - 2, amtY - 2, 45, String.valueOf(f.amount), i);
						return true;
					}
				}
			}
		}

		if (outF != null) {
			outF.setAnchor(cx + 10, fluidY);
			List<FluidEntry> fList = StationItemBridge.getFluidOutputs(d, type);
			for (int i = 0; i < outF.getTotalSlots() && i < fList.size(); i++) {
				FluidEntry f = fList.get(i);
				if (!f.isEmpty()) {
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
				co.chance = (float) Math.round(Math.min(1.0f, co.chance + 0.05f) * 100) / 100f;
				return true;
			}
			if (UiKit.hit(mx, my, chanceX, cpy + 7, UiKit.MINI_SPIN, UiKit.MINI_SPIN)) {
				co.chance = (float) Math.round(Math.max(0.01f, co.chance - 0.05f) * 100) / 100f;
				return true;
			}
		}
		return false;
	}
}