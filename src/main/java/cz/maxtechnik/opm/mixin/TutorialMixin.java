package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.init.OpmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Tutorial.class)
public class TutorialMixin{
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method="<init>", at=@At("RETURN"))
	private void opm$onInit(Minecraft mc,Options options,CallbackInfo ci){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.HIDE_TUTORIAL_TOAST.get()){
			this.minecraft.options.tutorialStep=TutorialSteps.NONE;
		}
	}

	@Inject(method={"start","tick"}, at=@At("HEAD"), cancellable=true)
	private void opm$noTutorial(CallbackInfo ci){
		if(OpmConfig.SPEC.isLoaded()&&OpmConfig.HIDE_TUTORIAL_TOAST.get()){
			ci.cancel();
		}
	}
}
