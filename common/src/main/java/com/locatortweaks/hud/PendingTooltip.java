package com.locatortweaks.hud;

import net.minecraft.client.Minecraft;

public class PendingTooltip {
    public final String markerKey;
    public float targetX;
    public float x;
    public final int baseY;
    public final String name;
    public final String typeName;
    public final PendingHearts hearts;
    public final String distance;
    public final int width;
    public final int height;
    public final int themeColor;
    public final float markerFade;
    public final boolean isFavorite;

    public PendingTooltip(
        String markerKey,
        float targetX,
        int baseY,
        String name,
        String typeName,
        PendingHearts hearts,
        String distance,
        int themeColor,
        float markerFade,
        boolean isFavorite,
        Minecraft mc
    ) {
        this.markerKey = markerKey;
        this.targetX = targetX;
        this.baseY = baseY;
        this.name = name;
        this.typeName = typeName;
        this.hearts = hearts;
        this.distance = distance;
        this.themeColor = themeColor;
        this.markerFade = markerFade;
        this.isFavorite = isFavorite;

        int maxW = 0;
        int h = 6;
        if (name != null && !name.isEmpty()) {
            maxW = Math.max(maxW, mc.font.width(name) + (isFavorite ? 10 : 0));
            h += 11;
        }

        if (typeName != null && !typeName.isEmpty() && !typeName.equalsIgnoreCase(name)) {
            boolean showStarOnType = isFavorite && (name == null || name.isEmpty());
            maxW = Math.max(maxW, mc.font.width(typeName) + (showStarOnType ? 10 : 0));
            h += 9;
        }

        if (hearts != null) {
            int heartsW = (hearts.totalHearts - 1) * 8 + 9;
            maxW = Math.max(maxW, heartsW);
            h += 11;
        }

        if (distance != null && !distance.isEmpty()) {
            maxW = Math.max(maxW, mc.font.width(distance));
            h += 9;
        }

        this.width = maxW + 8;
        this.height = h;
        this.x = targetX - this.width / 2.0F;
    }
}

