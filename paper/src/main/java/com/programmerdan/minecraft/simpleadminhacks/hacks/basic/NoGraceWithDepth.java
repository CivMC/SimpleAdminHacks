package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public final class NoGraceWithDepth extends BasicHack {
	public NoGraceWithDepth(final SimpleAdminHacks plugin, final BasicHackConfig config) {
		super(plugin, config);
	}

	@EventHandler(ignoreCancelled = true)
	public void removeGraceOnMove(final PlayerMoveEvent event) {
		if (!event.hasExplicitlyChangedPosition()) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
			return;
		}
		final ItemStack boots = player.getInventory().getBoots();
		if (boots == null || !boots.containsEnchantment(Enchantment.DEPTH_STRIDER)) {
			return;
		}
		player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
	}
}
