package cz.maxtechnik.opm.client.screen.layout;

import cz.maxtechnik.opm.client.recipe.RecipeJsonBuilder.StationType;

import java.util.ArrayList;
import java.util.List;

public class StationLayout {
	private final StationType stationType;
	private final ToggleGroup headerToggle;
	private final ToggleGroup subToggle;
	private final SlotGroup inputSlots;
	private final SlotGroup outputSlots;
	private final SlotGroup inputFluids;
	private final SlotGroup outputFluids;
	private final ProcessingTime processingTime;

	private StationLayout(Builder builder) {
		this.stationType = builder.stationType;
		this.headerToggle = builder.headerToggle;
		this.subToggle = builder.subToggle;
		this.inputSlots = builder.inputSlots;
		this.outputSlots = builder.outputSlots;
		this.inputFluids = builder.inputFluids;
		this.outputFluids = builder.outputFluids;
		this.processingTime = builder.processingTime;
	}

	public StationType getStationType() { return stationType; }
	public ToggleGroup getHeaderToggle() { return headerToggle; }
	public ToggleGroup getSubToggle() { return subToggle; }
	public SlotGroup getInputSlots() { return inputSlots; }
	public SlotGroup getOutputSlots() { return outputSlots; }
	public SlotGroup getInputFluids() { return inputFluids; }
	public SlotGroup getOutputFluids() { return outputFluids; }
	public ProcessingTime getProcessingTime() { return processingTime; }

	public static Builder builder(StationType type) {
		return new Builder(type);
	}

	public static class Builder {
		private final StationType stationType;
		private ToggleGroup headerToggle;
		private ToggleGroup subToggle;
		private SlotGroup inputSlots;
		private SlotGroup outputSlots;
		private SlotGroup inputFluids;
		private SlotGroup outputFluids;
		private ProcessingTime processingTime;

		public Builder(StationType type) {
			this.stationType = type;
		}

		public Builder headerToggle(ToggleGroup toggle) {
			this.headerToggle = toggle;
			return this;
		}

		public Builder subToggle(ToggleGroup toggle) {
			this.subToggle = toggle;
			return this;
		}

		public Builder input(SlotGroup input) {
			this.inputSlots = input;
			return this;
		}

		public Builder output(SlotGroup output) {
			this.outputSlots = output;
			return this;
		}

		public Builder inputFluids(SlotGroup inputFluids) {
			this.inputFluids = inputFluids;
			return this;
		}

		public Builder outputFluids(SlotGroup outputFluids) {
			this.outputFluids = outputFluids;
			return this;
		}

		public Builder processingTime(ProcessingTime time) {
			this.processingTime = time;
			return this;
		}

		public StationLayout build() {
			return new StationLayout(this);
		}
	}
}
