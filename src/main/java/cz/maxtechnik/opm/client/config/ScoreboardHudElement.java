package cz.maxtechnik.opm.client.config;

import cz.maxtechnik.opm.client.overlay.ScoreboardOverlay;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;

public class ScoreboardHudElement extends OffsetHudElement {

	private OpmConfig.HudLocation side;

	public ScoreboardHudElement() {
		super("scoreboard", "Scoreboard", "📊",
				OpmConfig.SCOREBOARD_ENABLED, OpmConfig.SCOREBOARD_SCALE,
				OpmConfig.SCOREBOARD_X_OFFSET, OpmConfig.SCOREBOARD_Y_OFFSET,
				Anchor.LEFT_RIGHT_TOP, 4);
		this.side = OpmConfig.SCOREBOARD_SIDE.get();
	}

	@Override
	protected boolean isLeftAligned() {
		return side == OpmConfig.HudLocation.LEFT;
	}

	@Override
	protected void handleSideDrag(int mx, int targetX, int screenW) {
		if (mx < screenW / 2) {
			side = OpmConfig.HudLocation.LEFT;
			xOffset = targetX - edgePad;
		} else {
			side = OpmConfig.HudLocation.RIGHT;
			xOffset = targetX - (screenW - edgePad - getW());
		}
	}

	@Override
	public int getW() {
		Minecraft mc = Minecraft.getInstance();
		return Math.max(30, (int) (ScoreboardOverlay.getScoreboardWidth(mc) * scale));
	}

	@Override
	public int getH() {
		Minecraft mc = Minecraft.getInstance();
		return Math.max(20, (int) (ScoreboardOverlay.getScoreboardHeight(mc) * scale));
	}

	@Override
	public void reset() {
		super.reset();
		side = OpmConfig.HudLocation.RIGHT;
	}

	@Override
	public void save() {
		super.save();
		OpmConfig.SCOREBOARD_SIDE.set(side);
	}

	@Override
	protected int renderExtraOptions(GuiGraphics g, Font font, int x, int y, int w, int mx, int my) {
		ConfigUiHelper.drawEnumCycler(g, font, "Side", side.name(), x, y, w, mx, my);
		return y + ConfigUiHelper.ITEM_H;
	}

	@Override
	protected int getExtraOptionsHeight(int startY) {
		return startY + ConfigUiHelper.ITEM_H;
	}

	@Override
	protected boolean handleExtraOptionsClick(int mx, int my, int x, int startY, int w) {
		if (ConfigUiHelper.isEnumHit(mx, my, x, startY, w)) {
			side = (side == OpmConfig.HudLocation.LEFT) ? OpmConfig.HudLocation.RIGHT : OpmConfig.HudLocation.LEFT;
			return true;
		}
		return false;
	}

	@Override
	protected void renderContent(GuiGraphics g, Font font, int x, int y, int screenW, int screenH) {
		int unscaledW = (int) (getW() / scale);
		int unscaledH = (int) (getH() / scale);
		g.fill(0, 0, unscaledW, unscaledH, 0x60000000);

		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) != null) {
			Scoreboard scoreboard = mc.level.getScoreboard();
			Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
			if (objective != null) {
				g.drawString(font, objective.getDisplayName(), (unscaledW - font.width(objective.getDisplayName())) / 2, 0, 0xFFFFFFFF, true);
				List<PlayerScoreEntry> scores = ScoreboardOverlay.getActiveScores(scoreboard, objective);
				for (int i = 0; i < scores.size(); i++) {
					PlayerScoreEntry entry = scores.get(i);
					PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
					Component nameComp = team != null ? team.getFormattedName(entry.ownerName()) : entry.ownerName();
					int ly = 9 + i * 9;
					g.drawString(font, nameComp, 0, ly, 0xFFDDDDDD, true);
					Component scoreComp = entry.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
					g.drawString(font, scoreComp, unscaledW - font.width(scoreComp) - 2, ly, 0xFFFFFFFF, true);
				}
			}
		} else {
			String testTitle = "§e§lOPM TEST SERVER";
			g.drawString(font, testTitle, (unscaledW - font.width(testTitle)) / 2, 0, 0xFFFFFFFF, true);
			for (int i = 0; i < ScoreboardOverlay.MOCK_PLAYERS.length; i++) {
				int ly = 9 + i * 9;
				g.drawString(font, ScoreboardOverlay.MOCK_PLAYERS[i], 0, ly, 0xFFDDDDDD, true);
				String scoreVal = ScoreboardOverlay.MOCK_SCORES[i];
				if (!scoreVal.isEmpty()) {
					g.drawString(font, scoreVal, unscaledW - font.width(scoreVal) - 2, ly, 0xFFFF5555, true);
				}
			}
		}
	}
}
