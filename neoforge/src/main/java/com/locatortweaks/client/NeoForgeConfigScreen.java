package com.locatortweaks.client;

import com.locatortweaks.config.ModConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class NeoForgeConfigScreen {
    public static Screen createScreen(Screen parent) {
        return ModConfigScreen.createScreen(parent);
    }
}
