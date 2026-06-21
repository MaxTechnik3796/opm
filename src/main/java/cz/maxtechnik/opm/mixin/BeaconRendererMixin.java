package cz.maxtechnik.opm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cz.maxtechnik.opm.client.handler.BeaconVisualizerHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin {

	@Inject(
			method = "render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At("TAIL")
	)
	private void injectBeaconRange(BeaconBlockEntity beacon, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, CallbackInfo ci) {
		if (!BeaconVisualizerHandler.isActive()) return;

		BeaconBlockEntityAccessor accessor = (BeaconBlockEntityAccessor) beacon;

		int levels = accessor.getLevels();
		if (levels <= 0) return;

		// Výpočet dosahu podle mechanik Minecraftu
		int range = (levels + 1) * 10;

		int color = 0x00FFFFFF; // Výchozí azurová
		Holder<MobEffect> primaryEffect = accessor.getPrimaryPower();

		if (primaryEffect != null && primaryEffect.isBound()) {
			color = primaryEffect.value().getColor();
		}

		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;

		// OPRAVA 1: Snížení průhlednosti na 10% (0.1F) - méně agresivní při pohledu zevnitř
		float a = 0.10F;

		// OPRAVA 2: Změna RenderType na 'lightning()'. Je to unlit, translucent overlay,
		// který mnohem lépe zvládá pohled zevnitř bez grafických artefaktů.
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lightning());
		Matrix4f matrix = poseStack.last().pose();

		// OPRAVA 3: Změna verticalního limitu. Teď je to perfektní krychle +/- range.
		// Relativní souřadnice vůči majáku (0,0,0)
		double minX = -range;
		double minZ = -range;
		double minY = -range; // Dříve -beacon_y, teď správně -range

		double maxX = range + 1;
		double maxZ = range + 1;
		double maxY = range + 1; // Dříve build_limit_y, teď správně range+1

		// VYKRESLENÍ 6 STĚN KOSTKY (Quads)
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
}