package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.bukkit.Material;

/**
 * A place holder for chunk data. This class will block and return proper values if necessary.
 */
public class FutureChunkData implements IChunkData {

	private final Future<ConcreteChunkData> future;

	public FutureChunkData(Future<ConcreteChunkData> future) {
		this.future = future;
	}

	@Override
	public int inc(Material type, int value) {
		try {
			return future.get().inc(type, value);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int dec(Material type, int value) {
		try {
			return future.get().dec(type, value);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int get(Material type) {
		try {
			return future.get().get(type);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int set(Material type, int value) {
		try {
			int oldValue = future.get().get(type);
			future.get().set(type, value);
			return oldValue;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
