package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.OneTimeTeleportConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;

public class OneTimeTeleport extends SimpleHack<OneTimeTeleportConfig> implements CommandExecutor {

	private BooleanSetting hasOTT;
	private Map<UUID, UUID> senderToReciever;

	public OneTimeTeleport(SimpleAdminHacks plugin, OneTimeTeleportConfig config) {
		super(plugin, config);
		this.senderToReciever = new HashMap<>();
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.AQUA + "Old school command sender!, Go away console, this is players only");
			return true;
		}
		// /ott should return if you can ott
		// /ott <player> should request ott
		// /ott revoke should cancel a request
		// /ott accept <player> should do teleport if valid ott request
		switch (args.length) {
			case 0:
				player.sendMessage(Component.text("Can you use a one time teleport? " + this.hasOTT.getValue(player.getUniqueId()), NamedTextColor.AQUA));
				return true;
			case 1:
				Player target = Bukkit.getPlayer(args[0]);
				if (target == null) {
					player.sendMessage(Component.text("The player " + args[0] + " does not exist or isn't online!", NamedTextColor.RED));
					return true;
				}
				if (!doesPlayerHaveOTTAvailable(player.getUniqueId())) {
					if (this.senderToReciever.containsKey(player.getUniqueId())) {
						player.sendMessage(Component.text("Revoke your existing request first!", NamedTextColor.RED));
						return true;
					}
					player.sendMessage(Component.text("You have already used your OTT!", NamedTextColor.RED));
					return true;
				}
				player.sendMessage(Component.text("You have requested to teleport to " + target.getName() + "!", NamedTextColor.GREEN));
				requestOTT(player.getUniqueId(), target.getUniqueId());
				return true;
			case 2:
				if (args[0].equalsIgnoreCase("revoke")) {
					this.hasOTT.setValue(player.getUniqueId(), true);
					this.senderToReciever.remove(player.getUniqueId());
					player.sendMessage(Component.text("You have revoked your OTT request!", NamedTextColor.GREEN));
					return true;
				}
			case 3:
				if (args[0].equalsIgnoreCase("accept")) {
					UUID uuid = null;
					for (Map.Entry<UUID, UUID> entries : this.senderToReciever.entrySet()) {
						if (entries.getValue().equals(player.getUniqueId())) {
							uuid = entries.getKey();
							break;
						}
					}
					if (uuid == null) {
						player.sendMessage(
								Component.text("We couldn't find anyone who has requested to teleport to you!",
										NamedTextColor.RED));
						return true;
					}
					this.senderToReciever.remove(uuid);
					this.hasOTT.setValue(uuid, false);
					Player targetPlayer = Bukkit.getPlayer(args[1]);
					if (targetPlayer == null) {
						player.sendMessage(Component.text("The player " + args[1] + " does not exist or isn't online!", NamedTextColor.RED));
						return true;
					}
					long timeJoined = targetPlayer.getFirstPlayed();
					if (System.currentTimeMillis() > (timeJoined + config.getTimelimitOnUsageInMillis())) {
						targetPlayer.sendMessage(Component.text("You have ran out of time to use your one time teleport!"));
						return true;
					}
					removeBlacklistItems(targetPlayer.getInventory());
					targetPlayer.teleport(player.getLocation());
					player.sendMessage(Component.text(targetPlayer.getName() + " has been teleported to you!", NamedTextColor.GREEN));
					return true;
				}
			default:
				return false;
		}
	}

	public boolean doesPlayerHaveOTTAvailable(UUID uuid) {
		return this.hasOTT.getValue(uuid);
	}

	public void requestOTT(UUID sender, UUID reciever) {
		this.senderToReciever.put(sender, reciever);
		this.hasOTT.setValue(sender, false);
	}

	public void removeBlacklistItems(Inventory inventory) {
		for (ItemStack is : inventory.getContents()) {
			if (is == null || is.getType() == Material.AIR) {
				continue;
			}
			if (this.config.getMaterialBlacklist().contains(is.getType())) {
				inventory.remove(is);
			}
		}
	}

	@EventHandler
	public void onFirstJoin(PlayerJoinEvent event) {
		if (!config.isEnabled()) {
			return;
		}
		if (event.getPlayer().hasPlayedBefore()) {
			return;
		}
		this.hasOTT.setValue(event.getPlayer().getUniqueId(), true);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		registerSettings();
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	@Override
	public void registerCommands() {
		if (config.isEnabled()) {
			plugin().registerCommand("ott", this);
		}
	}

	private void registerSettings() {
		//Default this to false since we want to set it true if the player has logged in for the first time
		this.hasOTT = new BooleanSetting(this.plugin,
				false,
				"Can you use a one time teleport?",
				"hasOTT",
				"Allows usage of /ott <player>");
		//We don't want to expose the setting to a players /config
		PlayerSettingAPI.registerSetting(hasOTT, null);
	}

	public static OneTimeTeleportConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
		return new OneTimeTeleportConfig(plugin, config);
	}
}
