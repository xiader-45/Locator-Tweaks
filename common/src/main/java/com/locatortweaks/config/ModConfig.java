package com.locatortweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.locatortweaks.LocatorTweaks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {
    public static java.nio.file.Path configDir;

    private static final Gson GSON = new GsonBuilder()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        .create();
    private static java.nio.file.Path getConfigPath() { return configDir.resolve("locator-tweaks.json"); }

    private static ModConfig instance;

    public BarDisplayMode barDisplayMode = BarDisplayMode.XP_WITH_MARKERS;
    public BarDisplayMode lastActiveBarDisplayMode = BarDisplayMode.XP_WITH_MARKERS;
    public PlayerMarkerMode playerMarkerMode = PlayerMarkerMode.DEFAULT;
    public LocatorPosition locatorPosition = LocatorPosition.BOTTOM;
    public boolean showCardinalDirections = false;

    // Backward compatibility for older config versions
    private Boolean enableLocator = null;
    private Boolean showXpWithLocator = null;
    private Boolean showPlayerMarkers = null;
    private Boolean showPlayerHeads = null;

    public java.util.List<String> lodestoneHidden;
    public java.util.Map<String, java.util.Map<String, LodestonePointData>> lodestones;

    public boolean isPlayerMarkersEnabled() {
        return playerMarkerMode != PlayerMarkerMode.DISABLED;
    }

    public boolean isPlayerHeadsEnabled() {
        return playerMarkerMode == PlayerMarkerMode.HEADS;
    }
    public boolean showHeightArrows = true;
    public InfoDisplayMode markerInfoMode = InfoDisplayMode.DISABLED;
    public InfoDisplayMode distanceDisplayMode = InfoDisplayMode.DISABLED;
    public int barBackgroundOpacity = 100;
    public int markerOpacity = 100;
    public int maxDistance = 0; // 0 = unlimited

    public boolean showDistanceOnShift = false;
    public boolean showMarkerOnNametag = false;
    public boolean showDeathPoint = true;
    public boolean hasDeathPoint = false;
    public String deathDimension = "";
    public int deathX = 0;
    public int deathY = 0;
    public int deathZ = 0;

    public static class DeathPointData {
        public String dimension = "";
        public boolean enabled = true;
        public boolean favorite = false;
        public boolean ignored = false;
        public int x = 0;
        public int y = 0;
        public int z = 0;

        public DeathPointData() {}

        public DeathPointData(String dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public Map<String, DeathPointData> deathPoints = new HashMap<>();

    public boolean showSpawnPoint = false;
    public boolean hasBedPoint = false;
    public String bedDimension = "";
    public int bedX = 0;
    public int bedY = 0;
    public int bedZ = 0;

    public static class LodestonePointData {
        public String id;
        public String dimension = "";
        public int x = 0;
        public int y = 0;
        public int z = 0;
        public String name = "";
        public boolean enabled = true;
        public boolean favorite = false;
        
        public LodestonePointData() {}
        public LodestonePointData(String id, String dim, int x, int y, int z, String name) {
            this.id = id; this.dimension = dim; this.x = x; this.y = y; this.z = z; this.name = name;
        }
    }

    public static class BedPointData {
        public String dimension = "";
        public boolean enabled = true;
        public boolean favorite = false;
        public boolean ignored = false;
        public int x = 0;
        public int y = 0;
        public int z = 0;

        public BedPointData() {}

        public BedPointData(String dimension, int x, int y, int z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class WorldSpawnPointData {
        public boolean enabled = true;
        public boolean favorite = false;
        public boolean ignored = false;

        public WorldSpawnPointData() {}
    }

    public Map<String, BedPointData> bedPoints = new HashMap<>();
    public Map<String, WorldSpawnPointData> worldSpawns = new HashMap<>();

    public boolean showNetherPortalPoint = true;
    public boolean hasOverworldPortal = false;
    public int overworldPortalX = 0;
    public int overworldPortalY = 0;
    public int overworldPortalZ = 0;
    public boolean hasNetherPortal = false;
    public int netherPortalX = 0;
    public int netherPortalY = 0;
    public int netherPortalZ = 0;

    public static class PortalPointData {
        public boolean enabled = true;
        public boolean favorite = false;
        public boolean ignored = false;
        public int x = 0;
        public int y = 0;
        public int z = 0;

        public PortalPointData() {}

        public PortalPointData(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public Map<String, PortalPointData> overworldPortals = new HashMap<>();
    public Map<String, PortalPointData> netherPortals = new HashMap<>();

    public boolean showLodestonePoints = false;
    public boolean showLodestoneNameOnShift = false;
    public boolean showWaypointNameOnShift = false;
    public boolean showPlayerNameOnShift = false;
    public boolean showPlayerHealthOnShift = false;
    public boolean preventDistanceScaling = false;
    public boolean showOnlyWithCompass = false;
    public boolean showHeadOutline = false;
    public LocatorDisplayMode displayMode = LocatorDisplayMode.ALL;

    public CustomWaypointDisplayMode customWaypointsDisplayMode = CustomWaypointDisplayMode.ALL;
    public Map<String, List<CustomWaypoint>> customWaypoints = new HashMap<>();

    public int toggleLocatorKey = -1;
    public int openPlayersKey = InputConstants.KEY_J;
    public int createWaypointKey = InputConstants.KEY_B;
    public int openWaypointsKey = InputConstants.KEY_U;

    public Map<UUID, PlayerLocatorConfig> perPlayerSettings = new HashMap<>();

    public enum InfoDisplayMode {
        HOLD("hold"),
        ALWAYS("always"),
        DISABLED("disabled");

        private final String name;

        InfoDisplayMode(String name) {
            this.name = name;
        }

        public Component getDisplayName() {
            return Component.translatable("locator-tweaks.config.info_display_mode." + this.name);
        }
    }

    public enum BarDisplayMode {
        DEFAULT("default"),
        XP_WITH_MARKERS("xp_with_markers"),
        NO_XP("no_xp"),
        DISABLED("disabled");

        private final String name;

        BarDisplayMode(String name) {
            this.name = name;
        }

        public Component getDisplayName() {
            return Component.translatable("locator-tweaks.config.bar_display_mode." + this.name);
        }
    }

    public enum PlayerMarkerMode {
        DEFAULT("default"),
        HEADS("heads"),
        DISABLED("disabled");

        private final String name;

        PlayerMarkerMode(String name) {
            this.name = name;
        }

        public Component getDisplayName() {
            return Component.translatable("locator-tweaks.config.player_marker_mode." + this.name);
        }
    }

    public enum LocatorPosition {
        BOTTOM("bottom"),
        TOP("top");

        private final String name;

        LocatorPosition(String name) {
            this.name = name;
        }

        public Component getDisplayName() {
            return Component.translatable("locator-tweaks.config.locator_position." + this.name);
        }
    }

    public enum LocatorDisplayMode {
        ALL("locator-tweaks.display_mode.all"),
        FAVORITES_ONLY("locator-tweaks.display_mode.favorites_only");

        private final String translationKey;

        LocatorDisplayMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }
    }

    public enum CustomWaypointDisplayMode {
        ALL("locator-tweaks.waypoint_display_mode.all"),
        FAVORITES_ONLY("locator-tweaks.waypoint_display_mode.favorites_only"),
        DISABLED("locator-tweaks.waypoint_display_mode.disabled");

        private final String translationKey;

        CustomWaypointDisplayMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(this.translationKey);
        }
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public List<CustomWaypoint> getCustomWaypoints(String worldKey) {
        if (customWaypoints == null) {
            customWaypoints = new HashMap<>();
        }
        return customWaypoints.computeIfAbsent(worldKey, k -> new ArrayList<>());
    }

    public void addCustomWaypoint(String worldKey, CustomWaypoint wp) {
        List<CustomWaypoint> list = getCustomWaypoints(worldKey);
        list.add(wp);
        save();
    }

    public void removeCustomWaypoint(String worldKey, UUID id) {
        List<CustomWaypoint> list = getCustomWaypoints(worldKey);
        list.removeIf(w -> w.id.equals(id));
        save();
    }

    public java.util.Map<String, LodestonePointData> getLodestones(String worldKey) {
        if (lodestones == null) lodestones = new java.util.HashMap<>();
        return lodestones.computeIfAbsent(worldKey, k -> new java.util.HashMap<>());
    }

    public boolean isLodestoneEnabled(String key) { return lodestoneHidden == null || !lodestoneHidden.contains(key); }

    public boolean isLodestoneIgnored(String key) { return lodestoneHidden != null && lodestoneHidden.contains(key); }

    public void setLodestoneEnabled(String key, boolean enabled) {
        if (!enabled) {
            if (lodestoneHidden == null) lodestoneHidden = new java.util.ArrayList<>();
            if (!lodestoneHidden.contains(key)) lodestoneHidden.add(key);
        } else {
            if (lodestoneHidden != null) lodestoneHidden.remove(key);
        }
        save();
    }

    public void setLodestoneFavorite(String worldKey, String key, boolean fav) {
        LodestonePointData lp = getLodestones(worldKey).get(key);
        if (lp != null) {
            lp.favorite = fav;
            save();
        }
    }

    public void removeLodestone(String worldKey, String key) {
        getLodestones(worldKey).remove(key);
        save();
    }

    public void ignoreLodestone(String key) {
        if (lodestoneHidden == null) lodestoneHidden = new java.util.ArrayList<>();
        if (!lodestoneHidden.contains(key)) lodestoneHidden.add(key);
        save();
    }

    public void removeOverworldPortal(String worldKey) {
        PortalPointData p = getOverworldPortal(worldKey);
        if (p != null) {
            p.ignored = true;
            p.enabled = false;
        }
        this.hasOverworldPortal = false;
        save();
    }

    public void removeNetherPortal(String worldKey) {
        PortalPointData p = getNetherPortal(worldKey);
        if (p != null) {
            p.ignored = true;
            p.enabled = false;
        }
        this.hasNetherPortal = false;
        save();
    }

    public WorldSpawnPointData getWorldSpawn(String worldKey) {
        if (worldSpawns == null) {
            worldSpawns = new HashMap<>();
        }
        return worldSpawns.get(worldKey);
    }

    public WorldSpawnPointData getOrCreateWorldSpawn(String worldKey) {
        if (worldSpawns == null) {
            worldSpawns = new HashMap<>();
        }
        return worldSpawns.computeIfAbsent(worldKey, k -> new WorldSpawnPointData());
    }

    public void removeWorldSpawn(String worldKey) {
        WorldSpawnPointData wsp = getOrCreateWorldSpawn(worldKey);
        wsp.ignored = true;
        save();
    }

    public void updateCustomWaypoint(String worldKey, CustomWaypoint wp) {
        List<CustomWaypoint> list = getCustomWaypoints(worldKey);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(wp.id)) {
                list.set(i, wp);
                save();
                return;
            }
        }
        list.add(wp);
        save();
    }

    public CustomWaypoint getCustomWaypoint(String worldKey, UUID id) {
        List<CustomWaypoint> list = getCustomWaypoints(worldKey);
        for (CustomWaypoint wp : list) {
            if (wp.id.equals(id)) {
                return wp;
            }
        }
        return null;
    }

    public DeathPointData getDeathPoint(String worldKey) {
        if (deathPoints == null) {
            deathPoints = new HashMap<>();
        }
        if (deathPoints.containsKey(worldKey)) {
            return deathPoints.get(worldKey);
        }
        if (hasDeathPoint && ("default".equals(worldKey) || deathPoints.isEmpty())) {
            DeathPointData data = new DeathPointData(deathDimension, deathX, deathY, deathZ);
            deathPoints.put(worldKey, data);
            return data;
        }
        return null;
    }

    public void setDeathPoint(String worldKey, String dimension, int x, int y, int z) {
        if (deathPoints == null) {
            deathPoints = new HashMap<>();
        }
        deathPoints.put(worldKey, new DeathPointData(dimension, x, y, z));
        this.hasDeathPoint = true;
        this.deathDimension = dimension;
        this.deathX = x;
        this.deathY = y;
        this.deathZ = z;
    }

    public void removeDeathPoint(String worldKey) {
        if (deathPoints != null) {
            deathPoints.remove(worldKey);
        }
        this.hasDeathPoint = false;
        save();
    }

    public BedPointData getBedPoint(String worldKey) {
        if (bedPoints == null) {
            bedPoints = new HashMap<>();
        }
        if (bedPoints.containsKey(worldKey)) {
            return bedPoints.get(worldKey);
        }
        if (hasBedPoint && ("default".equals(worldKey) || bedPoints.isEmpty())) {
            BedPointData data = new BedPointData(bedDimension, bedX, bedY, bedZ);
            bedPoints.put(worldKey, data);
            return data;
        }
        return null;
    }

    public void setBedPoint(String worldKey, String dimension, int x, int y, int z) {
        if (bedPoints == null) {
            bedPoints = new HashMap<>();
        }
        bedPoints.put(worldKey, new BedPointData(dimension, x, y, z));
        this.hasBedPoint = true;
        this.bedDimension = dimension;
        this.bedX = x;
        this.bedY = y;
        this.bedZ = z;
    }

    public void removeBedPoint(String worldKey) {
        if (bedPoints != null) {
            bedPoints.remove(worldKey);
        }
        this.hasBedPoint = false;
        save();
    }

    public PortalPointData getOverworldPortal(String worldKey) {
        if (overworldPortals == null) {
            overworldPortals = new HashMap<>();
        }
        if (overworldPortals.containsKey(worldKey)) {
            return overworldPortals.get(worldKey);
        }
        if (hasOverworldPortal && ("default".equals(worldKey) || overworldPortals.isEmpty())) {
            PortalPointData data = new PortalPointData(overworldPortalX, overworldPortalY, overworldPortalZ);
            overworldPortals.put(worldKey, data);
            save();
            return data;
        }
        return null;
    }

    public void setOverworldPortal(String worldKey, int x, int y, int z) {
        if (overworldPortals == null) {
            overworldPortals = new HashMap<>();
        }
        PortalPointData existing = overworldPortals.get(worldKey);
        if (existing != null) {
            existing.x = x;
            existing.y = y;
            existing.z = z;
            existing.ignored = false;
        } else {
            overworldPortals.put(worldKey, new PortalPointData(x, y, z));
        }
        this.hasOverworldPortal = true;
        this.overworldPortalX = x;
        this.overworldPortalY = y;
        this.overworldPortalZ = z;
    }

    public PortalPointData getNetherPortal(String worldKey) {
        if (netherPortals == null) {
            netherPortals = new HashMap<>();
        }
        if (netherPortals.containsKey(worldKey)) {
            return netherPortals.get(worldKey);
        }
        if (hasNetherPortal && ("default".equals(worldKey) || netherPortals.isEmpty())) {
            PortalPointData data = new PortalPointData(netherPortalX, netherPortalY, netherPortalZ);
            netherPortals.put(worldKey, data);
            save();
            return data;
        }
        return null;
    }

    public void setNetherPortal(String worldKey, int x, int y, int z) {
        if (netherPortals == null) {
            netherPortals = new HashMap<>();
        }
        PortalPointData existing = netherPortals.get(worldKey);
        if (existing != null) {
            existing.x = x;
            existing.y = y;
            existing.z = z;
            existing.ignored = false;
        } else {
            netherPortals.put(worldKey, new PortalPointData(x, y, z));
        }
        this.hasNetherPortal = true;
        this.netherPortalX = x;
        this.netherPortalY = y;
        this.netherPortalZ = z;
    }

    public PlayerLocatorConfig getPlayerConfig(UUID uuid) {
        if (uuid == null) return null;
        return perPlayerSettings.get(uuid);
    }

    public PlayerLocatorConfig getOrCreatePlayerConfig(UUID uuid, String name) {
        if (uuid == null) return new PlayerLocatorConfig(name);
        return perPlayerSettings.computeIfAbsent(uuid, k -> new PlayerLocatorConfig(name));
    }

    public void resetPlayerConfig(UUID uuid) {
        if (uuid != null) {
            PlayerLocatorConfig cfg = perPlayerSettings.get(uuid);
            if (cfg != null) {
                if (cfg.favorite) {
                    cfg.reset(uuid);
                } else {
                    perPlayerSettings.remove(uuid);
                }
                save();
            }
        }
    }

    public static void load() {
        if (Files.exists(getConfigPath())) {
            try (Reader reader = Files.newBufferedReader(getConfigPath())) {
                instance = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                LocatorTweaks.LOGGER.error("Failed to load locator-tweaks configuration, creating defaults", e);
            }
        }

        if (instance == null) {
            instance = new ModConfig();
        }

        if (instance.perPlayerSettings == null) {
            instance.perPlayerSettings = new HashMap<>();
        }

        if (instance.bedPoints == null) {
            instance.bedPoints = new HashMap<>();
        }

        if (instance.worldSpawns == null) {
            instance.worldSpawns = new HashMap<>();
        }

        if (instance.deathPoints == null) {
            instance.deathPoints = new HashMap<>();
        }

        if (instance.overworldPortals == null) {
            instance.overworldPortals = new HashMap<>();
        }

        if (instance.netherPortals == null) {
            instance.netherPortals = new HashMap<>();
        }

        if (instance.markerInfoMode == null) {
            instance.markerInfoMode = instance.showWaypointNameOnShift ? InfoDisplayMode.HOLD : InfoDisplayMode.DISABLED;
        }
        if (instance.distanceDisplayMode == null) {
            instance.distanceDisplayMode = instance.showDistanceOnShift ? InfoDisplayMode.HOLD : InfoDisplayMode.DISABLED;
        }

        if (instance.customWaypoints == null) {
            instance.customWaypoints = new HashMap<>();
        }

        if (instance.customWaypointsDisplayMode == null) {
            instance.customWaypointsDisplayMode = CustomWaypointDisplayMode.ALL;
        }

        if (instance.playerMarkerMode == null) {
            if (instance.showPlayerMarkers != null && !instance.showPlayerMarkers) {
                instance.playerMarkerMode = PlayerMarkerMode.DISABLED;
            } else if (instance.showPlayerHeads != null && instance.showPlayerHeads) {
                instance.playerMarkerMode = PlayerMarkerMode.HEADS;
            } else {
                instance.playerMarkerMode = PlayerMarkerMode.DEFAULT;
            }
        }

        if (instance.maxDistance >= 2001) {
            instance.maxDistance = 0;
        }

        if (instance.barDisplayMode == null) {
            if (instance.enableLocator != null && !instance.enableLocator) {
                instance.barDisplayMode = BarDisplayMode.DISABLED;
            } else if (instance.showXpWithLocator != null && instance.showXpWithLocator) {
                instance.barDisplayMode = BarDisplayMode.XP_WITH_MARKERS;
            } else {
                instance.barDisplayMode = BarDisplayMode.XP_WITH_MARKERS;
            }
        }

        if (instance.lastActiveBarDisplayMode == null) {
            instance.lastActiveBarDisplayMode = BarDisplayMode.XP_WITH_MARKERS;
        }

        // Migrate old GLFW keycodes (74 -> KEY_J, 66 -> KEY_B, 85 -> KEY_U)
        if (instance.openPlayersKey == 74) {
            instance.openPlayersKey = InputConstants.KEY_J;
        }
        if (instance.createWaypointKey == 66) {
            instance.createWaypointKey = InputConstants.KEY_B;
        }
        if (instance.openWaypointsKey == 85) {
            instance.openWaypointsKey = InputConstants.KEY_U;
        }

        if (instance.locatorPosition == null) {
            instance.locatorPosition = LocatorPosition.BOTTOM;
        }

        if (!instance.showWaypointNameOnShift && (instance.showPlayerNameOnShift || instance.showLodestoneNameOnShift)) {
            instance.showWaypointNameOnShift = true;
        }

        save();
    }

    public static void save() {
        if (instance == null) {
            instance = new ModConfig();
        }

        try {
            Files.createDirectories(getConfigPath().getParent());
            try (Writer writer = Files.newBufferedWriter(getConfigPath())) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LocatorTweaks.LOGGER.error("Failed to save locator-tweaks configuration", e);
        }
    }

}
