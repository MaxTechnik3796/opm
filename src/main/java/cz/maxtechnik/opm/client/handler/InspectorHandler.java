package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.screen.InspectorScreen;
import cz.maxtechnik.opm.init.OpmKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
public class InspectorHandler{
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event){
		Minecraft mc=Minecraft.getInstance();
		while(OpmKeys.OPEN_INSPECTOR.consumeClick()){
			ItemStack hoveredStack=ItemStack.EMPTY;
			if(mc.screen instanceof AbstractContainerScreen<?> containerScreen){
				hoveredStack=containerScreen.getMenu().getCarried();
				if(hoveredStack.isEmpty()){
					var slot=containerScreen.getSlotUnderMouse();
					if(slot!=null&&!slot.getItem().isEmpty()) hoveredStack=slot.getItem();
				}
			}else if(mc.screen==null){
				assert mc.player!=null;
				hoveredStack=mc.player.getMainHandItem();
			}
			if(hoveredStack.isEmpty()) continue;
			// Předej aktuální screen jako parent — zavřením se vrátíme zpět
			mc.setScreen(new InspectorScreen(hoveredStack,mc.screen));
		}
	}
}