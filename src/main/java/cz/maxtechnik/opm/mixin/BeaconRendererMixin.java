package cz.maxtechnik.opm.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import cz.maxtechnik.opm.client.handler.BeaconVisualizerHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.joml.Matrix4f;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin {

	@Unique
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

	@SuppressWarnings({"unused", "RedundantCast"}) // Utiší varování o nepoužitých parametrech světla a vynuceném Object castu
	@Inject(
			method = "render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At("TAIL")
	)
	private void injectBeaconRange(
			@NotNull BeaconBlockEntity beacon,
			float partialTick,
			@NotNull PoseStack poseStack,
			@NotNull MultiBufferSource bufferSource,
			int combinedLight,
			int combinedOverlay,
			@NotNull CallbackInfo ci
	) {
		if (!BeaconVisualizerHandler.isActive()) return;

		// Tento dvojitý cast přes (Object) tu musí zůstat pro úspěšnou kompilaci v Gradle
		BeaconBlockEntityAccessor accessor = (BeaconBlockEntityAccessor) (Object) beacon;

		int levels = accessor.getLevels();
		if (levels <= 0) return;

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

		VertexConsumer vertexConsumer = bufferSource.getBuffer(OPM_BEACON_ZONE);
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
	}

	// Anotace @Overwrite v kombinaci s potlačením varování spolehlivě vyřeší všechny hlášky o jménech a jedinečnosti
	/**
	 * @author MaxTechnik
	 * @reason BeaconRenderer
	 */
	@SuppressWarnings({"MixinNamePattern", "unused"})
	@Overwrite(remap = false)
	public boolean shouldRenderOffScreen(@NotNull BeaconBlockEntity beacon) {
		return BeaconVisualizerHandler.isActive();
	}

	/**
	 * @author MaxTechnik
	 * @reason BeaconRenderer
	 */
	@SuppressWarnings({"MixinNamePattern", "unused"})
	@Overwrite(remap = false)
	public int getViewDistance() {
		return BeaconVisualizerHandler.isActive() ? 256 : 64;
	}
}