package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = OpmMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class FullbrightHandler {

    // Původní gamma hráče - obnoví se při vypnutí
    private static double originalGamma = 1.0;
    private static boolean active = false;

    private static final double FULLBRIGHT_GAMMA = 10000.0;

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();

        if (!active) {
            originalGamma = mc.options.gamma().get();
            setGamma(mc, FULLBRIGHT_GAMMA);
            active = true;
            OpmMod.LOGGER.debug("[OPM] Fullbright on");
        } else {
            setGamma(mc, originalGamma);
            active = false;
            OpmMod.LOGGER.debug("[OPM] Fullbright off");
        }
    }

    private static void setGamma(Minecraft mc, double value) {
        try {
            java.lang.reflect.Field field = net.minecraft.client.OptionInstance.class
                    .getDeclaredField("value");
            field.setAccessible(true);
            field.set(mc.options.gamma(), value);
        } catch (Exception e) {
            OpmMod.LOGGER.error("[OPM] Nepodařilo se nastavit gammu: {}", e.getMessage());
        }
    }

    // Synchronizuje gammu pokud je fullbright aktivní
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (active) {
            double currentGamma = mc.options.gamma().get();
            if (currentGamma != FULLBRIGHT_GAMMA) {
                mc.options.gamma().set(FULLBRIGHT_GAMMA);
            }
        }
    }

    public static boolean isActive() {
        return active;
    }
}