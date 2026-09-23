package com.locatortweaks.gui;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.config.PlayerLocatorConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class PlayerDetailScreen extends Screen {
    private static final Identifier LOCATOR_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
    private static final Identifier EXPERIENCE_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier EXPERIENCE_BAR_PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");
    private static final Identifier ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
    private static final Identifier DEFAULT_DOT = Identifier.withDefaultNamespace("hud/locator_bar_dot/default_0");

    private final Screen parentScreen;
    private final UUID playerId;
    private final String playerName;
    private final Supplier<PlayerSkin> skinGetter;
    private final PlayerLocatorConfig config;

    private Button iconTypeBtn;
    private Button scaleBtn;
    private RgbSlider redSlider;
    private RgbSlider greenSlider;
    private RgbSlider blueSlider;

    public PlayerDetailScreen(Screen parentScreen, UUID playerId, String playerName, Supplier<PlayerSkin> skinGetter) {
        super(Component.translatable("locator-tweaks.gui.detail.title", playerName));
        this.parentScreen = parentScreen;
        this.playerId = playerId;
        this.playerName = playerName;
        this.skinGetter = skinGetter;
        this.config = ModConfig.getInstance().getOrCreatePlayerConfig(playerId, playerName);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 66;
        int btnWidth = 220;
        int btnHeight = 20;
        int spacing = 22;

        this.iconTypeBtn = Button.builder(getIconTypeText(), b -> {
            boolean currentlyHead = isHeadActive();
            config.iconType = currentlyHead ? PlayerLocatorConfig.IconType.SQUARE : PlayerLocatorConfig.IconType.HEAD;
            updateButtonLabels();
        }).bounds(centerX - btnWidth / 2, startY, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.iconTypeBtn);

        this.scaleBtn = Button.builder(getScaleText(), b -> {
            cycleScale();
            updateButtonLabels();
        }).bounds(centerX - btnWidth / 2, startY + spacing, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.scaleBtn);

        int vanillaColor = PlayerLocatorConfig.getVanillaMarkerColor(playerId);
        int currentColor = config.useCustomColor ? config.customColor : vanillaColor;
        int initR = (currentColor >> 16) & 0xFF;
        int initG = (currentColor >> 8) & 0xFF;
        int initB = currentColor & 0xFF;

        this.redSlider = new RgbSlider(centerX - btnWidth / 2, startY + spacing * 2, btnWidth, btnHeight, "r", 0xFFFF6666, initR, v -> onRgbChanged());
        this.addRenderableWidget(this.redSlider);

        this.greenSlider = new RgbSlider(centerX - btnWidth / 2, startY + spacing * 3, btnWidth, btnHeight, "g", 0xFF66FF66, initG, v -> onRgbChanged());
        this.addRenderableWidget(this.greenSlider);

        this.blueSlider = new RgbSlider(centerX - btnWidth / 2, startY + spacing * 4, btnWidth, btnHeight, "b", 0xFF66AAFF, initB, v -> onRgbChanged());
        this.addRenderableWidget(this.blueSlider);

        Button resetBtn = Button.builder(Component.translatable("locator-tweaks.gui.detail.reset"), b -> {
            config.reset(playerId);
            int vanilla = PlayerLocatorConfig.getVanillaMarkerColor(playerId);
            this.redSlider.setIntValue((vanilla >> 16) & 0xFF);
            this.greenSlider.setIntValue((vanilla >> 8) & 0xFF);
            this.blueSlider.setIntValue(vanilla & 0xFF);
            updateButtonLabels();
        }).bounds(centerX - btnWidth / 2, startY + spacing * 5 + 4, btnWidth / 2 - 2, btnHeight).build();
        this.addRenderableWidget(resetBtn);

        Button doneBtn = Button.builder(CommonComponents.GUI_DONE, b -> {
            ModConfig.save();
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(this.parentScreen);
            }
        }).bounds(centerX + 2, startY + spacing * 5 + 4, btnWidth / 2 - 2, btnHeight).build();
        this.addRenderableWidget(doneBtn);

        updateButtonLabels();
    }

    private void onRgbChanged() {
        int r = this.redSlider.getIntValue();
        int g = this.greenSlider.getIntValue();
        int b = this.blueSlider.getIntValue();
        config.customColor = 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        config.useCustomColor = true;
    }

    private boolean isHeadActive() {
        return (config.iconType == PlayerLocatorConfig.IconType.HEAD) ||
               (config.iconType == PlayerLocatorConfig.IconType.DEFAULT && ModConfig.getInstance().isPlayerHeadsEnabled());
    }

    private void updateButtonLabels() {
        this.iconTypeBtn.setMessage(getIconTypeText());
        this.scaleBtn.setMessage(getScaleText());

        boolean head = isHeadActive();
        boolean disableRgb = head && !ModConfig.getInstance().showHeadOutline;
        this.redSlider.active = !disableRgb;
        this.greenSlider.active = !disableRgb;
        this.blueSlider.active = !disableRgb;

        if (disableRgb) {
            Tooltip tooltip = Tooltip.create(Component.translatable("locator-tweaks.gui.detail.rgb_disabled_for_head"));
            this.redSlider.setTooltip(tooltip);
            this.greenSlider.setTooltip(tooltip);
            this.blueSlider.setTooltip(tooltip);
        } else {
            this.redSlider.setTooltip(null);
            this.greenSlider.setTooltip(null);
            this.blueSlider.setTooltip(null);
        }
    }

    private Component getIconTypeText() {
        boolean head = isHeadActive();
        return Component.translatable(
            "locator-tweaks.gui.detail.icon_type",
            Component.translatable(head ? "locator-tweaks.icon_type.head" : "locator-tweaks.icon_type.square")
        );
    }

    private Component getScaleText() {
        if (Math.abs(config.scale - PlayerLocatorConfig.SCALE_SMALL) < 0.05f) {
            return Component.translatable("locator-tweaks.gui.detail.scale", Component.translatable("locator-tweaks.scale.small"));
        } else if (Math.abs(config.scale - PlayerLocatorConfig.SCALE_LARGE) < 0.05f) {
            return Component.translatable("locator-tweaks.gui.detail.scale", Component.translatable("locator-tweaks.scale.large"));
        } else {
            return Component.translatable("locator-tweaks.gui.detail.scale", Component.translatable("locator-tweaks.scale.standard"));
        }
    }

    private void cycleScale() {
        float[] scales = PlayerLocatorConfig.PRESET_SCALES;
        int currentIndex = 1;
        for (int i = 0; i < scales.length; i++) {
            if (Math.abs(scales[i] - config.scale) < 0.05f) {
                currentIndex = i;
                break;
            }
        }
        config.scale = scales[(currentIndex + 1) % scales.length];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        extractor.text(this.font, this.title, centerX - this.font.width(this.title) / 2, 5, 0xFFFFFFFF);

        int previewWidth = 220;
        int previewHeight = 40;
        int previewX = centerX - previewWidth / 2;
        int previewY = 18;

        extractor.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0x88000000);
        extractor.fill(previewX, previewY, previewX + previewWidth, previewY + 1, 0x66FFFFFF);
        extractor.fill(previewX, previewY + previewHeight - 1, previewX + previewWidth, previewY + previewHeight, 0x66FFFFFF);
        extractor.fill(previewX, previewY, previewX + 1, previewY + previewHeight, 0x66FFFFFF);
        extractor.fill(previewX + previewWidth - 1, previewY, previewX + previewWidth, previewY + previewHeight, 0x66FFFFFF);

        Component previewLabel = Component.translatable("locator-tweaks.gui.detail.preview");
        extractor.text(this.font, previewLabel, previewX + 6, previewY + 3, 0xFFAAAAAA);

        int markerCenterX = previewX + previewWidth / 2;
        int barCenterY = previewY + 26;

        boolean showXp = ModConfig.getInstance().barDisplayMode == ModConfig.BarDisplayMode.XP_WITH_MARKERS;
        int barWidth = 140;
        int barX = markerCenterX - barWidth / 2;

        if (showXp) {
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND, barX, barCenterY, barWidth, 5);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS, 182, 5, 0, 0, barX, barCenterY, (int) (barWidth * 0.65f), 5);
        } else {
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_BACKGROUND, barX, barCenterY, barWidth, 5);
        }

        float alphaFactor = Math.clamp(ModConfig.getInstance().markerOpacity / 100.0f, 0.0f, 1.0f);
        int previewAlpha = Math.round(alphaFactor * 255.0f);
        if (previewAlpha <= 0) {
            Component hiddenMsg = Component.translatable("locator-tweaks.gui.detail.preview_hidden");
            extractor.text(this.font, hiddenMsg, markerCenterX - this.font.width(hiddenMsg) / 2, barCenterY - 4, 0x88FFFFFF);
            return;
        }

        boolean useHead = isHeadActive();
        int vanillaColor = PlayerLocatorConfig.getVanillaMarkerColor(playerId);

        if (useHead) {
            int headSize = Math.max(6, Math.round(8 * config.scale));
            int headX = markerCenterX - headSize / 2;
            int headY = barCenterY - 2 - Math.floorDiv(headSize - 8, 2);

            if (ModConfig.getInstance().showHeadOutline) {
                int outlineColor = config.useCustomColor ? config.customColor : vanillaColor;
                outlineColor = (previewAlpha << 24) | (outlineColor & 0x00FFFFFF);
                extractor.fill(headX - 1, headY - 1, headX + headSize + 1, headY, outlineColor);
                extractor.fill(headX - 1, headY + headSize, headX + headSize + 1, headY + headSize + 1, outlineColor);
                extractor.fill(headX - 1, headY, headX, headY + headSize, outlineColor);
                extractor.fill(headX + headSize, headY, headX + headSize + 1, headY + headSize, outlineColor);
            }

            PlayerSkin skin = this.skinGetter != null ? this.skinGetter.get() : null;
            if (skin != null) {
                PlayerFaceExtractor.extractRenderState(extractor, skin, headX, headY, headSize, (previewAlpha << 24) | 0x00FFFFFF);
            } else {
                extractor.fill(headX, headY, headX + headSize, headY + headSize, (previewAlpha << 24) | (vanillaColor & 0x00FFFFFF));
            }

            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_UP, markerCenterX - 3, headY - 6, 7, 5, (previewAlpha << 24) | 0x00FFFFFF);
        } else {
            int color = config.useCustomColor ? config.customColor : vanillaColor;
            int dotSize = Math.max(6, Math.round(9 * config.scale));
            int dotX = markerCenterX - dotSize / 2;
            int dotY = barCenterY - 2 - Math.floorDiv(dotSize - 9, 2);

            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, DEFAULT_DOT, dotX, dotY, dotSize, dotSize, (previewAlpha << 24) | (color & 0x00FFFFFF));
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_UP, markerCenterX - 3, dotY - 6, 7, 5, (previewAlpha << 24) | 0x00FFFFFF);
        }
    }

    @Override
    public void onClose() {
        ModConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        }
    }

    public static class RgbSlider extends AbstractSliderButton {
        private final String channelName;
        private final int textColor;
        private final IntConsumer onValueChange;

        public RgbSlider(int x, int y, int width, int height, String channelName, int textColor, int initialValue, IntConsumer onValueChange) {
            super(x, y, width, height, CommonComponents.EMPTY, Mth.clamp(initialValue / 255.0, 0.0, 1.0));
            this.channelName = channelName;
            this.textColor = textColor;
            this.onValueChange = onValueChange;
            this.updateMessage();
        }

        public int getIntValue() {
            return (int) Math.round(this.value * 255.0);
        }

        public void setIntValue(int intVal) {
            this.setValue(intVal / 255.0);
        }

        @Override
        protected void updateMessage() {
            Component label = Component.translatable("locator-tweaks.gui.detail.rgb_" + channelName, getIntValue());
            this.setMessage(label.copy().withColor(textColor));
        }

        @Override
        protected void applyValue() {
            this.onValueChange.accept(getIntValue());
        }
    }
}
