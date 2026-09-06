package com.locatortweaks.gui;

import com.locatortweaks.config.CustomWaypoint;
import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.util.WorldKeyUtil;
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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

public class WaypointListScreen extends Screen {
    private static final Identifier PIN_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_0");

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
    private final List<CustomWaypoint> allWaypoints = new ArrayList<>();
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
        allWaypoints.clear();
        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.player != null && mc.level != null;
        String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);

        List<CustomWaypoint> list = ModConfig.getInstance().getCustomWaypoints(worldKey);
        allWaypoints.addAll(list);
        allWaypoints.sort((a, b) -> {
            if (a.favorite != b.favorite) {
                return a.favorite ? -1 : 1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });

        String currentWorldDim = inGame ? mc.level.dimension().identifier().toString() : "minecraft:overworld";
        if (this.selectedDimension == null) {
            this.selectedDimension = currentWorldDim;
        }

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

        // Add waypoint button (+) - perfectly square 20x20 matching entry buttons
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
        for (CustomWaypoint wp : allWaypoints) {
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

    public void reloadWaypointsAndRefresh() {
        allWaypoints.clear();
        Minecraft mc = Minecraft.getInstance();
        String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);
        List<CustomWaypoint> list = ModConfig.getInstance().getCustomWaypoints(worldKey);
        allWaypoints.addAll(list);
        allWaypoints.sort((a, b) -> {
            if (a.favorite != b.favorite) {
                return a.favorite ? -1 : 1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });
        refreshFilteredList();
    }

    private void onSearchChanged(String query) {
        this.currentSearch = query.trim().toLowerCase(Locale.ROOT);
        refreshFilteredList();
    }

    public void refreshFilteredList() {
        if (this.listWidget == null) return;
        this.listWidget.clearEntries();

        for (CustomWaypoint wp : allWaypoints) {
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
        private final CustomWaypoint waypoint;
        private final Button visibilityBtn;
        private final Button favoriteBtn;
        private final Button deleteBtn;
        private final List<GuiEventListener> children = new ArrayList<>();

        public WaypointListEntry(WaypointListScreen screen, CustomWaypoint waypoint) {
            this.screen = screen;
            this.waypoint = waypoint;

            this.visibilityBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    waypoint.enabled = !waypoint.enabled;
                    Minecraft mc = Minecraft.getInstance();
                    String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);
                    ModConfig.getInstance().updateCustomWaypoint(worldKey, waypoint);
                    CustomWaypointManager.forceSync(mc);
                    b.setTooltip(Tooltip.create(Component.translatable(waypoint.enabled ? "locator-tweaks.gui.visibility.hide" : "locator-tweaks.gui.visibility.show")));
                    screen.reloadWaypointsAndRefresh();
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable(waypoint.enabled ? "locator-tweaks.gui.visibility.hide" : "locator-tweaks.gui.visibility.show")))
            .build();

            this.favoriteBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    waypoint.favorite = !waypoint.favorite;
                    Minecraft mc = Minecraft.getInstance();
                    String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);
                    ModConfig.getInstance().updateCustomWaypoint(worldKey, waypoint);
                    CustomWaypointManager.forceSync(mc);
                    b.setTooltip(Tooltip.create(Component.translatable(waypoint.favorite ? "locator-tweaks.gui.favorite.remove" : "locator-tweaks.gui.favorite.add")));
                    screen.reloadWaypointsAndRefresh();
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable(waypoint.favorite ? "locator-tweaks.gui.favorite.remove" : "locator-tweaks.gui.favorite.add")))
            .build();

            this.deleteBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    Minecraft mc = Minecraft.getInstance();
                    String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);
                    ModConfig.getInstance().removeCustomWaypoint(worldKey, waypoint.id);
                    CustomWaypointManager.untrackWaypoint(mc, waypoint.id);
                    CustomWaypointManager.forceSync(mc);
                    screen.reloadWaypointsAndRefresh();
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable("locator-tweaks.gui.waypoints.delete")))
            .build();

            this.children.add(this.visibilityBtn);
            this.children.add(this.favoriteBtn);
            this.children.add(this.deleteBtn);
        }

        private static Component getFavoriteButtonText(boolean favorite) {
            return favorite
                ? Component.literal("?").withColor(0xFFFF55)
                : Component.literal("?").withColor(0xFFFFFF);
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
            if (this.visibilityBtn.mouseClicked(event, doubleClick)) return true;
            if (this.favoriteBtn.mouseClicked(event, doubleClick)) return true;
            if (this.deleteBtn.mouseClicked(event, doubleClick)) return true;
            if (event.button() == 0) {
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.screen.minecraft.setScreenAndShow(new WaypointEditScreen(this.screen, this.waypoint));
                return true;
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

            // Hover / Normal background with full 4-sided border (matching PlayerListScreen, no clipping)
            if (hovering) {
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

            // Left pin icon (color of the waypoint)
            int pinSize = 18;
            int pinX = left + 8;
            int pinY = top + (height - pinSize) / 2;
            int pinColor = waypoint.enabled ? (0xFF000000 | (waypoint.color & 0x00FFFFFF)) : 0x55888888;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, PIN_SPRITE, pinX, pinY, pinSize, pinSize, pinColor);

            // Waypoint title
            int textX = left + 32;
            int textY = top + 5;
            int nameColor = waypoint.enabled ? 0xFFFFFFFF : 0xFF777777;
            extractor.text(screen.font, waypoint.name, textX, textY, nameColor);

            // Subtitle: Coordinates (and dimension if 'all' is selected)
            String coordStr;
            if ("all".equalsIgnoreCase(screen.selectedDimension)) {
                coordStr = String.format(Locale.ROOT, "%s  |  X: %d  Y: %d  Z: %d", getDimensionDisplayName(waypoint.dimension).getString(), waypoint.x, waypoint.y, waypoint.z);
            } else {
                coordStr = String.format(Locale.ROOT, "X: %d  Y: %d  Z: %d", waypoint.x, waypoint.y, waypoint.z);
            }
            extractor.text(screen.font, coordStr, textX, textY + 13, waypoint.enabled ? 0xFFA4C2D6 : 0xFF556677);

            // Action buttons on the right (Delete, Favorite, Visibility)
            int btnSize = 20;
            int btnY = top + (height - btnSize) / 2;
            int delBtnX = right - btnSize - 6;
            int favBtnX = delBtnX - btnSize - 4;
            int visBtnX = favBtnX - btnSize - 4;

            // 1. Delete Button (Crisp White 20x20 Cross 1:1 pixel grid)
            this.deleteBtn.setX(delBtnX);
            this.deleteBtn.setY(btnY);
            this.deleteBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, CROSS_SPRITE, delBtnX, btnY, btnSize, btnSize);

            // 2. Favorite Button (Rendered with custom pixel-art star sprite: star_active / star_inactive)
            this.favoriteBtn.setX(favBtnX);
            this.favoriteBtn.setY(btnY);
            this.favoriteBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            Identifier starSprite = waypoint.favorite ? STAR_ACTIVE_SPRITE : STAR_INACTIVE_SPRITE;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, starSprite, favBtnX, btnY, btnSize, btnSize);

            // 3. Visibility Button (Sleek eye icon)
            this.visibilityBtn.setX(visBtnX);
            this.visibilityBtn.setY(btnY);
            this.visibilityBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            Identifier eyeSprite = waypoint.enabled ? EYE_VISIBLE_SPRITE : EYE_HIDDEN_SPRITE;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, eyeSprite, visBtnX, btnY, btnSize, btnSize);

            // Tooltip on hovering over the entry (excluding action buttons)
            if (hovering && mouseX < visBtnX) {
                extractor.setTooltipForNextFrame(Component.translatable("locator-tweaks.gui.waypoints.click_to_edit"), mouseX, mouseY);
            }
        }
    }
}
