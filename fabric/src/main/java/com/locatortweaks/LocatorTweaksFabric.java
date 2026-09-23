package com.locatortweaks;

import com.locatortweaks.config.ModConfig;
import net.fabricmc.api.ModInitializer;

public class LocatorTweaksFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		LocatorTweaks.init();
	}
}
