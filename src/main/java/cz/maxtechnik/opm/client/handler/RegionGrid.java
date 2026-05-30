package cz.maxtechnik.opm.client.handler;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.init.OpmKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = OpmMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RegionGrid {

    private static boolean showGrid = false;

    public static void toggleGrid() {
        showGrid = !showGrid;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    showGrid ? "§aRegion Grid §7[§aON§7]" : "§cRegion Grid §7[§cOFF§7]"
                ), true // true = action bar (nad hotbarem), ne chat
            );
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!showGrid || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var camera = mc.gameRenderer.getMainCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;

        int playerX = (int) Math.floor(camX);
        int playerZ = (int) Math.floor(camZ);

        // Začátek aktuálního regionu (512×512 bloků = 32×32 chunků)
        int regionStartX = (playerX >> 9) << 9;
        int regionStartZ = (playerZ >> 9) << 9;

        final float MIN_Y = -64F;
        final float MAX_Y = 320F;
        final int REGION_SIZE = 512;
        final int CHUNK_SIZE = 16;

        // Barvy
        // Červená — vnější rám regionu
        final float rMain = 1F, gMain = 0.2F, bMain = 0.2F, aMain = 0.9F;
        // Žlutá — mřížka chunků
        final float rChunk = 1F, gChunk = 0.85F, bChunk = 0F, aChunk = 0.5F;
        // Bílá — vodorovné linky po obvodu
        final float rH = 0.8F, gH = 0.8F, bH = 0.8F, aH = 0.3F;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camX, -camY, -camZ);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.5F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // --- Svislé stěny chunků uvnitř regionu ---
        for (int offset = CHUNK_SIZE; offset < REGION_SIZE; offset += CHUNK_SIZE) {
            // Podél osy X
            buf.addVertex(matrix, regionStartX + offset, MIN_Y, regionStartZ).setColor(rChunk, gChunk, bChunk, aChunk);
            buf.addVertex(matrix, regionStartX + offset, MAX_Y, regionStartZ).setColor(rChunk, gChunk, bChunk, aChunk);
            buf.addVertex(matrix, regionStartX + offset, MIN_Y, regionStartZ + REGION_SIZE).setColor(rChunk, gChunk, bChunk, aChunk);
            buf.addVertex(matrix, regionStartX + offset, MAX_Y, regionStartZ + REGION_SIZE).setColor(rChunk, gChunk, bChunk, aChunk);
            // Podél osy Z
            buf.addVertex(matrix, regionStartX, MIN_Y, regionStartZ + offset).setColor(rChunk, gChunk, bChunk, aChunk);
            buf.addVertex(matrix, regionStartX, MAX_Y, regionStartZ + offset).setColor(rChunk, gChunk, bChunk, aChunk);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, MIN_Y, regionStartZ + offset).setColor(rChunk, gChunk, bChunk, aChunk);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, MAX_Y, regionStartZ + offset).setColor(rChunk, gChunk, bChunk, aChunk);
        }

        // --- Vodorovné linky po obvodu regionu ---
        for (float h = MIN_Y + CHUNK_SIZE; h < MAX_Y; h += CHUNK_SIZE) {
            buf.addVertex(matrix, regionStartX, h, regionStartZ).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, h, regionStartZ).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX, h, regionStartZ + REGION_SIZE).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, h, regionStartZ + REGION_SIZE).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX, h, regionStartZ).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX, h, regionStartZ + REGION_SIZE).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, h, regionStartZ).setColor(rH, gH, bH, aH);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, h, regionStartZ + REGION_SIZE).setColor(rH, gH, bH, aH);
        }

        // --- Hlavní vnější rám regionu (červený) ---
        // Rohové svislé linky
        for (int i = 0; i <= REGION_SIZE; i += REGION_SIZE) {
            for (int j = 0; j <= REGION_SIZE; j += REGION_SIZE) {
                buf.addVertex(matrix, regionStartX + i, MIN_Y, regionStartZ + j).setColor(rMain, gMain, bMain, aMain);
                buf.addVertex(matrix, regionStartX + i, MAX_Y, regionStartZ + j).setColor(rMain, gMain, bMain, aMain);
            }
        }
        // Podlaha a strop
        for (float y : new float[]{MIN_Y, MAX_Y}) {
            buf.addVertex(matrix, regionStartX, y, regionStartZ).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, y, regionStartZ).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX, y, regionStartZ + REGION_SIZE).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, y, regionStartZ + REGION_SIZE).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX, y, regionStartZ).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX, y, regionStartZ + REGION_SIZE).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, y, regionStartZ).setColor(rMain, gMain, bMain, aMain);
            buf.addVertex(matrix, regionStartX + REGION_SIZE, y, regionStartZ + REGION_SIZE).setColor(rMain, gMain, bMain, aMain);
        }

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.lineWidth(1F);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}