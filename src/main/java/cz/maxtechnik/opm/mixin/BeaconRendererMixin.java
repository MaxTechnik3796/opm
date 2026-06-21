package cz.maxtechnik.opm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cz.maxtechnik.opm.client.handler.BeaconVisualizerHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BeaconRenderer.class)
public class BeaconRendererMixin{
	@Inject(
			method="render(Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at=@At("TAIL")
	)
	private void injectBeaconRange(BeaconBlockEntity beacon,float partialTick,PoseStack poseStack,MultiBufferSource bufferSource,int combinedLight,int combinedOverlay,CallbackInfo ci){
		if(!BeaconVisualizerHandler.isActive()) return;
		// Přetypování beaconu na náš Accessor pro získání skrytých hodnot
		BeaconBlockEntityAccessor accessor=(BeaconBlockEntityAccessor)beacon;
		int levels=accessor.getLevels();
		if(levels<=0) return;
		int range=(levels+1)*10;
		// Výchozí barva (Azurová), pokud beacon zrovna nemá navolený efekt
		int color=0x00FFFFFF;
		Holder<MobEffect> primaryEffect=accessor.getPrimaryPower();
		// V 1.21.1 kontrolujeme funkčnost přes isBound()
		if(primaryEffect!=null&&primaryEffect.isBound()){
			color=primaryEffect.value().getColor();
		}
		float r=((color>>16)&0xFF)/255.0F;
		float g=((color>>8)&0xFF)/255.0F;
		float b=(color&0xFF)/255.0F;
		float a=0.15F; // PRŮHLEDNOST STĚN (0.15 je cca 15% viditelnost - ideální průsvitnost)
		// Použijeme debugFilledBox, který nepotřebuje UV mapování a skvěle renderuje quads
		VertexConsumer vertexConsumer=bufferSource.getBuffer(RenderType.debugFilledBox());
		Matrix4f matrix=poseStack.last().pose();
		// Výpočty hranic (relativní k pozici majáku)
		double minX=-range;
		double minZ=-range;
		double maxX=range+1;
		double maxZ=range+1;
		double minY=beacon.getLevel()!=null?beacon.getLevel().getMinBuildHeight()-beacon.getBlockPos().getY():-range;
		double maxY=beacon.getLevel()!=null?beacon.getLevel().getMaxBuildHeight()-beacon.getBlockPos().getY():range+1;
		// VYKRESLENÍ 6 STĚN VELKÉ KOSTKY (Quads)
		// Spodek
		vertexConsumer.addVertex(matrix,(float)minX,(float)minY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)minY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)minY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)minY,(float)maxZ).setColor(r,g,b,a);
		// Vršek
		vertexConsumer.addVertex(matrix,(float)minX,(float)maxY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)maxY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)maxY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)maxY,(float)minZ).setColor(r,g,b,a);
		// Sever
		vertexConsumer.addVertex(matrix,(float)minX,(float)minY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)maxY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)maxY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)minY,(float)minZ).setColor(r,g,b,a);
		// Jih
		vertexConsumer.addVertex(matrix,(float)minX,(float)minY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)minY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)maxY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)maxY,(float)maxZ).setColor(r,g,b,a);
		// Západ
		vertexConsumer.addVertex(matrix,(float)minX,(float)minY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)minY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)maxY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)minX,(float)maxY,(float)minZ).setColor(r,g,b,a);
		// Východ
		vertexConsumer.addVertex(matrix,(float)maxX,(float)minY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)maxY,(float)minZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)maxY,(float)maxZ).setColor(r,g,b,a);
		vertexConsumer.addVertex(matrix,(float)maxX,(float)minY,(float)maxZ).setColor(r,g,b,a);
	}
}