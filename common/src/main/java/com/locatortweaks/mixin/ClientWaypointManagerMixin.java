package com.locatortweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Comparator;
import java.util.stream.Stream;

@Mixin(ClientWaypointManager.class)
public abstract class ClientWaypointManagerMixin {
    @WrapOperation(
        method = "forEachWaypoint",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"
        )
    )
    private Stream<TrackedWaypoint> locatorTweaks$prioritizeDeathPoint(
        Stream<TrackedWaypoint> instance,
        Comparator<TrackedWaypoint> originalComparator,
        Operation<Stream<TrackedWaypoint>> original
    ) {
        Comparator<TrackedWaypoint> customComparator = (w1, w2) -> {
            boolean d1 = DeathPointManager.isDeathPoint(w1.id().left().orElse(null));
            boolean d2 = DeathPointManager.isDeathPoint(w2.id().left().orElse(null));
            if (d1 && !d2) {
                return 1;
            }
            if (!d1 && d2) {
                return -1;
            }
            boolean sp1 = SpawnPointManager.isSpawnPoint(w1.id().left().orElse(null)) || NetherPortalManager.isPortalPoint(w1.id().left().orElse(null));
            boolean sp2 = SpawnPointManager.isSpawnPoint(w2.id().left().orElse(null)) || NetherPortalManager.isPortalPoint(w2.id().left().orElse(null));
            if (sp1 && !sp2) {
                return 1;
            }
            if (!sp1 && sp2) {
                return -1;
            }
            return originalComparator.compare(w1, w2);
        };
        return original.call(instance, customComparator);
    }
}
