package com.programmerdan.minecraft.simpleadminhacks.configs.limits;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import static java.util.Objects.requireNonNull;

public class LimitsConfig extends SimpleHackConfig {

	private List<BlockLimitConfig> chunkBlockLimits = new ArrayList<>();
	private CacheConfig chunkDataCacheConfig = new CacheConfig(
		1000,
		5000,
		8,
		TimeUnit.HOURS,
		8,
		TimeUnit.HOURS
	);

	public LimitsConfig(SimpleAdminHacks plugin, ConfigurationSection base) {
		super(plugin, base);
		wireup(base);
	}

	public LimitsConfig(SimpleAdminHacks plugin, ConfigurationSection base, boolean wireup) {
		super(plugin, base);
		wireup(base);
	}

	@Override
	protected void wireup(ConfigurationSection config) {
		chunkBlockLimits = config
			.getConfigurationSection("chunk_limits")
			.getMapList("block_limits")
			.stream()
			.map(LimitsConfig::newBlockLimit)
			.collect(Collectors.toList());

		var chunkCacheConfig = config
			.getConfigurationSection("chunk_limits").getConfigurationSection("cache_configuration");

		chunkDataCacheConfig = new CacheConfig(
			getInitialCapacityFromConfig(chunkCacheConfig),
			getMaximumCapacityFromConfig(chunkCacheConfig),
			getExpireAfterAccessFromConfig(chunkCacheConfig),
			parseTimeUnit(chunkCacheConfig, "expire_after_access_time_unit"),
			getExpireAfterWriteFromConfig(chunkCacheConfig),
			TimeUnit.valueOf(chunkCacheConfig.getString("expire_after_write_time_unit").trim().toUpperCase())
		);
	}

	private TimeUnit parseTimeUnit(ConfigurationSection chunkCacheConfig, String name) {
		var timeunitname = Objects.requireNonNull(chunkCacheConfig.getString(name));
		try {
			return TimeUnit.valueOf(chunkCacheConfig.getString(name).trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Cannot parse value for " + name + " \"" + timeunitname + "\". " +
				"Possible values: " + Arrays.toString(TimeUnit.values()), e);
		}
	}

	private long getExpireAfterAccessFromConfig(ConfigurationSection chunkCacheConfig) {
		var expireAfterAccessValue = chunkCacheConfig.getLong("expire_after_access_value");

		if (expireAfterAccessValue <= 0) {
			throw new IllegalArgumentException("expire_after_access_value for Limits cannot be less than 0.");
		}

		return expireAfterAccessValue;
	}

	private long getExpireAfterWriteFromConfig(ConfigurationSection chunkCacheConfig) {
		var expireAfterWriteValue = chunkCacheConfig.getLong("expire_after_write_value");

		if (expireAfterWriteValue <= 0) {
			throw new IllegalArgumentException("expire_after_write_value for Limits cannot be less than 0.");
		}

		return expireAfterWriteValue;
	}

	private int getMaximumCapacityFromConfig(ConfigurationSection chunkCacheConfig) {
		var initial = getInitialCapacityFromConfig(chunkCacheConfig);
		var maxconf = chunkCacheConfig.getInt("maximum_capacity");
		if (maxconf < 1) throw new IllegalArgumentException("maximum_capacity for Limits cannot be less than 1.");
		if (maxconf < initial) {
			throw new IllegalArgumentException("maximum_capacity for Limits cannot be less than initial_capacity.");
		}

		return maxconf;
	}

	private int getInitialCapacityFromConfig(ConfigurationSection chunkCacheConfig) {
		var initial = chunkCacheConfig.getInt("initial_capacity");
		if (initial < 1) throw new IllegalArgumentException("initial_capacity for Limits cannot be less than 1.");
		return initial;
	}

	public CacheConfig getChunkDataCacheConfig() {
		return chunkDataCacheConfig;
	}

	public void setChunkDataCacheConfig(CacheConfig chunkDataCacheConfig) {
		this.chunkDataCacheConfig = chunkDataCacheConfig;
	}

	public List<BlockLimitConfig> getChunkBlockLimits() {
		return chunkBlockLimits;
	}

	// We will ignore casting checks now. If the config is broken, the operator will have to deal
	// with the error appropriately.
	@SuppressWarnings("unchecked")
	private static BlockLimitConfig newBlockLimit(Map<?, ?> obj) {
		verifyStringListType(obj.get("materials"));
		List<Material> materials = parseMaterials((List<String>) obj.get("materials"));
		int max;

		if (obj.get("max") instanceof String s) {
			max = Integer.parseInt(s);
		} else if (obj.get("max") instanceof Integer i) {
			max = i;
		} else {
			throw new IllegalArgumentException("Unknown type: " + obj.get("max"));
		}

		return new BlockLimitConfig(materials, max);
	}

	private static void verifyStringListType(Object materials) {
		requireNonNull(materials);

		if (!(materials instanceof List)) {
			throw new ClassCastException(
				String.format("Cannot cast %s(%s) to List<String>", materials.getClass(), materials)
			);
		}

		((List<?>) materials).forEach(x -> {
			if (!(x instanceof String)) {
				throw new ClassCastException(
					String.format("Cannot cast %s(%s) to String", materials.getClass(), materials)
				);
			}
		});
	}

	private static List<Material> parseMaterials(List<String> materials) {
		return materials.stream()
			.peek(m -> {
				if (m == null) {
					throw new IllegalArgumentException("Materials list must be a list of strings.");
				}
			})
			.map(m -> {
				var mat = Material.getMaterial(m);
				if (mat == null) {
					throw new IllegalArgumentException("Material name " + m + " not found.");
				}

				return mat;
			})
			.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return "LimitsConfig{" +
			"chunkBlockLimits=" + chunkBlockLimits +
			'}';
	}
}
