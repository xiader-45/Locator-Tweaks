package com.locatortweaks.gui.yacl;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class KeyBindingWidget extends ControllerWidget<KeyBindingController> {
    private boolean listening = false;

    public KeyBindingWidget(KeyBindingController control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
    }

    @Override
    protected Component getValueText() {
        if (this.listening) {
            return Component.literal("> ? <").withColor(0xFFFFA0);
        }
        return this.control.formatValue();
    }

    @Override
    protected int getValueColor() {
        if (this.listening) {
            return 0xFFFFA0;
        }
        return super.getValueColor();
    }

    @Override
    protected void extractValueText(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractValueText(extractor, mouseX, mouseY, delta);
        if (this.hovered) {
            extractor.requestCursor(this.isAvailable() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isMouseOver(event.x(), event.y()) && this.isAvailable()) {
            if (!this.listening) {
                this.listening = true;
                this.focused = true;
                this.screen.setFocused(this);
                this.playDownSound();
                return true;
            }
        } else if (this.listening) {
            this.listening = false;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.listening) {
            int key = event.key();
            if (key == InputConstants.KEY_ESCAPE) {
                this.control.option().requestSet(InputConstants.UNKNOWN.getValue());
            } else {
                this.control.option().requestSet(key);
            }
            this.listening = false;
            this.playDownSound();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void unfocus() {
        this.listening = false;
        super.unfocus();
    }

    @Override
    protected int getUnhoveredControlWidth() {
        return Math.max(60, this.textRenderer.width(this.getValueText()));
    }

    @Override
    protected int getHoveredControlWidth() {
        return this.getUnhoveredControlWidth();
    }
}
