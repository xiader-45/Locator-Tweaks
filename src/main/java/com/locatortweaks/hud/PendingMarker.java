package com.locatortweaks.hud;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.waypoints.TrackedWaypoint;

public class PendingMarker {
    public final String markerKey;
    public final TrackedWaypoint waypoint;
    public final float originalX;
    public float x;
    public final int y;
    public final int width;
    public final int height;
    public final int color;
    public final Identifier sprite;
    public final MarkerType type;
    public final int tier;
    public final PlayerSkin playerSkin;
    public final int headOutlineColor;
    public final String labelName;
    public final PendingHearts pendingHearts;
    public final String distText;
    public boolean collided = false;
    public boolean hasArrow = false;
    public Identifier arrowSprite;
    public int arrowTop;
    public int arrowWidth;
    public int arrowHeight;
    public float fadeProgress = 1.0F;

    public int getVisualLeft() {
        switch (this.type) {
            case DEATH:
                return this.tier == 0 ? 0 : this.tier;
            case SPAWN:
                return this.tier == 0 ? 1 : 2;
            case PORTAL:
                return this.tier <= 1 ? 1 : 2;
            case LODESTONE:
                return this.tier <= 1 ? 0 : this.tier - 1;
            case CUSTOM_WAYPOINT:
                return this.tier <= 1 ? 1 : 2;
            case HEAD:
                return 0;
            case DOT:
            default:
                return 1;
        }
    }

    public int getVisualRight() {
        switch (this.type) {
            case DEATH:
                return this.tier == 0 ? 8 : 8 - this.tier;
            case SPAWN:
                return this.tier == 0 ? 7 : 6;
            case PORTAL:
                return this.tier <= 1 ? 7 : 6;
            case LODESTONE:
                return this.tier <= 1 ? 8 : 9 - this.tier;
            case CUSTOM_WAYPOINT:
                return this.tier <= 1 ? 7 : 6;
            case HEAD:
                return this.width - 1;
            case DOT:
            default:
                return this.width - 2;
        }
    }

    public PendingMarker(
        String markerKey,
        TrackedWaypoint waypoint,
        float x,
        int y,
        int width,
        int height,
        int color,
        Identifier sprite,
        MarkerType type,
        int tier,
        PlayerSkin playerSkin,
        int headOutlineColor,
        String labelName,
        PendingHearts pendingHearts,
        String distText
    ) {
        this.markerKey = markerKey;
        this.waypoint = waypoint;
        this.originalX = x;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.sprite = sprite;
        this.type = type;
        this.tier = tier;
        this.playerSkin = playerSkin;
        this.headOutlineColor = headOutlineColor;
        this.labelName = labelName;
        this.pendingHearts = pendingHearts;
        this.distText = distText;
    }
}
