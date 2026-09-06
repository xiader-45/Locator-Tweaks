package com.locatortweaks.util;

import com.locatortweaks.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

import java.util.Objects;
import java.util.UUID;

public class NetherPortalManager {
    public static final UUID NETHER_PORTAL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private static BlockPos lastSyncedPos = null;
    private static String lastSyncedDimension = null;
    private static String lastSyncedWorldKey = null;

    public static boolean isPortalPoint(UUID uuid) {
        return NETHER_PORTAL_UUID.equals(uuid);
    }

    public static boolean hasActivePortalPoint() {
        Minecraft client = Minecraft.getInstance();
        return hasActivePortalPoint(client);
    }

    public static boolean hasActivePortalPoint(Minecraft client) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showNetherPortalPoint) return false;
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        return cfg.getOverworldPortal(worldKey) != null || cfg.getNetherPortal(worldKey) != null;
    }

    public static void recordPortal(String dimension, BlockPos pos) {
        if (pos == null || dimension == null) return;
        Minecraft client = Minecraft.getInstance();
        ModConfig cfg = ModConfig.getInstance();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);

        if (Level.OVERWORLD.identifier().toString().equals(dimension)) {
            cfg.setOverworldPortal(worldKey, pos.getX(), pos.getY(), pos.getZ());
            ModConfig.save();
            syncWaypoint(client);
        } else if (Level.NETHER.identifier().toString().equals(dimension)) {
            cfg.setNetherPortal(worldKey, pos.getX(), pos.getY(), pos.getZ());
            ModConfig.save();
            syncWaypoint(client);
        }
    }

    public static BlockPos getPos(UUID uuid) {
        if (!isPortalPoint(uuid)) return null;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        ModConfig cfg = ModConfig.getInstance();
        String currentDim = client.level.dimension().identifier().toString();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);

        if (Level.OVERWORLD.identifier().toString().equals(currentDim)) {
            ModConfig.PortalPointData p = cfg.getOverworldPortal(worldKey);
            return p != null ? new BlockPos(p.x, p.y, p.z) : null;
        } else if (Level.NETHER.identifier().toString().equals(currentDim)) {
            ModConfig.PortalPointData p = cfg.getNetherPortal(worldKey);
            return p != null ? new BlockPos(p.x, p.y, p.z) : null;
        }
        return null;
    }

    public static String getName(UUID uuid) {
        if (!isPortalPoint(uuid)) return null;
        return Component.translatable("locator-tweaks.waypoint.nether_portal").getString();
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        BlockPos playerPos = client.player.blockPosition();
        boolean inPortal = client.level.getBlockState(playerPos).is(Blocks.NETHER_PORTAL)
            || client.level.getBlockState(playerPos.above()).is(Blocks.NETHER_PORTAL);

        if (inPortal) {
            recordPortal(client.level.dimension().identifier().toString(), playerPos);
        }

        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showNetherPortalPoint) {
            removeWaypoint(client);
            return;
        }

        syncWaypoint(client);
    }

    public static void syncWaypoint(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null || client.level == null) return;
        ClientWaypointManager manager = connection.getWaypointManager();
        ModConfig cfg = ModConfig.getInstance();

        if (!cfg.showNetherPortalPoint) {
            removeWaypoint(client);
            return;
        }

        String currentDim = client.level.dimension().identifier().toString();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        BlockPos targetPos = null;

        if (Level.OVERWORLD.identifier().toString().equals(currentDim)) {
            ModConfig.PortalPointData p = cfg.getOverworldPortal(worldKey);
            if (p != null) {
                targetPos = new BlockPos(p.x, p.y, p.z);
            }
        } else if (Level.NETHER.identifier().toString().equals(currentDim)) {
            ModConfig.PortalPointData p = cfg.getNetherPortal(worldKey);
            if (p != null) {
                targetPos = new BlockPos(p.x, p.y, p.z);
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
                NETHER_PORTAL_UUID,
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
            manager.untrackWaypoint(TrackedWaypoint.empty(NETHER_PORTAL_UUID));
        }
    }

    public static void resetState() {
        lastSyncedPos = null;
        lastSyncedDimension = null;
        lastSyncedWorldKey = null;
    }
}
