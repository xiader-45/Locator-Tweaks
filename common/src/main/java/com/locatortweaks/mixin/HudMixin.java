package com.locatortweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.locatortweaks.config.ModConfig;
import com.locatortweaks.config.PlayerLocatorConfig;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.HudRaiseManager;
import com.locatortweaks.util.LodestoneManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private LocatorBar locatorTweaks$topLocatorBar;

    @WrapOperation(
        method = "nextContextualInfoState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/waypoints/ClientWaypointManager;hasWaypoints()Z"
        )
    )
    private boolean locatorTweaks$modifyHasWaypoints(ClientWaypointManager instance, Operation<Boolean> original) {
        ModConfig config = ModConfig.getInstance();
        if (config.barDisplayMode == ModConfig.BarDisplayMode.DISABLED) {
            return false;
        }
        if (config.locatorPosition == ModConfig.LocatorPosition.TOP) {
            return false;
        }
        if (config.showOnlyWithCompass && !locatorTweaks$isHoldingCompass(this.minecraft)) {
            return false;
        }
        boolean showCompass = config.showCardinalDirections;
        if (showCompass) {
            return true;
        }
        return locatorTweaks$hasAnyVisibleWaypoints(instance);
    }

    @WrapOperation(
        method = "nextContextualInfoState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;willPrioritizeExperienceInfo()Z"
        )
    )
    private boolean locatorTweaks$wrapWillPrioritizeExperience(Hud instance, Operation<Boolean> original) {
        ModConfig config = ModConfig.getInstance();
        if (config.locatorPosition == ModConfig.LocatorPosition.BOTTOM) {
            if (config.barDisplayMode == ModConfig.BarDisplayMode.XP_WITH_MARKERS
                || config.barDisplayMode == ModConfig.BarDisplayMode.NO_XP) {
                return false;
            }
        }
        return original.call(instance);
    }

    @Inject(
        method = {"extractHotbarAndDecorations", "extractHotbar"}, require = 1,
        at = @At("TAIL")
    )
    private void locatorTweaks$renderTopLocator(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (config.locatorPosition != ModConfig.LocatorPosition.TOP) {
            return;
        }
        if (config.barDisplayMode == ModConfig.BarDisplayMode.DISABLED) {
            return;
        }
        boolean showCompass = config.showCardinalDirections;
        if (config.showOnlyWithCompass && !locatorTweaks$isHoldingCompass(this.minecraft)) {
            return;
        }
        if (this.minecraft.player == null) {
            return;
        }
        if (!showCompass && !locatorTweaks$hasWaypoints()) {
            return;
        }
        if (this.locatorTweaks$topLocatorBar == null) {
            this.locatorTweaks$topLocatorBar = new LocatorBar(this.minecraft);
        }
        this.locatorTweaks$topLocatorBar.extractBackground(extractor, deltaTracker);
        this.locatorTweaks$topLocatorBar.extractRenderState(extractor, deltaTracker);
    }

    @Unique
    private boolean locatorTweaks$hasAnyVisibleWaypoints(ClientWaypointManager wm) {
        if (DeathPointManager.hasActiveDeathPoint(this.minecraft) || LodestoneManager.hasActiveLodestones()
            || SpawnPointManager.hasActiveSpawnPoint(this.minecraft) || NetherPortalManager.hasActivePortalPoint(this.minecraft)) {
            return true;
        }
        if (wm == null || !wm.hasWaypoints()) {
            return false;
        }

        ModConfig config = ModConfig.getInstance();
        Entity cameraEntity = this.minecraft.getCameraEntity();
        double maxDistSq = config.maxDistance > 0 ? ((double) config.maxDistance * config.maxDistance) : Double.MAX_VALUE;

        boolean[] hasVisible = new boolean[1];
        wm.forEachWaypoint(cameraEntity, waypoint -> {
            if (hasVisible[0]) return;
            Optional<UUID> playerUuid = waypoint.id().left();
            if (playerUuid.isPresent()) {
                UUID u = playerUuid.get();
                if (DeathPointManager.isDeathPoint(u) || LodestoneManager.isLodestone(u)
                    || SpawnPointManager.isSpawnPoint(u) || NetherPortalManager.isPortalPoint(u)) {
                    hasVisible[0] = true;
                    return;
                }
                if (!config.isPlayerMarkersEnabled()) {
                    return;
                }
                PlayerLocatorConfig pCfg = config.getPlayerConfig(u);
                if (pCfg != null && !pCfg.enabled) {
                    return;
                }
                if (config.displayMode == ModConfig.LocatorDisplayMode.FAVORITES_ONLY) {
                    if (pCfg == null || !pCfg.favorite) {
                        return;
                    }
                }
                if (cameraEntity != null && maxDistSq < Double.MAX_VALUE) {
                    if (waypoint.distanceSquared(cameraEntity) > maxDistSq) {
                        return;
                    }
                }
                hasVisible[0] = true;
            } else {
                hasVisible[0] = true;
            }
        });

        return hasVisible[0];
    }

    @Unique
    private boolean locatorTweaks$hasWaypoints() {
        if (this.minecraft.player != null && this.minecraft.player.connection != null) {
            ClientWaypointManager wm = this.minecraft.player.connection.getWaypointManager();
            return locatorTweaks$hasAnyVisibleWaypoints(wm);
        }
        return DeathPointManager.hasActiveDeathPoint(this.minecraft) || LodestoneManager.hasActiveLodestones()
            || SpawnPointManager.hasActiveSpawnPoint(this.minecraft) || NetherPortalManager.hasActivePortalPoint(this.minecraft);
    }

    @Unique
    private static boolean locatorTweaks$isCompass(ItemStack stack) {
        return stack != null && (stack.is(Items.COMPASS) || stack.is(Items.RECOVERY_COMPASS));
    }

    @Unique
    private static boolean locatorTweaks$isHoldingCompass(Minecraft mc) {
        if (mc.player == null) return false;
        return locatorTweaks$isCompass(mc.player.getMainHandItem()) || locatorTweaks$isCompass(mc.player.getOffhandItem());
    }

    @ModifyConstant(
        method = "extractPlayerHealth", require = 0,
        constant = @Constant(intValue = 39)
    )
    private int locatorTweaks$modifyHealthBottomOffset(int original) {
        if (ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP) {
            return original;
        }
        return original + HudRaiseManager.getRaiseOffset();
    }


    @ModifyConstant(
        method = "extractVehicleHealth", require = 0,
        constant = @Constant(intValue = 39)
    )
    private int locatorTweaks$modifyVehicleHealthBottomOffset(int original) {
        if (ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP) {
            return original;
        }
        return original + HudRaiseManager.getRaiseOffset();
    }

    @ModifyConstant(
        method = "extractSelectedItemName", require = 0,
        constant = @Constant(intValue = 59)
    )
    private int locatorTweaks$modifySelectedItemNameOffset(int original) {
        if (ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP) {
            return original;
        }
        return original + HudRaiseManager.getRaiseOffset();
    }
}


