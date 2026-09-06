package com.locatortweaks.mixin;

import com.locatortweaks.util.SpawnPointManager;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @Inject(method = "handleSystemMessage", at = @At("HEAD"))
    private void locatorTweaks$onSystemMessage(Component message, boolean bl, CallbackInfo ci) {
        if (message != null) {
            if (message.getContents() instanceof TranslatableContents tc) {
                if (tc.getKey().contains("set_spawn")) {
                    SpawnPointManager.confirmPotentialBed();
                }
            } else {
                String str = message.getString().toLowerCase();
                if (str.contains("set_spawn") || str.contains("spawn") || str.contains("возрожд")) {
                    SpawnPointManager.confirmPotentialBed();
                }
            }
        }
    }

    @Inject(method = "handleOverlay", at = @At("HEAD"))
    private void locatorTweaks$onOverlay(Component message, CallbackInfo ci) {
        if (message != null) {
            if (message.getContents() instanceof TranslatableContents tc) {
                if (tc.getKey().contains("set_spawn")) {
                    SpawnPointManager.confirmPotentialBed();
                }
            } else {
                String str = message.getString().toLowerCase();
                if (str.contains("set_spawn") || str.contains("spawn") || str.contains("возрожд")) {
                    SpawnPointManager.confirmPotentialBed();
                }
            }
        }
    }
}
