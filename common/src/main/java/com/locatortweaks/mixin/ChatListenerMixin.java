package com.locatortweaks.mixin;

import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.SpawnPointManager;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
    @Inject(method = "handleSystemMessage", at = @At("HEAD"))
    private void locatorTweaks$onSystemMessage(Component message, boolean bl, CallbackInfo ci) {
        locatorTweaks$checkSpawnMessage(message);
    }

    @Inject(method = "handleOverlay", at = @At("HEAD"))
    private void locatorTweaks$onOverlay(Component message, CallbackInfo ci) {
        locatorTweaks$checkSpawnMessage(message);
    }

    @Unique
    private static void locatorTweaks$checkSpawnMessage(Component message) {
        if (message == null || !ModConfig.getInstance().showSpawnPoint) {
            return;
        }

        boolean isSetSpawn = locatorTweaks$hasSetSpawnTranslation(message);

        if (!isSetSpawn) {
            String fullText = message.getString();
            if (fullText != null && !fullText.isEmpty()) {
                String bedMsg = Component.translatable("block.minecraft.bed.set_spawn").getString();
                String anchorMsg = Component.translatable("block.minecraft.respawn_anchor.set_spawn").getString();
                if ((bedMsg != null && !bedMsg.isEmpty() && fullText.contains(bedMsg))
                    || (anchorMsg != null && !anchorMsg.isEmpty() && fullText.contains(anchorMsg))) {
                    isSetSpawn = true;
                }
            }
        }

        if (isSetSpawn) {
            SpawnPointManager.confirmPotentialBed();
        }
    }

    @Unique
    private static boolean locatorTweaks$hasSetSpawnTranslation(Component component) {
        if (component.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if (key != null && (key.contains("set_spawn") || key.startsWith("block.minecraft.bed.set_spawn") || key.startsWith("block.minecraft.respawn_anchor.set_spawn"))) {
                return true;
            }
        }
        for (Component child : component.getSiblings()) {
            if (locatorTweaks$hasSetSpawnTranslation(child)) {
                return true;
            }
        }
        return false;
    }
}
