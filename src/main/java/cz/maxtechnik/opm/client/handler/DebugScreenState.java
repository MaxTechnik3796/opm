package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.ArrayList;
import java.util.List;
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT)
public class DebugScreenState{
	public static boolean showFullTags=false;
	public static final List<String> lastLeftList=new ArrayList<>();
	public static final List<String> lastRightList=new ArrayList<>();
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event){
		if(!OpmConfig.CUSTOM_DEBUG_SCREEN.get()||!OpmConfig.DEBUG_HIDE_OTHER_MODS.get()) return;
		if(!event.getLeft().isEmpty()&&!lastLeftList.isEmpty()){
			event.getLeft().clear();
			event.getLeft().addAll(lastLeftList);
		}
		if(!event.getRight().isEmpty()&&!lastRightList.isEmpty()){
			event.getRight().clear();
			event.getRight().addAll(lastRightList);
		}
	}
}