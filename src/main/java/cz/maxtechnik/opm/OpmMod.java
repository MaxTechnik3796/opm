package cz.maxtechnik.opm;

import com.mojang.logging.LogUtils;
import cz.maxtechnik.opm.client.handler.FullbrightHandler;
import cz.maxtechnik.opm.init.OpmConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.slf4j.Logger;
@SuppressWarnings("removal")
@Mod(OpmMod.MODID)
public class OpmMod{
	public static final String MODID="opm";
	public static final Logger LOGGER=LogUtils.getLogger();
	public OpmMod(IEventBus bus,ModContainer modContainer){
		bus.addListener(this::commonSetup);
		modContainer.registerConfig(ModConfig.Type.CLIENT,OpmConfig.SPEC);
	}
	@EventBusSubscriber(modid=MODID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
	public static class ClientModEvents{
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event){
			LOGGER.info("OptiMix: Client Setup");
		}
	}
	@EventBusSubscriber(modid=MODID, bus=EventBusSubscriber.Bus.GAME, value=Dist.CLIENT)
	public static class ClientGameEvents{
		@SubscribeEvent
		public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event){
			FullbrightHandler.disable();
		}
	}
	private void commonSetup(final FMLCommonSetupEvent event){
		LOGGER.info("OptiMix: Common Setup");
	}
}