package cz.maxtechnik.opm.client.handler;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import cz.maxtechnik.opm.OpmMod;
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

@SuppressWarnings("removal")
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

	private static final RenderType OPM_BEACON_LINES = RenderType.create(
			"opm_beacon_lines",
			DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.DEBUG_LINES,
			256,
			false,
			false,
			RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setCullState(RenderStateShard.NO_CULL)
					.setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // Viditelnost skrz stěny
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.of(2.5D))) // Tloušťka čar
					.createCompositeState(false)
	);

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (!BeaconVisualizerHandler.isActive()) return;
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		Vec3 cameraPos = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

		// ==========================================
		// FÁZE 1: VYKRESLENÍ POLOPRŮHLEDNÝCH STĚN
		// ==========================================
		VertexConsumer zoneConsumer = bufferSource.getBuffer(OPM_BEACON_ZONE);

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
			float zoneAlpha = 0.12F;

			poseStack.pushPose();

			double x = beacon.getBlockPos().getX() - cameraPos.x;
			double y = beacon.getBlockPos().getY() - cameraPos.y;
			double z = beacon.getBlockPos().getZ() - cameraPos.z;
			poseStack.translate(x, y, z);

			Matrix4f matrix = poseStack.last().pose();

			float minX = -range;
			float minY = -range;
			float minZ = -range;
			float maxX = range + 1;
			float maxY = range + 1;
			float maxZ = range + 1;

			// Spodek
			zoneConsumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, zoneAlpha);
			// Vršek
			zoneConsumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, zoneAlpha);
			// Sever
			zoneConsumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, zoneAlpha);
			// Jih
			zoneConsumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, zoneAlpha);
			// Západ
			zoneConsumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, zoneAlpha);
			// Východ
			zoneConsumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, zoneAlpha);
			zoneConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, zoneAlpha);

			poseStack.popPose();
		}
		bufferSource.endBatch(OPM_BEACON_ZONE); // Bezpečně uzavřeme stěny před spuštěním čar

		// ==========================================
		// FÁZE 2: VYKRESLENÍ HRAN (X-RAY LINES)
		// ==========================================
		VertexConsumer lineConsumer = bufferSource.getBuffer(OPM_BEACON_LINES);

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
			float lineAlpha = 0.85F;

			poseStack.pushPose();

			double x = beacon.getBlockPos().getX() - cameraPos.x;
			double y = beacon.getBlockPos().getY() - cameraPos.y;
			double z = beacon.getBlockPos().getZ() - cameraPos.z;
			poseStack.translate(x, y, z);

			Matrix4f matrix = poseStack.last().pose();

			float minX = -range;
			float minY = -range;
			float minZ = -range;
			float maxX = range + 1;
			float maxY = range + 1;
			float maxZ = range + 1;

			// Horizontální čáry (Osa X)
			drawDebugLine(matrix, lineConsumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, lineAlpha);

			// Vertikální čáry (Osa Y)
			drawDebugLine(matrix, lineConsumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, lineAlpha);

			// Horizontální čáry (Osa Z)
			drawDebugLine(matrix, lineConsumer, minX, minY, minZ, minX, minY, maxZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, lineAlpha);
			drawDebugLine(matrix, lineConsumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, lineAlpha);

			poseStack.popPose();
		}
		bufferSource.endBatch(OPM_BEACON_LINES); // Uzavřeme čáry
	}

	private static void drawDebugLine(Matrix4f matrix, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
		consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
		consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
	}
}