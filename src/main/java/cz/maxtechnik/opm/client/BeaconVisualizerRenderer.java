package cz.maxtechnik.opm.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.client.handler.BeaconTracker;
import cz.maxtechnik.opm.client.handler.BeaconVisualizerHandler;
import cz.maxtechnik.opm.mixin.BeaconBlockEntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = OpmMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class BeaconVisualizerRenderer {

    private static final RenderType OPM_BEACON_ZONE = RenderType.create(
        "opm_beacon_zone",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!BeaconVisualizerHandler.isActive()) return;
        // Spouštíme až po vykreslení translucentních bloků (voda, sklo), aby zóna správně prosvítala
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(OPM_BEACON_ZONE);

        for (BeaconBlockEntity beacon : BeaconTracker.BEACONS) {
            if (beacon.isRemoved() || beacon.getLevel() == null) continue;

            BeaconBlockEntityAccessor accessor = (BeaconBlockEntityAccessor) beacon;
            int levels = accessor.getLevels();
            if (levels <= 0) continue;

            int range = (levels + 1) * 10;

            int color = 0x00FFFFFF;
            Holder<MobEffect> primaryEffect = accessor.getPrimaryPower();
            if (primaryEffect != null && primaryEffect.isBound()) {
                color = primaryEffect.value().getColor();
            }

            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float a = 0.15F;

            poseStack.pushPose();
            
            // PŘEKLAD SOUŘADNIC: Jelikož kreslíme globálně, musíme odečíst pozici kamery, 
            // aby kostka seděla přesně na souřadnicích majáku ve světě.
            double x = beacon.getBlockPos().getX() - cameraPos.x;
            double y = beacon.getBlockPos().getY() - cameraPos.y;
            double z = beacon.getBlockPos().getZ() - cameraPos.z;
            poseStack.translate(x, y, z);

            Matrix4f matrix = poseStack.last().pose();

            double minX = -range;
            double minZ = -range;
            double minY = -range;
            double maxX = range + 1;
            double maxZ = range + 1;
            double maxY = range + 1;

            // Spodek
            vertexConsumer.addVertex(matrix, (float)minX, (float)minY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)minY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)minY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)minY, (float)maxZ).setColor(r, g, b, a);

            // Vršek
            vertexConsumer.addVertex(matrix, (float)minX, (float)maxY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)maxY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)maxY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)maxY, (float)minZ).setColor(r, g, b, a);

            // Sever
            vertexConsumer.addVertex(matrix, (float)minX, (float)minY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)maxY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)maxY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)minY, (float)minZ).setColor(r, g, b, a);

            // Jih
            vertexConsumer.addVertex(matrix, (float)minX, (float)minY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)minY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)maxY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)maxY, (float)maxZ).setColor(r, g, b, a);

            // Západ
            vertexConsumer.addVertex(matrix, (float)minX, (float)minY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)minY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)maxY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)minX, (float)maxY, (float)minZ).setColor(r, g, b, a);

            // Východ
            vertexConsumer.addVertex(matrix, (float)maxX, (float)minY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)maxY, (float)minZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)maxY, (float)maxZ).setColor(r, g, b, a);
            vertexConsumer.addVertex(matrix, (float)maxX, (float)minY, (float)maxZ).setColor(r, g, b, a);

            poseStack.popPose();
        }

        // Okamžitě odešleme data na GPU
        bufferSource.endBatch(OPM_BEACON_ZONE);
    }
}