package com.locatortweaks.mixin;

import com.locatortweaks.config.PlayerLocatorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void locatorTweaks$prependMarkerToNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player player) {
            UUID uuid = player.getUUID();
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
}
