package com.locatortweaks.compat.modmenu;

import net.fabricmc.loader.api.FabricLoader;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class LocatorTweaksModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return YaclBlocker::getScreen;
        }

        return parent -> null;
    }
}
