package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import org.bukkit.Material;

/**
 * Concrete chunk data containing the real data for a chunk. This class uses an internal format for speedup purposes,
 * and should not be serialized reflectively.
 */
public class ConcreteChunkData implements IChunkData {

	private final int[] materialCounters = new int[Material.values().length];

	@Override
	public int inc(Material type, int amount) {
		materialCounters[type.ordinal()] += amount;

		return materialCounters[type.ordinal()];
	}

	@Override
	public int dec(Material type, int amount) {
		materialCounters[type.ordinal()] -= amount;

		return materialCounters[type.ordinal()];
	}

	@Override
	public int get(Material type) {
		return materialCounters[type.ordinal()];
	}

	@Override
	public int set(Material type, int value) {
		var oldValue = materialCounters[type.ordinal()];
		materialCounters[type.ordinal()] = value;
		return oldValue;
	}
}
