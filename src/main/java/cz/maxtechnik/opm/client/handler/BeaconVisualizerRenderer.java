package cz.maxtechnik.opm.client.handler;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import cz.maxtechnik.opm.OpmMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Matrix4f;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.GAME)
public class BeaconVisualizerRenderer{
	private static final RenderType OPM_BEACON_ZONE=RenderType.create(
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
	private static final RenderType OPM_BEACON_LINES=RenderType.create(
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
					.setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // X-Ray skrz bloky
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.setLineState(new RenderStateShard.LineStateShard(java.util.OptionalDouble.of(2.5D)))
					.createCompositeState(false)
	);
	private static int getCustomEffectColor(Holder<MobEffect> effectHolder){
		if(effectHolder==null||!effectHolder.isBound()) return -1;
		MobEffect effect=effectHolder.value();
		if(effect==MobEffects.MOVEMENT_SPEED.value()) return 0x00D0FF; // Speed -> Azurová
		if(effect==MobEffects.DIG_SPEED.value()) return 0xFFD700; // Haste -> Zlatá
		if(effect==MobEffects.DAMAGE_RESISTANCE.value()) return 0x7F8C8D; // Resistance -> Šedá
		if(effect==MobEffects.JUMP.value()) return 0x2ECC71; // Jump Boost -> Zelená
		if(effect==MobEffects.DAMAGE_BOOST.value()) return 0xFF0000; // Strength -> Červená
		if(effect==MobEffects.REGENERATION.value()) return 0xFF69B4; // Regeneration -> Růžová
		return -1;
	}
	// EVENT 1: Sledujeme, kdy hráč klikne na maják, abychom znali jeho pozici pro uložení efektu
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
		if(event.getLevel().isClientSide()){
			if(event.getLevel().getBlockState(event.getPos()).is(Blocks.BEACON)){
				BeaconTracker.lastInteractedBeacon=event.getPos();
			}
		}
	}
	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event){
		if(!BeaconVisualizerHandler.isActive()) return;
		if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
		Minecraft mc=Minecraft.getInstance();
		if(mc.level==null||mc.player==null) return;
		// EVENT 2: Pokud má hráč otevřené GUI majáku, ukládáme vybraný efekt real-time do klientské cache
		if(mc.screen instanceof BeaconScreen&&BeaconTracker.lastInteractedBeacon!=null){
			if(mc.player.containerMenu instanceof BeaconMenu menu){
				Holder<MobEffect> primary=menu.getPrimaryEffect();
				if(primary!=null&&primary.isBound()){
					BeaconTracker.CACHED_EFFECTS.put(BeaconTracker.lastInteractedBeacon,primary);
				}
			}
		}
		Vec3 cameraPos=event.getCamera().getPosition();
		PoseStack poseStack=event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource=mc.renderBuffers().bufferSource();
		// FÁZE 1: Stěny
		VertexConsumer zoneConsumer=bufferSource.getBuffer(OPM_BEACON_ZONE);
		renderBeacons(poseStack,cameraPos,zoneConsumer,true);
		bufferSource.endBatch(OPM_BEACON_ZONE);
		// FÁZE 2: Rentgenové hrany
		VertexConsumer lineConsumer=bufferSource.getBuffer(OPM_BEACON_LINES);
		renderBeacons(poseStack,cameraPos,lineConsumer,false);
		bufferSource.endBatch(OPM_BEACON_LINES);
	}
	private static void renderBeacons(PoseStack poseStack,Vec3 cameraPos,VertexConsumer consumer,boolean isZone){
		for(BeaconBlockEntity beacon: BeaconTracker.BEACONS){
			if(beacon.isRemoved()||beacon.getLevel()==null) continue;
			if(beacon.getBeamSections().isEmpty()) continue; // Paprsek nesvítí -> maják je vypnutý
			BlockPos pos=beacon.getBlockPos();
			// KLIENTSKÝ SKEN: Spočítáme velikost pyramidy přímo na klientovi
			int levels=calculateClientLevels(beacon.getLevel(),pos);
			int range=(levels+1)*10;
			float alpha=isZone?0.12F:0.85F;
			int colorInt=0x00D0FF; // Výchozí barva (Azurová)
			// Pokusíme se vytáhnout efekt z naší klientské paměti
			Holder<MobEffect> cachedEffect=BeaconTracker.CACHED_EFFECTS.get(pos);
			if(cachedEffect!=null&&cachedEffect.isBound()){
				int customColor=getCustomEffectColor(cachedEffect);
				colorInt=(customColor!=-1)?customColor:cachedEffect.value().getColor();
			}
			float r=((colorInt>>16)&0xFF)/255.0F;
			float g=((colorInt>>8)&0xFF)/255.0F;
			float b=(colorInt&0xFF)/255.0F;
			poseStack.pushPose();
			double x=pos.getX()-cameraPos.x;
			double y=pos.getY()-cameraPos.y;
			double z=pos.getZ()-cameraPos.z;
			poseStack.translate(x,y,z);
			Matrix4f matrix=poseStack.last().pose();
			float minX=-range;
			float minY=-range;
			float minZ=-range;
			float maxX=range+1;
			float maxY=range+1;
			float maxZ=range+1;
			if(isZone){
				// Spodek
				consumer.addVertex(matrix,minX,minY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,minY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,minY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,minY,maxZ).setColor(r,g,b,alpha);
				// Vršek
				consumer.addVertex(matrix,minX,maxY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,maxY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,maxY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,maxY,minZ).setColor(r,g,b,alpha);
				// Sever
				consumer.addVertex(matrix,minX,minY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,maxY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,maxY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,minY,minZ).setColor(r,g,b,alpha);
				// Jih
				consumer.addVertex(matrix,minX,minY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,minY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,maxY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,maxY,maxZ).setColor(r,g,b,alpha);
				// Západ
				consumer.addVertex(matrix,minX,minY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,minY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,maxY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,minX,maxY,minZ).setColor(r,g,b,alpha);
				// Východ
				consumer.addVertex(matrix,maxX,minY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,maxY,minZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,maxY,maxZ).setColor(r,g,b,alpha);
				consumer.addVertex(matrix,maxX,minY,maxZ).setColor(r,g,b,alpha);
			}else{
				// Čáry X
				drawDebugLine(matrix,consumer,minX,minY,minZ,maxX,minY,minZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,minX,maxY,minZ,maxX,maxY,minZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,minX,minY,maxZ,maxX,minY,maxZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,minX,maxY,maxZ,maxX,maxY,maxZ,r,g,b,alpha);
				// Čáry Y
				drawDebugLine(matrix,consumer,minX,minY,minZ,minX,maxY,minZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,maxX,minY,minZ,maxX,maxY,minZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,minX,minY,maxZ,minX,maxY,maxZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,maxX,minY,maxZ,maxX,maxY,maxZ,r,g,b,alpha);
				// Čáry Z
				drawDebugLine(matrix,consumer,minX,minY,minZ,minX,minY,maxZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,maxX,minY,minZ,maxX,minY,maxZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,minX,maxY,minZ,minX,maxY,maxZ,r,g,b,alpha);
				drawDebugLine(matrix,consumer,maxX,maxY,minZ,maxX,maxY,maxZ,r,g,b,alpha);
			}
			poseStack.popPose();
		}
	}
	// Pomocná metoda pro real-time klientský výpočet úrovně pyramidy (100% Client-Side)
	private static int calculateClientLevels(Level level,BlockPos pos){
		int clientLevels=0;
		for(int i=1;i<=4;i++){
			int y=pos.getY()-i;
			if(y<level.getMinBuildHeight()) break;
			boolean layerValid=true;
			for(int x=pos.getX()-i;x<=pos.getX()+i;x++){
				for(int z=pos.getZ()-i;z<=pos.getZ()+i;z++){
					if(!level.getBlockState(new BlockPos(x,y,z)).is(BlockTags.BEACON_BASE_BLOCKS)){
						layerValid=false;
						break;
					}
				}
				if(!layerValid) break;
			}
			if(layerValid){
				clientLevels=i;
			}else{
				break;
			}
		}
		return clientLevels;
	}
	private static void drawDebugLine(Matrix4f matrix,VertexConsumer consumer,float x1,float y1,float z1,float x2,float y2,float z2,float r,float g,float b,float a){
		consumer.addVertex(matrix,x1,y1,z1).setColor(r,g,b,a);
		consumer.addVertex(matrix,x2,y2,z2).setColor(r,g,b,a);
	}
}