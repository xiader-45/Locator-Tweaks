package com.locatortweaks.util;

import com.locatortweaks.config.CustomWaypoint;
import com.locatortweaks.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.Vec3i;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomWaypointManager {
    private static final Map<UUID, CustomWaypoint> ACTIVE_WAYPOINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, WaypointSnapshot> SYNCED_SNAPSHOTS = new ConcurrentHashMap<>();

    private record WaypointSnapshot(int x, int y, int z, int color, String name) {}

    public static boolean isCustomWaypoint(UUID id) {
        if (id == null) return false;
        return ACTIVE_WAYPOINTS.containsKey(id);
    }

    public static CustomWaypoint getActiveWaypoint(UUID id) {
        if (id == null) return null;
        return ACTIVE_WAYPOINTS.get(id);
    }

    public static Map<UUID, CustomWaypoint> getActiveWaypoints() {
        return Collections.unmodifiableMap(ACTIVE_WAYPOINTS);
    }

    public static void untrackWaypoint(Minecraft client, UUID id) {
        if (id == null) return;
        if (client != null && client.getConnection() != null) {
            ClientWaypointManager manager = client.getConnection().getWaypointManager();
            if (manager != null) {
                manager.untrackWaypoint(TrackedWaypoint.empty(id));
            }
        }
        ACTIVE_WAYPOINTS.remove(id);
        SYNCED_SNAPSHOTS.remove(id);
    }

    public static void forceSync(Minecraft client) {
        tick(client);
    }

    public static void clearAllWaypoints(Minecraft client) {
        if (client != null && client.getConnection() != null) {
            ClientWaypointManager manager = client.getConnection().getWaypointManager();
            if (manager != null) {
                for (UUID id : new HashSet<>(SYNCED_SNAPSHOTS.keySet())) {
                    manager.untrackWaypoint(TrackedWaypoint.empty(id));
                }
            }
        }
        ACTIVE_WAYPOINTS.clear();
        SYNCED_SNAPSHOTS.clear();
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (!ACTIVE_WAYPOINTS.isEmpty() || !SYNCED_SNAPSHOTS.isEmpty()) {
                clearAllWaypoints(client);
            }
            return;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;
        ClientWaypointManager manager = connection.getWaypointManager();
        if (manager == null) return;

        ModConfig cfg = ModConfig.getInstance();
        ModConfig.CustomWaypointDisplayMode mode = cfg.customWaypointsDisplayMode;
        if (mode == ModConfig.CustomWaypointDisplayMode.DISABLED) {
            if (!ACTIVE_WAYPOINTS.isEmpty() || !SYNCED_SNAPSHOTS.isEmpty()) {
                clearAllWaypoints(client);
            }
            return;
        }

        String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
        String currentDim = client.level.dimension().identifier().toString();

        List<CustomWaypoint> list = cfg.getCustomWaypoints(worldKey);
        Map<UUID, CustomWaypoint> validWaypoints = new HashMap<>();

        for (CustomWaypoint wp : list) {
            if (!wp.enabled) continue;
            if (mode == ModConfig.CustomWaypointDisplayMode.FAVORITES_ONLY && !wp.favorite) continue;
            if (!currentDim.equalsIgnoreCase(wp.dimension)) continue;
            validWaypoints.put(wp.id, wp);
        }

        // Remove no longer valid waypoints
        Iterator<Map.Entry<UUID, WaypointSnapshot>> syncIt = SYNCED_SNAPSHOTS.entrySet().iterator();
        while (syncIt.hasNext()) {
            Map.Entry<UUID, WaypointSnapshot> entry = syncIt.next();
            UUID id = entry.getKey();
            if (!validWaypoints.containsKey(id)) {
                manager.untrackWaypoint(TrackedWaypoint.empty(id));
                ACTIVE_WAYPOINTS.remove(id);
                syncIt.remove();
            }
        }

        // Add or update valid waypoints
        for (CustomWaypoint wp : validWaypoints.values()) {
            WaypointSnapshot snap = new WaypointSnapshot(wp.x, wp.y, wp.z, wp.color, wp.name);
            WaypointSnapshot prevSnap = SYNCED_SNAPSHOTS.get(wp.id);

            if (prevSnap == null || !prevSnap.equals(snap)) {
                Waypoint.Icon icon = new Waypoint.Icon();
                icon.color = Optional.of(wp.color | 0xFF000000);

                TrackedWaypoint tw = TrackedWaypoint.setPosition(
                    wp.id,
                    icon,
                    new Vec3i(wp.x, wp.y, wp.z)
                );

                manager.trackWaypoint(tw);
                SYNCED_SNAPSHOTS.put(wp.id, snap);
                ACTIVE_WAYPOINTS.put(wp.id, wp);
            }
        }
    }
}
