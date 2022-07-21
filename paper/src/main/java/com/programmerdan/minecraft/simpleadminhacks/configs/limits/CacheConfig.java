package com.programmerdan.minecraft.simpleadminhacks.configs.limits;

import java.util.concurrent.TimeUnit;

public record CacheConfig(
	int initialCapacity,
	int maximumSize,
	long expireAfterAccess,
	TimeUnit expireAfterAccessTimeUnit,
	long expireAfterWrite,
	TimeUnit expireAfterWriteTimeUnit
) {

}
