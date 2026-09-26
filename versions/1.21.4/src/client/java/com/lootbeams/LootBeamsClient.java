package com.lootbeams;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LootBeamsClient implements ClientModInitializer {
	public static final String MODID = "lootbeams";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final List<ItemStack> CRASH_BLACKLIST = new ArrayList<>();

	@Override
	public void onInitializeClient() {
		LootBeamConfig.load();
		if (FabricLoader.getInstance().isModLoaded("oneconfig")) {
			try {
				com.lootbeams.compat.oneconfig.LootBeamsOneConfig.init();
				LOGGER.info("LootBeams OneConfig integration initialized");
			} catch (Throwable t) {
				LOGGER.warn("Failed to initialize OneConfig integration", t);
			}
		}
		LOGGER.info("LootBeams initialized for Fabric");
	}
}
