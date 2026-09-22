package cz.maxtechnik.opm.mixin.button_remover;

import cz.maxtechnik.opm.OpmModUtil;
import cz.maxtechnik.opm.config.OpmModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TitleScreen.class)
public class TitleScreenMixin{
	@Inject(method="init", at=@At("TAIL"))
	private void removeRealmsButton(CallbackInfo ci){
		if(!OpmModConfig.REMOVE_REALMS_BUTTON.get()) return;
		TitleScreen self=(TitleScreen)(Object)this;
		Button singleplayer=null;
		Button multiplayer=null;
		Button realms=null;
		for(var widget: self.children()){
			if(widget instanceof Button button){
				switch(OpmModUtil.keyExtractor(button.getMessage().toString())){
					case "menu.singleplayer" -> singleplayer=button;
					case "menu.multiplayer" -> multiplayer=button;
					case "menu.online" -> realms=button;
				}
			}
		}
		if(realms==null||singleplayer==null||multiplayer==null) return;
		realms.visible=false;
		realms.active=false;
		int multiplayerY=multiplayer.getY();
		int realmsY=realms.getY();
		singleplayer.setY(multiplayerY);
		multiplayer.setY(realmsY);
	}
}