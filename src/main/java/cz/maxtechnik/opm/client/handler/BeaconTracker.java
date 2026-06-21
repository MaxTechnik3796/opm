package cz.maxtechnik.opm.client.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
public class BeaconTracker{
	// Seznam načtených majáků na klientovi
	public static final Set<BeaconBlockEntity> BEACONS=Collections.newSetFromMap(new WeakHashMap<>());
	// KLIENTSKÁ CACHE: Uložené efekty pro konkrétní souřadnice majáků
	public static final Map<BlockPos,Holder<MobEffect>> CACHED_EFFECTS=new ConcurrentHashMap<>();
	// Pomocná proměnná pro sledování, na který maják hráč zrovna kliknul
	public static BlockPos lastInteractedBeacon=null;
}