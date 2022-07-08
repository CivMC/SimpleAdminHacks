package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author psygate
 */
public class BetterVehiclesConfig extends SimpleHackConfig {

	public enum GarbageCollectVehicleStrategy {
		DROP_ITEMSTACK,
		REMOVE;
	}

	private boolean returnVehiclesToInventoryOnExit = true;
	private boolean garbageCollectVehicles = true;
	private GarbageCollectVehicleStrategy garbageCollectVehicleStrategy;
	private long maxVehicleAgeInSeconds = 10;
	private boolean vehicleNotifyOnRemoval = true;

	private long gcIntervalInTicks = 20 * 5;
	private String persistenceFilePath;
	private long flushRecordIntervalInTicks = 10;

	public BetterVehiclesConfig(SimpleAdminHacks plugin, ConfigurationSection base) {
		super(plugin, base);
	}

	@Override
	protected void wireup(ConfigurationSection config) {
		returnVehiclesToInventoryOnExit = config.getBoolean("return_vehicle_to_inventory_on_exit");
		garbageCollectVehicles = config.getBoolean("garbage_collect_vehicle");
		garbageCollectVehicleStrategy = GarbageCollectVehicleStrategy.valueOf(config.getString("garbage_collect_vehicle_strategy").toUpperCase());
		maxVehicleAgeInSeconds = config.getLong("max_boat_age_in_seconds");
		gcIntervalInTicks = config.getLong("gc_interval_in_ticks");
		persistenceFilePath = config.getString("persistence_file_path");
		flushRecordIntervalInTicks = config.getLong("flush_record_interval_in_seconds");
	}

	public boolean isReturnVehiclesToInventoryOnExit() {
		return returnVehiclesToInventoryOnExit;
	}

	public void setReturnVehiclesToInventoryOnExit(boolean returnVehiclesToInventoryOnExit) {
		this.returnVehiclesToInventoryOnExit = returnVehiclesToInventoryOnExit;
	}

	public boolean isGarbageCollectVehicles() {
		return garbageCollectVehicles;
	}

	public void setGarbageCollectVehicles(boolean garbageCollectVehicles) {
		this.garbageCollectVehicles = garbageCollectVehicles;
	}

	public GarbageCollectVehicleStrategy getGarbageCollectVehicleStrategy() {
		return garbageCollectVehicleStrategy;
	}

	public void setGarbageCollectVehicleStrategy(GarbageCollectVehicleStrategy garbageCollectVehicleStrategy) {
		this.garbageCollectVehicleStrategy = garbageCollectVehicleStrategy;
	}

	public long getMaxVehicleAgeInSeconds() {
		return maxVehicleAgeInSeconds;
	}

	public void setMaxVehicleAgeInSeconds(long maxVehicleAgeInSeconds) {
		this.maxVehicleAgeInSeconds = maxVehicleAgeInSeconds;
	}

	public boolean isVehicleNotifyOnRemoval() {
		return vehicleNotifyOnRemoval;
	}

	public void setVehicleNotifyOnRemoval(boolean vehicleNotifyOnRemoval) {
		this.vehicleNotifyOnRemoval = vehicleNotifyOnRemoval;
	}

	public long getGcIntervalInTicks() {
		return gcIntervalInTicks;
	}

	public void setGcIntervalInTicks(long gcIntervalInTicks) {
		this.gcIntervalInTicks = gcIntervalInTicks;
	}

	public String getPersistenceFilePath() {
		return persistenceFilePath;
	}

	public void setPersistenceFilePath(String persistenceFilePath) {
		this.persistenceFilePath = persistenceFilePath;
	}

	public long getFlushRecordIntervalInTicks() {
		return flushRecordIntervalInTicks;
	}

	public void setFlushRecordIntervalInTicks(long flushRecordIntervalInTicks) {
		this.flushRecordIntervalInTicks = flushRecordIntervalInTicks;
	}
}
