package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.screen.OpmOnlineCreditsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void modifyOptionsScreen(CallbackInfo ci) {
        Button onlineBtn = null;
        Button creditsBtn = null;
        Button telemetryBtn = null;

        for (var widget : this.children()) {
            if (widget instanceof Button btn) {
                String text = btn.getMessage().getString();
                if (text.equals("Online...")) onlineBtn = btn;
                else if (text.contains("Credits")) creditsBtn = btn;
                else if (text.contains("Telemetry")) telemetryBtn = btn;
            }
        }

        if (telemetryBtn != null) {
            telemetryBtn.visible = false;
            telemetryBtn.active = false;
        }

        if (onlineBtn != null) onlineBtn.visible = false;
        if (creditsBtn != null) creditsBtn.visible = false;

        if (onlineBtn != null) {
            final Screen self = this;
            Button mergedBtn = Button.builder(
                            Component.literal("Online & Credits"),
                            btn -> Minecraft.getInstance().setScreen(new OpmOnlineCreditsScreen(self))
                    )
                    .pos(onlineBtn.getX(), onlineBtn.getY())
                    .size(onlineBtn.getWidth(), onlineBtn.getHeight())
                    .build();

            this.addRenderableWidget(mergedBtn);
        }
    }
}