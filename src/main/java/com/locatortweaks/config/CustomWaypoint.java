package com.locatortweaks.config;

import java.util.UUID;

public class CustomWaypoint {
    public UUID id;
    public String name;
    public int x;
    public int y;
    public int z;
    public String dimension;
    public int color;
    public boolean enabled;
    public boolean favorite;
    public long createdAt;

    public CustomWaypoint() {
        this.id = UUID.randomUUID();
        this.name = "Waypoint";
        this.dimension = "minecraft:overworld";
        this.color = 0xFF5555;
        this.enabled = true;
        this.favorite = false;
        this.createdAt = System.currentTimeMillis();
    }

    public CustomWaypoint(UUID id, String name, int x, int y, int z, String dimension, int color, boolean enabled, boolean favorite) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Waypoint";
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension != null ? dimension : "minecraft:overworld";
        this.color = color;
        this.enabled = enabled;
        this.favorite = favorite;
        this.createdAt = System.currentTimeMillis();
    }

    public CustomWaypoint copy() {
        CustomWaypoint copy = new CustomWaypoint(this.id, this.name, this.x, this.y, this.z, this.dimension, this.color, this.enabled, this.favorite);
        copy.createdAt = this.createdAt;
        return copy;
    }
}
