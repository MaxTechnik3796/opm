package cz.maxtechnik.opm.mixin;

import cz.maxtechnik.opm.client.HeadlessModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    /**
     * Přeskočí tikání vizuálních efektů světa (počasí, animace bloků, nebeský rotátor).
     * Tato metoda zpracovává čistě klientské vizuály – server o ní neví.
     */
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void onTickNonPassenger(Entity entity, CallbackInfo ci) {
        if (!HeadlessModeHandler.isHeadlessMode()) return;

        // Tikáme pouze lokálního hráče (aby nás server neodpojil).
        // Všechny ostatní entity (mobové, zvířata, jiní hráči) přeskakujeme –
        // jsou to čistě klientské interpolace/animace, server si je spravuje sám.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity == mc.player) return;

        ci.cancel();
    }

    /**
     * Zrušení výpočtu animací a přehrávání zvuků prostředí vázaných na klientský level tick
     * (déšť, bouřka, umbiální zvuky biomy, animované bloky jako voda/láva/oheň).
     * Server o tomto VŮBEC neví – je to čistě klientský kosmetický výpočet.
     */
    @Inject(method = "doAnimateTick", at = @At("HEAD"), cancellable = true)
    private void onDoAnimateTick(CallbackInfo ci) {
        if (HeadlessModeHandler.isHeadlessMode()) {
            ci.cancel();
        }
    }
}
