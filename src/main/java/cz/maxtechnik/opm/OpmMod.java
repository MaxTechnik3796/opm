package cz.maxtechnik.opm;

import com.mojang.logging.LogUtils;
import cz.maxtechnik.opm.client.handler.FullbrightHandler;
import cz.maxtechnik.opm.init.OpmConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.slf4j.Logger;

@SuppressWarnings("removal")
@Mod(OpmMod.MODID)
public class OpmMod {

    public static final String MODID = "opm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpmMod(IEventBus modEventBus, ModContainer modContainer) {
        // Config - CLIENT typ, uloží se do config/opm-client.toml
        modContainer.registerConfig(ModConfig.Type.CLIENT, OpmConfig.SPEC);

        LOGGER.info("[OPM] Toolka mod načten.");
    }

    // OpmKeys a případné další @EventBusSubscriber třídy
    // se registrují automaticky - není potřeba je volat zde

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("[OPM] Client setup hotov.");
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents {


        // Při odpojení resetuj gammu pokud byl fullbright zapnutý
        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            if (FullbrightHandler.isActive()) {
                net.minecraft.client.Minecraft.getInstance().options.gamma().set(1.0);
            }
        }
    }
}