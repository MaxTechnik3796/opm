package cz.maxtechnik.opm.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class BeaconVisualizerHandler {
    private static boolean active = false;

    public static void toggle() {
        active = !active;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (active) {
                mc.player.displayClientMessage(Component.literal("§b[Optimix] Beacon Visualizer: ZAPNUTÝ"), true);
            } else {
                mc.player.displayClientMessage(Component.literal("§c[Optimix] Beacon Visualizer: VYPNUTÝ"), true);
            }
        }
    }

    public static boolean isActive() {
        return active;
    }
}