package cz.maxtechnik.opm.mixin.button_remover;

import cz.maxtechnik.opm.OpmModConfig;
import cz.maxtechnik.opm.OpmModTexts;
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
		if(!OpmModConfig.REMOVE_TELEMETRY_BUTTON.get()) return;
		Button telemetryBtn=null;
		for(var widget: this.children()) if(widget instanceof Button button) if(OpmModTexts.keyExtractor(button.getMessage().toString()).equals("options.telemetry")) telemetryBtn=button;
		if(telemetryBtn!=null){
			telemetryBtn.visible=false;
			telemetryBtn.active=false;
		}
	}
}