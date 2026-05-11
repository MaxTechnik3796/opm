package cz.maxtechnik.opm.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class OpmFeedbackScreen extends Screen {

    private final Screen parent;
    private final Button feedbackBtn;
    private final Button bugsBtn;

    public OpmFeedbackScreen(Screen parent, Button feedbackBtn, Button bugsBtn) {
        super(Component.literal("Feedback & Bugs"));
        this.parent = parent;
        this.feedbackBtn = feedbackBtn;
        this.bugsBtn = bugsBtn;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();

        this.addRenderableWidget(Button.builder(
                feedbackBtn.getMessage(),
                btn -> feedbackBtn.onPress()
        ).pos(this.width / 2 - 155, 60).size(150, 20).build());

        this.addRenderableWidget(Button.builder(
                bugsBtn.getMessage(),
                btn -> bugsBtn.onPress()
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