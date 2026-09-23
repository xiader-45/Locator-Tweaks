package com.locatortweaks.gui;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.config.PlayerLocatorConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.*;
import java.util.function.Supplier;

public class PlayerListScreen extends Screen {
    private static final Identifier DEFAULT_DOT = Identifier.withDefaultNamespace("hud/locator_bar_dot/default_0");
    private static final Identifier EYE_VISIBLE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/eye_visible");
    private static final Identifier EYE_HIDDEN_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/eye_hidden");
    private static final Identifier STAR_ACTIVE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/star_active");
    private static final Identifier STAR_INACTIVE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/star_inactive");

    private final Screen parentScreen;
    private EditBox searchBox;
    public PlayerListWidget listWidget;
    private final List<PlayerEntryData> allPlayers = new ArrayList<>();
    private String currentSearch = "";
    public int lastMouseX;
    public int lastMouseY;

    public record PlayerEntryData(UUID uuid, String name, Supplier<PlayerSkin> skinGetter) {}

    public PlayerListScreen(Screen parentScreen) {
        super(Component.translatable("locator-tweaks.gui.players.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        allPlayers.clear();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        UUID localPlayerUuid = mc.player != null ? mc.player.getUUID() : null;

        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                if (info.getProfile() != null && info.getProfile().name() != null) {
                    UUID id = info.getProfile().id();
                    if (localPlayerUuid != null && localPlayerUuid.equals(id)) {
                        continue;
                    }
                    allPlayers.add(new PlayerEntryData(
                        id,
                        info.getProfile().name(),
                        info::getSkin
                    ));
                }
            }
        }

        allPlayers.sort(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)));

        int searchWidth = 240;
        this.searchBox = new EditBox(this.font, this.width / 2 - searchWidth / 2, 22, searchWidth, 20, Component.translatable("locator-tweaks.gui.search"));
        this.searchBox.setHint(Component.translatable("locator-tweaks.gui.search_hint"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        int listTop = 48;
        int listBottom = this.height - 36;
        int listHeight = listBottom - listTop;
        this.listWidget = new PlayerListWidget(this.minecraft, this.width, listHeight, listTop, 36);
        this.addRenderableWidget(this.listWidget);

        Button doneBtn = Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
            .build();
        this.addRenderableWidget(doneBtn);

        refreshFilteredList();
    }

    private void onSearchChanged(String query) {
        this.currentSearch = query.trim().toLowerCase(Locale.ROOT);
        refreshFilteredList();
    }

    public void refreshFilteredList() {
        if (this.listWidget == null) return;
        this.listWidget.clearEntries();

        for (PlayerEntryData player : allPlayers) {
            if (currentSearch.isEmpty() || player.name().toLowerCase(Locale.ROOT).contains(currentSearch)) {
                this.listWidget.addPlayer(new PlayerListEntry(this, player));
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
            Component emptyText = Component.translatable(allPlayers.isEmpty() ? "locator-tweaks.gui.players.empty" : "locator-tweaks.gui.players.no_results");
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

    public static class PlayerListWidget extends ContainerObjectSelectionList<PlayerListEntry> {
        public PlayerListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public PlayerListEntry getHoveredEntry() {
            return this.getHovered();
        }

        public void addPlayer(PlayerListEntry entry) {
            this.addEntry(entry);
        }

        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, this.width - 24);
        }
    }

    public static class PlayerListEntry extends ContainerObjectSelectionList.Entry<PlayerListEntry> {
        private final PlayerListScreen screen;
        private final PlayerEntryData playerData;
        private final Button visibilityBtn;
        private final Button favoriteBtn;
        private final Button resetBtn;
        private final List<GuiEventListener> children = new ArrayList<>();

        public PlayerListEntry(PlayerListScreen screen, PlayerEntryData playerData) {
            this.screen = screen;
            this.playerData = playerData;

            PlayerLocatorConfig initialConfig = ModConfig.getInstance().getPlayerConfig(playerData.uuid());
            boolean isFav = initialConfig != null && initialConfig.favorite;
            boolean isVis = initialConfig == null || initialConfig.enabled;

            this.visibilityBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    PlayerLocatorConfig cfg = ModConfig.getInstance().getOrCreatePlayerConfig(playerData.uuid(), playerData.name());
                    cfg.enabled = !cfg.enabled;
                    ModConfig.save();
                    b.setTooltip(Tooltip.create(Component.translatable(cfg.enabled ? "locator-tweaks.gui.visibility.hide" : "locator-tweaks.gui.visibility.show")));
                    screen.refreshFilteredList();
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable(isVis ? "locator-tweaks.gui.visibility.hide" : "locator-tweaks.gui.visibility.show")))
            .build();

            this.favoriteBtn = Button.builder(
                CommonComponents.EMPTY,
                b -> {
                    PlayerLocatorConfig cfg = ModConfig.getInstance().getOrCreatePlayerConfig(playerData.uuid(), playerData.name());
                    cfg.favorite = !cfg.favorite;
                    ModConfig.save();
                    b.setTooltip(Tooltip.create(Component.translatable(cfg.favorite ? "locator-tweaks.gui.favorite.remove" : "locator-tweaks.gui.favorite.add")));
                    screen.refreshFilteredList();
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable(isFav ? "locator-tweaks.gui.favorite.remove" : "locator-tweaks.gui.favorite.add")))
            .build();

            this.resetBtn = Button.builder(
                Component.literal("↺"),
                b -> {
                    ModConfig.getInstance().resetPlayerConfig(playerData.uuid());
                    screen.refreshFilteredList();
                }
            )
            .bounds(0, 0, 20, 20)
            .tooltip(Tooltip.create(Component.translatable("locator-tweaks.config.player.reset")))
            .build();

            this.children.add(this.visibilityBtn);
            this.children.add(this.favoriteBtn);
            this.children.add(this.resetBtn);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Arrays.asList(this.visibilityBtn, this.favoriteBtn, this.resetBtn);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            if (this.visibilityBtn.isMouseOver(event.x(), event.y())) {
                return this.visibilityBtn.mouseClicked(event, isDoubleClick);
            }
            if (this.favoriteBtn.isMouseOver(event.x(), event.y())) {
                return this.favoriteBtn.mouseClicked(event, isDoubleClick);
            }
            if (this.resetBtn.isMouseOver(event.x(), event.y())) {
                return this.resetBtn.mouseClicked(event, isDoubleClick);
            }

            boolean isLeftClick = event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == 0;
            if (isLeftClick && event.x() >= getContentX() && event.x() <= getContentRight()
                    && event.y() >= getContentY() && event.y() <= getContentBottom()) {
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                mc.setScreenAndShow(new PlayerDetailScreen(screen, playerData.uuid(), playerData.name(), playerData.skinGetter()));
                return true;
            }

            return super.mouseClicked(event, isDoubleClick);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int left = getContentX();
            int top = getContentY();
            int right = getContentRight();
            int height = getContentHeight();

            if (hovering) {
                extractor.fill(left, top, right, top + height, 0x44FFFFFF);
                extractor.fill(left, top, right, top + 1, 0xAAFFFFFF);
                extractor.fill(left, top + height - 1, right, top + height, 0xAAFFFFFF);
                extractor.fill(left, top, left + 1, top + height, 0xAAFFFFFF);
                extractor.fill(right - 1, top, right, top + height, 0xAAFFFFFF);
            } else {
                extractor.fill(left, top, right, top + height, 0x33000000);
                extractor.fill(left, top, right, top + 1, 0x22FFFFFF);
                extractor.fill(left, top + height - 1, right, top + height, 0x22FFFFFF);
                extractor.fill(left, top, left + 1, top + height, 0x22FFFFFF);
                extractor.fill(right - 1, top, right, top + height, 0x22FFFFFF);
            }

            PlayerLocatorConfig config = ModConfig.getInstance().getPlayerConfig(playerData.uuid());
            boolean enabled = config == null || config.enabled;
            boolean isFavorite = config != null && config.favorite;
            boolean useHead = (config != null && config.iconType != PlayerLocatorConfig.IconType.DEFAULT)
                ? (config.iconType == PlayerLocatorConfig.IconType.HEAD)
                : ModConfig.getInstance().isPlayerHeadsEnabled();

            int vanillaColor = PlayerLocatorConfig.getVanillaMarkerColor(playerData.uuid());
            int color = (config != null && config.useCustomColor && !useHead) ? config.customColor : vanillaColor;
            float scale = config != null ? config.scale : 1.0f;

            int iconBoxSize = 24;
            int iconX = left + 6;
            int iconY = top + (height - iconBoxSize) / 2;

            extractor.fill(iconX, iconY, iconX + iconBoxSize, iconY + iconBoxSize, 0x44000000);

            if (!enabled) {
                PlayerSkin skin = playerData.skinGetter() != null ? playerData.skinGetter().get() : null;
                if (skin != null) {
                    PlayerFaceExtractor.extractRenderState(extractor, skin, iconX + 4, iconY + 4, 16, 0x66FFFFFF);
                } else {
                    extractor.fill(iconX + 4, iconY + 4, iconX + 20, iconY + 20, 0x66888888);
                }
                extractor.fill(iconX + 2, iconY + iconBoxSize / 2 - 1, iconX + iconBoxSize - 2, iconY + iconBoxSize / 2 + 1, 0xFFFF4444);
            } else if (useHead) {
                PlayerSkin skin = playerData.skinGetter() != null ? playerData.skinGetter().get() : null;
                int renderSize = Math.max(8, Math.min(22, Math.round(16 * scale)));
                int drawX = iconX + (iconBoxSize - renderSize) / 2;
                int drawY = iconY + (iconBoxSize - renderSize) / 2;
                if (ModConfig.getInstance().showHeadOutline) {
                    int outlineColor = 0xFF000000 | (color & 0x00FFFFFF);
                    extractor.fill(drawX - 1, drawY - 1, drawX + renderSize + 1, drawY, outlineColor);
                    extractor.fill(drawX - 1, drawY + renderSize, drawX + renderSize + 1, drawY + renderSize + 1, outlineColor);
                    extractor.fill(drawX - 1, drawY, drawX, drawY + renderSize, outlineColor);
                    extractor.fill(drawX + renderSize, drawY, drawX + renderSize + 1, drawY + renderSize, outlineColor);
                }
                if (skin != null) {
                    PlayerFaceExtractor.extractRenderState(extractor, skin, drawX, drawY, renderSize, -1);
                } else {
                    extractor.fill(drawX, drawY, drawX + renderSize, drawY + renderSize, vanillaColor);
                }
            } else {
                int renderSize = Math.max(8, Math.min(22, Math.round(16 * scale)));
                int drawX = iconX + (iconBoxSize - renderSize) / 2;
                int drawY = iconY + (iconBoxSize - renderSize) / 2;
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, DEFAULT_DOT, drawX, drawY, renderSize, renderSize, color);
            }

            int textX = iconX + iconBoxSize + 8;
            int nameY = top + 5;

            Component displayName = Component.literal(playerData.name()).withColor(isFavorite ? 0xFFFFD700 : 0xFFFFFFFF);
            extractor.text(screen.font, displayName, textX, nameY, 0xFFFFFFFF);

            Component statusText;
            int statusColor;
            if (!enabled) {
                statusText = Component.translatable("locator-tweaks.status.hidden_on_locator");
                statusColor = 0xFFFF7777;
            } else if (config != null && !config.isDefault(playerData.uuid())) {
                statusText = Component.translatable("locator-tweaks.status.custom_configured");
                statusColor = 0xFFFFAA44;
            } else {
                statusText = Component.translatable("locator-tweaks.status.default");
                statusColor = 0xFF888888;
            }
            extractor.text(screen.font, statusText, textX, nameY + 12, statusColor);

            boolean isCustom = config != null && !config.isDefault(playerData.uuid());
            this.resetBtn.active = isCustom;

            int btnSize = 20;
            int resetBtnX = right - btnSize - 6;
            int favBtnX = resetBtnX - btnSize - 4;
            int visBtnX = favBtnX - btnSize - 4;
            int btnY = top + (height - btnSize) / 2;

            this.visibilityBtn.setX(visBtnX);
            this.visibilityBtn.setY(btnY);
            this.visibilityBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);

            Identifier eyeSprite = enabled ? EYE_VISIBLE_SPRITE : EYE_HIDDEN_SPRITE;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, eyeSprite, visBtnX, btnY, btnSize, btnSize);

            this.favoriteBtn.setX(favBtnX);
            this.favoriteBtn.setY(btnY);
            this.favoriteBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
            Identifier starSprite = isFavorite ? STAR_ACTIVE_SPRITE : STAR_INACTIVE_SPRITE;
            int starSize = 8; int starX = favBtnX + (btnSize - starSize) / 2; int starY = btnY + (btnSize - starSize) / 2; extractor.blitSprite(RenderPipelines.GUI_TEXTURED, starSprite, starX, starY, starSize, starSize);

            this.resetBtn.setX(resetBtnX);
            this.resetBtn.setY(btnY);
            this.resetBtn.extractRenderState(extractor, screen.lastMouseX, screen.lastMouseY, partialTick);
        }
    }
}
