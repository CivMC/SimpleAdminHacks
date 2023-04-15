package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSetting;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.SettingChangeListener;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;

import java.util.UUID;

public class Fullbright extends BasicHack implements CommandExecutor {

	private BooleanSetting fullbrightEnabled;

	public Fullbright(SimpleAdminHacks plugin, BasicHackConfig config) {
		super(plugin, config);
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
			plugin().registerCommand("gamma", this);
		}
	}

	private void registerSettings(){
		this.fullbrightEnabled = new BooleanSetting(this.plugin,
				false,
				"Fullbright",
				"fullbrightEnabled",
				"Whether or not fullbright is enabled");
		this.fullbrightEnabled.registerListener((player, setting, oldValue, newValue) -> {
			Player ply = Bukkit.getPlayer(player);
			if(ply == null){
				return;
			}
			Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
				this.updateFullbright(ply);
			}, 1L);
		});
		PlayerSettingAPI.registerSetting(this.fullbrightEnabled, this.plugin.getSettingManager().getMainMenu());
		//this.plugin.getSettingManager().getMainMenu()
	}

	private void updateFullbright(Player player){
		if(this.fullbrightEnabled.getValue(player.getUniqueId())){
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60 * 60 * 24 * 14, 1, false, false, false));
		}else{
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
		}
	}

	private void toggleFullbright(Player player){
		this.fullbrightEnabled.toggleValue(player.getUniqueId());
		updateFullbright(player);
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatColor.AQUA + "Old school command sender!, Go away console, this is players only");
			return true;
		}

		this.toggleFullbright(player);

		return true;
	}

	public void onPlayerJoin(PlayerJoinEvent event){
		this.updateFullbright(event.getPlayer());
	}
}
