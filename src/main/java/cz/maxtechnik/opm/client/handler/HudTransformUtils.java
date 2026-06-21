package cz.maxtechnik.opm.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
public class HudTransformUtils{
	//Zkontroluje, zda by se měl HUD renderovat (řeší hideGui, F1Handler, config screen atd.)
	public static boolean shouldRender(){
		Minecraft mc=Minecraft.getInstance();
		if(mc.player==null||mc.options.hideGui) return false;
		if(F1Handler.shouldHideHUD()) return false;
		return !(mc.screen instanceof cz.maxtechnik.opm.client.screen.OpmConfigScreen);
	}
	//Aplikuje posun a škálování na PoseStack vzhledem k zadanému středu [cx, cy]
	public static void pushTransform(PoseStack pose,float cx,float cy,double scale,int xOffset,int yOffset){
		if(scale==1.0&&xOffset==0&&yOffset==0) return;
		pose.pushPose();
		pose.translate(xOffset,yOffset,0);
		if(scale!=1.0){
			pose.translate(cx,cy,0);
			pose.scale((float)scale,(float)scale,1.0f);
			pose.translate(-cx,-cy,0);
		}
	}
	//Ukončí transformaci (pop) pokud byla aplikována
	public static void popTransform(PoseStack pose,double scale,int xOffset,int yOffset){
		if(scale!=1.0||xOffset!=0||yOffset!=0){
			pose.popPose();
		}
	}
}
