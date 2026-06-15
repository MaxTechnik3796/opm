package cz.maxtechnik.opm.client;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
public class ScratchTest {
    public static void test(PlayerScoreEntry entry, Objective objective) {
        Component name = entry.ownerName();
        Component score = entry.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
        Component score2 = entry.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT));
    }
}
