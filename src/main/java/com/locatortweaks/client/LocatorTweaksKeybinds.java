package com.locatortweaks.client;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.gui.PlayerListScreen;
import com.locatortweaks.gui.WaypointEditScreen;
import com.locatortweaks.gui.WaypointListScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class LocatorTweaksKeybinds {
   public static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath("locator-tweaks", "keybinds"));
   
   public static final KeyMapping TOGGLE_LOCATOR = KeyMappingHelper.registerKeyMapping(
      new KeyMapping("key.locator-tweaks.toggle_locator", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY)
   );
   public static final KeyMapping OPEN_PLAYERS = KeyMappingHelper.registerKeyMapping(
      new KeyMapping("key.locator-tweaks.open_players", Type.KEYSYM, 74, CATEGORY) // J
   );
   public static final KeyMapping CREATE_WAYPOINT = KeyMappingHelper.registerKeyMapping(
      new KeyMapping("key.locator-tweaks.create_waypoint", Type.KEYSYM, 66, CATEGORY) // B
   );
   public static final KeyMapping OPEN_WAYPOINTS = KeyMappingHelper.registerKeyMapping(
      new KeyMapping("key.locator-tweaks.open_waypoints", Type.KEYSYM, 85, CATEGORY) // U
   );

   public static void init() {
      syncFromConfig();

      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         if (client.player != null && (client.gui == null || client.gui.screen() == null)) {
            while (TOGGLE_LOCATOR.consumeClick()) {
               ModConfig config = ModConfig.getInstance();
               if (config.barDisplayMode == ModConfig.BarDisplayMode.DISABLED) {
                  config.barDisplayMode = (config.lastActiveBarDisplayMode != null && config.lastActiveBarDisplayMode != ModConfig.BarDisplayMode.DISABLED)
                     ? config.lastActiveBarDisplayMode
                     : ModConfig.BarDisplayMode.DEFAULT;
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

            while (OPEN_PLAYERS.consumeClick()) {
               client.setScreenAndShow(new PlayerListScreen(null));
            }

            while (CREATE_WAYPOINT.consumeClick()) {
               client.setScreenAndShow(new WaypointEditScreen(null, null));
            }

            while (OPEN_WAYPOINTS.consumeClick()) {
               client.setScreenAndShow(new WaypointListScreen(null));
            }
         }
      });
   }

   public static void syncFromConfig() {
      ModConfig config = ModConfig.getInstance();
      applyKey(TOGGLE_LOCATOR, config.toggleLocatorKey);
      applyKey(OPEN_PLAYERS, config.openPlayersKey);
      applyKey(CREATE_WAYPOINT, config.createWaypointKey);
      applyKey(OPEN_WAYPOINTS, config.openWaypointsKey);
      KeyMapping.resetMapping();
   }

   public static void syncToConfig() {
      ModConfig config = ModConfig.getInstance();
      config.toggleLocatorKey = KeyMappingHelper.getBoundKeyOf(TOGGLE_LOCATOR).getValue();
      config.openPlayersKey = KeyMappingHelper.getBoundKeyOf(OPEN_PLAYERS).getValue();
      config.createWaypointKey = KeyMappingHelper.getBoundKeyOf(CREATE_WAYPOINT).getValue();
      config.openWaypointsKey = KeyMappingHelper.getBoundKeyOf(OPEN_WAYPOINTS).getValue();
   }

   private static void applyKey(KeyMapping mapping, int keyCode) {
      if (keyCode <= 0 || keyCode == InputConstants.UNKNOWN.getValue()) {
         mapping.setKey(InputConstants.UNKNOWN);
      } else {
         mapping.setKey(Type.KEYSYM.getOrCreate(keyCode));
      }
   }

   public static void updateToggleLocatorKey(int key) {
      ModConfig.getInstance().toggleLocatorKey = key;
      applyKey(TOGGLE_LOCATOR, key);
      KeyMapping.resetMapping();
      saveVanillaOptions();
      ModConfig.save();
   }

   public static void updateOpenPlayersKey(int key) {
      ModConfig.getInstance().openPlayersKey = key;
      applyKey(OPEN_PLAYERS, key);
      KeyMapping.resetMapping();
      saveVanillaOptions();
      ModConfig.save();
   }

   public static void updateCreateWaypointKey(int key) {
      ModConfig.getInstance().createWaypointKey = key;
      applyKey(CREATE_WAYPOINT, key);
      KeyMapping.resetMapping();
      saveVanillaOptions();
      ModConfig.save();
   }

   public static void updateOpenWaypointsKey(int key) {
      ModConfig.getInstance().openWaypointsKey = key;
      applyKey(OPEN_WAYPOINTS, key);
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
