package com.locatortweaks.config;

import com.locatortweaks.client.LocatorTweaksKeybinds;
import com.locatortweaks.config.ModConfig.*;
import com.locatortweaks.gui.PlayerListScreen;
import com.locatortweaks.gui.WaypointListScreen;
import com.locatortweaks.gui.yacl.KeyBindingController;
import com.locatortweaks.util.WorldKeyUtil;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModConfigScreen {
    public static Screen createScreen(Screen parent) {
        LocatorTweaksKeybinds.syncToConfig();

        ModConfig defaults = new ModConfig();
        ModConfig current = ModConfig.getInstance();

        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        boolean hasServer = connection != null;

        boolean inGame = client.player != null && client.level != null;

        ConfigCategory.Builder customWaypointsCategoryBuilder = ConfigCategory.createBuilder()
            .name(Component.translatable("locator-tweaks.config.category.custom_waypoints"));

        customWaypointsCategoryBuilder.group(OptionGroup.createBuilder()
            .name(Component.translatable("locator-tweaks.config.group.custom_waypoints_settings"))
            .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.custom_waypoints_settings.desc")))
            .option(Option.<CustomWaypointDisplayMode>createBuilder()
                .name(Component.translatable("locator-tweaks.config.option.custom_waypoints_display_mode"))
                .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.custom_waypoints_display_mode.desc")))
                .binding(
                    defaults.customWaypointsDisplayMode,
                    () -> current.customWaypointsDisplayMode,
                    val -> current.customWaypointsDisplayMode = val
                )
                .controller(opt -> EnumControllerBuilder.create(opt)
                    .enumClass(CustomWaypointDisplayMode.class)
                    .formatValue(CustomWaypointDisplayMode::getDisplayName)
                )
                .build()
            )
            .build()
        );

        if (!inGame) {
            customWaypointsCategoryBuilder.group(OptionGroup.createBuilder()
                .name(Component.translatable("locator-tweaks.config.group.world_waypoints_header"))
                .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.world_waypoints_header.desc")))
                .option(LabelOption.createBuilder()
                    .line(Component.translatable("locator-tweaks.config.waypoints.not_in_game"))
                    .build()
                )
                .build()
            );
        } else {
            String worldKey = WorldKeyUtil.getCurrentWorldKey(client);
            List<CustomWaypoint> worldWaypoints = current.getCustomWaypoints(worldKey);
            int count = worldWaypoints.size();

            customWaypointsCategoryBuilder.group(OptionGroup.createBuilder()
                .name(Component.translatable("locator-tweaks.config.group.world_waypoints", count))
                .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.world_waypoints.desc")))
                .option(ButtonOption.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.open_waypoints_manager"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.open_waypoints_manager.desc")))
                    .action((yaclScreen, buttonOption) -> client.setScreenAndShow(new WaypointListScreen(yaclScreen)))
                    .build()
                )
                .build()
            );
        }

        ConfigCategory.Builder playersCategoryBuilder = ConfigCategory.createBuilder()
            .name(Component.translatable("locator-tweaks.config.category.players"));

        if (!hasServer) {
            playersCategoryBuilder.option(LabelOption.createBuilder()
                .line(Component.translatable("locator-tweaks.config.players.not_in_game"))
                .build()
            );
        } else {
            UUID localPlayerUuid = client.player != null ? client.player.getUUID() : null;
            List<PlayerInfo> players = new ArrayList<>(connection.getOnlinePlayers());
            players.removeIf(p -> p.getProfile() == null || p.getProfile().name() == null ||
                (localPlayerUuid != null && localPlayerUuid.equals(p.getProfile().id())));

            int onlineCount = players.size();

            playersCategoryBuilder.group(OptionGroup.createBuilder()
                .name(Component.translatable("locator-tweaks.config.group.server_players", onlineCount))
                .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.server_players.desc")))
                .option(ButtonOption.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.open_player_manager"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.open_player_manager.desc")))
                    .action((yaclScreen, buttonOption) -> client.setScreenAndShow(new PlayerListScreen(yaclScreen)))
                    .build()
                )
                .build()
            );
        }

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("locator-tweaks.config.title"))
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("locator-tweaks.config.category.locator_bar"))
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.bar_general"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.bar_general.desc")))
                    .option(Option.<BarDisplayMode>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.bar_display_mode"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.bar_display_mode.desc")))
                        .binding(
                            defaults.barDisplayMode,
                            () -> current.barDisplayMode,
                            val -> current.barDisplayMode = val
                        )
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(BarDisplayMode.class)
                            .formatValue(BarDisplayMode::getDisplayName)
                        )
                        .build()
                    )
                    .option(Option.<LocatorPosition>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.locator_position"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.locator_position.desc")))
                        .binding(
                            defaults.locatorPosition,
                            () -> current.locatorPosition,
                            val -> current.locatorPosition = val
                        )
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(LocatorPosition.class)
                        )
                        .build()
                    )
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_only_with_compass"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_only_with_compass.desc")))
                        .binding(
                            defaults.showOnlyWithCompass,
                            () -> current.showOnlyWithCompass,
                            val -> current.showOnlyWithCompass = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_height_arrows"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_height_arrows.desc")))
                        .binding(
                            defaults.showHeightArrows,
                            () -> current.showHeightArrows,
                            val -> current.showHeightArrows = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_cardinal_directions"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_cardinal_directions.desc")))
                        .binding(
                            defaults.showCardinalDirections,
                            () -> current.showCardinalDirections,
                            val -> current.showCardinalDirections = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.bar_appearance"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.bar_appearance.desc")))
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.bar_background_opacity"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.bar_background_opacity.desc")))
                        .binding(
                            defaults.barBackgroundOpacity,
                            () -> current.barBackgroundOpacity,
                            val -> current.barBackgroundOpacity = val
                        )
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .range(0, 100)
                            .step(5)
                            .formatValue(v -> Component.literal(v + "%"))
                        )
                        .build()
                    )
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.marker_opacity"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.marker_opacity.desc")))
                        .binding(
                            defaults.markerOpacity,
                            () -> current.markerOpacity,
                            val -> current.markerOpacity = val
                        )
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                            .range(10, 100)
                            .step(5)
                            .formatValue(v -> Component.literal(v + "%"))
                        )
                        .build()
                    )
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.prevent_distance_scaling"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.prevent_distance_scaling.desc")))
                        .binding(
                            defaults.preventDistanceScaling,
                            () -> current.preventDistanceScaling,
                            val -> current.preventDistanceScaling = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.bar_shift_info"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.bar_shift_info.desc")))
                    .option(Option.<InfoDisplayMode>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.marker_info_mode"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.marker_info_mode.desc")))
                        .binding(
                            defaults.markerInfoMode,
                            () -> current.markerInfoMode,
                            val -> {
                                current.markerInfoMode = val;
                                current.showWaypointNameOnShift = (val != InfoDisplayMode.DISABLED);
                            }
                        )
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(InfoDisplayMode.class)
                            .formatValue(InfoDisplayMode::getDisplayName)
                        )
                        .build()
                    )
                    .option(Option.<InfoDisplayMode>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.distance_display_mode"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.distance_display_mode.desc")))
                        .binding(
                            defaults.distanceDisplayMode,
                            () -> current.distanceDisplayMode,
                            val -> {
                                current.distanceDisplayMode = val;
                                current.showDistanceOnShift = (val != InfoDisplayMode.DISABLED);
                            }
                        )
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(InfoDisplayMode.class)
                            .formatValue(InfoDisplayMode::getDisplayName)
                        )
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("locator-tweaks.config.category.players_markers"))
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.players_general"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.players_general.desc")))
                    .option(Option.<PlayerMarkerMode>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.player_marker_mode"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.player_marker_mode.desc")))
                        .binding(
                            defaults.playerMarkerMode,
                            () -> current.playerMarkerMode,
                            val -> current.playerMarkerMode = val
                        )
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(PlayerMarkerMode.class)
                            .formatValue(PlayerMarkerMode::getDisplayName)
                        )
                        .build()
                    )
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_head_outline"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_head_outline.desc")))
                        .binding(
                            defaults.showHeadOutline,
                            () -> current.showHeadOutline,
                            val -> current.showHeadOutline = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .option(Option.<LocatorDisplayMode>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.display_mode"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.display_mode.desc")))
                        .binding(
                            defaults.displayMode,
                            () -> current.displayMode,
                            val -> current.displayMode = val
                        )
                        .controller(opt -> EnumControllerBuilder.create(opt)
                            .enumClass(LocatorDisplayMode.class)
                            .formatValue(LocatorDisplayMode::getDisplayName)
                        )
                        .build()
                    )
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.max_distance"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.max_distance.desc")))
                        .binding(
                            defaults.maxDistance,
                            () -> current.maxDistance,
                            val -> current.maxDistance = Math.max(0, val)
                        )
                        .controller(opt -> IntegerFieldControllerBuilder.create(opt)
                            .min(0)
                            .max(1000000)
                            .formatValue(v -> v <= 0
                                ? Component.translatable("locator-tweaks.config.max_distance.unlimited")
                                : Component.translatable("locator-tweaks.config.max_distance.blocks", v)
                            )
                        )
                        .build()
                    )
                    .build()
                )
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.player_shift_info"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.player_shift_info.desc")))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_player_health_on_shift"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_player_health_on_shift.desc")))
                        .binding(
                            defaults.showPlayerHealthOnShift,
                            () -> current.showPlayerHealthOnShift,
                            val -> current.showPlayerHealthOnShift = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .category(customWaypointsCategoryBuilder.build())
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("locator-tweaks.config.category.waypoints"))
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.waypoint_spawn"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.waypoint_spawn.desc")))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_spawn_point"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_spawn_point.desc")))
                        .binding(
                            defaults.showSpawnPoint,
                            () -> current.showSpawnPoint,
                            val -> current.showSpawnPoint = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.waypoint_nether_portal"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.waypoint_nether_portal.desc")))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_nether_portal_point"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_nether_portal_point.desc")))
                        .binding(
                            defaults.showNetherPortalPoint,
                            () -> current.showNetherPortalPoint,
                            val -> current.showNetherPortalPoint = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.waypoint_death"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.waypoint_death.desc")))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_death_point"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_death_point.desc")))
                        .binding(
                            defaults.showDeathPoint,
                            () -> current.showDeathPoint,
                            val -> current.showDeathPoint = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.waypoint_lodestone"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.waypoint_lodestone.desc")))
                    .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.show_lodestone_points"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.show_lodestone_points.desc")))
                        .binding(
                            defaults.showLodestonePoints,
                            () -> current.showLodestonePoints,
                            val -> current.showLodestonePoints = val
                        )
                        .controller(TickBoxControllerBuilder::create)
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .category(ConfigCategory.createBuilder()
                .name(Component.translatable("locator-tweaks.config.category.keybinds"))
                .group(OptionGroup.createBuilder()
                    .name(Component.translatable("locator-tweaks.config.group.keybinds"))
                    .description(OptionDescription.of(Component.translatable("locator-tweaks.config.group.keybinds.desc")))
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.key_toggle_locator"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.key_toggle_locator.desc")))
                        .binding(
                            defaults.toggleLocatorKey,
                            () -> current.toggleLocatorKey,
                            val -> LocatorTweaksKeybinds.updateToggleLocatorKey(val)
                        )
                        .customController(KeyBindingController::new)
                        .build()
                    )
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.key_open_players"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.key_open_players.desc")))
                        .binding(
                            defaults.openPlayersKey,
                            () -> current.openPlayersKey,
                            val -> LocatorTweaksKeybinds.updateOpenPlayersKey(val)
                        )
                        .customController(KeyBindingController::new)
                        .build()
                    )
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.key_create_waypoint"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.key_create_waypoint.desc")))
                        .binding(
                            defaults.createWaypointKey,
                            () -> current.createWaypointKey,
                            val -> LocatorTweaksKeybinds.updateCreateWaypointKey(val)
                        )
                        .customController(KeyBindingController::new)
                        .build()
                    )
                    .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("locator-tweaks.config.option.key_open_waypoints"))
                        .description(OptionDescription.of(Component.translatable("locator-tweaks.config.option.key_open_waypoints.desc")))
                        .binding(
                            defaults.openWaypointsKey,
                            () -> current.openWaypointsKey,
                            val -> LocatorTweaksKeybinds.updateOpenWaypointsKey(val)
                        )
                        .customController(KeyBindingController::new)
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .category(playersCategoryBuilder.build())
            .save(ModConfig::save)
            .build()
            .generateScreen(parent);
    }

}
