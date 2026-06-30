package cz.maxtechnik.opm.client.screen; // Uprav package podle sebe

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HeadlessAfkScreen extends Screen {

    public HeadlessAfkScreen() {
        super(Component.literal("Headless AFK"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Vykreslíme ultra tmavé charcoal pozadí (ne žhnoucí černou, aby to nevypadalo jako pád)
        //guiGraphics.fill(0, 0, this.width, this.height, 0xFF0A0A0A);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Vykreslení textů na obrazovku
        guiGraphics.drawCenteredString(this.font, "=== OPM HEADLESS AFK MODE ===", centerX, centerY - 30, 0xFFE04343); // Červená
        guiGraphics.drawCenteredString(this.font, "3D World rendering is suspended (~0% GPU)", centerX, centerY - 10, 0xFFAAAAAA); // Šedá
        guiGraphics.drawCenteredString(this.font, "All game sounds are currently MUTED.", centerX, centerY + 5, 0xFFAAAAAA);
        
        guiGraphics.drawCenteredString(this.font, "Press 'K' or 'ESC' to return to game", centerX, centerY + 35, 0xFF55FF55); // Zelená
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Pokud hráč stiskne K nebo ESC, bezpečně zavřeme obrazovku a vrátíme ho do hry
        if (keyCode == HeadlessModeHandler.AFK_KEY.getKey().getValue() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}