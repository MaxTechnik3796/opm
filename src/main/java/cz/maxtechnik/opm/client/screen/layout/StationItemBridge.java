package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.CrushingOutput;
import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType.FluidEntry;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Pomocný most (bridge) pro získávání a nastavování předmětů, výstupních šancí a tekutin
 * mezi RecipeEditorData a jednotlivými skupinami slotů stanic.
 */
public class StationItemBridge {

	private StationItemBridge() {}

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
		return type == StationType.MIXING ? d.mixFluidOuts : List.of();
	}

	public static int getResultCount(RecipeEditorData d, StationType type) {
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
