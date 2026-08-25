package cz.maxtechnik.opm.client.screen.config;

import cz.maxtechnik.opm.init.OpmConfig;

/**
 * Title HUD element využívající sdílený preset SimpleTextHudElement.
 */
public final class TitleHudElement extends SimpleTextHudElement {

	public TitleHudElement() {
		super("title", "Title HUD", "🔤",
				OpmConfig.TITLE_ENABLED, OpmConfig.TITLE_SCALE,
				OpmConfig.TITLE_X_OFFSET, OpmConfig.TITLE_Y_OFFSET,
				Anchor.CENTER, 2, "Title", 4.0f, 0xFFFFFFFF, 0.25, 2.0);
	}
}
