package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.google.common.collect.Maps;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import vg.civcraft.mc.civmodcore.utilities.CivLogger;

import java.util.*;

public final class BetterRailsConfig extends SimpleHackConfig {

	private final CivLogger logger;

	private Map<Material, Double> speeds;
	private double baseSpeed = 8;

	public BetterRailsConfig(SimpleAdminHacks plugin, ConfigurationSection base) {
		super(plugin, base, false);
		this.logger = CivLogger.getLogger(getClass());
		wireup(base);
	}

	@Override
	protected void wireup(ConfigurationSection config) {
		this.baseSpeed = config.getDouble("base");

		ConfigurationSection materials = config.getConfigurationSection("materials");
		Set<String> keys = materials.getKeys(false);
		this.speeds = Maps.newHashMapWithExpectedSize(keys.size());
		for (String key : keys) {
			this.speeds.put(Material.valueOf(key), materials.getDouble(key));
		}
	}

	public Double getMaxSpeedMetresPerSecond(Material material) {
		return speeds.get(material);
	}

	public double getBaseSpeed() {
		return baseSpeed;
	}
}
