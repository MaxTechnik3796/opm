package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.screen.OpmFeedbackScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void modifyPauseScreen(CallbackInfo ci) {
        Button advancementsBtn = null;
        Button statisticsBtn = null;
        Button feedbackBtn = null;
        Button bugsBtn = null;

        for (var widget : this.children()) {
            if (widget instanceof Button btn) {
                String text = btn.getMessage().getString();
                if (text.equals("Advancements")) advancementsBtn = btn;
                else if (text.equals("Statistics")) statisticsBtn = btn;
                else if (text.contains("Feedback")) feedbackBtn = btn;
                else if (text.contains("Bugs") || text.contains("Report")) bugsBtn = btn;
            }
        }

        if (advancementsBtn != null) {
            advancementsBtn.visible = false;
            advancementsBtn.active = false;
        }
        if (statisticsBtn != null) {
            statisticsBtn.visible = false;
            statisticsBtn.active = false;
        }

        if (feedbackBtn != null && bugsBtn != null) {
            feedbackBtn.visible = false;
            bugsBtn.visible = false;

            final Screen self = this;
            final Button fb = feedbackBtn;
            final Button bb = bugsBtn;

            Button mergedBtn = Button.builder(
                            Component.literal("Feedback & Bugs"),
                            btn -> Minecraft.getInstance().setScreen(new OpmFeedbackScreen(self, fb, bb))
                    )
                    .pos(feedbackBtn.getX(), feedbackBtn.getY())
                    .size(feedbackBtn.getWidth() * 2 + 4, feedbackBtn.getHeight())
                    .build();

            this.addRenderableWidget(mergedBtn);
        }
    }
}