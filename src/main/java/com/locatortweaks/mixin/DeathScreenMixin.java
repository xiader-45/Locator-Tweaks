package com.locatortweaks.mixin;

import com.locatortweaks.util.DeathPointManager;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void locatorTweaks$onDeathScreen(Component causeOfDeath, boolean hardcore, LocalPlayer player, CallbackInfo ci) {
        DeathPointManager.recordDeath(player);
    }
}
