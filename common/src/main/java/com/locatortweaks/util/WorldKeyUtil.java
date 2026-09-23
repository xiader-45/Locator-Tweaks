package com.locatortweaks.util;

import net.minecraft.client.Minecraft;

public final class WorldKeyUtil {
    private WorldKeyUtil() {}

    public static String getCurrentWorldKey(Minecraft client) {
        if (client == null) return "default";
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            return "server_" + client.getCurrentServer().ip.toLowerCase();
        }
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            return "sp_" + client.getSingleplayerServer().getWorldData().getLevelName();
        }
        return "default";
    }
}
