package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;
import cz.maxtechnik.opm.client.screen.RecipeEditorData;
import cz.maxtechnik.opm.client.widget.UiKit;

/**
 * Továrna pro vytváření specifikací rozvržení (StationLayout) jednotlivých stanic.
 */
public class StationLayoutFactory {

	private StationLayoutFactory() {}

	/** Vrátí konfiguraci rozvržení pro zadanou stanici a data editoru. */
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
}
