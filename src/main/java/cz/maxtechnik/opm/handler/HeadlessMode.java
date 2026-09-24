package cz.maxtechnik.opm.handler;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import cz.maxtechnik.opm.OpmMod;
import cz.maxtechnik.opm.OpmModKeys;
import cz.maxtechnik.opm.util.OpmColors;
import cz.maxtechnik.opm.util.OpmModUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
@SuppressWarnings("removal")
@EventBusSubscriber(modid=OpmMod.MODID, bus=EventBusSubscriber.Bus.GAME, value=Dist.CLIENT)
public class HeadlessMode extends Screen{
	private static final ResourceLocation SCREENSHOT_LOC=ResourceLocation.fromNamespaceAndPath(OpmMod.MODID,"afk_screenshot");
	private final NativeImage capturedImage;
	private final long startTime=System.currentTimeMillis();
	private DynamicTexture dynamicTexture;
	private static boolean active;
	public static void start(){
		Minecraft mc=Minecraft.getInstance();
		int width=mc.getMainRenderTarget().width;
		int height=mc.getMainRenderTarget().height;
		NativeImage nativeImage=new NativeImage(width,height,false);
		RenderSystem.bindTexture(mc.getMainRenderTarget().getColorTextureId());
		nativeImage.downloadTexture(0,false);
		nativeImage.flipY();
		mc.setScreen(new HeadlessMode(nativeImage));
	}
	public HeadlessMode(NativeImage nativeImage){
		super(Component.translatable("screen.opm.headless_mode"));
		active=true;
		this.capturedImage=nativeImage;
	}
	public static boolean isActive(){
		return active;
	}
	@Override
	public boolean isPauseScreen(){
		return false;
	}
	@Override
	protected void init(){
		super.init();
		if(this.dynamicTexture==null){
			this.dynamicTexture=new DynamicTexture(this.capturedImage);
			Minecraft.getInstance().getTextureManager().register(SCREENSHOT_LOC,this.dynamicTexture);
		}
		Minecraft.getInstance().getSoundManager().pause();
	}
	@Override
	public void render(@NotNull GuiGraphics gui,int mouseX,int mouseY,float partialTicks){
		OpmModUtil.drawCentredWindow(gui,280,140,this.width,this.height,1);
		gui.fill(width/2-131,height/2-58,width/2+131,height/2-42,OpmColors.DARK_GRAY);
		gui.fill(width/2-131,height/2-58,width/2-129,height/2-42,OpmColors.BLUE);
		int offset=125;
		gui.drawString(font,Component.translatable("info.opm.system"),width/2-offset,height/2-54,OpmColors.BLUE);
		String[] labels={"info.opm.afk_duration","info.opm.gpu_engine","info.opm.fps","info.opm.ram_alo","info.opm.audio"};
		for(int i=0;i<labels.length;i++) gui.drawString(font,Component.translatable(labels[i]).append(":"),width/2-offset,height/2-32+i*20,OpmColors.WHITE2);
		Component time=Component.literal(liveTime());
		Component gpuEngine=Component.translatable("info.opm.gpu_engine_value");
		Component fps=Component.translatable("info.opm.fps_value");
		Component ram=Component.literal(ram(true)+" MB / "+ram(false)+" MB");
		Component audio=Component.translatable("info.opm.audio_value");
		Component[] values={time,gpuEngine,fps,ram,audio};
		int[] colors={OpmColors.BLUE,OpmColors.GREEN,OpmColors.GREEN,OpmColors.WHITE2,OpmColors.RED,OpmColors.RED};
		for(int i=0;i<values.length;i++){
			int right=width/2+offset-this.font.width(values[i]);
			gui.drawString(font,values[i],right,height/2-32+i*20,colors[i]);
		}
		gui.drawCenteredString(font,Component.translatable("info.opm.afk_leave"),width/2,height/2+80,OpmColors.LIGHT_GRAY);
		gui.pose().pushPose();
		gui.pose().scale(2F,2F,1F);
		gui.drawCenteredString(font,Component.translatable("screen.opm.headless_mode"),width/4,height/4-48,OpmColors.BLUE);
		gui.pose().popPose();
	}
	@Override
	public boolean keyPressed(int key,int scan,int mods){
		if(key==GLFW.GLFW_KEY_ESCAPE||key==OpmModKeys.HEADLESS_MODE.getKey().getValue()){
			onClose();
			return true;
		}
		return super.keyPressed(key,scan,mods);
	}
	@Override
	public void onClose(){
		active=false;
		super.onClose();
	}
	@Override
	public void removed(){
		active=false;
		Minecraft.getInstance().getSoundManager().resume();
		Minecraft.getInstance().getTextureManager().release(SCREENSHOT_LOC);
		if(this.dynamicTexture!=null) this.dynamicTexture.close();
		Minecraft mc=Minecraft.getInstance();
		if(mc.player!=null) mc.player.displayClientMessage(Component.translatable("chat.opm.mod_prefix").append(" ").append(Component.translatable("chat.opm.afk_result")).append(": ").append(Component.literal(liveTime()).withColor(OpmColors.YELLOW)),false);
		super.removed();
	}
	@SubscribeEvent
	public static void onPlaySound(PlaySoundEvent event){
		if(isActive()) event.setSound(null);
	}
	private String liveTime(){
		long elapsedSec=Math.max(0,(System.currentTimeMillis()-startTime)/1000);
		long h=elapsedSec/3600;
		long m=(elapsedSec%3600)/60;
		long s=elapsedSec%60;
		return h>0?String.format("%d:%02d:%02d",h,m,s):String.format("%02d:%02d",m,s);
	}
	private long ram(boolean used){
		long totalMem=Runtime.getRuntime().totalMemory()/1024L/1024L;
		return used?totalMem-Runtime.getRuntime().freeMemory()/1024L/1024L:Runtime.getRuntime().maxMemory()/1024L/1024L-totalMem;
	}
}
