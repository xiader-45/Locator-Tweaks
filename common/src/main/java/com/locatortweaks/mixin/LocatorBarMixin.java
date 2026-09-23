package com.locatortweaks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.locatortweaks.hud.MarkerFadeState;
import com.locatortweaks.hud.MarkerType;
import com.locatortweaks.hud.PendingHearts;
import com.locatortweaks.hud.PendingMarker;
import com.locatortweaks.hud.PendingTooltip;
import com.locatortweaks.config.ModConfig;
import com.locatortweaks.config.CustomWaypoint;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.config.PlayerLocatorConfig;
import com.locatortweaks.util.DeathPointManager;
import com.locatortweaks.util.HudRaiseManager;
import com.locatortweaks.util.LocatorFadeManager;
import com.locatortweaks.util.LodestoneManager;
import com.locatortweaks.util.NetherPortalManager;
import com.locatortweaks.util.SpawnPointManager;
import com.locatortweaks.util.WorldKeyUtil;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin implements ContextualBar {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Unique
   private static final Identifier EXPERIENCE_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_background");
   @Unique
   private static final Identifier EXPERIENCE_BAR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_progress");
   @Unique
   private static final Identifier[] DEATH_POINT_SPRITES = new Identifier[]{
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/death_point_0"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/death_point_1"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/death_point_2"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/death_point_3")
   };
   @Unique
   private static final Identifier[] SPAWN_POINT_SPRITES = new Identifier[]{
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/spawn_point_0"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/spawn_point_1"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/spawn_point_2"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/spawn_point_3")
   };
   @Unique
   private static final Identifier[] NETHER_PORTAL_SPRITES = new Identifier[]{
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/nether_portal_point_0"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/nether_portal_point_1"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/nether_portal_point_2"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/nether_portal_point_3")
   };
   @Unique
   private static final Identifier[] WAYPOINT_PIN_SPRITES = new Identifier[]{
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_0"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_1"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_2"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_3")
   };
   @Unique
   private static final Identifier[] LODESTONE_POINT_SPRITES = new Identifier[]{
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/lodestone_point_0"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/lodestone_point_1"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/lodestone_point_2"),
      Identifier.fromNamespaceAndPath("locator-tweaks", "hud/lodestone_point_3")
   };
   @Unique
   private static final Identifier HEART_CONTAINER_SPRITE = Identifier.withDefaultNamespace("hud/heart/container");
   @Unique
   private static final Identifier HEART_FULL_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
   @Unique
   private static final Identifier HEART_HALF_SPRITE = Identifier.withDefaultNamespace("hud/heart/half");
   @Unique
   private static final Identifier STAR_ACTIVE_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "widget/star_active");
   @Unique
   private static final List<PendingMarker> locatorTweaks$pendingMarkers = new ArrayList<>();
   @Unique
   private static final Set<String> locatorTweaks$visibleKeys = new HashSet<>();
   @Unique
   private static final Set<String> locatorTweaks$worldKeys = new HashSet<>();
   @Unique
   private static final Map<UUID, String> locatorTweaks$playerKeyCache = new HashMap<>();
   @Unique
   private static final Set<String> locatorTweaks$knownWorldKeys = new HashSet<>();
   @Unique
   private static final Map<String, MarkerFadeState> locatorTweaks$fadeStates = new HashMap<>();
   @Unique
   private static long locatorTweaks$lastFadeTime = 0L;
   @Unique
   private static boolean locatorTweaks$firstFrame = true;
   @Unique
   private static int locatorTweaks$lastGuiWidth = 0;
   @Unique
   private static final Map<String, Float> locatorTweaks$tooltipSmoothX = new HashMap<>();
   @Unique
   private static final Map<String, Float> locatorTweaks$lastMarkerResolvedX = new HashMap<>();
   @Unique
   private static final List<PendingTooltip> locatorTweaks$pendingTooltips = new ArrayList<>();
   @Unique
   private static boolean locatorTweaks$zoomifyChecked = false;
   @Unique
   private static Method locatorTweaks$zoomifyMethodFloat = null;
   @Unique
   private static Method locatorTweaks$zoomifyMethodNoArg = null;

   @Unique
   private static int locatorTweaks$getDistanceTier(double dist) {
      if (ModConfig.getInstance().preventDistanceScaling) return 0;
      if (dist <= 15.0) {
         return 0;
      } else if (dist <= 50.0) {
         return 1;
      } else {
         return dist <= 100.0 ? 2 : 3;
      }
   }

   @Unique
   private static boolean locatorTweaks$isCompass(ItemStack stack) {
      return stack != null && (stack.is(Items.COMPASS) || stack.is(Items.RECOVERY_COMPASS));
   }

   @Unique
   private static boolean locatorTweaks$isHoldingCompass(Minecraft mc) {
      return mc.player == null ? false : locatorTweaks$isCompass(mc.player.getMainHandItem()) || locatorTweaks$isCompass(mc.player.getOffhandItem());
   }

   @Unique
   private static void locatorTweaks$resetFadeStates() {
      locatorTweaks$fadeStates.clear();
      locatorTweaks$tooltipSmoothX.clear();
      locatorTweaks$lastMarkerResolvedX.clear();
      locatorTweaks$knownWorldKeys.clear();
      locatorTweaks$visibleKeys.clear();
      locatorTweaks$worldKeys.clear();
      locatorTweaks$playerKeyCache.clear();
      locatorTweaks$firstFrame = true;
      locatorTweaks$lastFadeTime = 0L;
   }

   @Unique
   private static void locatorTweaks$collectWorldKeys(Minecraft mc, Set<String> keys) {
      keys.clear();
      if (mc.level != null && mc.player != null) {
         if (SpawnPointManager.hasActiveSpawnPoint(mc)) {
            BlockPos p = SpawnPointManager.getPos(SpawnPointManager.SPAWN_POINT_UUID);
            if (p != null) {
               keys.add("spawn_" + p.getX() + "_" + p.getY() + "_" + p.getZ());
            }
         }

         if (NetherPortalManager.hasActivePortalPoint(mc)) {
            BlockPos p = NetherPortalManager.getPos(NetherPortalManager.NETHER_PORTAL_UUID);
            if (p != null) {
               keys.add("portal_" + p.getX() + "_" + p.getY() + "_" + p.getZ());
            }
         }

         if (DeathPointManager.hasActiveDeathPoint(mc)) {
            BlockPos p = DeathPointManager.getPos();
            if (p != null) {
               keys.add("death_" + p.getX() + "_" + p.getY() + "_" + p.getZ());
            }
         }

         for (Entry<UUID, LodestoneManager.LodestoneData> entry : LodestoneManager.getActiveLodestones().entrySet()) {
            BlockPos p = entry.getValue().pos();
            if (p != null) {
               keys.add("lodestone_" + p.getX() + "_" + p.getY() + "_" + p.getZ());
            }
         }

         if (mc.getConnection() != null) {
            ClientWaypointManager wm = mc.getConnection().getWaypointManager();
            if (wm != null) {
               wm.forEachWaypoint(
                  mc.getCameraEntity(),
                  w -> {
                     Optional<UUID> pu = w.id().left();
                     if (pu.isPresent()) {
                        UUID u = pu.get();
                        if (!SpawnPointManager.isSpawnPoint(u)
                           && !NetherPortalManager.isPortalPoint(u)
                           && !DeathPointManager.isDeathPoint(u)
                           && !LodestoneManager.isLodestone(u)) {
                           if (CustomWaypointManager.isCustomWaypoint(u)) {
                              CustomWaypoint cwp = CustomWaypointManager.getCustomWaypoint(u);
                              if (cwp != null) {
                                 keys.add(cwp.getMarkerKey());
                              } else {
                                 keys.add("custom_wp_" + u.toString());
                              }
                           } else if (ModConfig.getInstance().isPlayerMarkersEnabled()) {
                              String pk = locatorTweaks$playerKeyCache.computeIfAbsent(u, id -> "player_" + id);
                              keys.add(pk);
                           }
                        }
                     } else {
                        keys.add("waypoint_" + w.id().toString());
                     }
                  }
               );
            }
         }
      }
   }

   @Unique
   private static int locatorTweaks$getTopY(Minecraft mc) {
      int topY = 10;
      if (mc != null && mc.gui != null && mc.gui.hud.getBossOverlay() != null) {
         try {
            Map<?, ?> map = ((BossHealthOverlayAccessor)mc.gui.hud.getBossOverlay()).getEvents();
            if (map != null && !map.isEmpty()) {
               topY = 12 + map.size() * 19;
            }
         } catch (Throwable var3) {
         }
      }

      return topY;
   }

   public int top(Window window) {
      return ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP
         ? locatorTweaks$getTopY(this.minecraft)
         : window.getGuiScaledHeight() - 24 - 5;
   }

   @WrapOperation(
      method = "extractBackground",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/LocatorBar;top(Lcom/mojang/blaze3d/platform/Window;)I")
   )
   private int locatorTweaks$wrapTopBackground(LocatorBar instance, Window window, Operation<Integer> original) {
      return ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP
         ? locatorTweaks$getTopY(this.minecraft)
         : (Integer)original.call(new Object[]{instance, window});
   }

   @WrapOperation(
      method = "extractRenderState",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/LocatorBar;top(Lcom/mojang/blaze3d/platform/Window;)I")
   )
   private int locatorTweaks$wrapTopRenderState(LocatorBar instance, Window window, Operation<Integer> original) {
      return ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP
         ? locatorTweaks$getTopY(this.minecraft)
         : (Integer)original.call(new Object[]{instance, window});
   }

   @Inject(method = "extractRenderState", at = @At("HEAD"))
   private void locatorTweaks$onStartExtractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (LocatorFadeManager.isResetRequested()) {
         LocatorFadeManager.clearResetRequest();
         locatorTweaks$resetFadeStates();
      }

      locatorTweaks$pendingMarkers.clear();
      locatorTweaks$pendingTooltips.clear();
      if (ModConfig.getInstance().showOnlyWithCompass && !locatorTweaks$isHoldingCompass(this.minecraft)) {
         HudRaiseManager.setTarget(0.0F, 0.0F);
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void locatorTweaks$onEndExtractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft mc = this.minecraft;
      if (mc.level != null && mc.player != null) {
         if (ModConfig.getInstance().showOnlyWithCompass && !locatorTweaks$isHoldingCompass(mc)) {
            HudRaiseManager.setTarget(0.0F, 0.0F);
            locatorTweaks$pendingMarkers.clear();
            locatorTweaks$pendingTooltips.clear();
            locatorTweaks$resetFadeStates();
         } else {
            long now = System.currentTimeMillis();
            float dt = locatorTweaks$lastFadeTime == 0L ? 0.016F : Math.clamp((float)(now - locatorTweaks$lastFadeTime) / 1000.0F, 0.001F, 0.1F);
            locatorTweaks$lastFadeTime = now;
            if (mc.isPaused()) {
               dt = 0.0F;
            }

            int guiWidth = extractor.guiWidth();
            boolean isResize = (locatorTweaks$lastGuiWidth != 0 && locatorTweaks$lastGuiWidth != guiWidth);
            locatorTweaks$lastGuiWidth = guiWidth;

            if (isResize) {
               locatorTweaks$tooltipSmoothX.clear();
               locatorTweaks$lastMarkerResolvedX.clear();
            }

            double centerX = guiWidth / 2.0;
            float fadeSpeed = 6.0F;
            locatorTweaks$visibleKeys.clear();
            locatorTweaks$collectWorldKeys(mc, locatorTweaks$worldKeys);

            for (PendingMarker m : locatorTweaks$pendingMarkers) {
               locatorTweaks$visibleKeys.add(m.markerKey);
               MarkerFadeState state = locatorTweaks$fadeStates.get(m.markerKey);
               if (state == null) {
                  float markerCenterX = m.x + (m.getVisualLeft() + m.getVisualRight()) / 2.0F;
                  boolean isAtEdge = locatorTweaks$getMarkerEdgeFade(markerCenterX, centerX) < 0.99F;
                  boolean isKnown = locatorTweaks$knownWorldKeys.contains(m.markerKey);
                  if (locatorTweaks$firstFrame || isKnown || isAtEdge) {
                     state = new MarkerFadeState(1.0F, false, 0.0F);
                  } else {
                     state = new MarkerFadeState(0.0F, false, 0.0F);
                  }
                  locatorTweaks$fadeStates.put(m.markerKey, state);
               }

               state.fadingOut = false;
               state.alpha = Math.min(1.0F, state.alpha + dt * fadeSpeed);
               if (state.introTooltipTimer > 0.0F) {
                  state.introTooltipTimer = Math.max(0.0F, state.introTooltipTimer - dt);
               }
               state.lastMarker = m;
               m.fadeProgress = state.alpha;
            }

            locatorTweaks$worldKeys.addAll(locatorTweaks$visibleKeys);

            Iterator<Entry<String, MarkerFadeState>> it = locatorTweaks$fadeStates.entrySet().iterator();

            while (it.hasNext()) {
               Entry<String, MarkerFadeState> entry = it.next();
               String key = entry.getKey();
               MarkerFadeState state = entry.getValue();
               if (!locatorTweaks$visibleKeys.contains(key)) {
                  if (locatorTweaks$worldKeys.contains(key)) {
                     state.lastMarker = null;
                     state.fadingOut = false;
                     state.alpha = 1.0F;
                     locatorTweaks$tooltipSmoothX.remove(key);
                     locatorTweaks$lastMarkerResolvedX.remove(key);
                     continue;
                  }

                  state.fadingOut = true;
                  state.alpha = Math.max(0.0F, state.alpha - dt * fadeSpeed);
                  if (state.alpha > 0.01F && state.lastMarker != null) {
                     state.lastMarker.fadeProgress = state.alpha;
                     locatorTweaks$pendingMarkers.add(state.lastMarker);
                  } else {
                     it.remove();
                     locatorTweaks$knownWorldKeys.remove(key);
                     locatorTweaks$tooltipSmoothX.remove(key);
                     locatorTweaks$lastMarkerResolvedX.remove(key);
                  }
               }
            }

            locatorTweaks$knownWorldKeys.retainAll(locatorTweaks$worldKeys);
            locatorTweaks$knownWorldKeys.addAll(locatorTweaks$worldKeys);

            if (locatorTweaks$firstFrame) {
               locatorTweaks$firstFrame = false;
            }

            int barLeft = (guiWidth - 182) / 2;
            resolveMarkers(locatorTweaks$pendingMarkers, barLeft, 182);
            float alphaFactor = Math.clamp(ModConfig.getInstance().markerOpacity / 100.0F, 0.0F, 1.0F);

            this.locatorTweaks$renderCardinalDirections(extractor, mc);

            for (PendingMarker m : locatorTweaks$pendingMarkers) {
               float markerCenterX = m.x + (m.getVisualLeft() + m.getVisualRight()) / 2.0F;
               float edgeFade = locatorTweaks$getMarkerEdgeFade(markerCenterX, centerX);
               m.fadeProgress *= edgeFade;

               int markerAlpha = Math.round(alphaFactor * m.fadeProgress * 255.0F);
               if (markerAlpha > 0 && edgeFade > 0.01F) {
                  this.renderResolvedMarker(extractor, m, markerAlpha);
               }
            }

            boolean isScreenOpen = mc.gui != null && mc.gui.screen() != null;
            boolean shiftDown = !isScreenOpen
               && (
                  (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT))
                     || mc.options != null && mc.options.keyShift.isDown()
               );
            boolean isTopBar = ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP;

            boolean isNameActive = switch (ModConfig.getInstance().markerInfoMode) {
               case ALWAYS -> true;
               case HOLD -> shiftDown;
               case DISABLED -> false;
            };

            boolean isDistanceActive = switch (ModConfig.getInstance().distanceDisplayMode) {
               case ALWAYS -> true;
               case HOLD -> shiftDown;
               case DISABLED -> false;
            };

            boolean hasAnyActive = isNameActive || isDistanceActive || (shiftDown && ModConfig.getInstance().showPlayerHealthOnShift);

            for (PendingMarker mx : locatorTweaks$pendingMarkers) {
               boolean shouldShow = hasAnyActive || HudRaiseManager.isTextVisible();
               if (shouldShow && mx.fadeProgress > 0.02F) {
                  String typeName = (mx.labelName != null && isNameActive && mx.type != null)
                     ? mx.type.getDisplayName()
                     : null;

                  boolean hasContent = mx.labelName != null || typeName != null || mx.pendingHearts != null || mx.distText != null;
                  if (!hasContent) {
                     continue;
                  }

                  float markerFade = mx.fadeProgress;
                  float markerCenterX = mx.x + (mx.getVisualLeft() + mx.getVisualRight()) / 2.0F;
                  int baseY = isTopBar ? mx.y + 7 : mx.y + 2;
                  int themeColor = locatorTweaks$getMarkerThemeColor(mx);
                  locatorTweaks$pendingTooltips.add(
                     new PendingTooltip(mx.markerKey, markerCenterX, baseY, mx.labelName, typeName, mx.pendingHearts, mx.distText, themeColor, markerFade, mx.isFavorite, mc)
                  );
               }
            }

            locatorTweaks$pendingMarkers.clear();
            if (!hasAnyActive || locatorTweaks$pendingTooltips.isEmpty()) {
               HudRaiseManager.setTarget(0.0F, 0.0F);
            } else if (isTopBar) {
               HudRaiseManager.setTarget(0.0F, 1.0F);
            } else {
               int maxBoxHeight = 0;

               for (PendingTooltip tooltip : locatorTweaks$pendingTooltips) {
                  if (tooltip.height > maxBoxHeight) {
                     maxBoxHeight = tooltip.height;
                  }
               }

               if (maxBoxHeight > 0) {
                  HudRaiseManager.setTarget(maxBoxHeight + 6.0F, 1.0F);
               } else {
                  HudRaiseManager.setTarget(0.0F, 0.0F);
               }
            }

            float prog = isTopBar ? (hasAnyActive ? 1.0F : 0.0F) : HudRaiseManager.getTextProgress();
            if ((hasAnyActive || prog > 0.01F) && !locatorTweaks$pendingTooltips.isEmpty()) {
               int alpha = Math.clamp((long)Math.round(prog * 255.0F), 0, 255);
               if (alpha < 4) {
                  locatorTweaks$pendingTooltips.clear();
               } else {
                  int yOffset = Math.round((1.0F - prog) * 5.0F);
                  resolveTooltips(locatorTweaks$pendingTooltips, guiWidth, 4.0F);

                  float smoothFactor = (mc.isPaused() || isResize) ? 1.0F : (1.0F - (float)Math.exp(-dt * 14.0F));

                  for (PendingTooltip tooltipx : locatorTweaks$pendingTooltips) {
                     float targetResolvedX = tooltipx.x;
                     if (tooltipx.markerKey != null) {
                        Float prevSmoothX = locatorTweaks$tooltipSmoothX.get(tooltipx.markerKey);
                        if (prevSmoothX == null || isResize || mc.isPaused()) {
                           locatorTweaks$tooltipSmoothX.put(tooltipx.markerKey, targetResolvedX);
                           tooltipx.x = targetResolvedX;
                        } else {
                           float newX = prevSmoothX + (targetResolvedX - prevSmoothX) * smoothFactor;
                           locatorTweaks$tooltipSmoothX.put(tooltipx.markerKey, newX);
                           tooltipx.x = newX;
                        }
                     }
                     this.renderTooltipBox(extractor, tooltipx, yOffset, prog);
                  }

                  locatorTweaks$pendingTooltips.clear();
               }
            } else {
               locatorTweaks$pendingTooltips.clear();
            }
         }
      } else {
         locatorTweaks$resetFadeStates();
         locatorTweaks$pendingMarkers.clear();
         locatorTweaks$pendingTooltips.clear();
      }
   }

   @Unique
   private static int locatorTweaks$compareMarkers(PendingMarker m1, PendingMarker m2) {
      if (m1 == m2) return 0;
      float diff = m1.originalX - m2.originalX;
      float hysteresis = 3.0F;

      if (diff < -hysteresis) {
         return -1;
      } else if (diff > hysteresis) {
         return 1;
      }

      if (m1.markerKey != null && m2.markerKey != null) {
         Float prev1 = locatorTweaks$lastMarkerResolvedX.get(m1.markerKey);
         Float prev2 = locatorTweaks$lastMarkerResolvedX.get(m2.markerKey);
         if (prev1 != null && prev2 != null) {
            float prevDiff = prev1 - prev2;
            if (Math.abs(prevDiff) > 0.001F) {
               return Float.compare(prev1, prev2);
            }
         }
         return m1.markerKey.compareTo(m2.markerKey);
      }

      return Float.compare(m1.originalX, m2.originalX);
   }

   @Unique
   private static int locatorTweaks$compareTooltips(PendingTooltip t1, PendingTooltip t2) {
      if (t1 == t2) return 0;
      float diff = t1.targetX - t2.targetX;
      if (Math.abs(diff) > 2.0F) {
         return Float.compare(t1.targetX, t2.targetX);
      }
      if (t1.markerKey != null && t2.markerKey != null) {
         Float prev1 = locatorTweaks$tooltipSmoothX.get(t1.markerKey);
         Float prev2 = locatorTweaks$tooltipSmoothX.get(t2.markerKey);
         if (prev1 != null && prev2 != null) {
            float prevDiff = prev1 - prev2;
            if (Math.abs(prevDiff) > 0.001F) {
               return Float.compare(prev1, prev2);
            }
         }
         return t1.markerKey.compareTo(t2.markerKey);
      }
      return Float.compare(t1.targetX, t2.targetX);
   }

   @Unique
   private static void resolveMarkers(List<PendingMarker> list, int barLeft, int barWidth) {
      if (list.size() > 1) {
         for (PendingMarker m : list) {
            m.x = m.originalX;
            m.collided = false;
         }

         list.sort(LocatorBarMixin::locatorTweaks$compareMarkers);
         int n = list.size();

         for (int iter = 0; iter < 25; iter++) {
            for (int i = 0; i < n - 1; i++) {
               PendingMarker m1 = list.get(i);
               PendingMarker m2 = list.get(i + 1);
               float vr1 = m1.x + m1.getVisualRight();
               float vl2 = m2.x + m2.getVisualLeft();
               float overlap = vr1 + 1.0F - vl2;
               if (overlap > 0.0F) {
                  m1.collided = true;
                  m2.collided = true;
                  float shift = overlap / 2.0F;
                  m1.x -= shift;
                  m2.x += shift;
               }
            }
         }

         list.get(0).x = Math.round(list.get(0).x);

         for (int ix = 0; ix < n - 1; ix++) {
            PendingMarker m1 = list.get(ix);
            PendingMarker m2 = list.get(ix + 1);
            m2.x = Math.round(m2.x);
            int vr1 = Math.round(m1.x) + m1.getVisualRight();
            int vl2 = Math.round(m2.x) + m2.getVisualLeft();
            if (m1.collided && m2.collided) {
               float flushX = vr1 + 1 - m2.getVisualLeft();
               if (flushX > m2.x || Math.abs(flushX - m2.x) <= 1.0F && vr1 + 1 != vl2) {
                  m2.x = flushX;
               }
            } else if (vl2 < vr1 + 1) {
               m2.x = vr1 + 1 - m2.getVisualLeft();
            }
         }

         int minVisibleX = barLeft + 1;
         int maxVisibleX = barLeft + barWidth - 2;
         PendingMarker last = list.get(n - 1);
         int lastVr = Math.round(last.x) + last.getVisualRight();
         if (lastVr > maxVisibleX) {
            float diff = lastVr - maxVisibleX;
            for (int i = n - 1; i >= 0; i--) {
               PendingMarker m = list.get(i);
               m.x -= diff;
               if (i > 0) {
                  PendingMarker prev = list.get(i - 1);
                  int prevVr = Math.round(prev.x) + prev.getVisualRight();
                  int curVl = Math.round(m.x) + m.getVisualLeft();
                  if (prevVr + 1 > curVl) {
                     diff = prevVr + 1 - curVl;
                  } else {
                     break;
                  }
               }
            }
         }

         PendingMarker first = list.get(0);
         int firstVl = Math.round(first.x) + first.getVisualLeft();
         if (firstVl < minVisibleX) {
            float diff = minVisibleX - firstVl;
            for (int i = 0; i < n; i++) {
               PendingMarker m = list.get(i);
               m.x += diff;
               if (i < n - 1) {
                  PendingMarker next = list.get(i + 1);
                  int curVr = Math.round(m.x) + m.getVisualRight();
                  int nextVl = Math.round(next.x) + next.getVisualLeft();
                  if (curVr + 1 > nextVl) {
                     diff = curVr + 1 - nextVl;
                  } else {
                     break;
                  }
               }
            }
         }
      }

      for (PendingMarker m : list) {
         if (m.markerKey != null) {
            locatorTweaks$lastMarkerResolvedX.put(m.markerKey, m.x);
         }
      }
   }

   @Unique
   private void renderResolvedMarker(GuiGraphicsExtractor extractor, PendingMarker m, int markerAlpha) {
      int drawX = Math.round(m.x);
      switch (m.type) {
         case DEATH:
            int skullColor = markerAlpha << 24 | 16777215;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, DEATH_POINT_SPRITES[m.tier], drawX, m.y, 9, 9, skullColor);
            break;
         case SPAWN:
            int spawnColor = markerAlpha << 24 | 16777215;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, SPAWN_POINT_SPRITES[m.tier], drawX, m.y, 9, 9, spawnColor);
            break;
         case PORTAL:
            int portalColor = markerAlpha << 24 | 16777215;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, NETHER_PORTAL_SPRITES[m.tier], drawX, m.y, 9, 9, portalColor);
            break;
         case LODESTONE:
            int lodeColor = markerAlpha << 24 | 16777215;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, LODESTONE_POINT_SPRITES[m.tier], drawX, m.y, 9, 9, lodeColor);
            break;
         case CUSTOM_WAYPOINT:
            int pinColor = markerAlpha << 24 | (m.color & 0x00FFFFFF);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, WAYPOINT_PIN_SPRITES[m.tier], drawX, m.y, 9, 9, pinColor);
            break;
         case HEAD:
            int headY = m.y + Math.floorDiv(9 - m.height, 2);
            if (m.headOutlineColor != 0) {
               int outlineColor = markerAlpha << 24 | m.headOutlineColor & 16777215;
               extractor.fill(drawX - 1, headY - 1, drawX + m.width + 1, headY, outlineColor);
               extractor.fill(drawX - 1, headY + m.height, drawX + m.width + 1, headY + m.height + 1, outlineColor);
               extractor.fill(drawX - 1, headY, drawX, headY + m.height, outlineColor);
               extractor.fill(drawX + m.width, headY, drawX + m.width + 1, headY + m.height, outlineColor);
            }

            int headColor = markerAlpha << 24 | 16777215;
            PlayerFaceExtractor.extractRenderState(extractor, m.playerSkin, drawX, headY, m.width, headColor);
            break;
         case DOT:
            int dotY = m.y + Math.floorDiv(9 - m.height, 2);
            int dotColor = markerAlpha << 24 | m.color & 16777215;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, m.sprite, drawX, dotY, m.width, m.height, dotColor);
            break;
         default:
            int fallbackColor = markerAlpha << 24 | m.color & 16777215;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, m.sprite, drawX, m.y, 9, 9, fallbackColor);
      }

      if (m.hasArrow && ModConfig.getInstance().showHeightArrows) {
         float markerCenter = m.x + (m.getVisualLeft() + m.getVisualRight()) / 2.0F;
         int arrowX = Math.round(markerCenter - m.arrowWidth / 2.0F);
         int arrowColor = markerAlpha << 24 | 16777215;
         extractor.blitSprite(RenderPipelines.GUI_TEXTURED, m.arrowSprite, arrowX, m.arrowTop, m.arrowWidth, m.arrowHeight, arrowColor);
      }
   }

   @Unique
   private static int locatorTweaks$getMarkerThemeColor(PendingMarker mx) {
      if (mx == null) return 0x3A4556;
      switch (mx.type) {
         case CUSTOM_WAYPOINT:
            return (mx.color & 0x00FFFFFF) != 0 ? (mx.color & 0x00FFFFFF) : 0x3A4556;
         case DEATH:
            return 0xD84444;
         case SPAWN:
            return 0x00F0FF;
         case PORTAL:
            return 0xA032DC;
         case LODESTONE:
            return 0xBAC0C7;
         case HEAD:
            if (mx.headOutlineColor != 0) {
               return mx.headOutlineColor & 0x00FFFFFF;
            }
            return (mx.color & 0x00FFFFFF) != 0 ? (mx.color & 0x00FFFFFF) : 0x55AAFF;
         case DOT:
         case FALLBACK:
         default:
            return (mx.color & 0x00FFFFFF) != 0 ? (mx.color & 0x00FFFFFF) : 0x3A4556;
      }
   }

   @Unique
   private void renderTooltipBox(GuiGraphicsExtractor extractor, PendingTooltip tooltip, int yOffset, float prog) {
      float effectiveProg = Math.clamp(prog * tooltip.markerFade, 0.0F, 1.0F);
      if (effectiveProg < 0.01F) return;

      int curYOffset = Math.round((1.0F - effectiveProg) * 6.0F);
      int boxLeft = Math.round(tooltip.x);
      boolean isTopBar = ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.TOP;
      int boxTop = isTopBar ? tooltip.baseY - yOffset + (6 - curYOffset) : tooltip.baseY + yOffset - tooltip.height - (6 - curYOffset);
      int boxBottom = boxTop + tooltip.height;
      int boxRight = boxLeft + tooltip.width;
      int themeColor = tooltip.themeColor != 0 ? tooltip.themeColor : 0x3A4556;
      int r = (themeColor >> 16) & 0xFF;
      int g = (themeColor >> 8) & 0xFF;
      int b = themeColor & 0xFF;

      int bgAlpha = Math.clamp((long)Math.round(effectiveProg * 220.0F), 0, 255);
      int bgR = Math.clamp((int)(r * 0.16f + 10), 0, 255);
      int bgG = Math.clamp((int)(g * 0.16f + 10), 0, 255);
      int bgB = Math.clamp((int)(b * 0.16f + 10), 0, 255);
      int fillColor = (bgAlpha << 24) | (bgR << 16) | (bgG << 8) | bgB;

      int borderAlpha = Math.clamp((long)Math.round(effectiveProg * 200.0F), 0, 255);
      int brR = Math.clamp((int)(r * 0.65f + 30), 0, 255);
      int brG = Math.clamp((int)(g * 0.65f + 30), 0, 255);
      int brB = Math.clamp((int)(b * 0.65f + 30), 0, 255);
      int borderColor = (borderAlpha << 24) | (brR << 16) | (brG << 8) | brB;

      extractor.fill(boxLeft + 1, boxTop + 1, boxRight - 1, boxBottom - 1, fillColor);
      extractor.fill(boxLeft + 1, boxTop, boxRight - 1, boxTop + 1, borderColor);
      extractor.fill(boxLeft + 1, boxBottom - 1, boxRight - 1, boxBottom, borderColor);
      extractor.fill(boxLeft, boxTop + 1, boxLeft + 1, boxBottom - 1, borderColor);
      extractor.fill(boxRight - 1, boxTop + 1, boxRight, boxBottom - 1, borderColor);

      int alpha = Math.clamp((long)Math.round(effectiveProg * 255.0F), 0, 255);
      int nameColor = (alpha << 24) | 0x00FFFFFF;

      int dtR = Math.clamp((int)(r * 0.30f + 160), 0, 255);
      int dtG = Math.clamp((int)(g * 0.30f + 160), 0, 255);
      int dtB = Math.clamp((int)(b * 0.30f + 160), 0, 255);
      int distColor = (alpha << 24) | (dtR << 16) | (dtG << 8) | dtB;
      int padY = 3;
      int curY = boxTop + padY;
      if (tooltip.name != null && !tooltip.name.isEmpty()) {
         int nWidth = this.minecraft.font.width(tooltip.name);
         int totalHeaderWidth = nWidth + (tooltip.isFavorite ? 10 : 0);
         int startX = Math.round(boxLeft + (tooltip.width - totalHeaderWidth) / 2.0F);
         if (tooltip.isFavorite) {
            int starSize = 8;
            int starX = startX;
            int starY = curY;
            int starColor = (alpha << 24) | 0x00FFFFFF;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, STAR_ACTIVE_SPRITE, starX, starY, starSize, starSize, starColor);
            extractor.text(this.minecraft.font, tooltip.name, startX + 10, curY, nameColor);
         } else {
            extractor.text(this.minecraft.font, tooltip.name, startX, curY, nameColor);
         }
         curY += 11;
      }

      if (tooltip.typeName != null && !tooltip.typeName.isEmpty() && !tooltip.typeName.equalsIgnoreCase(tooltip.name)) {
         int tWidth = this.minecraft.font.width(tooltip.typeName);
         boolean showStarOnType = tooltip.isFavorite && (tooltip.name == null || tooltip.name.isEmpty());
         int totalTypeWidth = tWidth + (showStarOnType ? 10 : 0);
         int startX = Math.round(boxLeft + (tooltip.width - totalTypeWidth) / 2.0F);
         int typeR = Math.clamp((int)(r * 0.50f + 140), 0, 255);
         int typeG = Math.clamp((int)(g * 0.50f + 140), 0, 255);
         int typeB = Math.clamp((int)(b * 0.50f + 140), 0, 255);
         int typeColor = (alpha << 24) | (typeR << 16) | (typeG << 8) | typeB;
         if (showStarOnType) {
            int starSize = 8;
            int starX = startX;
            int starY = curY;
            int starColor = (alpha << 24) | 0x00FFFFFF;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, STAR_ACTIVE_SPRITE, starX, starY, starSize, starSize, starColor);
            extractor.text(this.minecraft.font, tooltip.typeName, startX + 10, curY, typeColor);
         } else {
            extractor.text(this.minecraft.font, tooltip.typeName, startX, curY, typeColor);
         }
         curY += 9;
      }

      if (tooltip.hearts != null) {
         int heartsWidth = (tooltip.hearts.totalHearts - 1) * 8 + 9;
         int hX = Math.round(boxLeft + (tooltip.width - heartsWidth) / 2.0F);
         int heartSpriteColor = alpha << 24 | 16777215;

         for (int i = 0; i < tooltip.hearts.totalHearts; i++) {
            int px = hX + i * 8;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER_SPRITE, px, curY, 9, 9, heartSpriteColor);
            float hp = tooltip.hearts.health - i * 2;
            if (hp >= 1.5F) {
               extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL_SPRITE, px, curY, 9, 9, heartSpriteColor);
            } else if (hp >= 0.5F) {
               extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF_SPRITE, px, curY, 9, 9, heartSpriteColor);
            }
         }

         curY += 11;
      }

      if (tooltip.distance != null && !tooltip.distance.isEmpty()) {
         int dWidth = this.minecraft.font.width(tooltip.distance);
         int dX = Math.round(boxLeft + (tooltip.width - dWidth) / 2.0F);
         extractor.text(this.minecraft.font, tooltip.distance, dX, curY, distColor);
      }
   }

   @Unique
   private static void resolveTooltips(List<PendingTooltip> list, int guiWidth, float gap) {
      if (!list.isEmpty()) {
         list.sort(LocatorBarMixin::locatorTweaks$compareTooltips);
         int n = list.size();

         for (int iter = 0; iter < 25; iter++) {
            for (int i = 0; i < n - 1; i++) {
               PendingTooltip t1 = list.get(i);
               PendingTooltip t2 = list.get(i + 1);
               float w1 = Math.clamp(t1.markerFade, 0.15F, 1.0F);
               float w2 = Math.clamp(t2.markerFade, 0.15F, 1.0F);
               float effectiveGap = gap * Math.min(w1, w2);
               float overlap = t1.x + t1.width + effectiveGap - t2.x;
               if (overlap > 0.0F) {
                  float shift1 = overlap * (w2 / (w1 + w2));
                  float shift2 = overlap * (w1 / (w1 + w2));
                  t1.x -= shift1;
                  t2.x += shift2;
               }
            }
         }

         for (PendingTooltip tooltip : list) {
            float minX = 2.0F;
            float maxX = Math.max(minX, guiWidth - 2.0F - tooltip.width);
            if (tooltip.x < minX) {
               tooltip.x = minX;
            } else if (tooltip.x > maxX) {
               tooltip.x = maxX;
            }
         }

         for (int ix = 0; ix < n - 1; ix++) {
            PendingTooltip t1 = list.get(ix);
            PendingTooltip t2 = list.get(ix + 1);
            float minX2 = t1.x + t1.width + gap;
            if (t2.x < minX2) {
               t2.x = minX2;
            }
         }

         for (int ixx = n - 1; ixx > 0; ixx--) {
            PendingTooltip t2 = list.get(ixx);
            PendingTooltip t1 = list.get(ixx - 1);
            float maxX2 = Math.max(2.0F, guiWidth - 2.0F - t2.width);
            if (t2.x > maxX2) {
               t2.x = maxX2;
            }

            float maxX1 = t2.x - gap - t1.width;
            if (t1.x > maxX1) {
               t1.x = maxX1;
            }
         }

         for (int ixx = 0; ixx < n; ixx++) {
            PendingTooltip tooltipx = list.get(ixx);
            if (tooltipx.x < 2.0F) {
               tooltipx.x = 2.0F;
            }

            if (ixx > 0) {
               PendingTooltip prev = list.get(ixx - 1);
               float minX = prev.x + prev.width + gap;
               if (tooltipx.x < minX) {
                  tooltipx.x = minX;
               }
            }
         }

         PendingTooltip rightmost = list.get(n - 1);
         float maxAllowedRight = guiWidth - 2.0F;
         if (rightmost.x + rightmost.width > maxAllowedRight) {
            float excess = rightmost.x + rightmost.width - maxAllowedRight;

            for (PendingTooltip tooltipxx : list) {
               tooltipxx.x -= excess;
            }

            for (int ixx = 0; ixx < n; ixx++) {
               PendingTooltip tooltipxx = list.get(ixx);
               if (tooltipxx.x < 2.0F) {
                  tooltipxx.x = 2.0F;
               }

               if (ixx > 0) {
                  PendingTooltip prev = list.get(ixx - 1);
                  float minX = prev.x + prev.width + gap;
                  if (tooltipxx.x < minX) {
                     tooltipxx.x = minX;
                  }
               }
            }
         }
      }
   }

   @WrapOperation(
      method = "extractBackground",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
      )
   )
   private void locatorTweaks$wrapLocatorBackground(
      GuiGraphicsExtractor instance, RenderPipeline pipeline, Identifier sprite, int left, int top, int width, int height, Operation<Void> original
   ) {
      int barOpacity = ModConfig.getInstance().barBackgroundOpacity;
      if (barOpacity > 0) {
         int barAlpha = Math.round(barOpacity * 255.0F / 100.0F);
         int barColor = barAlpha << 24 | 16777215;
         if (ModConfig.getInstance().locatorPosition == ModConfig.LocatorPosition.BOTTOM
            && ModConfig.getInstance().barDisplayMode == ModConfig.BarDisplayMode.XP_WITH_MARKERS
            && this.minecraft.player != null
            && this.minecraft.gameMode != null
            && this.minecraft.gameMode.hasExperience()) {
            LocalPlayer player = this.minecraft.player;
            int xpNeeded = player.getXpNeededForNextLevel();
            if (xpNeeded > 0) {
               instance.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, left, top, 182, 5, barColor);
               int progress = (int)(player.experienceProgress * 183.0F);
               if (progress > 0) {
                  instance.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_PROGRESS_SPRITE, 182, 5, 0, 0, left, top, progress, 5, barColor);
               }

               return;
            }
         }

         if (barOpacity < 100) {
            instance.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, left, top, width, height, barColor);
         } else {
            original.call(new Object[]{instance, pipeline, sprite, left, top, width, height});
         }
      }
   }

   @Unique
   private static double locatorTweaks$getZoomifyDivisor() {
      if (!locatorTweaks$zoomifyChecked) {
         locatorTweaks$zoomifyChecked = true;

         try {
            Class<?> zoomifyClass = Class.forName("dev.isxander.zoomify.Zoomify");

            try {
               locatorTweaks$zoomifyMethodFloat = zoomifyClass.getMethod("getZoomDivisor", float.class);
            } catch (Throwable var6) {
               try {
                  locatorTweaks$zoomifyMethodNoArg = zoomifyClass.getMethod("getZoomDivisor");
               } catch (Throwable var5) {
               }
            }
         } catch (ClassNotFoundException var7) {
         } catch (Throwable var8) {
         }
      }

      if (locatorTweaks$zoomifyMethodFloat != null) {
         try {
            if (locatorTweaks$zoomifyMethodFloat.invoke(null, 0.0F) instanceof Number num) {
               return num.doubleValue();
            }
         } catch (Throwable var4) {
         }
      } else if (locatorTweaks$zoomifyMethodNoArg != null) {
         try {
            if (locatorTweaks$zoomifyMethodNoArg.invoke(null) instanceof Number num) {
               return num.doubleValue();
            }
         } catch (Throwable var3) {
         }
      }

      return 1.0;
   }

   @Unique
   private double locatorTweaks$getCurrentMaxAngle() {
      Minecraft mc = this.minecraft;
      if (mc != null && mc.options != null && mc.gameRenderer != null) {
         double zoomDivisor = locatorTweaks$getZoomifyDivisor();
         if (zoomDivisor > 1.001) {
            return Math.max(1.0, 60.0 / zoomDivisor);
         } else {
            try {
               Camera camera = mc.gameRenderer.mainCamera();
               if (camera != null) {
                  float currentFov = camera.getFov();
                  float baseFov = ((Integer)mc.options.fov().get()).intValue();
                  if (currentFov > 0.0F && baseFov > 0.0F && currentFov < baseFov - 0.5F) {
                     double zoomFactor = (double)baseFov / currentFov;
                     if (zoomFactor > 1.001) {
                        return Math.max(1.0, 60.0 / zoomFactor);
                     }
                  }
               }
            } catch (Throwable var9) {
            }

            return 60.0;
         }
      } else {
         return 60.0;
      }
   }

   @ModifyConstant(method = "lambda$extractRenderState$1", constant = @Constant(doubleValue = 60.0))
   private double locatorTweaks$modifyMaxAngle(double original) {
      return this.locatorTweaks$getCurrentMaxAngle();
   }

   @ModifyConstant(method = "lambda$extractRenderState$1", constant = @Constant(doubleValue = -60.0))
   private double locatorTweaks$modifyMinAngle(double original) {
      return -this.locatorTweaks$getCurrentMaxAngle();
   }

   @Inject(method = "lambda$extractRenderState$1", at = @At("HEAD"), cancellable = true)
   private void locatorTweaks$checkPlayerVisibility(
      Entity entity, Level level, PartialTickSupplier partialTickSupplier, GuiGraphicsExtractor extractor, int topY, TrackedWaypoint waypoint, CallbackInfo ci
   ) {
      ModConfig config = ModConfig.getInstance();
      if (config.showOnlyWithCompass && !locatorTweaks$isHoldingCompass(this.minecraft)) {
         ci.cancel();
      } else if (config.barDisplayMode == ModConfig.BarDisplayMode.DISABLED) {
         ci.cancel();
      } else {
         if (waypoint != null) {
            Optional<UUID> playerUuid = waypoint.id().left();
            if (playerUuid.isPresent()) {
               UUID uuid = playerUuid.get();
               if (DeathPointManager.isDeathPoint(uuid)) {
                  if (!DeathPointManager.hasActiveDeathPoint(this.minecraft)) {
                     ci.cancel();
                  }

                  return;
               }

               if (LodestoneManager.isLodestone(uuid)) {
                  if (!ModConfig.getInstance().showLodestonePoints) {
                     ci.cancel();
                  }

                  return;
               }

               if (CustomWaypointManager.isCustomWaypoint(uuid)) {
                  CustomWaypoint cwp = CustomWaypointManager.getActiveWaypoint(uuid);
                  if (cwp == null || !cwp.enabled) {
                     ci.cancel();
                     return;
                  }
                  ModConfig.CustomWaypointDisplayMode cMode = ModConfig.getInstance().customWaypointsDisplayMode;
                  if (cMode == ModConfig.CustomWaypointDisplayMode.DISABLED ||
                      (cMode == ModConfig.CustomWaypointDisplayMode.FAVORITES_ONLY && !cwp.favorite)) {
                     ci.cancel();
                     return;
                  }
                  return;
               }

               if (SpawnPointManager.isSpawnPoint(uuid)) {
                  if (!SpawnPointManager.hasActiveSpawnPoint(this.minecraft)) {
                     ci.cancel();
                  }

                  return;
               }

               if (NetherPortalManager.isPortalPoint(uuid)) {
                  if (!NetherPortalManager.hasActivePortalPoint(this.minecraft)) {
                     ci.cancel();
                  }

                  return;
               }

               if (!config.isPlayerMarkersEnabled()) {
                  ci.cancel();
                  return;
               }

               if (ModConfig.getInstance().maxDistance > 0) {
                  Entity cameraEntity = this.minecraft.getCameraEntity();
                  if (cameraEntity != null) {
                     double distSq = waypoint.distanceSquared(cameraEntity);
                     double maxD = ModConfig.getInstance().maxDistance;
                     if (distSq > maxD * maxD) {
                        ci.cancel();
                        return;
                     }
                  }
               }

               PlayerLocatorConfig pConfig = config.getPlayerConfig(uuid);
               if (pConfig != null && !pConfig.enabled) {
                  ci.cancel();
                  return;
               }

               if (config.displayMode == ModConfig.LocatorDisplayMode.FAVORITES_ONLY && (pConfig == null || !pConfig.favorite)) {
                  ci.cancel();
               }
            }
         }
      }
   }

   @WrapOperation(
      method = "lambda$extractRenderState$1",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/WaypointStyle;sprite(F)Lnet/minecraft/resources/Identifier;")
   )
   private Identifier locatorTweaks$wrapWaypointSprite(WaypointStyle instance, float distance, Operation<Identifier> original) {
      return ModConfig.getInstance().preventDistanceScaling
         ? (Identifier)original.call(new Object[]{instance, 0.0F})
         : (Identifier)original.call(new Object[]{instance, distance});
   }

   @WrapOperation(
      method = "lambda$extractRenderState$1",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
      )
   )
   private void locatorTweaks$wrapArrowSprite(
      GuiGraphicsExtractor instance, RenderPipeline pipeline, Identifier sprite, int left, int top, int width, int height, Operation<Void> original
   ) {
      if (ModConfig.getInstance().showHeightArrows) {
         if (!locatorTweaks$pendingMarkers.isEmpty()) {
            PendingMarker last = locatorTweaks$pendingMarkers.get(locatorTweaks$pendingMarkers.size() - 1);
            last.hasArrow = true;
            last.arrowSprite = sprite;
            last.arrowTop = top;
            last.arrowWidth = width;
            last.arrowHeight = height;
         } else {
            original.call(new Object[]{instance, pipeline, sprite, left, top, width, height});
         }
      }
   }

   @WrapOperation(
      method = "lambda$extractRenderState$1",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
      )
   )
   private void locatorTweaks$renderCustomLocatorMarker(
      GuiGraphicsExtractor instance,
      RenderPipeline pipeline,
      Identifier sprite,
      int x,
      int y,
      int width,
      int height,
      int color,
      Operation<Void> original,
      @Local TrackedWaypoint waypoint
   ) {
      float alphaFactor = Math.clamp(ModConfig.getInstance().markerOpacity / 100.0F, 0.0F, 1.0F);
      if (!(alphaFactor <= 0.0F)) {
         Minecraft mc = this.minecraft;
         boolean isScreenOpen = mc.gui != null && mc.gui.screen() != null;
         boolean shiftDown = !isScreenOpen
            && (
               (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT))
                  || mc.options != null && mc.options.keyShift.isDown()
            );

         boolean isNameActive = switch (ModConfig.getInstance().markerInfoMode) {
            case ALWAYS -> true;
            case HOLD -> shiftDown;
            case DISABLED -> false;
         };

         boolean isDistanceActive = switch (ModConfig.getInstance().distanceDisplayMode) {
            case ALWAYS -> true;
            case HOLD -> shiftDown;
            case DISABLED -> false;
         };

         boolean shouldProcessLabels = waypoint != null && (isNameActive || isDistanceActive || (shiftDown && ModConfig.getInstance().showPlayerHealthOnShift));
         String labelName = null;
         PendingHearts pendingHearts = null;
         String distText = null;
         Optional<UUID> playerUuid = waypoint != null ? waypoint.id().left() : Optional.empty();
         UUID uuid = playerUuid.orElse(null);
         boolean isLodestone = uuid != null && LodestoneManager.isLodestone(uuid);
         boolean isDeathPoint = uuid != null && DeathPointManager.isDeathPoint(uuid);
         boolean isSpawnPoint = uuid != null && SpawnPointManager.isSpawnPoint(uuid);
         boolean isPortalPoint = uuid != null && NetherPortalManager.isPortalPoint(uuid);
         boolean isCustomWaypoint = uuid != null && CustomWaypointManager.isCustomWaypoint(uuid);
         String markerKey;
         if (isCustomWaypoint) {
            CustomWaypoint cwp = CustomWaypointManager.getActiveWaypoint(uuid);
            markerKey = (cwp != null) ? cwp.getMarkerKey() : ("custom_wp_" + uuid.toString());
         } else if (isSpawnPoint) {
            BlockPos p = SpawnPointManager.getPos(uuid);
            markerKey = "spawn_" + (p != null ? p.getX() + "_" + p.getY() + "_" + p.getZ() : "default");
         } else if (isPortalPoint) {
            BlockPos p = NetherPortalManager.getPos(uuid);
            markerKey = "portal_" + (p != null ? p.getX() + "_" + p.getY() + "_" + p.getZ() : "default");
         } else if (isLodestone) {
            BlockPos p = LodestoneManager.getPos(uuid);
            markerKey = "lodestone_" + (p != null ? p.getX() + "_" + p.getY() + "_" + p.getZ() : "default");
         } else if (isDeathPoint) {
            BlockPos p = DeathPointManager.getPos();
            markerKey = "death_" + (p != null ? p.getX() + "_" + p.getY() + "_" + p.getZ() : "default");
         } else if (uuid != null) {
            markerKey = locatorTweaks$playerKeyCache.computeIfAbsent(uuid, id -> "player_" + id);
         } else if (waypoint != null) {
            markerKey = "waypoint_" + waypoint.id().toString();
         } else {
            markerKey = sprite.toString();
         }

         if (shouldProcessLabels) {
            if (isNameActive) {
               if (isCustomWaypoint) {
                  CustomWaypoint cwp = CustomWaypointManager.getActiveWaypoint(uuid);
                  if (cwp != null) {
                     labelName = cwp.name;
                  }
               } else if (isLodestone) {
                  labelName = LodestoneManager.getName(uuid);
               } else if (isSpawnPoint) {
                  labelName = SpawnPointManager.getName(uuid);
               } else if (isPortalPoint) {
                  labelName = NetherPortalManager.getName(uuid);
               } else if (isDeathPoint) {
                  labelName = DeathPointManager.getName();
               } else if (uuid != null) {
                  if (mc.getConnection() != null) {
                     PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(uuid);
                     if (playerInfo != null && playerInfo.getProfile() != null) {
                        labelName = playerInfo.getProfile().name();
                     }
                  }

                  if (labelName == null) {
                     PlayerLocatorConfig cfg = ModConfig.getInstance().getPlayerConfig(uuid);
                     if (cfg != null && cfg.lastKnownName != null && !cfg.lastKnownName.isEmpty()) {
                        labelName = cfg.lastKnownName;
                     }
                  }
               }
            }

            if (!isLodestone && !isDeathPoint && !isSpawnPoint && !isPortalPoint && uuid != null && ModConfig.getInstance().showPlayerHealthOnShift) {
               Player targetPlayer = mc.level != null ? mc.level.getPlayerByUUID(uuid) : null;
               if (targetPlayer != null) {
                  float hp = targetPlayer.getHealth();
                  float maxHp = targetPlayer.getMaxHealth();
                  int totalHearts = Math.min(10, (int)Math.ceil(maxHp / 2.0F));
                  if (totalHearts <= 0) {
                     totalHearts = 10;
                  }

                  pendingHearts = new PendingHearts(totalHearts, hp);
               }
            }

            if (isDistanceActive) {
               Entity cameraEntity = mc.getCameraEntity();
               if (cameraEntity != null) {
                  double dist = -1.0;
                  Double targetY = null;
                  if (isCustomWaypoint) {
                     CustomWaypoint cwp = CustomWaypointManager.getActiveWaypoint(uuid);
                     if (cwp != null) {
                        dist = Math.sqrt(cameraEntity.distanceToSqr(cwp.x + 0.5, cwp.y + 0.5, cwp.z + 0.5));
                        targetY = (double)cwp.y;
                     }
                  } else if (isLodestone) {
                     BlockPos lPos = LodestoneManager.getPos(uuid);
                     if (lPos != null) {
                        dist = Math.sqrt(cameraEntity.distanceToSqr(lPos.getX() + 0.5, lPos.getY() + 0.5, lPos.getZ() + 0.5));
                        targetY = (double)lPos.getY();
                     }
                  } else if (isSpawnPoint) {
                     BlockPos sPos = SpawnPointManager.getPos(uuid);
                     if (sPos != null) {
                        dist = Math.sqrt(cameraEntity.distanceToSqr(sPos.getX() + 0.5, sPos.getY() + 0.5, sPos.getZ() + 0.5));
                        targetY = (double)sPos.getY();
                     }
                  } else if (isPortalPoint) {
                     BlockPos pPos = NetherPortalManager.getPos(uuid);
                     if (pPos != null) {
                        dist = Math.sqrt(cameraEntity.distanceToSqr(pPos.getX() + 0.5, pPos.getY() + 0.5, pPos.getZ() + 0.5));
                        targetY = (double)pPos.getY();
                     }
                  } else if (isDeathPoint) {
                     BlockPos dPos = DeathPointManager.getPos();
                     if (dPos != null) {
                        dist = Math.sqrt(cameraEntity.distanceToSqr(dPos.getX() + 0.5, dPos.getY() + 0.5, dPos.getZ() + 0.5));
                        targetY = (double)dPos.getY();
                     }
                  } else if (uuid != null && mc.level != null) {
                     Player targetPlayer = mc.level.getPlayerByUUID(uuid);
                     if (targetPlayer != null) {
                        dist = cameraEntity.distanceTo(targetPlayer);
                        targetY = targetPlayer.getY();
                     }
                  }

                  if (dist < 0.0) {
                     double distSq = waypoint.distanceSquared(cameraEntity);
                     if (distSq >= 0.0) {
                        dist = Math.sqrt(distSq);
                     }
                  }

                  if (targetY == null && waypoint instanceof Vec3iWaypointAccessor vecAccessor) {
                     Vec3i vec = vecAccessor.getVector();
                     if (vec != null) {
                        targetY = (double)vec.getY();
                     }
                  }

                  if (Double.isInfinite(dist)) {
                     distText = Component.translatable("locator-tweaks.distance.far").getString();
                  } else if (dist >= 0.0 && !Double.isNaN(dist)) {
                     String mSuffix = Component.translatable("locator-tweaks.distance.meters").getString();
                     String kmSuffix = Component.translatable("locator-tweaks.distance.kilometers").getString();
                     distText = dist < 1000.0 ? Math.round(dist) + mSuffix : String.format(Locale.ROOT, "%.1f%s", dist / 1000.0, kmSuffix);
                     if (targetY != null) {
                        int deltaY = (int)Math.round(targetY - cameraEntity.getY());
                        distText = distText + (deltaY > 0 ? " (+" + deltaY + ")" : (deltaY < 0 ? " (" + deltaY + ")" : " (0)"));
                     }
                  }
               }
            }
         }

         if (waypoint == null || !playerUuid.isPresent()) {
            locatorTweaks$pendingMarkers.add(
               new PendingMarker(
                  markerKey, waypoint, x, y, 9, 9, color, sprite, MarkerType.FALLBACK, 0, null, 0, labelName, pendingHearts, distText
               )
            );
         } else if (isDeathPoint) {
            Entity cameraEntity = this.minecraft.getCameraEntity();
            double distSq = cameraEntity != null ? waypoint.distanceSquared(cameraEntity) : 0.0;
            int tier = locatorTweaks$getDistanceTier(Math.sqrt(distSq));
            PendingMarker pm = new PendingMarker(
               markerKey, waypoint, x, y, 9, 9, color, sprite, MarkerType.DEATH, tier, null, 0, labelName, pendingHearts, distText
            );
            if (DeathPointManager.isFavorite()) pm.isFavorite = true;
            locatorTweaks$pendingMarkers.add(pm);
         } else if (isLodestone) {
            Entity cameraEntity = this.minecraft.getCameraEntity();
            double distSq = cameraEntity != null ? waypoint.distanceSquared(cameraEntity) : 0.0;
            int tier = locatorTweaks$getDistanceTier(Math.sqrt(distSq));
            PendingMarker pm = new PendingMarker(
               markerKey, waypoint, x, y, 9, 9, color, sprite, MarkerType.LODESTONE, tier, null, 0, labelName, pendingHearts, distText
            );
            if (LodestoneManager.isFavorite(uuid)) pm.isFavorite = true;
            locatorTweaks$pendingMarkers.add(pm);
         } else if (isSpawnPoint) {
            Entity cameraEntity = this.minecraft.getCameraEntity();
            double distSq = cameraEntity != null ? waypoint.distanceSquared(cameraEntity) : 0.0;
            int tier = locatorTweaks$getDistanceTier(Math.sqrt(distSq));
            PendingMarker pm = new PendingMarker(
               markerKey, waypoint, x, y, 9, 9, color, sprite, MarkerType.SPAWN, tier, null, 0, labelName, pendingHearts, distText
            );
            if (SpawnPointManager.isFavorite(uuid)) pm.isFavorite = true;
            locatorTweaks$pendingMarkers.add(pm);
         } else if (isPortalPoint) {
            Entity cameraEntity = this.minecraft.getCameraEntity();
            double distSq = cameraEntity != null ? waypoint.distanceSquared(cameraEntity) : 0.0;
            int tier = locatorTweaks$getDistanceTier(Math.sqrt(distSq));
            PendingMarker pm = new PendingMarker(
               markerKey, waypoint, x, y, 9, 9, color, sprite, MarkerType.PORTAL, tier, null, 0, labelName, pendingHearts, distText
            );
            if (NetherPortalManager.isFavorite(uuid)) pm.isFavorite = true;
            locatorTweaks$pendingMarkers.add(pm);
         } else if (isCustomWaypoint) {
            Entity cameraEntity = this.minecraft.getCameraEntity();
            double distSq = cameraEntity != null ? waypoint.distanceSquared(cameraEntity) : 0.0;
            int tier = locatorTweaks$getDistanceTier(Math.sqrt(distSq));
            CustomWaypoint cwp = CustomWaypointManager.getActiveWaypoint(uuid);
            if (cwp == null && this.minecraft.player != null) {
               String worldKey = WorldKeyUtil.getCurrentWorldKey(this.minecraft);
               cwp = ModConfig.getInstance().getCustomWaypoint(worldKey, uuid);
            }
            int wpColor = (cwp != null ? cwp.color : color) & 0x00FFFFFF;
            PendingMarker pm = new PendingMarker(markerKey, waypoint, x, y, 9, 9, wpColor, WAYPOINT_PIN_SPRITES[tier], MarkerType.CUSTOM_WAYPOINT, tier, null, 0, labelName, pendingHearts, distText);
            if (cwp != null && cwp.favorite) pm.isFavorite = true;
            locatorTweaks$pendingMarkers.add(pm);
         } else {
            PlayerLocatorConfig config = ModConfig.getInstance().getPlayerConfig(uuid);
            boolean useHead = config != null && config.iconType != PlayerLocatorConfig.IconType.DEFAULT
               ? config.iconType == PlayerLocatorConfig.IconType.HEAD
               : ModConfig.getInstance().isPlayerHeadsEnabled();
            int finalColor = config != null && config.useCustomColor && !useHead ? config.customColor : color;
            float scale = config != null ? config.scale : 1.0F;
            if (useHead) {
               int headSize = Math.max(4, Math.round(8.0F * scale));
               Minecraft mcInstance = Minecraft.getInstance();
               ClientPacketListener connection = mcInstance.getConnection();
               if (connection != null) {
                  PlayerInfo playerInfo = connection.getPlayerInfo(uuid);
                  if (playerInfo != null) {
                     PlayerSkin skin = playerInfo.getSkin();
                     if (skin != null) {
                        int outlineColor = 0;
                        if (ModConfig.getInstance().showHeadOutline) {
                           outlineColor = config != null && config.useCustomColor ? config.customColor : PlayerLocatorConfig.getVanillaMarkerColor(uuid);
                        }

                        PendingMarker pm = new PendingMarker(
                           markerKey,
                           waypoint,
                           x,
                           y,
                           headSize,
                           headSize,
                           finalColor,
                           sprite,
                           MarkerType.HEAD,
                           0,
                           skin,
                           outlineColor,
                           labelName,
                           pendingHearts,
                           distText
                        );
                        if (config != null && config.favorite) pm.isFavorite = true;
                        locatorTweaks$pendingMarkers.add(pm);
                        return;
                     }
                  }
               }
            }

            int dotSize = Math.max(4, Math.round(9.0F * scale));
            PendingMarker pm = new PendingMarker(
               markerKey,
               waypoint,
               x,
               y,
               dotSize,
               dotSize,
               finalColor,
               sprite,
               MarkerType.DOT,
               0,
               null,
               0,
               labelName,
               pendingHearts,
               distText
            );
            if (config != null && config.favorite) pm.isFavorite = true;
            locatorTweaks$pendingMarkers.add(pm);
         }
      }
   }


   @Unique
   private static float locatorTweaks$getMarkerEdgeFade(float markerCenterX, double centerX) {
      double absOffset = Math.abs(markerCenterX - centerX);
      double maxOffset = 173.0 / 2.0;
      double fadeMargin = 14.0;
      double fadeStart = maxOffset - fadeMargin;
      if (absOffset > fadeStart) {
         return (float) Math.clamp((maxOffset - absOffset) / fadeMargin, 0.0, 1.0);
      }
      return 1.0F;
   }

   @Unique
   private void locatorTweaks$renderCardinalDirections(GuiGraphicsExtractor extractor, Minecraft mc) {
      if (!ModConfig.getInstance().showCardinalDirections) {
         return;
      }
      if (mc.gameRenderer == null) {
         return;
      }
      Camera camera = mc.gameRenderer.mainCamera();
      if (camera == null) {
         return;
      }

      int guiWidth = extractor.guiWidth();
      double centerX = guiWidth / 2.0;
      int barTop = this.top(mc.getWindow());
      int letterY = barTop - 2;

      float cameraYaw = camera.yRot();
      double maxAngle = this.locatorTweaks$getCurrentMaxAngle();
      float configOpacity = Math.clamp(ModConfig.getInstance().markerOpacity / 100.0F, 0.0F, 1.0F);
      if (configOpacity <= 0.01F) {
         return;
      }

      String[] directions = {"S", "W", "N", "E"};
      float[] yaws = {0.0F, 90.0F, 180.0F, -90.0F};

      for (int i = 0; i < 4; i++) {
         String dir = directions[i];
         float targetYaw = yaws[i];
         double diff = Mth.degreesDifference(cameraYaw, targetYaw);
         if (Math.abs(diff) > maxAngle) {
            continue;
         }

         double offset = diff * (173.0 / 2.0) / maxAngle;
         double absOffset = Math.abs(offset);
         double maxOffset = 173.0 / 2.0;
         if (absOffset > maxOffset) {
            continue;
         }

         float edgeFade = 1.0F;
         double fadeMargin = 14.0;
         double fadeStart = maxOffset - fadeMargin;
         if (absOffset > fadeStart) {
            edgeFade = (float) Math.clamp((maxOffset - absOffset) / fadeMargin, 0.0, 1.0);
         }
         if (edgeFade <= 0.02F) {
            continue;
         }

         float totalFade = configOpacity * edgeFade;
         int alpha = Math.clamp(Math.round(totalFade * 185.0F), 0, 255);
         if (alpha < 4) {
            continue;
         }

         int rgb = "N".equals(dir) ? 0x00FFF1B5 : 0x00E5E5E5;
         int color = (alpha << 24) | rgb;

         float targetX = (float) (centerX + offset);
         int textWidth = mc.font.width(dir);
         int letterX = Math.round(targetX - textWidth / 2.0F);

         int outlineAlpha = Math.clamp(Math.round(totalFade * 210.0F), 0, 255);
         if (outlineAlpha >= 4) {
            int outlineColor = (outlineAlpha << 24) | 0x000000;
            extractor.text(mc.font, dir, letterX - 1, letterY, outlineColor, false);
            extractor.text(mc.font, dir, letterX + 1, letterY, outlineColor, false);
            extractor.text(mc.font, dir, letterX, letterY - 1, outlineColor, false);
            extractor.text(mc.font, dir, letterX, letterY + 1, outlineColor, false);
            extractor.text(mc.font, dir, letterX + 1, letterY + 1, outlineColor, false);
         }

         extractor.text(mc.font, dir, letterX, letterY, color, false);
      }
   }
}