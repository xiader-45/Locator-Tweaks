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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
        ModConfig.load();
        LocatorTweaksKeybinds.init();
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
                        SpawnPointManager.recordPotentialBed(pos, world.dimension().identifier().toString());
                        SpawnPointManager.recordBed(pos, world.dimension().identifier().toString());
                    }
                } else if (state.getBlock() instanceof RespawnAnchorBlock) {
                    if (world.dimension() == Level.NETHER) {
                        SpawnPointManager.recordPotentialBed(pos, world.dimension().identifier().toString());
                        if (state.hasProperty(RespawnAnchorBlock.CHARGE) && state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                            SpawnPointManager.recordBed(pos, world.dimension().identifier().toString());
                        }
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}
