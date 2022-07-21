package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import java.util.concurrent.Callable;
import org.bukkit.Chunk;

public class Util {

	public static ChunkKey newChunkKey(Chunk c) {
		return new ChunkKey(c.getX(), c.getZ(), c.getWorld().getUID());
	}

	public static void timed(String name, Runnable run) {
		final var nanoStart = System.nanoTime();
		final var start = System.currentTimeMillis();
		run.run();
		final var end = System.currentTimeMillis() - start;
		final var nanoEnd = System.nanoTime() - nanoStart;

		System.out.format("Timing %s: %dms or %dns\n", name, end, nanoEnd);
	}

	public static <T> T timed(String name, Callable<T> run) throws Exception {
		final var nanoStart = System.nanoTime();
		final var start = System.currentTimeMillis();
		final var out = run.call();
		final var end = System.currentTimeMillis() - start;
		final var nanoEnd = System.nanoTime() - nanoStart;

		System.out.format("Timing %s: %dms or %dns\n", name, end, nanoEnd);

		return out;
	}
}
