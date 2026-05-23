package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.TranslationUtils;
import cz.maxtechnik.opm.init.OpmConfig;
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
		if(!OpmConfig.NO_REALMS_BUTTON.get()) return;
		TitleScreen self=(TitleScreen)(Object)this;
		Button singleplayer=null;
		Button multiplayer=null;
		Button realms=null;
		for(var widget: self.children()){
			if(widget instanceof Button btn){
				String key=TranslationUtils.extractKey(btn.getMessage().toString());
				switch(key){
					case "menu.singleplayer" -> singleplayer=btn;
					case "menu.multiplayer" -> multiplayer=btn;
					case "menu.online" -> realms=btn;
				}
			}
		}
		if(realms==null||singleplayer==null||multiplayer==null) return;
		// Schovej Realms
		realms.visible=false;
		realms.active=false;
		// Posuň Singleplayer na pozici Multiplayer
		// Posuň Multiplayer na pozici Realms
		int multiY=multiplayer.getY();
		int realmsY=realms.getY();
		singleplayer.setY(multiY);
		multiplayer.setY(realmsY);
	}
}