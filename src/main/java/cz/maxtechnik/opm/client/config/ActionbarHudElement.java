package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.init.OpmConfig;

/**
 * Actionbar HUD element využívající sdílený preset SimpleTextHudElement.
 */
public final class ActionbarHudElement extends SimpleTextHudElement {

	public ActionbarHudElement() {
		super("actionbar", "Actionbar HUD", "💬",
				OpmConfig.ACTIONBAR_ENABLED, OpmConfig.ACTIONBAR_SCALE,
				OpmConfig.ACTIONBAR_X_OFFSET, OpmConfig.ACTIONBAR_Y_OFFSET,
				Anchor.BOTTOM_CENTER, 2, "Actionbar", 1.0f, 0xFFFFFFFF, 0.25, 2.0);
	}

	@Override
	protected int getBottomCenterBaseY(int screenH) {
		return screenH - 68;
	}
}
