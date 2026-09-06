package com.locatortweaks.gui;

import com.locatortweaks.config.CustomWaypoint;
import com.locatortweaks.config.ModConfig;
import com.locatortweaks.util.CustomWaypointManager;
import com.locatortweaks.util.WorldKeyUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class WaypointEditScreen extends Screen {
    private static final Identifier PIN_SPRITE = Identifier.fromNamespaceAndPath("locator-tweaks", "hud/waypoint_pin_0");

    private final Screen parentScreen;
    private final CustomWaypoint originalWaypoint;
    private final boolean isNew;

    private EditBox nameBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private int selectedColor;
    private static final int[] PRESET_COLORS = new int[]{
        0xFF3333, // Red
        0xFF8800, // Orange
        0xFFDD00, // Yellow
        0x33CC33, // Green
        0x00D0FF, // Cyan
        0x3366FF, // Blue
        0x9933FF, // Purple
        0xFF33AA, // Pink
        0xFFFFFF, // White
        0x888888  // Gray
    };

    public WaypointEditScreen(Screen parentScreen, CustomWaypoint waypoint) {
        super(Component.translatable(waypoint == null ? "locator-tweaks.gui.waypoint_edit.title_create" : "locator-tweaks.gui.waypoint_edit.title_edit"));
        this.parentScreen = parentScreen;
        this.originalWaypoint = waypoint;
        this.isNew = (waypoint == null);

        if (waypoint != null) {
            this.selectedColor = waypoint.color;
        } else {
            this.selectedColor = 0x00D0FF; // default cyan
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int startY = 46;

        Minecraft mc = Minecraft.getInstance();
        int defaultX = 0;
        int defaultY = 64;
        int defaultZ = 0;
        if (mc.player != null) {
            defaultX = mc.player.getBlockX();
            defaultY = mc.player.getBlockY();
            defaultZ = mc.player.getBlockZ();
        }

        String initialName = originalWaypoint != null ? originalWaypoint.name : "";
        String initialX = originalWaypoint != null ? String.valueOf(originalWaypoint.x) : String.valueOf(defaultX);
        String initialY = originalWaypoint != null ? String.valueOf(originalWaypoint.y) : String.valueOf(defaultY);
        String initialZ = originalWaypoint != null ? String.valueOf(originalWaypoint.z) : String.valueOf(defaultZ);

        // Name input
        this.nameBox = new EditBox(this.font, centerX - 100, startY + 12, 200, 20, Component.translatable("locator-tweaks.gui.waypoint_edit.name_label"));
        this.nameBox.setValue(initialName);
        this.nameBox.setHint(Component.translatable("locator-tweaks.gui.waypoint_edit.name_hint"));
        this.addRenderableWidget(this.nameBox);

        // Coordinate inputs
        int coordWidth = 62;
        int coordGap = 7;
        this.xBox = new EditBox(this.font, centerX - 100, startY + 54, coordWidth, 20, Component.literal("X"));
        this.xBox.setValue(initialX);
        this.addRenderableWidget(this.xBox);

        this.yBox = new EditBox(this.font, centerX - 100 + coordWidth + coordGap, startY + 54, coordWidth, 20, Component.literal("Y"));
        this.yBox.setValue(initialY);
        this.addRenderableWidget(this.yBox);

        this.zBox = new EditBox(this.font, centerX - 100 + (coordWidth + coordGap) * 2, startY + 54, coordWidth, 20, Component.literal("Z"));
        this.zBox.setValue(initialZ);
        this.addRenderableWidget(this.zBox);

        // Save & Cancel buttons
        int btnWidth = 98;
        Button saveBtn = Button.builder(Component.translatable("locator-tweaks.gui.waypoint_edit.save"), b -> this.onSave())
            .bounds(centerX - 100, this.height - 32, btnWidth, 20)
            .build();
        this.addRenderableWidget(saveBtn);

        Button cancelBtn = Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
            .bounds(centerX + 2, this.height - 32, btnWidth, 20)
            .build();
        this.addRenderableWidget(cancelBtn);
    }

    private void onSave() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        String name = this.nameBox.getValue().trim();
        if (name.isEmpty()) {
            name = "Waypoint";
        }

        int x = 0, y = 64, z = 0;
        try {
            x = Integer.parseInt(this.xBox.getValue().trim());
        } catch (NumberFormatException ignored) {}
        try {
            y = Integer.parseInt(this.yBox.getValue().trim());
        } catch (NumberFormatException ignored) {}
        try {
            z = Integer.parseInt(this.zBox.getValue().trim());
        } catch (NumberFormatException ignored) {}

        String worldKey = WorldKeyUtil.getCurrentWorldKey(mc);
        String dim = (mc.level != null) ? mc.level.dimension().identifier().toString() : "minecraft:overworld";

        if (isNew) {
            UUID id = UUID.randomUUID();
            CustomWaypoint wp = new CustomWaypoint(id, name, x, y, z, dim, this.selectedColor, true, false);
            ModConfig.getInstance().addCustomWaypoint(worldKey, wp);
        } else {
            originalWaypoint.name = name;
            originalWaypoint.x = x;
            originalWaypoint.y = y;
            originalWaypoint.z = z;
            originalWaypoint.color = this.selectedColor;
            ModConfig.getInstance().updateCustomWaypoint(worldKey, originalWaypoint);
        }

        CustomWaypointManager.forceSync(mc);

        if (this.minecraft != null) {
            Screen backTo = this.parentScreen;
            if (backTo instanceof WaypointListScreen listScreen) {
                this.minecraft.setScreenAndShow(new WaypointListScreen(listScreen.getParentScreen(), isNew ? dim : listScreen.getSelectedDimension()));
            } else {
                this.minecraft.setScreenAndShow(new WaypointListScreen(this.parentScreen));
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int centerX = this.width / 2;
        int colorY = 46 + 96;
        int totalWidth = PRESET_COLORS.length * 18 + (PRESET_COLORS.length - 1) * 3;
        int startX = centerX - totalWidth / 2;

        if (mouseY >= colorY && mouseY <= colorY + 18) {
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int px = startX + i * 21;
                if (mouseX >= px && mouseX <= px + 18) {
                    this.selectedColor = PRESET_COLORS[i];
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        // Title
        extractor.text(this.font, this.title, centerX - this.font.width(this.title) / 2, 14, 0xFFFFFFFF);

        int startY = 46;

        // Name Label
        Component nameLabel = Component.translatable("locator-tweaks.gui.waypoint_edit.name_label");
        extractor.text(this.font, nameLabel, centerX - 100, startY + 1, 0xFFAAAAAA);

        // Coordinates Label
        Component coordLabel = Component.translatable("locator-tweaks.gui.waypoint_edit.coords_label");
        extractor.text(this.font, coordLabel, centerX - 100, startY + 42, 0xFFAAAAAA);

        // Color selection palette
        int colorY = startY + 84;
        Component colorLabel = Component.translatable("locator-tweaks.gui.waypoint_edit.color_label");
        extractor.text(this.font, colorLabel, centerX - 100, colorY, 0xFFAAAAAA);

        int totalWidth = PRESET_COLORS.length * 18 + (PRESET_COLORS.length - 1) * 3;
        int startX = centerX - totalWidth / 2;
        int palY = colorY + 12;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int px = startX + i * 21;
            int col = PRESET_COLORS[i];
            boolean selected = (col == this.selectedColor);
            boolean hovered = (mouseX >= px && mouseX <= px + 18 && mouseY >= palY && mouseY <= palY + 18);

            int outlineCol = selected ? 0xFFFFFFFF : (hovered ? 0xFFAAAAAA : 0xFF333333);
            extractor.fill(px - 1, palY - 1, px + 19, palY + 19, outlineCol);
            extractor.fill(px, palY, px + 18, palY + 18, 0xFF000000 | col);
        }

        // Live preview of the pin with selected color
        int previewY = palY + 28;
        Component previewLabel = Component.translatable("locator-tweaks.gui.waypoint_edit.preview_label");
        extractor.text(this.font, previewLabel, centerX - this.font.width(previewLabel) / 2, previewY, 0xFFAAAAAA);

        int pinBoxSize = 26;
        int pinBoxX = centerX - pinBoxSize / 2;
        int pinBoxY = previewY + 12;

        extractor.fill(pinBoxX, pinBoxY, pinBoxX + pinBoxSize, pinBoxY + pinBoxSize, 0x55000000);
        extractor.fill(pinBoxX, pinBoxY, pinBoxX + pinBoxSize, pinBoxY + 1, 0x44FFFFFF);
        extractor.fill(pinBoxX, pinBoxY + pinBoxSize - 1, pinBoxX + pinBoxSize, pinBoxY + pinBoxSize, 0x44FFFFFF);
        extractor.fill(pinBoxX, pinBoxY, pinBoxX + 1, pinBoxY + pinBoxSize, 0x44FFFFFF);
        extractor.fill(pinBoxX + pinBoxSize - 1, pinBoxY, pinBoxX + pinBoxSize, pinBoxY + pinBoxSize, 0x44FFFFFF);

        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, PIN_SPRITE, pinBoxX + 4, pinBoxY + 4, 18, 18, 0xFF000000 | this.selectedColor);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        }
    }
}
