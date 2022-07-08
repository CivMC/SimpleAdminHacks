package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.BetterVehiclesConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityMountEvent;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 *
 */
public class BetterVehicles extends SimpleHack<BetterVehiclesConfig> {

	public static final String NAME = "BetterVehicles";

	private final Map<UUID, TrackedEntity> trackedEntities = new HashMap<>();

	private static final class TrackedEntity {

		private final UUID id;
		private long touched;
		private UUID owner;

		public TrackedEntity(Entity entity, UUID owner) {
			this(entity.getUniqueId(), System.currentTimeMillis(), owner);
		}

		public TrackedEntity(Entity entity) {
			this(entity.getUniqueId(), System.currentTimeMillis(), null);
		}

		public TrackedEntity(UUID id, long touched, UUID owner) {
			this.id = id;
			this.touched = touched;
			this.owner = owner;
		}

		public UUID id() {
			return id;
		}

		public long touched() {
			return touched;
		}

		public Optional<UUID> owner() {
			if (owner == null) {
				return Optional.empty();
			} else {
				return Optional.of(owner);
			}
		}

		public void setOwner(UUID owner) {
			this.owner = owner;
		}

		public Entity toEntity() {
			return Bukkit.getEntity(id);
		}

		public void touch() {
			touched = System.currentTimeMillis();
		}

		private static TrackedEntity of(TrackedEntity en, Player owner) {
			return new TrackedEntity(Objects.requireNonNull(en).id(), System.currentTimeMillis(), Objects.requireNonNull(owner).getUniqueId());
		}

		public static TrackedEntity of(Entity en, Player owner) {
			return new TrackedEntity(Objects.requireNonNull(en).getUniqueId(), System.currentTimeMillis(), Objects.requireNonNull(owner).getUniqueId());
		}

		public static TrackedEntity of(Entity en) {
			return new TrackedEntity(Objects.requireNonNull(en).getUniqueId(), System.currentTimeMillis(), null);
		}
	}

	public BetterVehicles(SimpleAdminHacks plugin, BetterVehiclesConfig config) {
		super(plugin, config);
	}

	private void addListener(Listener listener) {
		Bukkit.getServer().getPluginManager().registerEvents(listener, plugin());
	}

	private static void debugLog(String format, Object... values) {
//		var msg = String.format(format, values);
//		System.out.println(msg);
//		Bukkit.broadcast(Component.text(msg));
	}

	/**
	 * Sets up the tracking for an entity, adding it to the tracked map. This will NOT set an owner on the entity.
	 * This action is idempotent and will not add the entitiy if it is already tracked.
	 *
	 * @param e Entity to track, cannot be null.
	 */
	private void setupEntityTracking(Entity e) {
		debugLog("Adding entity: %s (%d, %d, %d, %s)\n", e.getType(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ(), e.getLocation().getWorld().getName());

		trackedEntities.put(Objects.requireNonNull(e).getUniqueId(), TrackedEntity.of(e));
	}

	/**
	 * Sets up the tracking for an entity, adding it to the tracked map. This will set an owner on the entity.
	 * This action is idempotent and will not add the entitiy if it is already tracked.
	 *
	 * @param e     Entity to track, cannot be null.
	 * @param owner Owner of the entity, cannot be null.
	 */
	private void setupEntityTracking(Entity e, Player owner) {
		debugLog("Adding entity: %s (%d, %d, %d, %s) - %s\n", e.getType(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ(), e.getLocation().getWorld().getName(), owner);

		trackedEntities.put(Objects.requireNonNull(e).getUniqueId(), TrackedEntity.of(e, owner));
	}

	/**
	 * Return true if this entity type is tracked by this hack, false if not.
	 *
	 * @param entity Entity to check if tracked. Cannot be null.
	 * @return True if the entity is a tracked type.
	 */
	private static boolean isTrackedEntity(Entity entity) {
		switch (Objects.requireNonNull(entity).getType()) {
			case BOAT:
			case MINECART:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Setup tracking or update tracking, depending if the tracking record already exists. If it does, the owner
	 * will be updated.
	 *
	 * @param e     Entity to setup or update tracking on. Cannot be null.
	 * @param owner Owner to set / update. Cannot be null.
	 */
	private void setupOrUpdateEntityTracking(Entity e, Player owner) {
		debugLog("Updating entity: %s (%d, %d, %d, %s)\n", e.getType(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ(), e.getLocation().getWorld().getName());

		Objects.requireNonNull(e);
		Objects.requireNonNull(owner);
		trackedEntities.compute(e.getUniqueId(), (key, value) -> {
			if (value == null) {
				return TrackedEntity.of(e, owner);
			} else {
				value.setOwner(owner.getUniqueId());
				return value;
			}
		});
	}

	private void indexCurrentlyLoadedEntities() {
		Bukkit.getWorlds().stream().map(World::getEntities).flatMap(Collection::stream).filter(BetterVehicles::isTrackedEntity).forEach(this::setupEntityTracking);
	}

	private void indexChunkEntitiesOnEvent(List<Entity> entities) {
		entities.stream().filter(BetterVehicles::isTrackedEntity).forEach(BetterVehicles.this::setupEntityTracking);
	}

	/**
	 * Garbage collect entities from the list of entities.
	 *
	 * @param entities Entities to check if they have expired.
	 */
	private void garbageCollectEntitiesOnChunkLoad(List<Entity> entities) {
		long starttime = System.currentTimeMillis();

		var it = entities.iterator();
		while (it.hasNext()) {
			var en = it.next();
			trackedEntities.computeIfPresent(en.getUniqueId(), (key, value) -> {
				if (evictByAge(value, starttime)) {
					it.remove();
					return null;
				} else {
					return value;
				}
			});
		}
	}

	/**
	 * Register the basic listeners that are always used.
	 */
	private void addBaseListeners() {
		addListener(new Listener() {
			@EventHandler
			public void onEntityLoad(EntitiesLoadEvent ev) {
				indexChunkEntitiesOnEvent(ev.getEntities());
			}

			@EventHandler
			public void onEntitySpawn(EntitySpawnEvent ev) {
				if (isTrackedEntity(ev.getEntity())) {
					debugLog("Spawn: %s", ev.getEntityType());
					setupEntityTracking(ev.getEntity());
				}
			}

			@EventHandler
			public void onPlayerPlaceEntity(EntityPlaceEvent ev) {
				if (isTrackedEntity(ev.getEntity())) {
					debugLog("Place: %s", ev.getEntityType());
					setupEntityTracking(ev.getEntity(), ev.getPlayer());
				}
			}

			@EventHandler
			public void onPlayerMount(EntityMountEvent ev) {
				if (isTrackedEntity(ev.getEntity()) && ev.getEntity() instanceof Player) {
					debugLog("Mount: %s", ev.getEntityType());
					setupOrUpdateEntityTracking(ev.getMount(), (Player) ev.getEntity());
				}
			}

			@EventHandler
			public void onEntityRemove(EntityRemoveFromWorldEvent ev) {
				if (isTrackedEntity(ev.getEntity())) {
					debugLog("Untracking: %s", ev.getEntityType());
					untrackEntity(ev.getEntity());
				}
			}
		});
	}

	/**
	 * Removes the tracking data on an entity. This will not drop an item or interact with the world in any way.
	 * This will remove tracking internally in this plugin only.
	 *
	 * @param entity Entity to remove from internal tracking.
	 */
	private void untrackEntity(Entity entity) {
		trackedEntities.remove(entity.getUniqueId());
	}

	/**
	 * Add interaction listeners which may or may not be enabled.
	 */
	private void addInteractionListeners() {
		if (config().isReturnVehiclesToInventoryOnExit()) {
			debugLog("Vehicles will be returned to the owner on vehicle exit.");
			addListener(new Listener() {
				@EventHandler
				public void vehicleExit(VehicleExitEvent ev) {
					if (ev.getExited() instanceof Player) {
						var player = (Player) ev.getExited();
						if (ev.getVehicle() instanceof Vehicle) {
							var vehicle = (Vehicle) ev.getVehicle();
							// Return the vehicle to the owners inventory, or leave it alone if the inventory is full.
							// Adding more than one stack will requires this section to be adjusted.
							var stack = toItemStack(vehicle);
							var unstorable = player.getInventory().addItem(stack);

							if (unstorable.isEmpty()) {
								vehicle.remove();
								trackedEntities.remove(vehicle.getUniqueId());

								player.sendMessage(String.format("%s returned to inventory.", vehicle.getType()));
							} else {
								player.sendMessage(String.format("Inventory full, %s will remain here.", vehicle.getType()));
							}
						}
					}
				}
			});

			if (config().isGarbageCollectVehicles()) {
				debugLog("Vehicles will be garbage collected. Max. Age: %s sec", config().getMaxVehicleAgeInSeconds());

				addListener(new Listener() {
					@EventHandler
					public void vehicleTracking(ChunkUnloadEvent ev) {
						long starttime = System.currentTimeMillis();
						for (Entity en : ev.getChunk().getEntities()) {
							trackedEntities.computeIfPresent(en.getUniqueId(), (key, value) -> {
								if (evictByAge(value, starttime)) {
									return null;
								} else {
									return value;
								}
							});
						}
					}

					@EventHandler
					public void vehicleTracking(EntitiesLoadEvent ev) {
						garbageCollectEntitiesOnChunkLoad(ev.getEntities());
					}
				});
			}
		}
	}

	private ItemStack toItemStack(Vehicle vehicle) {
		if (vehicle instanceof Boat) {
			return new ItemStack(((Boat) vehicle).getBoatMaterial(), 1);
		} else if (vehicle instanceof Minecart) {
			return new ItemStack(Material.MINECART, 1);
		} else {
			throw new IllegalStateException("Cannot map vehicle to item stack: " + vehicle);
		}
	}

	/**
	 * Check whether an entity can be evicted or not.
	 *
	 * @param en Entity record to check against.
	 * @return True if the entity can be evicted for any number of reasons.
	 */
	private boolean canEvict(TrackedEntity en) {
		Entity e = en.toEntity();

		if (e == null) {
			return true;
		} else if (e instanceof Vehicle) {
			Vehicle v = (Vehicle) e;
			// Players in vehicles will stop them from being evicted.
			return !v.getPassengers().stream().anyMatch(p -> p instanceof Player);
		} else {
			return true;
		}
	}

	/**
	 * Evict an entity by age. If the entity can't be evicted (#canEvict) or the entity is not old enough to be evicted,
	 * this method does nothing. This method will do internal checks and may drop items in the world, or modify
	 * player inventory.
	 *
	 * @param rec       Entity record to check.
	 * @param starttime Time when the eviction process was started. (May be System.currentTimeMillis())
	 * @return True if the entity was removed, else false.
	 */
	private boolean evictByAge(TrackedEntity rec, long starttime) {
		//TODO prevent removal if someone is in the boat.
		if (canEvict(rec)) {
			var ageInSeconds = TimeUnit.MILLISECONDS.toSeconds(starttime - rec.touched());
			debugLog("Age: %d / %d", ageInSeconds, config().getMaxVehicleAgeInSeconds());
			if (ageInSeconds > config().getMaxVehicleAgeInSeconds()) {
				debugLog("Evicting on gc: " + rec.id());
				notifyOwnerOnGCEvent(rec);
				// For some ungodly reason, if you do not defer this until the next tick, it will throw a
				// ConcurrentModificationException.
				Bukkit.getScheduler().runTask(plugin(), () -> {
					var en = rec.toEntity();
					var loc = en.getLocation();
					if (en != null) {
						debugLog("Strategy: %s", config().getGarbageCollectVehicleStrategy());
						switch (config().getGarbageCollectVehicleStrategy()) {
							case DROP_ITEMSTACK:
								debugLog("Dropping itemstack of vehicle.");
								var vehicle = (Vehicle) en;
								var stack = toItemStack(vehicle);
								en.remove();
								loc.getWorld().dropItemNaturally(loc, stack);
								break;
							case REMOVE:
								debugLog("Removing vehicle.");
								en.remove();
								break;
							default:
								throw new IllegalStateException("Unknown collections strategy: " + config().getGarbageCollectVehicleStrategy());
						}
					}
				});
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	/**
	 * Add the garbage collectors, and necessary ticking infrastructure to support the garbage collecting process.
	 */
	private void addGarbageCollectors() {
		if (config().isGarbageCollectVehicles()) {
			//Skip cycles where we take longer or run twice at the same time.
			final AtomicBoolean runLock = new AtomicBoolean(false);
			Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin(), () -> {
				if (runLock.getAndSet(true)) return;

				debugLog("Running vehicle GC.");
				long starttime = System.currentTimeMillis();
				trackedEntities.entrySet().removeIf(en -> evictByAge(en.getValue(), starttime));
				runLock.set(false);
			}, config().getGcIntervalInTicks(), config().getGcIntervalInTicks());
		}
	}

	/**
	 * Notify the owner of a tracked entity if their vehicle was removed. Entity (NOT RECORD) and owner may be null in
	 * this case. If the entity is null, this method only notifies about a removal. If the owner is null, nobody
	 * is notified.
	 *
	 * @param record Record to notify about.
	 */
	private void notifyOwnerOnGCEvent(TrackedEntity record) {
		Entity en = Bukkit.getEntity(record.id());
		Component msg;

		if (en != null) {
			msg = Component.text(String.format("Your %s @%d,%d was removed.", en.getType(), en.getLocation().getBlockY(), en.getLocation().getBlockY()));
		} else {
			msg = Component.text(String.format("Your %s was removed.", en.getType()));
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> record.owner().map(Bukkit::getPlayer).ifPresent(c -> c.sendMessage(msg)));
	}

	private void startEntityTracking() {
		indexCurrentlyLoadedEntities();
		addBaseListeners();
		addInteractionListeners();
		addGarbageCollectors();
	}

	private void startRecordFlushTask() {
		Bukkit.getScheduler().runTaskTimer(plugin(), this::storeRecords, config().getFlushRecordIntervalInTicks(), config.getFlushRecordIntervalInTicks());
	}

	@Override
	public void onEnable() {
		loadRecords();
		startEntityTracking();
		startRecordFlushTask();
	}

	@Override
	public void onDisable() {
		storeRecords();
	}

	public static BetterVehiclesConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
		return new BetterVehiclesConfig(plugin, config);
	}

	private void loadRecords() {
		final var listType = new TypeToken<ArrayList<TrackedEntity>>() {
		}.getType();

		var gson = new GsonBuilder().create();
		var path = Paths.get(config().getPersistenceFilePath());

		if (Files.exists(path)) {

			List<TrackedEntity> list = Collections.emptyList();
			if (Files.exists(Paths.get(config().getPersistenceFilePath()))) {
				try (FileReader reader = new FileReader(config().getPersistenceFilePath())) {
					list = gson.fromJson(reader, listType);
				} catch (Exception e) {
					plugin().getLogger().log(Level.SEVERE, "Failed to persist BetterVehicle records.");
				}
			}

			list.forEach(en -> trackedEntities.put(en.id(), en));
		}
	}

	private void storeRecords() {
		var gson = new GsonBuilder().create();
		var path = Paths.get(config().getPersistenceFilePath());

		if (!Files.exists(path.getParent())) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			gson.toJson(new ArrayList<>(trackedEntities.values()), writer);
		} catch (Exception e) {
			plugin().getLogger().log(Level.SEVERE, "Failed to persist BetterVehicle records.");
		}
	}
}
