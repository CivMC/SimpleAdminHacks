package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.limits.BlockLimitConfig;
import com.programmerdan.minecraft.simpleadminhacks.configs.limits.LimitsConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;


public class Limits extends SimpleHack<LimitsConfig> {

	private ChunkTrackingManager chunkTrackingManager;
	private Map<Material, List<BlockLimitConfig>> blockLimits = new HashMap<>();

	public Limits(SimpleAdminHacks plugin, LimitsConfig config) {
		super(plugin, config);
	}

	public static LimitsConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
		return new LimitsConfig(plugin, config, true);
	}

	private void addListener(Listener listener) {
		Bukkit.getPluginManager().registerEvents(listener, plugin());
	}


	private void addBaseListeners() {
		addListener(new Listener() {
			@EventHandler(ignoreCancelled = true)
			public void onBlockPlacement(BlockPlaceEvent ev) {
				var chunkdata = chunkTrackingManager.get(ev.getBlock().getChunk());
				var mat = ev.getBlockPlaced().getType();

				if (blockLimits.containsKey(mat)) {
					for (var limit : blockLimits.get(mat)) {
						var sum = sumOf(limit, chunkdata);
						if (sum >= limit.max()) {
							notifyPlayer(ev.getPlayer(), limit);
							ev.setCancelled(true);
							break;
						}
					}
				}
			}

			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void onBlockPlacementInc(BlockPlaceEvent ev) {
				var chunkdata = chunkTrackingManager.get(ev.getBlock().getChunk());
				chunkdata.inc(ev.getBlockPlaced().getType());
			}

			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void onBlockPlacementInc(BlockBreakEvent ev) {
				var chunkdata = chunkTrackingManager.get(ev.getBlock().getChunk());
				chunkdata.dec(ev.getBlock().getType());
			}

			@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
			public void onBlockPlacementInc(BlockDestroyEvent ev) {
				var chunkdata = chunkTrackingManager.get(ev.getBlock().getChunk());
				chunkdata.dec(ev.getBlock().getType());
			}
		});
	}

	private void notifyPlayer(Player player, BlockLimitConfig limit) {
		if (limit.type().size() > 1) {
			player.sendMessage(ChatColor.RED + "Compound block limit for " + limit.type() + " reached. (Max " + limit.max() + ")");
		} else {
			player.sendMessage(ChatColor.RED + "Block limit for " + limit.type().get(0) + " reached. (Max " + limit.max() + ")");
		}
	}

	//Explicit for loops are faster than streams.
	private static int sumOf(BlockLimitConfig limit, IChunkData chunkdata) {
		int sum = 0;
		for (var mat : limit.type()) {
			sum += chunkdata.get(mat);
		}

		return sum;
	}

	private void setupConfiguration() {
		chunkTrackingManager = new ChunkTrackingManager(config().getChunkDataCacheConfig());
		for (var limit : config().getChunkBlockLimits()) {
			for (Material mat : limit.type()) {
				blockLimits.putIfAbsent(mat, new ArrayList<>());
				blockLimits.get(mat).add(limit);
			}
		}
	}

	private void setupTrackingForLoadedWorlds() {
		List<Chunk> loadedChunks = Bukkit.getWorlds().stream()
			.map(World::getLoadedChunks).flatMap(Arrays::stream)
			.collect(Collectors.toList());

		final var name = String.format("setupTrackingForLoadedWorlds(%d)", loadedChunks.size());
		chunkTrackingManager.initTracking(loadedChunks);
	}

	private void teardownTrackers() {
		chunkTrackingManager.persist();
	}

	@Override
	public void onEnable() {
		setupConfiguration();
		addBaseListeners();
		setupTrackingForLoadedWorlds();
	}


	@Override
	public void onDisable() {
		teardownTrackers();
	}
}
