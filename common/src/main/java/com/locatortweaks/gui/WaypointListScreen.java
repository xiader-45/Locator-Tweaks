package com.locatortweaks.gui;

import com.locatortweaks.config.CustomWaypoint;
import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.LodestoneManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;
import com.locatortweaks.util.WorldKeyUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

import java.util.*;

public class WaypointListScreen extends Screen {
    private static final Identifier PIN_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_0");
    private static final Identifier DEATH_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/death_point_0");
    private static final Identifier SPAWN_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/spawn_point_0");
    private static final Identifier PORTAL_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/nether_portal_point_0");
    private static final Identifier LODESTONE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/lodestone_point_0");

    private static final Identifier CROSS_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/cross");
    private static final Identifier EYE_VISIBLE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/eye_visible");
    private static final Identifier EYE_HIDDEN_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/eye_hidden");
    private static final Identifier STAR_ACTIVE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/star_active");
    private static final Identifier STAR_INACTIVE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/star_inactive");

    private final Screen parentScreen;
    private final String initialDimension;
    private String selectedDimension;
    private EditBox searchBox;
    public WaypointListWidget listWidget;
    private final List<WaypointItem> allWaypoints = new ArrayList<>();
    private String currentSearch = "";
    public int lastMouseX;
    public int lastMouseY;

    public WaypointListScreen(Screen parentScreen) {
        this(parentScreen, null);
    }

    public WaypointListScreen(Screen parentScreen, String initialDimension) {
        super(Component.translatable("locator-tweaks.gui.waypoints.title"));
        this.parentScreen = parentScreen;
        this.initialDimension = initialDimension;
        this.selectedDimension = initialDimension;
    }

    public Screen getParentScreen() {
        return this.parentScreen;
    }

    public String getSelectedDimension() {
        return this.selectedDimension;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.player != null && mc.level != null;
        String currentWorldDim = inGame ? mc.level.dimension().identifier().toString() : "minecraft:overworld";
        if (this.selectedDimension == null) {
            this.selectedDimension = currentWorldDim;
        }

        populateWaypoints();

        List<String> availableDimensions = buildAvailableDimensions(currentWorldDim);
        if (!availableDimensions.contains(this.selectedDimension)) {
            this.selectedDimension = availableDimensions.contains(currentWorldDim) ? currentWorldDim : availableDimensions.get(0);
        }

        int dimWidth = 100;
        int searchWidth = 150;
        int btnSize = 20;
        int gap = 4;
        int totalTopWidth = dimWidth + gap + searchWidth + gap + btnSize;
        int topY = 22;
        int dimX = this.width / 2 - totalTopWidth / 2;
        int searchX = dimX + dimWidth + gap;
        int addX = searchX + searchWidth + gap;

        CycleButton<String> dimButton = CycleButton.<String>builder(WaypointListScreen::getDimensionDisplayName, this.selectedDimension)
            .withValues(availableDimensions)
            .displayOnlyValue()
            .withTooltip(dimId -> Tooltip.create(Component.translatable("locator-tweaks.gui.dimension.tooltip", getDimensionDisplayName(dimId))))
            .create(dimX, topY, dimWidth, btnSize, Component.translatable("locator-tweaks.gui.dimension.label"), (btn, newDim) -> {
                this.selectedDimension = newDim;
                refreshFilteredList();
            });
        this.addRenderableWidget(dimButton);

        this.searchBox = new EditBox(this.font, searchX, topY, searchWidth, btnSize, Component.translatable("locator-tweaks.gui.search"));
        this.searchBox.setHint(Component.translatable("locator-tweaks.gui.waypoints.search_hint"));
        this.searchBox.setValue(this.currentSearch);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        Button addBtn = Button.builder(
            Component.literal("+"),
            b -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.setScreenAndShow(new WaypointEditScreen(this, null));
                }
            }
        )
        .bounds(addX, topY, btnSize, btnSize)
        .tooltip(Tooltip.create(Component.translatable(inGame ? "locator-tweaks.gui.waypoints.add" : "locator-tweaks.gui.waypoints.add.not_in_game")))
        .build();
        addBtn.active = inGame;
        this.addRenderableWidget(addBtn);

        int listTop = 48;
        int listBottom = this.height - 36;
        int listHeight = listBottom - listTop;
        this.listWidget = new WaypointListWidget(this.minecraft, this.width, listHeight, listTop, 36);
        this.addRenderableWidget(this.listWidget);

        Button doneBtn = Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
            .build();
        this.addRenderableWidget(doneBtn);

        refreshFilteredList();
    }

    private List<String> buildAvailableDimensions(String currentDim) {
        LinkedHashSet<String> dims = new LinkedHashSet<>();
        dims.add("minecraft:overworld");
        dims.add("minecraft:the_nether");
        dims.add("minecraft:the_end");
        if (currentDim != null && !currentDim.isEmpty()) {
            dims.add(currentDim);
        }
        for (WaypointItem wp : allWaypoints) {
            if (wp.dimension != null && !wp.dimension.isEmpty()) {
                dims.add(wp.dimension);
            }
        }
        dims.add("all");
        return new ArrayList<>(dims);
    }

    public static Component getDimensionDisplayName(String dimId) {
        if ("all".equalsIgnoreCase(dimId)) {
            return Component.translatable("locator-tweaks.gui.dimension.all");
        }
        if ("minecraft:overworld".equalsIgnoreCase(dimId)) {
            return Component.translatable("locator-tweaks.gui.dimension.overworld");
        }
        if ("minecraft:the_nether".equalsIgnoreCase(dimId)) {
            return Component.translatable("locator-tweaks.gui.dimension.the_nether");
        }
        if ("minecraft:the_end".equalsIgnoreCase(dimId)) {
            return Component.translatable("locator-tweaks.gui.dimension.the_end");
        }
        int colonIdx = dimId.indexOf(':');
        String path = (colonIdx >= 0) ? dimId.substring(colonIdx + 1) : dimId;
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return Component.literal(sb.toString());
    }

    private void populateWaypoints() {
        allWaypoints.clear();
        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.player != null && mc.level != null;
        String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);
        ModConfig cfg = ModConfig.getInstance();

        List<CustomWaypoint> customList = cfg.getCustomWaypoints(worldKey);
        for (CustomWaypoint cwp : customList) {
            boolean featDisabled = (cfg.customWaypointsDisplayMode == ModConfig.CustomWaypointDisplayMode.DISABLED);
            allWaypoints.add(new WaypointItem(
                WaypointType.CUSTOM,
                "custom_" + cwp.id.toString(),
                cwp.name,
                cwp.dimension,
                cwp.x, cwp.y, cwp.z,
                cwp.enabled,
                cwp.favorite,
                PIN_SPRITE,
                cwp.color,
                true,
                cwp,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.custom") : null,
                () -> {
                    cwp.enabled = !cwp.enabled;
                    cfg.updateCustomWaypoint(worldKey, cwp);
                    CustomWaypointManager.forceSync(mc);
                },
                () -> {
                    cwp.favorite = !cwp.favorite;
                    cfg.updateCustomWaypoint(worldKey, cwp);
                    CustomWaypointManager.forceSync(mc);
                },
                () -> {
                    cfg.removeCustomWaypoint(worldKey, cwp.id);
                    CustomWaypointManager.untrackWaypoint(mc, cwp.id);
                    CustomWaypointManager.forceSync(mc);
                }
            ));
        }

        ModConfig.DeathPointData dp = cfg.getDeathPoint(worldKey);
        if (dp != null) {
            boolean featDisabled = !cfg.showDeathPoint;
            String dim = (dp.dimension != null && !dp.dimension.isEmpty()) ? dp.dimension : (inGame ? mc.level.dimension().identifier().toString() : "minecraft:overworld");
            allWaypoints.add(new WaypointItem(
                WaypointType.DEATH,
                "death_point",
                Component.translatable("locator-tweaks.waypoint.death_point").getString(),
                dim,
                dp.x, dp.y, dp.z,
                dp.enabled,
                dp.favorite,
                DEATH_SPRITE,
                0xFFFFFFFF,
                false,
                null,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.death") : null,
                () -> {
                    dp.enabled = !dp.enabled;
                    ModConfig.save();
                    DeathPointManager.syncWaypoint(mc);
                },
                () -> {
                    dp.favorite = !dp.favorite;
                    ModConfig.save();
                },
                () -> {
                    cfg.removeDeathPoint(worldKey);
                    ModConfig.save();
                    DeathPointManager.clearDeathPoint();
                }
            ));
        }

        ModConfig.BedPointData bed = cfg.getBedPoint(worldKey);
        if (bed != null) {
            boolean featDisabled = !cfg.showSpawnPoint;
            String dim = (bed.dimension != null && !bed.dimension.isEmpty()) ? bed.dimension : "minecraft:overworld";
            allWaypoints.add(new WaypointItem(
                WaypointType.SPAWN,
                "spawn_bed",
                Component.translatable("locator-tweaks.waypoint.respawn_point").getString(),
                dim,
                bed.x, bed.y, bed.z,
                bed.enabled,
                bed.favorite,
                SPAWN_SPRITE,
                0xFFFFFFFF,
                false,
                null,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.spawn") : null,
                () -> {
                    bed.enabled = !bed.enabled;
                    ModConfig.save();
                    SpawnPointManager.syncWaypoint(mc);
                },
                () -> {
                    bed.favorite = !bed.favorite;
                    ModConfig.save();
                },
                () -> {
                    cfg.removeBedPoint(worldKey);
                    ModConfig.save();
                    SpawnPointManager.syncWaypoint(mc);
                }
            ));
        } else if (inGame && mc.level.dimension() == Level.OVERWORLD && mc.level.getRespawnData() != null) {
            boolean featDisabled = !cfg.showSpawnPoint;
            BlockPos sp = mc.level.getRespawnData().pos();
            allWaypoints.add(new WaypointItem(
                WaypointType.SPAWN,
                "spawn_world",
                Component.translatable("locator-tweaks.waypoint.world_spawn").getString(),
                "minecraft:overworld",
                sp.getX(), sp.getY(), sp.getZ(),
                true,
                false,
                SPAWN_SPRITE,
                0xFFFFFFFF,
                false,
                null,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.spawn") : null,
                null,
                null,
                null
            ));
        }

        ModConfig.PortalPointData owPortal = cfg.getOverworldPortal(worldKey);
        if (owPortal != null && !owPortal.ignored) {
            boolean featDisabled = !cfg.showNetherPortalPoint;
            allWaypoints.add(new WaypointItem(
                WaypointType.PORTAL_OVERWORLD,
                "portal_overworld",
                Component.translatable("locator-tweaks.waypoint.nether_portal_overworld").getString(),
                "minecraft:overworld",
                owPortal.x, owPortal.y, owPortal.z,
                owPortal.enabled,
                owPortal.favorite,
                PORTAL_SPRITE,
                0xFFFFFFFF,
                false,
                null,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.portal") : null,
                () -> {
                    owPortal.enabled = !owPortal.enabled;
                    ModConfig.save();
                    NetherPortalManager.syncWaypoint(mc);
                },
                () -> {
                    owPortal.favorite = !owPortal.favorite;
                    ModConfig.save();
                },
                () -> {
                    cfg.removeOverworldPortal(worldKey);
                    ModConfig.save();
                    NetherPortalManager.syncWaypoint(mc);
                }
            ));
        }

        ModConfig.PortalPointData netherPortal = cfg.getNetherPortal(worldKey);
        if (netherPortal != null && !netherPortal.ignored) {
            boolean featDisabled = !cfg.showNetherPortalPoint;
            allWaypoints.add(new WaypointItem(
                WaypointType.PORTAL_NETHER,
                "portal_nether",
                Component.translatable("locator-tweaks.waypoint.nether_portal_nether").getString(),
                "minecraft:the_nether",
                netherPortal.x, netherPortal.y, netherPortal.z,
                netherPortal.enabled,
                netherPortal.favorite,
                PORTAL_SPRITE,
                0xFFFFFFFF,
                false,
                null,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.portal") : null,
                () -> {
                    netherPortal.enabled = !netherPortal.enabled;
                    ModConfig.save();
                    NetherPortalManager.syncWaypoint(mc);
                },
                () -> {
                    netherPortal.favorite = !netherPortal.favorite;
                    ModConfig.save();
                },
                () -> {
                    cfg.removeNetherPortal(worldKey);
                    ModConfig.save();
                    NetherPortalManager.syncWaypoint(mc);
                }
            ));
        }

        Map<String, ModConfig.LodestonePointData> lodestoneMap = cfg.getLodestones(worldKey);
        for (ModConfig.LodestonePointData lp : lodestoneMap.values()) {
            boolean featDisabled = !cfg.showLodestonePoints;
            String lodeName = (lp.name != null && !lp.name.isEmpty()) ? lp.name : Component.translatable("locator-tweaks.waypoint.lodestone").getString();
            allWaypoints.add(new WaypointItem(
                WaypointType.LODESTONE,
                "lodestone_" + lp.id,
                lodeName,
                lp.dimension,
                lp.x, lp.y, lp.z,
                lp.enabled,
                lp.favorite,
                LODESTONE_SPRITE,
                0xFFFFFFFF,
                false,
                null,
                featDisabled,
                featDisabled ? Component.translatable("locator-tweaks.gui.waypoints.disabled_in_settings.lodestone") : null,
                () -> {
                    lp.enabled = !lp.enabled;
                    cfg.setLodestoneEnabled(lp.id, lp.enabled);
                    ModConfig.save();
                    LodestoneManager.tick(mc);
                },
                () -> {
                    lp.favorite = !lp.favorite;
                    cfg.setLodestoneFavorite(worldKey, lp.id, lp.favorite);
                    ModConfig.save();
                },
                () -> {
                    cfg.removeLodestone(worldKey, lp.id);
                    cfg.ignoreLodestone(lp.id);
                    ModConfig.save();
                    LodestoneManager.tick(mc);
                }
            ));
        }

        allWaypoints.sort((a, b) -> {
            if (a.favorite != b.favorite) {
                return a.favorite ? -1 : 1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });
    }

    public void reloadWaypointsAndRefresh() {
        populateWaypoints();
        refreshFilteredList();
    }

    private void onSearchChanged(String query) {
        this.currentSearch = query.trim().toLowerCase(Locale.ROOT);
        refreshFilteredList();
    }

    public void refreshFilteredList() {
        if (this.listWidget == null) return;
        this.listWidget.clearEntries();

        for (WaypointItem wp : allWaypoints) {
            boolean matchesDim = "all".equalsIgnoreCase(selectedDimension)
                || (wp.dimension != null && wp.dimension.equalsIgnoreCase(selectedDimension));
            if (!matchesDim) {
                continue;
            }

            if (currentSearch.isEmpty() || wp.name.toLowerCase(Locale.ROOT).contains(currentSearch)) {
                this.listWidget.addWaypoint(new WaypointListEntry(this, wp));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        extractor.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 8, 0xFFFFFFFF);

        if (this.listWidget.children().isEmpty()) {
            Component emptyText = Component.translatable(allWaypoints.isEmpty() ? "locator-tweaks.gui.waypoints.empty" : "locator-tweaks.gui.waypoints.no_results");
            extractor.text(this.font, emptyText, this.width / 2 - this.font.width(emptyText) / 2, this.height / 2 - 4, 0xFF888888);
        }
    }

    @Override
    public void onClose() {
        ModConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        }
    }

    public enum WaypointType {
        CUSTOM,
        DEATH,
        SPAWN,
        PORTAL_OVERWORLD,
        PORTAL_NETHER,
        LODESTONE
    }

    public static class WaypointItem {
        public final WaypointType type;
        public final String id;
        public final String name;
        public final String dimension;
        public final int x;
        public final int y;
        public final int z;
        public final boolean enabled;
        public final boolean favorite;
        public final Identifier sprite;
        public final int spriteColor;
        public final boolean isCustom;
        public final CustomWaypoint customWaypoint;
        public final boolean isFeatureDisabled;
        public final Component disabledReason;
        public final Runnable onToggleVisibility;
        public final Runnable onToggleFavorite;
        public final Runnable onDelete;

        public WaypointItem(
            WaypointType type,
            String id,
            String name,
            String dimension,
            int x, int y, int z,
            boolean enabled,
            boolean favorite,
            Identifier sprite,
            int spriteColor,
            boolean isCustom,
            CustomWaypoint customWaypoint,
            boolean isFeatureDisabled,
            Component disabledReason,
            Runnable onToggleVisibility,
            Runnable onToggleFavorite,
            Runnable onDelete
        ) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.enabled = enabled;
            this.favorite = favorite;
            this.sprite = sprite;
            this.spriteColor = spriteColor;
            this.isCustom = isCustom;
            this.customWaypoint = customWaypoint;
            this.isFeatureDisabled = isFeatureDisabled;
            this.disabledReason = disabledReason;
            this.onToggleVisibility = onToggleVisibility;
            this.onToggleFavorite = onToggleFavorite;
            this.onDelete = onDelete;
        }
    }

    public static class WaypointListWidget extends ContainerObjectSelectionList<WaypointListEntry> {
        public WaypointListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void addWaypoint(WaypointListEntry entry) {
            this.addEntry(entry);
        }

        public void clearEntries() {
            super.clearEntries();
        }

        public WaypointListEntry getHoveredEntry() {
            return this.getHovered();
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, this.width - 24);
        }
    }

    public static class WaypointListEntry extends ContainerObjectSelectionList.Entry<WaypointListEntry> {
        private final WaypointListScreen screen;
        private final WaypointItem item;
        private final Button visibilityBtn;
        private final Button favoriteBtn;
        private final Button deleteBtn;
        private final List<GuiEventListener> children = new ArrayList<>();

        public WaypointListEntry(WaypointListScreen screen, WaypointItem item) {
            this.screen = screen;
            this.item = item;

            this.visibilityBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    if (item.onToggleVisibility != null) {
                        item.onToggleVisibility.run();
                        screen.reloadWaypointsAndRefresh();
                    }
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable(item.enabled ? "locator-tweaks.gui.visibility.hide" : "locator-tweaks.gui.visibility.show")))
            .build();
            this.visibilityBtn.active = !item.isFeatureDisabled && item.onToggleVisibility != null;

            this.favoriteBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    if (item.onToggleFavorite != null) {
                        item.onToggleFavorite.run();
                        screen.reloadWaypointsAndRefresh();
                    }
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable(item.favorite ? "locator-tweaks.gui.favorite.remove" : "locator-tweaks.gui.favorite.add")))
            .build();
            this.favoriteBtn.active = !item.isFeatureDisabled && item.onToggleFavorite != null;

            this.deleteBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    if (item.onDelete != null) {
                        item.onDelete.run();
                        screen.reloadWaypointsAndRefresh();
                    }
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable("locator-tweaks.gui.waypoints.delete")))
            .build();
            this.deleteBtn.active = !item.isFeatureDisabled && item.onDelete != null;

            this.children.add(this.visibilityBtn);
            this.children.add(this.favoriteBtn);
            this.children.add(this.deleteBtn);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Arrays.asList(this.visibilityBtn, this.favoriteBtn, this.deleteBtn);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.visibilityBtn.isMouseOver(event.x(), event.y())) {
                return this.visibilityBtn.mouseClicked(event, doubleClick);
            }
            if (this.favoriteBtn.isMouseOver(event.x(), event.y())) {
                return this.favoriteBtn.mouseClicked(event, doubleClick);
            }
            if (this.deleteBtn.isMouseOver(event.x(), event.y())) {
                return this.deleteBtn.mouseClicked(event, doubleClick);
            }
            boolean isLeftClick = event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == 0;
            if (isLeftClick && event.x() >= getContentX() && event.x() <= getContentRight() && event.y() >= getContentY() && event.y() <= getContentBottom()) {
                if (this.item.isCustom && !this.item.isFeatureDisabled && this.item.customWaypoint != null) {
                    Minecraft mc = Minecraft.getInstance();
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.screen.minecraft.setScreenAndShow(new WaypointEditScreen(this.screen, this.item.customWaypoint));
                    return true;
                }
            }
            return false;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int left = getContentX();
            int top = getContentY();
            int width = getContentWidth();
            int height = getContentHeight();
            int right = getContentRight();
            int bottom = getContentBottom();

            boolean disabled = item.isFeatureDisabled;

            if (disabled) {
                if (hovering) {
                    extractor.fill(left, top, right, bottom, 0x22FFFFFF);
                    extractor.fill(left, top, right, top + 1, 0x55FFFFFF);
                    extractor.fill(left, bottom - 1, right, bottom, 0x55FFFFFF);
                    extractor.fill(left, top, left + 1, bottom, 0x55FFFFFF);
                    extractor.fill(right - 1, top, right, bottom, 0x55FFFFFF);
                } else {
                    extractor.fill(left, top, right, bottom, 0x1A000000);
                    extractor.fill(left, top, right, top + 1, 0x11FFFFFF);
                    extractor.fill(left, bottom - 1, right, bottom, 0x11FFFFFF);
                    extractor.fill(left, top, left + 1, bottom, 0x11FFFFFF);
                    extractor.fill(right - 1, top, right, bottom, 0x11FFFFFF);
                }
            } else if (hovering) {
                extractor.fill(left, top, right, bottom, 0x44FFFFFF);
                extractor.fill(left, top, right, top + 1, 0xAAFFFFFF);
                extractor.fill(left, bottom - 1, right, bottom, 0xAAFFFFFF);
                extractor.fill(left, top, left + 1, bottom, 0xAAFFFFFF);
                extractor.fill(right - 1, top, right, bottom, 0xAAFFFFFF);
            } else {
                extractor.fill(left, top, right, bottom, 0x33000000);
                extractor.fill(left, top, right, top + 1, 0x22FFFFFF);
                extractor.fill(left, bottom - 1, right, bottom, 0x22FFFFFF);
                extractor.fill(left, top, left + 1, bottom, 0x22FFFFFF);
                extractor.fill(right - 1, top, right, bottom, 0x22FFFFFF);
            }

            int iconSize = 18;
            int iconX = left + 8;
            int iconY = top + (height - iconSize) / 2;
            int iconColor;
            if (disabled) {
                iconColor = 0x44888888;
            } else if (item.enabled) {
                iconColor = item.isCustom ? (0xFF000000 | (item.spriteColor & 0x00FFFFFF)) : 0xFFFFFFFF;
            } else {
                iconColor = 0x55888888;
            }
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, item.sprite, iconX, iconY, iconSize, iconSize, iconColor);

            int textX = left + 32;
            int textY = top + 5;
            int nameColor;
            if (disabled) {
                nameColor = 0xFF666666;
            } else if (item.enabled) {
                nameColor = 0xFFFFFFFF;
            } else {
                nameColor = 0xFF777777;
            }
            extractor.text(screen.font, item.name, textX, textY, nameColor);

            String coordStr;
            if ("all".equalsIgnoreCase(screen.selectedDimension)) {
                coordStr = String.format(Locale.ROOT, "%s  |  X: %d  Y: %d  Z: %d", getDimensionDisplayName(item.dimension).getString(), item.x, item.y, item.z);
            } else {
                coordStr = String.format(Locale.ROOT, "X: %d  Y: %d  Z: %d", item.x, item.y, item.z);
            }
            int coordColor;
            if (disabled) {
                coordColor = 0xFF445566;
            } else if (item.enabled) {
                coordColor = 0xFFA4C2D6;
            } else {
                coordColor = 0xFF556677;
            }
            extractor.text(screen.font, coordStr, textX, textY + 13, coordColor);

            int btnSize = 20;
            int btnY = top + (height - btnSize) / 2;
            int delBtnX = right - btnSize - 6;
            int favBtnX = delBtnX - btnSize - 4;
            int visBtnX = favBtnX - btnSize - 4;

            this.deleteBtn.setX(delBtnX);
            this.deleteBtn.setY(btnY);
            this.deleteBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, CROSS_SPRITE, delBtnX, btnY, btnSize, btnSize, (disabled || item.onDelete == null) ? 0x44FFFFFF : 0xFFFFFFFF);

            this.favoriteBtn.setX(favBtnX);
            this.favoriteBtn.setY(btnY);
            this.favoriteBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            Identifier starSprite = item.favorite ? STAR_ACTIVE_SPRITE : STAR_INACTIVE_SPRITE;
            int starSize = 8;
            int starX = favBtnX + (btnSize - starSize) / 2;
            int starY = btnY + (btnSize - starSize) / 2;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, starSprite, starX, starY, starSize, starSize, (disabled || item.onToggleFavorite == null) ? 0x44FFFFFF : 0xFFFFFFFF);

            this.visibilityBtn.setX(visBtnX);
            this.visibilityBtn.setY(btnY);
            this.visibilityBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            Identifier eyeSprite = item.enabled ? EYE_VISIBLE_SPRITE : EYE_HIDDEN_SPRITE;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, eyeSprite, visBtnX, btnY, btnSize, btnSize, (disabled || item.onToggleVisibility == null) ? 0x44FFFFFF : 0xFFFFFFFF);

            if (hovering) {
                if (disabled) {
                    if (item.disabledReason != null) {
                        extractor.setTooltipForNextFrame(item.disabledReason, mouseX, mouseY);
                    }
                } else if (mouseX < visBtnX) {
                    if (item.isCustom) {
                        extractor.setTooltipForNextFrame(Component.translatable("locator-tweaks.gui.waypoints.click_to_edit"), mouseX, mouseY);
                    } else {
                        extractor.setTooltipForNextFrame(Component.translatable("locator-tweaks.gui.waypoints.system_not_editable"), mouseX, mouseY);
                    }
                }
            }
        }
    }
}
