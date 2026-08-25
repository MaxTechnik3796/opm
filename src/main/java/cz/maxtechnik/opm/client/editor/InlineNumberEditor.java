package cz.maxtechnik.opm.client.editor;
import cz.maxtechnik.opm.client.ui.UiKit;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.editor.RecipeEditorData;
import cz.maxtechnik.opm.client.editor.layout.StationLayoutEngine;
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

	public boolean mouseClicked(int localMx, int localMy, int button) {
		if (editBox == null) return false;
		if (UiKit.hit(localMx, localMy, editBox.getX() - 1, editBox.getY() - 1, editBox.getWidth() + 2, editBox.getHeight() + 2)) {
			return editBox.mouseClicked(localMx, localMy, button);
		}
		return false;
	}

	public void startEdit(Font font, String field, int bx, int by, int bw, String value, int idx) {
		this.fieldName = field;
		this.fieldIndex = idx;
		this.editBox = new EditBox(font, bx, by, bw, 12, Component.empty());
		this.editBox.setBordered(false);
		this.editBox.setValue(value);
		this.editBox.setFocused(true);
		this.editBox.setMaxLength(8);
		this.editBox.setTextColor(0xFFFFFFFF);
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

	public void render(GuiGraphics g, int mx, int my, float pt, int cx, int cy, float scale) {
		if (editBox != null) {
			boolean scaled = scale < 0.99f && scale > 0;
			if (scaled) {
				g.pose().pushPose();
				g.pose().translate(cx, cy, 0);
				g.pose().scale(scale, scale, 1.0f);
				g.pose().translate(-cx, -cy, 0);
			}

			int x = editBox.getX();
			int y = editBox.getY();
			int w = editBox.getWidth();
			int h = editBox.getHeight();

			// Solid background fill matching editor background (0xFF222222) with ZERO borders for seamless text illusion
			g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF222222);

			int localMx = (scaled) ? (int) (cx + (mx - cx) / scale) : mx;
			int localMy = (scaled) ? (int) (cy + (my - cy) / scale) : my;

			editBox.render(g, localMx, localMy, pt);

			if (scaled) {
				g.pose().popPose();
			}
		}
	}

	public boolean keyPressed(int key, int scan, int mods, RecipeEditorData data, StationType station) {
		if (!isActive()) return false;
		if (key == 257 || key == 335) { apply(data, station); return true; }
		if (key == 256) { cancel(); return true; }
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
