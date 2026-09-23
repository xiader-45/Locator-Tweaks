package com.locatortweaks.hud;

import net.minecraft.network.chat.Component;

public enum MarkerType {
    DEATH,
    SPAWN,
    PORTAL,
    LODESTONE,
    HEAD,
    DOT,
    FALLBACK,
    CUSTOM_WAYPOINT;

    public String getDisplayName() {
        return switch (this) {
            case HEAD, DOT -> Component.translatable("locator-tweaks.marker_type.player").getString();
            case CUSTOM_WAYPOINT -> Component.translatable("locator-tweaks.marker_type.custom_waypoint").getString();
            case DEATH -> Component.translatable("locator-tweaks.marker_type.death").getString();
            case SPAWN -> Component.translatable("locator-tweaks.marker_type.spawn").getString();
            case PORTAL -> Component.translatable("locator-tweaks.marker_type.portal").getString();
            case LODESTONE -> Component.translatable("locator-tweaks.marker_type.lodestone").getString();
            case FALLBACK -> Component.translatable("locator-tweaks.marker_type.world").getString();
        };
    }
}
