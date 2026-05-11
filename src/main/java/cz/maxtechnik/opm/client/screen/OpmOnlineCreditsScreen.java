package cz.maxtechnik.opm.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class OpmOnlineCreditsScreen extends Screen {

    private final Screen parent;

    public OpmOnlineCreditsScreen(Screen parent) {
        super(Component.literal("Online & Credits"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();

        this.addRenderableWidget(Button.builder(
                Component.literal("Online..."),
                btn -> mc.setScreen(new OnlineOptionsScreen(this, mc.options))
        ).pos(this.width / 2 - 155, 60).size(150, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Credits & Attribution..."),
                btn -> mc.setScreen(new WinScreen(false, () -> mc.setScreen(this)))
        ).pos(this.width / 2 + 5, 60).size(150, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Back"),
                btn -> mc.setScreen(parent)
        ).pos(this.width / 2 - 100, this.height - 40).size(200, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}