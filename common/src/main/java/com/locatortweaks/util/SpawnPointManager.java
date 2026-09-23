package com.locatortweaks.util;

import com.locatortweaks.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

import java.util.Objects;
import java.util.UUID;

public class SpawnPointManager {
    public static void forceSpawnPoint(net.minecraft.core.BlockPos pos, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) { /* dummy */ }
    public static final UUID SPAWN_POINT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static BlockPos potentialBedPos = null;
    private static String potentialBedDim = null;

    private static BlockPos lastSyncedPos = null;
    private static String lastSyncedDimension = null;
    private static String lastSyncedWorldKey = null;
    private static int validationTickCounter = 0;

    public static String getCurrentWorldKey(Minecraft client) {
        return WorldKeyUtil.getCurrentWorldKey(client);
    }

    public static boolean isSpawnPoint(UUID uuid) {
        return SPAWN_POINT_UUID.equals(uuid);
    }

    public static boolean isFavorite(UUID uuid) {
        if (!isSpawnPoint(uuid)) return false;
        Minecraft client = Minecraft.getInstance();
        ModConfig cfg = ModConfig.getInstance();
        String worldKey = getCurrentWorldKey(client);
        ModConfig.BedPointData bed = cfg.getBedPoint(worldKey);
        if (bed != null) {
            return bed.favorite;
        }
        ModConfig.WorldSpawnPointData wsp = cfg.getWorldSpawn(worldKey);
        return wsp != null && wsp.favorite;
    }

    public static boolean hasActiveSpawnPoint() {
        Minecraft client = Minecraft.getInstance();
        return hasActiveSpawnPoint(client);
    }

    public static boolean hasActiveSpawnPoint(Minecraft client) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showSpawnPoint) return false;
        if (client.level == null) return false;
        return getPos(SPAWN_POINT_UUID) != null;
    }

    public static void recordPotentialBed(BlockPos pos, String dimension) {
        potentialBedPos = pos;
        potentialBedDim = dimension;
    }

    public static void confirmPotentialBed() {
        if (potentialBedPos != null && potentialBedDim != null) {
            recordBed(potentialBedPos, potentialBedDim);
        }
    }

    public static void recordBed(BlockPos pos, String dimension) {
        if (pos == null || dimension == null) return;
        Minecraft client = Minecraft.getInstance();
        ModConfig cfg = ModConfig.getInstance();
        String worldKey = getCurrentWorldKey(client);
        cfg.setBedPoint(worldKey, dimension, pos.getX(), pos.getY(), pos.getZ());
        ModConfig.save();
        syncWaypoint(client);
    }

    public static BlockPos getPos(UUID uuid) {
        if (!isSpawnPoint(uuid)) return null;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        ModConfig cfg = ModConfig.getInstance();
        String currentDim = client.level.dimension().identifier().toString();
        String worldKey = getCurrentWorldKey(client);
        ModConfig.BedPointData bed = cfg.getBedPoint(worldKey);

        if (bed != null) {
            if (bed.enabled && currentDim.equals(bed.dimension)) {
                return new BlockPos(bed.x, bed.y, bed.z);
            }
            return null;
        }

        if (client.level.dimension() == Level.OVERWORLD && client.level.getRespawnData() != null) {
            ModConfig.WorldSpawnPointData wsp = cfg.getWorldSpawn(worldKey);
            if (wsp == null || (!wsp.ignored && wsp.enabled)) {
                return client.level.getRespawnData().pos();
            }
        }
        return null;
    }

    public static String getName(UUID uuid) {
        if (!isSpawnPoint(uuid)) return null;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        ModConfig cfg = ModConfig.getInstance();
        String currentDim = client.level.dimension().identifier().toString();
        String worldKey = getCurrentWorldKey(client);
        ModConfig.BedPointData bed = cfg.getBedPoint(worldKey);

        if (bed != null && currentDim.equals(bed.dimension)) {
            return Component.translatable("locator-tweaks.waypoint.respawn_point").getString();
        }
        return Component.translatable("locator-tweaks.waypoint.world_spawn").getString();
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (client.player.isSleeping()) {
            BlockPos sPos = client.player.getSleepingPos().orElse(client.player.blockPosition());
            recordBed(sPos, client.level.dimension().identifier().toString());
        }

        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showSpawnPoint) {
            removeWaypoint(client);
            return;
        }

        validationTickCounter++;
        if (validationTickCounter % 20 == 0) {
            String worldKey = getCurrentWorldKey(client);
            ModConfig.BedPointData bed = cfg.getBedPoint(worldKey);
            if (bed != null && client.level.dimension().identifier().toString().equals(bed.dimension)) {
                BlockPos bedPos = new BlockPos(bed.x, bed.y, bed.z);
                if (client.level.isLoaded(bedPos)) {
                    BlockState bState = client.level.getBlockState(bedPos);
                    boolean valid = (bState.getBlock() instanceof BedBlock) || (bState.getBlock() instanceof RespawnAnchorBlock);
                    if (!valid) {
                        cfg.removeBedPoint(worldKey);
                        ModConfig.save();
                    }
                }
            }
        }

        syncWaypoint(client);
    }

    public static void syncWaypoint(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null || client.level == null) return;
        ClientWaypointManager manager = connection.getWaypointManager();
        ModConfig cfg = ModConfig.getInstance();

        if (!cfg.showSpawnPoint) {
            removeWaypoint(client);
            return;
        }

        String currentDim = client.level.dimension().identifier().toString();
        String worldKey = getCurrentWorldKey(client);
        ModConfig.BedPointData bed = cfg.getBedPoint(worldKey);
        BlockPos targetPos = null;

        if (bed != null) {
            if (bed.enabled && currentDim.equals(bed.dimension)) {
                targetPos = new BlockPos(bed.x, bed.y, bed.z);
            }
        } else if (client.level.dimension() == Level.OVERWORLD && client.level.getRespawnData() != null) {
            ModConfig.WorldSpawnPointData wsp = cfg.getWorldSpawn(worldKey);
            if (wsp == null || (!wsp.ignored && wsp.enabled)) {
                targetPos = client.level.getRespawnData().pos();
            }
        }

        if (targetPos != null) {
            if (Objects.equals(targetPos, lastSyncedPos)
                && Objects.equals(currentDim, lastSyncedDimension)
                && Objects.equals(worldKey, lastSyncedWorldKey)) {
                return; // Already tracked and unchanged
            }

            Waypoint.Icon icon = new Waypoint.Icon();
            TrackedWaypoint waypoint = TrackedWaypoint.setPosition(
                SPAWN_POINT_UUID,
                icon,
                new Vec3i(targetPos.getX(), targetPos.getY(), targetPos.getZ())
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
            manager.untrackWaypoint(TrackedWaypoint.empty(SPAWN_POINT_UUID));
        }
    }

    public static void resetState() {
        lastSyncedPos = null;
        lastSyncedDimension = null;
        lastSyncedWorldKey = null;
        potentialBedPos = null;
        potentialBedDim = null;
        validationTickCounter = 0;
    }
}
