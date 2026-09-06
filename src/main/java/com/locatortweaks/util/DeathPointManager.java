package com.locatortweaks.util;

import com.locatortweaks.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DeathPointManager {
    public static final UUID DEATH_POINT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final int DEATH_MARKER_COLOR = 0xFFD84444;

    private static long lastDeathTimestamp = 0L;
    private static BlockPos lastSyncedPos = null;
    private static String lastSyncedDimension = null;
    private static String lastSyncedWorldKey = null;

    public static boolean isDeathPoint(UUID uuid) {
        return DEATH_POINT_UUID.equals(uuid);
    }

    public static boolean hasActiveDeathPoint() {
        Minecraft client = Minecraft.getInstance();
        return hasActiveDeathPoint(client);
    }

    public static boolean hasActiveDeathPoint(Minecraft client) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showDeathPoint) return false;
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        return cfg.getDeathPoint(worldKey) != null;
    }

    public static String getName() {
        return Component.translatable("locator-tweaks.waypoint.death_point").getString();
    }

    public static BlockPos getPos() {
        Minecraft client = Minecraft.getInstance();
        ModConfig cfg = ModConfig.getInstance();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        ModConfig.DeathPointData dp = cfg.getDeathPoint(worldKey);
        if (dp != null) {
            return new BlockPos(dp.x, dp.y, dp.z);
        }
        return null;
    }

    public static void recordDeath(LocalPlayer player) {
        if (player == null || player.level() == null) return;
        Minecraft client = Minecraft.getInstance();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        String dim = player.level().dimension().identifier().toString();
        BlockPos pos = player.blockPosition();

        ModConfig cfg = ModConfig.getInstance();
        cfg.setDeathPoint(worldKey, dim, pos.getX(), pos.getY(), pos.getZ());
        ModConfig.save();

        lastDeathTimestamp = System.currentTimeMillis();
        syncWaypoint(client);
    }

    public static void clearDeathPoint() {
        Minecraft client = Minecraft.getInstance();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        ModConfig cfg = ModConfig.getInstance();
        cfg.removeDeathPoint(worldKey);
        ModConfig.save();
        removeWaypoint(client);
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (client.player.isDeadOrDying()) {
            recordDeath(client.player);
            return;
        }

        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showDeathPoint) {
            removeWaypoint(client);
            return;
        }

        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        ModConfig.DeathPointData dp = cfg.getDeathPoint(worldKey);
        if (dp == null) {
            removeWaypoint(client);
            return;
        }

        String currentDim = client.level.dimension().identifier().toString();
        if (!currentDim.equals(dp.dimension)) {
            removeWaypoint(client);
            return;
        }

        // Grace period: do not immediately clear death point if player just respawned near it
        long timeSinceDeath = System.currentTimeMillis() - lastDeathTimestamp;
        if (timeSinceDeath >= 10000L) {
            BlockPos playerPos = client.player.blockPosition();
            BlockPos deathPos = new BlockPos(dp.x, dp.y, dp.z);
            double distSq = client.player.position().distanceToSqr(deathPos.getX() + 0.5, deathPos.getY() + 0.5, deathPos.getZ() + 0.5);
            if (playerPos.equals(deathPos) || distSq < 4.0) {
                clearDeathPoint();
                return;
            }
        }

        syncWaypoint(client);
    }

    public static void syncWaypoint(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null || client.level == null) return;
        ClientWaypointManager manager = connection.getWaypointManager();
        ModConfig cfg = ModConfig.getInstance();

        if (!cfg.showDeathPoint) {
            removeWaypoint(client);
            return;
        }

        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        ModConfig.DeathPointData dp = cfg.getDeathPoint(worldKey);
        String currentDim = client.level.dimension().identifier().toString();

        if (dp != null && currentDim.equals(dp.dimension)) {
            BlockPos targetPos = new BlockPos(dp.x, dp.y, dp.z);
            if (Objects.equals(targetPos, lastSyncedPos)
                && Objects.equals(currentDim, lastSyncedDimension)
                && Objects.equals(worldKey, lastSyncedWorldKey)) {
                return; // Already tracked and unchanged
            }

            Waypoint.Icon icon = new Waypoint.Icon();
            icon.color = Optional.of(DEATH_MARKER_COLOR);
            TrackedWaypoint waypoint = TrackedWaypoint.setPosition(
                DEATH_POINT_UUID,
                icon,
                new Vec3i(dp.x, dp.y, dp.z)
            );
            manager.trackWaypoint(waypoint);
            lastSyncedPos = targetPos;
            lastSyncedDimension = currentDim;
            lastSyncedWorldKey = worldKey;
        } else {
            removeWaypoint(client);
        }
    }

    public static void removeWaypoint(Minecraft client) {
        if (lastSyncedPos != null || lastSyncedDimension != null) {
            lastSyncedPos = null;
            lastSyncedDimension = null;
            lastSyncedWorldKey = null;
        }
        if (client != null && client.getConnection() != null) {
            ClientWaypointManager manager = client.getConnection().getWaypointManager();
            manager.untrackWaypoint(TrackedWaypoint.empty(DEATH_POINT_UUID));
        }
    }

    public static void resetState() {
        lastSyncedPos = null;
        lastSyncedDimension = null;
        lastSyncedWorldKey = null;
        lastDeathTimestamp = 0L;
    }
}
