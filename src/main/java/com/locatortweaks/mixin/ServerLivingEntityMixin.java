package com.locatortweaks.mixin;

import com.locatortweaks.server.OptimizedEntityBlockConnection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class ServerLivingEntityMixin {

    @Inject(method = "makeWaypointConnectionWith", at = @At("RETURN"), cancellable = true)
    private void locatorTweaks$replaceWaypointConnection(ServerPlayer receiver, CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir) {
        if ((Object) this instanceof ServerPlayer sourcePlayer) {
            Optional<WaypointTransmitter.Connection> currentOpt = cir.getReturnValue();
            if (currentOpt.isPresent()) {
                Waypoint.Icon icon = sourcePlayer.waypointIcon().cloneAndAssignStyle(sourcePlayer);
                cir.setReturnValue(Optional.of(new OptimizedEntityBlockConnection(sourcePlayer, icon, receiver)));
            }
        }
    }
}
