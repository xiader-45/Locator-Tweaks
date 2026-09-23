package com.locatortweaks.compat.modmenu;

import net.minecraft.client.gui.screens.Screen;

public class YaclBlocker {
    public static Screen getScreen(Screen parent) {
        return com.locatortweaks.config.ModConfigScreen.createScreen(parent);
    }
}
