package com.lootbeams;

import com.lootbeams.compat.oneconfig.LootBeamsOneConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootBeamsClient implements ClientModInitializer {
	public static final String MODID = "lootbeams";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	@Override
	public void onInitializeClient() {
		LootBeamConfig.load();

		if (FabricLoader.getInstance().isModLoaded("oneconfig")) {
			try {
				LootBeamsOneConfig.init();
			} catch (Throwable t) {
				LOGGER.error("Failed to initialize OneConfig integration: ", t);
			}
		}
	}
}
