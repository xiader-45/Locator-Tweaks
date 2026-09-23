package com.locatortweaks.client;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.gui.PlayerListScreen;
import com.locatortweaks.gui.WaypointEditScreen;
import com.locatortweaks.gui.WaypointListScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class LocatorTweaksKeybinds {
    public static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath("locator-tweaks", "keybinds"));

    public static final KeyMapping toggleLocatorKey = new KeyMapping(
        "key.locator-tweaks.toggle_locator", Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY
    );
    public static final KeyMapping openPlayersKey = new KeyMapping(
        "key.locator-tweaks.open_players", Type.KEYBOARD, InputConstants.KEY_J, CATEGORY // J
    );
    public static final KeyMapping createWaypointKey = new KeyMapping(
        "key.locator-tweaks.create_waypoint", Type.KEYBOARD, InputConstants.KEY_B, CATEGORY // B
    );
    public static final KeyMapping openWaypointsKey = new KeyMapping(
        "key.locator-tweaks.open_waypoints", Type.KEYBOARD, InputConstants.KEY_U, CATEGORY // U
    );

    public static void handleKeybinds(Minecraft client) {
        if (client.player != null && (client.gui == null || client.gui.screen() == null)) {
            while (toggleLocatorKey.consumeClick()) {
                ModConfig config = ModConfig.getInstance();
                if (config.barDisplayMode == ModConfig.BarDisplayMode.DISABLED) {
                    config.barDisplayMode = (config.lastActiveBarDisplayMode != null && config.lastActiveBarDisplayMode != ModConfig.BarDisplayMode.DISABLED)
                        ? config.lastActiveBarDisplayMode
                        : ModConfig.BarDisplayMode.XP_WITH_MARKERS;
                } else {
                    config.lastActiveBarDisplayMode = config.barDisplayMode;
                    config.barDisplayMode = ModConfig.BarDisplayMode.DISABLED;
                }
                ModConfig.save();
                boolean isOn = config.barDisplayMode != ModConfig.BarDisplayMode.DISABLED;
                Component stateText = isOn
                    ? Component.translatable("locator-tweaks.status.on").withColor(0x55FF55)
                    : Component.translatable("locator-tweaks.status.off").withColor(0xFF5555);
                client.player.sendOverlayMessage(Component.translatable("locator-tweaks.message.locator_toggled", stateText));
            }

            while (openPlayersKey.consumeClick()) {
                client.setScreenAndShow(new PlayerListScreen(null));
            }

            while (createWaypointKey.consumeClick()) {
                client.setScreenAndShow(new WaypointEditScreen(null, null));
            }

            while (openWaypointsKey.consumeClick()) {
                client.setScreenAndShow(new WaypointListScreen(null));
            }
        }
    }

    public static void syncToConfig() {
        // Can't easily use KeyMappingHelper in common module without abstraction, skipping for now
    }

    private static void applyKey(KeyMapping mapping, int keyCode) {
        if (keyCode <= 0 || keyCode == InputConstants.UNKNOWN.getValue()) {
            mapping.setKey(InputConstants.UNKNOWN);
        } else {
            mapping.setKey(Type.KEYBOARD.getOrCreate(keyCode));
        }
    }

    public static void updateToggleLocatorKey(int val) {
        ModConfig.getInstance().toggleLocatorKey = val;
        applyKey(toggleLocatorKey, val);
        KeyMapping.resetMapping();
        saveVanillaOptions();
        ModConfig.save();
    }

    public static void updateOpenPlayersKey(int val) {
        ModConfig.getInstance().openPlayersKey = val;
        applyKey(openPlayersKey, val);
        KeyMapping.resetMapping();
        saveVanillaOptions();
        ModConfig.save();
    }

    public static void updateCreateWaypointKey(int val) {
        ModConfig.getInstance().createWaypointKey = val;
        applyKey(createWaypointKey, val);
        KeyMapping.resetMapping();
        saveVanillaOptions();
        ModConfig.save();
    }

    public static void updateOpenWaypointsKey(int val) {
        ModConfig.getInstance().openWaypointsKey = val;
        applyKey(openWaypointsKey, val);
        KeyMapping.resetMapping();
        saveVanillaOptions();
        ModConfig.save();
    }

    public static void saveVanillaOptions() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null) {
                mc.options.save();
            }
        } catch (Exception ignored) {
        }
    }
}
