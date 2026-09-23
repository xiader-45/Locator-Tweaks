package com.locatortweaks.client;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.HudRaiseManager;
import com.locatortweaks.util.LocatorFadeManager;
import com.locatortweaks.util.LodestoneManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import org.joml.Matrix3x2fStack;

import java.util.HashSet;
import java.util.Set;

public class NeoForgeClientSetup {

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoForgeClientSetup::registerKeys);

        if (ModList.get().isLoaded("yet_another_config_lib_v3")) {
            modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, screen) -> NeoForgeConfigScreen.createScreen(screen)
            );
        }

        NeoForge.EVENT_BUS.register(new ClientEvents());
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(LocatorTweaksKeybinds.toggleLocatorKey);
        event.register(LocatorTweaksKeybinds.openPlayersKey);
        event.register(LocatorTweaksKeybinds.createWaypointKey);
        event.register(LocatorTweaksKeybinds.openWaypointsKey);
    }

    public static class ClientEvents {
        private final Set<Identifier> activeLayers = new HashSet<>();

        @SubscribeEvent
        public void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            LocatorTweaksKeybinds.handleKeybinds(client);
            
            DeathPointManager.tick(client);
            LodestoneManager.tick(client);
            SpawnPointManager.tick(client);
            NetherPortalManager.tick(client);
            CustomWaypointManager.tick(client);

            // Safety clear in case of skipped Post events
            activeLayers.clear();
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
            if (event.isCanceled()) return;

            Identifier name = event.getName();
            if (name.equals(VanillaGuiLayers.PLAYER_HEALTH) ||
                name.equals(VanillaGuiLayers.FOOD_LEVEL) ||
                name.equals(VanillaGuiLayers.ARMOR_LEVEL) ||
                name.equals(VanillaGuiLayers.AIR_LEVEL) ||
                name.equals(VanillaGuiLayers.VEHICLE_HEALTH) ||
                name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
                
                int raiseOffset = HudRaiseManager.getRaiseOffset();
                if (raiseOffset != 0 && ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.BOTTOM) {
                    Matrix3x2fStack pose = event.getGuiGraphics().pose();
                    pose.pushMatrix();
                    pose.translate(0, -raiseOffset);
                    activeLayers.add(name);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
            if (activeLayers.remove(event.getName())) {
                event.getGuiGraphics().pose().popMatrix();
            }
        }

        @SubscribeEvent
        public void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
            DeathPointManager.resetState();
            SpawnPointManager.resetState();
            NetherPortalManager.resetState();
            LodestoneManager.clearAll(Minecraft.getInstance());
            CustomWaypointManager.clearAllWaypoints(Minecraft.getInstance());
            LocatorFadeManager.requestReset();
        }
    }
}