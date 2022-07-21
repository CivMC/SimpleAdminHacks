package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import org.bukkit.Material;

/**
 * Chunk data holder class.
 */
public interface IChunkData {

	/**
	 * Increment the counter for the material by 1.
	 *
	 * @param type Type of material to increment. Cannot be null.
	 * @return Return the current value of the counter after incrementing.
	 */
	default int inc(Material type) {
		return inc(type, 1);
	}

	/**
	 * Increment the counter for the material by 1.
	 *
	 * @param type   Type of material to increment. Cannot be null.
	 * @param amount Amount to increment the counter by.
	 * @return Return the current value of the counter after incrementing.
	 */
	int inc(Material type, int amount);


	/**
	 * Decrement the counter for the material by 1.
	 *
	 * @param type Type of material to decrement. Cannot be null.
	 * @return Return the current value of the counter after decrementing.
	 */
	default int dec(Material type) {
		return dec(type, 1);
	}

	/**
	 * Decrement the counter for the material by 1.
	 *
	 * @param type   Type of material to increment. Cannot be null.
	 * @param amount Amount to decrement the counter by.
	 * @return Return the current value of the counter after decrementing.
	 */
	int dec(Material type, int amount);


	/**
	 * Get the current value of the counter for the material type.
	 *
	 * @param type Type of material.
	 * @return Current value of the counter for the material.
	 */
	int get(Material type);

	/**
	 * Set the value of the counter for the material.
	 *
	 * @param type  Type of material.
	 * @param value New value of the counter for the material.
	 * @return Old value of the counter for the material.
	 */
	int set(Material type, int value);
}
