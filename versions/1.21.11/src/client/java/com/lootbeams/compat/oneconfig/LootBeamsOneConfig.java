package com.lootbeams.compat.oneconfig;

import com.lootbeams.LootBeamConfig;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

public class LootBeamsOneConfig extends Config {
	public static final LootBeamsOneConfig INSTANCE = new LootBeamsOneConfig();

	@Switch(
			title = "Enable LootBeams",
			description = "Master switch to enable or disable loot beams and nametags"
	)
	public static boolean enabled = true;

	@Switch(
			title = "All Items",
			description = "Render loot beams for all dropped items"
	)
	public static boolean allItems = true;

	@Switch(
			title = "Only Rare Items",
			description = "Only render loot beams for rare items"
	)
	public static boolean onlyRare = false;

	@Switch(
			title = "Only Equipment",
			description = "Only render loot beams for equipment items"
	)
	public static boolean onlyEquipment = false;

	@Slider(
			title = "Beam Radius",
			description = "Width and scale of the beam cylinder",
			min = 0.1f,
			max = 3.0f,
			step = 0.05f
	)
	public static float beamRadius = 1.0f;

	@Slider(
			title = "Beam Height",
			description = "Height multiplier of the beam",
			min = 0.2f,
			max = 5.0f,
			step = 0.1f
	)
	public static float beamHeight = 1.0f;

	@Slider(
			title = "Beam Alpha",
			description = "Maximum opacity of the beam",
			min = 0.1f,
			max = 1.0f,
			step = 0.05f
	)
	public static float beamAlpha = 0.85f;

	@Slider(
			title = "Render Distance",
			description = "Maximum distance in blocks to render beams and nametags",
			min = 4.0f,
			max = 64.0f,
			step = 1.0f
	)
	public static float renderDistance = 24.0f;

	@Switch(
			title = "Render Nametags",
			description = "Render floating item name above dropped items"
	)
	public static boolean renderNametags = true;

	@Switch(
			title = "Nametags Only On Look",
			description = "Only show nametags when looking towards the item"
	)
	public static boolean renderNametagsOnLook = true;

	@Switch(
			title = "Render Stack Count",
			description = "Show item stack count in the nametag"
	)
	public static boolean renderStackCount = true;

	@Switch(
			title = "Borders",
			description = "Render colored borders on the nametag"
	)
	public static boolean borders = true;

	public LootBeamsOneConfig() {
		super("lootbeams.json", "LootBeams", "/assets/lootbeams/icon.png", Category.VISUALS);
		addCallback("enabled", LootBeamsOneConfig::syncToModConfig);
		addCallback("allItems", LootBeamsOneConfig::syncToModConfig);
		addCallback("onlyRare", LootBeamsOneConfig::syncToModConfig);
		addCallback("onlyEquipment", LootBeamsOneConfig::syncToModConfig);
		addCallback("beamRadius", LootBeamsOneConfig::syncToModConfig);
		addCallback("beamHeight", LootBeamsOneConfig::syncToModConfig);
		addCallback("beamAlpha", LootBeamsOneConfig::syncToModConfig);
		addCallback("renderDistance", LootBeamsOneConfig::syncToModConfig);
		addCallback("renderNametags", LootBeamsOneConfig::syncToModConfig);
		addCallback("renderNametagsOnLook", LootBeamsOneConfig::syncToModConfig);
		addCallback("renderStackCount", LootBeamsOneConfig::syncToModConfig);
		addCallback("borders", LootBeamsOneConfig::syncToModConfig);
	}

	public static void init() {
		INSTANCE.preload();
		syncToModConfig();
	}

	public static void syncToModConfig() {
		LootBeamConfig.INSTANCE.enabled = enabled;
		LootBeamConfig.INSTANCE.all_items = allItems;
		LootBeamConfig.INSTANCE.only_rare = onlyRare;
		LootBeamConfig.INSTANCE.only_equipment = onlyEquipment;
		LootBeamConfig.INSTANCE.beam_radius = beamRadius;
		LootBeamConfig.INSTANCE.beam_height = beamHeight;
		LootBeamConfig.INSTANCE.beam_alpha = beamAlpha;
		LootBeamConfig.INSTANCE.render_distance = renderDistance;
		LootBeamConfig.INSTANCE.render_nametags = renderNametags;
		LootBeamConfig.INSTANCE.render_nametags_onlook = renderNametagsOnLook;
		LootBeamConfig.INSTANCE.render_stackcount = renderStackCount;
		LootBeamConfig.INSTANCE.borders = borders;
	}
}
