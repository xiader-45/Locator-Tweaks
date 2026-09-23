package com.locatortweaks;

import com.locatortweaks.client.LocatorTweaksKeybinds;
import com.locatortweaks.client.NeoForgeClientSetup;
import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.LocatorFadeManager;
import com.locatortweaks.util.LodestoneManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("locator_tweaks")
public class LocatorTweaksNeoForge {

    public LocatorTweaksNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LocatorTweaks.LOGGER.info("Locator Tweaks (NeoForge) initializing...");

        ModConfig.configDir = FMLPaths.CONFIGDIR.get();
        ModConfig.load();

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.init(modEventBus, modContainer);
        }

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level world = event.getLevel();
        Player player = event.getEntity();
        if (world.isClientSide() && player != null) {
            BlockPos pos = event.getPos();
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
    }
}
