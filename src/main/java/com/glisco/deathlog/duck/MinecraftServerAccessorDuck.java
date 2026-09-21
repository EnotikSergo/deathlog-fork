package com.glisco.deathlog.duck;

import net.minecraft.world.level.storage.LevelStorageSource;

public interface MinecraftServerAccessorDuck {
    LevelStorageSource.LevelStorageAccess deathlog_getSession();
}
