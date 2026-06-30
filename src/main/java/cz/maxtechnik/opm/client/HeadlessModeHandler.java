package cz.maxtechnik.opm.client; // Uprav package podle svého opm módu

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
@SuppressWarnings("removal")
@EventBusSubscriber(modid = "opm", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HeadlessModeHandler {
    private static boolean headlessMode = false;

    // Registrace klávesy (v základu nastavená na klávesu 'K')
    public static final KeyMapping AFK_KEY = new KeyMapping(
            "key.opm.toggle_headless",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.opm"
    );

    public static boolean isHeadlessMode() {
        return headlessMode;
    }

    public static void toggleHeadlessMode() {
        headlessMode = !headlessMode;
        Minecraft mc = Minecraft.getInstance();

        if (headlessMode) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("Headless AFK mode ACTIVE. (GPU saved!)"));
            }
            // Natvrdo zavřeme jakékoliv otevřené GUI, aby se uvolnil myšokurz
            mc.setScreen(null);
        } else {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("Headless AFK mode INACTIVE."));
            }
        }
    }

    // Tisková kontrola klávesy (ticky běží dál, i když nerenderuješ!)
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (AFK_KEY.consumeClick()) {
            toggleHeadlessMode();
        }
    }

    // Registrace do Mod Busu
    @EventBusSubscriber(modid = "opm", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(AFK_KEY);
        }
    }
}