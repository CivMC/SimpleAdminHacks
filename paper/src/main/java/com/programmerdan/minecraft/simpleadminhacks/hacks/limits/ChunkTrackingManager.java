package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import static com.programmerdan.minecraft.simpleadminhacks.hacks.limits.Util.newChunkKey;
import static java.util.Objects.requireNonNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.programmerdan.minecraft.simpleadminhacks.configs.limits.CacheConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks chunk data persistently and handles chunk indexing.
 * <p>
 * This could technically be a class that uses no persistence at all, based on {@link FutureChunkData},
 * {@link ConcreteChunkData} and {@link #loadOrCreateTrackingDataDeferred(Chunk)}
 */
public class ChunkTrackingManager {

	private final static Logger log = LoggerFactory.getLogger(ChunkTrackingManager.class);

	private final static ExecutorService pool = Executors.newWorkStealingPool();
	// might be configurable in the future. For now its fixed.
	private final static Path STORAGE_PREFIX = Paths.get("plugins/SimpleAdminHacks/data/chunk_tracking/");
	private final Cache<ChunkKey, IChunkData> chunkDataCache;

	public ChunkTrackingManager(CacheConfig config) {
		chunkDataCache = CacheBuilder.newBuilder()
			.initialCapacity(config.initialCapacity())
			.expireAfterAccess(config.expireAfterAccess(), config.expireAfterAccessTimeUnit())
			.expireAfterWrite(config.expireAfterWrite(), config.expireAfterWriteTimeUnit())
			.maximumSize(config.maximumSize()).removalListener((RemovalListener<ChunkKey, IChunkData>) this::persistConcreteChunkData)
			.build();
	}

	/**
	 * Returns the collected data for the provided chunk.
	 *
	 * @param chunk Chunk to look up or index, must not be null.
	 * @return ChunkData for the provided chunk.
	 */
	public IChunkData get(Chunk chunk) {
		requireNonNull(chunk);
		try {
			return chunkDataCache.get(newChunkKey(chunk), () -> loadOrCreateTrackingDataDeferred(chunk));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Persists all data without flushing the cache.
	 */
	public void persist() {
		for (Map.Entry<ChunkKey, IChunkData> en : chunkDataCache.asMap().entrySet()) {
			persistConcreteChunkData(en);
		}
	}

	/**
	 * Persist the data entry.
	 *
	 * @param chunkKeyChunkDataEntry Data entry to persist.
	 */
	private void persistConcreteChunkData(Map.Entry<ChunkKey, IChunkData> chunkKeyChunkDataEntry) {
		persistConcreteChunkData(chunkKeyChunkDataEntry.getKey(), chunkKeyChunkDataEntry.getValue());
	}

	/**
	 * Load or create tracking data for the provided chunk.
	 *
	 * @param chunk Chunk to index, cannot be null.
	 * @return A FutureChunkData representing the completed operation. May be used like a {@link ConcreteChunkData}, if
	 * this operation finishes, it will automatically replace the FutureChunkData in the cache with the
	 * {@link ConcreteChunkData} object.
	 */
	private FutureChunkData loadOrCreateTrackingDataDeferred(Chunk chunk) {
		requireNonNull(chunk);
		return new FutureChunkData(pool.submit(() -> {
			final var data = loadOrCreateTrackingData(chunk);
			chunkDataCache.put(newChunkKey(chunk), data);
			return data;
		}));
	}

	/**
	 * Destroys all cached data, including persisted data.
	 */
	public void destroyData() {
		chunkDataCache.invalidateAll();
		try (var dirstream = Files.list(STORAGE_PREFIX)) {
			dirstream.forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load or create index data for a chunk right now.
	 *
	 * @param chunk Chunk to load data for or create new data.
	 * @return ConcreteChunkData populated with the chunk index data.
	 */
	private static ConcreteChunkData loadOrCreateTrackingData(Chunk chunk) {
		requireNonNull(chunk);
		final var chunkKey = newChunkKey(chunk);
		var path = getStoragePath(chunkKey);
		if (Files.exists(path)) {
			var data = new ConcreteChunkData();

			try (
				var reader = Files.newBufferedReader(path);
				var jreader = newGsonInstance().newJsonReader(reader)
			) {
				jreader.beginObject();
				while (jreader.hasNext()) {
					final var name = jreader.nextName();
					final var value = jreader.nextInt();
					data.set(Material.valueOf(name), value);
				}
				jreader.endObject();
			} catch (IOException e) {
				log.warn("Failed to load chunk data: " + chunkKey + ", forcing reindexing.", e);
			}

			return data;
		} else {
			return newChunkData(chunk);
		}
	}

	/**
	 * This method persists the data provided to it ONLY IF its an instance of {@link ConcreteChunkData}
	 *
	 * @param key  ChunkKey to use for indexing of data. Cannot be null.
	 * @param data Data to persist. Any type BUT {@link ConcreteChunkData} will be ignored. Cannot be null.
	 */
	private void persistConcreteChunkData(ChunkKey key, IChunkData data) {
		requireNonNull(key);
		requireNonNull(data);

		if (data instanceof ConcreteChunkData) {
			try {
				var path = getStoragePath(key);
				if (!Files.exists(path.getParent())) {
					Files.createDirectories(path.getParent());
				}

				// We need to fix material values somehow. ChunkData may use some internal representation that doesn't
				// serialize well.
				try (
					var writer = Files.newBufferedWriter(path);
					var jwriter = new JsonWriter(writer)
				) {
					jwriter.beginObject();

					for (Material mat : Material.values()) {
						var value = data.get(mat);
						jwriter.name(mat.name()).value(value);
					}

					jwriter.endObject();
				}
			} catch (IOException e) {
				log.error("Failed to persist chunk data: " + key, e);
			}
		}
	}

	/**
	 * @param key {@link ChunkKey} to derive the path from. Cannot be null.
	 * @return Full storage path for the file corresponding to the {@link ChunkKey}.
	 */
	private static Path getStoragePath(ChunkKey key) {
		requireNonNull(key);
		return STORAGE_PREFIX.resolve(String.format("chunk_data.%d.%d.%s.json", key.x(), key.z(), key.worldUUID()));
	}

	/**
	 * @param chunk {@link Chunk} to index. Cannot be null.
	 * @param data  Data to store the chunk data in. The data IS NOT CLEARED before being used.
	 */
	private static void populateChunkData(Chunk chunk, ConcreteChunkData data) {
		requireNonNull(chunk);
		requireNonNull(data);

		//Optimized code. Ignore empty sections.
		final var snap = chunk.getChunkSnapshot(false, false, false);
		final int maxY = chunk.getWorld().getMaxHeight();
		final int minY = chunk.getWorld().getMinHeight();
		final int maxSection = (maxY - minY) >> 4;

		for (int ySection = 0; ySection < maxSection; ySection++) {
			if (snap.isSectionEmpty(ySection)) continue;

			for (int ySectionOffset = 0; ySectionOffset < 16; ySectionOffset++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						final int yTrue = (ySection << 4) + minY + ySectionOffset;
						data.inc(snap.getBlockType(x, yTrue, z));
					}
				}
			}
		}
	}

	/**
	 * Generates new chunk data for a chunk.
	 *
	 * @param chunk {@link Chunk} to index.
	 * @return Newly created {@link ConcreteChunkData} initialized for the provided chunk.
	 */
	private static ConcreteChunkData newChunkData(Chunk chunk) {
		requireNonNull(chunk);

		final var data = new ConcreteChunkData();
		populateChunkData(chunk, data);
		return data;
	}

	/**
	 * @return Gson instance.
	 */
	private static Gson newGsonInstance() {
		return new GsonBuilder().create();
	}

	/**
	 * @param chunks Chunks to initialize tracking on. This method makes no guarantee about the time until those chunks
	 *               are indexed. The work may be done in the calling thread or in another thread.
	 */
	public void initTracking(Collection<Chunk> chunks) {
		requireNonNull(chunks);

		for (Chunk chunk : chunks) {
			loadOrCreateTrackingDataDeferred(chunk);
		}
	}

	/**
	 * @param chunk Chunk to initialize tracking on. This method makes no guarantee about the time until those chunks
	 *              are indexed. The work may be done in the calling thread or in another thread.
	 */
	public void initTracking(Chunk chunk) {
		requireNonNull(chunk);

		try {
			chunkDataCache.get(newChunkKey(chunk), () -> loadOrCreateTrackingDataDeferred(chunk));
		} catch (ExecutionException e) {
			log.error("Failed to start tracking of materials for chunk: " + chunk, e);
		}
	}
}
