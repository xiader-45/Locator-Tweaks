package com.locatortweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.locatortweaks.LocatorTweaks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        .create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("locator-tweaks.json");

    private static ModConfig instance;

    public BarDisplayMode barDisplayMode = BarDisplayMode.DEFAULT;
    public BarDisplayMode lastActiveBarDisplayMode = BarDisplayMode.DEFAULT;
    public PlayerMarkerMode playerMarkerMode = PlayerMarkerMode.DEFAULT;
    public LocatorPosition locatorPosition = LocatorPosition.BOTTOM;
    public boolean showCardinalDirections = true;

    // Backward compatibility for older config versions
    private Boolean enableLocator = null;
    private Boolean showXpWithLocator = null;
    private Boolean showPlayerMarkers = null;
    private Boolean showPlayerHeads = null;

    public boolean isPlayerMarkersEnabled() {
        return playerMarkerMode != PlayerMarkerMode.DISABLED;
    }

    public boolean isPlayerHeadsEnabled() {
        return playerMarkerMode == PlayerMarkerMode.HEADS;
    }
    public boolean showHeightArrows = true;
    public InfoDisplayMode markerInfoMode = InfoDisplayMode.HOLD;
    public InfoDisplayMode distanceDisplayMode = InfoDisplayMode.HOLD;
    public int barBackgroundOpacity = 100;
    public int markerOpacity = 100;
    public int maxDistance = 0; // 0 = unlimited

    public boolean showDistanceOnShift = false;
    public boolean showMarkerOnNametag = false;
    public boolean showDeathPoint = false;
    public boolean hasDeathPoint = false;
    public String deathDimension = "";
    public int deathX = 0;
    public int deathY = 0;
    public int deathZ = 0;

    public static class DeathPointData {
        public String dimension = "";
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

    public static class BedPointData {
        public String dimension = "";
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

    public Map<String, BedPointData> bedPoints = new HashMap<>();

    public boolean showNetherPortalPoint = false;
    public boolean hasOverworldPortal = false;
    public int overworldPortalX = 0;
    public int overworldPortalY = 0;
    public int overworldPortalZ = 0;
    public boolean hasNetherPortal = false;
    public int netherPortalX = 0;
    public int netherPortalY = 0;
    public int netherPortalZ = 0;

    public static class PortalPointData {
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
    public int openPlayersKey = 74;
    public int createWaypointKey = 66;
    public int openWaypointsKey = 85;

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
        if (deathPoints != null && deathPoints.containsKey(worldKey)) {
            return deathPoints.get(worldKey);
        }
        if (hasDeathPoint && ("default".equals(worldKey) || deathPoints == null || deathPoints.isEmpty())) {
            return new DeathPointData(deathDimension, deathX, deathY, deathZ);
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
        if (deathPoints == null || deathPoints.isEmpty()) {
            this.hasDeathPoint = false;
        }
    }

    public BedPointData getBedPoint(String worldKey) {
        if (bedPoints != null && bedPoints.containsKey(worldKey)) {
            return bedPoints.get(worldKey);
        }
        if (hasBedPoint && ("default".equals(worldKey) || bedPoints == null || bedPoints.isEmpty())) {
            return new BedPointData(bedDimension, bedX, bedY, bedZ);
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
        if (bedPoints == null || bedPoints.isEmpty()) {
            this.hasBedPoint = false;
        }
    }

    public PortalPointData getOverworldPortal(String worldKey) {
        if (overworldPortals != null && overworldPortals.containsKey(worldKey)) {
            return overworldPortals.get(worldKey);
        }
        if (hasOverworldPortal && ("default".equals(worldKey) || overworldPortals == null || overworldPortals.isEmpty())) {
            return new PortalPointData(overworldPortalX, overworldPortalY, overworldPortalZ);
        }
        return null;
    }

    public void setOverworldPortal(String worldKey, int x, int y, int z) {
        if (overworldPortals == null) {
            overworldPortals = new HashMap<>();
        }
        overworldPortals.put(worldKey, new PortalPointData(x, y, z));
        this.hasOverworldPortal = true;
        this.overworldPortalX = x;
        this.overworldPortalY = y;
        this.overworldPortalZ = z;
    }

    public PortalPointData getNetherPortal(String worldKey) {
        if (netherPortals != null && netherPortals.containsKey(worldKey)) {
            return netherPortals.get(worldKey);
        }
        if (hasNetherPortal && ("default".equals(worldKey) || netherPortals == null || netherPortals.isEmpty())) {
            return new PortalPointData(netherPortalX, netherPortalY, netherPortalZ);
        }
        return null;
    }

    public void setNetherPortal(String worldKey, int x, int y, int z) {
        if (netherPortals == null) {
            netherPortals = new HashMap<>();
        }
        netherPortals.put(worldKey, new PortalPointData(x, y, z));
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
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
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
                instance.barDisplayMode = BarDisplayMode.DEFAULT;
            }
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
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LocatorTweaks.LOGGER.error("Failed to save locator-tweaks configuration", e);
        }
    }

}
