package cz.maxtechnik.opm.client.handler;

import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = OpmMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class RecipeBookHandler {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // Kontrola configu - pokud nechceme skrývat, nedělej nic
        if (!OpmConfig.NO_RECIPE_BOOK.get()) return;

        Screen screen = event.getScreen();
        String title = screen.getTitle().getString().toLowerCase();

        if (!title.equals("crafting") && !title.equals("furnace")
                && !title.equals("smoker") && !title.equals("blast furnace")) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<GuiEventListener> widgets = (List<GuiEventListener>) screen.children();

        for (GuiEventListener widget : widgets) {
            if (widget instanceof ImageButton btn) {
                if (btn.getWidth() == 20 && btn.getHeight() == 18) {
                    btn.visible = false;
                    btn.active = false;
                    break;
                }
            }
        }
    }
}