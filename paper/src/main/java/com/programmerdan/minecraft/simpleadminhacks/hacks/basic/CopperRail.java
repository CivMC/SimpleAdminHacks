package com.programmerdan.minecraft.simpleadminhacks.hacks.basic;

import com.destroystokyo.paper.MaterialTags;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHack;
import com.programmerdan.minecraft.simpleadminhacks.framework.BasicHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.AutoLoad;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class CopperRail extends BasicHack {

	// ServerLevel has a private version of this so we will make one ourselves
	private final io.papermc.paper.util.math.ThreadUnsafeRandom randomTickRandom = new io.papermc.paper.util.math.ThreadUnsafeRandom();

	@AutoLoad
	private boolean deoxidise;

	@AutoLoad
	private double damage;

	public CopperRail(SimpleAdminHacks plugin, BasicHackConfig config) {
		super(plugin, config);
	}

	@EventHandler
	public void on(VehicleMoveEvent event) {
		if (this.damage <= 0 || !(event.getVehicle() instanceof Minecart minecart)) {
			return;
		}

		boolean hasPlayer = false;
		for (Entity entity : minecart.getPassengers()) {
			if (entity instanceof Player) {
				hasPlayer = true;
				break;
			}
		}

		if (!hasPlayer) {
			return;
		}

		Location to = event.getTo();
		Location from = event.getFrom();
		if (to.getBlockX() == from.getBlockX() && to.getBlockY() == from.getBlockY() && to.getBlockZ() == from.getBlockZ()) {
			return;
		}

		Block copperBlock = minecart.getLocation().getBlock().getRelative(BlockFace.DOWN);
		Optional<net.minecraft.world.level.block.Block> next = WeatheringCopper.getNext(((CraftBlock) copperBlock).getNMS().getBlock());
		if (next.isEmpty()) {
			copperBlock = copperBlock.getRelative(BlockFace.DOWN);
		}

		next = WeatheringCopper.getNext(((CraftBlock) copperBlock).getNMS().getBlock());
		if (next.isEmpty()) {
			return;
		}

		CraftBlock craftBlock = (CraftBlock) copperBlock;
		BlockState state = craftBlock.getNMS();
		ServerLevel level = ((CraftWorld) copperBlock.getWorld()).getHandle();
		// We damage the copper directly instead of using random ticking, as random ticking is easy to cheese
		// by placing waxed copper next to the rail, entirely preventing the rest of the rail from oxidising.
		WeatheringCopper copper = (WeatheringCopper) state.getBlock();
		float chanceModifier = copper.getChanceModifier();
		if (this.damage * chanceModifier > this.randomTickRandom.nextFloat()) {
			copper.getNext(state).ifPresent((iblockdata2) -> {
				CraftEventFactory.handleBlockFormEvent(level, craftBlock.getPosition(), iblockdata2);
			});
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void on(PlayerInteractEvent event) {
		if (!this.deoxidise) {
			return;
		}

		ItemStack item = event.getItem();
		if (item == null || !MaterialTags.AXES.isTagged(item)) {
			return;
		}

		Block block = event.getClickedBlock();
		if (block == null || !MaterialTags.RAILS.isTagged(block)) {
			return;
		}

		Block copperBlock = block.getRelative(BlockFace.DOWN);
		Optional<BlockState> previous = WeatheringCopper.getPrevious(((CraftBlock) copperBlock).getNMS());
		if (previous.isEmpty()) {
			copperBlock = copperBlock.getRelative(BlockFace.DOWN);
		}

		previous = WeatheringCopper.getPrevious(((CraftBlock) copperBlock).getNMS());
		if (previous.isEmpty()) {
			return;
		}

		copperBlock.setType(previous.get().getBukkitMaterial());

		block.getWorld().playSound(block.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1, 1);
		block.getWorld().playEffect(block.getLocation(), Effect.OXIDISED_COPPER_SCRAPE, 0);

		CraftPlayer player = (CraftPlayer) event.getPlayer();
		// TODO: In 1.19 or above, this can be replaced with ItemStack#damage thanks to Paper
		((CraftItemStack) item).handle.hurtAndBreak(1, player.getHandle(), p -> {
			p.broadcastBreakEvent(event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
		});

		event.setCancelled(true);
	}
}
