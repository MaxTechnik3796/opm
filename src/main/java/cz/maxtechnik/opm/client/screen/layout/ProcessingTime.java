package cz.maxtechnik.opm.client.screen.layout;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProcessingTime {
	private final Supplier<Integer> getter;
	private final Consumer<Integer> setter;
	private final int minTime;
	private final int maxTime;
	private final int step;

	private int anchorX;
	private int anchorY;

	public ProcessingTime(Supplier<Integer> getter, Consumer<Integer> setter, int minTime, int maxTime, int step) {
		this.getter = getter;
		this.setter = setter;
		this.minTime = minTime;
		this.maxTime = maxTime;
		this.step = step;
	}

	public static ProcessingTime standard(Supplier<Integer> getter, Consumer<Integer> setter) {
		return new ProcessingTime(getter, setter, 10, 10000, 10);
	}

	public void setAnchor(int x, int y) {
		this.anchorX = x;
		this.anchorY = y;
	}

	public int getValue() {
		return getter != null ? getter.get() : 200;
	}

	public void setValue(int val) {
		if (setter != null) {
			setter.accept(Math.clamp(val, minTime, maxTime));
		}
	}

	public void increment() {
		setValue(getValue() + step);
	}

	public void decrement() {
		setValue(getValue() - step);
	}

	public int getAnchorX() { return anchorX; }
	public int getAnchorY() { return anchorY; }
}
