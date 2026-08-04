package cz.maxtechnik.opm.client.screen.layout;

public final class SlotSpec {
	private final boolean hasCount;
	private final boolean hasChance;
	private final boolean isFluid;
	private final boolean isResult;

	private SlotSpec(boolean hasCount, boolean hasChance, boolean isFluid, boolean isResult) {
		this.hasCount = hasCount;
		this.hasChance = hasChance;
		this.isFluid = isFluid;
		this.isResult = isResult;
	}

	public static SlotSpec item() {
		return new SlotSpec(false, false, false, false);
	}

	public static SlotSpec result() {
		return new SlotSpec(false, false, false, true);
	}

	public static SlotSpec fluid() {
		return new SlotSpec(false, false, true, false);
	}

	public SlotSpec withCount() {
		return new SlotSpec(true, this.hasChance, this.isFluid, this.isResult);
	}

	public SlotSpec withChance() {
		return new SlotSpec(this.hasCount, true, this.isFluid, this.isResult);
	}

	public boolean hasCount() { return hasCount; }
	public boolean hasChance() { return hasChance; }
	public boolean isFluid() { return isFluid; }
	public boolean isResult() { return isResult; }
}
