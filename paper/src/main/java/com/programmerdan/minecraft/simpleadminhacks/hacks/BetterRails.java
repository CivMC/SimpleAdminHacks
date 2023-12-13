package com.programmerdan.minecraft.simpleadminhacks.hacks;


import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.BetterRailsConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public final class BetterRails extends SimpleHack<BetterRailsConfig> implements Listener {

	// A minecart goes at 8m/s but its internal speed is 0.4, this adjusts for that
	private static final double METRES_PER_SECOND_TO_SPEED = 0.05;
	private static final double VANILLA_SPEED = 0.4;

	public BetterRails(SimpleAdminHacks plugin, final BetterRailsConfig config) {
		super(plugin, config);
	}

	public static BetterRailsConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
		return new BetterRailsConfig(plugin, config);
	}

	@Override
	public void onEnable() {
		plugin().registerListener(this);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void on(VehicleMoveEvent event) {
		if (!(event.getVehicle() instanceof Minecart minecart)) {
			return;
		}

		Location to = event.getTo();
		Location from = event.getFrom();
		if (to.getBlockX() == from.getBlockX() && to.getBlockY() == from.getBlockY() && to.getBlockZ() == from.getBlockZ()) {
			return;
		}

		for (Entity entity : minecart.getPassengers()) {
			if (entity instanceof Player) {
				adjustSpeed(minecart);
				return;
			}
		}
	}

	@EventHandler
	public void on(VehicleEnterEvent event) {
		if (!(event.getVehicle() instanceof Minecart minecart)) {
			return;
		}

		if (event.getEntered() instanceof Player) {
			adjustSpeed(minecart);
		}
	}

	@EventHandler
	public void on(VehicleExitEvent event) {
		if (!(event.getVehicle() instanceof Minecart minecart)) {
			return;
		}

		// Empty minecarts should return to their vanilla speed
		minecart.setMaxSpeed(VANILLA_SPEED);
	}


	private void adjustSpeed(Minecart minecart) {
		Material belowRail = minecart.getLocation().subtract(0, 1, 0).getBlock().getType();
		Material belowRail2 = minecart.getLocation().subtract(0, 2, 0).getBlock().getType();

		Double belowRailSpeed = config.getMaxSpeedMetresPerSecond(belowRail);
		Double belowRail2Speed = config.getMaxSpeedMetresPerSecond(belowRail2);

		double speedMetresPerSecond;
		if (belowRailSpeed != null && belowRail2Speed != null) {
			speedMetresPerSecond = Math.max(belowRailSpeed, belowRail2Speed);
		} else if (belowRailSpeed != null) {
			speedMetresPerSecond = belowRailSpeed;
		} else if (belowRail2Speed != null) {
			speedMetresPerSecond = belowRail2Speed;
		} else {
			speedMetresPerSecond = config.getBaseSpeed();
		}

		minecart.setMaxSpeed(speedMetresPerSecond * METRES_PER_SECOND_TO_SPEED);
	}
}
