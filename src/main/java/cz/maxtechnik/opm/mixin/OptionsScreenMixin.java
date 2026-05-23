package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.TranslationUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen{
	protected OptionsScreenMixin(){
		super(Component.empty());
	}
	@Inject(method="init", at=@At("TAIL"), remap=false)
	private void modifyOptionsScreen(CallbackInfo ci){
		Button telemetryBtn=null;
		for(var widget: this.children()){
			if(widget instanceof Button btn){
				String key=TranslationUtils.extractKey(btn.getMessage().toString());
				if(key.equals("options.telemetry")) telemetryBtn=btn;
			}
		}
		if(telemetryBtn!=null){
			telemetryBtn.visible=false;
			telemetryBtn.active=false;
		}
	}
}