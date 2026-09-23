package com.locatortweaks.mixin;

import com.locatortweaks.config.PlayerLocatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void locatorTweaks$prependMarkerToTabList(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (playerInfo == null || playerInfo.getProfile() == null) {
            return;
        }

        UUID uuid = playerInfo.getProfile().id();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
            return;
        }

        Component prefix = PlayerLocatorConfig.getMarkerPrefix(uuid);
        if (prefix != null) {
            Component original = cir.getReturnValue();
            if (original != null) {
                cir.setReturnValue(Component.empty().append(prefix).append(original));
            }
        }
    }
}
