package com.locatortweaks.gui.yacl;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.network.chat.Component;

public class KeyBindingController implements Controller<Integer> {
    private final Option<Integer> option;

    public KeyBindingController(Option<Integer> option) {
        this.option = option;
    }

    @Override
    public Option<Integer> option() {
        return this.option;
    }

    @Override
    public Component formatValue() {
        int key = this.option.pendingValue();
        if (key == InputConstants.UNKNOWN.getValue() || key <= 0) {
            return Component.translatable("key.keyboard.unknown");
        }
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName();
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dimension) {
        return new KeyBindingWidget(this, screen, dimension);
    }
}
