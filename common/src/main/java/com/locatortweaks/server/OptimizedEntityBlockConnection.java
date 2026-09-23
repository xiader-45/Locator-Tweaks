package com.locatortweaks.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class OptimizedEntityBlockConnection implements WaypointTransmitter.Connection {
    private final ServerPlayer source;
    private Waypoint.Icon icon;
    private final ServerPlayer receiver;
    private BlockPos lastPosition;
    private int ticksSinceLastUpdate = 0;

    public OptimizedEntityBlockConnection(ServerPlayer source, Waypoint.Icon icon, ServerPlayer receiver) {
        this.source = source;
        this.icon = icon;
        this.receiver = receiver;
        this.lastPosition = source.blockPosition();
    }

    @Override
    public void connect() {
        if (this.receiver.connection != null) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(
                this.source.getUUID(),
                this.icon,
                this.lastPosition
            ));
        }
    }

    @Override
    public void disconnect() {
        if (this.receiver.connection != null) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(
                this.source.getUUID()
            ));
        }
    }

    @Override
    public void update() {
        if (this.receiver.connection == null) {
            return;
        }

        Waypoint.Icon currentIcon = this.source.waypointIcon().cloneAndAssignStyle(this.source);
        boolean iconChanged = !currentIcon.equals(this.icon);
        if (iconChanged) {
            this.icon = currentIcon;
        }

        BlockPos currentPos = this.source.blockPosition();
        int deltaManhattan = currentPos.distManhattan(this.lastPosition);

        if (deltaManhattan == 0 && !iconChanged) {
            return;
        }

        this.ticksSinceLastUpdate++;
        double distSq = this.source.distanceToSqr(this.receiver);

        boolean shouldUpdate;
        if (iconChanged) {
            shouldUpdate = true;
        } else if (distSq < 128.0 * 128.0) {
            shouldUpdate = true;
        } else if (distSq < 500.0 * 500.0) {
            shouldUpdate = deltaManhattan >= 2 || this.ticksSinceLastUpdate >= 10;
        } else if (distSq < 2000.0 * 2000.0) {
            shouldUpdate = deltaManhattan >= 4 || this.ticksSinceLastUpdate >= 20;
        } else {
            shouldUpdate = deltaManhattan >= 8 || this.ticksSinceLastUpdate >= 40;
        }

        if (shouldUpdate) {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(
                this.source.getUUID(),
                this.icon,
                currentPos
            ));
            this.lastPosition = currentPos;
            this.ticksSinceLastUpdate = 0;
        }
    }

    @Override
    public boolean isBroken() {
        if (this.source.isRemoved() || this.receiver.isRemoved() || this.receiver.hasDisconnected()) {
            return true;
        }
        if (this.source.level() != this.receiver.level()) {
            return true;
        }
        return WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver);
    }
}
