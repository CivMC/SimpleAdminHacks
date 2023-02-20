package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.AutoLoad;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.impl.DoubleSetting;

public class Autopot extends BasicHack {

	@AutoLoad
	private boolean should_pots_have_aoe;

	private DoubleSetting doubleSetting;
	public Autopot(SimpleAdminHacks plugin, BasicHackConfig config) {
		super(plugin, config);
		this.doubleSetting = new DoubleSetting(this.plugin,
				10.0D,
				"Auto pot threshold",
				"SAHAutopotThreshold",
				new ItemStack(Material.SPLASH_POTION),
				"When below this amount of health, autopot will run. Remember that full health is 20");
	}

	@Override
	public void onEnable() {
		super.onEnable();
		registerSettings();
	}

	@EventHandler
	public void onTakingDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (!((player.getHealth() - event.getFinalDamage()) < getDoubleSetting(player.getUniqueId()))) {
			return;
		}
		HealthType type = checkForHealthPotionAndType(player.getInventory());
		if (type == null) {
			return;
		}
		Integer amplifer = type.getValue();
		if (amplifer == null) {
			return;
		}
		PotionEffect effect = new PotionEffect(PotionEffectType.HEAL, 1, amplifer, true, true, true);
		if (should_pots_have_aoe) {
			ItemStack itemStack = new ItemStack(Material.SPLASH_POTION);
			PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

			potionMeta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, amplifer), true);

			itemStack.setItemMeta(potionMeta);

			ThrownPotion thrownPotion = (ThrownPotion) player.getWorld().spawnEntity(player.getLocation(), EntityType.SPLASH_POTION);
			thrownPotion.setItem(itemStack);
			return;
		}
		player.addPotionEffect(effect);
		player.getWorld().playEffect(player.getLocation(), Effect.INSTANT_POTION_BREAK, Color.RED);
	}

	public void registerSettings() {
		PlayerSettingAPI.registerSetting(doubleSetting, this.plugin.getSettingManager().getMainMenu());
	}

	public Double getDoubleSetting(UUID uuid) {
		return doubleSetting.getValue(uuid);
	}

	public HealthType checkForHealthPotionAndType(Inventory inventory){
		if (inventory == null) {
			return null;
		}
		for (int i = inventory.getSize(); i >= 0; i--) {
			ItemStack is = inventory.getItem(i);
			if (is == null) {
				continue;
			}
			if (is.getType() != Material.SPLASH_POTION) {
				continue;
			}
			PotionMeta meta = (PotionMeta) is.getItemMeta();
			if (meta.getBasePotionData().getType() != PotionType.INSTANT_HEAL) {
				continue;
			}
			is.setAmount(is.getAmount() - 1);
			return meta.getBasePotionData().isUpgraded() ? HealthType.HEALTH_2 : HealthType.HEALTH_1;
		}
		return null;
	}

	private enum HealthType {
		HEALTH_1(1),
		HEALTH_2(2);

		private Integer value;

		HealthType(Integer integer){
			this.value = integer;
		}
		public Integer getValue(){
			return value;
		}
	}
}
