package com.locatortweaks.config;

import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.UUID;

public class PlayerLocatorConfig {
    public static final float SCALE_SMALL = 0.75f;
    public static final float SCALE_STANDARD = 1.0f;
    public static final float SCALE_LARGE = 1.25f;
    public static final float[] PRESET_SCALES = { SCALE_SMALL, SCALE_STANDARD, SCALE_LARGE };

    public boolean enabled = true;
    public boolean favorite = false;
    public boolean useCustomColor = false;
    public int customColor = 0xFFFFFFFF;
    public float scale = SCALE_STANDARD;
    public IconType iconType = IconType.DEFAULT;
    public String lastKnownName = "";

    public enum IconType {
        DEFAULT("locator-tweaks.icon_type.default"),
        SQUARE("locator-tweaks.icon_type.square"),
        HEAD("locator-tweaks.icon_type.head");

        private final String translationKey;

        IconType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public IconType next() {
            IconType[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public static int getVanillaMarkerColor(UUID uuid) {
        if (uuid == null) return 0xFFFFFFFF;
        return ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9f);
    }

    public static Component getMarkerPrefix(UUID uuid) {
        if (!ModConfig.getInstance().showMarkerOnNametag || uuid == null) {
            return null;
        }

        if (!ModConfig.getInstance().isPlayerMarkersEnabled()) {
            return null;
        }

        PlayerLocatorConfig config = ModConfig.getInstance().getPlayerConfig(uuid);
        if (config != null && !config.enabled) {
            return null;
        }

        boolean useHead = (config != null && config.iconType != IconType.DEFAULT)
            ? (config.iconType == IconType.HEAD)
            : ModConfig.getInstance().isPlayerHeadsEnabled();

        if (useHead) {
            return null;
        }

        int vanillaColor = getVanillaMarkerColor(uuid);
        int color = (config != null && config.useCustomColor) ? config.customColor : vanillaColor;
        float scale = config != null ? config.scale : 1.0f;

        String symbol;
        if (scale <= 0.85f) {
            symbol = "▪ ";
        } else if (scale >= 1.15f) {
            symbol = "█ ";
        } else {
            symbol = "■ ";
        }

        int rgb = color & 0x00FFFFFF;
        return Component.literal(symbol).withColor(rgb);
    }

    public PlayerLocatorConfig() {
    }

    public PlayerLocatorConfig(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public void reset() {
        this.enabled = true;
        this.useCustomColor = false;
        this.customColor = 0xFFFFFFFF;
        this.scale = SCALE_STANDARD;
        this.iconType = IconType.DEFAULT;
    }

    public void reset(UUID uuid) {
        this.enabled = true;
        this.useCustomColor = false;
        this.customColor = getVanillaMarkerColor(uuid);
        this.scale = SCALE_STANDARD;
        this.iconType = IconType.DEFAULT;
    }

    public boolean isDefault(UUID uuid) {
        boolean isDefaultIcon = (iconType == IconType.DEFAULT) ||
            (!ModConfig.getInstance().isPlayerHeadsEnabled() && iconType == IconType.SQUARE) ||
            (ModConfig.getInstance().isPlayerHeadsEnabled() && iconType == IconType.HEAD);
        boolean isDefaultColor = !useCustomColor || (uuid != null && customColor == getVanillaMarkerColor(uuid));
        return enabled &&
               isDefaultColor &&
               Math.abs(scale - SCALE_STANDARD) < 0.01f &&
               isDefaultIcon;
    }

    public boolean isDefault() {
        return isDefault(null);
    }
}
