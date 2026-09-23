package com.locatortweaks.client;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.LocatorFadeManager;
import com.locatortweaks.util.LodestoneManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class LocatorTweaksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfig.configDir = FabricLoader.getInstance().getConfigDir();
        ModConfig.load();
        
        KeyMappingHelper.registerKeyMapping(LocatorTweaksKeybinds.toggleLocatorKey);
        KeyMappingHelper.registerKeyMapping(LocatorTweaksKeybinds.openPlayersKey);
        KeyMappingHelper.registerKeyMapping(LocatorTweaksKeybinds.createWaypointKey);
        KeyMappingHelper.registerKeyMapping(LocatorTweaksKeybinds.openWaypointsKey);
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> LocatorTweaksKeybinds.handleKeybinds(client));

        ClientTickEvents.END_CLIENT_TICK.register(DeathPointManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(LodestoneManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(SpawnPointManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(NetherPortalManager::tick);
        ClientTickEvents.END_CLIENT_TICK.register(CustomWaypointManager::tick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            DeathPointManager.resetState();
            SpawnPointManager.resetState();
            NetherPortalManager.resetState();
            LodestoneManager.clearAll(client);
            CustomWaypointManager.clearAllWaypoints(client);
            LocatorFadeManager.requestReset();
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() && player instanceof LocalPlayer) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof BedBlock) {
                    if (world.dimension() == Level.OVERWORLD) {
                        if (state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) != BedPart.HEAD) {
                            pos = pos.relative(state.getValue(BedBlock.FACING));
                        }
                        SpawnPointManager.forceSpawnPoint(pos, Level.OVERWORLD);
                    }
                } else if (state.getBlock() instanceof RespawnAnchorBlock) {
                    if (world.dimension() == Level.NETHER) {
                        int charges = state.getValue(RespawnAnchorBlock.CHARGE);
                        if (charges > 0) {
                            SpawnPointManager.forceSpawnPoint(pos, Level.NETHER);
                        }
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}
