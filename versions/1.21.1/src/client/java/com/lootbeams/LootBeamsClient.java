package com.lootbeams;

import net.fabricmc.api.ClientModInitializer;
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
		LOGGER.info("LootBeams initialized for Fabric");
	}
}
