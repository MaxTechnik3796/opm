package cz.maxtechnik.opm.client.widget;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
import cz.maxtechnik.opm.client.screen.layout.StationLayoutEngine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class InlineNumberEditor {
	private EditBox editBox = null;
	private String fieldName = null;
	private int fieldIndex = -1;

	public InlineNumberEditor() {}

	public boolean isActive() {
		return editBox != null;
	}


	public void startEdit(Font font, String field, int bx, int by, int bw, String value, int idx, float scrollOffset) {
		this.fieldName = field;
		this.fieldIndex = idx;
		this.editBox = new EditBox(font, bx, by - (int) scrollOffset, bw, 12, Component.empty());
		this.editBox.setValue(value);
		this.editBox.setFocused(true);
		this.editBox.setMaxLength(8);
	}

	public void cancel() {
		this.editBox = null;
		this.fieldName = null;
		this.fieldIndex = -1;
	}

	public void apply(RecipeEditorData data, StationType station) {
		if (editBox == null || fieldName == null) return;
		String value = editBox.getValue().trim();
		try {
			int parsedInt = value.isEmpty() ? 0 : Integer.parseInt(value);
			if (fieldName.endsWith("_out_count")) {
				applyOutCount(StationLayoutEngine.getCrushingOutputsForGroup(data, station), parsedInt);
			} else if (fieldName.endsWith("_out_chance")) {
				applyOutChance(StationLayoutEngine.getCrushingOutputsForGroup(data, station), parsedInt);
			} else if (fieldName.endsWith("Time")) {
				var processingTime = StationLayoutEngine.getLayout(station, data).getProcessingTime();
				if (processingTime != null) processingTime.setValue(parsedInt);
			} else if (fieldName.endsWith("Count")) {
				StationLayoutEngine.setResultCount(data, station, Math.clamp(parsedInt, 1, 64));
			} else switch (fieldName) {
				case "furnXp"        -> data.furnXp = Float.parseFloat(value);
				case "fluid_mix_in"  -> { if (fieldIndex >= 0 && fieldIndex < data.mixFluidIng.size()) data.mixFluidIng.get(fieldIndex).amount = Math.clamp(parsedInt, 1, 1000); }
				case "fluid_mix_out" -> { if (fieldIndex >= 0 && fieldIndex < data.mixFluidOuts.size()) data.mixFluidOuts.get(fieldIndex).amount = Math.clamp(parsedInt, 1, 1000); }
				case "fluid_fill_in" -> data.fillFluid.amount = Math.clamp(parsedInt, 1, 1000);
				case "grid_count"    -> {
					if (fieldIndex >= 0) {
						List<ItemStack> grid = StationLayoutEngine.getItemListForGroup(data, station, true);
						if (grid != null && fieldIndex < grid.size() && !grid.get(fieldIndex).isEmpty()) {
							grid.get(fieldIndex).setCount(Math.clamp(parsedInt, 1, 64));
						}
					}
				}

			}
		} catch (Exception ignored) {}
		cancel();
	}

	private void applyOutCount(List<StationType.CrushingOutput> list, int value) {
		if (list != null && fieldIndex >= 0 && fieldIndex < list.size()) {
			list.get(fieldIndex).count = Math.clamp(value, 1, 64);
		}
	}

	private void applyOutChance(List<StationType.CrushingOutput> list, int percent) {
		if (list != null && fieldIndex >= 0 && fieldIndex < list.size()) {
			list.get(fieldIndex).chance = Math.clamp(percent, 1, 100) / 100F;
		}
	}

	public void render(GuiGraphics g, int mx, int my, float pt) {
		if (editBox != null) editBox.render(g, mx, my, pt);
	}

	public boolean keyPressed(int key, int scan, int mods, RecipeEditorData data, StationType station) {
		if (!isActive()) return false;
		if (key == 257 || key == 335) { apply(data, station); return true; } // Enter
		if (key == 256) { cancel(); return true; }                            // ESC
		editBox.keyPressed(key, scan, mods);
		return true;
	}

	public boolean charTyped(char chr, int mods) {
		if (!isActive()) return false;
		if (Character.isDigit(chr) || (fieldName != null && fieldName.equals("furnXp") && chr == '.')) {
			return editBox.charTyped(chr, mods);
		}
		return true;
	}
}
