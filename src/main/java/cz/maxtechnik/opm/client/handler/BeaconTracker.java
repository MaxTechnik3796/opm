package cz.maxtechnik.opm.client.handler;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class BeaconTracker {
    // Bezpečný globální set načtených majáků na klientovi
    public static final Set<BeaconBlockEntity> BEACONS = Collections.newSetFromMap(new WeakHashMap<>());
}