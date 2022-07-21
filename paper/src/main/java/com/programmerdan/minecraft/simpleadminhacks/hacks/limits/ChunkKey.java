package com.programmerdan.minecraft.simpleadminhacks.hacks.limits;

import java.util.UUID;

/**
 * Represents a chunk. HashCode AND equals must be implemented.
 */
public record ChunkKey(int x, int z, UUID worldUUID) {

}
