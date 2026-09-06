package com.locatortweaks.util;

import com.locatortweaks.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LodestoneManager {
    public record LodestoneData(UUID id, String name, BlockPos pos, String dimension, boolean customName) {}

    private static final Map<UUID, LodestoneData> activeLodestones = new HashMap<>();
    private static int tickCounter = 0;

    public static boolean isLodestone(UUID uuid) {
        return activeLodestones.containsKey(uuid);
    }

    public static String getName(UUID uuid) {
        LodestoneData data = activeLodestones.get(uuid);
        return data != null ? data.name : null;
    }

    public static BlockPos getPos(UUID uuid) {
        LodestoneData data = activeLodestones.get(uuid);
        return data != null ? data.pos : null;
    }

    public static Map<UUID, LodestoneData> getActiveLodestones() {
        return activeLodestones;
    }

    public static boolean hasActiveLodestones() {
        return ModConfig.getInstance().showLodestonePoints && !activeLodestones.isEmpty();
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            clearAll(client);
            return;
        }

        tickCounter++;
        if (tickCounter % 10 != 0) {
            return;
        }

        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.showLodestonePoints) {
            if (!activeLodestones.isEmpty()) {
                clearAll(client);
            }
            return;
        }

        LocalPlayer player = client.player;
        String currentDim = client.level.dimension().identifier().toString();
        Map<UUID, LodestoneData> found = new HashMap<>();

        int invSize = player.getInventory().getContainerSize();
        for (int i = 0; i < invSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            scanStack(stack, 0, found, currentDim);
        }

        syncLodestones(client, found);
    }

    private static void scanStack(ItemStack stack, int depth, Map<UUID, LodestoneData> found, String currentDim) {
        if (stack == null || stack.isEmpty() || depth > 3) {
            return;
        }

        if (stack.has(DataComponents.LODESTONE_TRACKER)) {
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            if (tracker != null && tracker.target().isPresent()) {
                GlobalPos gp = tracker.target().get();
                String dim = gp.dimension().identifier().toString();
                if (dim.equals(currentDim)) {
                    BlockPos pos = gp.pos();
                    UUID id = UUID.nameUUIDFromBytes(("lodestone:" + dim + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ()).getBytes(StandardCharsets.UTF_8));
                    boolean isCustom = stack.has(DataComponents.CUSTOM_NAME);
                    String name = stack.getHoverName().getString();

                    if (found.containsKey(id)) {
                        if (isCustom && !found.get(id).customName) {
                            found.put(id, new LodestoneData(id, name, pos, dim, true));
                        }
                    } else {
                        found.put(id, new LodestoneData(id, name, pos, dim, isCustom));
                    }
                }
            }
        }

        if (stack.has(DataComponents.BUNDLE_CONTENTS)) {
            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                bundle.itemCopyStream().forEach(nested -> scanStack(nested, depth + 1, found, currentDim));
            }
        }

        if (stack.has(DataComponents.CONTAINER)) {
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container != null) {
                container.nonEmptyItemCopyStream().forEach(nested -> scanStack(nested, depth + 1, found, currentDim));
            }
        }
    }

    private static void syncLodestones(Minecraft client, Map<UUID, LodestoneData> found) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;
        ClientWaypointManager manager = connection.getWaypointManager();

        for (Map.Entry<UUID, LodestoneData> entry : found.entrySet()) {
            if (!activeLodestones.containsKey(entry.getKey())) {
                Waypoint.Icon icon = new Waypoint.Icon();
                TrackedWaypoint waypoint = TrackedWaypoint.setPosition(entry.getKey(), icon, entry.getValue().pos);
                manager.trackWaypoint(waypoint);
            }
        }

        for (UUID id : activeLodestones.keySet()) {
            if (!found.containsKey(id)) {
                manager.untrackWaypoint(TrackedWaypoint.empty(id));
            }
        }

        activeLodestones.clear();
        activeLodestones.putAll(found);
    }

    public static void clearAll(Minecraft client) {
        if (client != null && client.getConnection() != null) {
            ClientWaypointManager manager = client.getConnection().getWaypointManager();
            for (UUID id : activeLodestones.keySet()) {
                manager.untrackWaypoint(TrackedWaypoint.empty(id));
            }
        }
        activeLodestones.clear();
    }
}
