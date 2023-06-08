package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OneTimeTeleportConfig extends SimpleHackConfig {

	private List<Material> materialBlacklist;
	private List<Material> unsafeMaterials;
	private List<String> worldBlacklist;
	private long timelimitOnUsage;

	public OneTimeTeleportConfig(SimpleAdminHacks plugin,
								 ConfigurationSection base) {super(plugin, base);
	}

	@Override
	protected void wireup(ConfigurationSection config) {
		List<String> itemBlacklistString = config.getStringList("material_blacklist");
		if (itemBlacklistString.isEmpty()) {
			plugin().getLogger().warning("material_blacklist was empty? is this an error?");
		}
		this.materialBlacklist = new ArrayList<>();
		for (String s : itemBlacklistString) {
			Material material = Material.matchMaterial(s);
			if (material == null) {
				plugin().getLogger().warning("Material " + s + " in item black list for OTT couldn't be matched, skipping but is this a typo?");
				continue;
			}
			materialBlacklist.add(material);
		}


		this.unsafeMaterials = new ArrayList<>();
		List<String> unsafeMaterialsString = config.getStringList("unsafe_materials");
		for (String s : unsafeMaterialsString) {
			Material material = Material.matchMaterial(s);
			if (material == null) {
				plugin().getLogger().warning("Material " + s + " in unsafe materials list for OTT couldn't be matched, skipping but is this a typo?");
				continue;
			}
			materialBlacklist.add(material);
		}
		this.worldBlacklist = new ArrayList<>();
		List<String> worlds = config.getStringList("home_world_blacklist");
		for (String world : worlds) {
			plugin().getLogger().info("Adding: " + world + " : world to world blacklist for OTT");
			this.worldBlacklist.add(world);
		}
		this.timelimitOnUsage = ConfigHelper.parseTime(config.getString("ott_timeout", "2d"));
	}

	public List<Material> getMaterialBlacklist() {
		return Collections.unmodifiableList(materialBlacklist);
	}

	public List<Material> getUnsafeMaterials() {
		return Collections.unmodifiableList(unsafeMaterials);
	}

	public List<String> getWorldBlacklist() {
		return Collections.unmodifiableList(worldBlacklist);
	}

	public long getTimelimitOnUsageInMillis() {
		return timelimitOnUsage;
	}
}
