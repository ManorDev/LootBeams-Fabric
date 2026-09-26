package com.lootbeams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class LootBeamConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lootbeams.json");

	public static ConfigData INSTANCE = new ConfigData();

	public static class ConfigData {
		public boolean all_items = true;
		public boolean only_rare = false;
		public boolean only_equipment = false;
		public double beam_radius = 1.0D;
		public double beam_height = 1.0D;
		public double beam_y_offset = 0.0D;
		public double beam_alpha = 0.85D;
		public double render_distance = 24.0D;
		public boolean render_name_color = true;
		public boolean render_rarity_color = true;
		public boolean render_nametags = true;
		public boolean render_nametags_onlook = true;
		public boolean render_stackcount = true;
		public double nametag_look_sensitivity = 0.018D;
		public double nametag_text_alpha = 1.0D;
		public double nametag_background_alpha = 0.5D;
		public double nametag_scale = 1.0D;
		public double nametag_y_offset = 0.75D;
		public boolean borders = true;
		public boolean white_rarities = false;
		public List<String> whitelist = new ArrayList<>();
		public List<String> blacklist = new ArrayList<>();
		public List<String> color_overrides = new ArrayList<>();
		public List<String> custom_rarities = new ArrayList<>();
	}

	public static void load() {
		try {
			if (CONFIG_FILE.exists()) {
				try (FileReader reader = new FileReader(CONFIG_FILE)) {
					INSTANCE = GSON.fromJson(reader, ConfigData.class);
					if (INSTANCE == null) {
						INSTANCE = new ConfigData();
					}
				}
			} else {
				save();
			}
		} catch (Exception e) {
			LootBeamsClient.LOGGER.error("Failed to load config file: ", e);
		}
	}

	public static void save() {
		try {
			CONFIG_FILE.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (Exception e) {
			LootBeamsClient.LOGGER.error("Failed to save config file: ", e);
		}
	}

	public static Color getColorFromItemOverrides(Item item) {
		if (INSTANCE.color_overrides != null && !INSTANCE.color_overrides.isEmpty()) {
			Identifier itemId = Registries.ITEM.getId(item);
			for (String unparsed : INSTANCE.color_overrides) {
				if (unparsed == null || unparsed.isEmpty()) continue;
				String[] configValue = unparsed.split("=");
				if (configValue.length == 2) {
					String nameIn = configValue[0];
					Color colorIn;
					try {
						colorIn = Color.decode(configValue[1]);
					} catch (Exception e) {
						continue;
					}
					if (!nameIn.contains(":")) {
						if (itemId.getNamespace().equals(nameIn)) {
							return colorIn;
						}
					} else {
						if (itemId.toString().equals(nameIn)) {
							return colorIn;
						}
					}
				}
			}
		}
		return null;
	}
}
